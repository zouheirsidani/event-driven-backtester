CREATE TABLE backtest_runs (
    run_id            UUID         NOT NULL PRIMARY KEY,
    strategy_id       VARCHAR(100) NOT NULL,
    tickers           JSONB        NOT NULL,
    start_date        DATE         NOT NULL,
    end_date          DATE         NOT NULL,
    initial_cash      NUMERIC(18, 2) NOT NULL,
    slippage_config   JSONB        NOT NULL,
    commission_config JSONB        NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_backtest_runs_status ON backtest_runs (status);
CREATE INDEX idx_backtest_runs_created_at ON backtest_runs (created_at DESC);
