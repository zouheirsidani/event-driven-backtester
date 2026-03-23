package com.backtester.application.metrics;

import com.backtester.domain.backtest.PerformanceMetrics;
import com.backtester.domain.market.Bar;
import com.backtester.domain.order.Fill;
import com.backtester.domain.order.OrderSide;
import com.backtester.domain.portfolio.PortfolioSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes aggregate performance statistics from the output of a completed
 * event loop simulation.
 *
 * <p>Key metrics and their formulas:
 * <ul>
 *   <li><b>Total return</b>: (finalEquity - initialCash) / initialCash</li>
 *   <li><b>Annualised return</b>: (1 + R)^(252/days) - 1</li>
 *   <li><b>Annualised volatility</b>: stdDev(dailyReturns) × √252</li>
 *   <li><b>Sharpe ratio</b>: annualisedReturn / annualisedVolatility (rf = 0)</li>
 *   <li><b>Max drawdown</b>: max peak-to-trough decline over the equity curve</li>
 *   <li><b>Alpha / Beta</b>: CAPM regression of strategy returns vs benchmark returns</li>
 * </ul>
 */
@Component
public class MetricsCalculator {

    /**
     * Internal container for trade-level metrics aggregated from fill history.
     */
    private record TradeMetrics(
            BigDecimal winRate,
            BigDecimal avgWin,
            BigDecimal avgLoss,
            BigDecimal profitFactor,
            int totalTrades
    ) {}

    /**
     * Overload that omits benchmark calculation.
     *
     * @param snapshots    Daily equity snapshots from the event loop.
     * @param fills        All fills executed during the simulation.
     * @param initialCash  Starting capital used to compute total return.
     * @return Computed performance metrics (alpha and beta will be null).
     */
    public PerformanceMetrics calculate(List<PortfolioSnapshot> snapshots,
                                         List<Fill> fills,
                                         BigDecimal initialCash) {
        return calculate(snapshots, fills, initialCash, null);
    }

