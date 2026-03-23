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
import java.util.Optional;

/**
 * Counter-trend strategy based on the Bollinger Band z-score.
 *
 * <p>The z-score measures how many standard deviations the current close price
 * is above or below the 20-day rolling mean:
 * <pre>  z = (currentClose - mean20) / stdDev20</pre>
 *
 * <p>Signal logic:
 * <ul>
 *   <li><b>BUY</b>: z &lt; {@code ENTRY_Z} (-2.0) — price is statistically cheap;
 *       no position held.</li>
 *   <li><b>EXIT</b>: z &ge; {@code EXIT_Z} (0.0) — price has reverted to the mean
 *       (or above); a position is held.</li>
 * </ul>
 *
 * <p>At least 20 bars are required to compute a non-trivial mean and standard
 * deviation, so the strategy is silent during the warm-up period.
 */
@Component
public class MeanReversionStrategy implements Strategy {

    private static final int WINDOW = 20;
    private static final double ENTRY_Z = -2.0;
    private static final double EXIT_Z = 0.0;

    @Override
    public String strategyId() {
        return "MEAN_REVERSION_V1";
    }

    @Override
    public String displayName() {
        return "Mean Reversion Strategy (Bollinger Band / z-score, 20-day window)";
    }

    /**
     * Computes the 20-day rolling z-score and emits a signal if entry or exit
     * thresholds are crossed.
     *
     * @param history    All bars for this ticker up to today.
     * @param currentBar Today's bar.
     * @param portfolio  Used to check whether a position is already held.
     * @return A LONG signal when the price is 2+ std devs below the mean and no
     *         position is held; an EXIT signal when price reverts to mean or above
     *         and a position is held; empty otherwise.
     */
    @Override
    public Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio) {
        List<Bar> bars = history.bars();
        // Require a full WINDOW of bars before calculating statistics
        if (bars.size() < WINDOW) {
            return Optional.empty();
        }

        // Use only the most recent WINDOW bars to compute rolling statistics
        List<Bar> window = bars.subList(bars.size() - WINDOW, bars.size());

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

        if (zScore < ENTRY_Z && !hasPosition) {
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
}
