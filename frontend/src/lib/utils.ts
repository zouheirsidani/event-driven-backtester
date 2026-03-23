/**
 * Shared utility functions used across the React UI.
 * Covers Tailwind class merging and number/date formatting for display.
 */
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Merges Tailwind CSS class names, resolving conflicts via tailwind-merge.
 * Accepts any combination of strings, arrays, or conditional objects that clsx supports.
 *
 * @param inputs Class values to merge.
 * @returns Single merged class string.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Formats a decimal fraction as a percentage string with two decimal places.
 * Returns an em-dash for null/undefined values.
 *
 * @param value Decimal fraction (e.g. 0.15 → "15.00%").
 * @returns Formatted percentage string or "—".
 */
export function formatPct(value: number | undefined | null): string {
  if (value == null) return "—";
  // Multiply by 100 to convert from decimal fraction to percentage
  return (value * 100).toFixed(2) + "%";
}

/**
 * Formats a number to a fixed number of decimal places.
 * Returns an em-dash for null/undefined values.
 *
 * @param value   Number to format.
 * @param decimals Number of decimal places (default 2).
 * @returns Formatted number string or "—".
 */
export function formatNumber(value: number | undefined | null, decimals = 2): string {
  if (value == null) return "—";
  return value.toFixed(decimals);
}

/**
 * Formats a number as a USD currency string with no cents (rounded to whole dollars).
 * Returns an em-dash for null/undefined values.
 *
 * @param value Dollar amount to format.
 * @returns Formatted currency string (e.g. "$100,000") or "—".
 */
export function formatCurrency(value: number | undefined | null): string {
  if (value == null) return "—";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
}

/**
 * Formats an ISO 8601 date/datetime string as a human-readable date.
 *
 * @param iso ISO 8601 string (date or instant, e.g. "2022-01-03T00:00:00Z").
 * @returns Formatted string like "Jan 3, 2022".
 */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
