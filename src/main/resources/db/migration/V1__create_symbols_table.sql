CREATE TABLE symbols (
    ticker      VARCHAR(20)  NOT NULL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    exchange    VARCHAR(50)  NOT NULL,
    asset_class VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
