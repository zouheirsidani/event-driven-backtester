package com.backtester.application.port;

import com.backtester.domain.market.Bar;

import java.time.LocalDate;
import java.util.List;

public interface BarRepository {

    List<Bar> saveAll(List<Bar> bars);

    List<Bar> findByTickerAndDateRange(String ticker, LocalDate from, LocalDate to);

    boolean existsByTickerAndDate(String ticker, LocalDate date);
}
