package com.backtester.domain.market;

public record Symbol(
        String ticker,
        String name,
        String exchange,
        AssetClass assetClass
) {}
