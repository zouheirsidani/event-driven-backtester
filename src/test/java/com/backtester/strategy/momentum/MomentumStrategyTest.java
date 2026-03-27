package com.backtester.strategy.momentum;

import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.order.Fill;
import com.backtester.domain.order.OrderSide;
import com.backtester.domain.portfolio.Portfolio;
import com.backtester.domain.strategy.SignalDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MomentumStrategyTest {

    private MomentumStrategy strategy;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        strategy = new MomentumStrategy();
        portfolio = new Portfolio(new BigDecimal("100000"));
    }

    @Test
    void strategyId_isCorrect() {
        assertThat(strategy.strategyId()).isEqualTo("MOMENTUM_V1");
    }

    @Test
    void noSignal_withInsufficientHistory() {
        List<Bar> bars = buildBars("AAPL", 15, new BigDecimal("100"));
        BarSeries series = new BarSeries("AAPL", bars);
        LocalDate date = bars.get(bars.size() - 1).date();
        Map<String, BarSeries> universe = Map.of("AAPL", series);

        List<SignalEvent> signals = strategy.onDay(date, universe, portfolio);

        assertThat(signals).isEmpty();
    }

    @Test
    void longSignal_whenPositiveMomentumAndNoPosition() {
        // Rising prices — 20-day return is positive
        List<Bar> bars = buildTrendingBars("AAPL", 25, new BigDecimal("100"), new BigDecimal("1.00"));
        BarSeries series = new BarSeries("AAPL", bars);
        LocalDate date = bars.get(bars.size() - 1).date();
        Map<String, BarSeries> universe = Map.of("AAPL", series);

        List<SignalEvent> signals = strategy.onDay(date, universe, portfolio);

        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).direction()).isEqualTo(SignalDirection.LONG);
        assertThat(signals.get(0).ticker()).isEqualTo("AAPL");
        assertThat(signals.get(0).strength()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void noLongSignal_whenPositiveMomentumButAlreadyHasPosition() {
        // Establish a position
        portfolio.applyFill(new Fill("f1", "o1", "AAPL", OrderSide.BUY, 10,
                new BigDecimal("100"), BigDecimal.ZERO, LocalDate.now()));

        List<Bar> bars = buildTrendingBars("AAPL", 25, new BigDecimal("100"), new BigDecimal("1.00"));
        BarSeries series = new BarSeries("AAPL", bars);
        LocalDate date = bars.get(bars.size() - 1).date();
        Map<String, BarSeries> universe = Map.of("AAPL", series);

        List<SignalEvent> signals = strategy.onDay(date, universe, portfolio);

        // No LONG signal because we already hold the position
        assertThat(signals).isEmpty();
    }

    @Test
    void exitSignal_whenNegativeMomentumAndHasPosition() {
        // Establish a position
        portfolio.applyFill(new Fill("f1", "o1", "AAPL", OrderSide.BUY, 10,
                new BigDecimal("150"), BigDecimal.ZERO, LocalDate.now()));

        // Declining prices
        List<Bar> bars = buildTrendingBars("AAPL", 25, new BigDecimal("150"), new BigDecimal("-1.50"));
        BarSeries series = new BarSeries("AAPL", bars);
        LocalDate date = bars.get(bars.size() - 1).date();
        Map<String, BarSeries> universe = Map.of("AAPL", series);

        List<SignalEvent> signals = strategy.onDay(date, universe, portfolio);

        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).direction()).isEqualTo(SignalDirection.EXIT);
        assertThat(signals.get(0).ticker()).isEqualTo("AAPL");
    }

    @Test
    void noSignal_withExactly20Bars() {
        // 20 bars means size == LOOKBACK, need LOOKBACK + 1
        List<Bar> bars = buildBars("AAPL", 20, new BigDecimal("100"));
        BarSeries series = new BarSeries("AAPL", bars);
        LocalDate date = bars.get(bars.size() - 1).date();
        Map<String, BarSeries> universe = Map.of("AAPL", series);

        List<SignalEvent> signals = strategy.onDay(date, universe, portfolio);

        assertThat(signals).isEmpty();
    }

    // Helper: flat-price bars
    private List<Bar> buildBars(String ticker, int count, BigDecimal price) {
        List<Bar> bars = new ArrayList<>();
        LocalDate start = LocalDate.of(2023, 1, 1);
        for (int i = 0; i < count; i++) {
            bars.add(new Bar(ticker, start.plusDays(i), price, price, price, price, 1000L));
        }
        return bars;
    }

    // Helper: trending bars with a daily increment
    private List<Bar> buildTrendingBars(String ticker, int count, BigDecimal startPrice, BigDecimal dailyChange) {
        List<Bar> bars = new ArrayList<>();
        LocalDate start = LocalDate.of(2023, 1, 1);
        BigDecimal price = startPrice;
        for (int i = 0; i < count; i++) {
            bars.add(new Bar(ticker, start.plusDays(i), price, price, price, price, 1000L));
            price = price.add(dailyChange);
            if (price.compareTo(BigDecimal.ZERO) <= 0) price = new BigDecimal("0.01");
        }
        return bars;
    }
}
