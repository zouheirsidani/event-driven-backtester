package com.backtester.strategy.macrossover;

import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.portfolio.Portfolio;
import com.backtester.domain.strategy.SignalDirection;
import com.backtester.domain.strategy.Strategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trend-following strategy based on a dual Simple Moving Average (SMA) crossover.
 *
 * <p>Two SMAs are computed daily — a faster short-period SMA and a slower long-period SMA:
 * <pre>
 *   sma(bars, window) = mean of the last {@code window} close prices
 * </pre>
 *
 * <p>Signal logic (crossover detected by comparing today's MAs with yesterday's):
 * <ul>
 *   <li><b>BUY (golden cross)</b>: short SMA crosses <em>above</em> long SMA — uptrend begins;
 *       no existing position.</li>
 *   <li><b>EXIT (death cross)</b>: short SMA crosses <em>below</em> long SMA — downtrend begins;
 *       position held.</li>
 * </ul>
 *
 * <p>At least {@code longWindow + 1} bars are required before any signal is generated
 * (the extra bar is needed to detect a crossover from the previous day's MAs).
 *
 * <p>Default parameters: shortWindow = 50, longWindow = 200 (the classic "50/200" crossover).
 * Call {@link #withParameters(Map)} with {@code {"shortWindow": 20, "longWindow": 50}}
 * to create a parameterised copy for sweep runs.
 */
@Component
public class MovingAverageCrossoverStrategy implements Strategy {

    /** Number of days in the fast (short) moving average. */
    private final int shortWindow;

    /** Number of days in the slow (long) moving average. */
    private final int longWindow;

    /**
     * Default constructor used by Spring for component scanning.
     * Sets shortWindow = 50, longWindow = 200.
     */
    public MovingAverageCrossoverStrategy() {
        this.shortWindow = 50;
        this.longWindow = 200;
    }

    /**
     * Package-private constructor for use by {@link #withParameters(Map)} only.
     * Not annotated with {@code @Component} — Spring must not create a second bean.
     *
     * @param shortWindow Number of days in the fast SMA.
     * @param longWindow  Number of days in the slow SMA.
     */
    MovingAverageCrossoverStrategy(int shortWindow, int longWindow) {
        this.shortWindow = shortWindow;
        this.longWindow = longWindow;
    }

    @Override
    public String strategyId() {
        return "MA_CROSSOVER_V1";
    }

    @Override
    public String displayName() {
        return "Moving Average Crossover (50/200 SMA golden/death cross)";
    }

    /**
     * Detects SMA crossovers for every ticker in the universe and emits entry/exit signals.
     *
     * <p>A crossover is detected by comparing the relationship of the two SMAs today
     * versus the prior day. This avoids re-signalling on an already-crossed condition.
     *
     * @param date      The current trading date.
     * @param universe  Map of ticker to accumulated BarSeries up to and including today.
     * @param portfolio Used to check whether a position is already held per ticker.
     * @return List of LONG (golden cross) and EXIT (death cross) signals for applicable tickers.
     */
    @Override
    public List<SignalEvent> onDay(LocalDate date, Map<String, BarSeries> universe, Portfolio portfolio) {
        List<SignalEvent> signals = new ArrayList<>();

        for (Map.Entry<String, BarSeries> entry : universe.entrySet()) {
            String ticker = entry.getKey();
            List<Bar> bars = entry.getValue().bars();

            // Need longWindow + 1 bars: longWindow for today's MA + 1 for yesterday's MA
            if (bars.size() < longWindow + 1) continue;

            // Today's MAs
            double shortMaToday = sma(bars, bars.size(), shortWindow);
            double longMaToday  = sma(bars, bars.size(), longWindow);

            // Yesterday's MAs (using bars up to but not including today)
            double shortMaYest = sma(bars, bars.size() - 1, shortWindow);
            double longMaYest  = sma(bars, bars.size() - 1, longWindow);

            boolean hasPosition = portfolio.getPositions().containsKey(ticker)
                    && portfolio.getPositions().get(ticker).quantity() > 0;

            // Golden cross: short crossed above long since yesterday
            if (!hasPosition && shortMaYest <= longMaYest && shortMaToday > longMaToday) {
                signals.add(new SignalEvent(ticker, SignalDirection.LONG,
                        BigDecimal.ONE, strategyId(), Instant.now()));
            }
            // Death cross: short crossed below long since yesterday
            else if (hasPosition && shortMaYest >= longMaYest && shortMaToday < longMaToday) {
                signals.add(new SignalEvent(ticker, SignalDirection.EXIT,
                        BigDecimal.ONE, strategyId(), Instant.now()));
            }
        }

        return signals;
    }

    /**
     * Computes the Simple Moving Average of the last {@code window} close prices
     * in {@code bars[0..endIndex-1]}.
     *
     * @param bars     Full bar list.
     * @param endIndex Exclusive upper bound — use {@code bars.size()} for today's MA
     *                 and {@code bars.size() - 1} for yesterday's.
     * @param window   Number of bars to average.
     * @return SMA value.
     */
    private double sma(List<Bar> bars, int endIndex, int window) {
        double sum = 0.0;
        int start = endIndex - window;
        for (int i = start; i < endIndex; i++) {
            sum += bars.get(i).close().doubleValue();
        }
        return sum / window;
    }

    /**
     * Returns a new {@link MovingAverageCrossoverStrategy} instance with the given parameters applied.
     * Recognised keys:
     * <ul>
     *   <li>{@code "shortWindow"} — integer fast SMA window (defaults to current if absent).</li>
     *   <li>{@code "longWindow"}  — integer slow SMA window (defaults to current if absent).</li>
     * </ul>
     *
     * @param params Map of parameter name to value.
     * @return A new {@code MovingAverageCrossoverStrategy} with the overridden parameters.
     */
    @Override
    public Strategy withParameters(Map<String, Object> params) {
        int newShort = params.containsKey("shortWindow")
                ? ((Number) params.get("shortWindow")).intValue() : this.shortWindow;
        int newLong = params.containsKey("longWindow")
                ? ((Number) params.get("longWindow")).intValue() : this.longWindow;
        return new MovingAverageCrossoverStrategy(newShort, newLong);
    }
}
