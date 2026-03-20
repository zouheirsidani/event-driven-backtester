package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeDto(
        String fillId,
        String ticker,
        String side,
        int quantity,
        BigDecimal price,
        BigDecimal commission,
        LocalDate date
) {}
