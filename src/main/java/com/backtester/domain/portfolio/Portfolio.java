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

/**
 * Mutable portfolio that tracks cash, open positions, and fill history during
 * a backtest simulation.  This is the only mutable class in the domain layer;
 * all other domain types are immutable records.
 *
 * <p>The event loop calls {@link #applyFill}, {@link #updatePrices}, and
 * {@link #takeSnapshot} in that strict order at the end of each trading day.
 */
public class Portfolio {

    private BigDecimal cash;
    private final Map<String, Position> positions;
    private final List<Fill> completedFills;

    /**
     * Creates a new portfolio with the given starting cash and no positions.
     *
     * @param initialCash Starting capital, must be positive.
     */
    public Portfolio(BigDecimal initialCash) {
        this.cash = initialCash;
        this.positions = new HashMap<>();
        this.completedFills = new ArrayList<>();
    }

    /**
     * Applies a completed fill to the portfolio, updating cash and positions.
     *
     * <p>For a BUY: deducts {@code fillPrice * quantity + commission} from cash
     * and opens or increases the position using a weighted-average cost.
     *
     * <p>For a SELL: adds {@code fillPrice * quantity - commission} to cash
     * and reduces or closes the position.  Throws if no position exists.
     *
     * @param fill The completed fill to apply.
     * @throws IllegalStateException if attempting to sell a ticker with no open position.
     */
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

    /**
     * Refreshes the current price of every held position to today's closing prices.
     * This must be called before {@link #takeSnapshot} so that {@code totalEquity}
     * reflects end-of-day market values.
     *
     * @param prices Map of ticker → closing price for the current trading day.
     */
    public void updatePrices(Map<String, BigDecimal> prices) {
        prices.forEach((ticker, price) -> {
            Position pos = positions.get(ticker);
            if (pos != null) {
                positions.put(ticker, pos.withCurrentPrice(price));
            }
        });
    }

    /**
     * Calculates total portfolio equity as cash plus the market value of all positions.
     *
     * @return Current total equity.
     */
    public BigDecimal totalEquity() {
        BigDecimal positionValue = positions.values().stream()
                .map(Position::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cash.add(positionValue);
    }

    /**
     * Creates an immutable snapshot of the portfolio at the end of {@code date}.
     * Called once per trading day by the event loop after prices are updated.
     *
     * @param date The trading date being snapshotted.
     * @return Immutable {@link PortfolioSnapshot} for this day.
     */
    public PortfolioSnapshot takeSnapshot(LocalDate date) {
        return new PortfolioSnapshot(date, totalEquity(), cash, Map.copyOf(positions));
    }

    /**
     * Returns the current uninvested cash balance.
     *
     * @return Current cash.
     */
    public BigDecimal getCash() {
        return cash;
    }

    /**
     * Returns an unmodifiable view of all currently open positions keyed by ticker.
     *
     * @return Read-only map of open positions.
     */
    public Map<String, Position> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    /**
     * Returns an unmodifiable list of all fills applied so far, in chronological order.
     * Used by {@code MetricsCalculator} to compute trade-level metrics.
     *
     * @return Read-only list of completed fills.
     */
    public List<Fill> getCompletedFills() {
        return Collections.unmodifiableList(completedFills);
    }
}
