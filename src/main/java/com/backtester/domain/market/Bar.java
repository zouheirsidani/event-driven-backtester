package com.backtester.domain.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable OHLCV (Open/High/Low/Close/Volume) price bar for a single trading day.
 * This is the fundamental market data unit consumed by strategies and the event loop.
 *
 * @param ticker  Uppercase ticker symbol the bar belongs to.
 * @param date    Trading date this bar represents.
 * @param open    Opening price for the session.
 * @param high    Intraday high price.
 * @param low     Intraday low price.
 * @param close   Closing price; used as the reference price for order execution.
 * @param volume  Number of shares/contracts traded during the session.
 */
public record Bar(
        String ticker,
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {}
