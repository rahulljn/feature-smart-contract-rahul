"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { SparklineChart } from "@/components/ui/sparkline";
import { toast } from "sonner";

const USE_MOCK = true;
void USE_MOCK;

// ─── Mock data ────────────────────────────────────────────────────────────────
const MOCK_STATS = [
  { label: "SENDS",      value: "4,218",  color: "#497cff", sparkline: [380, 420, 390, 450, 410, 480, 420] },
  { label: "REJECTS",    value: "17",     color: "#ef4444", sparkline: [2, 3, 2, 4, 2, 3, 2] },
  { label: "BOUNCES",    value: "80",     color: "#f59e0b", sparkline: [10, 12, 9, 14, 11, 13, 11] },
  { label: "COMPLAINTS", value: "1",      color: "#a855f7", sparkline: [0, 1, 0, 0, 1, 0, 0] },
];

const MOCK_SUPPRESSION = [
  { id: "1", email: "j***@gmail.com",        reason: "Bounce",    addedDate: "2026-04-28", permanent: true  },
  { id: "2", email: "r***@yahoo.com",        reason: "Complaint", addedDate: "2026-04-27", permanent: true  },
  { id: "3", email: "s***@hotmail.com",      reason: "Manual",    addedDate: "2026-04-25", permanent: false },
  { id: "4", email: "p***@rediffmail.com",   reason: "Bounce",    addedDate: "2026-04-20", permanent: true  },
  { id: "5", email: "a***@geojit.com",       reason: "Manual",    addedDate: "2026-04-15", permanent: false },
];

const QUOTA_USED  = 18700;
const QUOTA_MAX   = 21600;
const QUOTA_PCT   = Math.round((QUOTA_USED / QUOTA_MAX) * 100);

function quotaBarColor(pct: number) {
  if (pct > 95) return "bg-red-500";
  if (pct > 80) return "bg-amber-400";
  return "bg-green-500";
}

function StatusDot({ ok }: { ok: boolean }) {
  return (
    <span className={cn("inline-block w-2 h-2 rounded-full mr-1.5", ok ? "bg-green-500" : "bg-red-500")} />
  );
}

