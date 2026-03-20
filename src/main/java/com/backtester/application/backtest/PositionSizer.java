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

@Component
public class PositionSizer {

    // Fixed-fractional: allocate 10% of portfolio equity per signal
    private static final BigDecimal PORTFOLIO_FRACTION = new BigDecimal("0.10");

    public Optional<OrderEvent> size(SignalEvent signal, Portfolio portfolio,
                                      Map<String, BigDecimal> currentPrices) {
        BigDecimal price = currentPrices.get(signal.ticker());
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        return switch (signal.direction()) {
            case LONG -> sizeLong(signal, portfolio, price);
            case EXIT -> sizeExit(signal, portfolio);
            case SHORT -> Optional.empty(); // Not implemented in V1
        };
    }

    private Optional<OrderEvent> sizeLong(SignalEvent signal, Portfolio portfolio, BigDecimal price) {
        BigDecimal equity = portfolio.totalEquity();
        BigDecimal allocation = equity.multiply(PORTFOLIO_FRACTION);

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
