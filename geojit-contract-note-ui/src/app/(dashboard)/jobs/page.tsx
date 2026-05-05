"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { cn, fmtDateTime } from "@/lib/utils";
import { toast } from "sonner";
import { jobsApi } from "@/lib/api";

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
  const [allJobs, setAllJobs] = useState<any[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const res = await jobsApi.list(page - 1, pageSize);
      const data = res.data.data;
      setAllJobs(data.content ?? []);
      setTotalElements(data.totalElements ?? 0);
      setTotalPages(data.totalPages ?? 1);
    } catch (err) {
      console.error("Failed to fetch jobs", err);
      setAllJobs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchJobs(); }, [page, pageSize]);

  useEffect(() => {
    const hasRunning = allJobs.some((j) => STATUS_RUNNING.includes(j.status));
    if (!hasRunning) return;
    const interval = setInterval(fetchJobs, 15000);
    return () => clearInterval(interval);
  }, [allJobs]);

  const jobs = activeTab === "Running"   ? allJobs.filter((j) => STATUS_RUNNING.includes(j.status))
             : activeTab === "Completed" ? allJobs.filter((j) => j.status === "COMPLETED")
             : activeTab === "Partial"   ? allJobs.filter((j) => j.status === "PARTIAL")
             : activeTab === "Failed"    ? allJobs.filter((j) => j.status === "FAILED")
             : allJobs;

  const filtered = jobs.filter((j) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return j.fileName?.toLowerCase().includes(q) || j.jobId?.toLowerCase().includes(q);
  });

  const totalRuns  = totalElements;
  const running    = allJobs.filter((j) => STATUS_RUNNING.includes(j.status)).length;
  const completed  = allJobs.filter((j) => j.status === "COMPLETED").length;
  const partFailed = allJobs.filter((j) => ["PARTIAL", "FAILED"].includes(j.status)).length;

  function downloadCsv() {
    const headers = ["Job ID", "File Name", "Segment", "Records", "Status", "Delivered", "Bounced", "Failed", "Uploaded"];
    const rows = jobs.map((j) => [
      j.jobId,
      j.fileName,
      j.segmentType,
      j.totalRecords,
      j.status,
      j.emailDeliveredCount,
      j.bounceCount,
      j.emailFailedCount,
      fmtDateTime(j.uploadedAt),
    ]);
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
          <button
            onClick={fetchJobs}
            className="flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors"
          >
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
              {loading ? (
                <tr>
                  <td colSpan={9} className="text-center py-12 text-gray-400 text-sm">
                    Loading…
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-12 text-gray-400 text-sm">
                    No jobs found
                  </td>
                </tr>
              ) : (
                filtered.map((j) => (
                  <tr key={j.jobId} className="t-row">
                    <td className="px-4 py-3">
                      <Link href={`/jobs/${j.jobId}`} className="text-[#497cff] hover:underline text-sm font-medium">
                        {j.fileName}
                      </Link>
                    </td>
                    <td className="px-4 py-3"><span className="seg-chip">{j.segmentType}</span></td>
                    <td className="px-4 py-3 text-right mono text-xs">{(j.totalRecords ?? 0).toLocaleString()}</td>
                    <td className="px-4 py-3"><StatusPill status={j.status} /></td>
                    <td className="px-4 py-3 text-xs text-gray-500">{fmtDateTime(j.uploadedAt)}</td>
                    <td className="px-4 py-3 text-xs text-gray-500 truncate max-w-[140px]">
                      {j.uploadedBy?.name ?? j.uploadedBy?.email ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-right text-xs text-gray-700">
                      {(j.emailDeliveredCount ?? 0).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right text-xs">
                      {(j.bounceCount ?? 0) > 0 ? (
                        <Link href={`/jobs/${j.jobId}?tab=customers&filter=BOUNCED`} className="text-amber-500 hover:underline font-medium">
                          {j.bounceCount.toLocaleString()}
                        </Link>
                      ) : <span className="text-gray-300">0</span>}
                    </td>
                    <td className="px-4 py-3 text-right text-xs">
                      {(j.emailFailedCount ?? 0) > 0 ? (
                        <Link href="/exceptions" className="text-red-500 hover:underline font-medium">
                          {j.emailFailedCount.toLocaleString()}
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
