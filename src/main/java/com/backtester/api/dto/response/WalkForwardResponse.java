package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response for {@code POST /api/v1/backtests/walk-forward}.
 *
 * <p>Contains one result per train/test window, plus aggregate out-of-sample
 * statistics computed across all successful test windows.
 *
 * @param strategyId          The strategy that was optimised.
 * @param totalWindows        Total number of train/test windows generated.
 * @param successfulWindows   Number of windows where both training and testing completed.
 * @param avgOutOfSampleSharpe Mean Sharpe ratio across all successful test windows; null if none.
 * @param avgOutOfSampleReturn Mean total return across all successful test windows; null if none.
 * @param windows             Per-window results in chronological order.
 */
public record WalkForwardResponse(
        String strategyId,
        int totalWindows,
        int successfulWindows,
        BigDecimal avgOutOfSampleSharpe,
        BigDecimal avgOutOfSampleReturn,
        List<WalkForwardWindowResult> windows
) {}
