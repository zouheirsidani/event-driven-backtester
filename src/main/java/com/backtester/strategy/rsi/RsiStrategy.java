package com.backtester.strategy.rsi;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mean-reversion strategy based on the Relative Strength Index (RSI).
 *
 * <p>RSI measures the magnitude of recent price gains versus losses over a rolling
 * window to identify overbought and oversold conditions:
 * <pre>
 *   RS  = avgGain / avgLoss (Wilder's smoothed average over {@code period} days)
 *   RSI = 100 - (100 / (1 + RS))
 * </pre>
 *
 * <p>Signal logic (evaluated once per day per ticker):
 * <ul>
 *   <li><b>BUY</b>: RSI &lt; {@code oversoldThreshold} — asset is oversold; no position held.</li>
 *   <li><b>EXIT</b>: RSI &gt; {@code overboughtThreshold} — asset is overbought; position held.</li>
 * </ul>
 *
 * <p>At least {@code period + 1} bars are required before any signal is generated.
 *
 * <p>Default parameters: period = 14, oversoldThreshold = 30, overboughtThreshold = 70.
 * Call {@link #withParameters(Map)} with {@code {"period": 14, "oversoldThreshold": 30,
 * "overboughtThreshold": 70}} to create a parameterised copy for sweep runs.
 */
@Component
public class RsiStrategy implements Strategy {

    /** Number of days in the RSI rolling window. */
    private final int period;

    /** RSI level below which a BUY signal is emitted (asset is oversold). */
    private final double oversoldThreshold;

    /** RSI level above which an EXIT signal is emitted (asset is overbought). */
    private final double overboughtThreshold;

    /**
     * Default constructor used by Spring for component scanning.
     * Sets period = 14, oversoldThreshold = 30, overboughtThreshold = 70.
     */
    public RsiStrategy() {
        this.period = 14;
        this.oversoldThreshold = 30.0;
        this.overboughtThreshold = 70.0;
    }

    /**
     * Package-private constructor for use by {@link #withParameters(Map)} only.
     * Not annotated with {@code @Component} — Spring must not create a second bean.
     *
     * @param period              RSI rolling window length in days.
     * @param oversoldThreshold   RSI threshold below which a BUY is emitted.
     * @param overboughtThreshold RSI threshold above which an EXIT is emitted.
     */
    RsiStrategy(int period, double oversoldThreshold, double overboughtThreshold) {
        this.period = period;
        this.oversoldThreshold = oversoldThreshold;
        this.overboughtThreshold = overboughtThreshold;
    }

    @Override
    public String strategyId() {
        return "RSI_V1";
    }

    @Override
    public String displayName() {
        return "RSI Strategy (14-day, oversold/overbought)";
    }

    /**
     * Computes the RSI for every ticker in the universe and emits signals where
     * the oversold or overbought thresholds are breached.
     *
     * @param date      The current trading date.
     * @param universe  Map of ticker to accumulated BarSeries up to and including today.
     * @param portfolio Used to check whether a position is already held per ticker.
     * @return List of LONG and EXIT signals for applicable tickers.
     */
    @Override
    public List<SignalEvent> onDay(LocalDate date, Map<String, BarSeries> universe, Portfolio portfolio) {
        List<SignalEvent> signals = new ArrayList<>();

        for (Map.Entry<String, BarSeries> entry : universe.entrySet()) {
            String ticker = entry.getKey();
            List<Bar> bars = entry.getValue().bars();

            // Need period + 1 bars to compute period price changes
            if (bars.size() < period + 1) continue;

            double rsi = computeRsi(bars);
            boolean hasPosition = portfolio.getPositions().containsKey(ticker)
                    && portfolio.getPositions().get(ticker).quantity() > 0;

            if (rsi < oversoldThreshold && !hasPosition) {
                // Oversold: price has fallen sharply, expect rebound
                BigDecimal strength = BigDecimal.valueOf(oversoldThreshold - rsi)
                        .setScale(4, RoundingMode.HALF_UP);
                signals.add(new SignalEvent(ticker, SignalDirection.LONG, strength, strategyId(), Instant.now()));
            } else if (rsi > overboughtThreshold && hasPosition) {
                // Overbought: price has risen sharply, take profit
                BigDecimal strength = BigDecimal.valueOf(rsi - overboughtThreshold)
                        .setScale(4, RoundingMode.HALF_UP);
                signals.add(new SignalEvent(ticker, SignalDirection.EXIT, strength, strategyId(), Instant.now()));
            }
        }

        return signals;
    }

    /**
     * Computes the RSI using Wilder's smoothed average method over the last
     * {@code period} daily price changes from the bar history.
     *
     * <p>Wilder's smoothing: the initial avgGain and avgLoss are simple averages
     * of the first {@code period} changes; subsequent values are exponentially
     * smoothed: {@code avgGain = (prevAvgGain × (period - 1) + gain) / period}.
     * This implementation applies a single-pass smoothed average over the available
     * price changes (last {@code period} changes), which is equivalent for the
     * lookback window used here.
     *
     * @param bars Bar history (must contain at least {@code period + 1} bars).
     * @return RSI value in [0, 100]; returns 100 if avgLoss is zero.
     */
    private double computeRsi(List<Bar> bars) {
        int start = bars.size() - period - 1;
        double avgGain = 0.0;
        double avgLoss = 0.0;

        // First pass: simple average over the period
        for (int i = start + 1; i <= start + period; i++) {
            double change = bars.get(i).close().doubleValue()
                    - bars.get(i - 1).close().doubleValue();
            if (change > 0) avgGain += change;
            else avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;

        if (avgLoss == 0.0) return 100.0;

        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    /**
     * Returns a new {@link RsiStrategy} instance with the given parameters applied.
     * Recognised keys:
     * <ul>
     *   <li>{@code "period"}              — integer RSI window (defaults to current if absent).</li>
     *   <li>{@code "oversoldThreshold"}   — double entry threshold (defaults to current if absent).</li>
     *   <li>{@code "overboughtThreshold"} — double exit threshold (defaults to current if absent).</li>
     * </ul>
     *
     * @param params Map of parameter name to value.
     * @return A new {@code RsiStrategy} with the overridden parameters.
     */
    @Override
    public Strategy withParameters(Map<String, Object> params) {
        int newPeriod = params.containsKey("period")
                ? ((Number) params.get("period")).intValue() : this.period;
        double newOversold = params.containsKey("oversoldThreshold")
                ? ((Number) params.get("oversoldThreshold")).doubleValue() : this.oversoldThreshold;
        double newOverbought = params.containsKey("overboughtThreshold")
                ? ((Number) params.get("overboughtThreshold")).doubleValue() : this.overboughtThreshold;
        return new RsiStrategy(newPeriod, newOversold, newOverbought);
    }
}
