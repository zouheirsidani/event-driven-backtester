package com.backtester.api.dto.response;

import java.util.List;

/**
 * Paginated response for {@code GET /api/v1/market-data/{ticker}/bars}.
 *
 * @param ticker     Ticker symbol for all bars in this response.
 * @param bars       Bar DTOs for the current page.
 * @param count      Number of bars in this page.
 * @param totalCount Total bars matching the query (for client-side pagination).
 * @param page       Zero-based page index that was requested.
 * @param size       Maximum bars per page that was requested.
 */
public record BarsResponse(String ticker, List<BarDto> bars, int count, long totalCount, int page, int size) {}
