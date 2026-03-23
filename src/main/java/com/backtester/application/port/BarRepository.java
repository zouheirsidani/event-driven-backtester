package com.backtester.application.port;

import com.backtester.domain.market.Bar;

import java.time.LocalDate;
import java.util.List;

/**
 * Application-layer port for persisting and querying OHLCV bar data.
 * Implemented by {@code BarRepositoryAdapter} in the infrastructure layer.
 * Application services depend on this interface, never on the JPA implementation.
 */
public interface BarRepository {

    /**
     * Persists a batch of bars, returning the saved instances.
     *
     * @param bars Bars to save.
     * @return Saved bar list (may include generated IDs).
     */
    List<Bar> saveAll(List<Bar> bars);

    /**
     * Fetches all bars for a ticker within the given date range, inclusive.
     *
     * @param ticker Ticker symbol.
     * @param from   Start date (inclusive).
     * @param to     End date (inclusive).
     * @return Bars ordered by date ascending.
     */
    List<Bar> findByTickerAndDateRange(String ticker, LocalDate from, LocalDate to);

    /**
     * Fetches a page of bars for a ticker within the given date range.
     *
     * @param ticker Ticker symbol.
     * @param from   Start date (inclusive).
     * @param to     End date (inclusive).
     * @param page   Zero-based page index.
     * @param size   Maximum number of bars to return.
     * @return Bars for the requested page, ordered by date ascending.
     */
    List<Bar> findByTickerAndDateRange(String ticker, LocalDate from, LocalDate to, int page, int size);

    /**
     * Counts the total number of bars for a ticker within the given date range.
     *
     * @param ticker Ticker symbol.
     * @param from   Start date (inclusive).
     * @param to     End date (inclusive).
     * @return Total bar count for pagination metadata.
     */
    long countByTickerAndDateRange(String ticker, LocalDate from, LocalDate to);

    /**
     * Checks whether a bar already exists for the given ticker and date.
     * Used during ingestion to avoid duplicate entries.
     *
     * @param ticker Ticker symbol.
     * @param date   Date to check.
     * @return {@code true} if a bar already exists for this ticker/date combination.
     */
    boolean existsByTickerAndDate(String ticker, LocalDate date);
}
