/**
 * Persistent application shell component that wraps all pages.
 * Renders a fixed sidebar with navigation links and a scrollable main content area.
 * Active route highlighting is handled by React Router's NavLink isActive prop.
 */
import { useState, useEffect } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { BarChart2, BookMarked, Database, FlaskConical, GitCompare, LayoutDashboard, Moon, RefreshCw, SlidersHorizontal, Sun } from "lucide-react";
import { cn } from "@/lib/utils";

const nav = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/market-data", label: "Market Data", icon: Database },
  { to: "/backtest", label: "Run Backtest", icon: FlaskConical },
  { to: "/results", label: "Results", icon: BarChart2 },
  { to: "/compare", label: "Compare", icon: GitCompare },
  { to: "/parameter-sweep", label: "Parameter Sweep", icon: SlidersHorizontal },
  { to: "/walk-forward", label: "Walk-Forward", icon: RefreshCw },
  { to: "/strategy-templates", label: "Strategy Templates", icon: BookMarked },
];

/**
 * Persistent application shell with sidebar navigation and a light/dark mode toggle.
 * Theme preference is stored in localStorage and applied via the "dark" class on <html>.
 */
export default function Layout() {
  const [dark, setDark] = useState(() => localStorage.getItem("theme") === "dark");

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
    localStorage.setItem("theme", dark ? "dark" : "light");
  }, [dark]);

  return (
    <div className="min-h-screen flex bg-background">
      {/* Sidebar */}
      <aside className="w-56 shrink-0 border-r flex flex-col">
        <div className="h-16 flex items-center justify-between px-6 border-b">
          <span className="font-semibold text-sm tracking-tight">⚡ Backtester</span>
          <button
            onClick={() => setDark((d) => !d)}
            className="rounded-md p-1.5 text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
            aria-label="Toggle theme"
          >
            {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>
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
          <p className="text-xs text-muted-foreground">Milestone 3 · Java 21</p>
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
