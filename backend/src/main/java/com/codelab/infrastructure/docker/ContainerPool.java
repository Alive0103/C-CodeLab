package com.codelab.infrastructure.docker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Docker 容器池管理器
 * 管理长期运行的容器，用于代码执行
 */
@Slf4j
@Component
public class ContainerPool {

    @Value("${code.sandboxImage:c-codelab-sandbox:latest}")
    private String sandboxImage;

    @Value("${code.containerPool.size:5}")
    private int poolSize;

    @Value("${code.containerPool.maxIdleTime:300}")
    private int maxIdleTimeSeconds; // 容器最大空闲时间（秒）

    private final BlockingQueue<ContainerInfo> availableContainers = new LinkedBlockingQueue<>();
    private final List<ContainerInfo> allContainers = new ArrayList<>();
    private final AtomicInteger containerCounter = new AtomicInteger(0);
    private volatile boolean shutdown = false;

    /**
     * 容器信息
     */
    public static class ContainerInfo {
        private final String name;
        private final long createdAt;
        private volatile long lastUsedAt;
        private volatile boolean inUse;

        public ContainerInfo(String name) {
            this.name = name;
            this.createdAt = System.currentTimeMillis();
            this.lastUsedAt = System.currentTimeMillis();
            this.inUse = false;
        }

        public String getName() {
            return name;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getLastUsedAt() {
            return lastUsedAt;
        }

        public void updateLastUsedAt() {
            this.lastUsedAt = System.currentTimeMillis();
        }

        public boolean isInUse() {
            return inUse;
        }

        public void setInUse(boolean inUse) {
            this.inUse = inUse;
        }

        public boolean isIdleTooLong(int maxIdleSeconds) {
            return !inUse && (System.currentTimeMillis() - lastUsedAt) > maxIdleSeconds * 1000L;
        }
    }

    /**
     * 初始化容器池
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化容器池，大小: {}", poolSize);
        for (int i = 0; i < poolSize; i++) {
            try {
                ContainerInfo container = createContainer();
                if (container != null) {
                    availableContainers.offer(container);
                    allContainers.add(container);
                }
            } catch (Exception e) {
                log.error("创建容器失败: {}", e.getMessage(), e);
            }
        }
        log.info("容器池初始化完成，可用容器数: {}", availableContainers.size());

        // 启动清理线程
        startCleanupThread();
    }

    /**
     * 创建新容器
     */
    private ContainerInfo createContainer() throws IOException, InterruptedException {
        int id = containerCounter.incrementAndGet();
        String containerName = "codelab_pool_" + id;

        List<String> createCmd = new ArrayList<>();
        createCmd.add("docker");
        createCmd.add("create");
        createCmd.add("--name");
        createCmd.add(containerName);
        createCmd.add("--network");
        createCmd.add("none");
        createCmd.add("--memory");
        createCmd.add("128m");
        createCmd.add("--cpus");
        createCmd.add("0.5");
        createCmd.add("--pids-limit");
        createCmd.add("10");
        createCmd.add("--read-only");
        createCmd.add("--tmpfs");
        createCmd.add("/tmp:rw,noexec,nosuid,size=50m");
        createCmd.add("--tmpfs");
        createCmd.add("/app:rw,exec,size=50m"); // 添加 exec 选项以允许执行文件
        createCmd.add(sandboxImage);
        createCmd.add("tail");
        createCmd.add("-f");
        createCmd.add("/dev/null"); // 保持容器运行

        ProcessBuilder pb = new ProcessBuilder(createCmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readStream(process.getInputStream(), 1024);
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);

        if (!finished || process.exitValue() != 0) {
            log.error("创建容器失败: {}", output);
            return null;
        }

        // 启动容器
        List<String> startCmd = new ArrayList<>();
        startCmd.add("docker");
        startCmd.add("start");
        startCmd.add(containerName);

        ProcessBuilder startPb = new ProcessBuilder(startCmd);
        Process startProcess = startPb.start();
        boolean started = startProcess.waitFor(3, TimeUnit.SECONDS);

        if (!started || startProcess.exitValue() != 0) {
            log.error("启动容器失败: {}", containerName);
            cleanupContainer(containerName);
            return null;
        }

        // 初始化容器环境
        initializeContainer(containerName);

        log.info("容器创建成功: {}", containerName);
        return new ContainerInfo(containerName);
    }

