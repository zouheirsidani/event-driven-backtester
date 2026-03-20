package com.backtester.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record PortfolioSnapshot(
        LocalDate date,
        BigDecimal totalEquity,
        BigDecimal cash,
        Map<String, Position> positions
) {}
