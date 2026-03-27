package com.backtester.api.mapper;

import com.backtester.api.dto.response.UserStrategyDto;
import com.backtester.domain.strategy.UserStrategyDefinition;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link UserStrategyDefinition} domain records and {@link UserStrategyDto} API DTOs.
 */
@Component
public class UserStrategyDtoMapper {

    /**
     * Converts a domain record to an API DTO.
     *
     * @param d The domain record to convert.
     * @return Equivalent API DTO.
     */
    public UserStrategyDto toDto(UserStrategyDefinition d) {
        return new UserStrategyDto(
                d.id().toString(),
                d.name(),
                d.baseStrategyId(),
                d.parameters(),
                d.createdAt().toString()
        );
    }
}
