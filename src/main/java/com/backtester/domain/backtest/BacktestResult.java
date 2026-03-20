package com.backtester.domain.backtest;

import com.backtester.domain.order.Fill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BacktestResult(
        UUID runId,
        PerformanceMetrics metrics,
        List<EquityCurvePoint> equityCurve,
        List<Fill> trades,
        Instant completedAt
) {}
