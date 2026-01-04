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

    public ExecutionResult compileAndRun(String code, String input, Long userId, String title) {
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
            ExecutionResult result = executeInDocker(codeFile, outputFile, input);
            
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
     * @param codeFile 代码文件路径
     * @param outputFile 输出文件路径（未使用，保留以兼容）
     * @param input 标准输入内容（可为null）
     */
    private ExecutionResult executeInDocker(Path codeFile, Path outputFile, String input) throws IOException, InterruptedException {
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
            
            // 4. 在容器中编译代码
            List<String> compileCmd = new ArrayList<>();
            compileCmd.add("docker");
            compileCmd.add("exec");
            compileCmd.add(container.getName());
            compileCmd.add("/bin/bash");
            compileCmd.add("-c");
            compileCmd.add(String.format(
                "cd /app/code && " +
                "gcc -o /app/code/program %s -std=c11 -Wall -Wextra -O2 -lm 2>&1",
                codePath
            ));

            log.debug("执行编译命令");
            ProcessBuilder compilePb = new ProcessBuilder(compileCmd);
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            
            String compileOutput = readStream(compileProcess.getInputStream(), MAX_OUTPUT_SIZE);
            boolean compileFinished = compileProcess.waitFor(10, TimeUnit.SECONDS);
            
            if (!compileFinished || compileProcess.exitValue() != 0) {
                // 编译失败，返回编译错误
                int exitCode = compileFinished ? compileProcess.exitValue() : -1;
                return new ExecutionResult(false, "", compileOutput, exitCode);
            }
            
            log.debug("编译成功，开始执行程序");

            // 5. 执行程序（提供标准输入）
            List<String> runCmd = new ArrayList<>();
            runCmd.add("docker");
            runCmd.add("exec");
            runCmd.add("-i"); // 使用 stdin 提供输入
            runCmd.add(container.getName());
            runCmd.add("/bin/bash");
            runCmd.add("-c");
            runCmd.add("cd /app/code && /app/code/program 2>&1 || exit $?");

            log.debug("执行程序命令");
            ProcessBuilder runPb = new ProcessBuilder(runCmd);
            runPb.redirectErrorStream(true);
            Process runProcess = runPb.start();

            // 如果有输入，写入到进程的 stdin
            if (input != null && !input.isEmpty()) {
                try (OutputStream os = runProcess.getOutputStream()) {
                    os.write(input.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close(); // 关闭 stdin，让程序知道输入结束
                }
            }

            // 读取输出（设置超时）
            String output = readStreamWithTimeout(runProcess.getInputStream(), MAX_OUTPUT_SIZE, RUN_TIMEOUT + 2);
            boolean finished = runProcess.waitFor(RUN_TIMEOUT + 2, TimeUnit.SECONDS);

            // 如果进程还在运行，强制停止
            if (!finished) {
                runProcess.destroyForcibly();
                output += "\n[程序执行超时，已强制终止]";
            } else {
                // 程序已结束，尝试读取剩余输出（可能还有缓冲的数据）
                try {
                    Thread.sleep(100); // 等待缓冲区刷新
                    byte[] remaining = new byte[8192];
                    int remainingBytes = runProcess.getInputStream().available();
                    if (remainingBytes > 0) {
                        int n = runProcess.getInputStream().read(remaining, 0, Math.min(remainingBytes, remaining.length));
                        if (n > 0 && output.length() + n <= MAX_OUTPUT_SIZE) {
                            output += new String(remaining, 0, n, StandardCharsets.UTF_8);
                        }
                    }
                } catch (Exception e) {
                    // 忽略读取剩余数据时的错误
                    log.debug("读取剩余输出时出错: {}", e.getMessage());
                }
            }

            int exitCode = finished ? runProcess.exitValue() : -1;
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
     * 使用阻塞读取，但通过超时机制控制总时间
     */
    private String readStreamWithTimeout(InputStream is, int maxSize, int timeoutSeconds) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        long lastDataTime = startTime;
        boolean hasData = false;
        
        while (true) {
            // 检查是否超时（如果已经有数据，给一点额外时间读取剩余数据）
            long elapsed = System.currentTimeMillis() - startTime;
            long sinceLastData = System.currentTimeMillis() - lastDataTime;
            
            // 如果超时且超过500ms没有新数据，则退出
            if (elapsed > timeoutMillis && (sinceLastData > 500 || !hasData)) {
                break;
            }
            
            try {
                // 设置非阻塞读取的超时
                if (is.available() > 0) {
                    int n = is.read(chunk);
                    if (n == -1) {
                        // 流已关闭，继续读取剩余数据
                        break;
                    }
                    if (n > 0) {
                        if (buffer.size() + n > maxSize) {
                            throw new IOException("输出超过最大限制（1MB）");
                        }
                        buffer.write(chunk, 0, n);
                        lastDataTime = System.currentTimeMillis();
                        hasData = true;
                    }
                } else {
                    // 没有可用数据，短暂休眠
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 尝试读取剩余数据（非阻塞）
        try {
            while (is.available() > 0) {
                int n = is.read(chunk);
                if (n == -1 || n == 0) break;
                if (buffer.size() + n > maxSize) {
                    throw new IOException("输出超过最大限制（1MB）");
                }
                buffer.write(chunk, 0, n);
            }
        } catch (IOException e) {
            // 忽略读取剩余数据时的错误
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

