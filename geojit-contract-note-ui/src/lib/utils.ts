import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// Backend serializes LocalDateTime without timezone suffix.
// Appending Z tells the browser it's UTC, preventing the 5.5h IST shift.
function ensureUtc(ts: string): string {
  return ts.endsWith("Z") || ts.includes("+") ? ts : ts + "Z";
}

const IST = "Asia/Kolkata";

/** "22 Apr 2026" */
export function fmtDate(ts: string | null | undefined): string {
  if (!ts) return "—";
  return new Date(ensureUtc(ts)).toLocaleDateString("en-GB", {
    day: "2-digit", month: "short", year: "numeric", timeZone: IST,
  });
}

/** "22 Apr 2026, 10:30 PM" */
export function fmtDateTime(ts: string | null | undefined): string {
  if (!ts) return "—";
  return new Date(ensureUtc(ts)).toLocaleString("en-GB", {
    day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit", timeZone: IST,
  });
}

/** "10:30 PM" */
export function fmtTime(ts: string | null | undefined): string {
  if (!ts) return "—";
  return new Date(ensureUtc(ts)).toLocaleTimeString("en-GB", {
    hour: "2-digit", minute: "2-digit", timeZone: IST,
  });
}

/** "2 hours ago" style (date-fns compatible input) */
export function toUtcDate(ts: string | null | undefined): Date | null {
  if (!ts) return null;
  return new Date(ensureUtc(ts));
}
