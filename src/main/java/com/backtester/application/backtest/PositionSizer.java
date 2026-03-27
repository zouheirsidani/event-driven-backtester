package com.backtester.application.backtest;

import com.backtester.domain.event.OrderEvent;
import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.order.OrderSide;
import com.backtester.domain.order.OrderType;
import com.backtester.domain.portfolio.Portfolio;
import com.backtester.domain.portfolio.Position;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Converts a {@link SignalEvent} into a concrete {@link OrderEvent} by determining
 * how many shares to trade based on the current portfolio state.
 *
 * <p>Uses a correlation-aware position sizing rule: each LONG signal receives a
 * pre-computed {@code allocationFraction} (calculated by {@link EventLoop} using
 * pairwise Pearson correlations between all co-signaled tickers). Uncorrelated
 * assets receive their full equal-weight fraction; highly correlated assets are
 * scaled down to avoid concentrating correlated risk.
 * EXIT signals sell the full open position for the ticker.
 */
@Component
public class PositionSizer {

    /**
     * Sizes an order based on the signal direction and current portfolio state.
     *
     * @param signal             The strategy signal to act on.
     * @param portfolio          Current portfolio for equity and cash lookups.
     * @param currentPrices      Today's closing prices keyed by ticker.
     * @param allocationFraction Fraction of total portfolio equity to allocate to this
     *                           signal (correlation-adjusted by EventLoop; equals 1/tickerCount
     *                           when no correlation data is available).
     * @return An order event if a trade should be placed; empty if skipped (e.g. zero
     *         price, no position to exit, or SHORT signal which is not yet supported).
     */
    public Optional<OrderEvent> size(SignalEvent signal, Portfolio portfolio,
                                      Map<String, BigDecimal> currentPrices,
                                      BigDecimal allocationFraction) {
        BigDecimal price = currentPrices.get(signal.ticker());
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        return switch (signal.direction()) {
            case LONG -> sizeLong(signal, portfolio, price, allocationFraction);
            case EXIT -> sizeExit(signal, portfolio);
            case SHORT -> Optional.empty(); // Not implemented in V1
        };
    }

    /**
     * Calculates the share quantity for a LONG (buy) order.
     * Allocates the given fraction of total portfolio equity, floors to whole shares,
     * and caps at the number of shares affordable with available cash.
     *
     * @param signal             The originating signal.
     * @param portfolio          Portfolio used to look up equity and cash.
     * @param price              Current price per share for the ticker.
     * @param allocationFraction Pre-computed equity fraction (correlation-adjusted).
     * @return A BUY order, or empty if the calculated quantity is zero (e.g. insufficient cash).
     */
    private Optional<OrderEvent> sizeLong(SignalEvent signal, Portfolio portfolio, BigDecimal price,
                                           BigDecimal allocationFraction) {
        BigDecimal equity = portfolio.totalEquity();
        BigDecimal allocation = equity.multiply(allocationFraction);

        int quantity = allocation.divide(price, 0, RoundingMode.FLOOR).intValue();
        if (quantity <= 0) return Optional.empty();

        // Respect available cash
        BigDecimal maxAffordable = portfolio.getCash().divide(price, 0, RoundingMode.FLOOR);
        quantity = Math.min(quantity, maxAffordable.intValue());
        if (quantity <= 0) return Optional.empty();

        return Optional.of(new OrderEvent(
                UUID.randomUUID().toString(),
                signal.ticker(),
                OrderType.MARKET,
                OrderSide.BUY,
                quantity,
                null,
                signal.timestamp()
        ));
    }

    /**
     * Calculates the share quantity for an EXIT (sell) order.
     * Sells the entire open position for the ticker.
     *
     * @param signal    The originating signal.
     * @param portfolio Portfolio used to look up the current position.
     * @return A SELL order for the full position, or empty if no position exists.
     */
    private Optional<OrderEvent> sizeExit(SignalEvent signal, Portfolio portfolio) {
        Position position = portfolio.getPositions().get(signal.ticker());
        if (position == null || position.quantity() <= 0) return Optional.empty();

        return Optional.of(new OrderEvent(
                UUID.randomUUID().toString(),
                signal.ticker(),
                OrderType.MARKET,
                OrderSide.SELL,
                position.quantity(),
                null,
                signal.timestamp()
        ));
    }
}
