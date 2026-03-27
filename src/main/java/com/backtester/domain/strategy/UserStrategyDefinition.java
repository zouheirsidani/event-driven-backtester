package com.backtester.domain.strategy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable record representing a user-saved strategy configuration.
 * Stores a human-readable name, the base strategy type to use, and the
 * parameter overrides to apply when running a backtest with this template.
 *
 * @param id             Unique identifier.
 * @param name           User-given display name (e.g. "My Aggressive Momentum").
 * @param baseStrategyId The strategy type to instantiate (must match a registered Strategy.strategyId()).
 * @param parameters     Parameter overrides passed to Strategy.withParameters() at execution time.
 * @param createdAt      Wall-clock creation time.
 */
public record UserStrategyDefinition(
        UUID id,
        String name,
        String baseStrategyId,
        Map<String, Object> parameters,
        Instant createdAt
) {}
