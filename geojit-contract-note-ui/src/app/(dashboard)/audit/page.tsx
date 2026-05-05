"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuditLog, AuditAction } from "@/types";
import { auditApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

const ACTION_TYPES: (AuditAction | "ALL")[] = ["ALL", "LOGIN", "LOGOUT", "UPLOAD", "RESEND", "BULK_RESEND", "CREATE", "UPDATE", "DELETE", "ACTIVATE", "TEMPLATE_EDIT"];

const ACTION_COLORS: Record<string, string> = {
  LOGIN:        "bg-green-50 text-green-700 border-green-200",
  LOGOUT:       "bg-gray-50 text-gray-600 border-gray-200",
  UPLOAD:       "bg-blue-50 text-blue-700 border-blue-200",
  RESEND:       "bg-amber-50 text-amber-700 border-amber-200",
  BULK_RESEND:  "bg-amber-50 text-amber-700 border-amber-200",
  CREATE:       "bg-purple-50 text-purple-700 border-purple-200",
  UPDATE:       "bg-sky-50 text-sky-700 border-sky-200",
  DELETE:        "bg-red-50 text-red-700 border-red-200",
  ACTIVATE:     "bg-teal-50 text-teal-700 border-teal-200",
  TEMPLATE_EDIT: "bg-orange-50 text-orange-700 border-orange-200",
};

const PAGE_SIZE = 10;

export default function AuditPage() {
  const user = useAuthStore((s) => s.user);
  const router = useRouter();

  const [search, setSearch] = useState("");
  const [actionFilter, setAction] = useState("ALL");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<{ content: AuditLog[]; totalElements: number; totalPages: number } | null>(null);

  useEffect(() => {
    if (user && user.role !== "ADMIN") router.replace("/dashboard");
  }, [user, router]);

  const fetchAudit = async () => {
    try {
      setLoading(true);
      setError(null);
      const status = actionFilter === "ALL" ? undefined : actionFilter;
      const res = await auditApi.list(page - 1, PAGE_SIZE, search, undefined, undefined, status);
      setData((res.data as any)?.data);
    } catch (err) {
      setError("Failed to load audit log");
      toast.error("Failed to load audit log");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAudit();
  }, [page, actionFilter, search]);

  if (user?.role !== "ADMIN") return null;

  const filtered = data?.content ?? [];

  const totalPages = data?.totalPages ?? 1;
  const paginated = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  function handleSearch(v: string) { setSearch(v); setPage(1); }
  function handleAction(v: AuditAction | "ALL") { setAction(v); setPage(1); }

  function downloadCsv() {
    if (filtered.length === 0) {
      toast.error("No data to export");
      return;
    }
    const headers = ["Timestamp","User","Action","Target","IP Address"];
    const rows = filtered.map((a) => [
      fmtDate(a.eventTimestamp),
      a.userEmail || "—",
      a.action,
      a.targetEntity || "—",
      a.ipAddress || "—",
    ]);
    const csv = [headers, ...rows].map((r) => r.join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = "audit-log.csv"; a.click();
    URL.revokeObjectURL(url);
    toast.success("Downloaded audit-log.csv");
  }

  function fmtDate(d: string): string {
    const date = new Date(d);
    return `${String(date.getFullYear())}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}:${String(date.getSeconds()).padStart(2, "0")}`;
  }

  if (error) {
    return (
      <div className="space-y-5 fade-up">
        <div className="flex items-center justify-between">
          <h1 className="font-bold text-2xl text-gray-900">Audit Log</h1>
        </div>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">{error}</div>
      </div>
    );
  }

  return (
    <div className="space-y-5 fade-up">

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="font-bold text-2xl text-gray-900">Audit Log</h1>
        <button className="flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors">
          <span className="material-symbols-outlined text-[16px]">download</span>
          Export CSV
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border p-3 flex flex-wrap items-center gap-3">
        {/* Search */}
        <div className="flex items-center gap-2 bg-gray-50 rounded-lg px-3 py-1.5 flex-1 min-w-[200px]">
          <span className="material-symbols-outlined text-gray-400 text-[18px]">search</span>
          <input
            value={search}
            onChange={(e) => handleSearch(e.target.value)}
            placeholder="Search user, action, target…"
            className="bg-transparent text-sm outline-none w-full text-gray-700 placeholder:text-gray-400"
          />
        </div>

        {/* Action type */}
        <select
          value={actionFilter}
          onChange={(e) => handleAction(e.target.value as AuditAction | "ALL")}
          className="border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm bg-white focus:outline-none text-gray-700"
        >
          {ACTION_TYPES.map((a) => (
            <option key={a} value={a}>{a === "ALL" ? "All Actions" : a.replace(/_/g, " ")}</option>
          ))}
        </select>

        <span className="text-xs text-gray-400 ml-auto">{data?.totalElements ?? 0} entries</span>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border overflow-hidden">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 gap-2">
            <span className="material-symbols-outlined text-gray-400 text-[36px] animate-spin">sync</span>
            <p className="text-gray-500 text-sm">Loading audit entries…</p>
          </div>
        ) : paginated.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-2">
            <span className="material-symbols-outlined text-gray-300 text-[36px]">manage_search</span>
            <p className="text-gray-400 text-sm">No audit entries match your filters</p>
          </div>
        ) : (
          <table className="w-full text-left">
            <thead>
              <tr className="t-hd">
                <th className="px-4 py-3">TIMESTAMP</th>
                <th className="px-4 py-3">USER</th>
                <th className="px-4 py-3">ACTION</th>
                <th className="px-4 py-3">TARGET</th>
                <th className="px-4 py-3">IP ADDRESS</th>
              </tr>
            </thead>
            <tbody>
              {paginated.map((entry) => (
                <tr key={entry.id} className="t-row">
                  <td className="px-4 py-3 mono text-xs text-gray-500 whitespace-nowrap">{fmtDate(entry.eventTimestamp)}</td>
                  <td className="px-4 py-3 text-xs text-gray-700 font-medium">{entry.userEmail}</td>
                  <td className="px-4 py-3">
                    <span className={cn(
                      "px-2 py-0.5 rounded-full text-[10px] font-bold border",
                      ACTION_COLORS[entry.action] ?? "bg-gray-50 text-gray-600 border-gray-200",
                    )}>
                      {entry.action.replace(/_/g, " ")}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-600 max-w-[300px] truncate">{entry.targetEntity || "—"}</td>
                  <td className="px-4 py-3 mono text-xs text-gray-400">{entry.ipAddress}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* Pagination */}
        <div className="px-4 py-3 border-t border-gray-100 flex items-center justify-between">
          <span className="text-xs text-gray-400">
            Showing {data?.totalElements === 0 ? 0 : (page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, data?.totalElements ?? 0)} of {data?.totalElements ?? 0}
          </span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1 || loading}
              className="px-3 py-1.5 text-xs border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              ← Prev
            </button>
            <span className="px-3 py-1.5 text-xs text-gray-600">
              Page {page} of {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page >= totalPages || loading}
              className="px-3 py-1.5 text-xs border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Next →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
