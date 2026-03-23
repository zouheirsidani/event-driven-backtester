package com.backtester.infrastructure.persistence.mapper;

import com.backtester.domain.backtest.BacktestRun;
import com.backtester.domain.backtest.BacktestStatus;
import com.backtester.domain.backtest.CommissionModel;
import com.backtester.domain.backtest.SlippageModel;
import com.backtester.infrastructure.persistence.entity.BacktestRunEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link BacktestRun} domain records and {@link BacktestRunEntity} JPA entities.
 *
 * <p>The polymorphic {@link com.backtester.domain.backtest.SlippageModel} and
 * {@link com.backtester.domain.backtest.CommissionModel} fields are serialised to
 * JSON strings using Jackson.  They carry {@code @JsonTypeInfo} / {@code @JsonSubTypes}
 * annotations so that the concrete subtype is embedded in the JSON as a {@code "type"} field,
 * enabling polymorphic deserialization when reading back from the JSONB column.
 */
@Component
public class BacktestRunEntityMapper {

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper Spring-provided Jackson {@link ObjectMapper}.
     */
    public BacktestRunEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts a domain {@link BacktestRun} to a {@link BacktestRunEntity}.
     * Serialises the slippage and commission models to JSON strings.
     *
     * @param run Domain record.
     * @return JPA entity ready for persistence.
     * @throws RuntimeException if JSON serialisation fails.
     */
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
                    run.createdAt(),
                    run.benchmarkTicker(),
                    run.sweepId()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize backtest run", e);
        }
    }

    /**
     * Converts a {@link BacktestRunEntity} loaded from the database back to a domain
     * {@link BacktestRun}.  Deserialises the JSON slippage and commission config strings
     * using the {@code @JsonTypeInfo} discriminator to reconstruct the correct concrete type.
     *
     * @param entity JPA entity.
     * @return Domain record.
     * @throws RuntimeException if JSON deserialisation fails.
     */
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
                    entity.getCreatedAt(),
                    entity.getBenchmarkTicker(),
                    entity.getSweepId()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize backtest run", e);
        }
    }
}
