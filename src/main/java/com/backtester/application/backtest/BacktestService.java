package com.backtester.application.backtest;

import com.backtester.application.port.BacktestResultRepository;
import com.backtester.application.port.BacktestRunRepository;
import com.backtester.domain.backtest.BacktestResult;
import com.backtester.domain.backtest.BacktestRun;
import com.backtester.domain.backtest.BacktestStatus;
import com.backtester.domain.backtest.CommissionModel;
import com.backtester.domain.backtest.FixedCommission;
import com.backtester.domain.backtest.FixedSlippage;
import com.backtester.domain.backtest.PerShareCommission;
import com.backtester.domain.backtest.PercentSlippage;
import com.backtester.domain.backtest.SlippageModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BacktestService {

    private final BacktestRunRepository runRepository;
    private final BacktestResultRepository resultRepository;
    private final BacktestExecutor executor;

    public BacktestService(BacktestRunRepository runRepository,
                            BacktestResultRepository resultRepository,
                            BacktestExecutor executor) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.executor = executor;
    }

    @Transactional
    public BacktestRun submit(String strategyId,
                               List<String> tickers,
                               LocalDate startDate,
                               LocalDate endDate,
                               BigDecimal initialCash,
                               String slippageType,
                               BigDecimal slippageAmount,
                               String commissionType,
                               BigDecimal commissionAmount,
                               String benchmarkTicker) {
        SlippageModel slippageModel = buildSlippageModel(slippageType, slippageAmount);
        CommissionModel commissionModel = buildCommissionModel(commissionType, commissionAmount);

        BacktestRun run = new BacktestRun(
                UUID.randomUUID(),
                strategyId,
                tickers,
                startDate,
                endDate,
                initialCash,
                slippageModel,
                commissionModel,
                BacktestStatus.PENDING,
                Instant.now(),
                benchmarkTicker
        );

        BacktestRun saved = runRepository.save(run);
        executor.execute(saved.runId());
        return saved;
    }

    public List<BacktestRun> listRuns() {
        return runRepository.findAll();
    }

    public List<BacktestRun> listRuns(int page, int size) {
        return runRepository.findAll(page, size);
    }

    public long countRuns() {
        return runRepository.count();
    }

    public Optional<BacktestRun> getRun(UUID runId) {
        return runRepository.findById(runId);
    }

    public Optional<BacktestResult> getResult(UUID runId) {
        return resultRepository.findByRunId(runId);
    }

    private SlippageModel buildSlippageModel(String type, BigDecimal amount) {
        if (type == null || type.isBlank()) {
            return new FixedSlippage(BigDecimal.ZERO);
        }
        return switch (type.toUpperCase()) {
            case "PERCENT" -> new PercentSlippage(amount != null ? amount : new BigDecimal("0.001"));
            default -> new FixedSlippage(amount != null ? amount : BigDecimal.ZERO);
        };
    }

    private CommissionModel buildCommissionModel(String type, BigDecimal amount) {
        if (type == null || type.isBlank()) {
            return new FixedCommission(BigDecimal.ZERO);
        }
        return switch (type.toUpperCase()) {
            case "PER_SHARE" -> new PerShareCommission(amount != null ? amount : new BigDecimal("0.01"));
            default -> new FixedCommission(amount != null ? amount : BigDecimal.ZERO);
        };
    }
}
