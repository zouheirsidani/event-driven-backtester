package com.backtester.infrastructure.persistence.repository;

import com.backtester.infrastructure.persistence.entity.UserStrategyDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data JPA repository for user strategy definition entities. */
public interface UserStrategyDefinitionJpaRepository extends JpaRepository<UserStrategyDefinitionEntity, UUID> {}
