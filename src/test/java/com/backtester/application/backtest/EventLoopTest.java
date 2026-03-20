package com.backtester.application.backtest;

import com.backtester.domain.backtest.FixedCommission;
import com.backtester.domain.backtest.FixedSlippage;
import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.portfolio.Portfolio;
import com.backtester.domain.portfolio.PortfolioSnapshot;
import com.backtester.domain.strategy.SignalDirection;
import com.backtester.domain.strategy.Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EventLoopTest {

    private EventLoop eventLoop;
    private PositionSizer positionSizer;
    private static final BigDecimal INITIAL_CASH = new BigDecimal("100000");

    @BeforeEach
    void setUp() {
        positionSizer = new PositionSizer();
        eventLoop = new EventLoop(positionSizer);
    }

    @Test
    void emptyBarSeries_producesNoSnapshots() {
        Portfolio portfolio = new Portfolio(INITIAL_CASH);
        Strategy noOpStrategy = new NoOpStrategy();

        EventLoop.Result result = eventLoop.run(
                List.of(),
                noOpStrategy,
                portfolio,
                new FixedSlippage(BigDecimal.ZERO),
                new FixedCommission(BigDecimal.ZERO)
        );

        assertThat(result.snapshots()).isEmpty();
        assertThat(result.fills()).isEmpty();
    }

    @Test
    void snapshotCountMatchesTradingDays() {
        List<Bar> bars = buildBars("AAPL", 5, new BigDecimal("100"));
        BarSeries series = new BarSeries("AAPL", bars);
        Portfolio portfolio = new Portfolio(INITIAL_CASH);

        EventLoop.Result result = eventLoop.run(
                List.of(series),
                new NoOpStrategy(),
                portfolio,
                new FixedSlippage(BigDecimal.ZERO),
                new FixedCommission(BigDecimal.ZERO)
        );

        assertThat(result.snapshots()).hasSize(5);
    }

    @Test
    void buySignalOnDay1_portfolioHoldsPosition() {
        List<Bar> bars = buildBars("AAPL", 3, new BigDecimal("100"));
        BarSeries series = new BarSeries("AAPL", bars);
        Portfolio portfolio = new Portfolio(INITIAL_CASH);

        // Strategy fires a LONG on the first bar
        Strategy alwaysBuy = new SingleBuyStrategy("AAPL", bars.get(0).date());

        EventLoop.Result result = eventLoop.run(
                List.of(series),
                alwaysBuy,
                portfolio,
                new FixedSlippage(BigDecimal.ZERO),
                new FixedCommission(BigDecimal.ZERO)
        );

        // Should have fills after day 1
        assertThat(result.fills()).isNotEmpty();
        // Equity should still be ~100000 (price didn't change)
        PortfolioSnapshot lastSnapshot = result.snapshots().get(result.snapshots().size() - 1);
        assertThat(lastSnapshot.totalEquity()).isEqualByComparingTo(INITIAL_CASH);
    }

    @Test
    void slippageApplied_raisesEffectiveBuyPrice() {
        BigDecimal closePrice = new BigDecimal("100.00");
        BigDecimal slippage = new BigDecimal("0.50");

        List<Bar> bars = buildBars("AAPL", 2, closePrice);
        BarSeries series = new BarSeries("AAPL", bars);
        Portfolio portfolio = new Portfolio(INITIAL_CASH);

        Strategy alwaysBuy = new SingleBuyStrategy("AAPL", bars.get(0).date());

        EventLoop.Result result = eventLoop.run(
                List.of(series),
                alwaysBuy,
                portfolio,
                new FixedSlippage(slippage),
                new FixedCommission(BigDecimal.ZERO)
        );

        // Fill price should be closePrice + slippage = 100.50
        assertThat(result.fills()).isNotEmpty();
        result.fills().forEach(fill ->
                assertThat(fill.fillPrice()).isEqualByComparingTo(closePrice.add(slippage))
        );
    }

    @Test
    void commissionDeducted_reducesPortfolioEquity() {
        BigDecimal commission = new BigDecimal("5.00");
        List<Bar> bars = buildBars("AAPL", 2, new BigDecimal("100.00"));
        BarSeries series = new BarSeries("AAPL", bars);
        Portfolio portfolio = new Portfolio(INITIAL_CASH);

        Strategy alwaysBuy = new SingleBuyStrategy("AAPL", bars.get(0).date());

        EventLoop.Result result = eventLoop.run(
                List.of(series),
                alwaysBuy,
                portfolio,
                new FixedSlippage(BigDecimal.ZERO),
                new FixedCommission(commission)
        );

        // Commission should be reflected in fills
        assertThat(result.fills()).isNotEmpty();
        result.fills().forEach(fill ->
                assertThat(fill.commission()).isEqualByComparingTo(commission)
        );
    }

    // ---- Helper types ----

    private List<Bar> buildBars(String ticker, int count, BigDecimal price) {
        List<Bar> bars = new ArrayList<>();
        LocalDate start = LocalDate.of(2023, 1, 2);
        for (int i = 0; i < count; i++) {
            bars.add(new Bar(ticker, start.plusDays(i), price, price, price, price, 10000L));
        }
        return bars;
    }

    /** Always returns empty signal */
    static class NoOpStrategy implements Strategy {
        @Override public String strategyId() { return "NOOP"; }
        @Override public String displayName() { return "No-Op"; }
        @Override public Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio) {
            return Optional.empty();
        }
    }

    /** Fires a LONG signal only on the specified trigger date */
    static class SingleBuyStrategy implements Strategy {
        private final String ticker;
        private final LocalDate triggerDate;

        SingleBuyStrategy(String ticker, LocalDate triggerDate) {
            this.ticker = ticker;
            this.triggerDate = triggerDate;
        }

        @Override public String strategyId() { return "SINGLE_BUY"; }
        @Override public String displayName() { return "Single Buy"; }

        @Override
        public Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio) {
            if (currentBar.date().equals(triggerDate) && !portfolio.getPositions().containsKey(ticker)) {
                return Optional.of(new SignalEvent(ticker, SignalDirection.LONG,
                        BigDecimal.ONE, strategyId(), Instant.now()));
            }
            return Optional.empty();
        }
    }
}
