package com.backtester.domain.order;

/**
 * Indicates whether an order purchases or sells shares.
 * Slippage direction is determined by this field: BUY orders fill slightly
 * above the close price, SELL orders slightly below.
 */
public enum OrderSide {
    /** Purchase shares, increasing the position and reducing cash. */
    BUY,
    /** Sell shares, decreasing the position and increasing cash. */
    SELL
}
