package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RunBacktestRequest(
        @NotBlank(message = "strategyId is required") String strategyId,
        @NotEmpty(message = "tickers must not be empty") List<String> tickers,
        @NotNull(message = "startDate is required") LocalDate startDate,
        @NotNull(message = "endDate is required") LocalDate endDate,
        @NotNull @Positive BigDecimal initialCash,
        String slippageType,
        BigDecimal slippageAmount,
        String commissionType,
        BigDecimal commissionAmount,
        String benchmarkTicker
) {}
