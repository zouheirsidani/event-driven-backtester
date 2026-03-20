package com.backtester.application.port;

import com.backtester.domain.backtest.BacktestResult;

import java.util.Optional;
import java.util.UUID;

public interface BacktestResultRepository {

    BacktestResult save(BacktestResult result);

    Optional<BacktestResult> findByRunId(UUID runId);
}
