package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Result for a single train/test window within a walk-forward optimisation run.
 *
 * @param trainStart     First day of the training (in-sample) period.
 * @param trainEnd       Last day of the training period.
 * @param testStart      First day of the test (out-of-sample) period.
 * @param testEnd        Last day of the test period.
 * @param bestParams     Parameter combination that produced the highest Sharpe ratio
 *                       on the training period; null if no combination succeeded.
 * @param trainSharpe    Best Sharpe ratio achieved during training; null on failure.
 * @param testReturn     Total return of the test backtest using the best training params;
 *                       null if the test run failed.
 * @param testSharpe     Sharpe ratio of the test backtest; null if the test run failed.
 * @param testMaxDrawdown Maximum drawdown of the test backtest; null if the test run failed.
 * @param testRunId      UUID of the individual backtest run created for the test period.
 */
public record WalkForwardWindowResult(
        LocalDate trainStart,
        LocalDate trainEnd,
        LocalDate testStart,
        LocalDate testEnd,
        Map<String, Object> bestParams,
        BigDecimal trainSharpe,
        BigDecimal testReturn,
        BigDecimal testSharpe,
        BigDecimal testMaxDrawdown,
        UUID testRunId
) {}
