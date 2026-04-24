"use client";

import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { jobsApi, lambdaExceptionsApi } from "@/lib/api";
import type { Job, JobCustomer, LambdaException } from "@/types";
import { Loader2 } from "lucide-react";
import { cn, fmtDateTime } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";
import { toast } from "sonner";

type TabKey = "SPLIT" | "INVOKE" | "GETJSON" | "PDF" | "EMAIL" | "PULLBOUNCE" | "PULLDELIVERY";

interface TabConfig {
  label: string;
  key: TabKey;
  dbType: string | null;
  lambdaName: string;
}

const TABS: TabConfig[] = [
  { label: "Split",         key: "SPLIT",        dbType: null,      lambdaName: "Split"        },
  { label: "Invoke",        key: "INVOKE",        dbType: null,      lambdaName: "Invoke"       },
  { label: "GetJson",       key: "GETJSON",       dbType: null,      lambdaName: "GetJson"      },
  { label: "PDF",           key: "PDF",           dbType: "PDF",     lambdaName: "PDF"          },
  { label: "Email",         key: "EMAIL",         dbType: "EMAIL",   lambdaName: "Email"        },
  { label: "Pull Bounce",   key: "PULLBOUNCE",    dbType: "BOUNCE",  lambdaName: "PullBounce"   },
  { label: "Pull Delivery", key: "PULLDELIVERY",  dbType: null,      lambdaName: "PullDelivery" },
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
  if (c.emailStatus === "FAILED") return c.bounceReason ?? "Email delivery failed";
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


function ExpandableText({ text, limit = 80 }: { text: string; limit?: number }) {
  const [expanded, setExpanded] = useState(false);
  if (text.length <= limit) return <span>{text}</span>;
  return (
    <span>
      {expanded ? text : `${text.slice(0, limit)}…`}
      {" "}
      <button
        onClick={() => setExpanded(p => !p)}
        className="text-[10px] text-[#003ea8] hover:underline"
      >
        {expanded ? "Show less" : "Show more"}
      </button>
    </span>
  );
}

function LambdaExceptionEntry({ ex }: { ex: LambdaException }) {
  const [expanded, setExpanded] = useState(false);
  const [expandedMsg, setExpandedMsg] = useState(false);
  return (
    <div className="bg-red-50 border border-red-100 rounded-lg px-3 py-2 text-xs">
      <div className="flex flex-wrap items-center gap-2 mb-1">
        {ex.errorType && (
          <span className="bg-red-100 text-red-700 text-[10px] font-bold px-1.5 py-0.5 rounded-full">
            {ex.errorType}
          </span>
        )}
        {ex.recordId && (
          <span className="bg-slate-100 text-slate-600 text-[10px] mono px-1.5 py-0.5 rounded-full">
            {ex.recordId}
          </span>
        )}
        <span className="mono text-slate-400 ml-auto">{fmtDateTime(ex.occurredAt)}</span>
      </div>
      {ex.errorMessage && (
        <div className="mb-1">
          <p className={cn("text-red-700", !expandedMsg && "line-clamp-3")}>
            {ex.errorMessage}
          </p>
          {ex.errorMessage.length > 120 && (
            <button
              onClick={() => setExpandedMsg(p => !p)}
              className="text-[10px] text-[#003ea8] hover:underline mt-0.5"
            >
              {expandedMsg ? "Show less" : "Show more"}
            </button>
          )}
        </div>
      )}
      {ex.stackTrace && (
        <>
          <button
            onClick={() => setExpanded(p => !p)}
            className="text-slate-500 hover:text-slate-700 flex items-center gap-1 text-[10px] mb-1"
          >
            <span className="material-symbols-outlined text-xs">{expanded ? "expand_less" : "expand_more"}</span>
            {expanded ? "Hide" : "Show"} stack trace
          </button>
          {expanded && (
            <pre className="text-red-700 whitespace-pre-wrap break-all leading-relaxed font-mono text-[11px] max-h-64 overflow-y-auto">
              {ex.stackTrace}
            </pre>
          )}
        </>
      )}
    </div>
  );
}

export default function ExceptionsPage() {
  const [tab, setTab] = useState<TabKey>("SPLIT");
  const [page, setPage] = useState(0);
  const [selectedJob, setSelectedJob] = useState("");
  const user = useAuthStore(s => s.user);

  const currentTabConfig = TABS.find(t => t.key === tab) ?? TABS[0];
  const hasDbRecords = currentTabConfig.dbType !== null;

  const { data: jobsRes, isLoading: loadingJobs } = useQuery({
    queryKey: ["jobs-for-exceptions"],
    queryFn: () => jobsApi.list(0, 50),
  });
  const allJobs: Job[] = jobsRes?.data?.data?.content ?? [];
  const jobs: Job[] = allJobs.filter((j: Job) => ["FAILED","PARTIAL","COMPLETED","PROCESSING","EMAILING","SPLITTING"].includes(j.status));

  useEffect(() => {
    if (!selectedJob && jobs.length > 0) {
      setSelectedJob(jobs[0].jobId);
    }
  }, [jobs, selectedJob]);

  const selectedJobObj = jobs.find(j => j.jobId === selectedJob);

  const { data: countsRes } = useQuery({
    queryKey: ["exception-counts", selectedJob],
    queryFn: () => jobsApi.exceptionCounts(selectedJob),
    enabled: !!selectedJob,
    staleTime: 10_000,
  });
  const invalidRecords = countsRes?.data?.data?.invalidRecords ?? 0;
  const pdfFailures    = countsRes?.data?.data?.pdfFailed      ?? 0;
  const emailFailures  = countsRes?.data?.data?.emailFailed    ?? 0;
  const hardBounces    = countsRes?.data?.data?.hardBounced    ?? 0;
  const softBounces    = countsRes?.data?.data?.softBounced    ?? 0;
  const skipped        = countsRes?.data?.data?.skipped        ?? 0;

  const { data: exceptionsRes, isLoading: loadingExc } = useQuery({
    queryKey: ["exceptions", selectedJob, currentTabConfig.dbType, page],
    queryFn: () => jobsApi.exceptions(selectedJob, currentTabConfig.dbType!, page, 50),
    enabled: !!selectedJob && hasDbRecords,
  });
  const excPage = exceptionsRes?.data?.data;
  const customers: JobCustomer[] = excPage?.content ?? [];
  const totalPages: number = excPage?.totalPages ?? 0;
  const totalElements: number = excPage?.totalElements ?? 0;

  const {
    data: lambdaExcData,
    isLoading: loadingLambdaExc,
    isError: lambdaExcError,
    refetch: refetchLambdaExc,
  } = useQuery({
    queryKey: ["lambda-exceptions", selectedJob],
    queryFn: () => lambdaExceptionsApi.list(selectedJob),
    enabled: !!selectedJob,
  });
  const allLambdaExceptions: LambdaException[] = lambdaExcData?.data?.data ?? [];
  const tabLambdaExceptions = allLambdaExceptions.filter(
    e => e.lambdaName === currentTabConfig.lambdaName
  );

  const handleTabChange = (key: TabKey) => { setTab(key); setPage(0); };
  const handleJobChange = (jobId: string) => { setSelectedJob(jobId); setTab("SPLIT"); setPage(0); };

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Exceptions</h2>
          <p className="text-slate-500 text-sm mt-1">Records where PDF generation or email delivery failed. Select a run to view details and retry.</p>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Invalid records</div><span className="dot" /></div><div className="text-2xl font-extrabold mono text-slate-700 mt-1">{invalidRecords}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">PDF failures</div><span className="dot dot-warn" /></div><div className="text-2xl font-extrabold mono text-red-700 mt-1">{pdfFailures}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Email failures</div><span className="dot dot-err" /></div><div className="text-2xl font-extrabold mono text-orange-600 mt-1">{emailFailures}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Hard bounces</div><span className="dot dot-err" /></div><div className="text-2xl font-extrabold mono text-red-600 mt-1">{hardBounces}</div></div>
        <div className="card p-4"><div className="flex items-center justify-between"><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Soft bounces</div><span className="dot dot-warn" /></div><div className="text-2xl font-extrabold mono text-amber-600 mt-1">{softBounces}</div></div>
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
            {jobs.map(j => <option key={j.jobId} value={j.jobId} title={j.jobId}>{j.fileName} · {j.jobId.slice(0, 8)}…</option>)}
          </select>
        </div>

        {/* Job header */}
        {selectedJobObj && (
          <div className="px-5 py-3 border-b border-slate-100 flex flex-wrap items-center gap-4 text-xs text-slate-600 bg-white">
            <div className="flex items-center gap-1.5 font-semibold text-slate-800">
              <span className="material-symbols-outlined text-sm text-slate-400">description</span>
              {selectedJobObj.fileName}
            </div>
            <div className="flex items-center gap-1 text-slate-400">
              Job ID:&nbsp;
              <span className="mono text-slate-600" title={selectedJobObj.jobId}>
                {selectedJobObj.jobId.slice(0, 8)}…
              </span>
              <button
                onClick={() => navigator.clipboard.writeText(selectedJobObj.jobId).then(() => toast.success("Copied"))}
                title={selectedJobObj.jobId}
                className="text-slate-400 hover:text-[#00174b] transition-colors leading-none"
              >
                <span className="material-symbols-outlined" style={{ fontSize: 14 }}>content_copy</span>
              </button>
            </div>
            {selectedJobObj.tradeDate && <div className="text-slate-400">Trade date: <span className="text-slate-600">{selectedJobObj.tradeDate}</span></div>}
            <span className={cn("pill", statusBadge(selectedJobObj.status))}>{selectedJobObj.status}</span>
            {selectedJobObj.totalRecords && <div className="text-slate-400">{selectedJobObj.totalRecords.toLocaleString()} records</div>}
          </div>
        )}

        {/* Lambda tabs */}
        <div className="flex flex-wrap gap-1 px-5 py-2.5 border-b border-slate-100 bg-slate-50/40">
          {TABS.map(t => (
            <button key={t.key} onClick={() => handleTabChange(t.key)} className={cn("tab-btn", tab === t.key && "active")}>{t.label}</button>
          ))}
        </div>

        {!selectedJob ? (
          <div className="text-center text-slate-500 py-12 text-sm">Select a job to view exceptions</div>
        ) : (
          <>
            {/* DB records table — only for tabs that have customer-level data */}
            {hasDbRecords && (
              <>
                {(loadingJobs || loadingExc) ? (
                  <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
                ) : customers.length === 0 ? (
                  <div className="text-center text-slate-500 py-10 text-sm">
                    No {currentTabConfig.label.toLowerCase()} exceptions for this run
                  </div>
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
                      </tr>
                    </thead>
                    <tbody>
                      {customers.map((c, i) => (
                        <tr key={`${c.partyCode}-${i}`} className="t-row">
                          <td className="px-5 py-3 mono text-xs font-bold text-slate-800">{normaliseCode(c.partyCode)}</td>
                          <td className="px-5 py-3 text-xs text-slate-600 truncate max-w-[180px]" title={c.email ?? ""}>{c.email ?? "—"}</td>
                          <td className="px-5 py-3"><span className={cn("pill", exceptionTypePill(c))}>{exceptionType(c)}</span></td>
                          <td className="px-5 py-3 text-xs text-slate-500 max-w-[240px]"><ExpandableText text={exceptionReason(c)} /></td>
                          <td className="px-5 py-3 text-xs text-slate-500"><ExpandableText text={bounceCategory(c)} /></td>
                          <td className="px-5 py-3 text-xs text-slate-500 mono">
                            {fmtDateTime(c.bouncedAt ?? c.emailSentAt)}
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
                    <span className="text-xs text-slate-500">Page {page + 1} of {totalPages} · {totalElements} total</span>
                    <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="px-3 py-1.5 text-sm text-slate-600 hover:text-[#00174b] disabled:opacity-40 flex items-center gap-1">
                      Next<span className="material-symbols-outlined text-base">chevron_right</span>
                    </button>
                  </div>
                )}
              </>
            )}

            {/* Lambda Exceptions (SQS-sourced) */}
            <div className="border-t border-slate-100">
              <div className="w-full flex items-center justify-between px-5 py-2.5 text-xs text-slate-500 bg-slate-50/60">
                <span className="flex items-center gap-1.5 font-semibold">
                  <span className="material-symbols-outlined text-sm">bug_report</span>
                  Lambda Exceptions — <span className="text-slate-700">{currentTabConfig.label}</span>
                </span>
                <button
                  onClick={() => refetchLambdaExc()}
                  disabled={loadingLambdaExc}
                  className="flex items-center gap-1 text-slate-500 hover:text-[#00174b] disabled:opacity-50"
                >
                  {loadingLambdaExc
                    ? <Loader2 size={12} className="animate-spin" />
                    : <span className="material-symbols-outlined text-sm">refresh</span>}
                  Refresh
                </button>
              </div>

              <div className="px-5 pb-4 pt-2">
                {lambdaExcError ? (
                  <div className="flex items-center gap-2 text-xs text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                    <span className="material-symbols-outlined text-sm">error</span>
                    Failed to fetch Lambda exceptions. Check API connectivity.
                  </div>
                ) : loadingLambdaExc ? (
                  <div className="flex items-center gap-2 text-xs text-slate-400 py-3">
                    <Loader2 size={12} className="animate-spin" /> Fetching Lambda exceptions…
                  </div>
                ) : tabLambdaExceptions.length === 0 ? (
                  <p className="text-xs text-slate-400 py-3 flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-sm text-emerald-500">check_circle</span>
                    No exceptions recorded for <span className="font-semibold">{currentTabConfig.label}</span> Lambda.
                  </p>
                ) : (
                  <div className="space-y-2">
                    {tabLambdaExceptions.map(ex => (
                      <LambdaExceptionEntry key={ex.id} ex={ex} />
                    ))}
                  </div>
                )}
              </div>
            </div>


          </>
        )}
      </div>
    </div>
  );
}
