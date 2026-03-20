package com.backtester.application.marketdata;

import com.backtester.application.port.BarRepository;
import com.backtester.application.port.SymbolRepository;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.Symbol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
        List<Bar> newBars = bars.stream()
                .filter(bar -> !barRepository.existsByTickerAndDate(bar.ticker(), bar.date()))
                .toList();
        return barRepository.saveAll(newBars);
    }

    /**
     * Parses a CSV stream with header row: date,open,high,low,close,volume
     * Skips malformed rows silently. Deduplicates against existing data.
     */
    @Transactional
    public List<Bar> ingestBarsFromCsv(String ticker, InputStream csvStream) throws IOException {
        if (!symbolRepository.existsByTicker(ticker)) {
            throw new IllegalArgumentException("Symbol not registered: " + ticker);
        }

        List<Bar> parsed = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {
            String line = reader.readLine(); // skip header row
            if (line == null) {
                return List.of();
            }
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 6) continue;
                try {
                    LocalDate date = LocalDate.parse(cols[0].trim());
                    BigDecimal open = new BigDecimal(cols[1].trim());
                    BigDecimal high = new BigDecimal(cols[2].trim());
                    BigDecimal low = new BigDecimal(cols[3].trim());
                    BigDecimal close = new BigDecimal(cols[4].trim());
                    long volume = Long.parseLong(cols[5].trim());
                    parsed.add(new Bar(ticker, date, open, high, low, close, volume));
                } catch (Exception ignored) {
                    // skip malformed rows
                }
            }
        }

        List<Bar> newBars = parsed.stream()
                .filter(bar -> !barRepository.existsByTickerAndDate(bar.ticker(), bar.date()))
                .toList();
        return barRepository.saveAll(newBars);
    }

    public List<Bar> queryBars(String ticker, LocalDate from, LocalDate to) {
        return barRepository.findByTickerAndDateRange(ticker, from, to);
    }

    public List<Bar> queryBars(String ticker, LocalDate from, LocalDate to, int page, int size) {
        return barRepository.findByTickerAndDateRange(ticker, from, to, page, size);
    }

    public long countBars(String ticker, LocalDate from, LocalDate to) {
        return barRepository.countByTickerAndDateRange(ticker, from, to);
    }
}
