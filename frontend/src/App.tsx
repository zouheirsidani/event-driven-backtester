/**
 * Root application component.
 * Sets up the React Query client for server state management and
 * defines all client-side routes via React Router.
 * All routes are nested inside the persistent Layout component (sidebar + main area).
 */
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Layout from "@/components/Layout";
import Dashboard from "@/pages/Dashboard";
import MarketData from "@/pages/MarketData";
import RunBacktest from "@/pages/RunBacktest";
import Results from "@/pages/Results";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 10_000,
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/market-data" element={<MarketData />} />
            <Route path="/backtest" element={<RunBacktest />} />
            <Route path="/results" element={<Results />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
