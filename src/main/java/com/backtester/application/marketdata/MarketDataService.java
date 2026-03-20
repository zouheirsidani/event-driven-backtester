package com.backtester.application.marketdata;

import com.backtester.application.port.BarRepository;
import com.backtester.application.port.SymbolRepository;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.Symbol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MarketDataService {

    private final SymbolRepository symbolRepository;
    private final BarRepository barRepository;

    public MarketDataService(SymbolRepository symbolRepository, BarRepository barRepository) {
        this.symbolRepository = symbolRepository;
        this.barRepository = barRepository;
    }

    @Transactional
    public Symbol registerSymbol(Symbol symbol) {
        if (symbolRepository.existsByTicker(symbol.ticker())) {
            throw new IllegalArgumentException("Symbol already exists: " + symbol.ticker());
        }
        return symbolRepository.save(symbol);
    }

    public List<Symbol> listSymbols() {
        return symbolRepository.findAll();
    }

    public Symbol getSymbol(String ticker) {
        return symbolRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + ticker));
    }

    @Transactional
    public List<Bar> ingestBars(String ticker, List<Bar> bars) {
        if (!symbolRepository.existsByTicker(ticker)) {
            throw new IllegalArgumentException("Symbol not registered: " + ticker);
        }
        // Filter out duplicates
        List<Bar> newBars = bars.stream()
                .filter(bar -> !barRepository.existsByTickerAndDate(bar.ticker(), bar.date()))
                .toList();
        return barRepository.saveAll(newBars);
    }

    public List<Bar> queryBars(String ticker, LocalDate from, LocalDate to) {
        return barRepository.findByTickerAndDateRange(ticker, from, to);
    }
}
