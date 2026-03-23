package com.backtester.api.dto.response;

import java.util.List;

/**
 * Response wrapper for {@code GET /api/v1/market-data/symbols}.
 *
 * @param symbols List of all registered symbol DTOs.
 * @param count   Total number of symbols returned.
 */
public record SymbolsResponse(List<SymbolDto> symbols, int count) {}
