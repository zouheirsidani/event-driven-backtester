package com.backtester.application.marketdata;

import com.backtester.application.port.BarRepository;
import com.backtester.application.port.SymbolRepository;
import com.backtester.application.port.YahooFinanceClient;
import com.backtester.domain.market.AssetClass;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service for managing market data: registering symbols, ingesting
 * price bars (via JSON body or CSV upload), and querying stored bars.
 *
 * <p>Symbols must be registered before bars can be ingested for them.
 * Ingestion is idempotent — duplicate bars (same ticker + date) are silently skipped.
 */
@Service
public class MarketDataService {

    private final SymbolRepository symbolRepository;
    private final BarRepository barRepository;
    private final YahooFinanceClient yahooFinanceClient;

    /**
     * @param symbolRepository   Port for persisting and querying symbols.
     * @param barRepository      Port for persisting and querying bar data.
     * @param yahooFinanceClient Port for fetching live market data from Yahoo Finance.
     */
    public MarketDataService(SymbolRepository symbolRepository,
                              BarRepository barRepository,
                              YahooFinanceClient yahooFinanceClient) {
        this.symbolRepository = symbolRepository;
        this.barRepository = barRepository;
        this.yahooFinanceClient = yahooFinanceClient;
    }

    /**
     * Registers a new symbol in the system.
     *
     * @param symbol Symbol to register.
     * @return The persisted symbol.
     * @throws IllegalArgumentException if a symbol with the same ticker already exists.
     */
    @Transactional
    public Symbol registerSymbol(Symbol symbol) {
        if (symbolRepository.existsByTicker(symbol.ticker())) {
            throw new IllegalArgumentException("Symbol already exists: " + symbol.ticker());
        }
        return symbolRepository.save(symbol);
    }

    /**
     * Returns all registered symbols.
     *
     * @return All symbols in the system.
     */
    public List<Symbol> listSymbols() {
        return symbolRepository.findAll();
    }

    /**
     * Fetches a registered symbol by ticker.
     *
     * @param ticker Uppercase ticker symbol.
     * @return The symbol.
     * @throws IllegalArgumentException if no symbol is registered for the given ticker.
     */
    public Symbol getSymbol(String ticker) {
        return symbolRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + ticker));
    }

    /**
     * Ingests a list of bars from a JSON request body.  Duplicate bars (same
     * ticker + date) are filtered out before saving.
     *
     * @param ticker Uppercase ticker symbol (must already be registered).
     * @param bars   Bars to ingest.
     * @return Only the newly saved bars (duplicates excluded).
     * @throws IllegalArgumentException if the ticker is not registered.
     */
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

    /**
     * Returns all bars for a ticker in the given date range without pagination.
     *
     * @param ticker Uppercase ticker symbol.
     * @param from   Start date (inclusive).
     * @param to     End date (inclusive).
     * @return Bars ordered by date ascending.
     */
    public List<Bar> queryBars(String ticker, LocalDate from, LocalDate to) {
        return barRepository.findByTickerAndDateRange(ticker, from, to);
    }

    /**
     * Returns a page of bars for a ticker in the given date range.
     *
     * @param ticker Uppercase ticker symbol.
     * @param from   Start date (inclusive).
     * @param to     End date (inclusive).
     * @param page   Zero-based page index.
     * @param size   Maximum bars per page.
     * @return Bars for the requested page.
     */
    public List<Bar> queryBars(String ticker, LocalDate from, LocalDate to, int page, int size) {
        return barRepository.findByTickerAndDateRange(ticker, from, to, page, size);
    }

    /**
     * Counts bars for a ticker in the given date range (for pagination metadata).
     *
     * @param ticker Uppercase ticker symbol.
     * @param from   Start date (inclusive).
     * @param to     End date (inclusive).
     * @return Total bar count for the query.
     */
    public long countBars(String ticker, LocalDate from, LocalDate to) {
        return barRepository.countByTickerAndDateRange(ticker, from, to);
    }

    /**
     * Fetches daily OHLCV bars from Yahoo Finance and persists any that are not
     * already in the database.
     *
     * <p>If the ticker is not yet registered as a symbol, it is auto-registered
     * with a placeholder name equal to the ticker and asset class {@code STOCK}.
     * Deduplication is performed by comparing each fetched bar's date against the
     * set of dates already stored for this ticker in the requested range.
     *
     * @param ticker Uppercase ticker symbol (e.g. "AAPL").
     * @param from   Start date (inclusive).
     * @param to     End date (inclusive).
     * @return A {@link FetchResult} carrying counts of fetched, saved, and skipped bars.
     */
    @Transactional
    public FetchResult fetchAndSaveFromYahoo(String ticker, LocalDate from, LocalDate to) {
        // Auto-register the symbol if it doesn't exist
        if (!symbolRepository.existsByTicker(ticker)) {
            symbolRepository.save(new Symbol(ticker, ticker, ticker, AssetClass.STOCK));
        }

        // Fetch from Yahoo Finance
        List<Bar> fetched = yahooFinanceClient.fetchDailyBars(ticker, from, to);

        // Collect dates that are already stored in the requested range to deduplicate
        Set<LocalDate> existingDates = barRepository.findByTickerAndDateRange(ticker, from, to)
                .stream()
                .map(Bar::date)
                .collect(Collectors.toSet());

        List<Bar> newBars = fetched.stream()
                .filter(bar -> !existingDates.contains(bar.date()))
                .toList();

        barRepository.saveAll(newBars);

        int saved = newBars.size();
        int skipped = fetched.size() - saved;
        return new FetchResult(fetched.size(), saved, skipped);
    }

    /**
     * Result of a Yahoo Finance fetch operation.
     *
     * @param fetched  Total bars returned by Yahoo Finance.
     * @param saved    New bars persisted (duplicates excluded).
     * @param skipped  Bars skipped because they already existed.
     */
    public record FetchResult(int fetched, int saved, int skipped) {}
}
