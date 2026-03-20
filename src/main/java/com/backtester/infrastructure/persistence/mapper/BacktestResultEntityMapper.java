package com.backtester.infrastructure.persistence.mapper;

import com.backtester.domain.backtest.BacktestResult;
import com.backtester.domain.backtest.EquityCurvePoint;
import com.backtester.domain.backtest.PerformanceMetrics;
import com.backtester.domain.order.Fill;
import com.backtester.infrastructure.persistence.entity.BacktestResultEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BacktestResultEntityMapper {

    private final ObjectMapper objectMapper;

    public BacktestResultEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BacktestResultEntity toEntity(BacktestResult result) {
        try {
            return new BacktestResultEntity(
                    result.runId(),
                    objectMapper.writeValueAsString(result.metrics()),
                    objectMapper.writeValueAsString(result.equityCurve()),
                    objectMapper.writeValueAsString(result.trades()),
                    result.completedAt()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize backtest result", e);
        }
    }

    public BacktestResult toDomain(BacktestResultEntity entity) {
        try {
            PerformanceMetrics metrics = objectMapper.readValue(entity.getMetrics(), PerformanceMetrics.class);
            List<EquityCurvePoint> equityCurve = objectMapper.readValue(
                    entity.getEquityCurve(), new TypeReference<>() {});
            List<Fill> trades = objectMapper.readValue(entity.getTrades(), new TypeReference<>() {});
            return new BacktestResult(
                    entity.getRunId(),
                    metrics,
                    equityCurve,
                    trades,
                    entity.getCompletedAt()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize backtest result", e);
        }
    }
}
