package com.backtester.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable snapshot of a single holding in the portfolio.
 * All mutations return a new {@code Position} instance — the portfolio
 * replaces the map entry rather than modifying in place.
 *
 * @param ticker        Ticker symbol of the held asset.
 * @param quantity      Number of shares currently held (always positive).
 * @param averageCost   Volume-weighted average cost per share across all purchases.
 * @param currentPrice  Most recent close price, updated by {@code Portfolio.updatePrices()}.
 */
public record Position(
        String ticker,
        int quantity,
        BigDecimal averageCost,
        BigDecimal currentPrice
) {

    /**
     * Returns a new {@code Position} reflecting the addition of {@code shares}
     * purchased at {@code price}.  The average cost is recalculated using a
     * weighted average: (oldAvg * oldQty + price * newShares) / totalQty.
     *
     * @param shares Number of shares purchased.
     * @param price  Purchase price per share.
     * @return New position with updated quantity, average cost, and current price.
     */
    public Position addShares(int shares, BigDecimal price) {
        int newQty = quantity + shares;
        BigDecimal newAvgCost = averageCost.multiply(BigDecimal.valueOf(quantity))
                .add(price.multiply(BigDecimal.valueOf(shares)))
                .divide(BigDecimal.valueOf(newQty), 6, RoundingMode.HALF_UP);
        return new Position(ticker, newQty, newAvgCost, price);
    }

    /**
     * Returns a new {@code Position} with {@code shares} removed.
     * The average cost is unchanged on a sale; the caller is responsible for
     * removing the position from the portfolio when quantity reaches zero.
     *
     * @param shares Number of shares to remove.
     * @return New position with reduced quantity.
     */
    public Position removeShares(int shares) {
        return new Position(ticker, quantity - shares, averageCost, currentPrice);
    }

    /**
     * Returns a new {@code Position} with the current market price updated.
     * Called at end-of-day to reflect today's closing price.
     *
     * @param price Updated market price per share.
     * @return New position with updated current price.
     */
    public Position withCurrentPrice(BigDecimal price) {
        return new Position(ticker, quantity, averageCost, price);
    }

    /**
     * Calculates the current market value of this position.
     *
     * @return {@code currentPrice × quantity}
     */
    public BigDecimal marketValue() {
        return currentPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Calculates the unrealized profit or loss for this position.
     *
     * @return {@code (currentPrice - averageCost) × quantity}; negative means a loss.
     */
    public BigDecimal unrealizedPnl() {
        return currentPrice.subtract(averageCost).multiply(BigDecimal.valueOf(quantity));
    }
}
