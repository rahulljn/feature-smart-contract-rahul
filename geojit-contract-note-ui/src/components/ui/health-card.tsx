"use client";

import { cn } from "@/lib/utils";

type Severity = "high" | "medium" | "low" | "ok";

const severityDot: Record<Severity, string> = {
  high:   "bg-red-500",
  medium: "bg-amber-400",
  low:    "bg-blue-500",
  ok:     "bg-green-500",
};

const severityText: Record<Severity, string> = {
  high:   "text-red-700",
  medium: "text-amber-700",
  low:    "text-blue-700",
  ok:     "text-green-700",
};

interface HealthCardProps {
  label: string;
  value: string | number;
  severity?: Severity;
  description?: string;
  className?: string;
}

export function HealthCard({ label, value, severity = "ok", description, className }: HealthCardProps) {
  return (
    <div className={cn("bg-white rounded-xl border border-gray-100 p-4", className)}>
      <div className="flex items-center gap-2 mb-2">
        <span className={cn("w-2 h-2 rounded-full flex-shrink-0", severityDot[severity])} />
        <span className="text-[10px] uppercase tracking-widest text-gray-400 font-medium">{label}</span>
      </div>
      <div className={cn("text-2xl font-bold", severityText[severity])}>{value}</div>
      {description && (
        <div className="text-xs text-gray-400 mt-1">{description}</div>
      )}
    </div>
  );
}
