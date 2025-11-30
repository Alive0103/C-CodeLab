package com.codelab.application;

import com.codelab.domain.ExecutionRecord;
import com.codelab.domain.User;
import com.codelab.domain.repository.ExecutionRecordRepository;
import com.codelab.infrastructure.docker.ContainerPool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 使用Docker沙箱执行C代码的服务（容器池方案）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerCodeExecutionService {

    private final ExecutionRecordRepository recordRepository;
    private final ContainerPool containerPool;

    @Value("${code.tempDir}")
    private String tempDir;

    @Value("${code.dockerTimeout:15}")
    private int dockerTimeout;

    private static final int RUN_TIMEOUT = 5; // seconds
    private static final int MAX_OUTPUT_SIZE = 1024 * 1024; // 1MB
    private static final int CONTAINER_ACQUIRE_TIMEOUT = 30; // 获取容器的超时时间（秒）

    public ExecutionResult compileAndRun(String code, Long userId, String title) {
        try {
            if (code == null || code.length() > 10 * 1024) {
                return ExecutionResult.systemError("代码长度超过限制（最大10KB）");
            }

            // 创建临时目录
            Files.createDirectories(Paths.get(tempDir));
            String randomId = UUID.randomUUID().toString().substring(0, 8);
            Path codeFile = Paths.get(tempDir, "code_" + randomId + ".c");
            Path outputFile = Paths.get(tempDir, "output_" + randomId + ".txt");
            
            // 写入代码文件
            Files.write(codeFile, code.getBytes(StandardCharsets.UTF_8));

            // 在Docker容器中执行代码
            ExecutionResult result = executeInDocker(codeFile, outputFile);
            
            // 清理临时文件
            try {
                Files.deleteIfExists(codeFile);
                Files.deleteIfExists(outputFile);
            } catch (IOException e) {
                // 忽略清理错误
            }

            // 异步保存执行记录
            saveExecutionRecordAsync(userId, title, code, result.output, result.success, result.exitCode);

            return result;
        } catch (Exception e) {
            // 异步保存系统错误记录
            saveExecutionRecordAsync(userId, title, code, "执行失败：" + e.getMessage(), false, -2);
            return ExecutionResult.systemError("执行失败：" + e.getMessage());
        }
    }

    /**
     * 在Docker容器中执行代码（使用容器池）
     */
    private ExecutionResult executeInDocker(Path codeFile, Path outputFile) throws IOException, InterruptedException {
        ContainerPool.ContainerInfo container = null;
        String codeFileName = codeFile.getFileName().toString();
        String codePath = "/app/code/" + codeFileName;

        try {
            // 1. 从容器池获取容器
            container = containerPool.acquireContainer(CONTAINER_ACQUIRE_TIMEOUT, TimeUnit.SECONDS);
            if (container == null) {
                return ExecutionResult.systemError("无法获取容器，容器池可能已满或超时");
            }

            // 检查容器健康状态
            if (!containerPool.isContainerHealthy(container.getName())) {
                return ExecutionResult.systemError("容器健康检查失败");
            }

            // 2. 读取代码内容
            String codeContent = new String(Files.readAllBytes(codeFile), StandardCharsets.UTF_8);
            
            // 3. 先写入代码文件到容器（通过 stdin）
            List<String> writeCmd = new ArrayList<>();
            writeCmd.add("docker");
            writeCmd.add("exec");
            writeCmd.add("-i"); // 使用 stdin
            writeCmd.add(container.getName());
            writeCmd.add("sh");
            writeCmd.add("-c");
            writeCmd.add(String.format(
                "mkdir -p /app/code && chmod 777 /app && chmod 777 /app/code && cat > %s",
                codePath
            ));
            
            log.debug("写入代码文件到容器: {}", container.getName());
            ProcessBuilder writePb = new ProcessBuilder(writeCmd);
            writePb.redirectErrorStream(true);
            Process writeProcess = writePb.start();
            
            // 将代码内容写入到进程的 stdin
            try (OutputStream os = writeProcess.getOutputStream()) {
                os.write(codeContent.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close(); // 关闭 stdin，让 cat 命令知道输入结束
            }
            
            String writeOutput = readStream(writeProcess.getInputStream(), MAX_OUTPUT_SIZE);
            boolean writeFinished = writeProcess.waitFor(5, TimeUnit.SECONDS);
            
            if (!writeFinished || writeProcess.exitValue() != 0) {
                log.error("写入代码文件失败: {}", writeOutput);
                return ExecutionResult.systemError("无法写入代码文件到容器: " + writeOutput);
            }
            log.debug("代码文件写入成功");
            
            // 4. 在容器中编译和执行代码
            List<String> execCmd = new ArrayList<>();
            execCmd.add("docker");
            execCmd.add("exec");
            execCmd.add(container.getName());
            execCmd.add("/bin/bash");
            execCmd.add("-c");
            execCmd.add(String.format(
                "cd /app/code && " +
                "gcc -o /app/code/program %s -Wall -Wextra 2>&1 && " +
                "chmod +x /app/code/program && " +
                "sh -c '/app/code/program' 2>&1 || exit $?",
                codePath
            ));

            log.debug("执行编译和运行命令");
            ProcessBuilder execPb = new ProcessBuilder(execCmd);
            execPb.redirectErrorStream(true);
            Process execProcess = execPb.start();

            // 读取输出（设置超时）
            String output = readStreamWithTimeout(execProcess.getInputStream(), MAX_OUTPUT_SIZE, RUN_TIMEOUT + 2);
            boolean finished = execProcess.waitFor(RUN_TIMEOUT + 2, TimeUnit.SECONDS);

            // 如果进程还在运行，强制停止
            if (!finished) {
                execProcess.destroyForcibly();
                output += "\n[程序执行超时，已强制终止]";
            }

            int exitCode = finished ? execProcess.exitValue() : -1;
            boolean success = finished && exitCode == 0;

            return new ExecutionResult(success, output, "", exitCode);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.systemError("获取容器被中断");
        } catch (Exception e) {
            return ExecutionResult.systemError("执行失败: " + e.getMessage());
        } finally {
            // 4. 归还容器到池中（会自动清理）
            if (container != null) {
                containerPool.releaseContainer(container);
            }
        }
    }


    private String readStream(InputStream is, int maxSize) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = is.read(chunk)) != -1) {
            if (buffer.size() + n > maxSize) {
                throw new IOException("输出超过最大限制（1MB）");
            }
            buffer.write(chunk, 0, n);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * 带超时的流读取方法
     */
    private String readStreamWithTimeout(InputStream is, int maxSize, int timeoutSeconds) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        
        while (true) {
            // 检查是否超时
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                break;
            }
            
            // 检查是否有可用数据（非阻塞）
            if (is.available() > 0) {
                int n = is.read(chunk);
                if (n == -1) {
                    break;
                }
                if (buffer.size() + n > maxSize) {
                    throw new IOException("输出超过最大限制（1MB）");
                }
                buffer.write(chunk, 0, n);
            } else {
                // 没有数据时短暂休眠，避免 CPU 占用过高
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Async("compileExecutor")
    public void saveExecutionRecordAsync(Long userId, String title, String code, String output, boolean success, int exitCode) {
        try {
            if (userId != null) {
                ExecutionRecord record = new ExecutionRecord();
                User u = new User();
                u.setId(userId);
                record.setUser(u);
                record.setTitle(title);
                record.setCode(code);
                record.setOutput(output);
                record.setError(success ? "" : output);
                record.setExitCode(exitCode);
                recordRepository.save(record);
            }
        } catch (Exception e) {
            // 记录保存失败不应该影响主要的代码执行流程
            e.printStackTrace();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecutionResult {
        private boolean success;
        private String output;
        private String error;
        private int exitCode;

        public static ExecutionResult compilationError(String error) {
            return new ExecutionResult(false, "", error, -1);
        }

        public static ExecutionResult systemError(String error) {
            return new ExecutionResult(false, "", error, -2);
        }
    }
}

