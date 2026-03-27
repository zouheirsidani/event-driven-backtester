package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Request body for {@code POST /api/v1/user-strategies}.
 *
 * @param name           Human-readable label for this template.
 * @param baseStrategyId The registered strategy type (e.g. "MOMENTUM_V1").
 * @param parameters     Key-value parameter overrides (e.g. {@code {"lookbackDays": 30}});
 *                       may be null or empty.
 */
public record CreateUserStrategyRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "baseStrategyId is required") String baseStrategyId,
        Map<String, Object> parameters
) {}
