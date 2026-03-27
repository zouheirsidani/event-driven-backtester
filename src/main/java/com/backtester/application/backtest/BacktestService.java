package com.backtester.application.backtest;

import com.backtester.application.port.BacktestResultRepository;
import com.backtester.application.port.BacktestRunRepository;
import com.backtester.application.port.UserStrategyDefinitionRepository;
import com.backtester.domain.backtest.BacktestResult;
import com.backtester.domain.backtest.BacktestRun;
import com.backtester.domain.backtest.BacktestStatus;
import com.backtester.domain.backtest.CommissionModel;
import com.backtester.domain.backtest.FixedCommission;
import com.backtester.domain.backtest.FixedSlippage;
import com.backtester.domain.backtest.PerShareCommission;
import com.backtester.domain.backtest.PercentSlippage;
import com.backtester.domain.backtest.SlippageModel;
import com.backtester.domain.strategy.Strategy;
import com.backtester.domain.strategy.UserStrategyDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates backtest lifecycle operations: submitting new runs, querying
 * existing runs, and retrieving results.
 *
 * <p>This service is intentionally thin.  It does not execute the simulation
 * itself; instead it persists the {@link BacktestRun} and then delegates async
 * execution to {@link BacktestExecutor} to avoid Spring AOP self-proxy issues
 * (the {@code @Async} annotation must be on a different Spring bean).
 */
@Service
public class BacktestService {

    private final BacktestRunRepository runRepository;
    private final BacktestResultRepository resultRepository;
    private final BacktestExecutor executor;
    private final UserStrategyDefinitionRepository userStrategyDefinitionRepository;
    private final List<Strategy> strategies;

