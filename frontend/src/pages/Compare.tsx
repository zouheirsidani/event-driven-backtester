/**
 * Compare page — select two or more completed backtest runs and view their
 * performance metrics side by side, plus overlaid equity curves on a single chart.
 *
 * The user picks runs from a multi-select list of completed runs.
 * Clicking "Compare" posts the selected run IDs to POST /api/v1/backtests/compare
 * and renders:
 *  - A side-by-side metrics table (one column per run).
 *  - A Recharts AreaChart with one line per run, each coloured distinctly.
 */
import { useState } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { getBacktests, compareBacktests } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { formatPct, formatNumber, formatCurrency } from "@/lib/utils";
import type { BacktestResultResponse, BacktestRunDto } from "@/lib/types";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";

/** Distinct colours for up to 6 overlaid equity curves. */
const PALETTE = ["#6366f1", "#22c55e", "#f59e0b", "#ef4444", "#06b6d4", "#a855f7"];

export default function Compare() {
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [results, setResults] = useState<BacktestResultResponse[]>([]);

  const { data: runs } = useQuery({
    queryKey: ["backtests"],
    queryFn: getBacktests,
    refetchInterval: 5000,
  });

  const completedRuns = runs?.runs.filter((r) => r.status === "COMPLETED") ?? [];

  const mutation = useMutation({
    mutationFn: compareBacktests,
    onSuccess: (data) => setResults(data.results),
  });

  /** Toggle a run ID in/out of the selection (cap at 6). */
  function toggleRun(runId: string) {
    setSelectedIds((prev) =>
      prev.includes(runId)
        ? prev.filter((id) => id !== runId)
        : prev.length < 6
        ? [...prev, runId]
        : prev
    );
  }

  function handleCompare() {
    if (selectedIds.length < 2) return;
    mutation.mutate({ runIds: selectedIds });
  }

  // Build a merged equity-curve dataset keyed by date, one field per run
  const mergedChart = buildMergedChart(results, completedRuns);

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Compare Backtests</h1>
        <p className="text-muted-foreground mt-1">
          Select 2–6 completed runs to compare metrics and equity curves side by side.
        </p>
      </div>

      {/* Run selector */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Select Runs</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {completedRuns.length === 0 ? (
            <p className="text-sm text-muted-foreground">No completed runs yet.</p>
          ) : (
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {completedRuns.map((run) => {
                const selected = selectedIds.includes(run.runId);
                const idx = selectedIds.indexOf(run.runId);
                return (
                  <button
                    key={run.runId}
                    onClick={() => toggleRun(run.runId)}
                    className={`text-left rounded-md border px-3 py-2.5 text-sm transition-colors ${
                      selected
                        ? "border-primary bg-primary/10"
                        : "hover:bg-muted/40"
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      {selected && (
                        <span
                          className="inline-block h-2.5 w-2.5 rounded-full shrink-0"
                          style={{ backgroundColor: PALETTE[idx] }}
                        />
                      )}
                      <span className="font-medium">{run.strategyId}</span>
                    </div>
                    <p className="text-xs text-muted-foreground mt-0.5 truncate">
                      {run.tickers.join(", ")} · {run.startDate} – {run.endDate}
                    </p>
                  </button>
                );
              })}
            </div>
          )}

          <div className="flex items-center gap-3">
            <Button
              onClick={handleCompare}
              disabled={selectedIds.length < 2 || mutation.isPending}
            >
              {mutation.isPending ? "Comparing…" : `Compare ${selectedIds.length} Run${selectedIds.length !== 1 ? "s" : ""}`}
            </Button>
            {selectedIds.length < 2 && (
              <p className="text-xs text-muted-foreground">Select at least 2 runs.</p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Side-by-side metrics */}
      {results.length >= 2 && (
        <>
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Metrics Comparison</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-muted-foreground text-xs">
                      <th className="text-left pb-2 font-medium w-36">Metric</th>
                      {results.map((r, i) => {
                        const run = completedRuns.find((ru) => ru.runId === r.runId);
                        return (
                          <th key={r.runId} className="text-right pb-2 font-medium">
                            <span
                              className="inline-block h-2 w-2 rounded-full mr-1.5"
                              style={{ backgroundColor: PALETTE[i] }}
                            />
                            {run ? `${run.strategyId} · ${run.tickers.join(",")}` : r.runId.slice(0, 8)}
                          </th>
                        );
                      })}
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {METRIC_ROWS.map(({ label, get, format, positiveIf }) => (
                      <tr key={label} className="hover:bg-muted/20">
                        <td className="py-2 text-muted-foreground text-xs font-medium">{label}</td>
                        {results.map((r) => {
                          const val = get(r.metrics);
                          const isPos = positiveIf === undefined ? null : positiveIf(val);
                          const colour =
                            isPos === true
                              ? "text-emerald-600"
                              : isPos === false
                              ? "text-red-500"
                              : "";
                          return (
                            <td key={r.runId} className={`py-2 text-right font-medium ${colour}`}>
                              {format(val)}
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>

          {/* Overlaid equity curves */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Equity Curves</CardTitle>
            </CardHeader>
            <CardContent>
              {mergedChart.length > 0 ? (
                <ResponsiveContainer width="100%" height={340}>
                  <LineChart data={mergedChart} margin={{ top: 4, right: 8, left: 0, bottom: 4 }}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                    <XAxis
                      dataKey="date"
                      tick={{ fontSize: 11 }}
                      tickFormatter={(v: string) => v.slice(0, 7)}
                      interval="preserveStartEnd"
                    />
                    <YAxis
                      tick={{ fontSize: 11 }}
                      domain={["auto", "auto"]}
                      tickFormatter={(v: number) => `$${(v / 1000).toFixed(0)}k`}
                      width={55}
                    />
                    <Tooltip
                      formatter={(v, name) => [formatCurrency(v as number), name]}
                      labelStyle={{ fontSize: 12 }}
                      contentStyle={{
                        backgroundColor: "hsl(var(--card))",
                        border: "1px solid hsl(var(--border))",
                        borderRadius: "6px",
                        fontSize: 12,
                      }}
                    />
                    <Legend />
                    {results.map((r, i) => {
                      const run = completedRuns.find((ru) => ru.runId === r.runId);
                      const label = run
                        ? `${run.strategyId} · ${run.tickers.join(",")}`
                        : r.runId.slice(0, 8);
                      return (
                        <Line
                          key={r.runId}
                          type="monotone"
                          dataKey={r.runId}
                          name={label}
                          stroke={PALETTE[i]}
                          strokeWidth={2}
                          dot={false}
                        />
                      );
                    })}
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <p className="text-sm text-muted-foreground">No equity curve data.</p>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}

/** Metric row descriptors for the comparison table. */
const METRIC_ROWS: {
  label: string;
  get: (m: BacktestResultResponse["metrics"]) => number;
  format: (v: number) => string;
  positiveIf?: (v: number) => boolean;
}[] = [
  { label: "Total Return", get: (m) => m.totalReturn, format: formatPct, positiveIf: (v) => v >= 0 },
  { label: "Annualised Return", get: (m) => m.annualizedReturn, format: formatPct, positiveIf: (v) => v >= 0 },
  { label: "Sharpe Ratio", get: (m) => m.sharpeRatio, format: formatNumber },
  { label: "Max Drawdown", get: (m) => m.maxDrawdown, format: formatPct, positiveIf: () => false },
  { label: "Volatility (ann.)", get: (m) => m.annualizedVolatility, format: formatPct },
  { label: "Win Rate", get: (m) => m.winRate, format: formatPct },
  { label: "Profit Factor", get: (m) => m.profitFactor, format: formatNumber },
  { label: "Total Trades", get: (m) => m.totalTrades, format: (v) => String(v) },
  { label: "Avg Win", get: (m) => m.avgWin, format: formatCurrency, positiveIf: () => true },
  { label: "Avg Loss", get: (m) => m.avgLoss, format: formatCurrency, positiveIf: () => false },
];

/**
 * Merges multiple equity curves into a single dataset keyed by date.
 * Each run's equity is stored under its runId field so Recharts can plot them
 * on separate lines.
 *
 * @param results Completed backtest results.
 * @param runs    All run DTOs (used for display labels).
 */
function buildMergedChart(
  results: BacktestResultResponse[],
  runs: BacktestRunDto[]
): Record<string, string | number>[] {
  if (results.length === 0) return [];

  // Collect all unique dates across all equity curves
  const allDates = new Set<string>();
  for (const r of results) {
    for (const p of r.equityCurve) allDates.add(p.date);
  }

  const sortedDates = [...allDates].sort();

  // Build one row per date
  return sortedDates.map((date) => {
    const row: Record<string, string | number> = { date };
    for (const r of results) {
      const point = r.equityCurve.find((p) => p.date === date);
      if (point !== undefined) row[r.runId] = Number(point.equity);
    }
    return row;
  });
}
