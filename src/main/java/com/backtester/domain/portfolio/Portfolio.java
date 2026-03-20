package com.backtester.domain.portfolio;

import com.backtester.domain.order.Fill;
import com.backtester.domain.order.OrderSide;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolio {

    private BigDecimal cash;
    private final Map<String, Position> positions;
    private final List<Fill> completedFills;

    public Portfolio(BigDecimal initialCash) {
        this.cash = initialCash;
        this.positions = new HashMap<>();
        this.completedFills = new ArrayList<>();
    }

    public void applyFill(Fill fill) {
        if (fill.side() == OrderSide.BUY) {
            BigDecimal cost = fill.fillPrice()
                    .multiply(BigDecimal.valueOf(fill.quantityFilled()))
                    .add(fill.commission());
            cash = cash.subtract(cost);
            positions.compute(fill.ticker(), (ticker, existing) -> {
                if (existing == null) {
                    return new Position(ticker, fill.quantityFilled(), fill.fillPrice(), fill.fillPrice());
                }
                return existing.addShares(fill.quantityFilled(), fill.fillPrice());
            });
        } else {
            BigDecimal proceeds = fill.fillPrice()
                    .multiply(BigDecimal.valueOf(fill.quantityFilled()))
                    .subtract(fill.commission());
            cash = cash.add(proceeds);
            positions.compute(fill.ticker(), (ticker, existing) -> {
                if (existing == null) {
                    throw new IllegalStateException("Cannot sell position that does not exist: " + ticker);
                }
                Position updated = existing.removeShares(fill.quantityFilled());
                return updated.quantity() == 0 ? null : updated;
            });
        }
        completedFills.add(fill);
    }

    public void updatePrices(Map<String, BigDecimal> prices) {
        prices.forEach((ticker, price) -> {
            Position pos = positions.get(ticker);
            if (pos != null) {
                positions.put(ticker, pos.withCurrentPrice(price));
            }
        });
    }

    public BigDecimal totalEquity() {
        BigDecimal positionValue = positions.values().stream()
                .map(Position::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cash.add(positionValue);
    }

    public PortfolioSnapshot takeSnapshot(LocalDate date) {
        return new PortfolioSnapshot(date, totalEquity(), cash, Map.copyOf(positions));
    }

    public BigDecimal getCash() {
        return cash;
    }

    public Map<String, Position> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    public List<Fill> getCompletedFills() {
        return Collections.unmodifiableList(completedFills);
    }
}