    /**
     * @param runRepository                    Port for persisting and querying backtest run records.
     * @param resultRepository                 Port for persisting and querying backtest results.
     * @param executor                         Async executor that runs the simulation on a background thread.
     * @param userStrategyDefinitionRepository Port for resolving user-saved strategy templates.
     * @param strategies                       All registered strategy implementations, used to resolve
     *                                         the base strategy when a user strategy template is provided.
     */
    public BacktestService(BacktestRunRepository runRepository,
                            BacktestResultRepository resultRepository,
                            BacktestExecutor executor,
                            UserStrategyDefinitionRepository userStrategyDefinitionRepository,
                            List<Strategy> strategies) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.executor = executor;
        this.userStrategyDefinitionRepository = userStrategyDefinitionRepository;
        this.strategies = strategies;
    }

    /**
     * Creates and persists a new backtest run, then kicks off async execution.
     * The run is initially saved with status PENDING; the executor will transition
     * it to RUNNING and then COMPLETED or FAILED.
     *
     * <p>Exactly one of {@code strategyId} or {@code userStrategyId} must be non-null.
     * When {@code userStrategyId} is provided, the template is resolved and its base
     * strategy is configured with the stored parameters before execution.
     *
     * @param strategyId       Strategy identifier (must match a registered strategy); null if using a template.
     * @param userStrategyId   UUID of a saved user strategy template; null if using strategyId directly.
     * @param tickers          Tickers to include in the simulation.
     * @param startDate        Start date (inclusive).
     * @param endDate          End date (inclusive).
     * @param initialCash      Starting capital.
     * @param slippageType     "FIXED" or "PERCENT" (case-insensitive); defaults to FIXED.
     * @param slippageAmount   Slippage amount; meaning depends on type.
     * @param commissionType   "FIXED" or "PER_SHARE" (case-insensitive); defaults to FIXED.
     * @param commissionAmount Commission amount; meaning depends on type.
     * @param benchmarkTicker  Optional ticker for CAPM alpha/beta calculation; may be null.
     * @return The saved {@link BacktestRun} with status PENDING.
     */
    @Transactional
    public BacktestRun submit(String strategyId,
                               UUID userStrategyId,
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

        // Resolve strategy: either use the provided strategyId directly, or look up a user template
        Strategy resolvedStrategy = null;
        String effectiveStrategyId = strategyId;

        if (userStrategyId != null) {
            UserStrategyDefinition template = userStrategyDefinitionRepository.findById(userStrategyId)
                    .orElseThrow(() -> new IllegalArgumentException("Strategy template not found: " + userStrategyId));
            final String baseId = template.baseStrategyId();
            effectiveStrategyId = baseId;
            Strategy base = strategies.stream()
                    .filter(s -> s.strategyId().equals(baseId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Base strategy not found: " + baseId));
            // Apply template parameters to get a configured strategy instance
            resolvedStrategy = base.withParameters(template.parameters());
        }

        BacktestRun run = new BacktestRun(
                UUID.randomUUID(),
                effectiveStrategyId,
                tickers,
                startDate,
                endDate,
                initialCash,
                slippageModel,
                commissionModel,
                BacktestStatus.PENDING,
                Instant.now(),
                benchmarkTicker,
                null  // sweepId — set later by SweepService if this is part of a sweep
        );

        BacktestRun saved = runRepository.save(run);

        // Delay execution until after this transaction commits.
        // If execute() were called here (inside the @Transactional boundary), the async
        // thread could start before the PENDING row is visible to other transactions,
        // causing a duplicate-key INSERT when the executor tries to save the RUNNING status.
        final Strategy strategyToExecute = resolvedStrategy;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executor.execute(saved, strategyToExecute);
            }
        });

        return saved;
    }

    /**
     * Creates, persists, and returns a new {@link BacktestRun} in PENDING state
     * without triggering async execution.
     *
     * <p>This method is used by {@link SweepService} to create individual sweep run
     * records before executing them synchronously.  Unlike {@link #submit}, it does
     * not call {@link BacktestExecutor#execute}.
     *
     * @param strategyId       Strategy identifier.
     * @param tickers          Tickers to include.
     * @param startDate        Start date (inclusive).
     * @param endDate          End date (inclusive).
     * @param initialCash      Starting capital.
     * @param slippageType     Slippage type string; may be null.
     * @param slippageAmount   Slippage amount; may be null.
     * @param commissionType   Commission type string; may be null.
     * @param commissionAmount Commission amount; may be null.
     * @param benchmarkTicker  Optional benchmark ticker; may be null.
     * @return The persisted {@link BacktestRun} with status PENDING and a fresh UUID.
     */
    @Transactional
    public BacktestRun submitAndReturnRun(String strategyId,
                                           List<String> tickers,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           BigDecimal initialCash,
                                           String slippageType,
                                           BigDecimal slippageAmount,
                                           String commissionType,
                                           BigDecimal commissionAmount,
                                           String benchmarkTicker) {
        // This method is for sweep use; strategyId is always supplied directly (no user template resolution needed)
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
                benchmarkTicker,
                null  // sweepId — set later by SweepService
        );

        return runRepository.save(run);
    }

    /**
     * Persists an updated {@link BacktestRun} record.
     * Used by {@link SweepService} to attach the {@code sweepId} after the run is created.
     *
     * @param run The run record to save (must already exist in the database).
     * @return The saved run instance.
     */
    @Transactional
    public BacktestRun updateRun(BacktestRun run) {
        return runRepository.save(run);
    }

    /**
     * Returns all backtest runs without pagination.
     *
     * @return List of all runs in insertion order.
     */
    public List<BacktestRun> listRuns() {
        return runRepository.findAll();
    }

    /**
     * Returns a page of backtest runs.
     *
     * @param page Zero-based page index.
     * @param size Maximum number of runs per page.
     * @return Runs on the requested page.
     */
    public List<BacktestRun> listRuns(int page, int size) {
        return runRepository.findAll(page, size);
    }

    /**
     * Returns the total number of backtest runs in the database.
     *
     * @return Total run count.
     */
    public long countRuns() {
        return runRepository.count();
    }

    /**
     * Fetches a single backtest run by its ID.
     *
     * @param runId UUID of the run to fetch.
     * @return The run, or empty if not found.
     */
    public Optional<BacktestRun> getRun(UUID runId) {
        return runRepository.findById(runId);
    }

    /**
     * Fetches the result for a completed backtest run.
     *
     * @param runId UUID of the run whose result to fetch.
     * @return The result, or empty if the run has not completed yet.
     */
    public Optional<BacktestResult> getResult(UUID runId) {
        return resultRepository.findByRunId(runId);
    }

    /**
     * Constructs the appropriate {@link SlippageModel} from string type and amount.
     * Defaults to {@link com.backtester.domain.backtest.FixedSlippage} with zero amount
     * if the type is null or blank.
     *
     * @param type   "PERCENT" or any other string (treated as FIXED).
     * @param amount Slippage amount; a default is used if null.
     * @return Resolved slippage model instance.
     */
    private SlippageModel buildSlippageModel(String type, BigDecimal amount) {
        if (type == null || type.isBlank()) {
            return new FixedSlippage(BigDecimal.ZERO);
        }
        return switch (type.toUpperCase()) {
            case "PERCENT" -> new PercentSlippage(amount != null ? amount : new BigDecimal("0.001"));
            default -> new FixedSlippage(amount != null ? amount : BigDecimal.ZERO);
        };
    }

    /**
     * Constructs the appropriate {@link CommissionModel} from string type and amount.
     * Defaults to {@link com.backtester.domain.backtest.FixedCommission} with zero amount
     * if the type is null or blank.
     *
     * @param type   "PER_SHARE" or any other string (treated as FIXED).
     * @param amount Commission amount; a default is used if null.
     * @return Resolved commission model instance.
     */
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
