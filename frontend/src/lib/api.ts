/**
 * API client module for the backtester backend.
 * All functions return typed promises and use the shared axios instance
 * pre-configured with the `/api/v1` base URL (proxied by Vite to the Spring Boot server).
 */
import axios from "axios";
import type {
  SymbolsResponse,
  SymbolDto,
  BarsResponse,
  IngestResponse,
  StrategiesResponse,
  BacktestRunDto,
  BacktestRunsResponse,
  BacktestResultResponse,
  CreateSymbolRequest,
  IngestBarsRequest,
  RunBacktestRequest,
} from "./types";

/** Axios instance pre-configured with the backend's API base path. */
const client = axios.create({ baseURL: "/api/v1" });

// ── Market Data ───────────────────────────────────────────────────────────────

/** Fetches all registered symbols. */
export const getSymbols = () =>
  client.get<SymbolsResponse>("/market-data/symbols").then((r) => r.data);

/**
 * Registers a new symbol.
 * @param req Symbol creation payload.
 */
export const createSymbol = (req: CreateSymbolRequest) =>
  client.post<SymbolDto>("/market-data/symbols", req).then((r) => r.data);

/**
 * Ingests OHLCV bars from a JSON body.
 * @param req Ticker and bar list to ingest.
 */
export const ingestBars = (req: IngestBarsRequest) =>
  client.post<IngestResponse>("/market-data/ingest", req).then((r) => r.data);

/**
 * Queries stored price bars for a ticker within a date range.
 * @param ticker Uppercase ticker symbol.
 * @param from   ISO 8601 start date string.
 * @param to     ISO 8601 end date string.
 */
export const getBars = (ticker: string, from: string, to: string) =>
  client
    .get<BarsResponse>(`/market-data/${ticker}/bars`, { params: { from, to } })
    .then((r) => r.data);

// ── Strategies ────────────────────────────────────────────────────────────────

/** Fetches all registered strategy descriptors (used to populate the strategy selector). */
export const getStrategies = () =>
  client.get<StrategiesResponse>("/strategies").then((r) => r.data);

// ── Backtests ─────────────────────────────────────────────────────────────────

/**
 * Submits a new backtest run.  Returns immediately with a PENDING run; execution
 * happens asynchronously on the server.
 * @param req Backtest configuration payload.
 */
export const submitBacktest = (req: RunBacktestRequest) =>
  client.post<BacktestRunDto>("/backtests", req).then((r) => r.data);

/** Fetches all backtest runs (most recent first, first page). */
export const getBacktests = () =>
  client.get<BacktestRunsResponse>("/backtests").then((r) => r.data);

/**
 * Fetches a single backtest run by its UUID.
 * @param runId UUID string of the backtest run.
 */
export const getBacktestRun = (runId: string) =>
  client.get<BacktestRunDto>(`/backtests/${runId}`).then((r) => r.data);

/**
 * Fetches the result for a completed backtest run.
 * Will throw (404) if the run has not yet completed.
 * @param runId UUID string of the backtest run.
 */
export const getBacktestResult = (runId: string) =>
  client
    .get<BacktestResultResponse>(`/backtests/${runId}/results`)
    .then((r) => r.data);
