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
`BacktestExecutor` (separate `@Component`) holds the `@Async("backtestExecutor")` method. `BacktestService` delegates to it to avoid Spring AOP self-proxy issues. The executor thread pool is declared in `infrastructure/config/AsyncConfig.java`.

### JSONB storage
`BacktestRunEntity` and `BacktestResultEntity` store structured fields as JSONB. String fields use `@ColumnTransformer(write = "?::jsonb")` for PostgreSQL casting. The `tickers` field (`List<String>`) uses Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)`. Sealed `SlippageModel`/`CommissionModel` carry `@JsonTypeInfo`/`@JsonSubTypes` annotations (Jackson, not Spring) to support polymorphic JSONB round-trips via the entity mappers.

### Adding a new strategy
1. Create `strategy/<name>/<Name>Strategy.java` implementing `domain/strategy/Strategy`
2. Annotate with `@Component`
3. Return a unique `strategyId()` string
4. The strategy is auto-discovered via Spring's `List<Strategy>` injection in `BacktestExecutor` and `StrategyController`

## Key design rules
- Domain records are immutable; `Portfolio` is the only mutable domain class (mutated by `applyFill` and `updatePrices` inside the event loop)
- `BacktestRun.withStatus(newStatus)` creates a new record instance — the pattern for "updating" immutable domain objects
- `PositionSizer` allocates 10% of portfolio equity per signal (fixed-fractional); `SignalDirection.SHORT` is a no-op in V1
- `ddl-auto=validate` — schema is owned by Flyway (V1–V4 in `src/main/resources/db/migration/`); never use `create` or `update`
