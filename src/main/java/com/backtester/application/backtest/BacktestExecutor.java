package com.backtester.application.backtest;

import com.backtester.application.metrics.MetricsCalculator;
import com.backtester.application.port.BacktestResultRepository;
import com.backtester.application.port.BacktestRunRepository;
import com.backtester.application.port.BarRepository;
import com.backtester.domain.backtest.BacktestResult;
import com.backtester.domain.backtest.BacktestRun;
import com.backtester.domain.backtest.BacktestStatus;
import com.backtester.domain.backtest.EquityCurvePoint;
import com.backtester.domain.backtest.PerformanceMetrics;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.order.Fill;
import com.backtester.domain.portfolio.Portfolio;
import com.backtester.domain.portfolio.PortfolioSnapshot;
import com.backtester.domain.strategy.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Executor that drives the simulation for a single backtest run.
 *
 * <p>This class is kept separate from {@link BacktestService} so that the
 * {@code @Async} annotation on {@link #execute} is honoured by Spring AOP.
 * If {@code execute} were on the same bean as the caller, the proxy would be
 * bypassed (self-invocation problem) and the method would run synchronously.
 *
 * <p>All available {@link com.backtester.domain.strategy.Strategy} implementations
 * are injected as a list by Spring; the correct one is resolved by {@code strategyId}
 * at execution time, unless a {@code strategyOverride} is explicitly supplied (used
 * by the parameter sweep feature to pass a pre-configured strategy instance).
 */
@Component
public class BacktestExecutor {

    private static final Logger log = LoggerFactory.getLogger(BacktestExecutor.class);

    private final BacktestRunRepository runRepository;
    private final BacktestResultRepository resultRepository;
    private final BarRepository barRepository;
    private final EventLoop eventLoop;
    private final MetricsCalculator metricsCalculator;
    private final List<Strategy> strategies;

    /**
     * @param runRepository    Port for persisting and querying backtest run records.
     * @param resultRepository Port for persisting and querying backtest results.
     * @param barRepository    Port for fetching bar data by ticker and date range.
     * @param eventLoop        The simulation core.
     * @param metricsCalculator Computes performance statistics from simulation output.
     * @param strategies       All registered strategy implementations, injected by Spring.
     */
    public BacktestExecutor(BacktestRunRepository runRepository,
                             BacktestResultRepository resultRepository,
                             BarRepository barRepository,
                             EventLoop eventLoop,
                             MetricsCalculator metricsCalculator,
                             List<Strategy> strategies) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.barRepository = barRepository;
        this.eventLoop = eventLoop;
        this.metricsCalculator = metricsCalculator;
        this.strategies = strategies;
    }

    /**
     * Runs the full backtest simulation for the given run on a background thread
     * from the {@code backtestThreadPool}.
     *
     * <p>Accepts the {@link BacktestRun} directly (rather than a UUID) to avoid a
     * transaction race condition: the caller's {@code @Transactional} context may not
     * have committed by the time this async thread starts, so a DB lookup by ID would
     * fail silently and leave the run stuck in PENDING.
     *
     * @param run              The persisted backtest run to execute.
     * @param strategyOverride Pre-configured strategy instance (e.g. from a user template with
     *                         parameters applied), or {@code null} to resolve by strategyId from
     *                         the registered strategy list.
     * @return A completed future (used only to satisfy the {@code @Async} contract).
     */
    @Async("backtestThreadPool")
    public CompletableFuture<Void> execute(BacktestRun run, Strategy strategyOverride) {
        try {
            runRepository.save(run.withStatus(BacktestStatus.RUNNING));
            log.info("Backtest {} started (strategy={}, tickers={})", run.runId(), run.strategyId(), run.tickers());
            BacktestResult result = executeRun(run, strategyOverride);
            log.info("Backtest {} completed. Total return: {}", run.runId(), result.metrics().totalReturn());
        } catch (Exception e) {
            log.error("Backtest {} failed: {}", run.runId(), e.getMessage(), e);
            runRepository.save(run.withStatus(BacktestStatus.FAILED));
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Executes the simulation synchronously (no {@code @Async}) and returns the result.
     * Used by {@link SweepService} to run each parameter combination in the calling thread,
     * avoiding thread-pool exhaustion when many combinations are executed sequentially.
     *
     * @param run              The backtest run configuration to execute.
     * @param strategyOverride If non-null, this strategy instance is used directly instead
     *                         of looking up by {@code strategyId}.
     * @return The completed {@link BacktestResult} with metrics, equity curve, and fills.
     * @throws RuntimeException if the simulation fails (e.g. no bar data found).
     */
    public BacktestResult executeSync(BacktestRun run, Strategy strategyOverride) {
        runRepository.save(run.withStatus(BacktestStatus.RUNNING));
        log.info("Backtest {} started synchronously (strategy={}, tickers={})",
                run.runId(), run.strategyId(), run.tickers());
        try {
            BacktestResult result = executeRun(run, strategyOverride);
            log.info("Backtest {} completed. Total return: {}", run.runId(), result.metrics().totalReturn());
            return result;
        } catch (Exception e) {
            log.error("Backtest {} failed: {}", run.runId(), e.getMessage(), e);
            runRepository.save(run.withStatus(BacktestStatus.FAILED));
            throw e;
        }
    }

    /**
     * Core simulation logic shared between {@link #execute(BacktestRun, Strategy)} and {@link #executeSync}.
     *
     * <p>Loads bar data, resolves or uses the provided strategy, runs the event loop,
     * computes metrics, and persists the result.  The run is transitioned to COMPLETED
     * on success; callers are responsible for transitioning to FAILED on exception.
     *
     * @param run              The backtest run to simulate.
     * @param strategyOverride Pre-configured strategy instance, or {@code null} to resolve
     *                         by {@code strategyId} from the registered strategy list.
     * @return The completed {@link BacktestResult}.
     */
    BacktestResult executeRun(BacktestRun run, Strategy strategyOverride) {
        // Load bar series for each ticker
        List<BarSeries> seriesList = run.tickers().stream()
                .map(ticker -> {
                    List<Bar> bars = barRepository.findByTickerAndDateRange(
                            ticker, run.startDate(), run.endDate());
                    return new BarSeries(ticker, bars);
                })
                .filter(s -> !s.bars().isEmpty())
                .toList();

        if (seriesList.isEmpty()) {
            throw new IllegalStateException("No bar data found for requested tickers and date range");
        }

        // Resolve strategy: use override if provided, otherwise look up by ID
        Strategy strategy;
        if (strategyOverride != null) {
            strategy = strategyOverride;
        } else {
            strategy = strategies.stream()
                    .filter(s -> s.strategyId().equals(run.strategyId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + run.strategyId()));
        }

        Portfolio portfolio = new Portfolio(run.initialCash());

        EventLoop.Result loopResult = eventLoop.run(
                seriesList, strategy, portfolio,
                run.slippageModel(), run.commissionModel()
        );

        List<PortfolioSnapshot> snapshots = loopResult.snapshots();
        List<Fill> fills = loopResult.fills();

        // Load benchmark bars if specified
        List<Bar> benchmarkBars = null;
        if (run.benchmarkTicker() != null && !run.benchmarkTicker().isBlank()) {
            benchmarkBars = barRepository.findByTickerAndDateRange(
                    run.benchmarkTicker(), run.startDate(), run.endDate());
            if (benchmarkBars.isEmpty()) {
                log.warn("Backtest {}: no benchmark data found for ticker {}", run.runId(), run.benchmarkTicker());
                benchmarkBars = null;
            }
        }

        PerformanceMetrics metrics = metricsCalculator.calculate(snapshots, fills, run.initialCash(), benchmarkBars);

        List<EquityCurvePoint> equityCurve = snapshots.stream()
                .map(s -> new EquityCurvePoint(s.date(), s.totalEquity()))
                .toList();

        BacktestResult result = new BacktestResult(run.runId(), metrics, equityCurve, fills, Instant.now());
        resultRepository.save(result);
        runRepository.save(run.withStatus(BacktestStatus.COMPLETED));

        return result;
    }
}
