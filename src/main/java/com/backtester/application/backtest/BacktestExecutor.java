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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous executor that drives the simulation for a single backtest run.
 *
 * <p>This class is kept separate from {@link BacktestService} so that the
 * {@code @Async} annotation on {@link #execute} is honoured by Spring AOP.
 * If {@code execute} were on the same bean as the caller, the proxy would be
 * bypassed (self-invocation problem) and the method would run synchronously.
 *
 * <p>All available {@link com.backtester.domain.strategy.Strategy} implementations
 * are injected as a list by Spring; the correct one is resolved by {@code strategyId}
 * at execution time.
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
     * Runs the full backtest simulation for the given run ID on a background thread
     * from the {@code backtestThreadPool}.
     *
     * <p>Steps performed:
     * <ol>
     *   <li>Load the run and transition it to RUNNING.</li>
     *   <li>Fetch bar data for each requested ticker in the date range.</li>
     *   <li>Resolve the strategy by ID from the injected strategy list.</li>
     *   <li>Run the event loop and collect snapshots and fills.</li>
     *   <li>Optionally load benchmark bar data for CAPM metrics.</li>
     *   <li>Compute performance metrics and persist the result.</li>
     *   <li>Transition the run to COMPLETED; on any error transition to FAILED.</li>
     * </ol>
     *
     * @param runId UUID of the run to execute.
     * @return A completed future (used only to satisfy the {@code @Async} contract).
     */
    @Async("backtestThreadPool")
    public CompletableFuture<Void> execute(UUID runId) {
        BacktestRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Backtest run not found: " + runId));

        runRepository.save(run.withStatus(BacktestStatus.RUNNING));
        log.info("Backtest {} started (strategy={}, tickers={})", runId, run.strategyId(), run.tickers());

        try {
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

            // Resolve strategy
            Strategy strategy = strategies.stream()
                    .filter(s -> s.strategyId().equals(run.strategyId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + run.strategyId()));

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
                    log.warn("Backtest {}: no benchmark data found for ticker {}", runId, run.benchmarkTicker());
                    benchmarkBars = null;
                }
            }

            PerformanceMetrics metrics = metricsCalculator.calculate(snapshots, fills, run.initialCash(), benchmarkBars);

            List<EquityCurvePoint> equityCurve = snapshots.stream()
                    .map(s -> new EquityCurvePoint(s.date(), s.totalEquity()))
                    .toList();

            BacktestResult result = new BacktestResult(runId, metrics, equityCurve, fills, Instant.now());
            resultRepository.save(result);
            runRepository.save(run.withStatus(BacktestStatus.COMPLETED));

            log.info("Backtest {} completed. Total return: {}", runId, metrics.totalReturn());

        } catch (Exception e) {
            log.error("Backtest {} failed: {}", runId, e.getMessage(), e);
            runRepository.save(run.withStatus(BacktestStatus.FAILED));
        }

        return CompletableFuture.completedFuture(null);
    }
}
