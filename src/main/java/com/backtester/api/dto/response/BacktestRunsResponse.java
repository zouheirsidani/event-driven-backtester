package com.backtester.api.dto.response;

import java.util.List;

/**
 * Paginated response for {@code GET /api/v1/backtests}.
 *
 * @param runs       Backtest run DTOs for the current page.
 * @param count      Number of runs in this page (≤ size).
 * @param totalCount Total number of runs across all pages (for client-side pagination).
 * @param page       Zero-based page index that was requested.
 * @param size       Maximum runs per page that was requested.
 */
public record BacktestRunsResponse(List<BacktestRunDto> runs, int count, long totalCount, int page, int size) {}
