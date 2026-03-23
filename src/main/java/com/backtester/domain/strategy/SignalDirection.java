package com.backtester.domain.strategy;

/**
 * The directional intent expressed by a strategy in a {@code SignalEvent}.
 * Only LONG and EXIT are fully implemented in V1; SHORT is a no-op placeholder.
 */
public enum SignalDirection {
    /** Open or add to a long (buy) position. */
    LONG,
    /** Open or add to a short (sell) position — not implemented in V1. */
    SHORT,
    /** Close an existing position entirely. */
    EXIT
}
