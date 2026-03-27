/**
 * Results page — detailed performance analysis for a selected backtest run.
 *
 * The user picks a run from the dropdown (or is directed here via the `?runId=`
 * query parameter). Runs that are still PENDING or RUNNING are polled every 2 s
 * until they complete. Once COMPLETED the result is fetched once and displayed as:
 * - A run summary banner (strategy, tickers, period, capital, status).
 * - A 10-tile metrics grid (return, Sharpe, drawdown, win rate, trades, etc.).
 * - A Recharts equity-curve line chart with a dashed reference line at initial cash.
 * - A scrollable trades table listing every fill with date, side, qty, price, and
 *   commission.
 */
import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { getBacktests, getBacktestRun, getBacktestResult } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import StatusBadge from "@/components/StatusBadge";
import { formatPct, formatNumber, formatCurrency } from "@/lib/utils";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from "recharts";
import type { TradeDto } from "@/lib/types";

export default function Results() {
  const [searchParams] = useSearchParams();
  const [selectedRunId, setSelectedRunId] = useState(searchParams.get("runId") ?? "");

  const { data: runs } = useQuery({ queryKey: ["backtests"], queryFn: getBacktests, refetchInterval: 3000 });

  // Auto-select first completed run if none selected
  useEffect(() => {
    if (!selectedRunId && runs?.runs.length) {
      const first = runs.runs.find((r) => r.status === "COMPLETED");
      if (first) setSelectedRunId(first.runId);
    }
  }, [runs, selectedRunId]);

  const { data: run } = useQuery({
    queryKey: ["run", selectedRunId],
    queryFn: () => getBacktestRun(selectedRunId),
    enabled: !!selectedRunId,
    refetchInterval: (query) =>
      query.state.data?.status === "RUNNING" || query.state.data?.status === "PENDING" ? 2000 : false,
  });

  const { data: result } = useQuery({
    queryKey: ["result", selectedRunId],
    queryFn: () => getBacktestResult(selectedRunId),
    enabled: run?.status === "COMPLETED",
  });

  const m = result?.metrics;

  const chartData = result?.equityCurve.map((p) => ({
    date: p.date,
    equity: Number(p.equity),
  }));

  const initialCash = run?.initialCash ?? 0;

  return (
    <div className="space-y-8">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Results</h1>
          <p className="text-muted-foreground mt-1">Detailed performance analysis</p>
        </div>

        {/* Run selector */}
        <div className="w-72">
          <Select value={selectedRunId} onValueChange={setSelectedRunId}>
            <SelectTrigger>
              <SelectValue placeholder="Select a backtest run…" />
            </SelectTrigger>
            <SelectContent>
              {runs?.runs.map((r) => (
                <SelectItem key={r.runId} value={r.runId}>
                  {r.strategyId} · {r.tickers.join(",")} · {r.startDate}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {!selectedRunId ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            Select a backtest run above to view results.
          </CardContent>
        </Card>
      ) : (
        <>
          {/* Run summary bar */}
          {run && (
            <div className="flex flex-wrap items-center gap-4 rounded-lg border px-4 py-3 text-sm">
              <div>
                <span className="text-muted-foreground">Strategy </span>
                <span className="font-medium">{run.strategyId}</span>
              </div>
              <div>
                <span className="text-muted-foreground">Tickers </span>
                <span className="font-medium">{run.tickers.join(", ")}</span>
              </div>
              <div>
                <span className="text-muted-foreground">Period </span>
                <span className="font-medium">{run.startDate} – {run.endDate}</span>
              </div>
              <div>
                <span className="text-muted-foreground">Capital </span>
                <span className="font-medium">{formatCurrency(run.initialCash)}</span>
              </div>
              <StatusBadge status={run.status} />
            </div>
          )}

          {run?.status === "RUNNING" || run?.status === "PENDING" ? (
            <Card>
              <CardContent className="py-12 text-center text-muted-foreground">
                <div className="inline-block h-5 w-5 animate-spin rounded-full border-2 border-primary border-t-transparent mr-3" />
                Backtest in progress…
              </CardContent>
            </Card>
          ) : run?.status === "FAILED" ? (
            <Card>
              <CardContent className="py-12 text-center text-destructive">
                Backtest failed. Check server logs for details.
              </CardContent>
            </Card>
          ) : result ? (
            <>
              {/* Metrics grid */}
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
                <MetricCard
                  label="Total Return"
                  value={formatPct(m?.totalReturn)}
                  sub={`from ${formatCurrency(initialCash)}`}
                  positive={m ? m.totalReturn >= 0 : null}
                />
                <MetricCard label="Annualised Return" value={formatPct(m?.annualizedReturn)} />
                <MetricCard label="Sharpe Ratio" value={formatNumber(m?.sharpeRatio)} sub="rf = 0" />
                <MetricCard
                  label="Max Drawdown"
                  value={formatPct(m?.maxDrawdown)}
                  positive={false}
                />
                <MetricCard label="Volatility" value={formatPct(m?.annualizedVolatility)} sub="annualised" />
                <MetricCard label="Win Rate" value={formatPct(m?.winRate)} />
                <MetricCard label="Avg Win" value={formatCurrency(m?.avgWin)} positive />
                <MetricCard label="Avg Loss" value={formatCurrency(m?.avgLoss)} positive={false} />
                <MetricCard label="Profit Factor" value={formatNumber(m?.profitFactor)} />
                <MetricCard label="Total Trades" value={String(m?.totalTrades ?? "—")} />
              </div>

              {/* Equity curve */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Equity Curve</CardTitle>
                </CardHeader>
                <CardContent>
                  {chartData && chartData.length > 0 ? (() => {
                    // Compute where the initial-cash line sits in the Y domain (0 = top, 1 = bottom in SVG).
                    // This drives the gradient split between the profit zone (green) and loss zone (red).
                    const equities = chartData.map((d) => d.equity);
                    const minE = Math.min(...equities);
                    const maxE = Math.max(...equities);
                    const range = maxE - minE;
                    // Fraction from the TOP of the chart where initialCash falls
                    const splitPct = range > 0
                      ? Math.max(0, Math.min(100, ((maxE - initialCash) / range) * 100))
                      : 0;
                    const splitStr = `${splitPct.toFixed(1)}%`;

                    return (
                      <ResponsiveContainer width="100%" height={320}>
                        <AreaChart data={chartData} margin={{ top: 4, right: 8, left: 0, bottom: 4 }}>
                          <defs>
                            <linearGradient id="equityGradient" x1="0" y1="0" x2="0" y2="1">
                              {/* Profit zone — green */}
                              <stop offset={splitStr} stopColor="#22c55e" stopOpacity={0.25} />
                              {/* Loss zone — red */}
                              <stop offset={splitStr} stopColor="#ef4444" stopOpacity={0.25} />
                              <stop offset="100%" stopColor="#ef4444" stopOpacity={0.05} />
                            </linearGradient>
                            <linearGradient id="lineGradient" x1="0" y1="0" x2="0" y2="1">
                              <stop offset={splitStr} stopColor="#22c55e" stopOpacity={1} />
                              <stop offset={splitStr} stopColor="#ef4444" stopOpacity={1} />
                            </linearGradient>
                          </defs>
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
                            formatter={(v) => [formatCurrency(v as number), "Portfolio Value"]}
                            labelStyle={{ fontSize: 12 }}
                            contentStyle={{
                              backgroundColor: "hsl(var(--popover))",
                              border: "1px solid hsl(var(--border))",
                              borderRadius: "6px",
                              fontSize: 12,
                            }}
                          />
                          <ReferenceLine
                            y={initialCash}
                            stroke="hsl(var(--muted-foreground))"
                            strokeDasharray="4 4"
                            label={{ value: "Initial", fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
                          />
                          <Area
                            type="monotone"
                            dataKey="equity"
                            stroke="url(#lineGradient)"
                            strokeWidth={2}
                            fill="url(#equityGradient)"
                            dot={false}
                          />
                        </AreaChart>
                      </ResponsiveContainer>
                    );
                  })() : (
                    <p className="text-muted-foreground text-sm">No equity curve data.</p>
                  )}
                </CardContent>
              </Card>

              {/* Trades table */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">
                    Trades{" "}
                    <span className="text-muted-foreground font-normal text-sm">
                      ({result.trades.length})
                    </span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {result.trades.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No trades executed.</p>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="border-b text-muted-foreground text-xs">
                            <th className="text-left pb-2 font-medium">Date</th>
                            <th className="text-left pb-2 font-medium">Ticker</th>
                            <th className="text-left pb-2 font-medium">Side</th>
                            <th className="text-right pb-2 font-medium">Qty</th>
                            <th className="text-right pb-2 font-medium">Price</th>
                            <th className="text-right pb-2 font-medium">Commission</th>
                            <th className="text-right pb-2 font-medium">Value</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y">
                          {result.trades.map((t) => (
                            <TradeRow key={t.fillId} trade={t} />
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </CardContent>
              </Card>
            </>
          ) : null}
        </>
      )}
    </div>
  );
}

/**
 * A single metric tile in the performance grid.
 *
 * @param label    Short metric name displayed above the value.
 * @param value    Pre-formatted value string (percentage, currency, or plain number).
 * @param sub      Optional small subtitle rendered below the value (e.g. "rf = 0").
 * @param positive Controls colour coding: true → emerald, false → red, null/undefined → neutral.
 */
function MetricCard({
  label,
  value,
  sub,
  positive,
}: {
  label: string;
  value: string;
  sub?: string;
  positive?: boolean | null;
}) {
  const colour =
    positive === true
      ? "text-emerald-600"
      : positive === false
        ? "text-red-500"
        : "text-foreground";
  return (
    <Card>
      <CardContent className="pt-5 pb-4">
        <p className="text-xs text-muted-foreground mb-1">{label}</p>
        <p className={`text-xl font-bold ${colour}`}>{value}</p>
        {sub && <p className="text-xs text-muted-foreground mt-0.5">{sub}</p>}
      </CardContent>
    </Card>
  );
}

/**
 * A single row in the trades table.
 * BUY fills are coloured emerald; SELL fills are coloured red for quick scanning.
 * The "Value" column is the gross fill value (price × quantity), before commission.
 *
 * @param trade The fill record to display.
 */
function TradeRow({ trade }: { trade: TradeDto }) {
  const isBuy = trade.side === "BUY";
  return (
    <tr className="hover:bg-muted/30 transition-colors">
      <td className="py-2 text-muted-foreground">{trade.date}</td>
      <td className="py-2 font-medium">{trade.ticker}</td>
      <td className={`py-2 font-medium ${isBuy ? "text-emerald-600" : "text-red-500"}`}>
        {trade.side}
      </td>
      <td className="py-2 text-right">{trade.quantity.toLocaleString()}</td>
      <td className="py-2 text-right">{formatCurrency(trade.price)}</td>
      <td className="py-2 text-right text-muted-foreground">{formatCurrency(trade.commission)}</td>
      <td className="py-2 text-right font-medium">
        {formatCurrency(trade.price * trade.quantity)}
      </td>
    </tr>
  );
}
