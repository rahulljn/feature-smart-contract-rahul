"use client";

import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { jobsApi } from "@/lib/api";
import type { Job, JobCustomer } from "@/types";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";
import { toast } from "sonner";

const TABS = ["All", "PDF failures", "Email failures", "Bounces", "Complaints"];

const normaliseCode = (code: string) => code?.split("/")[0] ?? code;
const PDF_LABEL: Record<string, string> = { PENDING: "Pending", GENERATED: "Ready", FAILED: "Failed" };
const EMAIL_LABEL: Record<string, string> = { PENDING: "Pending", SENT: "Dispatched", DELIVERED: "Delivered", BOUNCED: "Bounced", FAILED: "Failed", SKIPPED: "Failed" };

export default function ExceptionsPage() {
  const [tab, setTab] = useState("All");
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

  // Auto-select first eligible job when jobs load
  useEffect(() => {
    if (!selectedJob && jobs.length > 0) {
      setSelectedJob(jobs[0].jobId);
    }
  }, [jobs, selectedJob]);

  const { data: custRes, isLoading: loadingCust } = useQuery({
    queryKey: ["exceptions", selectedJob],
    queryFn: async () => {
      // Fetch ALL customers across all pages to get accurate exception counts
      const firstPage = await jobsApi.customers(selectedJob, 0, 200);
      const totalPg = firstPage.data?.data?.totalPages ?? 1;
      let allContent = [...(firstPage.data?.data?.content ?? [])];
      for (let p = 1; p < totalPg; p++) {
        const res = await jobsApi.customers(selectedJob, p, 200);
        allContent = [...allContent, ...(res.data?.data?.content ?? [])];
      }
      return allContent;
    },
    enabled: !!selectedJob,
  });

  const allCustomers: JobCustomer[] = Array.isArray(custRes) ? custRes : [];

  // Filter to only exceptions (failed/bounced)
  const exceptionCustomers = allCustomers.filter(
    (c: JobCustomer) => c.pdfStatus === "FAILED" || c.emailStatus === "FAILED" || c.emailStatus === "BOUNCED"
  );

  // Apply tab filter
  const customers = tab === "All" ? exceptionCustomers
    : tab === "PDF failures" ? exceptionCustomers.filter(c => c.pdfStatus === "FAILED")
    : tab === "Email failures" ? exceptionCustomers.filter(c => c.emailStatus === "FAILED")
    : tab === "Bounces" ? exceptionCustomers.filter(c => c.emailStatus === "BOUNCED")
    : tab === "Complaints" ? exceptionCustomers.filter(c => c.emailStatus === "BOUNCED" && c.bounceType?.toLowerCase().includes("complaint"))
    : exceptionCustomers;

  const { mutate: resend, isPending: resending } = useMutation({
    mutationFn: (partyCode: string) => jobsApi.resend(selectedJob, partyCode),
    onSuccess: () => { toast.success("Resend queued"); qc.invalidateQueries({ queryKey: ["exceptions"] }); },
  });

  const { mutate: bulkResend, isPending: bulkResending } = useMutation({
    mutationFn: () => jobsApi.bulkResend(selectedJob),
    onSuccess: () => { toast.success("Bulk resend queued for " + exceptionCustomers.length + " records"); qc.invalidateQueries({ queryKey: ["exceptions"] }); },
  });

  // Paginate filtered results client-side
  const pageSize = 50;
  const totalPages = Math.ceil(customers.length / pageSize);
  const pagedCustomers = customers.slice(page * pageSize, (page + 1) * pageSize);

  // Summary stats
  const pdfFailures = exceptionCustomers.filter(c => c.pdfStatus === "FAILED").length;
  const emailFailures = exceptionCustomers.filter(c => c.emailStatus === "FAILED").length;
  const bounces = exceptionCustomers.filter(c => c.emailStatus === "BOUNCED").length;
  const totalExceptions = exceptionCustomers.length;

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Exceptions</h2>
          <p className="text-slate-500 text-sm mt-1">Records where PDF generation or email delivery failed. Select a run to view details and retry individual or all failed records.</p>
        </div>
        {canResend && exceptionCustomers.length > 0 && (
          <button onClick={() => bulkResend()} disabled={bulkResending} className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50">
            {bulkResending ? <Loader2 size={14} className="animate-spin" /> : <span className="material-symbols-outlined text-base">forward_to_inbox</span>}
            Resend all ({exceptionCustomers.length})
          </button>
        )}
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Total exceptions</div><div className="p-1.5 rounded-lg bg-rose-50"><span className="material-symbols-outlined text-rose-600 text-base">error</span></div></div><div className="text-2xl font-extrabold mono text-rose-600 mt-1">{totalExceptions}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">PDF failures</div><span className="dot dot-err" /></div><div className="text-2xl font-extrabold mono text-amber-600 mt-1">{pdfFailures}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Email failures</div><span className="dot dot-err" /></div><div className="text-2xl font-extrabold mono text-orange-600 mt-1">{emailFailures}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Bounces</div><span className="dot dot-warn" /></div><div className="text-2xl font-extrabold mono text-amber-500 mt-1">{bounces}</div></div>
      </div>

      {/* Job selector + tab filter */}
      <div className="card overflow-hidden">
        <div className="flex flex-wrap items-center gap-3 px-5 py-3 border-b border-slate-100 bg-slate-50/70">
          <select
            value={selectedJob}
            onChange={e => { setSelectedJob(e.target.value); setPage(0); }}
            className="px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-sm font-medium"
          >
            <option value="">Select a job...</option>
            {jobs.map(j => <option key={j.jobId} value={j.jobId}>{j.fileName} · {j.jobId.slice(0, 8)}…</option>)}
          </select>
          <div className="h-5 w-px bg-slate-200" />
          {TABS.map(t => (
            <button key={t} onClick={() => setTab(t)} className={cn("tab-btn", tab === t && "active")}>{t}</button>
          ))}
        </div>

        {!selectedJob ? (
          <div className="text-center text-slate-500 py-12 text-sm">Select a job to view exceptions</div>
        ) : (loadingJobs || loadingCust) ? (
          <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
        ) : customers.length === 0 ? (
          <div className="text-center text-slate-500 py-12 text-sm">{tab === "All" ? "No exceptions for this run" : `No ${tab.toLowerCase()} found for this run`}</div>
        ) : (
          <table className="w-full text-left">
            <thead><tr className="t-hd"><th className="px-5 py-3">Client Code</th><th className="px-5 py-3">Email</th><th className="px-5 py-3">PDF Status</th><th className="px-5 py-3">Email Status</th><th className="px-5 py-3">Bounce</th><th className="px-5 py-3"></th></tr></thead>
            <tbody>
              {pagedCustomers.map(c => (
                <tr key={c.partyCode} className="t-row">
                  <td className="px-5 py-3 mono text-xs font-semibold text-slate-800">{normaliseCode(c.partyCode)}</td>
                  <td className="px-5 py-3 text-xs text-slate-600 truncate max-w-[200px]">{c.email ?? "—"}</td>
                  <td className="px-5 py-3"><span className={cn("pill", c.pdfStatus === "FAILED" ? "pill-err" : "pill-ok")}>{PDF_LABEL[c.pdfStatus] ?? c.pdfStatus}</span></td>
                  <td className="px-5 py-3"><span className={cn("pill", c.emailStatus === "FAILED" || c.emailStatus === "BOUNCED" ? "pill-err" : "pill-ok")}>{EMAIL_LABEL[c.emailStatus] ?? c.emailStatus}</span></td>
                  <td className="px-5 py-3 text-xs text-slate-500">{c.bounceType ? c.bounceType.toLowerCase().includes("permanent") ? "Permanent" : c.bounceType.toLowerCase().includes("transient") ? "Temporary" : c.bounceType : "—"}</td>
                  <td className="px-5 py-3">
                    {canResend && (
                      <button onClick={() => resend(normaliseCode(c.partyCode))} disabled={resending} className="text-[11px] font-bold text-[#003ea8] hover:underline flex items-center gap-1">
                        <span className="material-symbols-outlined text-sm">forward_to_inbox</span>Resend
                      </button>
                    )}
                  </td>
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
