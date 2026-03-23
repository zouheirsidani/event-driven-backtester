package com.backtester.api.mapper;

import com.backtester.api.dto.request.CreateSymbolRequest;
import com.backtester.api.dto.response.SymbolDto;
import com.backtester.domain.market.AssetClass;
import com.backtester.domain.market.Symbol;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Symbol} domain records and their API DTO/request equivalents.
 */
@Component
public class SymbolDtoMapper {

    /**
     * Converts a {@link CreateSymbolRequest} to a domain {@link Symbol}.
     * Normalises ticker and exchange to uppercase, and parses the assetClass string
     * to the {@link com.backtester.domain.market.AssetClass} enum.
     *
     * @param request Validated API request.
     * @return Domain symbol record.
     */
    public Symbol toDomain(CreateSymbolRequest request) {
        return new Symbol(
                request.ticker().toUpperCase(),
                request.name(),
                request.exchange().toUpperCase(),
                AssetClass.valueOf(request.assetClass().toUpperCase())
        );
    }

    /**
     * Converts a domain {@link Symbol} to a {@link SymbolDto} for API responses.
     * The {@code assetClass} enum is converted to its string name.
     *
     * @param symbol Domain symbol record.
     * @return API response DTO.
     */
    public SymbolDto toDto(Symbol symbol) {
        return new SymbolDto(
                symbol.ticker(),
                symbol.name(),
                symbol.exchange(),
                symbol.assetClass().name()
        );
    }
}
