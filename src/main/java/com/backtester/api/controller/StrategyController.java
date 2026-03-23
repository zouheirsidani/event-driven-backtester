package com.backtester.api.controller;

import com.backtester.api.dto.response.StrategiesResponse;
import com.backtester.api.dto.response.StrategyDto;
import com.backtester.domain.strategy.Strategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller that exposes the list of available trading strategies.
 * Any class annotated with {@code @Component} that implements the {@link Strategy}
 * interface is automatically included in this list by Spring injection.
 *
 * <p>Endpoint: {@code GET /api/v1/strategies}
 */
@RestController
@RequestMapping("/api/v1/strategies")
public class StrategyController {

    private final List<Strategy> strategies;

    /**
     * @param strategies All {@link Strategy} beans registered in the Spring context.
     */
    public StrategyController(List<Strategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * Returns all available strategy identifiers and display names.
     * The front-end populates the "Run Backtest" strategy drop-down from this endpoint.
     *
     * @return Wrapper containing all strategy DTOs.
     */
    @GetMapping
    public StrategiesResponse listStrategies() {
        List<StrategyDto> dtos = strategies.stream()
                .map(s -> new StrategyDto(s.strategyId(), s.displayName()))
                .toList();
        return new StrategiesResponse(dtos);
    }
}
