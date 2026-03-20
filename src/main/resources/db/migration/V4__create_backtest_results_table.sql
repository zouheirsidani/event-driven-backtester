CREATE TABLE backtest_results (
    run_id       UUID        NOT NULL PRIMARY KEY REFERENCES backtest_runs (run_id),
    metrics      JSONB       NOT NULL,
    equity_curve JSONB       NOT NULL,
    trades       JSONB       NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
