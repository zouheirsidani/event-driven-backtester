/**
 * Strategy Templates page — lists all saved user strategy templates and provides
 * a form to create new ones.
 *
 * Each template card shows the name, base strategy type, stored parameters, and
 * creation date. A delete button removes the template after confirmation.
 *
 * The "New Template" form at the top lets users pick a base strategy from the
 * registered list, supply a name, and set strategy-specific parameter values
 * (lookbackDays for MOMENTUM_V1; windowSize + zScoreThreshold for MEAN_REVERSION_V1).
 */
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getUserStrategies, createUserStrategy, deleteUserStrategy, getStrategies } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { UserStrategyDto } from "@/lib/types";
import { formatDate } from "@/lib/utils";

export default function StrategyTemplates() {
  const queryClient = useQueryClient();
  const { data: templatesData, isLoading } = useQuery({
    queryKey: ["userStrategies"],
    queryFn: getUserStrategies,
  });
  const { data: strategiesData } = useQuery({
    queryKey: ["strategies"],
    queryFn: getStrategies,
  });

  // New template form state
  const [name, setName] = useState("");
  const [baseStrategyId, setBaseStrategyId] = useState("");
  // Strategy-specific parameters
  const [lookbackDays, setLookbackDays] = useState("20");
  const [windowSize, setWindowSize] = useState("20");
  const [zScoreThreshold, setZScoreThreshold] = useState("-2.0");

  const createMutation = useMutation({
    mutationFn: createUserStrategy,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["userStrategies"] });
      setName("");
      setBaseStrategyId("");
      setLookbackDays("20");
      setWindowSize("20");
      setZScoreThreshold("-2.0");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteUserStrategy,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["userStrategies"] });
    },
  });

  /**
   * Builds the parameters map based on the selected base strategy and current
   * strategy-specific field values, then submits the create mutation.
   */
  function handleCreate() {
    if (!name.trim() || !baseStrategyId) return;
    const parameters: Record<string, unknown> = {};
    if (baseStrategyId === "MOMENTUM_V1") {
      parameters["lookbackDays"] = Number(lookbackDays);
    } else if (baseStrategyId === "MEAN_REVERSION_V1") {
      parameters["windowSize"] = Number(windowSize);
      parameters["zScoreThreshold"] = Number(zScoreThreshold);
    }
    createMutation.mutate({ name: name.trim(), baseStrategyId, parameters });
  }

  /**
   * Deletes a template after user confirmation.
   * @param id UUID of the template to delete.
   * @param templateName Display name shown in the confirm dialog.
   */
  function handleDelete(id: string, templateName: string) {
    if (!window.confirm(`Delete template "${templateName}"? This cannot be undone.`)) return;
    deleteMutation.mutate(id);
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Strategy Templates</h1>
        <p className="text-muted-foreground mt-1">
          Save named strategy configurations to reuse when running backtests.
        </p>
      </div>

      {/* New Template form */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">New Template</CardTitle>
          <CardDescription>
            Give your template a name, pick a base strategy, and set its parameters.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-1">
              <Label>Template Name</Label>
              <Input
                placeholder="e.g. Aggressive Momentum"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label>Base Strategy</Label>
              <Select value={baseStrategyId} onValueChange={setBaseStrategyId}>
                <SelectTrigger>
                  <SelectValue placeholder="Select a strategy…" />
                </SelectTrigger>
                <SelectContent>
                  {strategiesData?.strategies.map((s) => (
                    <SelectItem key={s.strategyId} value={s.strategyId}>
                      {s.displayName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Dynamic parameter fields based on selected strategy */}
          {baseStrategyId === "MOMENTUM_V1" && (
            <div className="space-y-1">
              <Label>Lookback Days</Label>
              <Input
                type="number"
                min={1}
                value={lookbackDays}
                onChange={(e) => setLookbackDays(e.target.value)}
                className="max-w-[160px]"
              />
            </div>
          )}

          {baseStrategyId === "MEAN_REVERSION_V1" && (
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <Label>Window Size (days)</Label>
                <Input
                  type="number"
                  min={1}
                  value={windowSize}
                  onChange={(e) => setWindowSize(e.target.value)}
                />
              </div>
              <div className="space-y-1">
                <Label>Z-Score Threshold (entry)</Label>
                <Input
                  type="number"
                  step="0.1"
                  value={zScoreThreshold}
                  onChange={(e) => setZScoreThreshold(e.target.value)}
                />
              </div>
            </div>
          )}

          {createMutation.isError && (
            <p className="text-sm text-destructive">{(createMutation.error as Error).message}</p>
          )}

          <Button
            onClick={handleCreate}
            disabled={!name.trim() || !baseStrategyId || createMutation.isPending}
          >
            {createMutation.isPending ? "Saving…" : "Save Template"}
          </Button>
        </CardContent>
      </Card>

      {/* Template list */}
      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading templates…</p>
      ) : !templatesData?.templates.length ? (
        <p className="text-sm text-muted-foreground">No templates saved yet.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {templatesData.templates.map((t) => (
            <TemplateCard
              key={t.id}
              template={t}
              onDelete={() => handleDelete(t.id, t.name)}
              isDeleting={deleteMutation.isPending}
            />
          ))}
        </div>
      )}
    </div>
  );
}

/**
 * Card displaying a single user strategy template with its metadata and a delete button.
 *
 * @param template  The template DTO to render.
 * @param onDelete  Callback invoked when the delete button is clicked.
 * @param isDeleting Whether a delete operation is currently in progress (disables button).
 */
function TemplateCard({
  template,
  onDelete,
  isDeleting,
}: {
  template: UserStrategyDto;
  onDelete: () => void;
  isDeleting: boolean;
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="text-sm">{template.name}</CardTitle>
            <CardDescription className="text-xs mt-0.5">{template.baseStrategyId}</CardDescription>
          </div>
          <Button
            variant="outline"
            size="sm"
            className="h-7 text-xs text-destructive hover:text-destructive"
            onClick={onDelete}
            disabled={isDeleting}
          >
            Delete
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-2 text-xs">
        {Object.keys(template.parameters).length > 0 && (
          <div className="rounded-md bg-muted px-3 py-2 font-mono space-y-0.5">
            {Object.entries(template.parameters).map(([k, v]) => (
              <div key={k} className="flex justify-between gap-4">
                <span className="text-muted-foreground">{k}</span>
                <span>{String(v)}</span>
              </div>
            ))}
          </div>
        )}
        <div className="text-muted-foreground">
          Created {formatDate(template.createdAt)}
        </div>
      </CardContent>
    </Card>
  );
}
