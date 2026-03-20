package com.backtester.domain.order;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Fill(
        String fillId,
        String orderId,
        String ticker,
        OrderSide side,
        int quantityFilled,
        BigDecimal fillPrice,
        BigDecimal commission,
        LocalDate date
) {}
