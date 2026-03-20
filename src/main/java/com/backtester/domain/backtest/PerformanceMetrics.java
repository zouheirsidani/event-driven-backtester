package com.backtester.domain.backtest;

import java.math.BigDecimal;

public record PerformanceMetrics(
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
) {

    public static PerformanceMetrics empty() {
        return new PerformanceMetrics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, null, null
        );
    }
}
