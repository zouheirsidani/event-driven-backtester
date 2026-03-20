package com.backtester.api.controller;

import com.backtester.api.dto.response.StrategiesResponse;
import com.backtester.api.dto.response.StrategyDto;
import com.backtester.domain.strategy.Strategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/strategies")
public class StrategyController {

    private final List<Strategy> strategies;

    public StrategyController(List<Strategy> strategies) {
        this.strategies = strategies;
    }

    @GetMapping
    public StrategiesResponse listStrategies() {
        List<StrategyDto> dtos = strategies.stream()
                .map(s -> new StrategyDto(s.strategyId(), s.displayName()))
                .toList();
        return new StrategiesResponse(dtos);
    }
}
