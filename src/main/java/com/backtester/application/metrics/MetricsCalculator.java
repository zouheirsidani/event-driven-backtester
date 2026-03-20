package com.backtester.application.metrics;

import com.backtester.domain.backtest.PerformanceMetrics;
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

@Component
public class MetricsCalculator {

    private record TradeMetrics(
            BigDecimal winRate,
            BigDecimal avgWin,
            BigDecimal avgLoss,
            BigDecimal profitFactor,
            int totalTrades
    ) {}

    public PerformanceMetrics calculate(List<PortfolioSnapshot> snapshots,
                                         List<Fill> fills,
                                         BigDecimal initialCash) {
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
                tradeMetrics.totalTrades()
        );
    }

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

    private TradeMetrics calculateTradeMetrics(List<Fill> fills) {
        // Match BUY fills to SELL fills FIFO per ticker to compute round-trip PnL
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

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }
}
