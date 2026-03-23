package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/v1/market-data/symbols}.
 * Registers a new tradable symbol in the system.
 *
 * @param ticker     Unique ticker symbol (e.g. "AAPL"); normalised to uppercase on save.
 * @param name       Full company or instrument name (e.g. "Apple Inc.").
 * @param exchange   Exchange name (e.g. "NASDAQ"); normalised to uppercase on save.
 * @param assetClass One of "STOCK", "ETF", "FUTURES", "CRYPTO" (case-insensitive).
 */
public record CreateSymbolRequest(
        @NotBlank(message = "ticker is required") String ticker,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "exchange is required") String exchange,
        @NotNull(message = "assetClass is required") String assetClass
) {}
