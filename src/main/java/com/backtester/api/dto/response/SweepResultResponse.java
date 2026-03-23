package com.backtester.api.dto.response;

import java.util.List;

/**
 * Response for {@code POST /api/v1/backtests/sweep}.
 * Contains a ranked summary of all parameter combinations tried.
 *
 * @param strategyId        The strategy that was swept.
 * @param totalCombinations Total number of parameter combinations attempted.
 * @param successfulRuns    Number of combinations that completed without error.
 * @param results           Per-combination results sorted by Sharpe ratio descending (best first);
 *                          failed combinations appear at the end with null metric fields.
 */
public record SweepResultResponse(
    String strategyId,
    int totalCombinations,
    int successfulRuns,
    /** Results sorted by Sharpe ratio descending (best first). */
    List<SweepResultEntry> results
) {}
