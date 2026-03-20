CREATE TABLE bars (
    id     BIGSERIAL    NOT NULL PRIMARY KEY,
    ticker VARCHAR(20)  NOT NULL REFERENCES symbols (ticker),
    date   DATE         NOT NULL,
    open   NUMERIC(18, 6) NOT NULL,
    high   NUMERIC(18, 6) NOT NULL,
    low    NUMERIC(18, 6) NOT NULL,
    close  NUMERIC(18, 6) NOT NULL,
    volume BIGINT       NOT NULL,
    CONSTRAINT bars_ticker_date_unique UNIQUE (ticker, date)
);

CREATE INDEX idx_bars_ticker_date ON bars (ticker, date);
