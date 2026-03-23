/**
 * Persistent application shell component that wraps all pages.
 * Renders a fixed sidebar with navigation links and a scrollable main content area.
 * Active route highlighting is handled by React Router's NavLink isActive prop.
 */
import { NavLink, Outlet } from "react-router-dom";
import { BarChart2, Database, FlaskConical, LayoutDashboard } from "lucide-react";
import { cn } from "@/lib/utils";

const nav = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/market-data", label: "Market Data", icon: Database },
  { to: "/backtest", label: "Run Backtest", icon: FlaskConical },
  { to: "/results", label: "Results", icon: BarChart2 },
];

export default function Layout() {
  return (
    <div className="min-h-screen flex bg-background">
      {/* Sidebar */}
      <aside className="w-56 shrink-0 border-r flex flex-col">
        <div className="h-16 flex items-center px-6 border-b">
          <span className="font-semibold text-sm tracking-tight">
            ⚡ Backtester
          </span>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          {nav.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors",
                  isActive
                    ? "bg-primary text-primary-foreground font-medium"
                    : "text-muted-foreground hover:text-foreground hover:bg-muted"
                )
              }
            >
              <Icon className="h-4 w-4 shrink-0" />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="px-6 py-4 border-t">
          <p className="text-xs text-muted-foreground">Milestone 1 · Java 21</p>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        <div className="max-w-6xl mx-auto px-6 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
