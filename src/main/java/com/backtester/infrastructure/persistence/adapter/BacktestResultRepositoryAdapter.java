package com.backtester.infrastructure.persistence.adapter;

import com.backtester.application.port.BacktestResultRepository;
import com.backtester.domain.backtest.BacktestResult;
import com.backtester.infrastructure.persistence.mapper.BacktestResultEntityMapper;
import com.backtester.infrastructure.persistence.repository.BacktestResultJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure adapter implementing the {@link BacktestResultRepository} port.
 * Persists structured result data (metrics, equity curve, trades) as JSONB
 * columns by delegating serialisation to {@code BacktestResultEntityMapper}.
 */
@Repository
public class BacktestResultRepositoryAdapter implements BacktestResultRepository {

    private final BacktestResultJpaRepository jpaRepository;
    private final BacktestResultEntityMapper mapper;

    public BacktestResultRepositoryAdapter(BacktestResultJpaRepository jpaRepository,
                                            BacktestResultEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public BacktestResult save(BacktestResult result) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(result)));
    }

    @Override
    public Optional<BacktestResult> findByRunId(UUID runId) {
        return jpaRepository.findById(runId).map(mapper::toDomain);
    }
}
