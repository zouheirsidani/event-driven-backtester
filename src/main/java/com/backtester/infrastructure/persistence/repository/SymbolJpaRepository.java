package com.backtester.infrastructure.persistence.repository;

import com.backtester.infrastructure.persistence.entity.SymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link SymbolEntity}.
 * The primary key is the ticker string, so {@code findById(ticker)} and
 * {@code existsById(ticker)} serve as the lookup and existence-check operations.
 * All other CRUD methods are inherited from {@link JpaRepository}.
 */
public interface SymbolJpaRepository extends JpaRepository<SymbolEntity, String> {}
