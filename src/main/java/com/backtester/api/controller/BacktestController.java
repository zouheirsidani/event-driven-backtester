package com.backtester.api.controller;

import com.backtester.api.dto.request.CompareBacktestsRequest;
import com.backtester.api.dto.request.RunBacktestRequest;
import com.backtester.api.dto.response.BacktestResultResponse;
import com.backtester.api.dto.response.BacktestRunDto;
import com.backtester.api.dto.response.BacktestRunsResponse;
import com.backtester.api.dto.response.CompareBacktestsResponse;
import com.backtester.api.exception.ResourceNotFoundException;
import com.backtester.api.mapper.BacktestDtoMapper;
import com.backtester.application.backtest.BacktestService;
import com.backtester.domain.backtest.BacktestRun;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backtests")
public class BacktestController {

    private final BacktestService backtestService;
    private final BacktestDtoMapper mapper;

    public BacktestController(BacktestService backtestService, BacktestDtoMapper mapper) {
        this.backtestService = backtestService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<BacktestRunDto> submitBacktest(@Valid @RequestBody RunBacktestRequest request) {
        BacktestRun run = backtestService.submit(
                request.strategyId(),
                request.tickers(),
                request.startDate(),
                request.endDate(),
                request.initialCash(),
                request.slippageType(),
                request.slippageAmount(),
                request.commissionType(),
                request.commissionAmount(),
                request.benchmarkTicker()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toRunDto(run));
    }

    @GetMapping
    public BacktestRunsResponse listBacktests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<BacktestRunDto> runs = backtestService.listRuns(page, size).stream()
                .map(mapper::toRunDto).toList();
        long totalCount = backtestService.countRuns();
        return new BacktestRunsResponse(runs, runs.size(), totalCount, page, size);
    }

    @GetMapping("/{runId}")
    public BacktestRunDto getBacktestRun(@PathVariable UUID runId) {
        return backtestService.getRun(runId)
                .map(mapper::toRunDto)
                .orElseThrow(() -> new ResourceNotFoundException("Backtest run not found: " + runId));
    }

    @GetMapping("/{runId}/results")
    public BacktestResultResponse getBacktestResult(@PathVariable UUID runId) {
        return backtestService.getResult(runId)
                .map(mapper::toResultResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Results not available yet for run: " + runId));
    }

    @PostMapping("/compare")
    public CompareBacktestsResponse compareBacktests(@Valid @RequestBody CompareBacktestsRequest request) {
        List<BacktestResultResponse> results = request.runIds().stream()
                .flatMap(id -> backtestService.getResult(id).stream())
                .map(mapper::toResultResponse)
                .toList();
        return new CompareBacktestsResponse(results);
    }
}
