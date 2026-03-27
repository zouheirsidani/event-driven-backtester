package com.backtester.api.dto.response;

import java.util.Map;

/**
 * DTO for a single user-defined strategy template.
 *
 * @param id             Template UUID string.
 * @param name           Human-readable label.
 * @param baseStrategyId Base strategy type identifier.
 * @param parameters     Parameter override map.
 * @param createdAt      ISO 8601 creation timestamp.
 */
public record UserStrategyDto(
        String id,
        String name,
        String baseStrategyId,
        Map<String, Object> parameters,
        String createdAt
) {}
