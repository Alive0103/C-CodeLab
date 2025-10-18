package com.codelab.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import java.util.concurrent.Executor;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableAsync
public class AsyncConfig {

    @PostConstruct
    public void init() {
        // 设置安全上下文策略为可继承的线程本地变量
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean(name = "compileExecutor")
    public Executor compileExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(3);
        ex.setMaxPoolSize(10);
        ex.setThreadNamePrefix("compile-");
        ex.initialize();
        // 使用Spring Security的异步任务执行器，确保安全上下文传播
        return new DelegatingSecurityContextAsyncTaskExecutor(ex);
    }

    @Bean(name = "runExecutor")
    public Executor runExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(5);
        ex.setMaxPoolSize(20);
        ex.setThreadNamePrefix("run-");
        ex.initialize();
        // 使用Spring Security的异步任务执行器，确保安全上下文传播
        return new DelegatingSecurityContextAsyncTaskExecutor(ex);
    }

    @Bean(name = "websocketExecutor")
    public Executor websocketExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(5);
        ex.setThreadNamePrefix("ws-");
        ex.initialize();
        // 使用Spring Security的异步任务执行器，确保安全上下文传播
        return new DelegatingSecurityContextAsyncTaskExecutor(ex);
    }
}


