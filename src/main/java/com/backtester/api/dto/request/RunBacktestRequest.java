package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/backtests}.
 * Exactly one of {@code strategyId} or {@code userStrategyId} must be supplied:
 * <ul>
 *   <li>{@code strategyId} — use a registered base strategy directly.</li>
 *   <li>{@code userStrategyId} — use a saved user strategy template; the base
 *       strategy ID and parameters are resolved from the saved template.</li>
 * </ul>
 *
 * @param strategyId       Identifier of a registered base strategy (e.g. "MOMENTUM_V1");
 *                         optional if userStrategyId is set.
 * @param userStrategyId   UUID of a saved user strategy template; optional if strategyId is set.
 * @param tickers          One or more ticker symbols to simulate.
 * @param startDate        First trading day (inclusive).
 * @param endDate          Last trading day (inclusive).
 * @param initialCash      Starting capital in USD; must be positive.
 * @param slippageType     "FIXED" or "PERCENT"; optional.
 * @param slippageAmount   Slippage amount; optional.
 * @param commissionType   "FIXED" or "PER_SHARE"; optional.
 * @param commissionAmount Commission amount; optional.
 * @param benchmarkTicker  Ticker for CAPM alpha/beta; optional.
 */
public record RunBacktestRequest(
        String strategyId,
        UUID userStrategyId,
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
