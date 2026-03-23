package com.backtester.application.backtest;

import com.backtester.api.dto.request.SweepBacktestRequest;
import com.backtester.api.dto.response.SweepResultEntry;
import com.backtester.api.dto.response.SweepResultResponse;
import com.backtester.domain.backtest.BacktestResult;
import com.backtester.domain.backtest.BacktestRun;
import com.backtester.domain.strategy.Strategy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates parameter sweep backtests.
 *
 * <p>Generates the cartesian product of all parameter combinations supplied in the
 * {@link SweepBacktestRequest}, runs each synchronously via {@link BacktestExecutor},
 * and returns a ranked summary sorted by Sharpe ratio descending.
 *
 * <p>Each combination is persisted as a separate {@link BacktestRun} record tagged with
 * a shared {@code sweepId} so that sweep runs can be filtered out in the dashboard.
 */
@Service
public class SweepService {

    private final BacktestService backtestService;
    private final BacktestExecutor backtestExecutor;
    private final List<Strategy> strategies;

    /**
     * @param backtestService  Used to create and persist individual run records.
     * @param backtestExecutor Used to execute each combination synchronously.
     * @param strategies       All registered strategy implementations injected by Spring.
     */
    public SweepService(BacktestService backtestService,
                        BacktestExecutor backtestExecutor,
                        List<Strategy> strategies) {
        this.backtestService = backtestService;
        this.backtestExecutor = backtestExecutor;
        this.strategies = strategies;
    }

    /**
     * Runs a parameter sweep and returns a ranked summary of all combinations.
     *
     * <p>Steps:
     * <ol>
     *   <li>Resolve the base strategy bean by {@code strategyId}.</li>
     *   <li>Compute the cartesian product of all parameter value lists.</li>
     *   <li>For each combination: create a {@link BacktestRun}, tag it with the sweep UUID,
     *       parameterise the strategy via {@link Strategy#withParameters}, and run it
     *       synchronously.</li>
     *   <li>Sort results by Sharpe ratio descending (failed runs with null metrics last).</li>
     * </ol>
     *
     * @param request Validated sweep request containing strategy, tickers, dates, and parameters.
     * @return Ranked summary of all parameter combinations.
     * @throws IllegalArgumentException if the strategy ID does not match any registered strategy.
     */
    public SweepResultResponse runSweep(SweepBacktestRequest request) {
        // Find the base strategy bean
        Strategy baseStrategy = strategies.stream()
            .filter(s -> s.strategyId().equals(request.strategyId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown strategy: " + request.strategyId()));

        // Generate all parameter combinations (cartesian product)
        List<Map<String, Object>> combinations = cartesianProduct(request.parameters());

        UUID sweepId = UUID.randomUUID();
        List<SweepResultEntry> entries = new ArrayList<>();
        int successfulRuns = 0;

        for (Map<String, Object> paramCombo : combinations) {
            // Submit creates + persists the BacktestRun record (in PENDING state)
            BacktestRun run = backtestService.submitAndReturnRun(
                request.strategyId(),
                request.tickers(),
                request.startDate(),
                request.endDate(),
                request.initialCash(),
                request.slippageType(),
                request.slippageAmount(),
                request.commissionType(),
                request.commissionAmount(),
                request.benchmarkTicker()
            );
            // Tag it with the sweep ID
            run = run.withSweepId(sweepId);
            backtestService.updateRun(run);

            // Configure the strategy with this combination's parameters
            Strategy parameterizedStrategy = baseStrategy.withParameters(paramCombo);

            try {
                // Execute synchronously (blocks until complete)
                BacktestResult result = backtestExecutor.executeSync(run, parameterizedStrategy);
                successfulRuns++;

                entries.add(new SweepResultEntry(
                    paramCombo,
                    run.runId(),
                    result.metrics().totalReturn(),
                    result.metrics().sharpeRatio(),
                    result.metrics().maxDrawdown()
                ));
            } catch (Exception e) {
                // Failed combinations are recorded with null metrics
                entries.add(new SweepResultEntry(paramCombo, run.runId(), null, null, null));
            }
        }

        // Sort by Sharpe ratio descending (nulls last)
        entries.sort(Comparator.comparing(
            SweepResultEntry::sharpeRatio,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return new SweepResultResponse(request.strategyId(), combinations.size(), successfulRuns, entries);
    }

    /**
     * Generates the cartesian product of all parameter value lists.
     *
     * <p>Example: {@code {"a": [1, 2], "b": [10, 20]}} produces:
     * {@code [{a=1, b=10}, {a=1, b=20}, {a=2, b=10}, {a=2, b=20}]}
     *
     * <p>Insertion order of keys is preserved via {@link LinkedHashMap} so that the
     * parameter names appear in a consistent order in the response.
     *
     * @param params Map of parameter name to list of candidate values.
     * @return All combinations as a list of maps.
     */
    List<Map<String, Object>> cartesianProduct(Map<String, List<Object>> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(new LinkedHashMap<>());

        for (String key : keys) {
            List<Object> values = params.get(key);
            List<Map<String, Object>> expanded = new ArrayList<>();
            for (Map<String, Object> existing : result) {
                for (Object value : values) {
                    Map<String, Object> combo = new LinkedHashMap<>(existing);
                    combo.put(key, value);
                    expanded.add(combo);
                }
            }
            result = expanded;
        }
        return result;
    }
}
