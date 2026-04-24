"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { jobsApi } from "@/lib/api";
import type { Job } from "@/types";
import { Loader2 } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import Link from "next/link";
import { cn, toUtcDate } from "@/lib/utils";
import { downloadCsv } from "@/lib/export";
import { toast } from "sonner";

const statusPill: Record<string, string> = {
  COMPLETED: "pill-ok", PROCESSING: "pill-info", SPLITTING: "pill-info",
  EMAILING: "pill-info", VALIDATING: "pill-neu", FAILED: "pill-err", PARTIAL: "pill-warn",
};
const statusDot: Record<string, string> = {
  COMPLETED: "dot-ok", PROCESSING: "dot-run", SPLITTING: "dot-run",
  EMAILING: "dot-run", VALIDATING: "dot-info", FAILED: "dot-err", PARTIAL: "dot-warn",
};
const FILTERS = ["All", "Running", "Completed", "Partial", "Failed"];

export default function JobsPage() {
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState("All");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  // Map UI filter to API status param
  const apiStatus = filter === "All" ? undefined
    : filter === "Running" ? undefined // multiple statuses, filter client-side
    : filter === "Completed" ? "COMPLETED"
    : filter === "Partial" ? "PARTIAL"
    : filter === "Failed" ? "FAILED"
    : undefined;

  const { data: jobsRes, isLoading } = useQuery({
    queryKey: ["jobs", page, apiStatus, dateFrom, dateTo],
    queryFn: () => jobsApi.list(page, 20, apiStatus, dateFrom || undefined, dateTo || undefined),
  });

  const jobs: Job[] = jobsRes?.data?.data?.content ?? [];
  const totalElements = jobsRes?.data?.data?.totalElements ?? 0;
  const totalPages = jobsRes?.data?.data?.totalPages ?? 0;

  // For "Running" filter, do client-side since API doesn't support multi-status
  const filtered = filter === "Running"
    ? jobs.filter(j => ["PROCESSING","SPLITTING","EMAILING","VALIDATING"].includes(j.status))
    : jobs;

  const running = jobs.filter(j => ["PROCESSING","SPLITTING","EMAILING","VALIDATING"].includes(j.status)).length;

  // Auto-refresh the same query when there are running jobs
  useQuery({
    queryKey: ["jobs", page, apiStatus, dateFrom, dateTo],
    queryFn: () => jobsApi.list(page, 20, apiStatus, dateFrom || undefined, dateTo || undefined),
    refetchInterval: running > 0 ? 15_000 : false,
    enabled: running > 0,
  });

  const completed = jobs.filter(j => j.status === "COMPLETED").length;
  const failed = jobs.filter(j => ["FAILED","PARTIAL"].includes(j.status)).length;

  const clearFilters = () => { setDateFrom(""); setDateTo(""); setFilter("All"); setPage(0); };

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Runs &amp; Jobs</h2>
          <p className="text-slate-500 text-sm mt-1">Click any row to open the run detail with pipeline, exceptions, bounces and client list.</p>
        </div>
        <div className="flex gap-2">
          <Link href="/process" className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5">
            <span className="material-symbols-outlined text-base">add</span>New run
          </Link>
          <button onClick={() => downloadCsv("runs.csv", ["Job ID","File Name","Segment","Records","Status","Uploaded","By"], filtered.map(j => [j.jobId, j.fileName ?? "", j.segmentType ?? "", String(j.totalRecords ?? ""), j.status, j.uploadedAt ?? "", j.uploadedBy?.name ?? ""]))} className="px-3.5 py-2 bg-white text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 border border-slate-200 flex items-center gap-1.5">
            <span className="material-symbols-outlined text-base">download</span>Export CSV
          </button>
        </div>
      </div>

      {/* Date filter bar */}
      <div className="card p-3">
        <div className="flex flex-wrap items-center gap-3">
          <span className="material-symbols-outlined text-slate-400 text-base">filter_list</span>
          <div className="flex items-center gap-2">
            <input type="date" value={dateFrom} onChange={e => { setDateFrom(e.target.value); setPage(0); }} className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-sm" />
            <span className="text-slate-400 text-xs">to</span>
            <input type="date" value={dateTo} onChange={e => { setDateTo(e.target.value); setPage(0); }} className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-sm" />
          </div>
          <div className="flex gap-1">
            <button onClick={() => { const t = new Date().toISOString().slice(0, 10); setDateFrom(t); setDateTo(t); setPage(0); }} className={cn("px-2.5 py-1 rounded-lg text-[11px] font-semibold", !dateFrom && !dateTo ? "bg-[#00174b] text-white" : "bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-[#003ea8]")}>Today</button>
            <button onClick={() => { const d = new Date(); setDateFrom(new Date(d.getTime() - 7 * 86400000).toISOString().slice(0, 10)); setDateTo(d.toISOString().slice(0, 10)); setPage(0); }} className="px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-[#003ea8]">Last 7d</button>
            <button onClick={() => { const d = new Date(); setDateFrom(new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)); setDateTo(d.toISOString().slice(0, 10)); setPage(0); }} className="px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-[#003ea8]">This month</button>
          </div>
          {(dateFrom || dateTo) && (
            <button onClick={clearFilters} className="p-1 text-slate-400 hover:text-slate-700"><span className="material-symbols-outlined text-sm">close</span></button>
          )}
          <div className="h-5 w-px bg-slate-200" />
          <div className="text-[11px] font-bold text-slate-500 uppercase tracking-widest">Status</div>
          {FILTERS.map(f => (
            <button key={f} onClick={() => { setFilter(f); setPage(0); }} className={cn("px-2.5 py-1 rounded-lg text-[11px] font-semibold", filter === f ? "bg-[#00174b] text-white" : "bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-[#003ea8]")}>{f}</button>
          ))}
          <div className="flex-1" />
          <span className="text-[11px] text-slate-400">{filtered.length} of {totalElements} runs</span>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Total runs</div><span className="material-symbols-outlined text-slate-300 text-lg">hub</span></div><div className="text-2xl font-extrabold mono text-slate-900 mt-1">{totalElements.toLocaleString()}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Running</div><span className="dot dot-run" /></div><div className="text-2xl font-extrabold mono text-[#497cff] mt-1">{running}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Completed</div><span className="dot dot-ok" /></div><div className="text-2xl font-extrabold mono text-emerald-600 mt-1">{completed}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Partial / failed</div><span className="dot dot-err" /></div><div className="text-2xl font-extrabold mono text-rose-600 mt-1">{failed}</div></div>
      </div>

      <div className="card overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
        ) : filtered.length === 0 ? (
          <div className="text-center text-slate-500 py-12 text-sm">No runs found</div>
        ) : (
          <table className="w-full text-left">
            <thead><tr className="t-hd"><th className="px-5 py-3">File Name</th><th className="px-5 py-3">Job ID</th><th className="px-5 py-3">Segment</th><th className="px-5 py-3 text-right">Records</th><th className="px-5 py-3">Status</th><th className="px-5 py-3">Uploaded</th><th className="px-5 py-3">By</th></tr></thead>
            <tbody>
              {filtered.map(job => (
                <tr key={job.jobId} className="t-row">
                  <td className="px-5 py-3"><Link href={`/jobs/${job.jobId}`} title={job.fileName} className="text-sm font-semibold text-slate-800 hover:text-[#003ea8] truncate block max-w-[200px]">{job.fileName}</Link></td>
                  <td className="px-5 py-3 mono text-xs text-slate-600" title={job.jobId}>{job.jobId.slice(0, 8)}…</td>
                  <td className="px-5 py-3"><span className="seg-chip">{job.segmentType ?? "—"}</span></td>
                  <td className="px-5 py-3 text-right mono text-xs text-slate-700">{job.totalRecords?.toLocaleString() ?? "—"}</td>
                  <td className="px-5 py-3"><span className={cn("pill", statusPill[job.status] ?? "pill-neu")}><span className={cn("dot", statusDot[job.status] ?? "dot-info")} />{{VALIDATING:"Validating",SPLITTING:"Reading file",PROCESSING:"Generating PDFs",EMAILING:"Sending emails",COMPLETED:"Completed",FAILED:"Failed",PARTIAL:"Completed with issues"}[job.status] ?? job.status}</span></td>
                  <td className="px-5 py-3 text-xs text-slate-500">{formatDistanceToNow(toUtcDate(job.uploadedAt) ?? new Date(), { addSuffix: true })}</td>
                  <td className="px-5 py-3 text-xs text-slate-500">{job.uploadedBy?.name ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-slate-100">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="px-3 py-1.5 text-sm text-slate-600 hover:text-[#00174b] disabled:opacity-40 flex items-center gap-1">
              <span className="material-symbols-outlined text-base">chevron_left</span>Prev
            </button>
            <span className="text-xs text-slate-500">Page {page + 1} of {totalPages}</span>
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="px-3 py-1.5 text-sm text-slate-600 hover:text-[#00174b] disabled:opacity-40 flex items-center gap-1">
              Next<span className="material-symbols-outlined text-base">chevron_right</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
