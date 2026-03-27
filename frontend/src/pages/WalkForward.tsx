/**
 * Walk-Forward Optimisation page.
 *
 * The user selects a strategy, tickers, date range, parameter value lists,
 * and rolling window sizes (trainMonths + testMonths).  On submit, the server
 * runs a parameter sweep on each training window, picks the best parameters
 * by Sharpe, and runs those parameters on the subsequent out-of-sample test
 * window.
 *
 * Results are displayed as:
 *  - Aggregate out-of-sample stats (avg Sharpe, avg return).
 *  - A table of windows showing the best training params and test-period performance.
 *  - Links to the individual test-period backtest run on the Results page.
 */
import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { getStrategies, runWalkForward } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { formatPct, formatNumber } from "@/lib/utils";
import type { WalkForwardResponse, WalkForwardWindowResult } from "@/lib/types";
import { Link } from "react-router-dom";

/** Strategy-specific sweepable parameters — mirrors ParameterSweep.tsx. */
const STRATEGY_PARAMS: Record<string, { key: string; label: string; default: string }[]> = {
  MOMENTUM_V1: [
    { key: "lookbackDays", label: "Lookback Days", default: "10, 20, 30" },
  ],
  MEAN_REVERSION_V1: [
    { key: "windowSize", label: "Window Size", default: "10, 20, 30" },
    { key: "zScoreThreshold", label: "Z-Score Threshold", default: "-1.5, -2.0, -2.5" },
  ],
  RSI_V1: [
    { key: "period", label: "RSI Period", default: "7, 14, 21" },
    { key: "oversoldThreshold", label: "Oversold Threshold", default: "25, 30, 35" },
    { key: "overboughtThreshold", label: "Overbought Threshold", default: "65, 70, 75" },
  ],
  MA_CROSSOVER_V1: [
    { key: "shortWindow", label: "Short Window", default: "20, 50" },
    { key: "longWindow", label: "Long Window", default: "100, 200" },
  ],
};

