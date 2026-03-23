package com.backtester.api.controller;

import com.backtester.api.dto.request.CreateSymbolRequest;
import com.backtester.api.dto.request.FetchMarketDataRequest;
import com.backtester.api.dto.request.IngestBarsRequest;
import com.backtester.api.dto.response.BarsResponse;
import com.backtester.api.dto.response.FetchMarketDataResponse;
import com.backtester.api.dto.response.IngestResponse;
import com.backtester.api.dto.response.SymbolDto;
import com.backtester.api.dto.response.SymbolsResponse;
import com.backtester.api.exception.ResourceNotFoundException;
import com.backtester.api.mapper.BarDtoMapper;
import com.backtester.api.mapper.SymbolDtoMapper;
import com.backtester.application.marketdata.MarketDataService;
import com.backtester.domain.market.Bar;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for market data management under {@code /api/v1/market-data}.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /symbols}         — Register a new symbol.</li>
 *   <li>{@code GET /symbols}          — List all registered symbols.</li>
 *   <li>{@code POST /ingest}          — Ingest bars via JSON body.</li>
 *   <li>{@code POST /ingest/csv}      — Bulk ingest bars from a CSV file upload.</li>
 *   <li>{@code POST /fetch}           — Fetch live bars from Yahoo Finance and persist them.</li>
 *   <li>{@code GET /{ticker}/bars}    — Query price bars with date range and pagination.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/market-data")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final SymbolDtoMapper symbolMapper;
    private final BarDtoMapper barMapper;

    /**
     * @param marketDataService Application service for symbols and bars.
     * @param symbolMapper      Mapper between symbol domain objects and DTOs.
     * @param barMapper         Mapper between bar domain objects and DTOs.
     */
    public MarketDataController(MarketDataService marketDataService,
                                 SymbolDtoMapper symbolMapper,
                                 BarDtoMapper barMapper) {
        this.marketDataService = marketDataService;
        this.symbolMapper = symbolMapper;
        this.barMapper = barMapper;
    }

    /**
     * Registers a new symbol.  Symbols must be registered before bar data can
     * be ingested for them.
     *
     * @param request Validated symbol creation request.
     * @return 201 Created with the symbol DTO.
     */
    @PostMapping("/symbols")
    public ResponseEntity<SymbolDto> createSymbol(@Valid @RequestBody CreateSymbolRequest request) {
        var symbol = marketDataService.registerSymbol(symbolMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(symbolMapper.toDto(symbol));
    }

    /**
     * Returns all symbols registered in the system.
     *
     * @return Wrapper containing the symbol list and count.
     */
    @GetMapping("/symbols")
    public SymbolsResponse listSymbols() {
        var symbols = marketDataService.listSymbols().stream().map(symbolMapper::toDto).toList();
        return new SymbolsResponse(symbols, symbols.size());
    }

    /**
     * Ingests OHLCV bars provided as a JSON array.
     * Duplicate bars (same ticker + date) are skipped and counted in {@code barsSkipped}.
     *
     * @param request Validated ingest request containing ticker and bar list.
     * @return 201 Created with ingestion summary.
     */
    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingestBars(@Valid @RequestBody IngestBarsRequest request) {
        String ticker = request.ticker().toUpperCase();
        List<Bar> inputBars = barMapper.toDomain(ticker, request.bars());
        List<Bar> ingested = marketDataService.ingestBars(ticker, inputBars);
        int skipped = request.bars().size() - ingested.size();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IngestResponse(ticker, ingested.size(), skipped));
    }

    /**
     * Bulk-ingests OHLCV bars from an uploaded CSV file.
     * Expected CSV columns (with header row): {@code date,open,high,low,close,volume}.
     * Malformed rows are silently skipped.  Duplicates are ignored.
     *
     * @param ticker Ticker symbol for all bars in the file (query parameter).
     * @param file   Multipart CSV file upload.
     * @return 201 Created with the count of newly saved bars.
     * @throws IOException if the uploaded file cannot be read.
     */
    @PostMapping("/ingest/csv")
    public ResponseEntity<IngestResponse> ingestCsv(
            @RequestParam String ticker,
            @RequestParam("file") MultipartFile file) throws IOException {
        String upperTicker = ticker.toUpperCase();
        List<Bar> ingested = marketDataService.ingestBarsFromCsv(upperTicker, file.getInputStream());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IngestResponse(upperTicker, ingested.size(), 0));
    }

    /**
     * Fetches daily OHLCV bars from Yahoo Finance for the given ticker and date range,
     * persisting only bars that are not already in the database.
     * Auto-registers the symbol as a STOCK if it is not yet known to the system.
     *
     * @param request Validated fetch request containing the ticker and date range.
     * @return 200 OK with a summary of bars fetched, saved, and skipped.
     */
    @PostMapping("/fetch")
    public ResponseEntity<FetchMarketDataResponse> fetchFromYahoo(
            @Valid @RequestBody FetchMarketDataRequest request) {
        String upperTicker = request.ticker().toUpperCase();
        MarketDataService.FetchResult result = marketDataService.fetchAndSaveFromYahoo(
                upperTicker, request.startDate(), request.endDate());
        return ResponseEntity.ok(new FetchMarketDataResponse(
                upperTicker, result.fetched(), result.saved(), result.skipped()));
    }

    /**
     * Queries stored price bars for a registered ticker within a date range.
     * Supports pagination via {@code page} and {@code size} query parameters.
     *
     * @param ticker Ticker symbol (case-insensitive; normalised to uppercase).
     * @param from   Start date in ISO 8601 format (e.g. "2022-01-01").
     * @param to     End date in ISO 8601 format (e.g. "2022-12-31").
     * @param page   Zero-based page index (default 0).
     * @param size   Maximum bars per page (default 100).
     * @return Paginated bar response including total count.
     * @throws ResourceNotFoundException if the ticker is not registered.
     */
    @GetMapping("/{ticker}/bars")
    public BarsResponse queryBars(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        String upperTicker = ticker.toUpperCase();
        try {
            marketDataService.getSymbol(upperTicker);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        List<Bar> bars = marketDataService.queryBars(upperTicker, from, to, page, size);
        long totalCount = marketDataService.countBars(upperTicker, from, to);
        var barDtos = bars.stream().map(barMapper::toDto).toList();
        return new BarsResponse(upperTicker, barDtos, barDtos.size(), totalCount, page, size);
    }
}
