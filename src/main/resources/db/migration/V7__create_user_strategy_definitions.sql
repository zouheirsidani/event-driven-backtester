CREATE TABLE user_strategy_definitions (
    id           UUID PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    base_strategy_id VARCHAR(100) NOT NULL,
    parameters   JSONB NOT NULL DEFAULT '{}',
    created_at   TIMESTAMPTZ NOT NULL
);
