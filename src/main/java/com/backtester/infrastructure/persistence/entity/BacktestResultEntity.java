package com.backtester.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backtest_results")
public class BacktestResultEntity {

    @Id
    @Column(name = "run_id")
    private UUID runId;

    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb", nullable = false)
    private String metrics;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "equity_curve", columnDefinition = "jsonb", nullable = false)
    private String equityCurve;

    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb", nullable = false)
    private String trades;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected BacktestResultEntity() {}

    public BacktestResultEntity(UUID runId, String metrics, String equityCurve,
                                 String trades, Instant completedAt) {
        this.runId = runId;
        this.metrics = metrics;
        this.equityCurve = equityCurve;
        this.trades = trades;
        this.completedAt = completedAt;
    }

    public UUID getRunId() { return runId; }
    public String getMetrics() { return metrics; }
    public String getEquityCurve() { return equityCurve; }
    public String getTrades() { return trades; }
    public Instant getCompletedAt() { return completedAt; }
}
