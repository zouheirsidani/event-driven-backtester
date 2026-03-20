package com.backtester.api.dto.response;

import java.math.BigDecimal;

public record PerformanceMetricsDto(
        BigDecimal totalReturn,
        BigDecimal annualizedReturn,
        BigDecimal annualizedVolatility,
        BigDecimal sharpeRatio,
        BigDecimal maxDrawdown,
        BigDecimal winRate,
        BigDecimal avgWin,
        BigDecimal avgLoss,
        BigDecimal profitFactor,
        int totalTrades,
        BigDecimal alpha,
        BigDecimal beta
) {}
