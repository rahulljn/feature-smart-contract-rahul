"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

const USE_MOCK = true;
void USE_MOCK;

// ─── Mock data ────────────────────────────────────────────────────────────────
const MOCK_JOBS = [
  { jobId: "a1b2c3d4", fileName: "EQUITY_20260429.txt",    segment: "EQUITY-COMBINEMARGIN", records: 28200, status: "COMPLETED",  uploadedAt: "2 hours ago", uploadedBy: "admin@geojit.com", delivered: 26100, bounced: 640,  failed: 318  },
  { jobId: "b2c3d4e5", fileName: "COMMODITY_20260429.txt", segment: "COMMODITY",            records: 5842,  status: "EMAILING",   uploadedAt: "3 hours ago", uploadedBy: "ops@geojit.com",   delivered: 3200,  bounced: 15,   failed: 8    },
  { jobId: "c3d4e5f6", fileName: "DP_HOLDING_20260428.txt",segment: "DP-HOLDING",           records: 12400, status: "PROCESSING", uploadedAt: "5 hours ago", uploadedBy: "admin@geojit.com", delivered: 0,     bounced: 0,    failed: 0    },
  { jobId: "d4e5f6a7", fileName: "PNL_20260428.txt",       segment: "PNL",                  records: 3100,  status: "FAILED",     uploadedAt: "6 hours ago", uploadedBy: "ops@geojit.com",   delivered: 0,     bounced: 0,    failed: 3100 },
  { jobId: "e5f6a7b8", fileName: "ROS_20260427.txt",       segment: "ROS",                  records: 8500,  status: "COMPLETED",  uploadedAt: "1 day ago",   uploadedBy: "admin@geojit.com", delivered: 8320,  bounced: 92,   failed: 42   },
  { jobId: "f6a7b8c9", fileName: "BILL_20260427.txt",      segment: "BILL",                 records: 4200,  status: "PARTIAL",    uploadedAt: "1 day ago",   uploadedBy: "ops@geojit.com",   delivered: 3900,  bounced: 210,  failed: 180  },
];

const STATUS_TABS = ["All", "Running", "Completed", "Partial", "Failed"];
const STATUS_RUNNING = ["EMAILING", "PROCESSING", "SPLITTING", "VALIDATING"];

const STATUS_BADGE: Record<string, string> = {
  COMPLETED:  "border border-green-500  text-green-700  bg-green-50",
  EMAILING:   "border border-blue-500   text-blue-700   bg-blue-50",
  PROCESSING: "border border-purple-500 text-purple-700 bg-purple-50",
  FAILED:     "border border-red-500    text-red-700    bg-red-50",
  PARTIAL:    "border border-amber-500  text-amber-700  bg-amber-50",
};

