package com.backtester.domain.market;

/**
 * Classifies the type of financial instrument represented by a {@link Symbol}.
 * Used for informational purposes and future routing/filtering logic.
 */
public enum AssetClass {
    /** Common or preferred shares traded on an exchange. */
    STOCK,
    /** Exchange-traded funds backed by a basket of assets. */
    ETF,
    /** Standardised futures contracts for commodities or financial indices. */
    FUTURES,
    /** Cryptocurrencies and digital tokens. */
    CRYPTO
}
