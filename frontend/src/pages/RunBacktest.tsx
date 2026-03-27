/**
 * Run Backtest page — form for configuring and submitting a new backtest run.
 *
 * The user selects a strategy mode ("Base Strategy" or "Saved Template"), one or more
 * registered tickers, a date range, starting capital, and optionally slippage/commission
 * parameters.  On submit the form posts to the backend and shows a confirmation card with
 * options to view results or submit another backtest.
 *
 * A "Save as Template" button lets users persist the current strategy selection with a
 * custom name for reuse.
 */
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getStrategies, getSymbols, submitBacktest, getUserStrategies, createUserStrategy } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import StatusBadge from "@/components/StatusBadge";
import { useNavigate } from "react-router-dom";
import type { BacktestRunDto } from "@/lib/types";
import { formatDate } from "@/lib/utils";

export default function RunBacktest() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: strategiesData } = useQuery({ queryKey: ["strategies"], queryFn: getStrategies });
  const { data: symbolsData } = useQuery({ queryKey: ["symbols"], queryFn: getSymbols });
  const { data: userStrategiesData } = useQuery({ queryKey: ["userStrategies"], queryFn: getUserStrategies });

  /** "base" = use a registered strategy directly; "template" = use a saved user template */
  const [strategyMode, setStrategyMode] = useState<"base" | "template">("base");
  const [strategyId, setStrategyId] = useState("");
  const [userStrategyId, setUserStrategyId] = useState("");
  const [selectedTickers, setSelectedTickers] = useState<string[]>([]);
  const [startDate, setStartDate] = useState("2021-01-01");
  const [endDate, setEndDate] = useState("2023-12-31");
  const [initialCash, setInitialCash] = useState("100000");
  const [slippageType, setSlippageType] = useState("FIXED");
  const [slippageAmount, setSlippageAmount] = useState("0.01");
  const [commissionType, setCommissionType] = useState("FIXED");
  const [commissionAmount, setCommissionAmount] = useState("1.00");

  /** State for the inline "Save as Template" form */
  const [showSaveTemplate, setShowSaveTemplate] = useState(false);
  const [templateName, setTemplateName] = useState("");

  const [submitted, setSubmitted] = useState<BacktestRunDto | null>(null);

  const mutation = useMutation({
    mutationFn: submitBacktest,
    onSuccess: (run) => {
      setSubmitted(run);
    },
  });

  const saveTemplateMutation = useMutation({
    mutationFn: createUserStrategy,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["userStrategies"] });
      setShowSaveTemplate(false);
      setTemplateName("");
    },
  });

  /**
   * Toggles a ticker's inclusion in the selectedTickers list.
   * Clicking an active ticker removes it; clicking an inactive one adds it.
   *
   * @param t Ticker symbol to toggle.
   */
  function toggleTicker(t: string) {
    setSelectedTickers((prev) =>
      prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]
    );
  }

  /**
   * Reads the current form state and submits the backtest request via the mutation.
   * Numeric string inputs (initialCash, slippageAmount, commissionAmount) are coerced
   * to numbers before being sent in the request body.
   * Sends either strategyId (base mode) or userStrategyId (template mode) — not both.
   */
  function handleSubmit() {
    mutation.mutate({
      ...(strategyMode === "base" ? { strategyId } : { userStrategyId }),
      tickers: selectedTickers,
      startDate,
      endDate,
      initialCash: Number(initialCash),
      slippageType,
      slippageAmount: Number(slippageAmount),
      commissionType,
      commissionAmount: Number(commissionAmount),
    });
  }

  /**
   * Saves the currently selected base strategy as a named user template.
   */
  function handleSaveTemplate() {
    if (!templateName.trim() || !strategyId) return;
    saveTemplateMutation.mutate({ name: templateName.trim(), baseStrategyId: strategyId });
  }

  /** Whether the submit button should be disabled based on current form state. */
  function isSubmitDisabled(): boolean {
    if (selectedTickers.length === 0 || !startDate || !endDate || mutation.isPending) return true;
    if (strategyMode === "base") return !strategyId;
    return !userStrategyId;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Run Backtest</h1>
        <p className="text-muted-foreground mt-1">
          Configure and submit a new backtest — runs asynchronously in the background
        </p>
      </div>

      {submitted ? (
        <SubmittedCard run={submitted} onViewResults={() => navigate(`/results?runId=${submitted.runId}`)} onNew={() => { setSubmitted(null); mutation.reset(); }} />
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          {/* Left — main config */}
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Strategy & Symbols</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* Strategy mode toggle */}
                <div className="space-y-1">
                  <Label>Strategy Mode</Label>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setStrategyMode("base")}
                      className={`flex-1 px-3 py-1.5 rounded-md text-sm border transition-colors ${
                        strategyMode === "base"
                          ? "bg-primary text-primary-foreground border-primary"
                          : "border-border hover:bg-muted"
                      }`}
                    >
                      Base Strategy
                    </button>
                    <button
                      onClick={() => setStrategyMode("template")}
                      className={`flex-1 px-3 py-1.5 rounded-md text-sm border transition-colors ${
                        strategyMode === "template"
                          ? "bg-primary text-primary-foreground border-primary"
                          : "border-border hover:bg-muted"
                      }`}
                    >
                      Saved Template
                    </button>
                  </div>
                </div>

                {strategyMode === "base" ? (
                  <div className="space-y-1">
                    <Label>Strategy</Label>
                    {!strategiesData?.strategies.length ? (
                      <p className="text-sm text-muted-foreground">No strategies available.</p>
                    ) : (
                      <Select value={strategyId} onValueChange={setStrategyId}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select a strategy…" />
                        </SelectTrigger>
                        <SelectContent>
                          {strategiesData.strategies.map((s) => (
                            <SelectItem key={s.strategyId} value={s.strategyId}>
                              {s.displayName}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                    {/* Save as Template inline form */}
                    {strategyId && (
                      <div className="pt-1">
                        {!showSaveTemplate ? (
                          <button
                            className="text-xs text-muted-foreground underline underline-offset-2 hover:text-foreground"
                            onClick={() => setShowSaveTemplate(true)}
                          >
                            Save as Template
                          </button>
                        ) : (
                          <div className="flex gap-2 items-center mt-1">
                            <Input
                              className="h-7 text-xs"
                              placeholder="Template name…"
                              value={templateName}
                              onChange={(e) => setTemplateName(e.target.value)}
                            />
                            <Button
                              size="sm"
                              className="h-7 text-xs px-2"
                              disabled={!templateName.trim() || saveTemplateMutation.isPending}
                              onClick={handleSaveTemplate}
                            >
                              Save
                            </Button>
                            <button
                              className="text-xs text-muted-foreground hover:text-foreground"
                              onClick={() => { setShowSaveTemplate(false); setTemplateName(""); }}
                            >
                              Cancel
                            </button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="space-y-1">
                    <Label>Saved Template</Label>
                    {!userStrategiesData?.templates.length ? (
                      <p className="text-sm text-muted-foreground">
                        No templates saved yet. Use "Base Strategy" mode and click "Save as Template".
                      </p>
                    ) : (
                      <Select value={userStrategyId} onValueChange={setUserStrategyId}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select a template…" />
                        </SelectTrigger>
                        <SelectContent>
                          {userStrategiesData.templates.map((t) => (
                            <SelectItem key={t.id} value={t.id}>
                              {t.name} ({t.baseStrategyId})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  </div>
                )}

                <div className="space-y-2">
                  <Label>Tickers (select one or more)</Label>
                  {!symbolsData?.symbols.length ? (
                    <p className="text-sm text-muted-foreground">
                      No symbols registered. Go to Market Data first.
                    </p>
                  ) : (
                    <div className="flex flex-wrap gap-2">
                      {symbolsData.symbols.map((s) => {
                        const active = selectedTickers.includes(s.ticker);
                        return (
                          <button
                            key={s.ticker}
                            onClick={() => toggleTicker(s.ticker)}
                            className={`px-3 py-1 rounded-full text-sm border transition-colors ${
                              active
                                ? "bg-primary text-primary-foreground border-primary"
                                : "border-border hover:bg-muted"
                            }`}
                          >
                            {s.ticker}
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Date Range & Capital</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Start Date</Label>
                    <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                  </div>
                  <div className="space-y-1">
                    <Label>End Date</Label>
                    <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                  </div>
                </div>
                <div className="space-y-1">
                  <Label>Initial Cash (USD)</Label>
                  <Input
                    type="number"
                    min={1000}
                    value={initialCash}
                    onChange={(e) => setInitialCash(e.target.value)}
                  />
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Right — execution params */}
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Slippage</CardTitle>
                <CardDescription>Applied to fill price on each order</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="space-y-1">
                  <Label>Type</Label>
                  <Select value={slippageType} onValueChange={setSlippageType}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="FIXED">Fixed ($)</SelectItem>
                      <SelectItem value="PERCENT">Percent (%)</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label>Amount</Label>
                  <Input type="number" step="0.001" min={0} value={slippageAmount} onChange={(e) => setSlippageAmount(e.target.value)} />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Commission</CardTitle>
                <CardDescription>Deducted from proceeds on each fill</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="space-y-1">
                  <Label>Type</Label>
                  <Select value={commissionType} onValueChange={setCommissionType}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="FIXED">Fixed ($)</SelectItem>
                      <SelectItem value="PER_SHARE">Per Share ($)</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label>Amount</Label>
                  <Input type="number" step="0.01" min={0} value={commissionAmount} onChange={(e) => setCommissionAmount(e.target.value)} />
                </div>
              </CardContent>
            </Card>

            <Separator />

            {mutation.isError && (
              <p className="text-sm text-destructive">{(mutation.error as Error).message}</p>
            )}

            <Button
              className="w-full"
              size="lg"
              onClick={handleSubmit}
              disabled={isSubmitDisabled()}
            >
              {mutation.isPending ? "Submitting…" : "Submit Backtest"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function SubmittedCard({
  run,
  onViewResults,
  onNew,
}: {
  run: BacktestRunDto;
  onViewResults: () => void;
  onNew: () => void;
}) {
  return (
    <Card className="max-w-lg">
      <CardHeader>
        <CardTitle className="text-base">Backtest Submitted ✓</CardTitle>
        <CardDescription>
          The backtest is running in the background. Check results once completed.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="text-sm space-y-1.5">
          <Row label="Run ID" value={run.runId.slice(0, 18) + "…"} />
          <Row label="Strategy" value={run.strategyId} />
          <Row label="Tickers" value={run.tickers.join(", ")} />
          <Row label="Period" value={`${run.startDate} – ${run.endDate}`} />
          <Row label="Created" value={formatDate(run.createdAt)} />
          <div className="flex items-center justify-between pt-1">
            <span className="text-muted-foreground">Status</span>
            <StatusBadge status={run.status} />
          </div>
        </div>
        <div className="flex gap-3 pt-2">
          <Button className="flex-1" onClick={onViewResults}>View Results</Button>
          <Button variant="outline" className="flex-1" onClick={onNew}>New Backtest</Button>
        </div>
      </CardContent>
    </Card>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium font-mono text-xs">{value}</span>
    </div>
  );
}
