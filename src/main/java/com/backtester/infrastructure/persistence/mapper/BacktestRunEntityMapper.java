package com.backtester.infrastructure.persistence.mapper;

import com.backtester.domain.backtest.BacktestRun;
import com.backtester.domain.backtest.BacktestStatus;
import com.backtester.domain.backtest.CommissionModel;
import com.backtester.domain.backtest.SlippageModel;
import com.backtester.infrastructure.persistence.entity.BacktestRunEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class BacktestRunEntityMapper {

    private final ObjectMapper objectMapper;

    public BacktestRunEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BacktestRunEntity toEntity(BacktestRun run) {
        try {
            return new BacktestRunEntity(
                    run.runId(),
                    run.strategyId(),
                    run.tickers(),
                    run.startDate(),
                    run.endDate(),
                    run.initialCash(),
                    objectMapper.writeValueAsString(run.slippageModel()),
                    objectMapper.writeValueAsString(run.commissionModel()),
                    run.status().name(),
                    run.createdAt()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize backtest run", e);
        }
    }

    public BacktestRun toDomain(BacktestRunEntity entity) {
        try {
            SlippageModel slippageModel = objectMapper.readValue(entity.getSlippageConfig(), SlippageModel.class);
            CommissionModel commissionModel = objectMapper.readValue(entity.getCommissionConfig(), CommissionModel.class);
            return new BacktestRun(
                    entity.getRunId(),
                    entity.getStrategyId(),
                    entity.getTickers(),
                    entity.getStartDate(),
                    entity.getEndDate(),
                    entity.getInitialCash(),
                    slippageModel,
                    commissionModel,
                    BacktestStatus.valueOf(entity.getStatus()),
                    entity.getCreatedAt()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize backtest run", e);
        }
    }
}
