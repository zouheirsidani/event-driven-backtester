package com.backtester.domain.strategy;

import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.portfolio.Portfolio;

import java.util.Optional;

/**
 * Contract that every trading strategy must implement.
 * Implementations are discovered automatically via Spring's {@code List<Strategy>}
 * injection — simply annotate a class with {@code @Component} and implement this
 * interface to register a new strategy.
 *
 * <p>The domain layer intentionally has no Spring or JPA dependencies, so
 * implementations are free to use any pure-Java logic.
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
     * Called once per ticker per trading day by the event loop.
     * The strategy may inspect the price history and current portfolio state
     * to decide whether to emit a signal.
     *
     * @param history    All bars accumulated for this ticker up to and including today.
     * @param currentBar The bar for today (also the last element of {@code history.bars()}).
     * @param portfolio  Read-only view of current cash and positions.
     * @return An optional signal; empty means no action for this bar.
     */
    Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio);
}
