package com.backtester.domain.backtest;

import com.backtester.domain.order.Fill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable result of a completed backtest simulation, produced by
 * {@code BacktestExecutor} and persisted to the database as JSONB.
 *
 * @param runId        Matches the corresponding {@link BacktestRun#runId()}.
 * @param metrics      Aggregate performance statistics computed by {@code MetricsCalculator}.
 * @param equityCurve  Daily portfolio equity values for charting.
 * @param trades       Complete list of fills executed during the simulation.
 * @param completedAt  Wall-clock time the result was stored.
 */
public record BacktestResult(
        UUID runId,
        PerformanceMetrics metrics,
        List<EquityCurvePoint> equityCurve,
        List<Fill> trades,
        Instant completedAt
) {}
