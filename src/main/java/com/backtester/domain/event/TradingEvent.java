package com.backtester.domain.event;

/**
 * Sealed marker interface for every event type that flows through the
 * {@code EventLoop}'s {@code ArrayDeque}.  The sealed hierarchy enables
 * exhaustive {@code switch} expressions across all permitted subtypes,
 * preventing unhandled-case bugs at compile time.
 *
 * <p>Processing order within a single trading day:
 * {@link MarketDataEvent} → {@link SignalEvent} → {@link OrderEvent} → {@link FillEvent}
 */
public sealed interface TradingEvent
        permits MarketDataEvent, SignalEvent, OrderEvent, FillEvent {}
