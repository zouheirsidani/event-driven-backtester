ALTER TABLE backtest_runs ADD COLUMN sweep_id UUID NULL;
CREATE INDEX idx_backtest_runs_sweep_id ON backtest_runs(sweep_id);
