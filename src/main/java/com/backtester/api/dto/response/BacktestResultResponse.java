package com.backtester.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BacktestResultResponse(
        UUID runId,
        PerformanceMetricsDto metrics,
        List<EquityCurvePointDto> equityCurve,
        List<TradeDto> trades,
        Instant completedAt
) {}
