package com.backtester.infrastructure.persistence.adapter;

import com.backtester.application.port.BarRepository;
import com.backtester.domain.market.Bar;
import com.backtester.infrastructure.persistence.mapper.BarEntityMapper;
import com.backtester.infrastructure.persistence.repository.BarJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class BarRepositoryAdapter implements BarRepository {

    private final BarJpaRepository jpaRepository;
    private final BarEntityMapper mapper;

    public BarRepositoryAdapter(BarJpaRepository jpaRepository, BarEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Bar> saveAll(List<Bar> bars) {
        var entities = bars.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Bar> findByTickerAndDateRange(String ticker, LocalDate from, LocalDate to) {
        return jpaRepository.findByTickerAndDateBetween(ticker, from, to)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByTickerAndDate(String ticker, LocalDate date) {
        return jpaRepository.existsByTickerAndDate(ticker, date);
    }
}