export default function EmailAnalyticsPage() {
  const [period, setPeriod]       = useState("7d");
  const [suppression, setSuppression] = useState(MOCK_SUPPRESSION);
  const [showAddForm, setShowAddForm] = useState(false);
  const [newEmail, setNewEmail]   = useState("");
  const [newReason, setNewReason] = useState("Bounce");
  const [newPermanent, setNewPermanent] = useState(false);

  function removeEntry(id: string) {
    setSuppression((prev) => prev.filter((e) => e.id !== id));
    toast.success("Removed from suppression list");
  }

  function addEntry() {
    if (!newEmail.trim()) return;
    setSuppression((prev) => [
      ...prev,
      { id: String(Date.now()), email: newEmail.trim(), reason: newReason, addedDate: new Date().toISOString().slice(0, 10), permanent: newPermanent },
    ]);
    setNewEmail(""); setNewReason("Bounce"); setNewPermanent(false);
    setShowAddForm(false);
    toast.success("Added to suppression list");
  }

  return (
    <div className="space-y-5 fade-up">

      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h1 className="font-bold text-2xl text-gray-900">Email Analytics</h1>
          <span className="px-2 py-0.5 bg-blue-100 text-blue-700 text-xs font-medium rounded-full">
            ap-south-1
          </span>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
            className="border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm bg-white focus:outline-none"
          >
            <option value="7d">Last 7 days</option>
            <option value="14d">Last 14 days</option>
            <option value="30d">Last 30 days</option>
          </select>
          <button className="flex items-center gap-1.5 px-3 py-1.5 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors">
            <span className="material-symbols-outlined text-[16px]">refresh</span>
            Refresh
          </button>
        </div>
      </div>

      {/* Stats band */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {MOCK_STATS.map((tile) => (
          <div key={tile.label} className="bg-white rounded-xl border border-gray-100 p-4">
            <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-1">
              {tile.label}
            </div>
            <div className="text-xl font-bold text-gray-900 mb-2">{tile.value}</div>
            <SparklineChart data={tile.sparkline} color={tile.color} height={50} />
          </div>
        ))}
      </div>

      {/* Account Health Banner */}
      <div className="bg-white rounded-xl border p-4">
        <div className="text-sm font-semibold text-gray-900 mb-3">Account Health</div>
        <div className="grid md:grid-cols-3 gap-6">
          {/* Quota */}
          <div>
            <div className="text-xs text-gray-500 mb-1">Daily Send Quota</div>
            <div className="text-sm font-medium text-gray-800 mb-1.5">
              {QUOTA_USED.toLocaleString()} of {QUOTA_MAX.toLocaleString()} sends
              <span className={cn("ml-2 text-xs font-bold", QUOTA_PCT > 95 ? "text-red-600" : QUOTA_PCT > 80 ? "text-amber-600" : "text-green-600")}>
                ({QUOTA_PCT}%)
              </span>
            </div>
            <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
              <div
                className={cn("h-full rounded-full transition-all", quotaBarColor(QUOTA_PCT))}
                style={{ width: `${QUOTA_PCT}%` }}
              />
            </div>
          </div>
          {/* Bounce Rate */}
          <div>
            <div className="text-xs text-gray-500 mb-1">Bounce Rate (7d)</div>
            <div className="flex items-center text-sm font-medium text-gray-800">
              <StatusDot ok={true} />
              1.9% <span className="text-xs text-gray-400 ml-1">(threshold: 5%)</span>
            </div>
          </div>
          {/* Complaint Rate */}
          <div>
            <div className="text-xs text-gray-500 mb-1">Complaint Rate (7d)</div>
            <div className="flex items-center text-sm font-medium text-gray-800">
              <StatusDot ok={true} />
              0.02% <span className="text-xs text-gray-400 ml-1">(threshold: 0.1%)</span>
            </div>
          </div>
        </div>
      </div>

      {/* Suppression List */}
      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100 flex items-center gap-2">
          <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs font-medium rounded-full">
            {suppression.length} entries
          </span>
          <button
            onClick={() => setShowAddForm((v) => !v)}
            className="ml-auto flex items-center gap-1.5 px-3 py-1.5 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">add</span>
            Add to Suppression
          </button>
        </div>

        {/* Inline add form */}
        {showAddForm && (
          <div className="px-5 py-3 bg-blue-50 border-b border-blue-100 flex flex-wrap items-end gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600 font-medium">Email address</label>
              <input
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                placeholder="user@example.com"
                className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#00174b]/20 w-56"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600 font-medium">Reason</label>
              <select
                value={newReason}
                onChange={(e) => setNewReason(e.target.value)}
                className="border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm bg-white"
              >
                <option>Bounce</option>
                <option>Complaint</option>
                <option>Manual</option>
              </select>
            </div>
            <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer pb-1.5">
              <input
                type="checkbox"
                checked={newPermanent}
                onChange={(e) => setNewPermanent(e.target.checked)}
                className="rounded"
              />
              Permanent
            </label>
            <button
              onClick={addEntry}
              className="px-4 py-1.5 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors"
            >
              Save
            </button>
            <button
              onClick={() => setShowAddForm(false)}
              className="px-3 py-1.5 border border-gray-200 rounded-lg text-sm text-gray-600 hover:bg-white"
            >
              Cancel
            </button>
          </div>
        )}

        <table className="w-full text-left">
          <thead>
            <tr className="t-hd">
              <th className="px-4 py-3">EMAIL</th>
              <th className="px-4 py-3">REASON</th>
              <th className="px-4 py-3">ADDED DATE</th>
              <th className="px-4 py-3 text-center">PERMANENT</th>
              <th className="px-4 py-3 w-12" />
            </tr>
          </thead>
          <tbody>
            {suppression.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center py-10 text-gray-400 text-sm">
                  Suppression list is empty
                </td>
              </tr>
            ) : (
              suppression.map((entry) => (
                <tr key={entry.id} className="t-row">
                  <td className="px-4 py-3 mono text-xs text-gray-700">{entry.email}</td>
                  <td className="px-4 py-3">
                    <span className={cn(
                      "px-2 py-0.5 rounded-full text-[10px] font-medium",
                      entry.reason === "Bounce"    && "bg-amber-50 text-amber-700",
                      entry.reason === "Complaint" && "bg-red-50 text-red-700",
                      entry.reason === "Manual"    && "bg-gray-100 text-gray-600",
                    )}>
                      {entry.reason}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-500">{entry.addedDate}</td>
                  <td className="px-4 py-3 text-center text-sm">
                    {entry.permanent ? (
                      <span className="text-green-600">✓</span>
                    ) : (
                      <span className="text-gray-300">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => removeEntry(entry.id)}
                      className="text-gray-400 hover:text-red-500 transition-colors"
                      title="Remove from suppression"
                    >
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
