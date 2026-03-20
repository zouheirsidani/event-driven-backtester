package com.backtester.domain.market;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Bar(
        String ticker,
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {}
