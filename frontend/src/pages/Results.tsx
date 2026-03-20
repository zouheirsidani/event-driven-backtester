import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { getBacktests, getBacktestRun, getBacktestResult } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import StatusBadge from "@/components/StatusBadge";
import { formatPct, formatNumber, formatCurrency } from "@/lib/utils";
import {
  LineChart,
  Line,
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
                  {chartData && chartData.length > 0 ? (
                    <ResponsiveContainer width="100%" height={320}>
                      <LineChart data={chartData} margin={{ top: 4, right: 8, left: 0, bottom: 4 }}>
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
                        />
                        <ReferenceLine
                          y={initialCash}
                          stroke="hsl(var(--muted-foreground))"
                          strokeDasharray="4 4"
                          label={{ value: "Initial", fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
                        />
                        <Line
                          type="monotone"
                          dataKey="equity"
                          stroke="hsl(var(--primary))"
                          dot={false}
                          strokeWidth={2}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  ) : (
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
