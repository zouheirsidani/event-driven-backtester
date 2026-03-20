package com.backtester.infrastructure.persistence.adapter;

import com.backtester.application.port.BacktestRunRepository;
import com.backtester.domain.backtest.BacktestRun;
import com.backtester.infrastructure.persistence.mapper.BacktestRunEntityMapper;
import com.backtester.infrastructure.persistence.repository.BacktestRunJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BacktestRunRepositoryAdapter implements BacktestRunRepository {

    private final BacktestRunJpaRepository jpaRepository;
    private final BacktestRunEntityMapper mapper;

    public BacktestRunRepositoryAdapter(BacktestRunJpaRepository jpaRepository,
                                         BacktestRunEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public BacktestRun save(BacktestRun run) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(run)));
    }

    @Override
    public Optional<BacktestRun> findById(UUID runId) {
        return jpaRepository.findById(runId).map(mapper::toDomain);
    }

    @Override
    public List<BacktestRun> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
