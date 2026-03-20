package com.backtester.api.dto.response;

public record SymbolDto(
        String ticker,
        String name,
        String exchange,
        String assetClass
) {}
