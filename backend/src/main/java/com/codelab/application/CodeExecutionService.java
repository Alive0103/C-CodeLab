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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private final ExecutionRecordRepository recordRepository;

    @Value("${code.tempDir}")
    private String tempDir;

    @Value("${code.gccPath:gcc}")
    private String gccPath;

    private static final int COMPILE_TIMEOUT = 10; // seconds
    private static final int RUN_TIMEOUT = 5; // seconds
    private static final int MAX_OUTPUT_SIZE = 1024 * 1024; // 1MB

    public ExecutionResult compileAndRun(String code, Long userId, String title) {
        try {
            if (code == null || code.length() > 10 * 1024) {
                return ExecutionResult.systemError("代码长度超过限制（最大10KB）");
            }
            Files.createDirectories(Paths.get(tempDir));
            String randomId = UUID.randomUUID().toString().substring(0, 8);
            Path src = Paths.get(tempDir, "code_" + randomId + ".c");
            Path exe = Paths.get(tempDir, "program_" + randomId + (isWindows() ? ".exe" : ""));
            Files.write(src, code.getBytes(StandardCharsets.UTF_8));

            CompilationResult cr = compileCode(src, exe);
            if (!cr.success) {
                return ExecutionResult.compilationError(cr.error);
            }

            ExecutionResult rr = runProgram(exe);

            if (userId != null) {
                ExecutionRecord record = new ExecutionRecord();
                User u = new User();
                u.setId(userId);
                record.setUser(u);
                record.setTitle(title);
                record.setCode(code);
                record.setOutput(rr.output);
                record.setError(rr.success ? "" : rr.output);
                record.setExitCode(rr.success ? 0 : 1);
                recordRepository.save(record);
            }

            return rr;
        } catch (Exception e) {
            return ExecutionResult.systemError("执行失败：" + e.getMessage());
        }
    }

    private CompilationResult compileCode(Path src, Path exe) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(gccPath);
        cmd.add("-o");
        cmd.add(exe.toString());
        cmd.add(src.toString());
        cmd.add("-Wall");
        cmd.add("-Wextra");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = readStream(p.getInputStream(), MAX_OUTPUT_SIZE);
        boolean finished = p.waitFor(COMPILE_TIMEOUT, TimeUnit.SECONDS);
        boolean success = finished && p.exitValue() == 0;
        return new CompilationResult(success, out);
    }

    private ExecutionResult runProgram(Path exe) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(exe.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = readStream(p.getInputStream(), MAX_OUTPUT_SIZE);
        boolean finished = p.waitFor(RUN_TIMEOUT, TimeUnit.SECONDS);
        boolean success = finished && p.exitValue() == 0;
        return new ExecutionResult(success, out, "", success ? 0 : 1);
    }

    private String readStream(InputStream is, int maxSize) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = is.read(chunk)) != -1) {
            if (buffer.size() + n > maxSize) throw new IOException("输出超过最大限制（1MB）");
            buffer.write(chunk, 0, n);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    @Data
    @AllArgsConstructor
    private static class CompilationResult {
        private boolean success;
        private String error;
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


