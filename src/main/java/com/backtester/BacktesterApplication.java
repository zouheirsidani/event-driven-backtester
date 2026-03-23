package com.backtester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Event-Driven Trading Strategy Backtester Spring Boot application.
 *
 * <p>{@code @EnableAsync} activates Spring's async task execution support so that
 * {@code @Async} methods on {@code BacktestExecutor} run on the {@code backtestThreadPool}
 * rather than the caller's thread.
 */
@SpringBootApplication
@EnableAsync
public class BacktesterApplication {

    /**
     * Bootstraps the Spring application context and starts the embedded web server.
     *
     * @param args Command-line arguments (passed through to Spring Boot).
     */
    public static void main(String[] args) {
        SpringApplication.run(BacktesterApplication.class, args);
    }
}
