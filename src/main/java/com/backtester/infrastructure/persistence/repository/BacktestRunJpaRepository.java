package com.backtester.infrastructure.persistence.repository;

import com.backtester.infrastructure.persistence.entity.BacktestRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link BacktestRunEntity}.
 * The primary key is the UUID {@code run_id} generated at submission time.
 * Pagination is handled by the inherited {@code findAll(Pageable)} overload.
 */
public interface BacktestRunJpaRepository extends JpaRepository<BacktestRunEntity, UUID> {}
