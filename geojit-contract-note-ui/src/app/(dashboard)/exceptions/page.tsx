"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { lambdaExceptionsApi, jobsApi } from "@/lib/api";
import type { LambdaException } from "@/types";

// ─── Helpers ────────────────────────────────────────────────────────────────
const LAMBDA_TABS = [
  { key: "Split", label: "Split" },
  { key: "Invoke", label: "Invoke" },
  { key: "GetJson", label: "GetJson" },
  { key: "PDF", label: "PDF" },
  { key: "Email", label: "Email" },
  { key: "Pull Bounce", label: "Pull Bounce" },
  { key: "Pull Delivery", label: "Pull Delivery" },
];

const LAMBDA_NAMES: Record<string, string> = {
  "Split": "split-lambda-geojit",
  "Invoke": "invoke-lambda-geojit",
  "GetJson": "json-lambda-geojit",
  "PDF": "create-pdf-geojit",
  "Email": "email-notification-geojit",
  "Pull Bounce": "pull-bounce-geojit",
  "Pull Delivery": "pull-delivery-geojit",
};

function formatExceptionDate(date?: string): string {
  if (!date) return "—";
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}:${String(d.getSeconds()).padStart(2, "0")}`;
}

function ExceptionsContent() {
  const searchParams = useSearchParams();
  const [activeTab, setActiveTab] = useState("PDF");
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [jobs, setJobs] = useState<any[]>([]);
  const [selectedJobId, setSelectedJobId] = useState<string>(searchParams.get("jobId") ?? "");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exceptions, setExceptions] = useState<LambdaException[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  const fetchExceptions = async () => {
    try {
      setLoading(true);
      setError(null);
      const lambdaName = LAMBDA_NAMES[activeTab];
      const res = await lambdaExceptionsApi.list(selectedJobId || undefined, lambdaName);
      setExceptions(res.data.data ?? []);
    } catch (err) {
      setError("Failed to load exceptions");
      toast.error("Failed to load exceptions");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    jobsApi.list(0, 50).then(res => {
      setJobs(res.data.data?.content ?? []);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    fetchExceptions();
  }, [activeTab, selectedJobId]);

  function handleRefresh() {
    setRefreshing(true);
    fetchExceptions();
  }

  function copyTrace(trace?: string) {
    if (!trace) return;
    navigator.clipboard.writeText(trace).then(() => toast.success("Copied to clipboard"));
  }

  if (error) {
    return (
      <div className="space-y-5 fade-up">
        <div className="flex items-center justify-between">
          <h1 className="font-bold text-2xl text-gray-900">Exceptions</h1>
        </div>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">{error}</div>
      </div>
    );
  }

  return (
    <div className="space-y-5 fade-up">

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="font-bold text-2xl text-gray-900">Exceptions</h1>
        <div className="flex items-center gap-2">
          <select
            value={selectedJobId}
            onChange={(e) => setSelectedJobId(e.target.value)}
            className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm bg-white text-gray-700 min-w-[260px]"
          >
            <option value="">All Jobs</option>
            {jobs.map((job) => (
              <option key={job.jobId} value={job.jobId}>
                {job.fileName} · {new Date(job.uploadedAt).toLocaleDateString("en-GB")}
              </option>
            ))}
          </select>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors disabled:opacity-50"
          >
            <span className={cn("material-symbols-outlined text-[16px]", refreshing && "animate-spin")}>refresh</span>
            Refresh
          </button>
        </div>
      </div>

      {/* Lambda tabs */}
      <div className="flex gap-6 border-b border-gray-200 overflow-x-auto">
        {LAMBDA_TABS.map((tab) => {
          const count = exceptions.length;
          return (
            <button
              key={tab.key}
              onClick={() => { setActiveTab(tab.key); setExpandedId(null); }}
              className={cn(
                "flex items-center gap-1.5 pb-2.5 text-sm font-medium transition-colors -mb-px whitespace-nowrap",
                activeTab === tab.key
                  ? "border-b-2 border-[#00174b] text-[#00174b]"
                  : "text-gray-500 hover:text-gray-900",
              )}
            >
              {tab.label}
              {count > 0 && (
                <span className="px-1.5 py-0.5 rounded-full bg-red-100 text-red-600 text-[10px] font-bold">
                  {count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Table / Empty state */}
      <div className="bg-white rounded-xl border overflow-hidden">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 gap-2">
            <span className="material-symbols-outlined text-gray-400 text-[36px] animate-spin">sync</span>
            <p className="text-gray-500 text-sm">Loading exceptions…</p>
          </div>
        ) : exceptions.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-2">
            <span className="material-symbols-outlined text-green-400 text-[36px]">check_circle</span>
            <p className="text-gray-500 text-sm">No exceptions for this Lambda</p>
          </div>
        ) : (
          <table className="w-full text-left">
            <thead>
              <tr className="t-hd">
                <th className="px-4 py-3">JOB ID</th>
                <th className="px-4 py-3">ERROR TYPE</th>
                <th className="px-4 py-3">MESSAGE</th>
                <th className="px-4 py-3">TIMESTAMP</th>
                <th className="px-4 py-3 w-8" />
              </tr>
            </thead>
            <tbody>
              {exceptions.map((ex) => (
                <React.Fragment key={ex.id}>
                  <tr
                    className={cn("t-row cursor-pointer", expandedId === ex.id && "bg-gray-50")}
                    onClick={() => setExpandedId(expandedId === ex.id ? null : ex.id)}
                  >
                    <td className="px-4 py-3 mono text-xs font-bold text-gray-700">{ex.jobId}</td>
                    <td className="px-4 py-3 text-red-500 font-medium text-xs">{ex.errorType || "—"}</td>
                    <td className="px-4 py-3 text-gray-600 text-xs max-w-[320px] truncate" title={ex.errorMessage}>{ex.errorMessage || "—"}</td>
                    <td className="px-4 py-3 text-xs text-gray-400 mono">{formatExceptionDate(ex.occurredAt)}</td>
                    <td className="px-4 py-3">
                      <span className={cn(
                        "material-symbols-outlined text-gray-400 text-sm transition-transform duration-200",
                        expandedId === ex.id ? "rotate-90" : "",
                      )}>
                        chevron_right
                      </span>
                    </td>
                  </tr>
                  {expandedId === ex.id && (
                    <tr>
                      <td colSpan={5} className="p-0">
                        <div className="bg-gray-950 rounded-lg m-4 p-4">
                          <div className="flex justify-between items-center mb-2">
                            <span className="text-green-400 text-xs font-mono">Stack Trace</span>
                            {ex.stackTrace && (
                              <button
                                onClick={(e) => { e.stopPropagation(); copyTrace(ex.stackTrace); }}
                                className="text-xs text-gray-400 hover:text-white border border-gray-700 rounded px-2 py-0.5"
                              >
                                Copy
                              </button>
                            )}
                          </div>
                          <pre className="text-green-400 text-xs font-mono overflow-x-auto whitespace-pre-wrap">
                            {ex.stackTrace || "No stack trace available"}
                          </pre>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default function ExceptionsPage() {
  return (
    <Suspense>
      <ExceptionsContent />
    </Suspense>
  );
}
