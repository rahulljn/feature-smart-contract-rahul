"use client";

import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { jobsApi, templatesApi } from "@/lib/api";
import type { Job, EmailTemplate, JobCustomer } from "@/types";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import Link from "next/link";

// ─── helpers ──────────────────────────────────────────────────────────────────

function formatDate(iso?: string) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-IN", {
    day: "2-digit", month: "short", year: "numeric",
  });
}

function StatusBadge({ status }: { status: string }) {
  const map: Record<string, string> = {
    BOUNCED: "bg-red-100 text-red-700",
    FAILED:  "bg-orange-100 text-orange-700",
    SKIPPED: "bg-slate-100 text-slate-500",
  };
  return (
    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wide ${map[status] ?? "bg-slate-100 text-slate-500"}`}>
      {status}
    </span>
  );
}

// ─── main component ───────────────────────────────────────────────────────────

export default function ResendPage() {
  const qc = useQueryClient();

  // ── bulk config state ──────────────────────────────────────────────────────
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

  // ── bulk resend mutations ──────────────────────────────────────────────────
  const { mutate: bulkResend, isPending: bulkResending } = useMutation({
    mutationFn: () => jobsApi.bulkResend(selectedJobId || resendableJobs[0]?.jobId, bulkTemplateId || undefined),
    onSuccess: (res) => {
      toast.success(`${res.data?.data?.queued ?? 0} customers queued for resend`);
      qc.invalidateQueries({ queryKey: ["jobs-resend"] });
    },
    onError: () => toast.error("Bulk resend failed"),
  });

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

  const { mutate: bulkResendAllBounced, isPending: bulkResendingAllBounced } = useMutation({
    mutationFn: () => jobsApi.bulkResendAllBounced(bulkTemplateId || undefined),
    onSuccess: (res) => toast.success(`${res.data?.data?.queued ?? 0} bounced records queued for resend`),
    onError: () => toast.error("Bulk resend failed"),
  });

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

  // ── failed customers table state ───────────────────────────────────────────
  const [tableStatus, setTableStatus] = useState<"ALL" | "BOUNCED" | "FAILED">("ALL");
  const [tableJobId, setTableJobId] = useState("");
  const [tableFrom, setTableFrom] = useState("");
  const [tableTo, setTableTo] = useState("");
  const [tablePage, setTablePage] = useState(0);
  const TABLE_SIZE = 50;

  const statusParam = tableStatus === "ALL" ? "BOUNCED,FAILED" : tableStatus;

  const { data: failedRes, isLoading: loadingFailed, isFetching: fetchingFailed } = useQuery({
    queryKey: ["failed-customers", tableStatus, tableJobId, tableFrom, tableTo, tablePage],
    queryFn: () => jobsApi.failedCustomers(
      tablePage, TABLE_SIZE,
      statusParam,
      tableJobId || undefined,
      tableFrom || undefined,
      tableTo || undefined,
    ),
  });

  const failedPage = failedRes?.data?.data;
  const failedCustomers: JobCustomer[] = failedPage?.content ?? [];
  const totalPages: number = failedPage?.totalPages ?? 0;
  const totalElements: number = failedPage?.totalElements ?? 0;

  // ── selection state ────────────────────────────────────────────────────────
  const [selected, setSelected] = useState<Set<number>>(new Set());

  const allPageIds = failedCustomers.map(c => c.id);
  const allSelected = allPageIds.length > 0 && allPageIds.every(id => selected.has(id));
  const someSelected = allPageIds.some(id => selected.has(id));

  function toggleAll() {
    if (allSelected) {
      setSelected(prev => { const n = new Set(prev); allPageIds.forEach(id => n.delete(id)); return n; });
    } else {
      setSelected(prev => { const n = new Set(prev); allPageIds.forEach(id => n.add(id)); return n; });
    }
  }

  function toggleRow(id: number) {
    setSelected(prev => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n; });
  }

  const selectedRows = useMemo(
    () => failedCustomers.filter(c => selected.has(c.id)),
    [failedCustomers, selected],
  );

  // ── single row resend ──────────────────────────────────────────────────────
  const [resendingIds, setResendingIds] = useState<Set<number>>(new Set());

  async function resendSingle(c: JobCustomer) {
    if (!c.jobId) { toast.error("No job ID for this record"); return; }
    setResendingIds(prev => new Set(prev).add(c.id));
    try {
      await jobsApi.resend(c.jobId, c.partyCode);
      toast.success(`Resend triggered for ${c.partyCode}`);
      qc.invalidateQueries({ queryKey: ["failed-customers"] });
    } catch {
      toast.error(`Resend failed for ${c.partyCode}`);
    } finally {
      setResendingIds(prev => { const n = new Set(prev); n.delete(c.id); return n; });
    }
  }

  // ── bulk resend selected rows ──────────────────────────────────────────────
  const [resendingSelected, setResendingSelected] = useState(false);

  async function resendSelected() {
    if (selectedRows.length === 0) return;
    setResendingSelected(true);
    try {
      // Group by jobId
      const byJob = new Map<string, string[]>();
      for (const c of selectedRows) {
        if (!c.jobId) continue;
        if (!byJob.has(c.jobId)) byJob.set(c.jobId, []);
        byJob.get(c.jobId)!.push(c.partyCode);
      }
      let total = 0;
      for (const [jid, codes] of byJob.entries()) {
        const res = await jobsApi.bulkResendCodes(jid, codes);
        total += res.data?.data?.queued ?? 0;
      }
      toast.success(`${total} customer${total !== 1 ? "s" : ""} queued for resend`);
      setSelected(new Set());
      qc.invalidateQueries({ queryKey: ["failed-customers"] });
    } catch {
      toast.error("Resend failed for selected records");
    } finally {
      setResendingSelected(false);
    }
  }

  function resetFilters() {
    setTableStatus("ALL");
    setTableJobId("");
    setTableFrom("");
    setTableTo("");
    setTablePage(0);
    setSelected(new Set());
  }

  function applyFilter() {
    setTablePage(0);
    setSelected(new Set());
    qc.invalidateQueries({ queryKey: ["failed-customers"] });
  }

  // ─────────────────────────────────────────────────────────────────────────
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

        {/* ── Failed & Bounced Emails Table ─────────────────────────────────── */}
        <div className="card p-6 space-y-4">
          {/* Section header */}
          <div className="flex items-center justify-between flex-wrap gap-3">
            <div>
              <div className="text-sm font-bold text-slate-700">Failed &amp; Bounced Emails</div>
              <p className="text-[11px] text-slate-400 mt-0.5">
                Select individual records to resend, or use checkboxes for bulk action.
              </p>
            </div>
            {totalElements > 0 && (
              <span className="text-[11px] text-slate-500 font-medium">
                {totalElements.toLocaleString()} record{totalElements !== 1 ? "s" : ""}
              </span>
            )}
          </div>

          {/* Filter bar */}
          <div className="flex flex-wrap items-end gap-3">
            {/* Status pills */}
            <div>
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">Status</label>
              <div className="flex gap-1">
                {(["ALL", "BOUNCED", "FAILED"] as const).map(s => (
                  <button
                    key={s}
                    onClick={() => { setTableStatus(s); setTablePage(0); setSelected(new Set()); }}
                    className={`px-3 py-1.5 rounded-lg text-[11px] font-bold border transition-colors ${
                      tableStatus === s
                        ? "bg-[#00174b] text-white border-[#00174b]"
                        : "bg-white text-slate-600 border-slate-200 hover:border-slate-400"
                    }`}
                  >
                    {s === "ALL" ? "All" : s.charAt(0) + s.slice(1).toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            {/* Job filter */}
            <div className="flex-1 min-w-[200px]">
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">Job</label>
              <select
                value={tableJobId}
                onChange={e => { setTableJobId(e.target.value); setTablePage(0); setSelected(new Set()); }}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-[12px] font-medium"
              >
                <option value="">All jobs</option>
                {jobs.map(j => <option key={j.jobId} value={j.jobId}>{j.fileName} · {j.segmentType}</option>)}
              </select>
            </div>

            {/* Date range */}
            <div>
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">From</label>
              <input
                type="date"
                value={tableFrom}
                onChange={e => { setTableFrom(e.target.value); setTablePage(0); setSelected(new Set()); }}
                className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-[12px]"
              />
            </div>
            <div>
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">To</label>
              <input
                type="date"
                value={tableTo}
                onChange={e => { setTableTo(e.target.value); setTablePage(0); setSelected(new Set()); }}
                className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-[12px]"
              />
            </div>

            {/* Refresh / Reset */}
            <button
              onClick={applyFilter}
              className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-xl text-[12px] font-medium flex items-center gap-1"
            >
              <span className="material-symbols-outlined text-sm">refresh</span>
              Refresh
            </button>
            {(tableJobId || tableFrom || tableTo || tableStatus !== "ALL") && (
              <button
                onClick={resetFilters}
                className="px-3 py-2 text-slate-400 hover:text-slate-600 rounded-xl text-[12px] font-medium"
              >
                Clear filters
              </button>
            )}
          </div>

          {/* Bulk action bar — shows when rows selected */}
          {selected.size > 0 && (
            <div className="flex items-center gap-3 px-4 py-2.5 bg-blue-50 border border-blue-200 rounded-xl">
              <span className="text-[12px] text-blue-800 font-semibold">{selected.size} record{selected.size !== 1 ? "s" : ""} selected</span>
              <button
                onClick={resendSelected}
                disabled={resendingSelected}
                className="ml-auto px-4 py-1.5 bg-[#00174b] text-white rounded-lg text-[12px] font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50"
              >
                {resendingSelected ? <Loader2 className="animate-spin h-3.5 w-3.5" /> : <span className="material-symbols-outlined text-sm">send</span>}
                Resend selected ({selected.size})
              </button>
              <button onClick={() => setSelected(new Set())} className="text-[11px] text-blue-600 hover:text-blue-800 font-medium">
                Clear
              </button>
            </div>
          )}

          {/* Table */}
          <div className="overflow-x-auto rounded-xl border border-slate-200">
            <table className="w-full text-[12px]">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200">
                  <th className="w-10 px-3 py-3 text-center">
                    <input
                      type="checkbox"
                      checked={allSelected}
                      ref={el => { if (el) el.indeterminate = someSelected && !allSelected; }}
                      onChange={toggleAll}
                      disabled={failedCustomers.length === 0}
                      className="accent-[#00174b]"
                    />
                  </th>
                  <th className="px-4 py-3 text-left font-bold text-slate-500 uppercase tracking-widest text-[10px]">Party Code</th>
                  <th className="px-4 py-3 text-left font-bold text-slate-500 uppercase tracking-widest text-[10px]">Email</th>
                  <th className="px-4 py-3 text-left font-bold text-slate-500 uppercase tracking-widest text-[10px]">Status</th>
                  <th className="px-4 py-3 text-left font-bold text-slate-500 uppercase tracking-widest text-[10px]">Bounce Type</th>
                  <th className="px-4 py-3 text-left font-bold text-slate-500 uppercase tracking-widest text-[10px]">Job / Segment</th>
                  <th className="px-4 py-3 text-left font-bold text-slate-500 uppercase tracking-widest text-[10px]">Date</th>
                  <th className="px-4 py-3 text-right font-bold text-slate-500 uppercase tracking-widest text-[10px]">Action</th>
                </tr>
              </thead>
              <tbody>
                {loadingFailed || fetchingFailed ? (
                  <tr>
                    <td colSpan={8} className="py-12 text-center">
                      <Loader2 className="animate-spin h-5 w-5 mx-auto text-slate-400" />
                    </td>
                  </tr>
                ) : failedCustomers.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-12 text-center">
                      <div className="flex flex-col items-center gap-2 text-slate-400">
                        <span className="material-symbols-outlined text-3xl">mark_email_read</span>
                        <span className="text-[12px]">No failed or bounced emails found</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  failedCustomers.map(c => (
                    <tr key={c.id} className={`border-b border-slate-100 hover:bg-slate-50 transition-colors ${selected.has(c.id) ? "bg-blue-50" : ""}`}>
                      <td className="px-3 py-3 text-center">
                        <input
                          type="checkbox"
                          checked={selected.has(c.id)}
                          onChange={() => toggleRow(c.id)}
                          className="accent-[#00174b]"
                        />
                      </td>
                      <td className="px-4 py-3 font-mono font-bold text-slate-800">{c.partyCode}</td>
                      <td className="px-4 py-3 text-slate-600 max-w-[220px] truncate">{c.email || "—"}</td>
                      <td className="px-4 py-3"><StatusBadge status={c.emailStatus} /></td>
                      <td className="px-4 py-3 text-slate-500">
                        {c.bounceType ? (
                          <span className={`text-[11px] font-medium ${c.bounceType === "Permanent" ? "text-red-600" : "text-orange-500"}`}>
                            {c.bounceType}
                          </span>
                        ) : "—"}
                      </td>
                      <td className="px-4 py-3 text-slate-500 max-w-[180px] truncate">
                        {c.fileName ? (
                          <span title={c.fileName}>{c.fileName.length > 22 ? c.fileName.slice(0, 22) + "…" : c.fileName}</span>
                        ) : "—"}
                        {c.segment && <span className="ml-1 text-slate-400">· {c.segment}</span>}
                      </td>
                      <td className="px-4 py-3 text-slate-400 whitespace-nowrap">
                        {formatDate(c.bouncedAt || c.emailSentAt)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => resendSingle(c)}
                          disabled={resendingIds.has(c.id)}
                          className="px-3 py-1.5 bg-[#00174b] text-white rounded-lg text-[11px] font-bold hover:bg-[#003ea8] flex items-center gap-1 ml-auto disabled:opacity-50"
                        >
                          {resendingIds.has(c.id)
                            ? <Loader2 className="animate-spin h-3 w-3" />
                            : <span className="material-symbols-outlined text-sm">send</span>
                          }
                          Resend
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-1">
              <span className="text-[11px] text-slate-400">
                Page {tablePage + 1} of {totalPages}
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => { setTablePage(p => p - 1); setSelected(new Set()); }}
                  disabled={tablePage === 0}
                  className="px-3 py-1.5 border border-slate-200 rounded-lg text-[12px] font-medium hover:bg-slate-50 disabled:opacity-40"
                >
                  Prev
                </button>
                <button
                  onClick={() => { setTablePage(p => p + 1); setSelected(new Set()); }}
                  disabled={tablePage >= totalPages - 1}
                  className="px-3 py-1.5 border border-slate-200 rounded-lg text-[12px] font-medium hover:bg-slate-50 disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            </div>
          )}

          {/* Tip linking to Client 360 for single client detail */}
          <div className="flex items-start gap-2 p-3 bg-slate-50 border border-slate-100 rounded-xl">
            <span className="material-symbols-outlined text-slate-400 text-base flex-shrink-0 mt-0.5">info</span>
            <p className="text-[11px] text-slate-500">
              For full client history and email override, go to{" "}
              <Link href="/clients" className="underline font-semibold hover:text-slate-700">Client 360</Link>
              {" "}→ search client → Actions tab.
            </p>
          </div>
        </div>

      </div>
    </div>
  );
}
