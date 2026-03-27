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
import com.backtester.domain.strategy.SignalDirection;
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
import java.util.TreeSet;
import java.util.UUID;

/**
 * The simulation core of the backtesting engine.
 *
 * <p>Iterates over every unique trading date across all tickers and processes a
 * strict causal event sequence per day using a synchronous {@link ArrayDeque}:
 * <ol>
 *   <li>Emit {@link MarketDataEvent} for each ticker that has a bar on that date.</li>
 *   <li>Call {@code strategy.onDay()} once with the full universe; queue any returned {@link SignalEvent}s.</li>
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
     * @param strategy        Strategy implementation to call once per day with the full universe.
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

        // Total number of tickers — used as the baseline for per-ticker allocation
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
            Map<String, BarSeries> universe = new HashMap<>();

            // Step 1: Emit MarketDataEvents and build universe for tickers present today
            for (String ticker : barsByTicker.keySet()) {
                Bar bar = barIndex.get(ticker).get(date);
                if (bar == null) continue;

                closePrices.put(ticker, bar.close());
                historyMap.get(ticker).add(bar);

                List<Bar> immutableHistory = List.copyOf(historyMap.get(ticker));
                BarSeries history = new BarSeries(ticker, immutableHistory);
                universe.put(ticker, history);

                eventQueue.add(new MarketDataEvent(bar, Instant.now()));
            }

            // Step 2: Call strategy once with the full universe; add all returned signals
            List<SignalEvent> signals = strategy.onDay(date, universe, portfolio);
            signals.forEach(eventQueue::add);

            // Compute correlation-adjusted allocation fractions for today's LONG signals.
            // Tickers that are highly correlated with other co-signaled tickers receive
            // a reduced fraction so that the portfolio does not concentrate correlated risk.
            List<String> longTickers = signals.stream()
                    .filter(s -> s.direction() == SignalDirection.LONG)
                    .map(SignalEvent::ticker)
                    .distinct()
                    .toList();
            Map<String, BigDecimal> allocationFractions =
                    computeAllocationFractions(longTickers, historyMap, tickerCount);

            // Step 3: Process SignalEvents → OrderEvents
            List<TradingEvent> snapshot1 = new ArrayList<>(eventQueue);
            eventQueue.clear();

            BigDecimal equalFraction = BigDecimal.ONE.divide(
                    BigDecimal.valueOf(Math.max(1, tickerCount)), 10, RoundingMode.HALF_UP);
            for (TradingEvent event : snapshot1) {
                if (event instanceof SignalEvent signal) {
                    BigDecimal fraction = allocationFractions.getOrDefault(signal.ticker(), equalFraction);
                    positionSizer.size(signal, portfolio, closePrices, fraction)
                            .ifPresent(eventQueue::add);
                }
            }

            // Step 4: Execute OrderEvents → FillEvents
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

            // Step 5: Apply FillEvents to Portfolio
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

            // Step 6: Update all position prices to close prices
            portfolio.updatePrices(closePrices);

            // Step 7: Take portfolio snapshot
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

    /**
     * Minimum number of daily return observations required to compute a reliable
     * Pearson correlation. Falls back to equal-weight sizing if any signaled ticker
     * has fewer than this many bars in its running history.
     */
    private static final int MIN_CORR_DAYS = 20;

    /**
     * Computes correlation-adjusted allocation fractions for a set of LONG-signaled tickers.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Derive the last {@value #MIN_CORR_DAYS} daily returns for each ticker from its
     *       running bar history.</li>
     *   <li>For each ticker compute its average absolute Pearson correlation with all other
     *       signaled tickers.</li>
     *   <li>Scale the baseline equal-weight fraction by
     *       {@code max(0.2, 1 − avgAbsCorrelation)}.  Highly correlated assets receive at
     *       most 20 % of their baseline to avoid concentrating correlated risk.</li>
     * </ol>
     *
     * <p>Falls back to equal-weight ({@code 1 / tickerCount}) for all tickers when:
     * <ul>
     *   <li>fewer than two LONG signals fired today (no pairs to correlate), or</li>
     *   <li>any signaled ticker has insufficient bar history.</li>
     * </ul>
     *
     * @param longTickers  Distinct tickers for which LONG signals were generated today.
     * @param historyMap   Running bar history for every ticker in this simulation.
     * @param tickerCount  Total number of tickers in the backtest (baseline denominator).
     * @return Map of ticker → correlation-adjusted allocation fraction.
     */
    private Map<String, BigDecimal> computeAllocationFractions(
            List<String> longTickers,
            Map<String, List<Bar>> historyMap,
            int tickerCount) {

        Map<String, BigDecimal> fractions = new HashMap<>();
        BigDecimal equalFraction = BigDecimal.ONE.divide(
                BigDecimal.valueOf(Math.max(1, tickerCount)), 10, RoundingMode.HALF_UP);

        // Only one (or zero) LONG signal today — no correlation possible, use equal weight
        if (longTickers.size() <= 1) {
            longTickers.forEach(t -> fractions.put(t, equalFraction));
            return fractions;
        }

        // Build return series for each signaled ticker (last MIN_CORR_DAYS returns)
        Map<String, List<Double>> returns = new HashMap<>();
        for (String ticker : longTickers) {
            List<Bar> history = historyMap.get(ticker);
            if (history == null || history.size() < MIN_CORR_DAYS + 1) {
                // Insufficient history: fall back to equal weight for all tickers
                longTickers.forEach(t -> fractions.put(t, equalFraction));
                return fractions;
            }
            int start = history.size() - MIN_CORR_DAYS;
            List<Double> rets = new ArrayList<>(MIN_CORR_DAYS);
            for (int i = start; i < history.size(); i++) {
                double prev = history.get(i - 1).close().doubleValue();
                double curr = history.get(i).close().doubleValue();
                rets.add(prev == 0 ? 0.0 : (curr - prev) / prev);
            }
            returns.put(ticker, rets);
        }

        // Compute average absolute correlation of each ticker with all other signaled tickers
        for (String t1 : longTickers) {
            double totalAbsCorr = 0.0;
            int pairCount = 0;
            for (String t2 : longTickers) {
                if (t1.equals(t2)) continue;
                totalAbsCorr += Math.abs(pearsonCorrelation(returns.get(t1), returns.get(t2)));
                pairCount++;
            }
            double avgAbsCorr = pairCount > 0 ? totalAbsCorr / pairCount : 0.0;

            // Scale factor: 1.0 when uncorrelated, 0.2 when fully correlated (minimum 20 % of base)
            double scaleFactor = Math.max(0.2, 1.0 - avgAbsCorr);
            BigDecimal adjustedFraction = equalFraction.multiply(
                    BigDecimal.valueOf(scaleFactor)).setScale(10, RoundingMode.HALF_UP);
            fractions.put(t1, adjustedFraction);
        }

        return fractions;
    }

    /**
     * Computes the Pearson correlation coefficient between two equal-length return series.
     * Returns 0.0 if the denominator is zero (i.e. one series has zero variance).
     *
     * @param x First return series.
     * @param y Second return series (must have the same length as {@code x}).
     * @return Pearson correlation in [-1, 1], or 0.0 when undefined.
     */
    private double pearsonCorrelation(List<Double> x, List<Double> y) {
        int n = x.size();
        double sumX = 0.0, sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumX += x.get(i);
            sumY += y.get(i);
        }
        double meanX = sumX / n, meanY = sumY / n;

        double numerator = 0.0, denX = 0.0, denY = 0.0;
        for (int i = 0; i < n; i++) {
            double dx = x.get(i) - meanX;
            double dy = y.get(i) - meanY;
            numerator += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }
        double denominator = Math.sqrt(denX * denY);
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }
}
