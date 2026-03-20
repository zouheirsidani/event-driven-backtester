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
 * Mean reversion strategy using Bollinger Band z-score.
 * BUY when z-score < -2 (price 2 std devs below 20-day mean) and no position held.
 * EXIT when z-score >= 0 (price reverts to mean) and position is held.
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

    @Override
    public Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio) {
        List<Bar> bars = history.bars();
        if (bars.size() < WINDOW) {
            return Optional.empty();
        }

        List<Bar> window = bars.subList(bars.size() - WINDOW, bars.size());

        double mean = window.stream()
                .mapToDouble(b -> b.close().doubleValue())
                .average()
                .orElse(0.0);

        double variance = window.stream()
                .mapToDouble(b -> Math.pow(b.close().doubleValue() - mean, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        if (stdDev == 0.0) {
            return Optional.empty();
        }

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
