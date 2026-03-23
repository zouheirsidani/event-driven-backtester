package com.backtester.api.dto.response;

/**
 * Response for {@code POST /api/v1/market-data/fetch}.
 * Reports how many bars were fetched from Yahoo Finance and how many were
 * actually persisted vs. skipped (already existed in the database).
 *
 * @param ticker       The ticker symbol that was fetched.
 * @param barsFetched  Total number of bars returned by Yahoo Finance.
 * @param barsSaved    Number of new bars persisted (duplicates excluded).
 * @param barsSkipped  Number of bars skipped because they already existed.
 */
public record FetchMarketDataResponse(
    String ticker,
    int barsFetched,
    int barsSaved,
    int barsSkipped
) {}
