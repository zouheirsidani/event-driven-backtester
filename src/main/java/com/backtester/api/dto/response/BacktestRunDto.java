package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BacktestRunDto(
        UUID runId,
        String strategyId,
        List<String> tickers,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal initialCash,
        String status,
        Instant createdAt,
        String benchmarkTicker
) {}
