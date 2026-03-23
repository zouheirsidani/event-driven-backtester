package com.backtester.application.port;

import com.backtester.domain.market.Symbol;

import java.util.List;
import java.util.Optional;

/**
 * Application-layer port for persisting and querying registered symbols.
 * Implemented by {@code SymbolRepositoryAdapter} in the infrastructure layer.
 */
public interface SymbolRepository {

    /**
     * Persists a new symbol.
     *
     * @param symbol Symbol to save.
     * @return The saved symbol.
     */
    Symbol save(Symbol symbol);

    /**
     * Looks up a symbol by its ticker.
     *
     * @param ticker Uppercase ticker symbol.
     * @return The symbol, or empty if not registered.
     */
    Optional<Symbol> findByTicker(String ticker);

    /**
     * Returns all registered symbols.
     *
     * @return All symbols in the system.
     */
    List<Symbol> findAll();

    /**
     * Returns {@code true} if a symbol with the given ticker is already registered.
     *
     * @param ticker Uppercase ticker symbol.
     * @return {@code true} if the symbol exists.
     */
    boolean existsByTicker(String ticker);
}
