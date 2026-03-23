package com.backtester.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA and transaction management configuration.
 * Explicitly scopes Spring Data repository scanning to the
 * {@code infrastructure.persistence.repository} package so that no other
 * packages are accidentally picked up as JPA repositories.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.backtester.infrastructure.persistence.repository")
public class JpaConfig {}
