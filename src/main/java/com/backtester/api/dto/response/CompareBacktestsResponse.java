package com.backtester.api.dto.response;

import java.util.List;

/**
 * Response for {@code POST /api/v1/backtests/compare}.
 * Wraps the results of multiple backtest runs for side-by-side comparison.
 *
 * @param results Ordered list of result responses, one per requested run ID
 *                (runs without results yet are omitted).
 */
public record CompareBacktestsResponse(List<BacktestResultResponse> results) {}
