package com.backtester.domain.market;

import java.util.List;

/**
 * An ordered sequence of daily {@link Bar} records for a single ticker.
 * Passed to {@code Strategy.onDay()} (as part of the universe map) as the historical
 * context accumulated up to and including the current trading day.
 *
 * @param ticker Uppercase ticker symbol for all bars in this series.
 * @param bars   Bars in ascending chronological order.
 */
public record BarSeries(
        String ticker,
        List<Bar> bars
) {}
