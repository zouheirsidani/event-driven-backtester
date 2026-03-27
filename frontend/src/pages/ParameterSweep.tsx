/**
 * Parameter Sweep page — run a strategy across every combination of parameter values
 * (cartesian product) and view results ranked by Sharpe ratio.
 *
 * The sweep is executed synchronously on the server, so the UI shows a spinner while
 * it runs.  For large sweeps (many combinations × long date ranges) this can take
 * tens of seconds.
 *
 * Parameter inputs are strategy-specific:
 *  - MOMENTUM_V1:      lookbackDays (one or more values)
 *  - MEAN_REVERSION_V1: windowSize and zScoreThreshold (one or more values each)
 */
import { useState } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { getStrategies, runSweep } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { formatPct, formatNumber, formatCurrency } from "@/lib/utils";
import type { SweepResultEntry, SweepResultResponse } from "@/lib/types";
import { Link } from "react-router-dom";

/** Known strategy parameter schemas — drives which fields are shown. */
const STRATEGY_PARAMS: Record<string, { key: string; label: string; default: string }[]> = {
  MOMENTUM_V1: [{ key: "lookbackDays", label: "Lookback Days", default: "10, 20, 30" }],
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

export default function ParameterSweep() {
  const [strategyId, setStrategyId] = useState("");
  const [tickers, setTickers] = useState("AAPL");
  const [startDate, setStartDate] = useState("2022-01-01");
  const [endDate, setEndDate] = useState("2023-12-31");
  const [initialCash, setInitialCash] = useState("100000");
  /** Raw comma-separated value strings keyed by parameter name. */
  const [paramInputs, setParamInputs] = useState<Record<string, string>>({});
  const [result, setResult] = useState<SweepResultResponse | null>(null);

  const { data: strategies } = useQuery({ queryKey: ["strategies"], queryFn: getStrategies });

  const mutation = useMutation({
    mutationFn: runSweep,
    onSuccess: (data) => setResult(data),
  });

  /** Parse a comma-separated string of numbers into a number array, ignoring blanks. */
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
      if (vals.length === 0) return; // guard: need at least one value
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
      parameters,
    });
  }

  const paramDefs = STRATEGY_PARAMS[strategyId] ?? [];
  const totalCombinations = paramDefs.reduce((acc, def) => {
    const vals = parseValues(paramInputs[def.key] ?? def.default);
    return acc * Math.max(1, vals.length);
  }, 1);

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Parameter Sweep</h1>
        <p className="text-muted-foreground mt-1">
          Run a strategy across all combinations of parameter values and rank by Sharpe ratio.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Sweep Configuration</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Strategy */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Strategy">
                <Select value={strategyId} onValueChange={(v) => { setStrategyId(v); setParamInputs({}); }}>
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
            </div>

            {/* Dynamic parameter range inputs */}
            {strategyId && paramDefs.length > 0 && (
              <div>
                <p className="text-sm font-medium mb-3">Parameter Ranges</p>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {paramDefs.map((def) => (
                    <Field key={def.key} label={`${def.label} (comma-separated values)`}>
                      <input
                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        value={paramInputs[def.key] ?? def.default}
                        onChange={(e) =>
                          setParamInputs((prev) => ({ ...prev, [def.key]: e.target.value }))
                        }
                        placeholder={def.default}
                      />
                    </Field>
                  ))}
                </div>
                <p className="text-xs text-muted-foreground mt-2">
                  {totalCombinations} combination{totalCombinations !== 1 ? "s" : ""} will be run.
                  Large sweeps may take a while.
                </p>
              </div>
            )}

            <Button type="submit" disabled={!strategyId || mutation.isPending}>
              {mutation.isPending ? (
                <>
                  <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent mr-2" />
                  Running sweep…
                </>
              ) : (
                "Run Sweep"
              )}
            </Button>

            {mutation.isError && (
              <p className="text-sm text-destructive">
                Sweep failed. Check the server logs for details.
              </p>
            )}
          </form>
        </CardContent>
      </Card>

      {/* Results */}
      {result && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">
              Results — {result.strategyId}{" "}
              <span className="text-muted-foreground font-normal text-sm">
                ({result.successfulRuns}/{result.totalCombinations} successful, ranked by Sharpe)
              </span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-muted-foreground text-xs">
                    <th className="text-left pb-2 font-medium">#</th>
                    {/* Dynamic parameter columns */}
                    {Object.keys(result.results[0]?.parameterValues ?? {}).map((k) => (
                      <th key={k} className="text-left pb-2 font-medium">{k}</th>
                    ))}
                    <th className="text-right pb-2 font-medium">Sharpe</th>
                    <th className="text-right pb-2 font-medium">Total Return</th>
                    <th className="text-right pb-2 font-medium">Max Drawdown</th>
                    <th className="text-right pb-2 font-medium">Run</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {result.results.map((entry, i) => (
                    <SweepRow key={entry.backtestRunId} rank={i + 1} entry={entry} />
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

/** A labelled form field wrapper. */
function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium">{label}</label>
      {children}
    </div>
  );
}

/** A single row in the sweep results table. */
function SweepRow({ rank, entry }: { rank: number; entry: SweepResultEntry }) {
  const failed = entry.sharpeRatio === null;
  const paramKeys = Object.keys(entry.parameterValues);

  return (
    <tr className={`hover:bg-muted/30 transition-colors ${failed ? "opacity-50" : ""}`}>
      <td className="py-2 text-muted-foreground">{rank}</td>
      {paramKeys.map((k) => (
        <td key={k} className="py-2 font-medium">
          {String(entry.parameterValues[k])}
        </td>
      ))}
      <td className="py-2 text-right font-medium">
        {entry.sharpeRatio !== null ? formatNumber(entry.sharpeRatio) : "—"}
      </td>
      <td className={`py-2 text-right font-medium ${entry.totalReturn !== null && entry.totalReturn >= 0 ? "text-emerald-600" : "text-red-500"}`}>
        {entry.totalReturn !== null ? formatPct(entry.totalReturn) : "—"}
      </td>
      <td className="py-2 text-right text-red-500">
        {entry.maxDrawdown !== null ? formatPct(entry.maxDrawdown) : "—"}
      </td>
      <td className="py-2 text-right">
        {failed ? (
          <span className="text-muted-foreground text-xs">failed</span>
        ) : (
          <Link
            to={`/results?runId=${entry.backtestRunId}`}
            className="text-xs underline underline-offset-2 text-muted-foreground hover:text-foreground"
          >
            view →
          </Link>
        )}
      </td>
    </tr>
  );
}
