package com.backtester.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Spring configuration for asynchronous task execution.
 * Declares the {@code backtestThreadPool} executor used by {@link com.backtester.application.backtest.BacktestExecutor}.
 *
 * <p>The bean is named {@code backtestThreadPool} (not {@code backtestExecutor}) to
 * avoid a Spring bean name conflict with the {@code BacktestExecutor} component itself,
 * which would otherwise match the default {@code backtestExecutor} name.
 */
@Configuration
public class AsyncConfig {

    /**
     * Creates the thread pool executor for running backtest simulations in the background.
     * Configuration: 2 core threads, up to 5 max threads, queue capacity of 10.
     * Threads are named with the prefix {@code "backtest-"} for easy identification in logs.
     *
     * @return Configured executor bean.
     */
    @Bean(name = "backtestThreadPool")
    public Executor backtestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("backtest-");
        executor.initialize();
        return executor;
    }
}
