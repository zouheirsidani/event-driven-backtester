package com.backtester.infrastructure.persistence.repository;

import com.backtester.infrastructure.persistence.entity.SymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SymbolJpaRepository extends JpaRepository<SymbolEntity, String> {}
