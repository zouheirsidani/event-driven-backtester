package com.backtester.domain.event;

import com.backtester.domain.order.OrderSide;
import com.backtester.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderEvent(
        String orderId,
        String ticker,
        OrderType type,
        OrderSide side,
        int quantity,
        BigDecimal limitPrice,
        Instant timestamp
) implements TradingEvent {}
