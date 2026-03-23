package com.backtester.domain.market;

/**
 * Represents a tradable financial instrument registered in the system.
 * A symbol must be registered before bar data can be ingested for it.
 *
 * @param ticker     Unique uppercase ticker symbol (e.g. "AAPL").
 * @param name       Human-readable company or instrument name.
 * @param exchange   Exchange where the instrument is listed (e.g. "NASDAQ").
 * @param assetClass Classification of the instrument type.
 */
public record Symbol(
        String ticker,
        String name,
        String exchange,
        AssetClass assetClass
) {}
