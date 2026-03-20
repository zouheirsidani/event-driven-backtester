package com.backtester.domain.portfolio;

import com.backtester.domain.order.Fill;
import com.backtester.domain.order.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioTest {

    private Portfolio portfolio;
    private static final BigDecimal INITIAL_CASH = new BigDecimal("100000.00");

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(INITIAL_CASH);
    }

    @Test
    void initialState() {
        assertThat(portfolio.getCash()).isEqualByComparingTo(INITIAL_CASH);
        assertThat(portfolio.getPositions()).isEmpty();
        assertThat(portfolio.totalEquity()).isEqualByComparingTo(INITIAL_CASH);
    }

    @Test
    void applyBuyFill_deductsCashAndCreatesPosition() {
        Fill fill = new Fill("f1", "o1", "AAPL", OrderSide.BUY, 10,
                new BigDecimal("150.00"), new BigDecimal("1.00"), LocalDate.now());

        portfolio.applyFill(fill);

        // cash = 100000 - (10 * 150 + 1) = 100000 - 1501 = 98499
        assertThat(portfolio.getCash()).isEqualByComparingTo(new BigDecimal("98499.00"));
        assertThat(portfolio.getPositions()).containsKey("AAPL");
        assertThat(portfolio.getPositions().get("AAPL").quantity()).isEqualTo(10);
        assertThat(portfolio.getPositions().get("AAPL").averageCost())
                .isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void applySellFill_addsCashAndReducesPosition() {
        Fill buy = new Fill("f1", "o1", "AAPL", OrderSide.BUY, 10,
                new BigDecimal("150.00"), new BigDecimal("1.00"), LocalDate.now());
        Fill sell = new Fill("f2", "o2", "AAPL", OrderSide.SELL, 5,
                new BigDecimal("160.00"), new BigDecimal("1.00"), LocalDate.now());

        portfolio.applyFill(buy);
        portfolio.applyFill(sell);

        // proceeds = 5 * 160 - 1 = 799
        // cash after buy = 98499
        // cash after sell = 98499 + 799 = 99298
        assertThat(portfolio.getCash()).isEqualByComparingTo(new BigDecimal("99298.00"));
        assertThat(portfolio.getPositions().get("AAPL").quantity()).isEqualTo(5);
    }

    @Test
    void applySellFillFullPosition_removesPosition() {
        Fill buy = new Fill("f1", "o1", "AAPL", OrderSide.BUY, 10,
                new BigDecimal("150.00"), BigDecimal.ZERO, LocalDate.now());
        Fill sell = new Fill("f2", "o2", "AAPL", OrderSide.SELL, 10,
                new BigDecimal("160.00"), BigDecimal.ZERO, LocalDate.now());

        portfolio.applyFill(buy);
        portfolio.applyFill(sell);

        assertThat(portfolio.getPositions()).doesNotContainKey("AAPL");
    }

    @Test
    void applySellWithoutPosition_throwsException() {
        Fill sell = new Fill("f1", "o1", "AAPL", OrderSide.SELL, 10,
                new BigDecimal("150.00"), BigDecimal.ZERO, LocalDate.now());

        assertThatThrownBy(() -> portfolio.applyFill(sell))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updatePrices_affectsTotalEquity() {
        Fill buy = new Fill("f1", "o1", "AAPL", OrderSide.BUY, 10,
                new BigDecimal("150.00"), BigDecimal.ZERO, LocalDate.now());
        portfolio.applyFill(buy);

        // Before price update: cash = 100000 - 1500 = 98500; position value = 10 * 150 = 1500
        assertThat(portfolio.totalEquity()).isEqualByComparingTo(INITIAL_CASH);

        portfolio.updatePrices(Map.of("AAPL", new BigDecimal("200.00")));

        // After: cash = 98500; position value = 10 * 200 = 2000; total = 100500
        assertThat(portfolio.totalEquity()).isEqualByComparingTo(new BigDecimal("100500.00"));
    }

    @Test
    void takeSnapshot_capturesCurrentState() {
        LocalDate date = LocalDate.of(2023, 1, 15);
        PortfolioSnapshot snapshot = portfolio.takeSnapshot(date);

        assertThat(snapshot.date()).isEqualTo(date);
        assertThat(snapshot.totalEquity()).isEqualByComparingTo(INITIAL_CASH);
        assertThat(snapshot.cash()).isEqualByComparingTo(INITIAL_CASH);
        assertThat(snapshot.positions()).isEmpty();
    }

    @Test
    void averageCostUpdatedOnAdditionalBuy() {
        Fill buy1 = new Fill("f1", "o1", "AAPL", OrderSide.BUY, 10,
                new BigDecimal("100.00"), BigDecimal.ZERO, LocalDate.now());
        Fill buy2 = new Fill("f2", "o2", "AAPL", OrderSide.BUY, 10,
                new BigDecimal("200.00"), BigDecimal.ZERO, LocalDate.now());

        portfolio.applyFill(buy1);
        portfolio.applyFill(buy2);

        // avg cost = (10*100 + 10*200) / 20 = 150
        assertThat(portfolio.getPositions().get("AAPL").averageCost())
                .isEqualByComparingTo(new BigDecimal("150.000000"));
        assertThat(portfolio.getPositions().get("AAPL").quantity()).isEqualTo(20);
    }
}
