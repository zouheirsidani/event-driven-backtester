package com.backtester.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Position(
        String ticker,
        int quantity,
        BigDecimal averageCost,
        BigDecimal currentPrice
) {

    public Position addShares(int shares, BigDecimal price) {
        int newQty = quantity + shares;
        BigDecimal newAvgCost = averageCost.multiply(BigDecimal.valueOf(quantity))
                .add(price.multiply(BigDecimal.valueOf(shares)))
                .divide(BigDecimal.valueOf(newQty), 6, RoundingMode.HALF_UP);
        return new Position(ticker, newQty, newAvgCost, price);
    }

    public Position removeShares(int shares) {
        return new Position(ticker, quantity - shares, averageCost, currentPrice);
    }

    public Position withCurrentPrice(BigDecimal price) {
        return new Position(ticker, quantity, averageCost, price);
    }

    public BigDecimal marketValue() {
        return currentPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal unrealizedPnl() {
        return currentPrice.subtract(averageCost).multiply(BigDecimal.valueOf(quantity));
    }
}
