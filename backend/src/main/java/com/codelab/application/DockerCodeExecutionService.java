package com.codelab.application;

import com.codelab.domain.ExecutionRecord;
import com.codelab.domain.User;
import com.codelab.domain.repository.ExecutionRecordRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
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
 * 使用Docker沙箱执行C代码的服务
 */
@Service
@RequiredArgsConstructor
public class DockerCodeExecutionService {

    private final ExecutionRecordRepository recordRepository;

    @Value("${code.tempDir}")
    private String tempDir;

    @Value("${code.sandboxImage:c-codelab-sandbox:latest}")
    private String sandboxImage;

    @Value("${code.dockerTimeout:15}")
    private int dockerTimeout;

    private static final int RUN_TIMEOUT = 5; // seconds
    private static final int MAX_OUTPUT_SIZE = 1024 * 1024; // 1MB

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
     * 在Docker容器中执行代码
     */
    private ExecutionResult executeInDocker(Path codeFile, Path outputFile) throws IOException, InterruptedException {
        String containerName = "codelab_" + UUID.randomUUID().toString().substring(0, 8);
        String codePath = "/app/code/" + codeFile.getFileName().toString();

        try {
            // 1. 创建容器（不启动）
            List<String> createCmd = new ArrayList<>();
            createCmd.add("docker");
            createCmd.add("create");
            createCmd.add("--name");
            createCmd.add(containerName);
            createCmd.add("--network");
            createCmd.add("none"); // 网络隔离
            createCmd.add("--memory");
            createCmd.add("128m"); // 内存限制128MB
            createCmd.add("--cpus");
            createCmd.add("0.5"); // CPU限制0.5核
            createCmd.add("--pids-limit");
            createCmd.add("10"); // 进程数限制
            createCmd.add("--read-only"); // 只读根文件系统
            createCmd.add("--tmpfs");
            createCmd.add("/tmp:rw,noexec,nosuid,size=50m"); // 临时文件系统
            createCmd.add("--tmpfs");
            createCmd.add("/app:rw,noexec,nosuid,size=50m");
            createCmd.add(sandboxImage);
            createCmd.add("/bin/bash");
            createCmd.add("-c");
            // 编译和执行命令
            createCmd.add(String.format(
                "cd /app/code && gcc -o program %s -Wall -Wextra 2>&1 && timeout %ds ./program 2>&1 || exit $?",
                codePath, RUN_TIMEOUT
            ));

            ProcessBuilder createPb = new ProcessBuilder(createCmd);
            createPb.redirectErrorStream(true);
            Process createProcess = createPb.start();
            String createOutput = readStream(createProcess.getInputStream(), MAX_OUTPUT_SIZE);
            boolean createFinished = createProcess.waitFor(5, TimeUnit.SECONDS);
            
            if (!createFinished || createProcess.exitValue() != 0) {
                return ExecutionResult.systemError("无法创建Docker容器: " + createOutput);
            }

            // 2. 复制代码文件到容器
            List<String> cpCmd = new ArrayList<>();
            cpCmd.add("docker");
            cpCmd.add("cp");
            cpCmd.add(codeFile.toString());
            cpCmd.add(containerName + ":" + codePath);

            ProcessBuilder cpPb = new ProcessBuilder(cpCmd);
            cpPb.redirectErrorStream(true);
            Process cpProcess = cpPb.start();
            String cpOutput = readStream(cpProcess.getInputStream(), MAX_OUTPUT_SIZE);
            boolean cpFinished = cpProcess.waitFor(5, TimeUnit.SECONDS);
            
            if (!cpFinished || cpProcess.exitValue() != 0) {
                cleanupContainer(containerName);
                return ExecutionResult.systemError("无法复制代码文件到容器: " + cpOutput);
            }

            // 3. 启动容器并执行
            List<String> startCmd = new ArrayList<>();
            startCmd.add("docker");
            startCmd.add("start");
            startCmd.add("-a"); // 附加输出
            startCmd.add(containerName);

            ProcessBuilder startPb = new ProcessBuilder(startCmd);
            startPb.redirectErrorStream(true);
            Process startProcess = startPb.start();
            
            // 读取输出
            String output = readStream(startProcess.getInputStream(), MAX_OUTPUT_SIZE);
            boolean finished = startProcess.waitFor(dockerTimeout, TimeUnit.SECONDS);
            
            int exitCode = finished ? startProcess.exitValue() : -1;
            boolean success = finished && exitCode == 0;

            // 4. 清理容器
            cleanupContainer(containerName);

            return new ExecutionResult(success, output, "", exitCode);

        } catch (Exception e) {
            // 确保清理容器
            cleanupContainer(containerName);
            throw e;
        }
    }

    /**
     * 清理Docker容器
     */
    private void cleanupContainer(String containerName) {
        try {
            // 停止容器（如果还在运行）
            ProcessBuilder stopPb = new ProcessBuilder("docker", "stop", containerName);
            Process stopProcess = stopPb.start();
            stopProcess.waitFor(2, TimeUnit.SECONDS);

            // 删除容器
            ProcessBuilder rmPb = new ProcessBuilder("docker", "rm", containerName);
            Process rmProcess = rmPb.start();
            rmProcess.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 忽略清理错误
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

