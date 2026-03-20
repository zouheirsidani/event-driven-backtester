package com.backtester.strategy.momentum;

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
 * Momentum strategy using 20-day return as signal.
 * BUY when momentum is positive and no position held.
 * EXIT when momentum turns negative and position is held.
 */
@Component
public class MomentumStrategy implements Strategy {

    private static final int LOOKBACK = 20;

    @Override
    public String strategyId() {
        return "MOMENTUM_V1";
    }

    @Override
    public String displayName() {
        return "Momentum Strategy (20-day lookback)";
    }

    @Override
    public Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio) {
        List<Bar> bars = history.bars();

        // Need at least LOOKBACK + 1 bars to compute the return
        if (bars.size() < LOOKBACK + 1) {
            return Optional.empty();
        }

        Bar lookbackBar = bars.get(bars.size() - LOOKBACK - 1);
        BigDecimal lookbackPrice = lookbackBar.close();

        if (lookbackPrice.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        // 20-day momentum = (current - past) / past
        BigDecimal momentum = currentBar.close()
                .subtract(lookbackPrice)
                .divide(lookbackPrice, 6, RoundingMode.HALF_UP);

        boolean hasPosition = portfolio.getPositions().containsKey(currentBar.ticker());

        if (momentum.compareTo(BigDecimal.ZERO) > 0 && !hasPosition) {
            return Optional.of(new SignalEvent(
                    currentBar.ticker(),
                    SignalDirection.LONG,
                    momentum.abs(),
                    strategyId(),
                    Instant.now()
            ));
        }

        if (momentum.compareTo(BigDecimal.ZERO) < 0 && hasPosition) {
            return Optional.of(new SignalEvent(
                    currentBar.ticker(),
                    SignalDirection.EXIT,
                    momentum.abs(),
                    strategyId(),
                    Instant.now()
            ));
        }

        return Optional.empty();
    }
}
