package com.backtester.domain.strategy;

import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.portfolio.Portfolio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Contract that every trading strategy must implement.
 * Implementations are discovered automatically via Spring's {@code List<Strategy>}
 * injection — simply annotate a class with {@code @Component} and implement this
 * interface to register a new strategy.
 *
 * <p>The domain layer intentionally has no Spring or JPA dependencies, so
 * implementations are free to use any pure-Java logic.
 *
 * <p>Strategies receive the full universe of tickers for each day, enabling
 * cross-ticker logic such as regime filters, pairs trading, and sector rotation.
 */
public interface Strategy {

    /**
     * Unique machine-readable identifier for this strategy (e.g. "MOMENTUM_V1").
     * Used to look up the strategy when a backtest run is executed.
     *
     * @return non-null, non-blank strategy identifier string.
     */
    String strategyId();

    /**
     * Human-readable name shown in the UI and strategy list endpoint.
     *
     * @return display name for this strategy.
     */
    String displayName();

    /**
     * Called once per trading day by the event loop with data for all tickers.
     * The strategy may inspect any ticker in the universe and emit signals for any subset.
     *
     * @param date     The current trading date.
     * @param universe Map of ticker to accumulated BarSeries up to and including today.
     *                 Only tickers that have a bar on this date are present in the map.
     * @param portfolio Read-only view of current cash and positions.
     * @return List of signals (zero or more); each signal carries its own ticker field.
     */
    List<SignalEvent> onDay(LocalDate date, Map<String, BarSeries> universe, Portfolio portfolio);

    /**
     * Returns a new strategy instance configured with the given parameter map.
     * Each strategy decides which keys it recognises; unknown keys are ignored.
     * The default implementation is a no-op that returns {@code this}, so strategies
     * that do not support parameter overrides do not need to override this method.
     *
     * @param params Map of parameter name to value (e.g. {@code {"lookbackDays": 30}}).
     * @return A (possibly new) strategy instance with the parameters applied.
     */
    default Strategy withParameters(Map<String, Object> params) {
        return this;
    }
}
