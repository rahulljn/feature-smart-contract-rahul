"use client";

import { use, useEffect, useRef, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { jobsApi, pipelineApi } from "@/lib/api";
import { downloadCsv } from "@/lib/export";
import type { Job, JobCustomer, PipelineEvent } from "@/types";
import { Loader2 } from "lucide-react";
import { cn, fmtDateTime, fmtTime } from "@/lib/utils";
import { toast } from "sonner";
import { useAuthStore } from "@/store/auth";
import Link from "next/link";

const STATUS_LABEL: Record<string, string> = {
  VALIDATING: "Validating",
  SPLITTING:  "Reading file",
  PROCESSING: "Generating PDFs",
  EMAILING:   "Sending emails",
  COMPLETED:  "Completed",
  FAILED:     "Failed",
  PARTIAL:    "Completed with issues",
};

const PDF_LABEL: Record<string, string> = {
  PENDING:   "Pending",
  GENERATED: "Ready",
  FAILED:    "Failed",
};

const EMAIL_LABEL: Record<string, string> = {
  PENDING:   "Pending",
  SENT:      "Dispatched",
  DELIVERED: "Delivered",
  BOUNCED:   "Bounced",
  FAILED:    "Failed",
  SKIPPED:   "Failed",
};

const EVENT_LABEL: Record<string, string> = {
  JOB_REGISTERED:      "File registered — records counted",
  CUSTOMER_REGISTERED: "Customer record added to pipeline",
  PDF_TRIGGERED:       "PDF generation started",
  PDF_GENERATED:       "Contract note PDF created",
  PDF_FAILED:          "PDF generation failed",
  EMAIL_SENT:          "Email dispatched to client",
  EMAIL_FAILED:        "Email delivery failed",
  EMAIL_SKIPPED:       "Email delivery failed — no address on file",
  DELIVERY:            "Email confirmed received by client",
  BOUNCE:              "Email bounced — address rejected",
  COMPLAINT:           "Spam report received",
  SPLIT_COMPLETE:      "File reading complete",
  SPLIT_PROGRESS:      "Reading file…",
  RESEND_TRIGGERED:    "Resend queued",
};

const normaliseCode = (code?: string | null) => code?.split("/")[0] ?? code ?? "";

function bounceLabel(type?: string | null) {
  if (!type) return "Bounced";
  if (type.toLowerCase().includes("permanent")) return "Permanent — address doesn't exist";
  if (type.toLowerCase().includes("transient")) return "Temporary — try again later";
  if (type.toLowerCase().includes("complaint")) return "Spam report";
  return type;
}

export default function JobDetailPage({ params }: { params: Promise<{ jobId: string }> }) {
  const { jobId } = use(params);
  const qc = useQueryClient();
  const { user } = useAuthStore();
  const [activeTab, setActiveTab] = useState("ov");
  const [page, setPage] = useState(0);
  const [liveEvents, setLiveEvents] = useState<PipelineEvent[]>([]);
  const [splitProgress, setSplitProgress] = useState<number | null>(null);
  const sseRef = useRef<EventSource | null>(null);
  const invalidateTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const canResend = user?.role === "ADMIN" || user?.role === "OPS_MANAGER";

  const { data: jobRes } = useQuery({
    queryKey: ["job", jobId],
    queryFn: () => jobsApi.get(jobId),
  });

  const { data: custRes, isLoading: loadingCust } = useQuery({
    queryKey: ["job-customers", jobId, page],
    queryFn: () => jobsApi.customers(jobId, page, 50),
  });

  const { data: eventsRes } = useQuery({
    queryKey: ["pipeline-events", jobId],
    queryFn: () => pipelineApi.events(jobId),
  });

  const job: Job | undefined = jobRes?.data?.data;
  const customers: JobCustomer[] = custRes?.data?.data?.content ?? [];
  const _evRaw = eventsRes?.data?.data;
  const events: PipelineEvent[] = Array.isArray(_evRaw) ? _evRaw : [];
  const totalPages = custRes?.data?.data?.totalPages ?? 0;
  const totalElements = custRes?.data?.data?.totalElements ?? customers.length;

  useEffect(() => {
    if (!jobId) return;
    const token = user?.accessToken;
    const url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/jobs/${jobId}/stream`;
    const es = new EventSource(`${url}?token=${token}`);
    sseRef.current = es;
    es.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data);
        if (data.eventType === "SPLIT_PROGRESS" && data.payload?.customersFound) {
          setSplitProgress(Number(data.payload.customersFound));
          return;
        }
        setLiveEvents(prev => [{ ...data, eventId: Date.now() } as PipelineEvent, ...prev].slice(0, 100));
        if (!invalidateTimerRef.current) {
          qc.invalidateQueries({ queryKey: ["job", jobId] });
          qc.invalidateQueries({ queryKey: ["job-customers", jobId] });
          invalidateTimerRef.current = setTimeout(() => { invalidateTimerRef.current = null; }, 2000);
        }
      } catch { /* ignore */ }
    };
    return () => { es.close(); sseRef.current = null; if (invalidateTimerRef.current) clearTimeout(invalidateTimerRef.current); };
  }, [jobId, user?.accessToken, qc]);

  const { mutate: resend, isPending: resending } = useMutation({
    mutationFn: (partyCode: string) => jobsApi.resend(jobId, partyCode),
    onSuccess: (_, partyCode) => { toast.success(`Resend queued for ${partyCode}`); qc.invalidateQueries({ queryKey: ["job-customers", jobId] }); },
    onError: () => toast.error("Resend failed — please try again"),
  });

  const { mutate: bulkResend, isPending: bulkResending } = useMutation({
    mutationFn: () => jobsApi.bulkResend(jobId),
    onSuccess: (res) => toast.success(`${res.data?.data?.queued ?? 0} customers queued for resend`),
    onError: () => toast.error("Resend failed — please try again"),
  });

  if (!job) return <div className="flex justify-center py-16"><Loader2 className="animate-spin text-[#00174b]" /></div>;

  const exceptionCustomers = customers.filter(c => c.pdfStatus === "FAILED" || c.emailStatus === "FAILED");
  const bounceCustomers    = customers.filter(c => c.emailStatus === "BOUNCED");
  const exceptionCount     = exceptionCustomers.length;
  const bounceCount        = bounceCustomers.length;

  const tabs = [
    { id: "ov",   label: "Overview" },
    { id: "pipe", label: "Pipeline" },
    { id: "exc",  label: "Exceptions", badge: exceptionCount > 0 ? String(exceptionCount) : undefined },
    { id: "bnc",  label: "Bounces",    badge: bounceCount > 0    ? String(bounceCount)    : undefined },
    { id: "cli",  label: "All clients" },
  ];

  const statusPill = job.status === "COMPLETED" ? "pill-ok"
    : job.status === "FAILED"  ? "pill-err"
    : job.status === "PARTIAL" ? "pill-warn"
    : "pill-info";

  return (
    <div className="space-y-0">

      {/* ─── Sticky Header ─────────────────────────────────────────── */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-10">
        <div className="px-6 pt-5 pb-3 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 text-[11px] text-slate-500 mb-1">
              <Link href="/jobs" className="hover:text-[#003ea8]">Runs</Link>
              <span className="material-symbols-outlined text-sm">chevron_right</span>
              <span className="text-slate-700 font-semibold">Run detail</span>
            </div>
            <div className="flex items-center gap-3 flex-wrap">
              <h3 className="text-lg font-extrabold text-slate-900 headline">{job.fileName}</h3>
              <span className={cn("pill", statusPill)}>
                {["PROCESSING","SPLITTING","EMAILING"].includes(job.status) && <span className="dot dot-run" />}
                {STATUS_LABEL[job.status] ?? job.status}
              </span>
            </div>
            <div className="text-[11px] text-slate-500 mt-1">
              {(job.totalRecords ?? 0).toLocaleString()} records · {job.segmentType ?? "—"} · {job.uploadedBy?.name ?? "—"} · {job.uploadedAt ? new Date(job.uploadedAt).toLocaleString("en-GB", { timeZone: "Asia/Kolkata", day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" }) + " IST" : "—"}
            </div>
          </div>
          <button
            onClick={() => { qc.invalidateQueries({ queryKey: ["job", jobId] }); qc.invalidateQueries({ queryKey: ["job-customers", jobId] }); toast.success("Refreshed"); }}
            className="p-2 text-slate-400 hover:text-[#00174b] hover:bg-slate-100 rounded-lg"
            title="Refresh"
          >
            <span className="material-symbols-outlined text-xl">refresh</span>
          </button>
        </div>
        <div className="px-6 flex gap-1 border-t border-slate-100">
          {tabs.map(t => (
            <button key={t.id} onClick={() => setActiveTab(t.id)} className={cn("drw-tab", activeTab === t.id && "active")}>
              {t.label}
              {t.badge && <span className="ml-1.5 text-[9px] bg-rose-100 text-rose-700 px-1.5 py-0.5 rounded-full font-bold">{t.badge}</span>}
            </button>
          ))}
        </div>
      </div>

      {/* ─── Tab 1: Overview ───────────────────────────────────────── */}
      {activeTab === "ov" && (
        <div className="p-6 space-y-5">

          {/* 6 metric cards */}
          {(() => {
            const total          = job.totalRecords ?? 0;
            const pdfs           = job.pdfGeneratedCount ?? 0;
            const sent           = job.emailSentCount ?? 0;
            const confirmed      = job.emailDeliveredCount ?? 0;
            const failed         = job.failureCount ?? 0;
            const deliveryFailed = (job.bounceCount ?? 0) + (job.emailFailedCount ?? 0);
            return (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
                <div className="card p-4">
                  <div className="text-[10px] text-slate-500 uppercase font-bold tracking-wider mb-1">Total customers</div>
                  <div className="text-[26px] font-extrabold mono text-slate-900 leading-none">{total.toLocaleString()}</div>
                  <div className="text-[10px] text-slate-400 mt-1.5">Records in this run</div>
                </div>
                <div className="card p-4">
                  <div className="text-[10px] text-purple-600 uppercase font-bold tracking-wider mb-1">PDFs created</div>
                  <div className="text-[26px] font-extrabold mono text-purple-600 leading-none">{pdfs.toLocaleString()}</div>
                  <div className="text-[10px] text-slate-400 mt-1.5">{total > 0 ? Math.round(pdfs / total * 100) : 0}% · Contract notes ready</div>
                </div>
                <div className="card p-4">
                  <div className="text-[10px] text-emerald-600 uppercase font-bold tracking-wider mb-1">Emails sent</div>
                  <div className="text-[26px] font-extrabold mono text-emerald-600 leading-none">{sent.toLocaleString()}</div>
                  <div className="text-[10px] text-slate-400 mt-1.5">{total > 0 ? Math.round(sent / total * 100) : 0}% · {confirmed > 0 ? `${confirmed.toLocaleString()} confirmed` : "Dispatched to SES"}</div>
                </div>
                <div className="card p-4">
                  <div className="text-[10px] text-orange-600 uppercase font-bold tracking-wider mb-1">Delivery failed</div>
                  <div className="text-[26px] font-extrabold mono text-orange-600 leading-none">{deliveryFailed.toLocaleString()}</div>
                  <div className="text-[10px] text-slate-400 mt-1.5">No address · bounce · delivery errors</div>
                </div>
                <div className="card p-4">
                  <div className="text-[10px] text-rose-600 uppercase font-bold tracking-wider mb-1">Invalid records</div>
                  <div className="text-[26px] font-extrabold mono text-rose-600 leading-none">{failed.toLocaleString()}</div>
                  <div className="text-[10px] text-slate-400 mt-1.5">Could not generate contract note</div>
                </div>
              </div>
            );
          })()}

          {/* Live progress bar — only shown while actively running */}
          {["PROCESSING","SPLITTING","EMAILING"].includes(job.status) && (
            <div className="card p-4">
              <div className="flex items-center justify-between mb-2">
                <div className="text-[10px] text-slate-500 uppercase font-bold tracking-wider flex items-center gap-2">
                  <span className="dot dot-run" />Processing
                </div>
                <div className="text-sm font-bold mono text-slate-900">{job.progressPercent ?? 0}%</div>
              </div>
              <div className="h-2.5 bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full bg-[#00174b] rounded-full transition-all duration-700" style={{ width: `${job.progressPercent ?? 0}%` }} />
              </div>
              <div className="flex justify-between text-[10px] text-slate-400 mt-2">
                <span>{(job.pdfGeneratedCount ?? 0).toLocaleString()} PDFs created</span>
                <span>{(job.emailSentCount ?? 0).toLocaleString()} emails sent</span>
                <span>{(job.bounceCount ?? 0).toLocaleString()} bounced</span>
              </div>
            </div>
          )}

          {/* Run details */}
          <div className="card p-5">
            <div className="text-[10px] uppercase font-bold tracking-wider text-slate-500 mb-3">Run details</div>
            <div className="grid grid-cols-2 gap-x-8 gap-y-2.5 text-[12px]">
              {[
                { label: "Report type",  value: job.segmentType ?? "—" },
                { label: "Trade date",   value: job.tradeDate ?? "—" },
                { label: "Input file",   value: job.fileName ?? "—" },
                { label: "Uploaded by",  value: job.uploadedBy?.name ?? "—" },
                { label: "Uploaded at",  value: fmtDateTime(job.uploadedAt) },
              ].map(({ label, value }) => (
                <div key={label} className="flex justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">{label}</span>
                  <span className="font-semibold text-slate-800 mono text-[11px]">{value}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center gap-2 flex-wrap">
            {canResend && ["COMPLETED","PARTIAL"].includes(job.status) && (
              <button
                onClick={() => bulkResend()}
                disabled={bulkResending}
                className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50"
              >
                {bulkResending ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-base">replay</span>}
                Resend all failed &amp; bounced
              </button>
            )}
            <button
              onClick={() => {
                const blob = new Blob([JSON.stringify({ jobId, fileName: job.fileName, status: job.status, reportType: job.segmentType, totalRecords: job.totalRecords, progress: job.progressPercent, uploadedBy: job.uploadedBy, uploadedAt: job.uploadedAt }, null, 2)], { type: "application/json" });
                const a = document.createElement("a"); a.href = URL.createObjectURL(blob); a.download = `run-${jobId.slice(0,8)}.json`; a.click();
              }}
              className="px-4 py-2 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-bold hover:bg-slate-50 flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-base">download</span>Export run summary
            </button>
          </div>
        </div>
      )}

      {/* ─── Tab 2: Pipeline ───────────────────────────────────────── */}
      {activeTab === "pipe" && (() => {
        const hasSplit        = events.some(e => e.eventType === "JOB_REGISTERED");
        const hasPdfTriggered = events.some(e => e.eventType === "PDF_TRIGGERED");
        const hasPdfGenerated = events.some(e => e.eventType === "PDF_GENERATED");
        const hasPdfFailed    = events.some(e => e.eventType === "PDF_FAILED");
        const hasEmailSent    = events.some(e => e.eventType === "EMAIL_SENT");
        // Use job-level counter — EMAIL_FAILED pipeline events include blank-email skips which are not real failures
        const hasEmailFailed  = (job.emailFailedCount ?? 0) > 0;
        const hasDelivery     = events.some(e => e.eventType === "DELIVERY");
        const hasBounce       = events.some(e => e.eventType === "BOUNCE");
        const dispatched      = job.pdfGeneratedCount ?? 0;
        const isRunning     = ["PROCESSING","SPLITTING","EMAILING"].includes(job.status);
        const isSettled     = ["COMPLETED","PARTIAL","FAILED"].includes(job.status);
        const stepStatus    = (done: boolean, running: boolean) =>
          done ? "done" as const : running ? "run" as const : "pend" as const;

        const steps = [
          {
            name: "File parsing",
            status: stepStatus(hasSplit, isRunning),
            pill: hasSplit ? "pill-ok" : isRunning ? "pill-warn" : "pill-neu",
            pillText: hasSplit ? "Done" : isRunning ? "Running" : "Waiting",
            desc: "File read and split into individual customer records",
            icon: "file_open",
          },
          {
            name: "Records queued",
            status: stepStatus(hasSplit, false),
            pill: hasSplit ? "pill-ok" : "pill-neu",
            pillText: hasSplit ? "Done" : "Waiting",
            desc: "All customer records accepted for processing",
            icon: "list_alt",
          },
          {
            name: "PDF generation",
            status: stepStatus(hasPdfGenerated, isRunning && hasSplit),
            pill: hasPdfFailed ? "pill-err" : hasPdfGenerated ? "pill-ok" : isRunning && hasSplit ? "pill-warn" : "pill-neu",
            pillText: hasPdfFailed
              ? `${(job.failureCount ?? 0).toLocaleString()} failed`
              : hasPdfGenerated
              ? `${(job.pdfGeneratedCount ?? 0).toLocaleString()} created`
              : isRunning && hasSplit ? "Running" : "Waiting",
            desc: "Contract note PDFs created — password-protected and digitally signed",
            icon: "picture_as_pdf",
          },
          {
            name: "Email delivery",
            status: stepStatus(hasEmailSent, isRunning && hasPdfGenerated),
            pill: hasEmailFailed ? "pill-err" : hasEmailSent ? "pill-ok" : isRunning && hasPdfGenerated ? "pill-warn" : "pill-neu",
            pillText: hasEmailFailed
              ? `${(job.emailFailedCount ?? 0).toLocaleString()} failed`
              : hasEmailSent
              ? `${dispatched.toLocaleString()} dispatched`
              : isRunning && hasPdfGenerated ? "Running" : "Waiting",
            desc: "Contract note emailed to each client with PDF attached",
            icon: "send",
          },
          {
            name: "Delivery confirmation",
            status: stepStatus(hasDelivery || hasBounce || isSettled, isRunning && hasEmailSent),
            pill: (hasDelivery || hasBounce) ? "pill-ok"
              : isSettled && (job.emailDeliveredCount ?? 0) > 0 ? "pill-ok"
              : isSettled ? "pill-ok"
              : "pill-neu",
            pillText: (job.emailDeliveredCount ?? 0) > 0
              ? `${(job.emailDeliveredCount ?? 0).toLocaleString()} confirmed`
              : isSettled
              ? `${(job.emailSentCount ?? 0).toLocaleString()} dispatched`
              : "Waiting",
            desc: "Confirming delivery and recording any bounced addresses",
            icon: "mark_email_read",
          },
          {
            name: "Run complete",
            status: stepStatus(isSettled, false),
            pill: job.status === "COMPLETED" ? "pill-ok" : job.status === "PARTIAL" ? "pill-warn" : isSettled ? "pill-err" : "pill-neu",
            pillText: isSettled ? (STATUS_LABEL[job.status] ?? job.status) : "Waiting",
            desc: "All records processed and final status recorded",
            icon: "task_alt",
          },
        ];

        const allEvents = [...liveEvents, ...events];

        return (
          <div className="p-6 space-y-4">
            {job.status === "SPLITTING" && splitProgress !== null && (
              <div className="flex items-center gap-2 px-4 py-3 bg-purple-50 border border-purple-100 rounded-xl text-sm text-purple-800 font-semibold">
                <span className="dot bg-purple-500 animate-pulse" />
                Reading file — {splitProgress.toLocaleString()} customer records found so far…
              </div>
            )}

            {/* Pipeline steps */}
            <div className="card p-5 space-y-4">
              {steps.map((step, i) => (
                <div key={i} className={cn("pstep", step.status === "done" && "pstep-done", step.status === "run" && "pstep-run", step.status === "pend" && "pstep-pend")}>
                  <div className={cn(
                    "pstep-ico",
                    step.status === "done" ? "bg-emerald-100 text-emerald-700"
                    : step.status === "run" ? "bg-amber-100 text-amber-700"
                    : "bg-slate-100 text-slate-400"
                  )}>
                    <span className="material-symbols-outlined text-lg">
                      {step.status === "run" ? "bolt" : step.status === "done" ? "check_circle" : step.icon}
                    </span>
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center justify-between gap-3">
                      <div className="font-bold text-sm text-slate-900">{step.name}</div>
                      <span className={cn("pill flex-shrink-0", step.pill)}>{step.pillText}</span>
                    </div>
                    <div className="text-[11px] text-slate-500 mt-0.5">{step.desc}</div>
                  </div>
                </div>
              ))}
            </div>

            {/* Event log */}
            {allEvents.length > 0 && (
              <div className="card p-5">
                <div className="flex items-center justify-between mb-3">
                  <div className="text-[10px] uppercase font-bold tracking-wider text-slate-500">
                    Activity log
                    {liveEvents.length > 0 && <span className="ml-2 text-emerald-600">· Live</span>}
                  </div>
                  <span className="text-[10px] text-slate-400">{allEvents.length} events</span>
                </div>
                <div className="space-y-1.5 max-h-[320px] overflow-y-auto pr-1">
                  {allEvents.map((ev, i) => (
                    <div key={`${ev.eventId ?? ev.eventType}-${i}`} className="flex items-start gap-3 p-2.5 rounded-lg bg-slate-50 hover:bg-slate-100 transition-colors">
                      <span className={cn(
                        "dot mt-1 flex-shrink-0",
                        ["PDF_FAILED","EMAIL_FAILED","BOUNCE","COMPLAINT"].includes(ev.eventType) ? "dot-err"
                        : ["DELIVERY","PDF_GENERATED","EMAIL_SENT","JOB_REGISTERED"].includes(ev.eventType) ? "dot-ok"
                        : "dot-info"
                      )} />
                      <div className="flex-1 min-w-0">
                        <div className="text-[12px] font-medium text-slate-800">
                          {EVENT_LABEL[ev.eventType] ?? ev.eventType.replace(/_/g, " ")}
                        </div>
                        <div className="text-[10px] text-slate-400 mt-0.5 flex items-center gap-2">
                          {ev.partyCode && <span className="mono font-semibold text-slate-600">{normaliseCode(ev.partyCode)}</span>}
                          {ev.eventTimestamp && <span>{fmtTime(ev.eventTimestamp)}</span>}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        );
      })()}

      {/* ─── Tab 3: Exceptions ─────────────────────────────────────── */}
      {activeTab === "exc" && (
        <div className="p-6 space-y-4">
          {exceptionCount > 0 ? (
            <>
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm font-bold text-slate-900">{exceptionCount} record{exceptionCount !== 1 ? "s" : ""} could not be processed</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">PDF generation or email delivery failed for these clients. Use Retry to re-process individually.</div>
                </div>
                <div className="flex gap-2">
                  {canResend && (
                    <button onClick={() => bulkResend()} disabled={bulkResending} className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50">
                      {bulkResending ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-base">replay</span>}
                      Retry all ({exceptionCount})
                    </button>
                  )}
                  <button
                    onClick={() => downloadCsv(`exceptions-${jobId.slice(0,8)}.csv`,
                      ["Client code","Email","Failed stage","Reason"],
                      exceptionCustomers.map(c => [normaliseCode(c.partyCode), c.email ?? "", c.pdfStatus === "FAILED" ? "Contract note creation" : "Email delivery", c.bounceReason ?? "Unknown"])
                    )}
                    className="px-3.5 py-2 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5"
                  >
                    <span className="material-symbols-outlined text-base">download</span>Export CSV
                  </button>
                </div>
              </div>
              <div className="card overflow-hidden">
                <table className="w-full text-left">
                  <thead>
                    <tr className="t-hd">
                      <th className="px-5 py-3">Client code</th>
                      <th className="px-5 py-3">Email address</th>
                      <th className="px-5 py-3">Failed stage</th>
                      <th className="px-5 py-3">Reason</th>
                      {canResend && <th className="px-5 py-3" />}
                    </tr>
                  </thead>
                  <tbody>
                    {exceptionCustomers.map(c => (
                      <tr key={c.id} className="t-row">
                        <td className="px-5 py-3 text-[12px] font-bold text-slate-800 mono">{normaliseCode(c.partyCode)}</td>
                        <td className="px-5 py-3 text-[11px] text-slate-600 mono">{c.email || <span className="text-slate-400 italic">not on file</span>}</td>
                        <td className="px-5 py-3">
                          <span className="pill pill-err">
                            {c.pdfStatus === "FAILED" ? "Contract note creation" : "Email delivery"}
                          </span>
                        </td>
                        <td className="px-5 py-3 text-[11px] text-slate-600">{c.bounceReason ?? "Check client record for details"}</td>
                        {canResend && (
                          <td className="px-5 py-3 text-right">
                            <button onClick={() => resend(normaliseCode(c.partyCode))} disabled={resending} className="text-[11px] font-bold text-[#003ea8] hover:underline flex items-center gap-1 ml-auto">
                              <span className="material-symbols-outlined text-sm">replay</span>Retry
                            </button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <div className="card p-14 text-center">
              <span className="material-symbols-outlined text-5xl text-emerald-300" style={{ fontVariationSettings: "'FILL' 1" }}>check_circle</span>
              <div className="font-bold text-slate-700 mt-3">No errors in this run</div>
              <div className="text-[12px] text-slate-400 mt-1">All records were processed without failures</div>
            </div>
          )}
        </div>
      )}

      {/* ─── Tab 4: Bounces ────────────────────────────────────────── */}
      {activeTab === "bnc" && (
        <div className="p-6 space-y-4">
          {bounceCount > 0 ? (
            <>
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm font-bold text-slate-900">{bounceCount} email address{bounceCount !== 1 ? "es" : ""} rejected by recipient mail server</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">These clients did not receive their contract note. Update their email address in Client 360, then resend.</div>
                </div>
                <div className="flex gap-2">
                  {canResend && (
                    <button onClick={() => bulkResend()} disabled={bulkResending} className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50">
                      {bulkResending ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-base">forward_to_inbox</span>}
                      Resend all ({bounceCount})
                    </button>
                  )}
                  <button
                    onClick={() => downloadCsv(`bounces-${jobId.slice(0,8)}.csv`,
                      ["Client code","Email address","Bounce reason"],
                      bounceCustomers.map(c => [normaliseCode(c.partyCode), c.email ?? "", bounceLabel(c.bounceType)])
                    )}
                    className="px-3.5 py-2 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5"
                  >
                    <span className="material-symbols-outlined text-base">download</span>Export CSV
                  </button>
                </div>
              </div>
              <div className="card overflow-hidden">
                <table className="w-full text-left">
                  <thead>
                    <tr className="t-hd">
                      <th className="px-5 py-3">Client code</th>
                      <th className="px-5 py-3">Email address</th>
                      <th className="px-5 py-3">Bounce reason</th>
                      <th className="px-5 py-3">Bounced at</th>
                      {canResend && <th className="px-5 py-3" />}
                    </tr>
                  </thead>
                  <tbody>
                    {bounceCustomers.map(c => (
                      <tr key={c.id} className="t-row">
                        <td className="px-5 py-3 text-[12px] font-bold text-slate-800 mono">{normaliseCode(c.partyCode)}</td>
                        <td className="px-5 py-3 text-[11px] text-slate-600 mono">{c.email || "—"}</td>
                        <td className="px-5 py-3">
                          <span className={cn("pill", c.bounceType?.toLowerCase().includes("permanent") ? "pill-err" : "pill-warn")}>
                            {bounceLabel(c.bounceType)}
                          </span>
                        </td>
                        <td className="px-5 py-3 text-[11px] text-slate-500">
                          {c.bouncedAt ? new Date(c.bouncedAt.endsWith("Z") ? c.bouncedAt : c.bouncedAt + "Z").toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit", timeZone: "Asia/Kolkata" }) : "—"}
                        </td>
                        {canResend && (
                          <td className="px-5 py-3 text-right">
                            <button onClick={() => resend(normaliseCode(c.partyCode))} disabled={resending} className="text-[11px] font-bold text-[#003ea8] hover:underline flex items-center gap-1 ml-auto">
                              <span className="material-symbols-outlined text-sm">forward_to_inbox</span>Resend
                            </button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <div className="card p-14 text-center">
              <span className="material-symbols-outlined text-5xl text-emerald-300" style={{ fontVariationSettings: "'FILL' 1" }}>mark_email_read</span>
              <div className="font-bold text-slate-700 mt-3">No bounced emails</div>
              <div className="text-[12px] text-slate-400 mt-1">All sent emails were accepted by recipient mail servers</div>
            </div>
          )}
        </div>
      )}

      {/* ─── Tab 5: All clients ────────────────────────────────────── */}
      {activeTab === "cli" && (
        <div className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div className="text-sm font-bold text-slate-900">
              {totalElements.toLocaleString()} client{totalElements !== 1 ? "s" : ""} in this run
            </div>
            <button
              onClick={() => downloadCsv(`clients-${jobId.slice(0,8)}.csv`,
                ["Client code","Email","PDF","Email status","Received at"],
                customers.map(c => [
                  normaliseCode(c.partyCode), c.email ?? "",
                  PDF_LABEL[c.pdfStatus] ?? c.pdfStatus,
                  EMAIL_LABEL[c.emailStatus] ?? c.emailStatus,
                  c.deliveredAt ? fmtDateTime(c.deliveredAt) : ""
                ])
              )}
              className="px-3.5 py-2 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-base">download</span>Export CSV
            </button>
          </div>

          {loadingCust ? (
            <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
          ) : (
            <div className="card overflow-hidden">
              <table className="w-full text-left">
                <thead>
                  <tr className="t-hd">
                    <th className="px-5 py-3">Client code</th>
                    <th className="px-5 py-3">Email address</th>
                    <th className="px-5 py-3">Contract note PDF</th>
                    <th className="px-5 py-3">Email</th>
                    <th className="px-5 py-3">Received at</th>
                  </tr>
                </thead>
                <tbody>
                  {customers.map(c => (
                    <tr key={c.id} className="t-row">
                      <td className="px-5 py-3 text-[12px] font-bold text-slate-800 mono">{normaliseCode(c.partyCode)}</td>
                      <td className="px-5 py-3 text-[11px] text-slate-600 mono">
                        {c.email || <span className="text-slate-300 italic">not on file</span>}
                      </td>
                      <td className="px-5 py-3">
                        <span className={cn("pill",
                          c.pdfStatus === "GENERATED" ? "pill-ok"
                          : c.pdfStatus === "FAILED" ? "pill-err"
                          : "pill-neu"
                        )}>
                          {PDF_LABEL[c.pdfStatus] ?? c.pdfStatus}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <span className={cn("pill",
                          c.emailStatus === "DELIVERED" ? "pill-ok"
                          : c.emailStatus === "SENT"    ? "pill-ok"
                          : c.emailStatus === "BOUNCED" ? "pill-err"
                          : c.emailStatus === "FAILED"  ? "pill-err"
                          : c.emailStatus === "SKIPPED" ? "pill-err"
                          : "pill-neu"
                        )}>
                          {EMAIL_LABEL[c.emailStatus] ?? c.emailStatus}
                        </span>
                      </td>
                      <td className="px-5 py-3 text-[11px] text-slate-500">
                        {c.deliveredAt
                          ? fmtTime(c.deliveredAt)
                          : <span className="text-slate-300">—</span>
                        }
                      </td>
                    </tr>
                  ))}
                  {customers.length === 0 && (
                    <tr><td colSpan={5} className="text-center text-slate-400 py-10 text-sm">No client records found</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="px-3 py-1.5 text-sm text-slate-600 hover:text-[#00174b] disabled:opacity-40 flex items-center gap-1">
                <span className="material-symbols-outlined text-base">chevron_left</span>Previous
              </button>
              <span className="text-xs text-slate-500">Page {page + 1} of {totalPages} · {totalElements.toLocaleString()} total</span>
              <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="px-3 py-1.5 text-sm text-slate-600 hover:text-[#00174b] disabled:opacity-40 flex items-center gap-1">
                Next<span className="material-symbols-outlined text-base">chevron_right</span>
              </button>
            </div>
          )}
        </div>
      )}

    </div>
  );
}
