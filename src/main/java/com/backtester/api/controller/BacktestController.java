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

/**
 * REST controller exposing backtest lifecycle endpoints under {@code /api/v1/backtests}.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /}            — Submit a new backtest (returns 202 Accepted).</li>
 *   <li>{@code GET /}             — List all runs with pagination.</li>
 *   <li>{@code GET /{runId}}      — Get a single run by ID.</li>
 *   <li>{@code GET /{runId}/results} — Get the result for a completed run.</li>
 *   <li>{@code POST /compare}     — Fetch and compare results for multiple runs.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/backtests")
public class BacktestController {

    private final BacktestService backtestService;
    private final BacktestDtoMapper mapper;

    /**
     * @param backtestService Application service for backtest lifecycle.
     * @param mapper          Mapper that converts domain objects to response DTOs.
     */
    public BacktestController(BacktestService backtestService, BacktestDtoMapper mapper) {
        this.backtestService = backtestService;
        this.mapper = mapper;
    }

    /**
     * Submits a new backtest run.  The run is persisted immediately and execution
     * starts asynchronously on a background thread.
     *
     * @param request Validated backtest configuration.
     * @return 202 Accepted with the created run DTO.
     */
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

    /**
     * Returns a paginated list of all backtest runs.
     *
     * @param page Zero-based page index (default 0).
     * @param size Maximum runs per page (default 20).
     * @return Paginated response including total count for client-side pagination.
     */
    @GetMapping
    public BacktestRunsResponse listBacktests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<BacktestRunDto> runs = backtestService.listRuns(page, size).stream()
                .map(mapper::toRunDto).toList();
        long totalCount = backtestService.countRuns();
        return new BacktestRunsResponse(runs, runs.size(), totalCount, page, size);
    }

    /**
     * Retrieves a single backtest run by its UUID.
     *
     * @param runId UUID path variable.
     * @return Run DTO.
     * @throws ResourceNotFoundException if no run with the given ID exists.
     */
    @GetMapping("/{runId}")
    public BacktestRunDto getBacktestRun(@PathVariable UUID runId) {
        return backtestService.getRun(runId)
                .map(mapper::toRunDto)
                .orElseThrow(() -> new ResourceNotFoundException("Backtest run not found: " + runId));
    }

    /**
     * Retrieves the performance results for a completed backtest run.
     *
     * @param runId UUID path variable.
     * @return Result response including metrics, equity curve, and trade list.
     * @throws ResourceNotFoundException if results are not yet available (run pending/running/failed).
     */
    @GetMapping("/{runId}/results")
    public BacktestResultResponse getBacktestResult(@PathVariable UUID runId) {
        return backtestService.getResult(runId)
                .map(mapper::toResultResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Results not available yet for run: " + runId));
    }

    /**
     * Fetches and returns the results of multiple backtest runs side by side.
     * Runs whose results are not yet available are silently omitted from the response.
     *
     * @param request List of run IDs to compare.
     * @return Wrapper containing an ordered list of result responses.
     */
    @PostMapping("/compare")
    public CompareBacktestsResponse compareBacktests(@Valid @RequestBody CompareBacktestsRequest request) {
        List<BacktestResultResponse> results = request.runIds().stream()
                .flatMap(id -> backtestService.getResult(id).stream())
                .map(mapper::toResultResponse)
                .toList();
        return new CompareBacktestsResponse(results);
    }
}
