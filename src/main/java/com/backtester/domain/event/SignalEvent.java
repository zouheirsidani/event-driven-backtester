package com.backtester.domain.event;

import com.backtester.domain.strategy.SignalDirection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Emitted by a strategy to express a directional trading intent for a ticker.
 * The {@code PositionSizer} consumes this event and converts it into an
 * {@link OrderEvent} with a concrete share quantity.
 *
 * @param ticker      Ticker symbol this signal applies to.
 * @param direction   LONG to open a position, EXIT to close, SHORT is a no-op in V1.
 * @param strength    Normalised signal magnitude (e.g. absolute z-score or momentum).
 *                    Used for informational purposes; sizing is fixed-fractional regardless.
 * @param strategyId  Identifier of the strategy that generated this signal.
 * @param timestamp   Wall-clock time when the signal was generated.
 */
public record SignalEvent(
        String ticker,
        SignalDirection direction,
        BigDecimal strength,
        String strategyId,
        Instant timestamp
) implements TradingEvent {}
