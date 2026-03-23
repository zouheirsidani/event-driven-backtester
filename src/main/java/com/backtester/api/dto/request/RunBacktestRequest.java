package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for {@code POST /api/v1/backtests}.
 * Carries all parameters needed to configure and launch a backtest run.
 *
 * @param strategyId       Identifier of the strategy to run (must match a registered strategy).
 * @param tickers          One or more ticker symbols to simulate.
 * @param startDate        First trading day (inclusive); bars before this date are ignored.
 * @param endDate          Last trading day (inclusive).
 * @param initialCash      Starting capital in USD; must be a positive value.
 * @param slippageType     "FIXED" (dollars per share) or "PERCENT" (fraction of price); optional.
 * @param slippageAmount   Numeric slippage amount; meaning depends on {@code slippageType}; optional.
 * @param commissionType   "FIXED" (flat fee) or "PER_SHARE" (per-share rate); optional.
 * @param commissionAmount Numeric commission amount; meaning depends on {@code commissionType}; optional.
 * @param benchmarkTicker  Ticker used to compute CAPM alpha/beta (e.g. "SPY"); optional.
 */
public record RunBacktestRequest(
        @NotBlank(message = "strategyId is required") String strategyId,
        @NotEmpty(message = "tickers must not be empty") List<String> tickers,
        @NotNull(message = "startDate is required") LocalDate startDate,
        @NotNull(message = "endDate is required") LocalDate endDate,
        @NotNull @Positive BigDecimal initialCash,
        String slippageType,
        BigDecimal slippageAmount,
        String commissionType,
        BigDecimal commissionAmount,
        String benchmarkTicker
) {}
