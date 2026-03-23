package com.backtester.domain.backtest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Immutable configuration record for a single backtest simulation.
 * All parameters are set at submission time and do not change during execution.
 * Status transitions are represented by creating new instances via {@link #withStatus}.
 *
 * @param runId           Unique identifier assigned at submission time.
 * @param strategyId      Strategy to run (must match a registered {@code Strategy.strategyId()}).
 * @param tickers         One or more ticker symbols to include in the simulation.
 * @param startDate       First trading date (inclusive).
 * @param endDate         Last trading date (inclusive).
 * @param initialCash     Starting capital in USD.
 * @param slippageModel   How execution slippage is modelled (fixed amount or percent).
 * @param commissionModel How brokerage commission is charged (flat fee or per-share).
 * @param status          Current lifecycle state of this run.
 * @param createdAt       Wall-clock time the run record was created.
 * @param benchmarkTicker Optional ticker used to compute CAPM alpha/beta (may be null).
 * @param sweepId         Optional UUID grouping this run with other runs in a parameter sweep
 *                        (may be null for standalone runs).
 */
public record BacktestRun(
        UUID runId,
        String strategyId,
        List<String> tickers,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal initialCash,
        SlippageModel slippageModel,
        CommissionModel commissionModel,
        BacktestStatus status,
        Instant createdAt,
        String benchmarkTicker,
        UUID sweepId
) {

    /**
     * Returns a copy of this run with the status field replaced.
     * This is the idiomatic pattern for "updating" an immutable domain record.
     *
     * @param newStatus The new lifecycle status.
     * @return New {@code BacktestRun} with all other fields unchanged.
     */
    public BacktestRun withStatus(BacktestStatus newStatus) {
        return new BacktestRun(runId, strategyId, tickers, startDate, endDate,
                initialCash, slippageModel, commissionModel, newStatus, createdAt, benchmarkTicker, sweepId);
    }

    /**
     * Returns a copy of this run with the sweepId field replaced.
     * Used by {@link com.backtester.application.backtest.SweepService} to tag each
     * individual sweep run with the shared sweep group identifier.
     *
     * @param newSweepId The UUID that identifies the sweep group.
     * @return New {@code BacktestRun} with all other fields unchanged.
     */
    public BacktestRun withSweepId(UUID newSweepId) {
        return new BacktestRun(runId, strategyId, tickers, startDate, endDate,
                initialCash, slippageModel, commissionModel, status, createdAt, benchmarkTicker, newSweepId);
    }
}
