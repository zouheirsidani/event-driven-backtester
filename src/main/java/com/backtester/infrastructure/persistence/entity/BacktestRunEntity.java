package com.backtester.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "backtest_runs")
public class BacktestRunEntity {

    @Id
    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "strategy_id", nullable = false)
    private String strategyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tickers", columnDefinition = "jsonb", nullable = false)
    private List<String> tickers;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "initial_cash", nullable = false, precision = 18, scale = 2)
    private BigDecimal initialCash;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "slippage_config", columnDefinition = "jsonb", nullable = false)
    private String slippageConfig;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "commission_config", columnDefinition = "jsonb", nullable = false)
    private String commissionConfig;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BacktestRunEntity() {}

    public BacktestRunEntity(UUID runId, String strategyId, List<String> tickers,
                              LocalDate startDate, LocalDate endDate, BigDecimal initialCash,
                              String slippageConfig, String commissionConfig,
                              String status, Instant createdAt) {
        this.runId = runId;
        this.strategyId = strategyId;
        this.tickers = tickers;
        this.startDate = startDate;
        this.endDate = endDate;
        this.initialCash = initialCash;
        this.slippageConfig = slippageConfig;
        this.commissionConfig = commissionConfig;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getRunId() { return runId; }
    public String getStrategyId() { return strategyId; }
    public List<String> getTickers() { return tickers; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getInitialCash() { return initialCash; }
    public String getSlippageConfig() { return slippageConfig; }
    public String getCommissionConfig() { return commissionConfig; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
}
