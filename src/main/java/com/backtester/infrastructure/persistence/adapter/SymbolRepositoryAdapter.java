package com.backtester.infrastructure.persistence.adapter;

import com.backtester.application.port.SymbolRepository;
import com.backtester.domain.market.Symbol;
import com.backtester.infrastructure.persistence.mapper.SymbolEntityMapper;
import com.backtester.infrastructure.persistence.repository.SymbolJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Infrastructure adapter implementing the {@link SymbolRepository} port.
 * Delegates to {@code SymbolJpaRepository} and uses {@code SymbolEntityMapper}
 * to convert between domain records and JPA entities.
 */
@Repository
public class SymbolRepositoryAdapter implements SymbolRepository {

    private final SymbolJpaRepository jpaRepository;
    private final SymbolEntityMapper mapper;

    public SymbolRepositoryAdapter(SymbolJpaRepository jpaRepository, SymbolEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Symbol save(Symbol symbol) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(symbol)));
    }

    @Override
    public Optional<Symbol> findByTicker(String ticker) {
        return jpaRepository.findById(ticker).map(mapper::toDomain);
    }

    @Override
    public List<Symbol> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByTicker(String ticker) {
        return jpaRepository.existsById(ticker);
    }
}
