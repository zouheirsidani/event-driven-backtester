package com.backtester.application.backtest;

import com.backtester.api.dto.request.SweepBacktestRequest;
import com.backtester.api.dto.request.WalkForwardRequest;
import com.backtester.api.dto.response.SweepResultEntry;
import com.backtester.api.dto.response.SweepResultResponse;
import com.backtester.api.dto.response.WalkForwardResponse;
import com.backtester.api.dto.response.WalkForwardWindowResult;
import com.backtester.domain.backtest.BacktestResult;
import com.backtester.domain.backtest.BacktestRun;
import com.backtester.domain.strategy.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates walk-forward optimisation runs.
 *
 * <p>The full date range is divided into rolling windows of fixed length:
 * <ul>
 *   <li><b>Training window</b>: {@code trainMonths} months — a parameter sweep is run here
 *       to find the best-performing parameter combination (highest Sharpe).</li>
 *   <li><b>Test window</b>: {@code testMonths} months — a single backtest using the best
 *       training parameters is run here to measure out-of-sample performance.</li>
 * </ul>
 *
 * <p>Both windows advance together by {@code testMonths} for each successive iteration,
 * keeping the training window length constant (rolling, not expanding).
 *
 * <p>Example with trainMonths=12, testMonths=3:
 * <pre>
 *   Window 1: Train [Jan 2020 – Dec 2020], Test [Jan 2021 – Mar 2021]
 *   Window 2: Train [Apr 2020 – Mar 2021], Test [Apr 2021 – Jun 2021]
 *   ...
 * </pre>
 */
@Service
public class WalkForwardService {

    private static final Logger log = LoggerFactory.getLogger(WalkForwardService.class);

    private final SweepService sweepService;
    private final BacktestService backtestService;
    private final BacktestExecutor backtestExecutor;
    private final List<Strategy> strategies;

    /**
     * @param sweepService     Used to run parameter sweeps on each training window.
     * @param backtestService  Used to create and persist individual test-window run records.
     * @param backtestExecutor Used to execute the test-window backtests synchronously.
     * @param strategies       All registered strategy implementations.
     */
    public WalkForwardService(SweepService sweepService,
                               BacktestService backtestService,
                               BacktestExecutor backtestExecutor,
                               List<Strategy> strategies) {
        this.sweepService = sweepService;
        this.backtestService = backtestService;
        this.backtestExecutor = backtestExecutor;
        this.strategies = strategies;
    }

