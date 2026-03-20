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

@Component
public class EventLoop {

    public record Result(List<PortfolioSnapshot> snapshots, List<Fill> fills) {}

    private final PositionSizer positionSizer;

    public EventLoop(PositionSizer positionSizer) {
        this.positionSizer = positionSizer;
    }

    public Result run(List<BarSeries> seriesList,
                      Strategy strategy,
                      Portfolio portfolio,
                      SlippageModel slippageModel,
                      CommissionModel commissionModel) {

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
                    positionSizer.size(signal, portfolio, closePrices)
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

    private BigDecimal applySlippage(BigDecimal price, OrderSide side, SlippageModel model) {
        return switch (model) {
            case FixedSlippage fs -> side == OrderSide.BUY
                    ? price.add(fs.amount())
                    : price.subtract(fs.amount());
            case PercentSlippage ps -> {
                BigDecimal slippage = price.multiply(ps.percent());
                yield side == OrderSide.BUY ? price.add(slippage) : price.subtract(slippage);
            }
        };
    }

    private BigDecimal calculateCommission(int quantity, BigDecimal fillPrice, CommissionModel model) {
        return switch (model) {
            case FixedCommission fc -> fc.amount();
            case PerShareCommission psc -> psc.perShare()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(6, RoundingMode.HALF_UP);
        };
    }
}
