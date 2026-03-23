package com.backtester.api.dto.response;

/**
 * API response DTO describing a single trading strategy.
 *
 * @param strategyId  Machine-readable identifier (used in {@code RunBacktestRequest.strategyId}).
 * @param displayName Human-readable label shown in the UI drop-down.
 */
public record StrategyDto(String strategyId, String displayName) {}
