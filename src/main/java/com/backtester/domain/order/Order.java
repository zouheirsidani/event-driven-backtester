package com.backtester.domain.order;

import java.math.BigDecimal;
import java.time.Instant;

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
