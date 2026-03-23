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

/**
 * Converts between {@link BacktestResult} domain records and {@link BacktestResultEntity}
 * JPA entities.
 *
 * <p>The three structured fields ({@code metrics}, {@code equityCurve}, {@code trades})
 * are serialised/deserialised as JSON strings using Jackson.  The entity stores them as
 * PostgreSQL JSONB columns, with the {@code @ColumnTransformer(write = "?::jsonb")}
 * annotation performing the implicit type cast on every write.
 */
@Component
public class BacktestResultEntityMapper {

    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper Spring-provided Jackson {@link ObjectMapper}.
     */
    public BacktestResultEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts a domain {@link BacktestResult} to a {@link BacktestResultEntity}.
     * Serialises metrics, equity curve, and trades to JSON strings.
     *
     * @param result Domain result record.
     * @return JPA entity ready for persistence.
     * @throws RuntimeException if JSON serialisation fails.
     */
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

    /**
     * Converts a {@link BacktestResultEntity} loaded from the database back to a domain
     * {@link BacktestResult}.  Deserialises the JSON strings to typed domain objects.
     *
     * @param entity JPA entity.
     * @return Domain result record.
     * @throws RuntimeException if JSON deserialisation fails.
     */
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
