/**
 * Dashboard page — the default landing page of the application.
 * Displays a high-level summary of the platform state:
 * - Counts of registered symbols, available strategies, and backtest runs.
 * - Key performance metrics for the most recently completed backtest.
 * - A table of the most recent backtest runs with live status polling every 3 seconds.
 */
import { useQuery } from "@tanstack/react-query";
import { getBacktests, getBacktestResult, getSymbols, getStrategies } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import StatusBadge from "@/components/StatusBadge";
import { formatPct, formatNumber, formatDate } from "@/lib/utils";
import { Activity, Database, FlaskConical, TrendingUp } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export default function Dashboard() {
  const { data: symbols } = useQuery({ queryKey: ["symbols"], queryFn: getSymbols });
  const { data: strategies } = useQuery({ queryKey: ["strategies"], queryFn: getStrategies });
  const { data: runs } = useQuery({
    queryKey: ["backtests"],
    queryFn: getBacktests,
    refetchInterval: 3000,
  });

  const latestCompleted = runs?.runs.find((r) => r.status === "COMPLETED");

  const { data: latestResult } = useQuery({
    queryKey: ["result", latestCompleted?.runId],
    queryFn: () => getBacktestResult(latestCompleted!.runId),
    enabled: !!latestCompleted,
  });

  const m = latestResult?.metrics;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground mt-1">
          Event-driven backtesting platform overview
        </p>
      </div>

      {/* Summary stats */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard
          icon={<Database className="h-4 w-4" />}
          label="Symbols"
          value={String(symbols?.count ?? "—")}
        />
        <StatCard
          icon={<FlaskConical className="h-4 w-4" />}
          label="Strategies"
          value={String(strategies?.strategies.length ?? "—")}
        />
        <StatCard
          icon={<Activity className="h-4 w-4" />}
          label="Backtest Runs"
          value={String(runs?.totalCount ?? "—")}
        />
        <StatCard
          icon={<TrendingUp className="h-4 w-4" />}
          label="Completed"
          value={String(runs?.runs.filter((r) => r.status === "COMPLETED").length ?? "—")}
        />
      </div>

      {/* Latest result */}
      {latestResult && latestCompleted && (
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Latest Backtest Result</CardTitle>
              <Link to={`/results?runId=${latestCompleted.runId}`}>
                <Button variant="outline" size="sm">View full results →</Button>
              </Link>
            </div>
            <p className="text-sm text-muted-foreground">
              {latestCompleted.strategyId} · {latestCompleted.tickers.join(", ")} ·{" "}
              {formatDate(latestCompleted.startDate)} – {formatDate(latestCompleted.endDate)}
            </p>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
              <MetricTile
                label="Total Return"
                value={formatPct(m?.totalReturn)}
                positive={m ? m.totalReturn >= 0 : null}
              />
              <MetricTile label="Sharpe Ratio" value={formatNumber(m?.sharpeRatio)} />
              <MetricTile
                label="Max Drawdown"
                value={formatPct(m?.maxDrawdown)}
                positive={false}
              />
              <MetricTile label="Win Rate" value={formatPct(m?.winRate)} />
              <MetricTile label="Total Trades" value={String(m?.totalTrades ?? "—")} />
            </div>
          </CardContent>
        </Card>
      )}

      {/* Recent runs */}
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Recent Runs</CardTitle>
            <Link to="/backtest">
              <Button variant="outline" size="sm">New backtest →</Button>
            </Link>
          </div>
        </CardHeader>
        <CardContent>
          {!runs || runs.runs.length === 0 ? (
            <p className="text-sm text-muted-foreground py-4 text-center">
              No backtests yet.{" "}
              <Link to="/backtest" className="underline underline-offset-2">
                Run your first one.
              </Link>
            </p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-muted-foreground">
                  <th className="text-left pb-2 font-medium">Strategy</th>
                  <th className="text-left pb-2 font-medium">Tickers</th>
                  <th className="text-left pb-2 font-medium">Period</th>
                  <th className="text-left pb-2 font-medium">Status</th>
                  <th className="text-left pb-2 font-medium">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {runs.runs.slice(0, 8).map((run) => (
                  <tr key={run.runId} className="hover:bg-muted/30 transition-colors">
                    <td className="py-2.5 font-medium">{run.strategyId}</td>
                    <td className="py-2.5 text-muted-foreground">{run.tickers.join(", ")}</td>
                    <td className="py-2.5 text-muted-foreground">
                      {run.startDate} – {run.endDate}
                    </td>
                    <td className="py-2.5">
                      <StatusBadge status={run.status} />
                    </td>
                    <td className="py-2.5 text-muted-foreground">
                      {formatDate(run.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

/**
 * A small summary card showing a labelled numeric stat with an icon.
 *
 * @param icon  Icon element to display alongside the label.
 * @param label Short descriptive label (e.g. "Symbols").
 * @param value Value to display prominently (e.g. "12").
 */
function StatCard({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-center gap-2 text-muted-foreground mb-1">
          {icon}
          <span className="text-xs font-medium uppercase tracking-wide">{label}</span>
        </div>
        <p className="text-2xl font-bold">{value}</p>
      </CardContent>
    </Card>
  );
}

/**
 * A compact metric tile used in the latest-result summary section.
 *
 * @param label    Metric name (e.g. "Total Return").
 * @param value    Pre-formatted value string (e.g. "15.23%").
 * @param positive Controls colour coding: true → green, false → red, null/undefined → default.
 */
function MetricTile({
  label,
  value,
  positive,
}: {
  label: string;
  value: string;
  positive?: boolean | null;
}) {
  // Green for positive metrics, red for negative, neutral otherwise
  const colour =
    positive === true
      ? "text-emerald-600"
      : positive === false
        ? "text-red-500"
        : "text-foreground";
  return (
    <div className="rounded-md bg-muted/40 px-4 py-3">
      <p className="text-xs text-muted-foreground mb-1">{label}</p>
      <p className={`text-lg font-semibold ${colour}`}>{value}</p>
    </div>
  );
}
