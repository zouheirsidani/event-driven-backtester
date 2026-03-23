package com.backtester.domain.event;

import com.backtester.domain.market.Bar;

import java.time.Instant;

/**
 * Signals that new price data has arrived for a ticker on a given trading day.
 * This is the first event emitted per ticker in each iteration of the event loop.
 * Strategies are called immediately after this event is queued.
 *
 * @param bar       The OHLCV bar for the ticker on this trading day.
 * @param timestamp Wall-clock time when the event was created (simulation time).
 */
public record MarketDataEvent(
        Bar bar,
        Instant timestamp
) implements TradingEvent {}
