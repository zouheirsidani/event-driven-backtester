package com.backtester.application.port;

import com.backtester.domain.backtest.BacktestRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BacktestRunRepository {

    BacktestRun save(BacktestRun run);

    Optional<BacktestRun> findById(UUID runId);

    List<BacktestRun> findAll();
}