export default function WalkForward() {
  const [strategyId, setStrategyId] = useState("");
  const [tickers, setTickers] = useState("AAPL");
  const [startDate, setStartDate] = useState("2018-01-01");
  const [endDate, setEndDate] = useState("2023-12-31");
  const [initialCash, setInitialCash] = useState("100000");
  const [trainMonths, setTrainMonths] = useState("12");
  const [testMonths, setTestMonths] = useState("3");
  const [paramInputs, setParamInputs] = useState<Record<string, string>>({});
  const [result, setResult] = useState<WalkForwardResponse | null>(null);

  const { data: strategies } = useQuery({ queryKey: ["strategies"], queryFn: getStrategies });

  const mutation = useMutation({
    mutationFn: runWalkForward,
    onSuccess: (data) => setResult(data),
  });

  function parseValues(raw: string): number[] {
    return raw
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean)
      .map(Number)
      .filter((n) => !isNaN(n));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!strategyId) return;

    const paramDefs = STRATEGY_PARAMS[strategyId] ?? [];
    const parameters: Record<string, number[]> = {};
    for (const def of paramDefs) {
      const vals = parseValues(paramInputs[def.key] ?? def.default);
      if (vals.length === 0) return;
      parameters[def.key] = vals;
    }

    const tickerList = tickers
      .split(",")
      .map((t) => t.trim().toUpperCase())
      .filter(Boolean);

    mutation.mutate({
      strategyId,
      tickers: tickerList,
      startDate,
      endDate,
      initialCash: Number(initialCash),
      trainMonths: Number(trainMonths),
      testMonths: Number(testMonths),
      parameters,
    });
  }

  const paramDefs = STRATEGY_PARAMS[strategyId] ?? [];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Walk-Forward Optimisation</h1>
        <p className="text-muted-foreground mt-1">
          Optimise parameters on rolling training windows and validate out-of-sample on test windows.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Configuration</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <Field label="Strategy">
                <Select
                  value={strategyId}
                  onValueChange={(v) => { setStrategyId(v); setParamInputs({}); }}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select strategy…" />
                  </SelectTrigger>
                  <SelectContent>
                    {strategies?.strategies.map((s) => (
                      <SelectItem key={s.strategyId} value={s.strategyId}>
                        {s.displayName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>

              <Field label="Tickers (comma-separated)">
                <input
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={tickers}
                  onChange={(e) => setTickers(e.target.value)}
                  placeholder="AAPL, MSFT"
                />
              </Field>

              <Field label="Start Date">
                <input
                  type="date"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </Field>

              <Field label="End Date">
                <input
                  type="date"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </Field>

              <Field label="Initial Capital ($)">
                <input
                  type="number"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={initialCash}
                  onChange={(e) => setInitialCash(e.target.value)}
                  min={1}
                />
              </Field>

              <Field label="Training Window (months)">
                <input
                  type="number"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={trainMonths}
                  onChange={(e) => setTrainMonths(e.target.value)}
                  min={1}
                />
              </Field>

              <Field label="Test Window (months)">
                <input
                  type="number"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={testMonths}
                  onChange={(e) => setTestMonths(e.target.value)}
                  min={1}
                />
              </Field>
            </div>

            {/* Dynamic parameter range inputs */}
            {strategyId && paramDefs.length > 0 && (
              <div>
                <p className="text-sm font-medium mb-3">Parameter Ranges to Sweep</p>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {paramDefs.map((def) => (
                    <Field key={def.key} label={`${def.label} (comma-separated)`}>
                      <input
                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        value={paramInputs[def.key] ?? def.default}
                        onChange={(e) =>
                          setParamInputs((prev) => ({ ...prev, [def.key]: e.target.value }))
                        }
                      />
                    </Field>
                  ))}
                </div>
              </div>
            )}

            <div className="flex items-center gap-4">
              <Button type="submit" disabled={!strategyId || mutation.isPending}>
                {mutation.isPending ? (
                  <>
                    <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent mr-2" />
                    Running…
                  </>
                ) : (
                  "Run Walk-Forward"
                )}
              </Button>
              {mutation.isPending && (
                <p className="text-xs text-muted-foreground">
                  This may take a while — each window runs a full parameter sweep.
                </p>
              )}
            </div>

            {mutation.isError && (
              <p className="text-sm text-destructive">
                Walk-forward failed. Check server logs for details.
              </p>
            )}
          </form>
        </CardContent>
      </Card>

      {/* Aggregate stats */}
      {result && (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <StatTile label="Windows" value={String(result.totalWindows)} />
            <StatTile label="Successful" value={String(result.successfulWindows)} />
            <StatTile
              label="Avg OOS Sharpe"
              value={result.avgOutOfSampleSharpe !== null ? formatNumber(result.avgOutOfSampleSharpe) : "—"}
              positive={result.avgOutOfSampleSharpe !== null ? result.avgOutOfSampleSharpe >= 0 : null}
            />
            <StatTile
              label="Avg OOS Return"
              value={result.avgOutOfSampleReturn !== null ? formatPct(result.avgOutOfSampleReturn) : "—"}
              positive={result.avgOutOfSampleReturn !== null ? result.avgOutOfSampleReturn >= 0 : null}
            />
          </div>

          {/* Window table */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">
                Window Results{" "}
                <span className="font-normal text-muted-foreground text-sm">
                  ({result.strategyId} · {result.totalWindows} windows)
                </span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-muted-foreground text-xs">
                      <th className="text-left pb-2 font-medium">#</th>
                      <th className="text-left pb-2 font-medium">Train Period</th>
                      <th className="text-left pb-2 font-medium">Test Period</th>
                      <th className="text-left pb-2 font-medium">Best Params</th>
                      <th className="text-right pb-2 font-medium">Train Sharpe</th>
                      <th className="text-right pb-2 font-medium">OOS Sharpe</th>
                      <th className="text-right pb-2 font-medium">OOS Return</th>
                      <th className="text-right pb-2 font-medium">OOS Drawdown</th>
                      <th className="text-right pb-2 font-medium">Run</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {result.windows.map((w, i) => (
                      <WindowRow key={i} idx={i + 1} window={w} />
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium">{label}</label>
      {children}
    </div>
  );
}

function StatTile({
  label,
  value,
  positive,
}: {
  label: string;
  value: string;
  positive?: boolean | null;
}) {
  const colour =
    positive === true ? "text-emerald-600" : positive === false ? "text-red-500" : "text-foreground";
  return (
    <Card>
      <CardContent className="pt-5 pb-4">
        <p className="text-xs text-muted-foreground mb-1">{label}</p>
        <p className={`text-xl font-bold ${colour}`}>{value}</p>
      </CardContent>
    </Card>
  );
}

function WindowRow({ idx, window: w }: { idx: number; window: WalkForwardWindowResult }) {
  const failed = w.testSharpe === null;
  const paramStr = w.bestParams
    ? Object.entries(w.bestParams)
        .map(([k, v]) => `${k}=${v}`)
        .join(", ")
    : "—";

  return (
    <tr className={`hover:bg-muted/30 transition-colors ${failed ? "opacity-50" : ""}`}>
      <td className="py-2 text-muted-foreground">{idx}</td>
      <td className="py-2 text-xs">{w.trainStart} – {w.trainEnd}</td>
      <td className="py-2 text-xs">{w.testStart} – {w.testEnd}</td>
      <td className="py-2 text-xs font-mono text-muted-foreground">{paramStr}</td>
      <td className="py-2 text-right">
        {w.trainSharpe !== null ? formatNumber(w.trainSharpe) : "—"}
      </td>
      <td className={`py-2 text-right font-medium ${w.testSharpe !== null && w.testSharpe >= 0 ? "text-emerald-600" : "text-red-500"}`}>
        {w.testSharpe !== null ? formatNumber(w.testSharpe) : "—"}
      </td>
      <td className={`py-2 text-right font-medium ${w.testReturn !== null && w.testReturn >= 0 ? "text-emerald-600" : "text-red-500"}`}>
        {w.testReturn !== null ? formatPct(w.testReturn) : "—"}
      </td>
      <td className="py-2 text-right text-red-500">
        {w.testMaxDrawdown !== null ? formatPct(w.testMaxDrawdown) : "—"}
      </td>
      <td className="py-2 text-right">
        {w.testRunId ? (
          <Link
            to={`/results?runId=${w.testRunId}`}
            className="text-xs underline underline-offset-2 text-muted-foreground hover:text-foreground"
          >
            view →
          </Link>
        ) : (
          <span className="text-xs text-muted-foreground">failed</span>
        )}
      </td>
    </tr>
  );
}
