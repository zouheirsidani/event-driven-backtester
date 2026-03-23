package com.backtester.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for {@code POST /api/v1/market-data/ingest}.
 * Carries a batch of OHLCV bars to be stored for a given ticker.
 *
 * @param ticker Uppercase ticker symbol (must already be registered).
 * @param bars   Non-empty list of bar data records to ingest.
 */
public record IngestBarsRequest(
        @NotBlank(message = "ticker is required") String ticker,
        @NotEmpty(message = "bars must not be empty") @Valid List<BarData> bars
) {

    /**
     * Individual OHLCV bar data as provided by the API caller.
     *
     * @param date   Trading date for this bar.
     * @param open   Opening price; must be positive.
     * @param high   Intraday high price; must be positive.
     * @param low    Intraday low price; must be positive.
     * @param close  Closing price; must be positive.
     * @param volume Share volume; must be positive.
     */
    public record BarData(
            @NotNull(message = "date is required") LocalDate date,
            @NotNull @Positive BigDecimal open,
            @NotNull @Positive BigDecimal high,
            @NotNull @Positive BigDecimal low,
            @NotNull @Positive BigDecimal close,
            @Positive long volume
    ) {}
}