    /**
     * 初始化容器环境
     */
    private void initializeContainer(String containerName) {
        try {
            List<String> initCmd = new ArrayList<>();
            initCmd.add("docker");
            initCmd.add("exec");
            initCmd.add(containerName);
            initCmd.add("/bin/bash");
            initCmd.add("-c");
            initCmd.add("mkdir -p /app/code && chmod 777 /app && chmod 777 /app/code");

            ProcessBuilder pb = new ProcessBuilder(initCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            readStream(process.getInputStream(), 1024);
            process.waitFor(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("初始化容器环境失败: {}", e.getMessage());
        }
    }

    /**
     * 从池中获取容器（阻塞等待）
     */
    public ContainerInfo acquireContainer(long timeout, TimeUnit unit) throws InterruptedException {
        if (shutdown) {
            throw new IllegalStateException("容器池已关闭");
        }

        ContainerInfo container = availableContainers.poll(timeout, unit);
        if (container != null) {
            container.setInUse(true);
            container.updateLastUsedAt();
            log.debug("获取容器: {}", container.getName());
        }
        return container;
    }

    /**
     * 归还容器到池中
     */
    public void releaseContainer(ContainerInfo container) {
        if (container == null || shutdown) {
            return;
        }

        // 清理容器
        cleanupContainerFiles(container.getName());

        container.setInUse(false);
        container.updateLastUsedAt();
        availableContainers.offer(container);
        log.debug("归还容器: {}", container.getName());
    }

    /**
     * 清理容器内的文件
     */
    private void cleanupContainerFiles(String containerName) {
        try {
            List<String> cleanupCmd = new ArrayList<>();
            cleanupCmd.add("docker");
            cleanupCmd.add("exec");
            cleanupCmd.add(containerName);
            cleanupCmd.add("/bin/bash");
            cleanupCmd.add("-c");
            cleanupCmd.add("rm -rf /app/code/* && mkdir -p /app/code && chmod 777 /app/code");

            ProcessBuilder pb = new ProcessBuilder(cleanupCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            readStream(process.getInputStream(), 1024);
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("清理容器文件失败: {}", e.getMessage());
        }
    }

    /**
     * 启动清理线程（定期清理空闲过久的容器）
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!shutdown) {
                try {
                    Thread.sleep(60000); // 每分钟检查一次
                    cleanupIdleContainers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("ContainerPool-Cleanup");
        cleanupThread.start();
    }

    /**
     * 清理空闲过久的容器
     */
    private void cleanupIdleContainers() {
        synchronized (allContainers) {
            List<ContainerInfo> toRemove = new ArrayList<>();
            for (ContainerInfo container : allContainers) {
                if (container.isIdleTooLong(maxIdleTimeSeconds) && !container.isInUse()) {
                    toRemove.add(container);
                }
            }

            for (ContainerInfo container : toRemove) {
                log.info("清理空闲容器: {}", container.getName());
                availableContainers.remove(container);
                allContainers.remove(container);
                cleanupContainer(container.getName());

                // 创建新容器补充
                try {
                    ContainerInfo newContainer = createContainer();
                    if (newContainer != null) {
                        availableContainers.offer(newContainer);
                        allContainers.add(newContainer);
                    }
                } catch (Exception e) {
                    log.error("补充容器失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 检查容器健康状态
     */
    public boolean isContainerHealthy(String containerName) {
        try {
            List<String> checkCmd = new ArrayList<>();
            checkCmd.add("docker");
            checkCmd.add("ps");
            checkCmd.add("--filter");
            checkCmd.add("name=" + containerName);
            checkCmd.add("--format");
            checkCmd.add("{{.Names}}");

            ProcessBuilder pb = new ProcessBuilder(checkCmd);
            Process process = pb.start();
            String output = readStream(process.getInputStream(), 1024);
            process.waitFor(2, TimeUnit.SECONDS);
            return output.trim().equals(containerName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清理容器
     */
    private void cleanupContainer(String containerName) {
        try {
            ProcessBuilder stopPb = new ProcessBuilder("docker", "stop", containerName);
            Process stopProcess = stopPb.start();
            stopProcess.waitFor(2, TimeUnit.SECONDS);

            ProcessBuilder rmPb = new ProcessBuilder("docker", "rm", containerName);
            Process rmProcess = rmPb.start();
            rmProcess.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("清理容器失败: {}", e.getMessage());
        }
    }

    /**
     * 关闭容器池
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭容器池...");
        shutdown = true;

        synchronized (allContainers) {
            for (ContainerInfo container : allContainers) {
                cleanupContainer(container.getName());
            }
            allContainers.clear();
            availableContainers.clear();
        }

        log.info("容器池已关闭");
    }

    /**
     * 获取池状态
     */
    public PoolStatus getStatus() {
        int available = availableContainers.size();
        int total = allContainers.size();
        int inUse = (int) allContainers.stream().filter(ContainerInfo::isInUse).count();
        return new PoolStatus(total, available, inUse);
    }

    public static class PoolStatus {
        private final int total;
        private final int available;
        private final int inUse;

        public PoolStatus(int total, int available, int inUse) {
            this.total = total;
            this.available = available;
            this.inUse = inUse;
        }

        public int getTotal() {
            return total;
        }

        public int getAvailable() {
            return available;
        }

        public int getInUse() {
            return inUse;
        }
    }

    private String readStream(InputStream is, int maxSize) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = is.read(chunk)) != -1) {
            if (buffer.size() + n > maxSize) {
                break;
            }
            buffer.write(chunk, 0, n);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}