    /**
     * Computes all performance metrics, optionally including CAPM alpha/beta.
     *
     * @param snapshots     Daily equity snapshots from the event loop.
     * @param fills         All fills executed during the simulation.
     * @param initialCash   Starting capital used to compute total return.
     * @param benchmarkBars Daily bars for the benchmark ticker; may be null to skip CAPM.
     * @return Fully populated {@link PerformanceMetrics}.
     */
    public PerformanceMetrics calculate(List<PortfolioSnapshot> snapshots,
                                         List<Fill> fills,
                                         BigDecimal initialCash,
                                         List<Bar> benchmarkBars) {
        if (snapshots.isEmpty()) {
            return PerformanceMetrics.empty();
        }

        BigDecimal finalEquity = snapshots.get(snapshots.size() - 1).totalEquity();
        BigDecimal totalReturn = finalEquity.subtract(initialCash)
                .divide(initialCash, 8, RoundingMode.HALF_UP);

        // Daily returns
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < snapshots.size(); i++) {
            BigDecimal prev = snapshots.get(i - 1).totalEquity();
            BigDecimal curr = snapshots.get(i).totalEquity();
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                dailyReturns.add(curr.subtract(prev).divide(prev, 10, RoundingMode.HALF_UP).doubleValue());
            }
        }

        int days = snapshots.size();

        // Annualized return: (1 + R)^(252/days) - 1
        double annualizedReturn = days > 0
                ? Math.pow(1 + totalReturn.doubleValue(), 252.0 / days) - 1
                : 0.0;

        // Annualized volatility: std dev of daily returns × √252
        double annualizedVolatility = 0.0;
        if (dailyReturns.size() > 1) {
            double mean = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance = dailyReturns.stream()
                    .mapToDouble(r -> Math.pow(r - mean, 2))
                    .average().orElse(0);
            annualizedVolatility = Math.sqrt(variance) * Math.sqrt(252);
        }

        // Sharpe ratio (risk-free rate = 0)
        double sharpeRatio = annualizedVolatility > 0 ? annualizedReturn / annualizedVolatility : 0.0;

        // Max drawdown
        double maxDrawdown = calculateMaxDrawdown(snapshots);

        // Trade-level metrics
        TradeMetrics tradeMetrics = calculateTradeMetrics(fills);

        // Alpha / Beta vs benchmark (CAPM, risk-free rate = 0)
        BigDecimal alpha = null;
        BigDecimal beta = null;
        if (benchmarkBars != null && benchmarkBars.size() > 1 && dailyReturns.size() > 1) {
            // Step 1: Compute daily benchmark returns from consecutive close prices
            List<Double> benchmarkReturns = new ArrayList<>();
            for (int i = 1; i < benchmarkBars.size(); i++) {
                double prev = benchmarkBars.get(i - 1).close().doubleValue();
                double curr = benchmarkBars.get(i).close().doubleValue();
                if (prev > 0) {
                    benchmarkReturns.add((curr - prev) / prev);
                }
            }

            // Step 2: Align series to the shorter length to avoid index mismatch
            int n = Math.min(dailyReturns.size(), benchmarkReturns.size());
            if (n > 1) {
                double stratMean = dailyReturns.subList(0, n).stream().mapToDouble(d -> d).average().orElse(0);
                double benchMean = benchmarkReturns.subList(0, n).stream().mapToDouble(d -> d).average().orElse(0);

                // Step 3: Compute covariance(strategy, benchmark) and variance(benchmark)
                double covariance = 0.0;
                double benchVariance = 0.0;
                for (int i = 0; i < n; i++) {
                    double sd = dailyReturns.get(i) - stratMean;
                    double bd = benchmarkReturns.get(i) - benchMean;
                    covariance += sd * bd;
                    benchVariance += bd * bd;
                }
                // Divide by n (population covariance/variance — consistent with both series)
                covariance /= n;
                benchVariance /= n;

                if (benchVariance > 0) {
                    // Beta = Cov(strategy, benchmark) / Var(benchmark)
                    double betaVal = covariance / benchVariance;
                    // Annualise mean daily benchmark return: (1 + dailyMean)^252 - 1
                    double benchAnnualizedReturn = Math.pow(1 + benchMean, 252) - 1;
                    // Alpha = strategy annualised return - beta * benchmark annualised return
                    double alphaVal = annualizedReturn - betaVal * benchAnnualizedReturn;
                    alpha = bd(alphaVal);
                    beta = bd(betaVal);
                }
            }
        }

        return new PerformanceMetrics(
                totalReturn.setScale(6, RoundingMode.HALF_UP),
                bd(annualizedReturn),
                bd(annualizedVolatility),
                bd(sharpeRatio),
                bd(maxDrawdown),
                tradeMetrics.winRate(),
                tradeMetrics.avgWin(),
                tradeMetrics.avgLoss(),
                tradeMetrics.profitFactor(),
                tradeMetrics.totalTrades(),
                alpha,
                beta
        );
    }

    /**
     * Computes the maximum peak-to-trough drawdown over the equity curve.
     * Tracks the running peak equity and records the largest percentage decline
     * from any peak to any subsequent trough.
     *
     * @param snapshots Daily equity snapshots in chronological order.
     * @return Max drawdown as a positive decimal fraction (e.g. 0.20 = 20% drawdown).
     */
    private double calculateMaxDrawdown(List<PortfolioSnapshot> snapshots) {
        double peak = Double.NEGATIVE_INFINITY;
        double maxDrawdown = 0.0;
        for (PortfolioSnapshot snapshot : snapshots) {
            double equity = snapshot.totalEquity().doubleValue();
            if (equity > peak) peak = equity;
            if (peak > 0) {
                double drawdown = (peak - equity) / peak;
                if (drawdown > maxDrawdown) maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    /**
     * Computes win rate, average win/loss, and profit factor from the fill history.
     * Uses a FIFO matching approach: each SELL fill is paired with the earliest
     * unmatched BUY fill for the same ticker to form a closed round-trip trade.
     * P&amp;L = (sellPrice - buyPrice) × min(sellQty, buyQty) - commissions.
     *
     * @param fills Ordered list of all fills from the simulation.
     * @return Trade-level metrics aggregate.
     */
    private TradeMetrics calculateTradeMetrics(List<Fill> fills) {
        Map<String, Deque<Fill>> openPositions = new HashMap<>();
        List<BigDecimal> pnls = new ArrayList<>();

        for (Fill fill : fills) {
            if (fill.side() == OrderSide.BUY) {
                openPositions.computeIfAbsent(fill.ticker(), k -> new ArrayDeque<>()).add(fill);
            } else {
                Deque<Fill> opens = openPositions.get(fill.ticker());
                if (opens != null && !opens.isEmpty()) {
                    Fill openFill = opens.poll();
                    BigDecimal pnl = fill.fillPrice()
                            .subtract(openFill.fillPrice())
                            .multiply(BigDecimal.valueOf(Math.min(fill.quantityFilled(), openFill.quantityFilled())))
                            .subtract(fill.commission())
                            .subtract(openFill.commission());
                    pnls.add(pnl);
                }
            }
        }

        if (pnls.isEmpty()) {
            return new TradeMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        List<BigDecimal> wins = pnls.stream().filter(p -> p.compareTo(BigDecimal.ZERO) > 0).toList();
        List<BigDecimal> losses = pnls.stream().filter(p -> p.compareTo(BigDecimal.ZERO) <= 0).toList();

        BigDecimal winRate = BigDecimal.valueOf(wins.size())
                .divide(BigDecimal.valueOf(pnls.size()), 4, RoundingMode.HALF_UP);

        BigDecimal avgWin = wins.isEmpty() ? BigDecimal.ZERO
                : wins.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(wins.size()), 4, RoundingMode.HALF_UP);

        BigDecimal avgLoss = losses.isEmpty() ? BigDecimal.ZERO
                : losses.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(losses.size()), 4, RoundingMode.HALF_UP);

        BigDecimal totalWins = wins.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLossAbs = losses.stream()
                .map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitFactor = totalLossAbs.compareTo(BigDecimal.ZERO) > 0
                ? totalWins.divide(totalLossAbs, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new TradeMetrics(winRate, avgWin, avgLoss, profitFactor, pnls.size());
    }

    /**
     * Converts a {@code double} to a {@link java.math.BigDecimal} rounded to 6 decimal places.
     * Used throughout to keep metrics at a consistent precision.
     *
     * @param value Raw double value.
     * @return Scaled BigDecimal.
     */
    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }
}
