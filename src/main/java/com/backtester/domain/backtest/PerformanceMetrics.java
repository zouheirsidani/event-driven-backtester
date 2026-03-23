package com.backtester.domain.backtest;

import java.math.BigDecimal;

/**
 * Aggregate performance statistics for a completed backtest.
 * Computed by {@code MetricsCalculator} from portfolio snapshots and fill history.
 * All rates and ratios are expressed as decimals (e.g. 0.15 means 15%).
 *
 * @param totalReturn           Simple total return: (finalEquity - initialCash) / initialCash.
 * @param annualizedReturn      Total return compounded to a 252-trading-day year.
 * @param annualizedVolatility  Standard deviation of daily returns scaled by √252.
 * @param sharpeRatio           Annualized return / annualized volatility (risk-free rate = 0).
 * @param maxDrawdown           Peak-to-trough decline as a positive decimal (e.g. 0.20 = 20% drawdown).
 * @param winRate               Fraction of closed round-trips that were profitable.
 * @param avgWin                Average dollar profit on winning trades.
 * @param avgLoss               Average dollar loss on losing trades (typically negative).
 * @param profitFactor          Total gross profit / total gross loss; {@code null} or 0 if no losses.
 * @param totalTrades           Number of completed round-trip trades (BUY matched to SELL).
 * @param alpha                 CAPM alpha vs benchmark (annualised); null if no benchmark provided.
 * @param beta                  CAPM beta vs benchmark; null if no benchmark provided.
 */
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

    /**
     * Returns a zero-valued {@code PerformanceMetrics} used when the backtest
     * produced no snapshots (e.g. no bar data found for the requested period).
     *
     * @return Metrics record with all fields set to zero / null.
     */
    public static PerformanceMetrics empty() {
        return new PerformanceMetrics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, null, null
        );
    }
}
