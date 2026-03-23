# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build              # compile + test
./gradlew bootJar            # fat JAR at build/libs/

# Test
./gradlew test               # all unit tests
./gradlew test --tests "com.backtester.domain.portfolio.PortfolioTest"
./gradlew test --tests "com.backtester.application.backtest.EventLoopTest"
./gradlew test --info        # verbose output on failure

# Run (requires PostgreSQL on localhost:5432)
./gradlew bootRun

# Full stack
docker-compose up            # PostgreSQL 16 + app on :8080
docker-compose up postgres   # DB only (then bootRun locally)
```

Windows: use `.\gradlew.bat` instead of `./gradlew`.

## Architecture

Five-layer hexagonal architecture. The critical invariant: **domain layer has zero Spring/JPA/Jackson imports** — it is pure Java 21 and fully unit-testable without a Spring context.

```
api.*           REST controllers, record DTOs, @ControllerAdvice
application.*   Services, EventLoop, MetricsCalculator — depend on port interfaces only
domain.*        Records, sealed classes, enums, Strategy interface — no framework deps
infrastructure.*  JPA entities, Flyway, Spring Data repos, adapters that implement ports
strategy.*      @Component beans implementing Strategy interface (resolved by strategyId string)
```

### Port/Adapter wiring
`application/port/` defines four repository interfaces (`BarRepository`, `SymbolRepository`, `BacktestRunRepository`, `BacktestResultRepository`). Their implementations live in `infrastructure/persistence/adapter/` and are the only place JPA is touched. Application services are injected with the port interfaces, never the JPA types.

### Event loop (`application/backtest/EventLoop.java`)
The simulation core. Per trading day, in strict causal order:
1. Emit `MarketDataEvent` per ticker → call `strategy.onBar()` → queue `SignalEvent`
2. `SignalEvent` → `PositionSizer` → `OrderEvent`
3. `OrderEvent` → apply slippage + commission → `FillEvent`
4. `FillEvent` → `portfolio.applyFill()`
5. `portfolio.updatePrices(closePrices)` → `portfolio.takeSnapshot(date)`

Uses `ArrayDeque<TradingEvent>` — synchronous and deterministic. The sealed `TradingEvent` hierarchy enables exhaustive `switch` expressions throughout.

### Async execution
`BacktestExecutor` (separate `@Component`) holds the `@Async("backtestThreadPool")` method. `BacktestService` delegates to it to avoid Spring AOP self-proxy issues. The executor thread pool bean is named `backtestThreadPool` and declared in `infrastructure/config/AsyncConfig.java`. (Named `backtestThreadPool` to avoid a bean name clash with the `BacktestExecutor` component itself.)

### JSONB storage
`BacktestRunEntity` and `BacktestResultEntity` store structured fields as JSONB. String fields use `@ColumnTransformer(write = "?::jsonb")` for PostgreSQL casting. The `tickers` field (`List<String>`) uses Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)`. Sealed `SlippageModel`/`CommissionModel` carry `@JsonTypeInfo`/`@JsonSubTypes` annotations (Jackson, not Spring) to support polymorphic JSONB round-trips via the entity mappers.

### Adding a new strategy
1. Create `strategy/<name>/<Name>Strategy.java` implementing `domain/strategy/Strategy`
2. Annotate with `@Component`
3. Return a unique `strategyId()` string
4. The strategy is auto-discovered via Spring's `List<Strategy>` injection in `BacktestExecutor` and `StrategyController`

## Project milestones

### Milestone 1 — COMPLETE ✓
- Full domain model (records, sealed events, sealed slippage/commission models)
- `MomentumStrategy` (MOMENTUM_V1) — 20-day return lookback
- Market data ingestion + symbol registration + bar query endpoints
- `BacktestService` + `EventLoop` with `FixedSlippage` + `FixedCommission`
- `Portfolio` with cash/position tracking, `applyFill`, snapshots
- `MetricsCalculator` — total return, Sharpe, max drawdown, win rate, profit factor
- All 10 REST endpoints (compare returns empty for M1)
- Flyway migrations V1–V4, Docker + Docker Compose
- Unit tests: `PortfolioTest`, `MetricsCalculatorTest`, `MomentumStrategyTest`, `EventLoopTest`

### Milestone 2 — COMPLETE ✓
- **`MeanReversionStrategy`** (`MEAN_REVERSION_V1`) — Bollinger Band / z-score, 20-day window; BUY at z < -2, EXIT at z ≥ 0; in `strategy/meanreversion/`
- **Wire `PercentSlippage` + `PerShareCommission`** — exposed via `RunBacktestRequest` (`slippageType`/`commissionType`); resolved in `BacktestService.buildSlippageModel/buildCommissionModel`
- **Alpha/Beta vs benchmark** — optional `benchmarkTicker` on `RunBacktestRequest`; `MetricsCalculator` computes CAPM alpha/beta when benchmark bars available; `PerformanceMetrics` + `PerformanceMetricsDto` extended; Flyway V5 adds `benchmark_ticker` column
- **CSV bulk ingestion** — `POST /api/v1/market-data/ingest/csv` (multipart, columns: date,open,high,low,close,volume); parsed in `MarketDataService.ingestBarsFromCsv`
- **Pagination** — `page`/`size` query params on `GET /{ticker}/bars` and `GET /backtests`; `BarsResponse` includes `totalCount`; `GET /backtests` returns `BacktestRunsResponse`
- **OpenAPI/Swagger** — `springdoc-openapi-starter-webmvc-ui:2.3.0`; UI auto-discovered at `/swagger-ui.html`

### Milestone 3 — FUTURE
- Real market data integration (Yahoo Finance / Alpha Vantage adapter)
- Portfolio-level multi-ticker backtesting with correlation-aware position sizing
- Walk-forward optimisation / parameter sweep
- WebSocket streaming of live backtest progress events

## Git workflow

After completing any meaningful unit of work, commit and push immediately so the repository on GitHub always reflects the current state. Never leave work uncommitted at the end of a session.

```bash
git add <specific files>
git commit -m "<type>(<scope>): <short summary>"
git push
```

**Commit types:** `feat`, `fix`, `test`, `refactor`, `chore`, `docs`
**Scope:** layer or component, e.g. `domain`, `api`, `eventloop`, `metrics`, `strategy`

Commit granularity guidelines:
- One logical change per commit (new feature, bug fix, test addition, config change)
- Never bundle unrelated changes in a single commit
- Commit after each completed feature, each bug fix, and each test addition — don't batch them up

Use `git push` after every commit. The remote at `https://github.com/zouheirsidani/event-driven-backtester` is the source of truth for recovery.

## Code style

- **Always add Javadoc comments to every new class and method you create.** Include a summary line, `@param` tags for non-obvious parameters, and `@return` where applicable.
- Add inline comments for any logic that isn't immediately self-evident (e.g. slippage arithmetic, z-score calculation, event queue ordering).
- Frontend: add JSDoc comments to all new functions and components.

## Key design rules
- Domain records are immutable; `Portfolio` is the only mutable domain class (mutated by `applyFill` and `updatePrices` inside the event loop)
- `BacktestRun.withStatus(newStatus)` creates a new record instance — the pattern for "updating" immutable domain objects
- `PositionSizer` allocates 10% of portfolio equity per signal (fixed-fractional); `SignalDirection.SHORT` is a no-op in V1
- `ddl-auto=validate` — schema is owned by Flyway (V1–V5 in `src/main/resources/db/migration/`); never use `create` or `update`
