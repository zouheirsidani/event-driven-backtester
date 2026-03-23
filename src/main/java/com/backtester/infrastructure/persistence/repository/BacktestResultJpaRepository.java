package com.backtester.infrastructure.persistence.repository;

import com.backtester.infrastructure.persistence.entity.BacktestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link BacktestResultEntity}.
 * The primary key is the same UUID as the corresponding {@code backtest_runs} record.
 * Lookup by run ID uses the inherited {@code findById(UUID)} method.
 */
public interface BacktestResultJpaRepository extends JpaRepository<BacktestResultEntity, UUID> {}
