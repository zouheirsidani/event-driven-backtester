package com.backtester.api.mapper;

import com.backtester.api.dto.response.BacktestResultResponse;
import com.backtester.api.dto.response.BacktestRunDto;
import com.backtester.api.dto.response.EquityCurvePointDto;
import com.backtester.api.dto.response.PerformanceMetricsDto;
import com.backtester.api.dto.response.TradeDto;
import com.backtester.domain.backtest.BacktestResult;
import com.backtester.domain.backtest.BacktestRun;
import com.backtester.domain.backtest.PerformanceMetrics;
import com.backtester.domain.order.Fill;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts backtest domain objects to their corresponding API response DTOs.
 * Keeps the API layer decoupled from domain internals (e.g. enum types vs. strings).
 */
@Component
public class BacktestDtoMapper {

    /**
     * Converts a {@link BacktestRun} domain record to a {@link BacktestRunDto}.
     * The {@code status} enum is converted to its string name for JSON serialisation.
     *
     * @param run Domain run record.
     * @return API response DTO.
     */
    public BacktestRunDto toRunDto(BacktestRun run) {
        return new BacktestRunDto(
                run.runId(),
                run.strategyId(),
                run.tickers(),
                run.startDate(),
                run.endDate(),
                run.initialCash(),
                run.status().name(),
                run.createdAt(),
                run.benchmarkTicker()
        );
    }

    /**
     * Converts a {@link BacktestResult} domain record to a {@link BacktestResultResponse}.
     * Delegates to helper methods for metrics and trade conversion.
     *
     * @param result Domain result record.
     * @return API response DTO with metrics, equity curve, and trade list.
     */
    public BacktestResultResponse toResultResponse(BacktestResult result) {
        List<EquityCurvePointDto> curve = result.equityCurve().stream()
                .map(p -> new EquityCurvePointDto(p.date(), p.equity()))
                .toList();

        List<TradeDto> trades = result.trades().stream()
                .map(this::toTradeDto)
                .toList();

        return new BacktestResultResponse(
                result.runId(),
                toMetricsDto(result.metrics()),
                curve,
                trades,
                result.completedAt()
        );
    }

    private PerformanceMetricsDto toMetricsDto(PerformanceMetrics m) {
        return new PerformanceMetricsDto(
                m.totalReturn(),
                m.annualizedReturn(),
                m.annualizedVolatility(),
                m.sharpeRatio(),
                m.maxDrawdown(),
                m.winRate(),
                m.avgWin(),
                m.avgLoss(),
                m.profitFactor(),
                m.totalTrades(),
                m.alpha(),
                m.beta()
        );
    }

    private TradeDto toTradeDto(Fill fill) {
        return new TradeDto(
                fill.fillId(),
                fill.ticker(),
                fill.side().name(),
                fill.quantityFilled(),
                fill.fillPrice(),
                fill.commission(),
                fill.date()
        );
    }
}
