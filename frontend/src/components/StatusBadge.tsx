import { Badge } from "@/components/ui/badge";
import type { BacktestStatus } from "@/lib/types";

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
