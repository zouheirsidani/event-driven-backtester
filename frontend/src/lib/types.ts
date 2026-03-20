// ── Market Data ──────────────────────────────────────────────────────────────

export type AssetClass = "STOCK" | "ETF" | "FUTURES" | "CRYPTO";

export interface SymbolDto {
  ticker: string;
  name: string;
  exchange: string;
  assetClass: AssetClass;
}

export interface SymbolsResponse {
  symbols: SymbolDto[];
  count: number;
}

export interface BarDto {
  ticker: string;
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface BarsResponse {
  ticker: string;
  bars: BarDto[];
  count: number;
}

export interface IngestResponse {
  ticker: string;
  barsIngested: number;
  barsSkipped: number;
}

// ── Strategies ───────────────────────────────────────────────────────────────

export interface StrategyDto {
  strategyId: string;
  displayName: string;
}

export interface StrategiesResponse {
  strategies: StrategyDto[];
}

// ── Backtests ─────────────────────────────────────────────────────────────────

export type BacktestStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";

export interface BacktestRunDto {
  runId: string;
  strategyId: string;
  tickers: string[];
  startDate: string;
  endDate: string;
  initialCash: number;
  status: BacktestStatus;
  createdAt: string;
}

export interface PerformanceMetricsDto {
  totalReturn: number;
  annualizedReturn: number;
  annualizedVolatility: number;
  sharpeRatio: number;
  maxDrawdown: number;
  winRate: number;
  avgWin: number;
  avgLoss: number;
  profitFactor: number;
  totalTrades: number;
}

export interface EquityCurvePointDto {
  date: string;
  equity: number;
}

export interface TradeDto {
  fillId: string;
  ticker: string;
  side: "BUY" | "SELL";
  quantity: number;
  price: number;
  commission: number;
  date: string;
}

export interface BacktestResultResponse {
  runId: string;
  metrics: PerformanceMetricsDto;
  equityCurve: EquityCurvePointDto[];
  trades: TradeDto[];
  completedAt: string;
}

// ── Requests ──────────────────────────────────────────────────────────────────

export interface CreateSymbolRequest {
  ticker: string;
  name: string;
  exchange: string;
  assetClass: AssetClass;
}

export interface BarData {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface IngestBarsRequest {
  ticker: string;
  bars: BarData[];
}

export interface RunBacktestRequest {
  strategyId: string;
  tickers: string[];
  startDate: string;
  endDate: string;
  initialCash: number;
  slippageType?: string;
  slippageAmount?: number;
  commissionType?: string;
  commissionAmount?: number;
}
