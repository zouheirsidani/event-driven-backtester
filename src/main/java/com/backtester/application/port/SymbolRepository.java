package com.backtester.application.port;

import com.backtester.domain.market.Symbol;

import java.util.List;
import java.util.Optional;

public interface SymbolRepository {

    Symbol save(Symbol symbol);

    Optional<Symbol> findByTicker(String ticker);

    List<Symbol> findAll();

    boolean existsByTicker(String ticker);
}
