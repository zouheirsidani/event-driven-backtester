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
import java.util.Map;
import java.util.Optional;

/**
 * Trend-following strategy that uses the N-day price return as a momentum signal.
 *
 * <p>Signal logic (evaluated once per bar, per ticker):
 * <ul>
 *   <li><b>BUY</b>: N-day momentum &gt; 0 and no existing position.</li>
 *   <li><b>EXIT</b>: N-day momentum &lt; 0 and an existing position is held.</li>
 * </ul>
 *
 * <p>At least {@code lookbackDays + 1} bars of history are required before any signal
 * is generated, so the strategy is silent during the warm-up period.
 *
 * <p>The default lookback is 20 days.  Call {@link #withParameters(Map)} with
 * {@code {"lookbackDays": N}} to create a parameterised copy for sweep runs.
 */
@Component
public class MomentumStrategy implements Strategy {

    /** Number of days to look back when computing the momentum return. */
    private final int lookbackDays;

    /**
     * Default constructor used by Spring for component scanning.
     * Sets {@code lookbackDays} to 20.
     */
    public MomentumStrategy() {
        this.lookbackDays = 20;
    }

    /**
     * Package-private constructor for use by {@link #withParameters(Map)} only.
     * Not annotated with {@code @Component} — Spring must not create a second bean.
     *
     * @param lookbackDays Number of days for the momentum lookback window.
     */
    MomentumStrategy(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    @Override
    public String strategyId() {
        return "MOMENTUM_V1";
    }

    @Override
    public String displayName() {
        return "Momentum Strategy (20-day lookback)";
    }

    /**
     * Evaluates the N-day momentum and emits a signal if entry or exit conditions are met.
     *
     * @param history    All bars accumulated for this ticker up to today.
     * @param currentBar Today's bar (also the last element in history).
     * @param portfolio  Used to check whether a position is already held.
     * @return A LONG signal if momentum is positive and no position; EXIT signal if
     *         momentum is negative and a position is held; empty otherwise.
     */
    @Override
    public Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio) {
        List<Bar> bars = history.bars();

        // Need at least lookbackDays + 1 bars to compute the return
        if (bars.size() < lookbackDays + 1) {
            return Optional.empty();
        }

        Bar lookbackBar = bars.get(bars.size() - lookbackDays - 1);
        BigDecimal lookbackPrice = lookbackBar.close();

        if (lookbackPrice.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        // N-day momentum = (currentClose - priceNDaysAgo) / priceNDaysAgo
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

    /**
     * Returns a new {@link MomentumStrategy} instance with the given parameters applied.
     * Recognised keys:
     * <ul>
     *   <li>{@code "lookbackDays"} — integer lookback window (defaults to current value if absent).</li>
     * </ul>
     *
     * @param params Map of parameter name to value.
     * @return A new {@code MomentumStrategy} with the overridden parameters, or {@code this}
     *         if no recognised keys were found.
     */
    @Override
    public Strategy withParameters(Map<String, Object> params) {
        if (params.containsKey("lookbackDays")) {
            return new MomentumStrategy(((Number) params.get("lookbackDays")).intValue());
        }
        return this;
    }
}
