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

const client = axios.create({ baseURL: "/api/v1" });

// ── Market Data ───────────────────────────────────────────────────────────────

export const getSymbols = () =>
  client.get<SymbolsResponse>("/market-data/symbols").then((r) => r.data);

export const createSymbol = (req: CreateSymbolRequest) =>
  client.post<SymbolDto>("/market-data/symbols", req).then((r) => r.data);

export const ingestBars = (req: IngestBarsRequest) =>
  client.post<IngestResponse>("/market-data/ingest", req).then((r) => r.data);

export const getBars = (ticker: string, from: string, to: string) =>
  client
    .get<BarsResponse>(`/market-data/${ticker}/bars`, { params: { from, to } })
    .then((r) => r.data);

// ── Strategies ────────────────────────────────────────────────────────────────

export const getStrategies = () =>
  client.get<StrategiesResponse>("/strategies").then((r) => r.data);

// ── Backtests ─────────────────────────────────────────────────────────────────

export const submitBacktest = (req: RunBacktestRequest) =>
  client.post<BacktestRunDto>("/backtests", req).then((r) => r.data);

export const getBacktests = () =>
  client.get<BacktestRunsResponse>("/backtests").then((r) => r.data);

export const getBacktestRun = (runId: string) =>
  client.get<BacktestRunDto>(`/backtests/${runId}`).then((r) => r.data);

export const getBacktestResult = (runId: string) =>
  client
    .get<BacktestResultResponse>(`/backtests/${runId}/results`)
    .then((r) => r.data);
