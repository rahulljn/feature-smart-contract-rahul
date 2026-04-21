"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { jobsApi, templatesApi } from "@/lib/api";
import type { Job, EmailTemplate } from "@/types";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import Link from "next/link";

export default function ResendPage() {
  const qc = useQueryClient();

  // Bulk state
  const [bulkScope, setBulkScope] = useState("job");
  const [bulkFromDate, setBulkFromDate] = useState("");
  const [bulkToDate, setBulkToDate] = useState("");
  const [bulkClientCodes, setBulkClientCodes] = useState("");
  const [bulkTemplateId, setBulkTemplateId] = useState("");

  const { data: jobsRes, isLoading: loadingJobs } = useQuery({
    queryKey: ["jobs-resend"],
    queryFn: () => jobsApi.list(0, 50),
  });
  const jobs: Job[] = jobsRes?.data?.data?.content ?? [];
  const resendableJobs = jobs.filter(j => j.status === "COMPLETED" || j.status === "PARTIAL");
  const [selectedJobId, setSelectedJobId] = useState("");

  const { data: templatesRes } = useQuery({
    queryKey: ["templates-resend"],
    queryFn: () => templatesApi.list(),
  });
  const _tRaw = templatesRes?.data?.data;
  const templates: EmailTemplate[] = Array.isArray(_tRaw) ? _tRaw : Array.isArray(_tRaw?.content) ? _tRaw.content : [];

  // Bulk resend by job
  const { mutate: bulkResend, isPending: bulkResending } = useMutation({
    mutationFn: () => jobsApi.bulkResend(selectedJobId || resendableJobs[0]?.jobId, bulkTemplateId || undefined),
    onSuccess: (res) => {
      toast.success(`${res.data?.data?.queued ?? 0} customers queued for resend`);
      qc.invalidateQueries({ queryKey: ["jobs-resend"] });
    },
    onError: () => toast.error("Bulk resend failed"),
  });

  // Bulk resend by party codes
  const { mutate: bulkResendCodes, isPending: bulkResendingCodes } = useMutation({
    mutationFn: () => {
      const codes = bulkClientCodes.split(/[\n,]+/).map(s => s.trim()).filter(Boolean);
      if (codes.length === 0) throw new Error("No client codes entered");
      if (!selectedJobId) throw new Error("Select a source job first");
      return jobsApi.bulkResendCodes(selectedJobId, codes, bulkTemplateId || undefined);
    },
    onSuccess: (res) => toast.success(`${res.data?.data?.queued ?? 0} customers queued for resend`),
    onError: (e: unknown) => toast.error((e as Error).message || "Bulk resend failed"),
  });

  // Bulk resend all bounced
  const { mutate: bulkResendAllBounced, isPending: bulkResendingAllBounced } = useMutation({
    mutationFn: () => jobsApi.bulkResendAllBounced(bulkTemplateId || undefined),
    onSuccess: (res) => toast.success(`${res.data?.data?.queued ?? 0} bounced records queued for resend`),
    onError: () => toast.error("Bulk resend failed"),
  });

  // Date-range bulk: filter jobs by date and resend each
  const { mutate: bulkResendByDate, isPending: bulkResendingByDate } = useMutation({
    mutationFn: async () => {
      const matchingJobs = resendableJobs.filter(j => {
        const d = j.uploadedAt ? j.uploadedAt.slice(0, 10) : "";
        return (!bulkFromDate || d >= bulkFromDate) && (!bulkToDate || d <= bulkToDate);
      });
      if (matchingJobs.length === 0) throw new Error("No completed jobs found in that date range");
      let total = 0;
      for (const j of matchingJobs) {
        const res = await jobsApi.bulkResend(j.jobId, bulkTemplateId || undefined);
        total += res.data?.data?.queued ?? 0;
      }
      return total;
    },
    onSuccess: (total) => toast.success(`${total} customers queued across date range`),
    onError: (e: unknown) => toast.error((e as Error).message || "Bulk resend by date failed"),
  });

  const handleBulkAction = () => {
    if (bulkScope === "job") {
      if (!selectedJobId && !resendableJobs[0]?.jobId) { toast.warning("No completed jobs available to resend"); return; }
      bulkResend();
    } else if (bulkScope === "daterange") {
      if (!bulkFromDate && !bulkToDate) { toast.warning("Select a date range"); return; }
      bulkResendByDate();
    } else if (bulkScope === "clients") {
      if (!selectedJobId) { toast.warning("Select a source job first"); return; }
      if (!bulkClientCodes.trim()) { toast.warning("Enter at least one client code"); return; }
      bulkResendCodes();
    } else if (bulkScope === "bounced") {
      bulkResendAllBounced();
    }
  };

  const isBulkPending = bulkResending || bulkResendingCodes || bulkResendingAllBounced || bulkResendingByDate;

  return (
    <div className="fade-up">
      {/* Sticky header */}
      <div className="bg-white border-b border-slate-200 px-6 py-4 sticky top-0 z-10">
        <div className="max-w-[1600px] mx-auto">
          <h2 className="text-xl font-extrabold text-slate-900 headline">Resend</h2>
          <p className="text-slate-500 text-[12px] mt-0.5">Re-send contract note emails in bulk for failed, bounced, or specific clients.</p>
        </div>
      </div>

      <div className="p-6 max-w-[1600px] mx-auto w-full space-y-5">

        {/* Info banner — redirect single resends to Client 360 */}
        <div className="flex items-start gap-3 p-4 bg-blue-50 border border-blue-100 rounded-xl">
          <span className="material-symbols-outlined text-blue-500 flex-shrink-0 mt-0.5">info</span>
          <div className="text-[12px] text-blue-800">
            <span className="font-bold">Need to resend for a single client?</span> Go to{" "}
            <Link href="/clients" className="underline font-semibold hover:text-blue-900">Client 360</Link>
            {" "}→ search for the client → open their profile → Actions tab → Resend latest.
          </div>
        </div>

        {/* Bulk configuration */}
        <div className="card p-6 space-y-5">
          <div className="text-sm font-bold text-slate-700">Configure batch resend</div>
          <div className="grid md:grid-cols-2 gap-5">
            <div>
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1.5">Resend scope</label>
              <select value={bulkScope} onChange={e => setBulkScope(e.target.value)} className="w-full px-3.5 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium">
                <option value="job">By Job — re-queue all failed/bounced in a specific run</option>
                <option value="daterange">By date range — all jobs within a window</option>
                <option value="clients">By client code list — specific party codes</option>
                <option value="bounced">All bounced — retry all soft-bounced records globally</option>
              </select>
            </div>
            <div>
              {bulkScope === "job" && (
                <div>
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1.5">Source job</label>
                  <select value={selectedJobId} onChange={e => setSelectedJobId(e.target.value)} className="w-full px-3.5 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium">
                    <option value="">Select job...</option>
                    {loadingJobs ? <option disabled>Loading...</option> : resendableJobs.map(j => <option key={j.jobId} value={j.jobId}>{j.fileName} · {j.segmentType}</option>)}
                  </select>
                </div>
              )}
              {bulkScope === "daterange" && (
                <div className="space-y-2">
                  <div className="flex gap-2">
                    <div className="flex-1">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">From</label>
                      <input type="date" value={bulkFromDate} onChange={e => setBulkFromDate(e.target.value)} className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                    <div className="flex-1">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">To</label>
                      <input type="date" value={bulkToDate} onChange={e => setBulkToDate(e.target.value)} className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                  </div>
                  <p className="text-[10px] text-slate-400">Resends all failed/bounced customers in completed jobs within this date range.</p>
                </div>
              )}
              {bulkScope === "clients" && (
                <div>
                  <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">Client codes (one per line or comma-separated)</label>
                  <textarea rows={4} value={bulkClientCodes} onChange={e => setBulkClientCodes(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm mono" placeholder={"8000274\n8000281\n8000305"} />
                  <div className="mt-2">
                    <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">Source job</label>
                    <select value={selectedJobId} onChange={e => setSelectedJobId(e.target.value)} className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium">
                      <option value="">Select job...</option>
                      {resendableJobs.map(j => <option key={j.jobId} value={j.jobId}>{j.fileName} · {j.segmentType}</option>)}
                    </select>
                  </div>
                </div>
              )}
              {bulkScope === "bounced" && (
                <div className="p-3 bg-blue-50 border border-blue-100 rounded-xl text-[12px] text-blue-800">
                  All email addresses that bounced (except permanently blocked ones) will be retried across all runs.
                </div>
              )}
            </div>
            <div>
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1.5">Template override <span className="text-slate-400 font-normal">(optional)</span></label>
              <select value={bulkTemplateId} onChange={e => setBulkTemplateId(e.target.value)} className="w-full px-3.5 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium">
                <option value="">Use job&apos;s original template</option>
                {templates.map(t => (
                  <option key={t.templateId} value={t.templateId}>{t.name}{t.isActive ? " (active)" : ""}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Action */}
          <div className="border-t border-slate-100 pt-4 flex items-center gap-3">
            <button onClick={handleBulkAction} disabled={isBulkPending} className="px-5 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50">
              {isBulkPending ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-base">rocket_launch</span>}
              Queue resend
            </button>
            <span className="text-[11px] text-slate-400">Re-queued records are picked up and processed automatically within 60 seconds.</span>
          </div>
        </div>

      </div>
    </div>
  );
}
