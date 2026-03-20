package com.backtester.infrastructure.persistence.repository;

import com.backtester.infrastructure.persistence.entity.BacktestRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BacktestRunJpaRepository extends JpaRepository<BacktestRunEntity, UUID> {}
