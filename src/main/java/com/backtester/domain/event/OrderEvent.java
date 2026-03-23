package com.backtester.domain.event;

import com.backtester.domain.order.OrderSide;
import com.backtester.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a sized order ready for execution, produced by {@code PositionSizer}
 * from a {@link SignalEvent}.  The event loop converts this into a
 * {@link FillEvent} after applying slippage.
 *
 * @param orderId    Unique identifier for this order (random UUID string).
 * @param ticker     Ticker to trade.
 * @param type       MARKET or LIMIT; only MARKET orders are executed in V1.
 * @param side       BUY or SELL.
 * @param quantity   Number of shares to trade.
 * @param limitPrice Limit price (null for market orders).
 * @param timestamp  Time the order was created.
 */
public record OrderEvent(
        String orderId,
        String ticker,
        OrderType type,
        OrderSide side,
        int quantity,
        BigDecimal limitPrice,
        Instant timestamp
) implements TradingEvent {}
