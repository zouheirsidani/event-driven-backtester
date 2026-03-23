package com.backtester.domain.order;

/**
 * Specifies how an order should be filled.
 * In V1 the event loop only executes MARKET orders against the closing price.
 */
public enum OrderType {
    /** Execute immediately at the prevailing market (close) price. */
    MARKET,
    /** Execute only if the price reaches the specified limit — not supported in V1. */
    LIMIT
}
