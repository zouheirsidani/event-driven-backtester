package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API response DTO representing a backtest run.
 * Mirrors the {@link com.backtester.domain.backtest.BacktestRun} domain record
 * but uses serialisation-friendly types (e.g. {@code String} for status).
 *
 * @param runId           Unique run identifier.
 * @param strategyId      Strategy used for this run.
 * @param tickers         Tickers included in the simulation.
 * @param startDate       Simulation start date.
 * @param endDate         Simulation end date.
 * @param initialCash     Starting capital.
 * @param status          Current lifecycle status as a string (PENDING / RUNNING / COMPLETED / FAILED).
 * @param createdAt       Timestamp when the run was submitted.
 * @param benchmarkTicker Optional benchmark ticker; null if none was specified.
 */
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
