package com.backtester.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API response DTO for a completed backtest result.
 * Returned by {@code GET /api/v1/backtests/{runId}/results}.
 *
 * @param runId        UUID matching the backtest run.
 * @param metrics      Aggregate performance metrics.
 * @param equityCurve  Daily equity values for charting.
 * @param trades       All executed fills for the simulation.
 * @param completedAt  Timestamp when the result was stored.
 */
public record BacktestResultResponse(
        UUID runId,
        PerformanceMetricsDto metrics,
        List<EquityCurvePointDto> equityCurve,
        List<TradeDto> trades,
        Instant completedAt
) {}
