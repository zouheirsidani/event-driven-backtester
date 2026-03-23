package com.backtester.application.backtest;

import com.backtester.domain.backtest.CommissionModel;
import com.backtester.domain.backtest.FixedCommission;
import com.backtester.domain.backtest.FixedSlippage;
import com.backtester.domain.backtest.PerShareCommission;
import com.backtester.domain.backtest.PercentSlippage;
import com.backtester.domain.backtest.SlippageModel;
import com.backtester.domain.event.FillEvent;
import com.backtester.domain.event.MarketDataEvent;
import com.backtester.domain.event.OrderEvent;
import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.event.TradingEvent;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.order.Fill;
import com.backtester.domain.order.OrderSide;
import com.backtester.domain.portfolio.Portfolio;
import com.backtester.domain.portfolio.PortfolioSnapshot;
import com.backtester.domain.strategy.Strategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The simulation core of the backtesting engine.
 *
 * <p>Iterates over every unique trading date across all tickers and processes a
 * strict causal event sequence per day using a synchronous {@link ArrayDeque}:
 * <ol>
 *   <li>Emit {@link MarketDataEvent} for each ticker that has a bar on that date.</li>
 *   <li>Call {@code strategy.onBar()} immediately; queue any returned {@link SignalEvent}.</li>
 *   <li>Convert {@link SignalEvent}s to {@link OrderEvent}s via {@link PositionSizer}.</li>
 *   <li>Execute {@link OrderEvent}s at close price + slippage → produce {@link FillEvent}s.</li>
 *   <li>Apply all {@link FillEvent}s to the {@link Portfolio}.</li>
 *   <li>Update position prices to today's closes, then take a snapshot.</li>
 * </ol>
 *
 * <p>The synchronous, deterministic design means results are fully reproducible
 * and no thread-safety is required within the loop.
 */
@Component
public class EventLoop {

    /**
     * Holds the output of a completed simulation run.
     *
     * @param snapshots End-of-day portfolio snapshots in chronological order.
     * @param fills     All fills executed during the simulation.
     */
    public record Result(List<PortfolioSnapshot> snapshots, List<Fill> fills) {}

    private final PositionSizer positionSizer;

    /**
     * @param positionSizer Used to size orders from strategy signals.
     */
    public EventLoop(PositionSizer positionSizer) {
        this.positionSizer = positionSizer;
    }

