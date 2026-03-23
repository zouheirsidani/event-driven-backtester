/**
 * TypeScript type definitions for all API request and response shapes.
 * These mirror the Java record/DTO classes in the backend and are used by
 * api.ts and throughout the React pages to type-check API data.
 */

// ── Market Data ──────────────────────────────────────────────────────────────

/** Classification of a tradable financial instrument. */
export type AssetClass = "STOCK" | "ETF" | "FUTURES" | "CRYPTO";

/** A registered tradable symbol returned by the symbols endpoint. */
export interface SymbolDto {
  ticker: string;
  name: string;
  exchange: string;
  assetClass: AssetClass;
}

/** Response envelope for GET /market-data/symbols. */
export interface SymbolsResponse {
  symbols: SymbolDto[];
  count: number;
}

/** A single OHLCV price bar for a trading day. */
export interface BarDto {
  ticker: string;
  /** ISO 8601 date string, e.g. "2022-01-03". */
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

/** Paginated response for GET /market-data/{ticker}/bars. */
export interface BarsResponse {
  ticker: string;
  bars: BarDto[];
  count: number;
}

/** Summary returned after a bar ingestion operation. */
export interface IngestResponse {
  ticker: string;
  /** Number of new bars saved (duplicates excluded). */
  barsIngested: number;
  /** Number of bars skipped because they already existed. */
  barsSkipped: number;
}

// ── Strategies ───────────────────────────────────────────────────────────────

/** A registered trading strategy available for backtesting. */
export interface StrategyDto {
  /** Machine-readable identifier used in RunBacktestRequest. */
  strategyId: string;
  /** Human-readable label shown in the UI strategy drop-down. */
  displayName: string;
}

/** Response envelope for GET /strategies. */
export interface StrategiesResponse {
  strategies: StrategyDto[];
}

// ── Backtests ─────────────────────────────────────────────────────────────────

/** Lifecycle state of a backtest run. */
export type BacktestStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";

/** A backtest run record returned by the backtests API. */
export interface BacktestRunDto {
  runId: string;
  strategyId: string;
  tickers: string[];
  /** ISO 8601 date string, e.g. "2021-01-01". */
  startDate: string;
  /** ISO 8601 date string, e.g. "2023-12-31". */
  endDate: string;
  initialCash: number;
  status: BacktestStatus;
  /** ISO 8601 instant string. */
  createdAt: string;
  /** Optional benchmark ticker for CAPM metrics; omitted if not specified. */
  benchmarkTicker?: string;
}

/** Paginated response for GET /backtests. */
export interface BacktestRunsResponse {
  runs: BacktestRunDto[];
  /** Number of runs in this page. */
  count: number;
  /** Total runs across all pages. */
  totalCount: number;
  page: number;
  size: number;
}

/**
 * Aggregate performance metrics for a completed backtest.
 * All rate/ratio fields are expressed as decimal fractions (e.g. 0.15 = 15%).
 */
export interface PerformanceMetricsDto {
  totalReturn: number;
  annualizedReturn: number;
  annualizedVolatility: number;
  sharpeRatio: number;
  /** Positive decimal; e.g. 0.20 means a 20% peak-to-trough decline. */
  maxDrawdown: number;
  winRate: number;
  avgWin: number;
  avgLoss: number;
  profitFactor: number;
  totalTrades: number;
}

/** A single point on the daily portfolio equity curve. */
export interface EquityCurvePointDto {
  date: string;
  equity: number;
}

/** A single executed trade fill. */
export interface TradeDto {
  fillId: string;
  ticker: string;
  side: "BUY" | "SELL";
  quantity: number;
  /** Execution price after slippage. */
  price: number;
  commission: number;
  date: string;
}

/** Full result for a completed backtest run. */
export interface BacktestResultResponse {
  runId: string;
  metrics: PerformanceMetricsDto;
  equityCurve: EquityCurvePointDto[];
  trades: TradeDto[];
  completedAt: string;
}

// ── Requests ──────────────────────────────────────────────────────────────────

/** Request body for POST /market-data/symbols. */
export interface CreateSymbolRequest {
  ticker: string;
  name: string;
  exchange: string;
  assetClass: AssetClass;
}

/** Individual OHLCV bar within an IngestBarsRequest. */
export interface BarData {
  /** ISO 8601 date string, e.g. "2022-01-03". */
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

/** Request body for POST /market-data/ingest (JSON body ingestion). */
export interface IngestBarsRequest {
  ticker: string;
  bars: BarData[];
}

/** Request body for POST /backtests. */
export interface RunBacktestRequest {
  strategyId: string;
  tickers: string[];
  /** ISO 8601 date string, e.g. "2021-01-01". */
  startDate: string;
  /** ISO 8601 date string, e.g. "2023-12-31". */
  endDate: string;
  initialCash: number;
  /** "FIXED" or "PERCENT"; defaults to FIXED with zero amount if omitted. */
  slippageType?: string;
  slippageAmount?: number;
  /** "FIXED" or "PER_SHARE"; defaults to FIXED with zero amount if omitted. */
  commissionType?: string;
  commissionAmount?: number;
}
