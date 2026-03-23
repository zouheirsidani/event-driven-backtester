package com.backtester.api.dto.response;

import java.math.BigDecimal;

/**
 * API response DTO for the computed performance metrics of a backtest.
 * All rates and ratios are expressed as decimals (0.15 = 15%).
 *
 * @param totalReturn          Simple total return over the simulation period.
 * @param annualizedReturn     Total return compounded to a 252-trading-day year.
 * @param annualizedVolatility Standard deviation of daily returns scaled to annual.
 * @param sharpeRatio          Annualized return / annualized volatility (risk-free rate = 0).
 * @param maxDrawdown          Maximum peak-to-trough decline as a positive decimal.
 * @param winRate              Fraction of closed round-trips that were profitable.
 * @param avgWin               Average dollar profit on winning trades.
 * @param avgLoss              Average dollar loss on losing trades (typically negative).
 * @param profitFactor         Total gross profit / total gross loss.
 * @param totalTrades          Number of completed round-trip trades.
 * @param alpha                CAPM alpha vs benchmark (annualised); null if no benchmark.
 * @param beta                 CAPM beta vs benchmark; null if no benchmark.
 */
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