    /**
     * Runs the full event-driven simulation and returns results.
     *
     * @param seriesList      Bar series for each ticker to simulate.
     * @param strategy        Strategy implementation to call each bar.
     * @param portfolio       Pre-initialised portfolio with starting capital.
     * @param slippageModel   Model for adjusting fill prices away from close.
     * @param commissionModel Model for calculating brokerage costs per fill.
     * @return Snapshots and fill list for metrics calculation.
     */
    public Result run(List<BarSeries> seriesList,
                      Strategy strategy,
                      Portfolio portfolio,
                      SlippageModel slippageModel,
                      CommissionModel commissionModel) {

        // Total number of tickers drives the equal-weight allocation fraction
        int tickerCount = seriesList.size();

        // Build lookup: ticker → bars sorted by date
        Map<String, List<Bar>> barsByTicker = new HashMap<>();
        for (BarSeries series : seriesList) {
            barsByTicker.put(series.ticker(), new ArrayList<>(series.bars()));
        }

        // Collect all unique trading dates in sorted order
        TreeSet<LocalDate> allDates = new TreeSet<>();
        for (List<Bar> bars : barsByTicker.values()) {
            for (Bar bar : bars) {
                allDates.add(bar.date());
            }
        }

        // Build running history per ticker (accumulated as we advance through dates)
        Map<String, List<Bar>> historyMap = new HashMap<>();
        for (String ticker : barsByTicker.keySet()) {
            historyMap.put(ticker, new ArrayList<>());
        }

        // Build a date-keyed lookup for fast access
        Map<String, Map<LocalDate, Bar>> barIndex = new HashMap<>();
        for (Map.Entry<String, List<Bar>> entry : barsByTicker.entrySet()) {
            Map<LocalDate, Bar> dateMap = new HashMap<>();
            for (Bar bar : entry.getValue()) {
                dateMap.put(bar.date(), bar);
            }
            barIndex.put(entry.getKey(), dateMap);
        }

        List<PortfolioSnapshot> snapshots = new ArrayList<>();
        ArrayDeque<TradingEvent> eventQueue = new ArrayDeque<>();

        for (LocalDate date : allDates) {
            eventQueue.clear();
            Map<String, BigDecimal> closePrices = new HashMap<>();

            // Step 1: Emit MarketDataEvents and gather signals
            for (String ticker : barsByTicker.keySet()) {
                Bar bar = barIndex.get(ticker).get(date);
                if (bar == null) continue;

                closePrices.put(ticker, bar.close());
                historyMap.get(ticker).add(bar);

                List<Bar> immutableHistory = List.copyOf(historyMap.get(ticker));
                BarSeries history = new BarSeries(ticker, immutableHistory);

                eventQueue.add(new MarketDataEvent(bar, Instant.now()));

                Optional<SignalEvent> signal = strategy.onBar(history, bar, portfolio);
                signal.ifPresent(eventQueue::add);
            }

            // Step 2: Process SignalEvents → OrderEvents
            List<TradingEvent> snapshot1 = new ArrayList<>(eventQueue);
            eventQueue.clear();

            for (TradingEvent event : snapshot1) {
                if (event instanceof SignalEvent signal) {
                    positionSizer.size(signal, portfolio, closePrices, tickerCount)
                            .ifPresent(eventQueue::add);
                }
            }

            // Step 3: Execute OrderEvents → FillEvents
            List<OrderEvent> orders = new ArrayList<>();
            while (!eventQueue.isEmpty()) {
                if (eventQueue.peek() instanceof OrderEvent order) {
                    orders.add(order);
                }
                eventQueue.poll();
            }

            for (OrderEvent order : orders) {
                BigDecimal closePrice = closePrices.get(order.ticker());
                if (closePrice == null) continue;

                BigDecimal fillPrice = applySlippage(closePrice, order.side(), slippageModel);
                BigDecimal commission = calculateCommission(order.quantity(), fillPrice, commissionModel);

                eventQueue.add(new FillEvent(
                        UUID.randomUUID().toString(),
                        order.orderId(),
                        order.ticker(),
                        order.side(),
                        order.quantity(),
                        fillPrice,
                        commission,
                        Instant.now()
                ));
            }

            // Step 4: Apply FillEvents to Portfolio
            while (!eventQueue.isEmpty()) {
                TradingEvent event = eventQueue.poll();
                if (event instanceof FillEvent fillEvent) {
                    Fill fill = new Fill(
                            fillEvent.fillId(),
                            fillEvent.orderId(),
                            fillEvent.ticker(),
                            fillEvent.side(),
                            fillEvent.quantityFilled(),
                            fillEvent.fillPrice(),
                            fillEvent.commission(),
                            date
                    );
                    portfolio.applyFill(fill);
                }
            }

            // Step 5: Update all position prices to close prices
            portfolio.updatePrices(closePrices);

            // Step 6: Take portfolio snapshot
            snapshots.add(portfolio.takeSnapshot(date));
        }

        return new Result(snapshots, portfolio.getCompletedFills());
    }

    /**
     * Adjusts the reference close price to simulate execution slippage.
     * BUY orders are filled above the close (adverse to the buyer);
     * SELL orders are filled below (adverse to the seller).
     *
     * @param price Unadjusted close price.
     * @param side  BUY or SELL.
     * @param model The slippage configuration to apply.
     * @return Adjusted fill price.
     */
    private BigDecimal applySlippage(BigDecimal price, OrderSide side, SlippageModel model) {
        return switch (model) {
            // Fixed: add/subtract a flat dollar amount per share
            case FixedSlippage fs -> side == OrderSide.BUY
                    ? price.add(fs.amount())
                    : price.subtract(fs.amount());
            // Percent: add/subtract a fraction of the price (e.g. 0.1% of close)
            case PercentSlippage ps -> {
                BigDecimal slippage = price.multiply(ps.percent());
                yield side == OrderSide.BUY ? price.add(slippage) : price.subtract(slippage);
            }
        };
    }

    /**
     * Calculates the total commission for a fill.
     *
     * @param quantity  Number of shares filled.
     * @param fillPrice Execution price (used for value-based models; not used in V1 implementations).
     * @param model     The commission configuration to apply.
     * @return Total commission amount for this fill.
     */
    private BigDecimal calculateCommission(int quantity, BigDecimal fillPrice, CommissionModel model) {
        return switch (model) {
            // Fixed: flat fee regardless of quantity or price
            case FixedCommission fc -> fc.amount();
            // Per-share: multiply rate by shares filled
            case PerShareCommission psc -> psc.perShare()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(6, RoundingMode.HALF_UP);
        };
    }
}
