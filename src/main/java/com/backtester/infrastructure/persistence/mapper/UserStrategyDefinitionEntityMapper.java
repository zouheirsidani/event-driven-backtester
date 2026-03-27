package com.backtester.infrastructure.persistence.mapper;

import com.backtester.domain.strategy.UserStrategyDefinition;
import com.backtester.infrastructure.persistence.entity.UserStrategyDefinitionEntity;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link UserStrategyDefinition} domain records and
 * {@link UserStrategyDefinitionEntity} JPA entities.
 */
@Component
public class UserStrategyDefinitionEntityMapper {

    /**
     * Maps a domain record to a JPA entity.
     *
     * @param d The domain record.
     * @return Equivalent entity ready for persistence.
     */
    public UserStrategyDefinitionEntity toEntity(UserStrategyDefinition d) {
        return new UserStrategyDefinitionEntity(d.id(), d.name(), d.baseStrategyId(),
                d.parameters(), d.createdAt());
    }

    /**
     * Maps a JPA entity to a domain record.
     *
     * @param e The entity loaded from the database.
     * @return Equivalent immutable domain record.
     */
    public UserStrategyDefinition toDomain(UserStrategyDefinitionEntity e) {
        return new UserStrategyDefinition(e.getId(), e.getName(), e.getBaseStrategyId(),
                e.getParameters(), e.getCreatedAt());
    }
}
