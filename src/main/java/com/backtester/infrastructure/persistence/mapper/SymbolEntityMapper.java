package com.backtester.infrastructure.persistence.mapper;

import com.backtester.domain.market.AssetClass;
import com.backtester.domain.market.Symbol;
import com.backtester.infrastructure.persistence.entity.SymbolEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Converts between {@link Symbol} domain records and {@link SymbolEntity} JPA entities.
 * Sets {@code createdAt} to the current instant on every save since the domain record
 * does not carry a creation timestamp.
 */
@Component
public class SymbolEntityMapper {

    /**
     * Converts a domain {@link Symbol} to a new {@link SymbolEntity} ready for persistence.
     * The {@code createdAt} timestamp is set to the current instant.
     *
     * @param symbol Domain record.
     * @return Corresponding JPA entity.
     */
    public SymbolEntity toEntity(Symbol symbol) {
        return new SymbolEntity(
                symbol.ticker(),
                symbol.name(),
                symbol.exchange(),
                symbol.assetClass().name(),
                Instant.now()
        );
    }

    /**
     * Converts a persisted {@link SymbolEntity} back to a domain {@link Symbol}.
     * Parses the stored asset class string to the {@link com.backtester.domain.market.AssetClass} enum.
     *
     * @param entity JPA entity loaded from the database.
     * @return Domain record.
     */
    public Symbol toDomain(SymbolEntity entity) {
        return new Symbol(
                entity.getTicker(),
                entity.getName(),
                entity.getExchange(),
                AssetClass.valueOf(entity.getAssetClass())
        );
    }
}
