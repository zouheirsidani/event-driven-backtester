package com.backtester.domain.backtest;

/**
 * Represents the lifecycle state of a {@link BacktestRun}.
 * Transitions are: PENDING → RUNNING → COMPLETED or FAILED.
 */
public enum BacktestStatus {
    /** Saved to the database and queued for async execution. */
    PENDING,
    /** Currently being processed by {@code BacktestExecutor} on a background thread. */
    RUNNING,
    /** The event loop finished successfully and results are available. */
    COMPLETED,
    /** The event loop threw an exception; check server logs for details. */
    FAILED
}
