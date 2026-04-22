"use client";

import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { jobsApi } from "@/lib/api";
import type { Job, JobCustomer } from "@/types";
import { Loader2 } from "lucide-react";
import { cn, fmtDateTime } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";
import { toast } from "sonner";

type TabKey = "ALL" | "PDF" | "EMAIL" | "BOUNCE" | "SKIPPED";

const TABS: { label: string; key: TabKey }[] = [
  { label: "All", key: "ALL" },
  { label: "PDF failures", key: "PDF" },
  { label: "Email failures", key: "EMAIL" },
  { label: "Bounced", key: "BOUNCE" },
  { label: "Skipped", key: "SKIPPED" },
];

const normaliseCode = (code: string) => code?.split("/")[0] ?? code;

function exceptionType(c: JobCustomer): string {
  if (c.pdfStatus === "FAILED") return "PDF Failed";
  if (c.emailStatus === "BOUNCED") return "Bounced";
  if (c.emailStatus === "SKIPPED") return "Skipped";
  if (c.emailStatus === "FAILED") return "Email Failed";
  return "—";
}

function exceptionTypePill(c: JobCustomer): string {
  if (c.pdfStatus === "FAILED") return "pill-warn";
  if (c.emailStatus === "BOUNCED") return "pill-err";
  if (c.emailStatus === "SKIPPED") return "pill-neu";
  if (c.emailStatus === "FAILED") return "pill-err";
  return "pill-neu";
}

function exceptionReason(c: JobCustomer): string {
  if (c.pdfStatus === "FAILED") return "PDF generation failed";
  if (c.emailStatus === "BOUNCED") return c.bounceReason ?? c.bounceType ?? "Bounced";
  if (c.emailStatus === "SKIPPED") return "No email address on file";
  if (c.emailStatus === "FAILED") return "Email delivery failed";
  return "—";
}

function bounceCategory(c: JobCustomer): string {
  if (c.emailStatus !== "BOUNCED") return "—";
  const t = (c.bounceType ?? "").toLowerCase();
  if (t.includes("permanent")) return "Permanent";
  if (t.includes("transient")) return "Transient";
  return c.bounceType ?? "—";
}

function statusBadge(status: string) {
  const map: Record<string, string> = {
    COMPLETED: "pill-ok", PARTIAL: "pill-warn", FAILED: "pill-err",
    PROCESSING: "pill-info", EMAILING: "pill-info", SPLITTING: "pill-neu",
  };
  return map[status] ?? "pill-neu";
}

