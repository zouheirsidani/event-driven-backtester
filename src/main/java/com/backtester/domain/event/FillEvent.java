package com.backtester.domain.event;

import com.backtester.domain.order.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;

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
