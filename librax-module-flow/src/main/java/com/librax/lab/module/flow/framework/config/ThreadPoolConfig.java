package com.librax.lab.module.flow.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    /**
     * 步骤执行线程池
     * 核心线程数：CPU 核心数，步骤执行以 IO 等待为主，可适当调大
     * 最大线程数：根据最大并发流程数 × 平均并行步骤数估算
     * 队列：有界队列，防止内存溢出，满了触发 CallerRunsPolicy 让调用线程执行
     */
    @Bean("stepExecutorPool")
    public ThreadPoolExecutor stepExecutorPool() {
        int core = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                core,
                core * 4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new ThreadFactoryBuilder("step-executor-%d"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Watchdog 扫描线程池，单线程足够
     */
    @Bean("watchdogPool")
    public ScheduledExecutorService watchdogPool() {
        return Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "step-watchdog")
        );
    }

    // 简单的 ThreadFactory 构建器，避免引入额外依赖
    private static ThreadFactory threadFactoryBuilder(String nameFormat) {
        return r -> {
            Thread t = new Thread(r);
            t.setName(String.format(nameFormat, t.getId()));
            t.setDaemon(false);
            return t;
        };
    }

    private static class ThreadFactoryBuilder implements ThreadFactory {
        private final String nameFormat;
        private int count = 0;

        ThreadFactoryBuilder(String nameFormat) {
            this.nameFormat = nameFormat;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, String.format(nameFormat, ++count));
            t.setDaemon(false);
            return t;
        }
    }
}