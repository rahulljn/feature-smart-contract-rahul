"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth";
import { cn } from "@/lib/utils";

const USE_MOCK = true;
void USE_MOCK;

const ACTION_TYPES = ["ALL", "LOGIN", "LOGOUT", "UPLOAD", "RESEND", "BULK_RESEND", "CREATE", "UPDATE", "DELETE", "ACTIVATE", "TEMPLATE_EDIT"];

const MOCK_AUDIT = [
  { id: "1",  timestamp: "2026-04-29 10:45:22", user: "admin@geojit.com",     action: "UPLOAD",        target: "JOB-0428-01 / EQUITY-COMBINEMARGIN", ip: "192.168.1.10" },
  { id: "2",  timestamp: "2026-04-29 10:30:11", user: "ops@geojit.com",        action: "RESEND",        target: "Party: ZYR175 (JOB-0428-01)",         ip: "192.168.1.14" },
  { id: "3",  timestamp: "2026-04-29 09:58:03", user: "admin@geojit.com",     action: "ACTIVATE",      target: "Certificate: GEOJIT-DR-2025",          ip: "192.168.1.10" },
  { id: "4",  timestamp: "2026-04-29 09:44:17", user: "admin@geojit.com",     action: "CREATE",        target: "User: hammad@geojit.com (OPS_MANAGER)", ip: "192.168.1.10" },
  { id: "5",  timestamp: "2026-04-29 09:22:55", user: "ops@geojit.com",        action: "BULK_RESEND",   target: "JOB-0428-01 — 318 records",           ip: "192.168.1.14" },
  { id: "6",  timestamp: "2026-04-29 08:55:40", user: "admin@geojit.com",     action: "TEMPLATE_EDIT", target: "Template: contract-note.html",         ip: "192.168.1.10" },
  { id: "7",  timestamp: "2026-04-29 08:31:09", user: "viewer@geojit.com",    action: "LOGIN",         target: "—",                                    ip: "10.0.0.22"    },
  { id: "8",  timestamp: "2026-04-29 08:12:44", user: "ops@geojit.com",        action: "LOGIN",         target: "—",                                    ip: "192.168.1.14" },
  { id: "9",  timestamp: "2026-04-28 17:03:29", user: "admin@geojit.com",     action: "UPDATE",        target: "SES Config: config-set-3 (activated)", ip: "192.168.1.10" },
  { id: "10", timestamp: "2026-04-28 16:45:00", user: "admin@geojit.com",     action: "DELETE",        target: "User: old.user@geojit.com",            ip: "192.168.1.10" },
];

const ACTION_COLORS: Record<string, string> = {
  LOGIN:        "bg-green-50 text-green-700 border-green-200",
  LOGOUT:       "bg-gray-50 text-gray-600 border-gray-200",
  UPLOAD:       "bg-blue-50 text-blue-700 border-blue-200",
  RESEND:       "bg-amber-50 text-amber-700 border-amber-200",
  BULK_RESEND:  "bg-amber-50 text-amber-700 border-amber-200",
  CREATE:       "bg-purple-50 text-purple-700 border-purple-200",
  UPDATE:       "bg-sky-50 text-sky-700 border-sky-200",
  DELETE:       "bg-red-50 text-red-700 border-red-200",
  ACTIVATE:     "bg-teal-50 text-teal-700 border-teal-200",
  TEMPLATE_EDIT:"bg-orange-50 text-orange-700 border-orange-200",
};

const PAGE_SIZE = 10;

export default function AuditPage() {
  const user   = useAuthStore((s) => s.user);
  const router = useRouter();

  useEffect(() => {
    if (user && user.role !== "ADMIN") router.replace("/dashboard");
  }, [user, router]);

  const [search,     setSearch]     = useState("");
  const [actionFilter, setAction]   = useState("ALL");
  const [page,       setPage]       = useState(1);

  if (user?.role !== "ADMIN") return null;

  const filtered = MOCK_AUDIT.filter((entry) => {
    const matchAction = actionFilter === "ALL" || entry.action === actionFilter;
    const q = search.toLowerCase();
    const matchSearch = !q || entry.user.toLowerCase().includes(q) || entry.target.toLowerCase().includes(q) || entry.action.toLowerCase().includes(q);
    return matchAction && matchSearch;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const pageData   = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  function handleSearch(v: string) { setSearch(v); setPage(1); }
  function handleAction(v: string) { setAction(v); setPage(1); }

  return (
    <div className="space-y-5 fade-up max-w-6xl">

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
          onChange={(e) => handleAction(e.target.value)}
          className="border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm bg-white focus:outline-none text-gray-700"
        >
          {ACTION_TYPES.map((a) => (
            <option key={a} value={a}>{a === "ALL" ? "All Actions" : a.replace(/_/g, " ")}</option>
          ))}
        </select>

        <span className="text-xs text-gray-400 ml-auto">{filtered.length} entries</span>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border overflow-hidden">
        {pageData.length === 0 ? (
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
              {pageData.map((entry) => (
                <tr key={entry.id} className="t-row">
                  <td className="px-4 py-3 mono text-xs text-gray-500 whitespace-nowrap">{entry.timestamp}</td>
                  <td className="px-4 py-3 text-xs text-gray-700 font-medium">{entry.user}</td>
                  <td className="px-4 py-3">
                    <span className={cn(
                      "px-2 py-0.5 rounded-full text-[10px] font-bold border",
                      ACTION_COLORS[entry.action] ?? "bg-gray-50 text-gray-600 border-gray-200",
                    )}>
                      {entry.action.replace(/_/g, " ")}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-600 max-w-[300px] truncate">{entry.target}</td>
                  <td className="px-4 py-3 mono text-xs text-gray-400">{entry.ip}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* Pagination */}
        <div className="px-4 py-3 border-t border-gray-100 flex items-center justify-between">
          <span className="text-xs text-gray-400">
            Showing {filtered.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, filtered.length)} of {filtered.length}
          </span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              className="px-3 py-1.5 text-xs border border-gray-200 rounded-lg disabled:opacity-40 hover:bg-gray-50 transition-colors"
            >
              ← Prev
            </button>
            <span className="px-3 py-1.5 text-xs text-gray-600">
              Page {page} of {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
              className="px-3 py-1.5 text-xs border border-gray-200 rounded-lg disabled:opacity-40 hover:bg-gray-50 transition-colors"
            >
              Next →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
