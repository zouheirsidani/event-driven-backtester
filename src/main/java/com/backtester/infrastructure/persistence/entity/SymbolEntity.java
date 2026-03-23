package com.backtester.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity mapping to the {@code symbols} table.
 * The ticker is the primary key (no surrogate ID needed since it is inherently unique).
 * Domain-to-entity mapping is performed by {@code SymbolEntityMapper}.
 */
@Entity
@Table(name = "symbols")
public class SymbolEntity {

    @Id
    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String exchange;

    @Column(name = "asset_class", nullable = false)
    private String assetClass;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SymbolEntity() {}

    public SymbolEntity(String ticker, String name, String exchange, String assetClass, Instant createdAt) {
        this.ticker = ticker;
        this.name = name;
        this.exchange = exchange;
        this.assetClass = assetClass;
        this.createdAt = createdAt;
    }

    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public String getExchange() { return exchange; }
    public String getAssetClass() { return assetClass; }
    public Instant getCreatedAt() { return createdAt; }
}
