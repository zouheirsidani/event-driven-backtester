ALTER TABLE backtest_runs
    ADD COLUMN IF NOT EXISTS benchmark_ticker VARCHAR(20);
