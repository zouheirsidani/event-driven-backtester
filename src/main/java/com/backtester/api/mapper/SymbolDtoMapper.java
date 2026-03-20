package com.backtester.api.mapper;

import com.backtester.api.dto.request.CreateSymbolRequest;
import com.backtester.api.dto.response.SymbolDto;
import com.backtester.domain.market.AssetClass;
import com.backtester.domain.market.Symbol;
import org.springframework.stereotype.Component;

@Component
public class SymbolDtoMapper {

    public Symbol toDomain(CreateSymbolRequest request) {
        return new Symbol(
                request.ticker().toUpperCase(),
                request.name(),
                request.exchange().toUpperCase(),
                AssetClass.valueOf(request.assetClass().toUpperCase())
        );
    }

    public SymbolDto toDto(Symbol symbol) {
        return new SymbolDto(
                symbol.ticker(),
                symbol.name(),
                symbol.exchange(),
                symbol.assetClass().name()
        );
    }
}
