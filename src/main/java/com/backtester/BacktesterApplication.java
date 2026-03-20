package com.backtester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BacktesterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BacktesterApplication.class, args);
    }
}
