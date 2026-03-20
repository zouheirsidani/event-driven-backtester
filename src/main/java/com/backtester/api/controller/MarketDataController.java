package com.backtester.api.controller;

import com.backtester.api.dto.request.CreateSymbolRequest;
import com.backtester.api.dto.request.IngestBarsRequest;
import com.backtester.api.dto.response.BarsResponse;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market-data")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final SymbolDtoMapper symbolMapper;
    private final BarDtoMapper barMapper;

    public MarketDataController(MarketDataService marketDataService,
                                 SymbolDtoMapper symbolMapper,
                                 BarDtoMapper barMapper) {
        this.marketDataService = marketDataService;
        this.symbolMapper = symbolMapper;
        this.barMapper = barMapper;
    }

    @PostMapping("/symbols")
    public ResponseEntity<SymbolDto> createSymbol(@Valid @RequestBody CreateSymbolRequest request) {
        var symbol = marketDataService.registerSymbol(symbolMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(symbolMapper.toDto(symbol));
    }

    @GetMapping("/symbols")
    public SymbolsResponse listSymbols() {
        var symbols = marketDataService.listSymbols().stream().map(symbolMapper::toDto).toList();
        return new SymbolsResponse(symbols, symbols.size());
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingestBars(@Valid @RequestBody IngestBarsRequest request) {
        String ticker = request.ticker().toUpperCase();
        List<Bar> inputBars = barMapper.toDomain(ticker, request.bars());
        List<Bar> ingested = marketDataService.ingestBars(ticker, inputBars);
        int skipped = request.bars().size() - ingested.size();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IngestResponse(ticker, ingested.size(), skipped));
    }

    @GetMapping("/{ticker}/bars")
    public BarsResponse queryBars(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String upperTicker = ticker.toUpperCase();
        // Validate symbol exists
        try {
            marketDataService.getSymbol(upperTicker);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        var bars = marketDataService.queryBars(upperTicker, from, to)
                .stream().map(barMapper::toDto).toList();
        return new BarsResponse(upperTicker, bars, bars.size());
    }
}
