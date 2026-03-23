package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * A single parameter combination result within a sweep run.
 * Null metric fields indicate that the combination failed to execute.
 *
 * @param parameterValues The specific parameter values used for this combination.
 * @param backtestRunId   UUID of the individual backtest run created for this combination.
 * @param totalReturn     Total return of the portfolio over the simulation period; null on failure.
 * @param sharpeRatio     Annualised Sharpe ratio; null on failure.
 * @param maxDrawdown     Maximum peak-to-trough drawdown; null on failure.
 */
public record SweepResultEntry(
    Map<String, Object> parameterValues,
    UUID backtestRunId,
    BigDecimal totalReturn,
    BigDecimal sharpeRatio,
    BigDecimal maxDrawdown
) {}
