package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API response DTO for a single OHLCV bar.
 * Mirrors the {@link com.backtester.domain.market.Bar} domain record for serialisation.
 *
 * @param ticker Ticker symbol.
 * @param date   Trading date.
 * @param open   Opening price.
 * @param high   Intraday high.
 * @param low    Intraday low.
 * @param close  Closing price.
 * @param volume Share/contract volume.
 */
public record BarDto(
        String ticker,
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {}
