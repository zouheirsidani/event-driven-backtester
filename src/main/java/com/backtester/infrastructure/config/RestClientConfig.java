package com.backtester.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the Spring {@link RestClient} bean used for outbound HTTP calls
 * (e.g. Yahoo Finance).
 *
 * <p>The builder is auto-configured by Spring Boot; this class simply materialises
 * it into a named singleton bean so it can be injected into adapters.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a {@link RestClient} bean with sensible defaults for external API calls.
     * Spring Boot's {@link RestClient.Builder} is auto-configured and injected here.
     *
     * @param builder Auto-configured builder provided by Spring Boot.
     * @return Ready-to-use {@link RestClient} instance.
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
