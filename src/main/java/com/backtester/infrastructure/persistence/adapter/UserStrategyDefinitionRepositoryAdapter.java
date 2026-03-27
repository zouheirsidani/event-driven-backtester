package com.backtester.infrastructure.persistence.adapter;

import com.backtester.application.port.UserStrategyDefinitionRepository;
import com.backtester.domain.strategy.UserStrategyDefinition;
import com.backtester.infrastructure.persistence.mapper.UserStrategyDefinitionEntityMapper;
import com.backtester.infrastructure.persistence.repository.UserStrategyDefinitionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapts the {@link UserStrategyDefinitionJpaRepository} JPA repository to the
 * {@link UserStrategyDefinitionRepository} port used by application services.
 */
@Component
public class UserStrategyDefinitionRepositoryAdapter implements UserStrategyDefinitionRepository {

    private final UserStrategyDefinitionJpaRepository jpa;
    private final UserStrategyDefinitionEntityMapper mapper;

    /**
     * @param jpa    Spring Data JPA repository for the underlying table.
     * @param mapper Mapper between domain records and JPA entities.
     */
    public UserStrategyDefinitionRepositoryAdapter(UserStrategyDefinitionJpaRepository jpa,
                                                    UserStrategyDefinitionEntityMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserStrategyDefinition save(UserStrategyDefinition definition) {
        return mapper.toDomain(jpa.save(mapper.toEntity(definition)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserStrategyDefinition> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserStrategyDefinition> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
