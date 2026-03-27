package com.backtester.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST /api/v1/backtests/walk-forward}.
 *
 * <p>The full date range [{@code startDate}, {@code endDate}] is divided into
 * rolling windows of {@code trainMonths} (optimisation) + {@code testMonths}
 * (out-of-sample validation). For each window the best parameters from the
 * training sweep are applied to the test period.
 *
 * @param strategyId       Strategy to optimise; must match a registered strategy.
 * @param tickers          Tickers to include in every window run.
 * @param startDate        Start of the full evaluation period (inclusive).
 * @param endDate          End of the full evaluation period (inclusive).
 * @param initialCash      Starting capital for every individual backtest.
 * @param trainMonths      Length of each training (in-sample) window in calendar months; min 1.
 * @param testMonths       Length of each test (out-of-sample) window in calendar months; min 1.
 * @param slippageType     "FIXED" or "PERCENT"; optional.
 * @param slippageAmount   Slippage amount; optional.
 * @param commissionType   "FIXED" or "PER_SHARE"; optional.
 * @param commissionAmount Commission amount; optional.
 * @param benchmarkTicker  Optional benchmark ticker for CAPM metrics.
 * @param parameters       Map of parameter name to list of candidate values (cartesian product per window).
 */
public record WalkForwardRequest(
        @NotBlank String strategyId,
        @NotEmpty List<String> tickers,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Positive BigDecimal initialCash,
        @NotNull @Min(1) int trainMonths,
        @NotNull @Min(1) int testMonths,
        String slippageType,
        BigDecimal slippageAmount,
        String commissionType,
        BigDecimal commissionAmount,
        String benchmarkTicker,
        /** Parameter name → list of values to sweep per training window. */
        @NotEmpty Map<String, List<Object>> parameters
) {}
