package com.backtester.strategy.meanreversion;

import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.portfolio.Portfolio;
import com.backtester.domain.strategy.SignalDirection;
import com.backtester.domain.strategy.Strategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Counter-trend strategy based on the Bollinger Band z-score.
 *
 * <p>The z-score measures how many standard deviations the current close price
 * is above or below the N-day rolling mean:
 * <pre>  z = (currentClose - mean_N) / stdDev_N</pre>
 *
 * <p>Signal logic:
 * <ul>
 *   <li><b>BUY</b>: z &lt; {@code zScoreThreshold} — price is statistically cheap;
 *       no position held.</li>
 *   <li><b>EXIT</b>: z &ge; 0.0 — price has reverted to the mean
 *       (or above); a position is held.</li>
 * </ul>
 *
 * <p>At least {@code windowSize} bars are required to compute a non-trivial mean and
 * standard deviation, so the strategy is silent during the warm-up period.
 *
 * <p>The default window is 20 days with a z-score entry threshold of -2.0.
 * Call {@link #withParameters(Map)} with {@code {"windowSize": N, "zScoreThreshold": -2.5}}
 * to create a parameterised copy for sweep runs.
 */
@Component
public class MeanReversionStrategy implements Strategy {

    /** Number of days in the rolling statistics window. */
    private final int windowSize;

    /** Z-score threshold below which a BUY signal is emitted. */
    private final double zScoreThreshold;

    /** Fixed exit z-score: exit when price has reverted to or above the mean. */
    private static final double EXIT_Z = 0.0;

    /**
     * Default constructor used by Spring for component scanning.
     * Sets {@code windowSize} to 20 and {@code zScoreThreshold} to -2.0.
     */
    public MeanReversionStrategy() {
        this.windowSize = 20;
        this.zScoreThreshold = -2.0;
    }

    /**
     * Package-private constructor for use by {@link #withParameters(Map)} only.
     * Not annotated with {@code @Component} — Spring must not create a second bean.
     *
     * @param windowSize      Number of days in the rolling statistics window.
     * @param zScoreThreshold Z-score entry threshold (typically negative, e.g. -2.0).
     */
    MeanReversionStrategy(int windowSize, double zScoreThreshold) {
        this.windowSize = windowSize;
        this.zScoreThreshold = zScoreThreshold;
    }

    @Override
    public String strategyId() {
        return "MEAN_REVERSION_V1";
    }

    @Override
    public String displayName() {
        return "Mean Reversion Strategy (Bollinger Band / z-score, 20-day window)";
    }

    /**
     * Computes the rolling z-score and emits a signal if entry or exit
     * thresholds are crossed.
     *
     * @param history    All bars for this ticker up to today.
     * @param currentBar Today's bar.
     * @param portfolio  Used to check whether a position is already held.
     * @return A LONG signal when the price is beyond the z-score threshold and no
     *         position is held; an EXIT signal when price reverts to mean or above
     *         and a position is held; empty otherwise.
     */
    @Override
    public Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio) {
        List<Bar> bars = history.bars();
        // Require a full window of bars before calculating statistics
        if (bars.size() < windowSize) {
            return Optional.empty();
        }

        // Use only the most recent windowSize bars to compute rolling statistics
        List<Bar> window = bars.subList(bars.size() - windowSize, bars.size());

        // Step 1: Compute the rolling mean of close prices over the window
        double mean = window.stream()
                .mapToDouble(b -> b.close().doubleValue())
                .average()
                .orElse(0.0);

        // Step 2: Compute the population variance (average squared deviation from mean)
        double variance = window.stream()
                .mapToDouble(b -> Math.pow(b.close().doubleValue() - mean, 2))
                .average()
                .orElse(0.0);

        // Step 3: Standard deviation = sqrt(variance); guard against flat price series
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0.0) {
            return Optional.empty();
        }

        // Step 4: z-score = (currentClose - rollingMean) / rollingStdDev
        double zScore = (currentBar.close().doubleValue() - mean) / stdDev;
        boolean hasPosition = portfolio.getPositions().containsKey(currentBar.ticker());

        if (zScore < zScoreThreshold && !hasPosition) {
            BigDecimal strength = BigDecimal.valueOf(Math.abs(zScore)).setScale(6, RoundingMode.HALF_UP);
            return Optional.of(new SignalEvent(
                    currentBar.ticker(),
                    SignalDirection.LONG,
                    strength,
                    strategyId(),
                    Instant.now()
            ));
        }

        if (zScore >= EXIT_Z && hasPosition) {
            BigDecimal strength = BigDecimal.valueOf(Math.abs(zScore)).setScale(6, RoundingMode.HALF_UP);
            return Optional.of(new SignalEvent(
                    currentBar.ticker(),
                    SignalDirection.EXIT,
                    strength,
                    strategyId(),
                    Instant.now()
            ));
        }

        return Optional.empty();
    }

    /**
     * Returns a new {@link MeanReversionStrategy} instance with the given parameters applied.
     * Recognised keys:
     * <ul>
     *   <li>{@code "windowSize"} — integer rolling window length (defaults to current value if absent).</li>
     *   <li>{@code "zScoreThreshold"} — double entry threshold (defaults to current value if absent).</li>
     * </ul>
     *
     * @param params Map of parameter name to value.
     * @return A new {@code MeanReversionStrategy} with the overridden parameters.
     */
    @Override
    public Strategy withParameters(Map<String, Object> params) {
        int newWindow = params.containsKey("windowSize")
                ? ((Number) params.get("windowSize")).intValue()
                : this.windowSize;
        double newThreshold = params.containsKey("zScoreThreshold")
                ? ((Number) params.get("zScoreThreshold")).doubleValue()
                : this.zScoreThreshold;
        return new MeanReversionStrategy(newWindow, newThreshold);
    }
}
