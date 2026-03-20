package com.backtester.application.metrics;

import com.backtester.domain.backtest.PerformanceMetrics;
import com.backtester.domain.order.Fill;
import com.backtester.domain.order.OrderSide;
import com.backtester.domain.portfolio.PortfolioSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsCalculatorTest {

    private MetricsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MetricsCalculator();
    }

    @Test
    void emptySnapshots_returnsEmptyMetrics() {
        PerformanceMetrics metrics = calculator.calculate(List.of(), List.of(), new BigDecimal("100000"));
        assertThat(metrics.totalReturn()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.totalTrades()).isZero();
    }

    @Test
    void totalReturn_calculatedCorrectly() {
        BigDecimal initial = new BigDecimal("100000");
        List<PortfolioSnapshot> snapshots = List.of(
                snap(LocalDate.of(2023, 1, 1), new BigDecimal("100000")),
                snap(LocalDate.of(2023, 1, 2), new BigDecimal("110000"))
        );

        PerformanceMetrics metrics = calculator.calculate(snapshots, List.of(), initial);

        // (110000 - 100000) / 100000 = 0.1
        assertThat(metrics.totalReturn()).isEqualByComparingTo(new BigDecimal("0.100000"));
    }

    @Test
    void maxDrawdown_calculatedCorrectly() {
        BigDecimal initial = new BigDecimal("100000");
        List<PortfolioSnapshot> snapshots = List.of(
                snap(LocalDate.of(2023, 1, 1), new BigDecimal("100000")),
                snap(LocalDate.of(2023, 1, 2), new BigDecimal("120000")),  // peak
                snap(LocalDate.of(2023, 1, 3), new BigDecimal("90000")),   // trough: (120-90)/120 = 0.25
                snap(LocalDate.of(2023, 1, 4), new BigDecimal("110000"))
        );

        PerformanceMetrics metrics = calculator.calculate(snapshots, List.of(), initial);

        // Max drawdown from peak 120000 to trough 90000 = 30000/120000 = 0.25
        assertThat(metrics.maxDrawdown().doubleValue()).isCloseTo(0.25, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void winRate_calculatedCorrectly() {
        BigDecimal initial = new BigDecimal("100000");
        List<PortfolioSnapshot> snapshots = List.of(
                snap(LocalDate.of(2023, 1, 1), initial)
        );

        // 2 winning trades, 1 losing trade
        List<Fill> fills = List.of(
                fill("f1", "o1", "AAPL", OrderSide.BUY, 10, new BigDecimal("100"), LocalDate.of(2023, 1, 1)),
                fill("f2", "o2", "AAPL", OrderSide.SELL, 10, new BigDecimal("120"), LocalDate.of(2023, 1, 2)), // win +200
                fill("f3", "o3", "GOOG", OrderSide.BUY, 5, new BigDecimal("200"), LocalDate.of(2023, 1, 1)),
                fill("f4", "o4", "GOOG", OrderSide.SELL, 5, new BigDecimal("180"), LocalDate.of(2023, 1, 2))  // loss -100
        );

        PerformanceMetrics metrics = calculator.calculate(snapshots, fills, initial);

        // 1 win, 1 loss → win rate = 0.5
        assertThat(metrics.winRate()).isEqualByComparingTo(new BigDecimal("0.5000"));
        assertThat(metrics.totalTrades()).isEqualTo(2);
    }

    @Test
    void sharpeRatio_positiveForProfitableStrategy() {
        BigDecimal initial = new BigDecimal("100000");
        // Alternating up/down days with a positive drift — ensures variance > 0 so Sharpe is computable
        BigDecimal[] multipliers = {
            new BigDecimal("1.015"), new BigDecimal("0.992"),
            new BigDecimal("1.018"), new BigDecimal("0.995"),
            new BigDecimal("1.020"), new BigDecimal("0.990")
        };
        List<PortfolioSnapshot> snapshots = new ArrayList<>();
        BigDecimal equity = initial;
        for (int i = 0; i < 252; i++) {
            equity = equity.multiply(multipliers[i % multipliers.length]);
            snapshots.add(snap(LocalDate.of(2023, 1, 1).plusDays(i), equity));
        }

        PerformanceMetrics metrics = calculator.calculate(snapshots, List.of(), initial);

        assertThat(metrics.annualizedReturn().doubleValue()).isGreaterThan(0);
        assertThat(metrics.annualizedVolatility().doubleValue()).isGreaterThan(0);
        assertThat(metrics.sharpeRatio().doubleValue()).isGreaterThan(0);
    }

    private PortfolioSnapshot snap(LocalDate date, BigDecimal equity) {
        return new PortfolioSnapshot(date, equity, equity, Map.of());
    }

    private Fill fill(String fillId, String orderId, String ticker, OrderSide side,
                      int qty, BigDecimal price, LocalDate date) {
        return new Fill(fillId, orderId, ticker, side, qty, price, BigDecimal.ZERO, date);
    }
}
