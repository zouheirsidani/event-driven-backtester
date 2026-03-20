package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BarDto(
        String ticker,
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {}