    /**
     * Runs a walk-forward optimisation and returns per-window and aggregate results.
     *
     * <p>For each train/test window:
     * <ol>
     *   <li>Build a {@link SweepBacktestRequest} for the training period and delegate to
     *       {@link SweepService}.</li>
     *   <li>Pick the parameter combination with the highest training Sharpe ratio.</li>
     *   <li>Create a {@link BacktestRun} for the test period, parameterise the strategy
     *       with the best training params, and execute synchronously.</li>
     * </ol>
     *
     * @param request Validated walk-forward configuration.
     * @return {@link WalkForwardResponse} with per-window results and aggregate out-of-sample stats.
     */
    public WalkForwardResponse run(WalkForwardRequest request) {
        // Resolve the base strategy bean once — reused for test-window execution
        Strategy baseStrategy = strategies.stream()
                .filter(s -> s.strategyId().equals(request.strategyId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown strategy: " + request.strategyId()));

        List<WalkForwardWindowResult> windowResults = new ArrayList<>();
        LocalDate windowTrainStart = request.startDate();

        while (true) {
            LocalDate windowTrainEnd = windowTrainStart.plusMonths(request.trainMonths()).minusDays(1);
            LocalDate windowTestStart = windowTrainEnd.plusDays(1);
            LocalDate windowTestEnd = windowTestStart.plusMonths(request.testMonths()).minusDays(1);

            // Stop if the test window would exceed the full evaluation range
            if (windowTestEnd.isAfter(request.endDate())) break;

            log.info("Walk-forward window: train [{} – {}], test [{} – {}]",
                    windowTrainStart, windowTrainEnd, windowTestStart, windowTestEnd);

            WalkForwardWindowResult windowResult = runWindow(
                    request, baseStrategy,
                    windowTrainStart, windowTrainEnd,
                    windowTestStart, windowTestEnd);
            windowResults.add(windowResult);

            // Advance both windows by testMonths (rolling, not expanding)
            windowTrainStart = windowTrainStart.plusMonths(request.testMonths());
        }

        return buildResponse(request.strategyId(), windowResults);
    }

    /**
     * Runs a single train/test window: sweeps on the training period, picks the best
     * parameter combination, then backtests it on the test period.
     *
     * @param request          The original walk-forward request (for shared fields).
     * @param baseStrategy     The resolved base strategy bean.
     * @param trainStart       First day of the training period.
     * @param trainEnd         Last day of the training period.
     * @param testStart        First day of the test period.
     * @param testEnd          Last day of the test period.
     * @return A {@link WalkForwardWindowResult} for this window.
     */
    private WalkForwardWindowResult runWindow(WalkForwardRequest request,
                                               Strategy baseStrategy,
                                               LocalDate trainStart,
                                               LocalDate trainEnd,
                                               LocalDate testStart,
                                               LocalDate testEnd) {
        // ── Training: parameter sweep ────────────────────────────────────────────
        SweepBacktestRequest sweepReq = new SweepBacktestRequest(
                request.strategyId(),
                request.tickers(),
                trainStart,
                trainEnd,
                request.initialCash(),
                request.slippageType(),
                request.slippageAmount(),
                request.commissionType(),
                request.commissionAmount(),
                request.benchmarkTicker(),
                request.parameters()
        );

        SweepResultResponse sweepResult;
        try {
            sweepResult = sweepService.runSweep(sweepReq);
        } catch (Exception e) {
            log.warn("Walk-forward: training sweep failed for window [{} – {}]: {}",
                    trainStart, trainEnd, e.getMessage());
            return new WalkForwardWindowResult(
                    trainStart, trainEnd, testStart, testEnd,
                    null, null, null, null, null, null);
        }

        // Pick the best parameter combination (highest training Sharpe)
        SweepResultEntry best = sweepResult.results().stream()
                .filter(e -> e.sharpeRatio() != null)
                .max(Comparator.comparing(SweepResultEntry::sharpeRatio))
                .orElse(null);

        if (best == null) {
            log.warn("Walk-forward: no successful training combination for window [{} – {}]",
                    trainStart, trainEnd);
            return new WalkForwardWindowResult(
                    trainStart, trainEnd, testStart, testEnd,
                    null, null, null, null, null, null);
        }

        Map<String, Object> bestParams = best.parameterValues();
        BigDecimal trainSharpe = best.sharpeRatio();

        // ── Test: run best params out-of-sample ──────────────────────────────────
        try {
            BacktestRun testRun = backtestService.submitAndReturnRun(
                    request.strategyId(),
                    request.tickers(),
                    testStart,
                    testEnd,
                    request.initialCash(),
                    request.slippageType(),
                    request.slippageAmount(),
                    request.commissionType(),
                    request.commissionAmount(),
                    request.benchmarkTicker()
            );

            Strategy parameterizedStrategy = baseStrategy.withParameters(bestParams);
            BacktestResult testResult = backtestExecutor.executeSync(testRun, parameterizedStrategy);

            return new WalkForwardWindowResult(
                    trainStart, trainEnd, testStart, testEnd,
                    bestParams,
                    trainSharpe,
                    testResult.metrics().totalReturn(),
                    testResult.metrics().sharpeRatio(),
                    testResult.metrics().maxDrawdown(),
                    testRun.runId()
            );
        } catch (Exception e) {
            log.warn("Walk-forward: test run failed for window [{} – {}]: {}",
                    testStart, testEnd, e.getMessage());
            return new WalkForwardWindowResult(
                    trainStart, trainEnd, testStart, testEnd,
                    bestParams, trainSharpe,
                    null, null, null, null);
        }
    }

    /**
     * Assembles the final {@link WalkForwardResponse} from per-window results,
     * computing aggregate out-of-sample statistics across all successful test windows.
     *
     * @param strategyId    The strategy ID.
     * @param windowResults All window results in chronological order.
     * @return The complete walk-forward response.
     */
    private WalkForwardResponse buildResponse(String strategyId,
                                               List<WalkForwardWindowResult> windowResults) {
        List<WalkForwardWindowResult> successful = windowResults.stream()
                .filter(w -> w.testSharpe() != null)
                .toList();

        BigDecimal avgSharpe = null;
        BigDecimal avgReturn = null;

        if (!successful.isEmpty()) {
            double sharpeSum = successful.stream()
                    .mapToDouble(w -> w.testSharpe().doubleValue())
                    .sum();
            double returnSum = successful.stream()
                    .mapToDouble(w -> w.testReturn().doubleValue())
                    .sum();
            avgSharpe = BigDecimal.valueOf(sharpeSum / successful.size())
                    .setScale(6, RoundingMode.HALF_UP);
            avgReturn = BigDecimal.valueOf(returnSum / successful.size())
                    .setScale(6, RoundingMode.HALF_UP);
        }

        return new WalkForwardResponse(
                strategyId,
                windowResults.size(),
                successful.size(),
                avgSharpe,
                avgReturn,
                windowResults
        );
    }
}
