package com.backtester.api.dto.response;

import java.util.List;

/**
 * Response wrapper for {@code GET /api/v1/strategies}.
 *
 * @param strategies List of all registered strategy descriptors.
 */
public record StrategiesResponse(List<StrategyDto> strategies) {}
