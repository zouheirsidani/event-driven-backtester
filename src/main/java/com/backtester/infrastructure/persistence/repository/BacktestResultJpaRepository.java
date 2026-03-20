package com.backtester.infrastructure.persistence.repository;

import com.backtester.infrastructure.persistence.entity.BacktestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BacktestResultJpaRepository extends JpaRepository<BacktestResultEntity, UUID> {}
