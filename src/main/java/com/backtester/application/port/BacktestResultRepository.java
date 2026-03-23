package com.backtester.application.port;

import com.backtester.domain.backtest.BacktestResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Application-layer port for persisting and querying {@link BacktestResult} records.
 * Implemented by {@code BacktestResultRepositoryAdapter} in the infrastructure layer.
 */
public interface BacktestResultRepository {

    /**
     * Persists the result of a completed backtest.
     *
     * @param result Result to save.
     * @return The saved result.
     */
    BacktestResult save(BacktestResult result);

    /**
     * Fetches the result associated with the given backtest run.
     *
     * @param runId UUID of the backtest run.
     * @return The result, or empty if the run has not yet completed.
     */
    Optional<BacktestResult> findByRunId(UUID runId);
}
