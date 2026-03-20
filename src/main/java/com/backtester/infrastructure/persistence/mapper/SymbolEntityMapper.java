package com.backtester.infrastructure.persistence.mapper;

import com.backtester.domain.market.AssetClass;
import com.backtester.domain.market.Symbol;
import com.backtester.infrastructure.persistence.entity.SymbolEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SymbolEntityMapper {

    public SymbolEntity toEntity(Symbol symbol) {
        return new SymbolEntity(
                symbol.ticker(),
                symbol.name(),
                symbol.exchange(),
                symbol.assetClass().name(),
                Instant.now()
        );
    }

    public Symbol toDomain(SymbolEntity entity) {
        return new Symbol(
                entity.getTicker(),
                entity.getName(),
                entity.getExchange(),
                AssetClass.valueOf(entity.getAssetClass())
        );
    }
}
