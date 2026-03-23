package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST /api/v1/backtests/sweep}.
 * Runs the given strategy across all combinations of the supplied parameter values
 * (cartesian product) and returns a ranked summary of results.
 *
 * @param strategyId       Identifier of the strategy to sweep (must match a registered strategy).
 * @param tickers          One or more ticker symbols to include in every combination run.
 * @param startDate        First trading day (inclusive) for all runs in this sweep.
 * @param endDate          Last trading day (inclusive) for all runs in this sweep.
 * @param initialCash      Starting capital for each combination run; must be positive.
 * @param slippageType     "FIXED" or "PERCENT"; optional.
 * @param slippageAmount   Slippage amount; optional.
 * @param commissionType   "FIXED" or "PER_SHARE"; optional.
 * @param commissionAmount Commission amount; optional.
 * @param benchmarkTicker  Optional ticker for CAPM alpha/beta calculation; may be null.
 * @param parameters       Map of parameter name to list of candidate values.
 *                         e.g. {@code {"lookbackDays": [10, 20, 30]}}.
 *                         All combinations of the value lists are tried (cartesian product).
 */
public record SweepBacktestRequest(
    @NotBlank String strategyId,
    @NotEmpty List<String> tickers,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull @Positive BigDecimal initialCash,
    String slippageType,
    BigDecimal slippageAmount,
    String commissionType,
    BigDecimal commissionAmount,
    String benchmarkTicker,
    /** Map of parameter name to list of values to try, e.g. {@code {"lookbackDays": [10, 20, 30]}}. */
    @NotEmpty Map<String, List<Object>> parameters
) {}
