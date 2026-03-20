package com.backtester.infrastructure.persistence.repository;

import com.backtester.infrastructure.persistence.entity.BarEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BarJpaRepository extends JpaRepository<BarEntity, Long> {

    @Query("SELECT b FROM BarEntity b WHERE b.ticker = :ticker AND b.date >= :from AND b.date <= :to ORDER BY b.date ASC")
    List<BarEntity> findByTickerAndDateBetween(@Param("ticker") String ticker,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    @Query("SELECT b FROM BarEntity b WHERE b.ticker = :ticker AND b.date >= :from AND b.date <= :to ORDER BY b.date ASC")
    Page<BarEntity> findByTickerAndDateBetweenPaged(@Param("ticker") String ticker,
                                                    @Param("from") LocalDate from,
                                                    @Param("to") LocalDate to,
                                                    Pageable pageable);

    @Query("SELECT COUNT(b) FROM BarEntity b WHERE b.ticker = :ticker AND b.date >= :from AND b.date <= :to")
    long countByTickerAndDateBetween(@Param("ticker") String ticker,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    boolean existsByTickerAndDate(String ticker, LocalDate date);
}
