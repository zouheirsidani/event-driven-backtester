package com.backtester.domain.event;

import com.backtester.domain.order.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Confirms that an {@link OrderEvent} was executed at a specific price.
 * The fill price already includes slippage; commission is recorded separately.
 * The portfolio's {@code applyFill()} method consumes this event to update
 * cash and positions.
 *
 * @param fillId         Unique identifier for this fill (random UUID string).
 * @param orderId        ID of the originating order.
 * @param ticker         Ticker that was traded.
 * @param side           BUY or SELL.
 * @param quantityFilled Number of shares actually filled.
 * @param fillPrice      Execution price after slippage has been applied.
 * @param commission     Brokerage commission charged for this fill.
 * @param timestamp      Wall-clock time when the fill was created.
 */
public record FillEvent(
        String fillId,
        String orderId,
        String ticker,
        OrderSide side,
        int quantityFilled,
        BigDecimal fillPrice,
        BigDecimal commission,
        Instant timestamp
) implements TradingEvent {}
