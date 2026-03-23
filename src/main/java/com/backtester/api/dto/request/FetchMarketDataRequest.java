package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/market-data/fetch}.
 * Triggers a live fetch of historical daily OHLCV bars from Yahoo Finance,
 * auto-registering the symbol if it is not already in the system,
 * and persisting only bars that are not already stored.
 *
 * @param ticker    The ticker symbol to fetch (e.g. "AAPL").
 * @param startDate Start date (inclusive).
 * @param endDate   End date (inclusive).
 */
public record FetchMarketDataRequest(
    /** The ticker symbol to fetch (e.g. "AAPL"). Must not be blank. */
    @NotBlank String ticker,
    /** Start date (inclusive). Must not be null. */
    @NotNull LocalDate startDate,
    /** End date (inclusive). Must not be null. */
    @NotNull LocalDate endDate
) {}
