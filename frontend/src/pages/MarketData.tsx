import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getSymbols, createSymbol, getBars } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import type { AssetClass } from "@/lib/types";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

export default function MarketData() {
  const qc = useQueryClient();
  const { data: symbolsData } = useQuery({ queryKey: ["symbols"], queryFn: getSymbols });

  // Register symbol form
  const [ticker, setTicker] = useState("");
  const [name, setName] = useState("");
  const [exchange, setExchange] = useState("NASDAQ");
  const [assetClass, setAssetClass] = useState<AssetClass>("STOCK");

  const registerMutation = useMutation({
    mutationFn: createSymbol,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["symbols"] });
      setTicker("");
      setName("");
    },
  });

  // Bar query form
  const [queryTicker, setQueryTicker] = useState("");
  const [from, setFrom] = useState("2022-01-01");
  const [to, setTo] = useState("2023-12-31");
  const [fetchEnabled, setFetchEnabled] = useState(false);

  const { data: barsData, isFetching: barsLoading } = useQuery({
    queryKey: ["bars", queryTicker, from, to],
    queryFn: () => getBars(queryTicker.toUpperCase(), from, to),
    enabled: fetchEnabled && !!queryTicker,
  });

  const chartData = barsData?.bars.map((b) => ({
    date: b.date,
    close: Number(b.close),
  }));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Market Data</h1>
        <p className="text-muted-foreground mt-1">Register symbols and explore price history</p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Register symbol */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Register Symbol</CardTitle>
            <CardDescription>Add a new ticker to the system before ingesting bars</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Ticker</Label>
                <Input
                  placeholder="AAPL"
                  value={ticker}
                  onChange={(e) => setTicker(e.target.value.toUpperCase())}
                />
              </div>
              <div className="space-y-1">
                <Label>Exchange</Label>
                <Input
                  placeholder="NASDAQ"
                  value={exchange}
                  onChange={(e) => setExchange(e.target.value.toUpperCase())}
                />
              </div>
            </div>
            <div className="space-y-1">
              <Label>Name</Label>
              <Input
                placeholder="Apple Inc."
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label>Asset Class</Label>
              <Select value={assetClass} onValueChange={(v) => setAssetClass(v as AssetClass)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {["STOCK", "ETF", "FUTURES", "CRYPTO"].map((a) => (
                    <SelectItem key={a} value={a}>{a}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            {registerMutation.isError && (
              <p className="text-sm text-destructive">
                {(registerMutation.error as Error).message}
              </p>
            )}
            {registerMutation.isSuccess && (
              <p className="text-sm text-emerald-600">Symbol registered ✓</p>
            )}
            <Button
              className="w-full"
              onClick={() => registerMutation.mutate({ ticker, name, exchange, assetClass })}
              disabled={!ticker || !name || registerMutation.isPending}
            >
              {registerMutation.isPending ? "Registering…" : "Register Symbol"}
            </Button>
          </CardContent>
        </Card>

        {/* Symbols list */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">
              Registered Symbols{" "}
              <span className="text-muted-foreground font-normal text-sm">
                ({symbolsData?.count ?? 0})
              </span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            {!symbolsData?.symbols.length ? (
              <p className="text-sm text-muted-foreground">No symbols registered yet.</p>
            ) : (
              <div className="divide-y">
                {symbolsData.symbols.map((s) => (
                  <div key={s.ticker} className="flex items-center justify-between py-2">
                    <div>
                      <span className="font-medium text-sm">{s.ticker}</span>
                      <span className="text-muted-foreground text-sm ml-2">{s.name}</span>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span>{s.exchange}</span>
                      <Separator orientation="vertical" className="h-3" />
                      <span>{s.assetClass}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Price chart */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Price Chart</CardTitle>
          <CardDescription>Query closing prices for a registered ticker</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-3 items-end">
            <div className="space-y-1">
              <Label>Ticker</Label>
              <Input
                className="w-28"
                placeholder="AAPL"
                value={queryTicker}
                onChange={(e) => {
                  setQueryTicker(e.target.value.toUpperCase());
                  setFetchEnabled(false);
                }}
              />
            </div>
            <div className="space-y-1">
              <Label>From</Label>
              <Input
                type="date"
                className="w-36"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label>To</Label>
              <Input
                type="date"
                className="w-36"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            </div>
            <Button
              onClick={() => setFetchEnabled(true)}
              disabled={!queryTicker || barsLoading}
            >
              {barsLoading ? "Loading…" : "Fetch Bars"}
            </Button>
          </div>

          {barsData && chartData && chartData.length > 0 ? (
            <>
              <p className="text-sm text-muted-foreground">
                {barsData.count} bars · {barsData.ticker}
              </p>
              <ResponsiveContainer width="100%" height={280}>
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
                    tickFormatter={(v: number) => `$${v.toFixed(0)}`}
                    width={55}
                  />
                  <Tooltip
                    formatter={(v) => [`$${(v as number).toFixed(2)}`, "Close"]}
                    labelStyle={{ fontSize: 12 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="close"
                    stroke="hsl(var(--primary))"
                    dot={false}
                    strokeWidth={1.5}
                  />
                </LineChart>
              </ResponsiveContainer>
            </>
          ) : fetchEnabled && !barsLoading ? (
            <p className="text-sm text-muted-foreground">No bars found for that query.</p>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
