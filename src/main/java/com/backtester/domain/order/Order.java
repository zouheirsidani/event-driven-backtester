package com.backtester.domain.order;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable snapshot of an order at a point in time.
 * In the current implementation orders are not persisted directly; only
 * their resulting {@link Fill} records are stored.  This record exists for
 * completeness and potential future order-management features.
 *
 * @param orderId    Unique identifier for this order.
 * @param ticker     Ticker to trade.
 * @param type       MARKET or LIMIT.
 * @param side       BUY or SELL.
 * @param quantity   Number of shares requested.
 * @param limitPrice Required only when {@code type} is LIMIT; otherwise null.
 * @param status     Current lifecycle state of the order.
 * @param createdAt  Time the order was created.
 */
public record Order(
        String orderId,
        String ticker,
        OrderType type,
        OrderSide side,
        int quantity,
        BigDecimal limitPrice,
        OrderStatus status,
        Instant createdAt
) {}
