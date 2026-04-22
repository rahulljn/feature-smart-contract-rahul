"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { auditApi } from "@/lib/api";
import type { AuditLog } from "@/types";
import { Loader2 } from "lucide-react";
import { cn, fmtDateTime } from "@/lib/utils";
import { downloadCsv } from "@/lib/export";
import { toast } from "sonner";

const TABS = ["All", "Uploads", "Resends", "Template changes", "User changes"];
const tabToAction: Record<string, string[]> = {
  "All": [],
  "Uploads": ["UPLOAD"],
  "Resends": ["RESEND", "BULK_RESEND"],
  "Template changes": ["TEMPLATE_EDIT"],
  "User changes": ["USER_CREATE", "USER_UPDATE", "USER_DEACTIVATE"],
};

const actionPill: Record<string, string> = {
  "LOGIN": "pill-info", "LOGOUT": "pill-neu", "UPLOAD": "pill-info",
  "RESEND": "pill-warn", "BULK_RESEND": "pill-warn",
  "SUPPRESS": "pill-err", "UNSUPPRESS": "pill-ok",
  "TEMPLATE_EDIT": "pill-warn", "CERT_UPLOAD": "pill-info",
  "CONFIG_CHANGE": "pill-info", "USER_CREATE": "pill-info",
  "USER_UPDATE": "pill-neu", "USER_DEACTIVATE": "pill-err",
  "VIEW_PDF": "pill-neu", "DOWNLOAD_REPORT": "pill-neu",
};

const ACTION_LABEL: Record<string, string> = {
  LOGIN: "Login", LOGOUT: "Logout", UPLOAD: "File uploaded",
  RESEND: "Email resent", BULK_RESEND: "Bulk resend",
  SUPPRESS: "Address suppressed", UNSUPPRESS: "Address unsuppressed",
  TEMPLATE_EDIT: "Template edited", CERT_UPLOAD: "Certificate uploaded",
  CONFIG_CHANGE: "Config changed", USER_CREATE: "User created",
  USER_UPDATE: "User updated", USER_DEACTIVATE: "User deactivated",
  VIEW_PDF: "PDF viewed", DOWNLOAD_REPORT: "Report downloaded",
};

export default function AuditPage() {
  const [page, setPage] = useState(0);
  const [tab, setTab] = useState("All");
  const [search, setSearch] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  const tabActions = tabToAction[tab] ?? [];
  const actionsParam = tabActions.length > 0 ? tabActions.join(",") : undefined;

  const { data, isLoading } = useQuery({
    queryKey: ["audit", page, search, fromDate, toDate, tab],
    queryFn: () => auditApi.list(
      page, 20,
      search || undefined,
      fromDate ? `${fromDate}T00:00:00` : undefined,
      toDate ? `${toDate}T23:59:59` : undefined,
      actionsParam
    ),
  });

  const rawLogs: AuditLog[] = data?.data?.data?.content ?? [];
  const totalPages = data?.data?.data?.totalPages ?? 0;
  const logs = rawLogs;

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Audit Log</h2>
          <p className="text-slate-500 text-sm mt-1">Every operator action recorded for SEBI compliance · 7-year retention.</p>
        </div>
        <button onClick={() => downloadCsv("audit-log.csv", ["When","Actor","Action","Target","IP"], rawLogs.map(l => [l.eventTimestamp ?? "", l.userEmail ?? "", l.action, l.targetEntity ?? "", l.ipAddress ?? ""]))} className="px-4 py-2 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5">
          <span className="material-symbols-outlined text-base">download</span>Export CSV
        </button>
      </div>

      {/* Search + filter bar */}
      <div className="card p-4">
        <div className="flex flex-wrap gap-3 items-end">
          <div className="flex-1 min-w-[180px]">
            <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">Search</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-base">search</span>
              <input
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                placeholder="Actor, action, target..."
              />
            </div>
          </div>
          <div className="min-w-[130px]">
            <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">From</label>
            <input type="date" value={fromDate} onChange={e => setFromDate(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm" />
          </div>
          <div className="min-w-[130px]">
            <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1">To</label>
            <input type="date" value={toDate} onChange={e => setToDate(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm" />
          </div>
        </div>
      </div>

      {/* Tab filter + Table */}
      <div className="card overflow-hidden">
        <div className="flex items-center gap-2 px-5 py-3 border-b border-slate-100 bg-slate-50/70 overflow-x-auto">
          {TABS.map(t => (
            <button key={t} onClick={() => { setTab(t); setPage(0); }} className={cn("tab-btn", tab === t && "active")}>{t}</button>
          ))}
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
        ) : (
          <table className="w-full text-left">
            <thead><tr className="t-hd"><th className="px-5 py-3">When</th><th className="px-5 py-3">Actor</th><th className="px-5 py-3">Action</th><th className="px-5 py-3">Target</th><th className="px-5 py-3">IP</th></tr></thead>
            <tbody>
              {logs.map((log, i) => (
                <tr key={i} className="t-row">
                  <td className="px-5 py-3 text-[11px] text-slate-500 mono">{fmtDateTime(log.eventTimestamp)}</td>
                  <td className="px-5 py-3 text-xs font-semibold">{log.userEmail ?? "—"}</td>
                  <td className="px-5 py-3"><span className={cn("pill", actionPill[log.action] ?? "pill-neu")}>{ACTION_LABEL[log.action] ?? log.action}</span></td>
                  <td className="px-5 py-3 text-xs mono">{log.targetEntity ?? "—"}</td>
                  <td className="px-5 py-3 text-[11px] text-slate-500 mono">{log.ipAddress ?? "—"}</td>
                </tr>
              ))}
              {logs.length === 0 && (
                <tr><td colSpan={5} className="text-center text-slate-500 py-12 text-sm">No audit entries</td></tr>
              )}
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
