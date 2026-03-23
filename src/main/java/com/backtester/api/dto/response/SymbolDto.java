package com.backtester.api.dto.response;

/**
 * API response DTO for a registered symbol.
 * The {@code assetClass} field is the enum name string rather than the enum type
 * to keep the API response decoupled from the domain enum.
 *
 * @param ticker     Uppercase ticker symbol.
 * @param name       Company or instrument name.
 * @param exchange   Exchange name.
 * @param assetClass Asset class as a string (e.g. "STOCK", "ETF").
 */
public record SymbolDto(
        String ticker,
        String name,
        String exchange,
        String assetClass
) {}
