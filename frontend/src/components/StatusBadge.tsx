/**
 * Renders a coloured badge for a backtest lifecycle status.
 * Maps each BacktestStatus value to a Badge variant and a human-readable label.
 */
import { Badge } from "@/components/ui/badge";
import type { BacktestStatus } from "@/lib/types";

/**
 * Displays the current status of a backtest run as a styled badge.
 *
 * @param status - The BacktestStatus to display (PENDING, RUNNING, COMPLETED, or FAILED).
 * @returns A Badge component with appropriate variant and label.
 */
export default function StatusBadge({ status }: { status: BacktestStatus }) {
  const map = {
    PENDING: { variant: "outline" as const, label: "Pending" },
    RUNNING: { variant: "running" as const, label: "Running" },
    COMPLETED: { variant: "success" as const, label: "Completed" },
    FAILED: { variant: "destructive" as const, label: "Failed" },
  };
  const { variant, label } = map[status] ?? { variant: "outline" as const, label: status };
  return <Badge variant={variant}>{label}</Badge>;
}
