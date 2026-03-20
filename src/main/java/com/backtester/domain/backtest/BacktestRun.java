package com.backtester.domain.backtest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
        String benchmarkTicker
) {

    public BacktestRun withStatus(BacktestStatus newStatus) {
        return new BacktestRun(runId, strategyId, tickers, startDate, endDate,
                initialCash, slippageModel, commissionModel, newStatus, createdAt, benchmarkTicker);
    }
}
