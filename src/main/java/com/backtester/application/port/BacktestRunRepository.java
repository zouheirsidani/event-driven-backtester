package com.backtester.application.port;

import com.backtester.domain.backtest.BacktestRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-layer port for persisting and querying {@link BacktestRun} records.
 * Implemented by {@code BacktestRunRepositoryAdapter} in the infrastructure layer.
 */
public interface BacktestRunRepository {

    /**
     * Persists or updates a backtest run record.
     *
     * @param run Run to save.
     * @return The saved (possibly updated) run.
     */
    BacktestRun save(BacktestRun run);

    /**
     * Fetches a run by its unique ID.
     *
     * @param runId UUID of the run.
     * @return The run, or empty if not found.
     */
    Optional<BacktestRun> findById(UUID runId);

    /**
     * Returns all runs without pagination.
     *
     * @return All runs in insertion order.
     */
    List<BacktestRun> findAll();

    /**
     * Returns a page of runs.
     *
     * @param page Zero-based page index.
     * @param size Maximum runs per page.
     * @return Runs for the requested page.
     */
    List<BacktestRun> findAll(int page, int size);

    /**
     * Returns the total number of backtest runs.
     *
     * @return Total count for pagination metadata.
     */
    long count();
}
