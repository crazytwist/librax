package com.librax.lab.module.lab.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Lab 模块线程池配置
 * <p>
 * 专供 @Async 事件监听器使用（如 {@code SampleFlowIntegrationListener}）
 * 避免使用默认的 SimpleAsyncTaskExecutor（每次创建新线程）
 */
@Configuration
@EnableAsync
public class LabThreadPoolConfig {

    /**
     * 事件监听异步处理线程池
     * <p>
     * 用于样本-流程集成监听器等 @Async 事件处理器
     * 核心线程数：CPU 核心数（事件处理轻量，以日志和 DB 更新为主）
     * 队列容量：1000（有界队列，防止事件堆积导致 OOM）
     * 拒绝策略：CallerRunsPolicy（队列满时由发布事件的线程处理，起到背压作用）
     */
    @Primary
    @Bean("labEventListenerExecutor")
    public Executor LabEventListenerExecutor() {
        int core = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(core * 2);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("lab-event-listener-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
