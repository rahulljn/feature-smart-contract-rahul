"use client";

import { useState, useRef, useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { jobsApi, pipelineApi } from "@/lib/api";
import type { Job, PipelineEvent, ValidationResult } from "@/types";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import Link from "next/link";
import { formatDistanceToNow } from "date-fns";

const SEGMENTS = [
  "EQUITY-COMBINEMARGIN",
  "ROS",
  "BILL",
  "COMMODITY",
  "DP-HOLDING",
  "DP-HOLDING-YEARLY",
  "DP-LEDGER-WEEKLY",
  "DP-TRADE-TXN",
  "STT",
  "PNL",
  "AGTS",
  "QS-LEDGER",
  "QS-RETENTION",
  "DMR",
];

const STEPS = ["Upload", "Configure", "Pre-flight", "Submitted"];

export default function ProcessPage() {
  const qc = useQueryClient();
  const [step, setStep] = useState(0); // 0=Upload, 1=Configure, 2=Preflight, 3=Submitted
  const [file, setFile] = useState<File | null>(null);
  const [segment, setSegment] = useState(0);
  const [createdJob, setCreatedJob] = useState<Job | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [validating, setValidating] = useState(false);
  const [validated, setValidated] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [pipelineEvents, setPipelineEvents] = useState<PipelineEvent[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);
  const sseRef = useRef<EventSource | null>(null);

  // Connect SSE when job is created
  useEffect(() => {
    if (step === 3 && createdJob) {
      const token = typeof window !== "undefined" ? JSON.parse(localStorage.getItem("auth-storage") ?? "{}")?.state?.user?.accessToken : "";
      const url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/jobs/${createdJob.jobId}/stream`;
      const es = new EventSource(`${url}?token=${token}`);
      sseRef.current = es;
      es.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data);
          if (data.eventType) {
            setPipelineEvents(prev => [data, ...prev].slice(0, 50));
          }
        } catch { /* ignore */ }
      };
      return () => { es.close(); sseRef.current = null; };
    }
  }, [step, createdJob]);

  // Also fetch historical events
  const { data: histEventsRes } = useQuery({
    queryKey: ["pipeline-events-process", createdJob?.jobId],
    queryFn: () => pipelineApi.events(createdJob!.jobId),
    enabled: step === 3 && !!createdJob,
  });
  const histEvents: PipelineEvent[] = Array.isArray(histEventsRes?.data?.data) ? histEventsRes.data.data : [];
  const allEvents = pipelineEvents.length > 0 ? pipelineEvents : histEvents;

  // Derive pipeline step statuses from events
  const hasEvent = (type: string) => allEvents.some(e => e.eventType === type);
  const stepStatus = (types: string[]) => {
    if (types.some(t => hasEvent(t))) return "done";
    return "pend";
  };

  const { data: recentJobsRes } = useQuery({
    queryKey: ["recent-jobs-process"],
    queryFn: () => jobsApi.list(0, 3),
  });
  const recentJobs: Job[] = recentJobsRes?.data?.data?.content ?? [];

  // Poll live job status every 5s while on submitted step — reads job table (DB)
  const { data: liveJobRes } = useQuery({
    queryKey: ["process-live-job", createdJob?.jobId],
    queryFn: () => jobsApi.get(createdJob!.jobId),
    enabled: step === 3 && !!createdJob?.jobId,
    refetchInterval: 5_000,
  });
  const liveJob = liveJobRes?.data?.data ?? createdJob;
  const jobIsSettled = liveJob?.status === "COMPLETED" || liveJob?.status === "PARTIAL" || liveJob?.status === "FAILED";
  const needsAttention = jobIsSettled && ((liveJob?.failureCount ?? 0) > 0 || (liveJob?.bounceCount ?? 0) > 0);

  const { mutate: quickBulkResend, isPending: quickBulkResending } = useMutation({
    mutationFn: () => jobsApi.bulkResend(createdJob!.jobId),
    onSuccess: (res) => {
      toast.success(`${res.data?.data?.queued ?? 0} customers queued for resend`);
      qc.invalidateQueries({ queryKey: ["process-live-job", createdJob?.jobId] });
    },
    onError: () => toast.error("Resend failed"),
  });

  const { mutate: upload, isPending } = useMutation({
    mutationFn: () => {
      const form = new FormData();
      form.append("file", file!);
      form.append("segmentType", SEGMENTS[segment]);
      return jobsApi.upload(form);
    },
    onSuccess: (res) => {
      setCreatedJob(res.data.data);
      setStep(3);
      toast.success("Run submitted — pipeline triggered");
    },
    onError: () => {
      toast.error("Upload failed — please try again");
    },
  });

  const handleFile = (f: File) => {
    setFile(f);
  };

  const next = () => {
    if (step === 2) {
      upload();
    } else {
      setStep(s => Math.min(s + 1, 3));
    }
  };

  const prev = () => setStep(s => Math.max(s - 1, 0));

  const reset = () => {
    setStep(0);
    setFile(null);
    setCreatedJob(null);
    setValidating(false);
    setValidated(false);
    setValidationResult(null);
    setValidationError(null);
    if (inputRef.current) inputRef.current.value = "";
  };

  const runValidation = async () => {
    if (!file) return;
    setValidating(true);
    setValidated(false);
    setValidationResult(null);
    setValidationError(null);
    try {
      const form = new FormData();
      form.append("file", file);
      const res = await jobsApi.validate(form);
      setValidationResult(res.data.data);
      setValidated(true);
    } catch {
      setValidationError("Validation failed — could not reach the server. You can still submit.");
      setValidated(true);
    } finally {
      setValidating(false);
    }
  };

  return (
    <div className="p-6 max-w-3xl mx-auto w-full fade-up">
      {/* Header */}
      <div className="mb-6">
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Ops</span>
          <span className="material-symbols-outlined text-sm">chevron_right</span>
          <span className="text-slate-700 font-semibold">Process File</span>
        </div>
        <h2 className="text-2xl font-extrabold text-slate-900 mb-0.5 headline">Process a raw file</h2>
        <p className="text-slate-500 text-sm">Upload a tilde-delimited (~) raw file, select the report type, run pre-flight checks, then submit to start processing.</p>
      </div>

      {/* Step indicator */}
      <div className="flex items-center mb-8 px-2">
        {STEPS.map((label, i) => (
          <div key={label} className="contents">
            <div className="flex flex-col items-center gap-1.5">
              <div className={cn(
                "step-dot",
                i < step && "done",
                i === step && "current",
                i > step && "pending"
              )}>
                {i < step ? <span className="material-symbols-outlined text-sm">check</span> : i + 1}
              </div>
              <div className={cn(
                "text-[10px] font-semibold",
                i <= step ? "text-slate-600" : "text-slate-400"
              )}>{label}</div>
            </div>
            {i < 3 && <div className={cn("step-line", i < step && "done")} />}
          </div>
        ))}
      </div>

      {/* STEP 1: Upload */}
      {step === 0 && (
        <div className="card overflow-hidden">
          <div className="p-6">
            <div
              className={cn("dropzone", dragOver && "!border-[#497cff] !bg-[#eff6ff]")}
              onClick={() => inputRef.current?.click()}
              onDragOver={e => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={e => { e.preventDefault(); setDragOver(false); const f = e.dataTransfer.files[0]; if (f) handleFile(f); }}
            >
              <div className="w-14 h-14 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-3">
                <span className="material-symbols-outlined text-3xl text-[#00174b]">cloud_upload</span>
              </div>
              <div className="font-bold text-slate-800 mb-1 headline">Drop raw file here</div>
              <div className="text-slate-500 text-sm mb-4">.txt · tilde-delimited (H / HT / OT / T record types) · up to 10 GB</div>
              <button className="px-5 py-2 bg-[#00174b] text-white rounded-full text-sm font-bold hover:bg-[#003ea8] transition-colors shadow-md pointer-events-none">
                Browse files
              </button>
              <input
                ref={inputRef}
                type="file"
                accept=".txt,.csv"
                className="hidden"
                onChange={e => { const f = e.target.files?.[0]; if (f) handleFile(f); }}
              />
            </div>

            {file && (
              <div className="mt-5 p-3 bg-emerald-50 rounded-xl border border-emerald-100 flex items-center gap-3">
                <span className="material-symbols-outlined text-emerald-600">check_circle</span>
                <div className="flex-1">
                  <div className="text-sm font-bold text-emerald-800">{file.name}</div>
                  <div className="text-xs text-emerald-700">{(file.size / 1024 / 1024).toFixed(2)} MB · ready for configuration</div>
                </div>
                <button onClick={() => setFile(null)} className="text-xs font-bold text-emerald-700 hover:underline">Remove</button>
              </div>
            )}

            <div className="mt-5 p-3 bg-amber-50 border border-amber-100 rounded-xl flex items-start gap-2.5">
              <span className="material-symbols-outlined text-amber-600 text-lg flex-shrink-0">info</span>
              <div className="text-[11px] text-amber-800 leading-relaxed">
                <span className="font-bold">Format:</span> Each line is tilde-delimited. First line is a header (<span className="mono">H</span>), client rows are <span className="mono">HT</span>, optional address rows <span className="mono">OT</span>, trailer is <span className="mono">T</span>.
              </div>
            </div>

            <div className="mt-5">
              <div className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2">Recent uploads</div>
              <div className="space-y-1.5">
                {recentJobs.length > 0 ? recentJobs.map(j => (
                  <Link
                    key={j.jobId}
                    href={`/jobs/${j.jobId}`}
                    className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl hover:bg-blue-50 cursor-pointer transition-colors group"
                  >
                    <span className="material-symbols-outlined text-slate-400 group-hover:text-[#00174b]">description</span>
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-semibold text-slate-700 truncate">{j.fileName}</div>
                      <div className="text-[11px] text-slate-400">{j.totalRecords?.toLocaleString() ?? "—"} records · {formatDistanceToNow(new Date(j.uploadedAt), { addSuffix: true })}</div>
                    </div>
                    <span className="seg-chip">{j.segmentType ?? "—"}</span>
                  </Link>
                )) : (
                  <div className="text-sm text-slate-400 py-2">No recent uploads</div>
                )}
              </div>
            </div>
          </div>
          <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-100 flex justify-end">
            <button
              onClick={next}
              disabled={!file}
              className="px-5 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] transition-colors flex items-center gap-1.5 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Next: Configure <span className="material-symbols-outlined text-base">arrow_forward</span>
            </button>
          </div>
        </div>
      )}

      {/* STEP 2: Configure */}
      {step === 1 && file && (
        <div className="card overflow-hidden">
          <div className="p-6 space-y-5">
            <div className="flex items-center gap-3 p-3 bg-emerald-50 rounded-xl border border-emerald-100">
              <span className="material-symbols-outlined text-emerald-600">check_circle</span>
              <div className="flex-1">
                <div className="text-sm font-bold text-emerald-800">{file.name}</div>
                <div className="text-xs text-emerald-700">{(file.size / 1024 / 1024).toFixed(2)} MB · ready to configure</div>
              </div>
              <button onClick={prev} className="text-xs font-bold text-emerald-700 hover:underline">Change</button>
            </div>

            <div>
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1.5">Report type</label>
              <select
                value={segment}
                onChange={e => setSegment(Number(e.target.value))}
                className="w-full px-3.5 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium"
              >
                {SEGMENTS.map((s, i) => <option key={i} value={i}>{s}</option>)}
              </select>
              <p className="text-[10px] text-slate-400 mt-1">Determines the PDF layout and email template used for this batch.</p>
            </div>

          </div>
          <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-100 flex justify-between">
            <button onClick={prev} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-base">arrow_back</span>Back
            </button>
            <button onClick={next} className="px-5 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5">
              Next: Pre-flight <span className="material-symbols-outlined text-base">arrow_forward</span>
            </button>
          </div>
        </div>
      )}

      {/* STEP 3: Pre-flight Validate */}
      {step === 2 && file && (
        <div className="card overflow-hidden">
          <div className="p-6 space-y-4">
            <div className="text-sm font-bold text-slate-800 headline">Pre-submission review</div>
            <div className="grid grid-cols-2 gap-2.5">
              <div className="p-3.5 bg-slate-50 rounded-xl">
                <div className="text-[10px] text-slate-400 uppercase font-bold mb-0.5">File</div>
                <div className="text-sm font-semibold text-slate-800 truncate">{file.name}</div>
              </div>
              <div className="p-3.5 bg-slate-50 rounded-xl">
                <div className="text-[10px] text-slate-400 uppercase font-bold mb-0.5">Segment</div>
                <div className="text-sm font-semibold text-slate-800">{SEGMENTS[segment]}</div>
              </div>
              <div className="p-3.5 bg-slate-50 rounded-xl">
                <div className="text-[10px] text-slate-400 uppercase font-bold mb-0.5">Size</div>
                <div className="text-sm font-semibold text-slate-800 mono">{(file.size / 1024 / 1024).toFixed(2)} MB</div>
              </div>
            </div>

            <div className="space-y-2">
              <div className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Static checks</div>
              <div className="flex items-center gap-2.5 p-3 bg-emerald-50 rounded-xl">
                <span className="material-symbols-outlined text-emerald-500 text-lg">check_circle</span>
                <span className="text-sm text-emerald-700 font-medium">File extension valid · {file.name.split(".").pop()}</span>
              </div>
              <div className="flex items-center gap-2.5 p-3 bg-emerald-50 rounded-xl">
                <span className="material-symbols-outlined text-emerald-500 text-lg">check_circle</span>
                <span className="text-sm text-emerald-700 font-medium">File size within limits ({(file.size / 1024 / 1024).toFixed(2)} MB)</span>
              </div>
              <div className="flex items-center gap-2.5 p-3 bg-emerald-50 rounded-xl">
                <span className="material-symbols-outlined text-emerald-500 text-lg">check_circle</span>
                <span className="text-sm text-emerald-700 font-medium">Segment selected · {SEGMENTS[segment]}</span>
              </div>
            </div>

            {/* Optional validation block */}
            <div className="rounded-xl border border-blue-200 bg-blue-50/60 p-4">
              <div className="flex items-start gap-3">
                <span className="material-symbols-outlined text-[#003ea8] text-lg flex-shrink-0 mt-0.5">fact_check</span>
                <div className="flex-1">
                  <div className="text-sm font-bold text-slate-800 mb-1">Validate file (optional)</div>
                  <p className="text-xs text-slate-600 leading-relaxed">
                    Scan every row before submission to catch missing client codes, invalid email addresses, and date format errors. Safe to skip — invalid rows are skipped automatically during processing.
                  </p>
                  <div className="flex flex-wrap gap-2 mt-3">
                    <button
                      onClick={runValidation}
                      disabled={validating}
                      className="px-4 py-2 bg-[#003ea8] text-white rounded-lg text-xs font-bold hover:bg-[#00174b] flex items-center gap-1.5 disabled:opacity-50"
                    >
                      {validating ? <Loader2 size={14} className="animate-spin" /> : <span className="material-symbols-outlined text-sm">play_arrow</span>}
                      {validating ? "Validating…" : "Validate raw file"}
                    </button>
                    <button onClick={next} className="px-4 py-2 bg-white border border-slate-300 text-slate-700 rounded-lg text-xs font-bold hover:bg-slate-50 flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-sm">skip_next</span>Skip &amp; proceed
                    </button>
                  </div>

                  {validating && (
                    <div className="mt-4">
                      <div className="flex items-center gap-3 p-3 bg-white border border-blue-200 rounded-lg">
                        <div className="w-5 h-5 rounded-full border-2 border-[#497cff] border-t-transparent animate-spin" />
                        <div className="flex-1">
                          <div className="text-xs font-semibold text-slate-800">Validating rows…</div>
                          <div className="text-[11px] text-slate-500 mono">Processing chunks</div>
                        </div>
                      </div>
                    </div>
                  )}

                  {validated && validationError && (
                    <div className="mt-4 p-3 bg-slate-50 border border-slate-200 rounded-xl flex items-start gap-2">
                      <span className="material-symbols-outlined text-slate-400 text-base">warning</span>
                      <div className="text-[11px] text-slate-600">{validationError}</div>
                    </div>
                  )}

                  {validated && validationResult && (
                    <div className="mt-4 space-y-3">
                      <div className="grid grid-cols-3 gap-2">
                        <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-center">
                          <div className="text-[9px] text-slate-500 font-bold uppercase mb-1">Total customers</div>
                          <div className="text-xl font-extrabold text-slate-900 mono">{validationResult.totalCustomers.toLocaleString()}</div>
                        </div>
                        <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-center">
                          <div className="text-[9px] text-emerald-700 font-bold uppercase mb-1">Valid</div>
                          <div className="text-xl font-extrabold text-emerald-700 mono">{validationResult.validCustomers.toLocaleString()}</div>
                        </div>
                        <div className={cn("p-3 rounded-lg text-center border", validationResult.invalidCustomers > 0 ? "bg-rose-50 border-rose-200" : "bg-emerald-50 border-emerald-200")}>
                          <div className={cn("text-[9px] font-bold uppercase mb-1", validationResult.invalidCustomers > 0 ? "text-rose-700" : "text-emerald-700")}>Invalid</div>
                          <div className={cn("text-xl font-extrabold mono", validationResult.invalidCustomers > 0 ? "text-rose-700" : "text-emerald-700")}>{validationResult.invalidCustomers.toLocaleString()}</div>
                        </div>
                      </div>

                      {validationResult.invalidCustomers === 0 ? (
                        <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center gap-2">
                          <span className="material-symbols-outlined text-emerald-600 text-base">check_circle</span>
                          <div className="text-[11px] text-emerald-800 font-semibold">
                            All {validationResult.totalCustomers.toLocaleString()} customers passed validation. Ready to submit.
                          </div>
                        </div>
                      ) : (
                        <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl space-y-2">
                          <div className="flex items-start gap-2">
                            <span className="material-symbols-outlined text-amber-600 text-base flex-shrink-0">warning</span>
                            <div className="text-[11px] text-amber-800 leading-relaxed">
                              <span className="font-bold">{validationResult.invalidCustomers.toLocaleString()} customer{validationResult.invalidCustomers > 1 ? "s" : ""} will be rejected</span> — they contain validation errors and will be written to the error bucket.
                              <span className="block mt-1 text-amber-700">You can still submit — invalid customers are skipped, valid ones are processed normally.</span>
                            </div>
                          </div>
                          {validationResult.invalidReasons && Object.keys(validationResult.invalidReasons).length > 0 && (
                            <div className="ml-6 space-y-1">
                              <div className="text-[9px] font-bold uppercase tracking-widest text-amber-700 mb-1">Failure breakdown</div>
                              {Object.entries(validationResult.invalidReasons)
                                .sort(([, a], [, b]) => b - a)
                                .map(([reason, count]) => (
                                  <div key={reason} className="flex items-center gap-2">
                                    <span className="mono text-[10px] bg-amber-100 text-amber-800 px-1.5 py-0.5 rounded font-bold">{reason}</span>
                                    <span className="text-[10px] text-amber-700">{count.toLocaleString()} customer{count > 1 ? "s" : ""}</span>
                                  </div>
                                ))}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
          <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-100 flex justify-between">
            <button onClick={prev} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-base">arrow_back</span>Back
            </button>
            <button
              onClick={next}
              disabled={isPending}
              className="px-5 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-60"
            >
              {isPending ? <Loader2 size={16} className="animate-spin" /> : <span className="material-symbols-outlined text-base">rocket_launch</span>}
              {isPending ? "Submitting…" : "Confirm & submit"}
            </button>
          </div>
        </div>
      )}

      {/* STEP 4: Submitted */}
      {step === 3 && (
        <div className="card p-8">
          <div className="text-center mb-7">
            <div className="w-[4.5rem] h-[4.5rem] bg-emerald-50 rounded-full flex items-center justify-center mx-auto mb-5">
              <span className="material-symbols-outlined text-5xl text-emerald-500" style={{ fontVariationSettings: "'FILL' 1" }}>rocket_launch</span>
            </div>
            <h3 className="text-xl font-extrabold text-slate-900 mb-1.5 headline">Run submitted</h3>
            <p className="text-slate-500 text-sm mb-3">The pipeline is now processing your file.</p>
            {createdJob && (
              <div className="inline-block px-5 py-2 bg-blue-50 text-[#00174b] font-bold mono text-sm rounded-lg">{createdJob.jobId}</div>
            )}
          </div>

          <div className="mb-6">
            <div className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">Live pipeline {allEvents.length > 0 && <span className="text-emerald-600">· {allEvents.length} events received</span>}</div>
            <div className="space-y-2">
              {[
                { name: "File uploaded", types: ["JOB_REGISTERED"], desc: `${file?.name} · received and queued for processing` },
                { name: "File parsing", types: ["JOB_REGISTERED", "SPLIT_COMPLETE"], desc: "Splitting into individual customer records" },
                { name: "PDF generation", types: ["PDF_GENERATED", "PDF_TRIGGERED"], desc: "Creating password-protected, digitally signed contract notes" },
                { name: "Email dispatch", types: ["EMAIL_SENT"], desc: "Sending contract notes to each client" },
              ].map((s, i) => {
                const status = stepStatus(s.types);
                return (
                  <div key={i} className={cn("pnode flex items-center gap-3", status === "done" && "done", status === "pend" && "opacity-60")}>
                    {status === "done"
                      ? <span className="material-symbols-outlined text-emerald-600 text-lg">check_circle</span>
                      : <div className="w-5 h-5 rounded-full bg-slate-200" />}
                    <div className="flex-1">
                      <div className={cn("font-bold text-sm", status === "pend" && "text-slate-500")}>{s.name}</div>
                      <div className="text-[10px] text-slate-500">{s.desc}</div>
                    </div>
                    {status === "done" && <span className="pill pill-ok">Done</span>}
                  </div>
                );
              })}
            </div>
            {allEvents.length > 0 && (
              <div className="mt-4 space-y-1 max-h-[200px] overflow-y-auto">
                {allEvents.slice(0, 10).map((ev, i) => (
                  <div key={i} className="flex items-center gap-2 px-3 py-1.5 bg-slate-50 rounded-lg text-xs">
                    <span className="font-medium text-slate-800">{{ JOB_REGISTERED:"File registered", SPLIT_PROGRESS:"Reading file", SPLIT_COMPLETE:"File read", CUSTOMER_REGISTERED:"Record registered", PDF_TRIGGERED:"PDF started", PDF_GENERATED:"PDF created", PDF_FAILED:"PDF failed", EMAIL_SENT:"Email sent", EMAIL_FAILED:"Email failed", EMAIL_SKIPPED:"No email on file", DELIVERY:"Delivered", BOUNCE:"Bounced", COMPLAINT:"Spam report", RESEND_TRIGGERED:"Resend triggered" }[ev.eventType] ?? ev.eventType}</span>
                    {ev.partyCode && <span className="mono text-slate-500">{ev.partyCode.split("/")[0]}</span>}
                    <span className="text-slate-400 ml-auto">{ev.eventTimestamp ? new Date(ev.eventTimestamp.endsWith("Z") ? ev.eventTimestamp : ev.eventTimestamp + "Z").toLocaleTimeString("en-GB", { timeZone: "Asia/Kolkata" }) : ""}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Failure summary — appears when job settles with failures/bounces */}
          {needsAttention && (
            <div className="mt-2 p-4 bg-amber-50 border border-amber-200 rounded-xl">
              <div className="flex items-center gap-2 mb-2">
                <span className="material-symbols-outlined text-amber-600">warning</span>
                <span className="text-sm font-bold text-amber-900">
                  {(liveJob!.failureCount ?? 0) + (liveJob!.bounceCount ?? 0)} customers need attention
                </span>
              </div>
              <div className="flex gap-5 text-[11px] text-amber-800 mb-3">
                {(liveJob!.failureCount ?? 0) > 0 && (
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-sm text-rose-500">error</span>
                    {liveJob!.failureCount} invalid records
                  </span>
                )}
                {(liveJob!.bounceCount ?? 0) > 0 && (
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-sm text-amber-500">unsubscribe</span>
                    {liveJob!.bounceCount} bounced
                  </span>
                )}
              </div>
              <button
                onClick={() => quickBulkResend()}
                disabled={quickBulkResending}
                className="px-4 py-2 bg-amber-600 text-white rounded-lg text-xs font-bold hover:bg-amber-700 flex items-center gap-1.5 disabled:opacity-50"
              >
                {quickBulkResending ? <Loader2 size={12} className="animate-spin" /> : <span className="material-symbols-outlined text-sm">replay</span>}
                Resend all failed now
              </button>
            </div>
          )}

          <div className="flex gap-2.5 justify-center">
            <Link
              href="/jobs"
              className="px-5 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8]"
            >
              Open in Runs
            </Link>
            {createdJob && (
              <Link
                href={`/jobs/${createdJob.jobId}`}
                className="px-4 py-2.5 bg-blue-50 text-[#003ea8] rounded-xl text-sm font-semibold hover:bg-blue-100"
              >
                View run detail
              </Link>
            )}
            <button onClick={reset} className="px-4 py-2.5 bg-slate-100 text-slate-600 rounded-xl text-sm font-semibold hover:bg-slate-200">
              Process another
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