export default function ExceptionsPage() {
  const [tab, setTab] = useState<TabKey>("ALL");
  const [page, setPage] = useState(0);
  const [selectedJob, setSelectedJob] = useState("");
  const user = useAuthStore(s => s.user);
  const qc = useQueryClient();
  const canResend = user?.role === "ADMIN" || user?.role === "OPS_MANAGER";

  const { data: jobsRes, isLoading: loadingJobs } = useQuery({
    queryKey: ["jobs-for-exceptions"],
    queryFn: () => jobsApi.list(0, 50),
  });
  const allJobs: Job[] = jobsRes?.data?.data?.content ?? [];
  const jobs: Job[] = allJobs.filter((j: Job) => ["FAILED","PARTIAL","COMPLETED"].includes(j.status));

  useEffect(() => {
    if (!selectedJob && jobs.length > 0) {
      setSelectedJob(jobs[0].jobId);
    }
  }, [jobs, selectedJob]);

  const selectedJobObj = jobs.find(j => j.jobId === selectedJob);

  // Server-side paginated exceptions
  const { data: exceptionsRes, isLoading: loadingExc } = useQuery({
    queryKey: ["exceptions", selectedJob, tab, page],
    queryFn: () => jobsApi.exceptions(selectedJob, tab, page, 50),
    enabled: !!selectedJob,
  });
  const excPage = exceptionsRes?.data?.data;
  const customers: JobCustomer[] = excPage?.content ?? [];
  const totalPages: number = excPage?.totalPages ?? 0;
  const totalElements: number = excPage?.totalElements ?? 0;

  const { mutate: resend, isPending: resending } = useMutation({
    mutationFn: (partyCode: string) => jobsApi.resend(selectedJob, partyCode),
    onSuccess: () => { toast.success("Resend queued"); qc.invalidateQueries({ queryKey: ["exceptions"] }); },
  });

  const { mutate: bulkResend, isPending: bulkResending } = useMutation({
    mutationFn: () => jobsApi.bulkResend(selectedJob),
    onSuccess: () => { toast.success("Bulk resend queued"); qc.invalidateQueries({ queryKey: ["exceptions"] }); },
  });

  const handleTabChange = (key: TabKey) => { setTab(key); setPage(0); };
  const handleJobChange = (jobId: string) => { setSelectedJob(jobId); setTab("ALL"); setPage(0); };

  // Summary from job object
  const pdfFailures = selectedJobObj?.failureCount ?? 0;
  const emailFailures = selectedJobObj?.emailFailedCount ?? 0;
  const bounces = selectedJobObj?.bounceCount ?? 0;
  const skipped = selectedJobObj?.emailSkippedCount ?? 0;

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Exceptions</h2>
          <p className="text-slate-500 text-sm mt-1">Records where PDF generation or email delivery failed. Select a run to view details and retry.</p>
        </div>
        {canResend && selectedJob && (
          <button onClick={() => bulkResend()} disabled={bulkResending} className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50">
            {bulkResending ? <Loader2 size={14} className="animate-spin" /> : <span className="material-symbols-outlined text-base">forward_to_inbox</span>}
            Resend all failed
          </button>
        )}
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">PDF failures</div><span className="dot dot-warn" /></div><div className="text-2xl font-extrabold mono text-amber-600 mt-1">{pdfFailures}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Email failures</div><span className="dot dot-err" /></div><div className="text-2xl font-extrabold mono text-orange-600 mt-1">{emailFailures}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Bounces</div><span className="dot dot-warn" /></div><div className="text-2xl font-extrabold mono text-amber-500 mt-1">{bounces}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Skipped</div><span className="dot" /></div><div className="text-2xl font-extrabold mono text-slate-500 mt-1">{skipped}</div></div>
      </div>

      <div className="card overflow-hidden">
        {/* Job selector */}
        <div className="px-5 py-3 border-b border-slate-100 bg-slate-50/70">
          <select
            value={selectedJob}
            onChange={e => handleJobChange(e.target.value)}
            className="px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-sm font-medium"
          >
            <option value="">Select a job...</option>
            {jobs.map(j => <option key={j.jobId} value={j.jobId}>{j.fileName} · {j.jobId.slice(0, 8)}…</option>)}
          </select>
        </div>

        {/* Job header */}
        {selectedJobObj && (
          <div className="px-5 py-3 border-b border-slate-100 flex flex-wrap items-center gap-4 text-xs text-slate-600 bg-white">
            <div className="flex items-center gap-1.5 font-semibold text-slate-800">
              <span className="material-symbols-outlined text-sm text-slate-400">description</span>
              {selectedJobObj.fileName}
            </div>
            <div className="text-slate-400">Job ID: <span className="mono text-slate-600">{selectedJobObj.jobId.slice(0, 8)}…</span></div>
            {selectedJobObj.tradeDate && <div className="text-slate-400">Trade date: <span className="text-slate-600">{selectedJobObj.tradeDate}</span></div>}
            <span className={cn("pill", statusBadge(selectedJobObj.status))}>{selectedJobObj.status}</span>
            {selectedJobObj.totalRecords && <div className="text-slate-400">{selectedJobObj.totalRecords.toLocaleString()} records</div>}
          </div>
        )}

        {/* Tabs */}
        <div className="flex flex-wrap gap-1 px-5 py-2.5 border-b border-slate-100 bg-slate-50/40">
          {TABS.map(t => (
            <button key={t.key} onClick={() => handleTabChange(t.key)} className={cn("tab-btn", tab === t.key && "active")}>{t.label}</button>
          ))}
        </div>

        {!selectedJob ? (
          <div className="text-center text-slate-500 py-12 text-sm">Select a job to view exceptions</div>
        ) : (loadingJobs || loadingExc) ? (
          <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
        ) : customers.length === 0 ? (
          <div className="text-center text-slate-500 py-12 text-sm">{tab === "ALL" ? "No exceptions for this run" : `No ${TABS.find(t => t.key === tab)?.label.toLowerCase()} found`}</div>
        ) : (
          <table className="w-full text-left">
            <thead>
              <tr className="t-hd">
                <th className="px-5 py-3">Party Code</th>
                <th className="px-5 py-3">Email</th>
                <th className="px-5 py-3">Type</th>
                <th className="px-5 py-3">Reason</th>
                <th className="px-5 py-3">Bounce Category</th>
                <th className="px-5 py-3">Time</th>
                {canResend && <th className="px-5 py-3"></th>}
              </tr>
            </thead>
            <tbody>
              {customers.map((c, i) => (
                <tr key={`${c.partyCode}-${i}`} className="t-row">
                  <td className="px-5 py-3 mono text-xs font-bold text-slate-800">{normaliseCode(c.partyCode)}</td>
                  <td className="px-5 py-3 text-xs text-slate-600 truncate max-w-[180px]">{c.email ?? "—"}</td>
                  <td className="px-5 py-3"><span className={cn("pill", exceptionTypePill(c))}>{exceptionType(c)}</span></td>
                  <td className="px-5 py-3 text-xs text-slate-500 max-w-[200px] truncate" title={exceptionReason(c)}>{exceptionReason(c)}</td>
                  <td className="px-5 py-3 text-xs text-slate-500">{bounceCategory(c)}</td>
                  <td className="px-5 py-3 text-xs text-slate-500 mono">
                    {fmtDateTime(c.bouncedAt ?? c.emailSentAt)}
                  </td>
                  {canResend && (
                    <td className="px-5 py-3">
                      <button onClick={() => resend(normaliseCode(c.partyCode))} disabled={resending} className="text-[11px] font-bold text-[#003ea8] hover:underline flex items-center gap-1">
                        <span className="material-symbols-outlined text-sm">forward_to_inbox</span>Resend
                      </button>
                    </td>
                  )}
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
            <span className="text-xs text-slate-500">Page {page + 1} of {totalPages} · {totalElements} total</span>
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="px-3 py-1.5 text-sm text-slate-600 hover:text-[#00174b] disabled:opacity-40 flex items-center gap-1">
              Next<span className="material-symbols-outlined text-base">chevron_right</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