function StatusPill({ status }: { status: string }) {
  const cls = STATUS_BADGE[status] ?? "border border-gray-300 text-gray-600 bg-gray-50";
  const isActive = STATUS_RUNNING.includes(status);
  return (
    <span className={cn("inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold", cls)}>
      {isActive && <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse" />}
      {status}
    </span>
  );
}

export default function JobsPage() {
  const [activeTab, setActiveTab] = useState("All");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const jobs = MOCK_JOBS;

  const filtered = jobs.filter((j) => {
    if (activeTab === "Running"   && !STATUS_RUNNING.includes(j.status))      return false;
    if (activeTab === "Completed" && j.status !== "COMPLETED")                return false;
    if (activeTab === "Partial"   && j.status !== "PARTIAL")                  return false;
    if (activeTab === "Failed"    && j.status !== "FAILED")                   return false;
    if (search && !j.fileName.toLowerCase().includes(search.toLowerCase()) &&
        !j.jobId.toLowerCase().includes(search.toLowerCase()))               return false;
    return true;
  });

  const totalPages = Math.ceil(filtered.length / pageSize);
  const paginated  = filtered.slice((page - 1) * pageSize, page * pageSize);

  const totalRuns  = jobs.length;
  const running    = jobs.filter((j) => STATUS_RUNNING.includes(j.status)).length;
  const completed  = jobs.filter((j) => j.status === "COMPLETED").length;
  const partFailed = jobs.filter((j) => ["PARTIAL", "FAILED"].includes(j.status)).length;

  useEffect(() => {
    const hasRunning = jobs.some((j) => STATUS_RUNNING.includes(j.status));
    if (!hasRunning || USE_MOCK) return;
    const interval = setInterval(() => {
      // trigger refetch when real API is available
    }, 15000);
    return () => clearInterval(interval);
  }, [jobs]);

  function downloadCsv() {
    const headers = ["Job ID","File Name","Segment","Records","Status","Delivered","Bounced","Failed","Uploaded"];
    const rows = jobs.map((j) => [j.jobId, j.fileName, j.segment, j.records, j.status, j.delivered, j.bounced, j.failed, j.uploadedAt]);
    const csv  = [headers, ...rows].map((r) => r.join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement("a");
    a.href = url; a.download = "runs.csv"; a.click();
    URL.revokeObjectURL(url);
    toast.success("Downloaded runs.csv");
  }

  return (
    <div className="space-y-5 fade-up">

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="font-bold text-2xl text-gray-900">Jobs</h1>
        <div className="flex gap-2">
          <button
            onClick={downloadCsv}
            className="flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">download</span>
            Download CSV
          </button>
          <button className="flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors">
            <span className="material-symbols-outlined text-[16px]">refresh</span>
            Refresh
          </button>
        </div>
      </div>

      {/* Summary strip */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: "Total Runs",       value: totalRuns,  pulse: false, href: undefined },
          { label: "Running",          value: running,    pulse: true,  href: undefined },
          { label: "Completed",        value: completed,  pulse: false, href: undefined },
          { label: "Partial / Failed", value: partFailed, pulse: false, href: "/exceptions" },
        ].map((card) => (
          <div
            key={card.label}
            className={cn(
              "bg-white rounded-xl border border-gray-100 p-3",
              card.href && "cursor-pointer hover:border-red-200 transition-colors",
            )}
            onClick={card.href ? () => { window.location.href = card.href!; } : undefined}
          >
            <div className="text-xs text-gray-500 mb-1">{card.label}</div>
            <div className="flex items-center gap-1.5">
              <span className="text-2xl font-bold text-gray-900">{card.value}</span>
              {card.pulse && card.value > 0 && (
                <span className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Filter tabs + search */}
      <div className="flex items-end gap-0 border-b border-gray-200">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => { setActiveTab(tab); setPage(1); }}
            className={cn(
              "px-4 py-2.5 text-sm font-medium transition-colors -mb-px",
              activeTab === tab
                ? "border-b-2 border-[#00174b] text-[#00174b]"
                : "text-gray-500 hover:text-gray-900",
            )}
          >
            {tab}
          </button>
        ))}
        <div className="ml-auto flex items-center gap-2 pb-1">
          <div className="relative">
            <span className="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400 text-[16px]">
              search
            </span>
            <input
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(1); }}
              placeholder="Search file name or ID…"
              className="pl-8 pr-3 py-1.5 bg-gray-50 border border-gray-200 rounded-lg text-sm w-52 focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
            />
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="t-hd">
                <th className="px-4 py-3">FILE NAME</th>
                <th className="px-4 py-3">SEGMENT</th>
                <th className="px-4 py-3 text-right">RECORDS</th>
                <th className="px-4 py-3">STATUS</th>
                <th className="px-4 py-3">UPLOADED</th>
                <th className="px-4 py-3">BY</th>
                <th className="px-4 py-3 text-right">DELIVERED</th>
                <th className="px-4 py-3 text-right">BOUNCED</th>
                <th className="px-4 py-3 text-right">FAILED</th>
              </tr>
            </thead>
            <tbody>
              {paginated.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-12 text-gray-400 text-sm">
                    No jobs found
                  </td>
                </tr>
              ) : (
                paginated.map((j) => (
                  <tr key={j.jobId} className="t-row">
                    <td className="px-4 py-3">
                      <Link href={`/jobs/${j.jobId}`} className="text-[#497cff] hover:underline text-sm font-medium">
                        {j.fileName}
                      </Link>
                    </td>
                    <td className="px-4 py-3"><span className="seg-chip">{j.segment}</span></td>
                    <td className="px-4 py-3 text-right mono text-xs">{j.records.toLocaleString()}</td>
                    <td className="px-4 py-3"><StatusPill status={j.status} /></td>
                    <td className="px-4 py-3 text-xs text-gray-500">{j.uploadedAt}</td>
                    <td className="px-4 py-3 text-xs text-gray-500 truncate max-w-[140px]">{j.uploadedBy}</td>
                    <td className="px-4 py-3 text-right text-xs text-gray-700">
                      {j.delivered.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right text-xs">
                      {j.bounced > 0 ? (
                        <Link href={`/jobs/${j.jobId}?tab=customers&filter=BOUNCED`} className="text-amber-500 hover:underline font-medium">
                          {j.bounced.toLocaleString()}
                        </Link>
                      ) : <span className="text-gray-300">0</span>}
                    </td>
                    <td className="px-4 py-3 text-right text-xs">
                      {j.failed > 0 ? (
                        <Link href="/exceptions" className="text-red-500 hover:underline font-medium">
                          {j.failed.toLocaleString()}
                        </Link>
                      ) : <span className="text-gray-300">0</span>}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100">
          <div className="flex items-center gap-2 text-sm text-gray-500">
            Rows per page:
            <select
              value={pageSize}
              onChange={(e) => { setPageSize(Number(e.target.value)); setPage(1); }}
              className="border border-gray-200 rounded-lg px-2 py-1 text-sm bg-white"
            >
              {[10, 25, 50].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              className="px-3 py-1 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              ← Prev
            </button>
            <span className="text-gray-700 font-medium">
              Page {page} of {Math.max(1, totalPages)}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page >= totalPages}
              className="px-3 py-1 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Next →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
