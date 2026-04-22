"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { clientsApi, jobsApi } from "@/lib/api";
import type { JobCustomer, EmailEvent } from "@/types";
import { Loader2 } from "lucide-react";
import { cn, fmtDate, fmtDateTime } from "@/lib/utils";
import { downloadCsv } from "@/lib/export";
import { toast } from "sonner";
import Link from "next/link";

const PDF_LABEL: Record<string, string> = { PENDING: "Pending", GENERATED: "Ready", FAILED: "Failed" };
const EMAIL_LABEL: Record<string, string> = { PENDING: "Pending", SENT: "Dispatched", DELIVERED: "Delivered", BOUNCED: "Bounced", FAILED: "Failed", SKIPPED: "Failed" };
const EVENT_LABEL: Record<string, string> = { DELIVERY: "Delivered", BOUNCE: "Bounced", COMPLAINT: "Spam report", SEND: "Sent", EMAIL_SENT: "Sent" };

export default function ClientDetailPage() {
  const { partyCode } = useParams<{ partyCode: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState("stmt");
  const [openingPdf, setOpeningPdf] = useState<string | null>(null);
  const [newEmail, setNewEmail] = useState("");

  const { data: histRes, isLoading: loadingHistory } = useQuery({
    queryKey: ["client-history", partyCode],
    queryFn: () => clientsApi.search(partyCode),
  });
  // API returns { partyCode, clientDetails, processHistory, totalContracts }
  const _histRaw = histRes?.data?.data;
  const clientDetails: Record<string, string> = _histRaw?.clientDetails ?? {};
  const history: JobCustomer[] = Array.isArray(_histRaw?.processHistory)
    ? _histRaw.processHistory
    : Array.isArray(_histRaw) ? _histRaw : [];

  const { data: pdfsRes, isLoading: loadingPdfs } = useQuery({
    queryKey: ["client-pdfs", partyCode],
    queryFn: () => clientsApi.pdfs(partyCode),
  });
  const _pdfsRaw = pdfsRes?.data?.data;
  // API returns { s3Key, size, lastModified, metadata }
  const pdfs: Array<{ s3Key: string; size?: number; lastModified?: string; metadata?: Record<string, string> }> = Array.isArray(_pdfsRaw) ? _pdfsRaw : [];

  const { data: timelineRes, isLoading: loadingTimeline } = useQuery({
    queryKey: ["client-timeline", partyCode],
    queryFn: () => clientsApi.timeline(partyCode),
  });
  const _tlRaw = timelineRes?.data?.data;
  const timeline: EmailEvent[] = Array.isArray(_tlRaw) ? _tlRaw : [];

  const handleOpenPdf = async (s3Key: string) => {
    setOpeningPdf(s3Key);
    try {
      const res = await clientsApi.pdfUrl(partyCode, s3Key);
      const url: string = res.data?.data?.url ?? res.data?.data;
      if (url) window.open(url, "_blank");
      else toast.error("Could not get PDF link — URL not returned");
    } catch {
      toast.error("Failed to open PDF — check S3 configuration");
    } finally {
      setOpeningPdf(null);
    }
  };

  const { mutate: resendLatest, isPending: resendingLatest } = useMutation({
    mutationFn: () => {
      const latestJob = history[0];
      if (!latestJob?.jobId) throw new Error("No job found");
      return jobsApi.resend(latestJob.jobId, partyCode);
    },
    onSuccess: () => toast.success(`Resend queued for ${partyCode}`),
    onError: () => toast.error("Resend failed"),
  });

  const { mutate: updateEmail, isPending: updatingEmail } = useMutation({
    mutationFn: () => clientsApi.updateEmail(partyCode, newEmail.trim()),
    onSuccess: () => {
      toast.success(`Email updated to ${newEmail.trim()}`);
      setNewEmail("");
      qc.invalidateQueries({ queryKey: ["client-history", partyCode] });
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "Failed to update email";
      toast.error(msg);
    },
  });

  const delivered = history.filter(h => h.emailStatus === "DELIVERED").length;
  const bounced = history.filter(h => h.emailStatus === "BOUNCED").length;
  const failed = history.filter(h => h.emailStatus === "FAILED" || h.pdfStatus === "FAILED").length;
  const latestEmail = history[0]?.email ?? "—";
  const initials = partyCode.slice(0, 2).toUpperCase();

  const tabs = [
    { id: "stmt", label: "Statements", count: history.length },
    { id: "dlog", label: "Delivery log" },
    { id: "act", label: "Actions" },
    { id: "dl", label: "Export" },
  ];

  return (
    <div className="fade-up space-y-0">
      {/* Back nav */}
      <div className="bg-white border-b border-slate-200 px-6 py-3 sticky top-0 z-10">
        <div className="max-w-[1600px] mx-auto flex items-center gap-3">
          <Link href="/clients" className="p-2 text-slate-500 hover:text-[#00174b] hover:bg-slate-100 rounded-lg">
            <span className="material-symbols-outlined">arrow_back</span>
          </Link>
          <div className="flex items-center gap-3 flex-1">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#00174b] to-[#003ea8] text-white flex items-center justify-center font-bold">{initials}</div>
            <div>
              <h2 className="text-lg font-extrabold text-slate-900 mono">{partyCode}</h2>
              <p className="text-[11px] text-slate-500 mono">{latestEmail}</p>
            </div>
          </div>
          <div className="flex gap-3">
            <div className="card p-3"><div className="text-[10px] text-slate-500 uppercase font-bold">Contracts</div><div className="text-lg font-extrabold mono">{history.length}</div></div>
            <div className="card p-3"><div className="text-[10px] text-emerald-600 uppercase font-bold">Delivered</div><div className="text-lg font-extrabold mono text-emerald-600">{delivered}</div></div>
            <div className="card p-3"><div className="text-[10px] text-amber-600 uppercase font-bold">Bounced</div><div className="text-lg font-extrabold mono text-amber-600">{bounced}</div></div>
            <div className="card p-3"><div className="text-[10px] text-rose-600 uppercase font-bold">Failed</div><div className="text-lg font-extrabold mono text-rose-600">{failed}</div></div>
          </div>
        </div>
        <div className="max-w-[1600px] mx-auto flex gap-1 mt-2">
          {tabs.map(t => (
            <button key={t.id} onClick={() => setActiveTab(t.id)} className={cn("tab-btn flex items-center gap-1", activeTab === t.id && "active")}>
              {t.label}
              {t.count !== undefined && <span className="ml-1">({t.count})</span>}
            </button>
          ))}
        </div>
      </div>

      <div className="p-6 max-w-[1600px] mx-auto">
        {/* STATEMENTS TAB */}
        {activeTab === "stmt" && (
          <div className="space-y-2">
            {loadingHistory ? (
              <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
            ) : history.length === 0 ? (
              <div className="card p-12 text-center">
                <span className="material-symbols-outlined text-5xl text-slate-200">description</span>
                <div className="text-slate-400 font-semibold mt-3 text-sm">No contract history found</div>
              </div>
            ) : (
              history.map(h => (
                <div key={`${h.jobId}-${h.partyCode}`} className="card p-3.5 flex items-center gap-3 hover:border-[#497cff] cursor-pointer">
                  <div className="p-2 bg-emerald-50 rounded-lg"><span className="material-symbols-outlined text-emerald-600">picture_as_pdf</span></div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-semibold text-slate-900">{h.segment ?? "Contract Note"} · {h.tradeDate ? fmtDate(h.tradeDate) : "—"}</div>
                    <div className="text-[11px] text-slate-500 mono">{h.fileName ?? "—"}</div>
                  </div>
                  <span className={cn("pill", h.emailStatus === "DELIVERED" ? "pill-ok" : h.emailStatus === "BOUNCED" ? "pill-err" : h.emailStatus === "FAILED" ? "pill-err" : "pill-neu")}>{EMAIL_LABEL[h.emailStatus] ?? PDF_LABEL[h.pdfStatus] ?? (h.emailStatus || h.pdfStatus)}</span>
                  <button onClick={() => h.pdfS3Key && handleOpenPdf(h.pdfS3Key)} className="text-[11px] font-bold text-[#003ea8] hover:underline">View</button>
                </div>
              ))
            )}
          </div>
        )}

        {/* DELIVERY LOG TAB */}
        {activeTab === "dlog" && (
          <div className="card overflow-hidden">
            <table className="w-full text-left">
              <thead><tr className="t-hd"><th className="px-4 py-3">Job ID</th><th className="px-4 py-3">File</th><th className="px-4 py-3">PDF</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Segment</th><th className="px-4 py-3">Processed</th></tr></thead>
              <tbody>
                {loadingHistory ? (
                  <tr><td colSpan={6} className="text-center py-12"><Loader2 className="animate-spin text-[#00174b] inline" /></td></tr>
                ) : history.map(h => (
                  <tr key={`${h.jobId}-${h.partyCode}`} className="t-row">
                    <td className="px-4 py-3 mono text-[11px] text-slate-500">{h.jobId ? h.jobId.slice(0, 8) + "…" : "—"}</td>
                    <td className="px-4 py-3 text-[12px] max-w-[160px] truncate">{h.fileName ?? "—"}</td>
                    <td className="px-4 py-3"><span className={cn("pill", h.pdfStatus === "GENERATED" ? "pill-ok" : h.pdfStatus === "FAILED" ? "pill-err" : "pill-neu")}>{PDF_LABEL[h.pdfStatus] ?? h.pdfStatus}</span></td>
                    <td className="px-4 py-3"><span className={cn("pill", h.emailStatus === "DELIVERED" ? "pill-ok" : h.emailStatus === "BOUNCED" ? "pill-err" : h.emailStatus === "FAILED" ? "pill-err" : "pill-neu")}>{EMAIL_LABEL[h.emailStatus] ?? h.emailStatus}</span></td>
                    <td className="px-4 py-3 text-[12px]">{h.segment ?? "—"}</td>
                    <td className="px-4 py-3 text-[11px] text-slate-500">{h.emailSentAt ? new Date(h.emailSentAt.endsWith("Z") ? h.emailSentAt : h.emailSentAt + "Z").toLocaleString("en-GB", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit", timeZone: "Asia/Kolkata" }) : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* ACTIONS TAB */}
        {activeTab === "act" && (
          <div className="space-y-3">
            <div className="card p-5 space-y-4">
              <div>
                <div className="text-sm font-bold text-slate-900">Update registered email</div>
                <p className="text-[12px] text-slate-500 mt-0.5">Current: <span className="mono font-semibold text-slate-700">{latestEmail}</span></p>
              </div>
              <div className="flex gap-3">
                <input
                  className="flex-1 px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm mono focus:outline-none focus:ring-2 focus:ring-[#497cff]/30"
                  placeholder="Enter new email address"
                  value={newEmail}
                  onChange={e => setNewEmail(e.target.value)}
                  type="email"
                />
                <button
                  onClick={() => { if (!newEmail.trim()) { toast.warning("Enter a new email address"); return; } updateEmail(); }}
                  disabled={updatingEmail || !newEmail.trim()}
                  className="px-4 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50"
                >
                  {updatingEmail ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-sm">mail</span>}
                  Update email
                </button>
              </div>
              <p className="text-[11px] text-slate-400">This updates the email across all contract note records for this client. It does not automatically resend — use Resend below.</p>
            </div>
            <div className="card p-5 space-y-4">
              <div className="text-sm font-bold text-slate-900">Resend latest contract note</div>
              <p className="text-[12px] text-slate-500">Re-sends the most recent contract note to this client. If the PDF was already generated, it is sent immediately; otherwise the contract note is regenerated first.</p>
              <button onClick={() => resendLatest()} disabled={resendingLatest || history.length === 0} className="px-4 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50">
                {resendingLatest ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-sm">send</span>}
                Resend
              </button>
            </div>
          </div>
        )}

        {/* DOWNLOAD LOGS TAB */}
        {activeTab === "dl" && (
          <div className="space-y-3">
            <div className="card p-5">
              <div className="flex items-center justify-between mb-3">
                <div className="text-sm font-bold text-slate-900">Download delivery logs</div>
                <button onClick={() => downloadCsv(`delivery-log-${partyCode}.csv`, ["Event","Recipient","Bounce type","When"], timeline.map(ev => [EVENT_LABEL[ev.eventType] ?? ev.eventType ?? "", ev.recipientEmail ?? "", ev.bounceType ?? "", ev.eventTimestamp ?? ""]))} className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-base">download</span>Export CSV
                </button>
              </div>
              <p className="text-[12px] text-slate-500">Export all delivery events for this client in CSV format. Includes timestamps, delivery status, and bounce details.</p>
            </div>
            {loadingTimeline ? (
              <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
            ) : timeline.length > 0 ? (
              <div className="card overflow-hidden">
                <table className="w-full text-left">
                  <thead><tr className="t-hd"><th className="px-4 py-3">Event</th><th className="px-4 py-3">Recipient</th><th className="px-4 py-3">Bounce type</th><th className="px-4 py-3">When</th></tr></thead>
                  <tbody>
                    {timeline.map(ev => (
                      <tr key={ev.id} className="t-row">
                        <td className="px-4 py-3"><span className={cn("pill", ev.eventType === "DELIVERY" ? "pill-ok" : ev.eventType === "BOUNCE" ? "pill-err" : ev.eventType === "COMPLAINT" ? "pill-err" : "pill-neu")}>{{DELIVERY:"Delivered",BOUNCE:"Bounced",COMPLAINT:"Spam report",SEND:"Sent"}[ev.eventType] ?? ev.eventType}</span></td>
                        <td className="px-4 py-3 text-[11px] mono text-slate-600">{ev.recipientEmail ?? "—"}</td>
                        <td className="px-4 py-3 text-[11px] text-slate-500">{ev.bounceType ?? "—"}</td>
                        <td className="px-4 py-3 text-[11px] text-slate-500">{ev.eventTimestamp ? fmtDateTime(ev.eventTimestamp) : "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="card p-12 text-center">
                <span className="material-symbols-outlined text-4xl text-slate-200">mail</span>
                <div className="text-slate-400 font-semibold mt-3 text-sm">No email events recorded</div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
