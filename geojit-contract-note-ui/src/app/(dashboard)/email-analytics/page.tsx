"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { cn } from "@/lib/utils";
import { SparklineChart } from "@/components/ui/sparkline";
import { configApi, suppressionApi } from "@/lib/api";
import type { SesStatistics, SuppressionEntry } from "@/types";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

function periodDates(period: string): { startDate: string; endDate: string } {
  const today = new Date();
  const days = period === "30d" ? 30 : period === "14d" ? 14 : 7;
  const start = new Date(today.getTime() - days * 86400000);
  return {
    startDate: start.toISOString().split("T")[0],
    endDate: today.toISOString().split("T")[0],
  };
}

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
  const qc = useQueryClient();
  const [period, setPeriod] = useState("7d");
  const [showAddForm, setShowAddForm] = useState(false);
  const [newEmail, setNewEmail] = useState("");
  const [newReason, setNewReason] = useState("Bounce");

  const { startDate, endDate } = periodDates(period);

  const { data: statsRes, isLoading: loadingStats } = useQuery({
    queryKey: ["ses-statistics", period],
    queryFn: () => configApi.sesStatistics(startDate, endDate),
    staleTime: 5 * 60 * 1000,
  });
  const stats: SesStatistics | undefined = statsRes?.data?.data;

  const { data: suppressionRes, isLoading: loadingSupp } = useQuery({
    queryKey: ["suppression-list"],
    queryFn: () => suppressionApi.list(0, 100),
    staleTime: 60 * 1000,
  });
  const suppression: SuppressionEntry[] = suppressionRes?.data?.data?.content ?? [];

  const addMutation = useMutation({
    mutationFn: () => suppressionApi.add(newEmail.trim(), newReason),
    onSuccess: () => {
      toast.success("Added to suppression list");
      setNewEmail(""); setNewReason("Bounce"); setShowAddForm(false);
      qc.invalidateQueries({ queryKey: ["suppression-list"] });
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "Failed to add email";
      toast.error(msg);
    },
  });

  const removeMutation = useMutation({
    mutationFn: (email: string) => suppressionApi.remove(email),
    onSuccess: () => {
      toast.success("Removed from suppression list");
      qc.invalidateQueries({ queryKey: ["suppression-list"] });
    },
    onError: () => toast.error("Failed to remove"),
  });

  const dataPoints = stats?.dataPoints ?? [];
  const statTiles = [
    {
      label: "SENDS",
      value: stats ? dataPoints.reduce((s, d) => s + (d.sends ?? 0), 0).toLocaleString() : "—",
      color: "#497cff",
      sparkline: dataPoints.map(d => d.sends ?? 0),
    },
    {
      label: "REJECTS",
      value: stats ? dataPoints.reduce((s, d) => s + (d.rejects ?? 0), 0).toLocaleString() : "—",
      color: "#ef4444",
      sparkline: dataPoints.map(d => d.rejects ?? 0),
    },
    {
      label: "BOUNCES",
      value: stats ? dataPoints.reduce((s, d) => s + (d.bounces ?? 0), 0).toLocaleString() : "—",
      color: "#f59e0b",
      sparkline: dataPoints.map(d => d.bounces ?? 0),
    },
    {
      label: "COMPLAINTS",
      value: stats ? dataPoints.reduce((s, d) => s + (d.complaints ?? 0), 0).toLocaleString() : "—",
      color: "#a855f7",
      sparkline: dataPoints.map(d => d.complaints ?? 0),
    },
  ];

  const quotaPct = stats?.quotaUsedPercent ?? 0;

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
          <button
            onClick={() => { qc.invalidateQueries({ queryKey: ["ses-statistics", period] }); }}
            className="flex items-center gap-1.5 px-3 py-1.5 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">refresh</span>
            Refresh
          </button>
        </div>
      </div>

      {/* Stats band */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {statTiles.map((tile) => (
          <div key={tile.label} className="bg-white rounded-xl border border-gray-100 p-4">
            <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-1">
              {tile.label}
            </div>
            {loadingStats ? (
              <div className="h-8 flex items-center"><Loader2 className="animate-spin text-gray-300 h-4 w-4" /></div>
            ) : (
              <div className="text-xl font-bold text-gray-900 mb-2">{tile.value}</div>
            )}
            <SparklineChart data={tile.sparkline.length > 0 ? tile.sparkline : [0]} color={tile.color} height={50} />
          </div>
        ))}
      </div>

      {/* Account Health Banner */}
      <div className="bg-white rounded-xl border p-4">
        <div className="text-sm font-semibold text-gray-900 mb-3">Account Health</div>
        {loadingStats ? (
          <div className="flex justify-center py-4"><Loader2 className="animate-spin text-gray-300 h-5 w-5" /></div>
        ) : (
          <div className="grid md:grid-cols-3 gap-6">
            {/* Quota */}
            <div>
              <div className="text-xs text-gray-500 mb-1">Daily Send Quota</div>
              <div className="text-sm font-medium text-gray-800 mb-1.5">
                {(stats?.sentLast24h ?? 0).toLocaleString()} of {(stats?.dailySendQuota ?? 0).toLocaleString()} sends
                <span className={cn("ml-2 text-xs font-bold", quotaPct > 95 ? "text-red-600" : quotaPct > 80 ? "text-amber-600" : "text-green-600")}>
                  ({quotaPct.toFixed(1)}%)
                </span>
              </div>
              <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                <div
                  className={cn("h-full rounded-full transition-all", quotaBarColor(quotaPct))}
                  style={{ width: `${Math.min(quotaPct, 100)}%` }}
                />
              </div>
            </div>
            {/* Bounce Rate */}
            <div>
              <div className="text-xs text-gray-500 mb-1">Bounce Rate ({period})</div>
              <div className="flex items-center text-sm font-medium text-gray-800">
                <StatusDot ok={(stats?.bounceRate ?? 0) < 5} />
                {((stats?.bounceRate ?? 0)).toFixed(2)}%
                <span className="text-xs text-gray-400 ml-1">(threshold: 5%)</span>
              </div>
            </div>
            {/* Complaint Rate */}
            <div>
              <div className="text-xs text-gray-500 mb-1">Complaint Rate ({period})</div>
              <div className="flex items-center text-sm font-medium text-gray-800">
                <StatusDot ok={(stats?.complaintRate ?? 0) < 0.1} />
                {((stats?.complaintRate ?? 0)).toFixed(3)}%
                <span className="text-xs text-gray-400 ml-1">(threshold: 0.1%)</span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Suppression List */}
      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100 flex items-center gap-2">
          <span className="font-semibold text-sm text-gray-900">Suppression List</span>
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
            <button
              onClick={() => { if (!newEmail.trim()) return; addMutation.mutate(); }}
              disabled={addMutation.isPending || !newEmail.trim()}
              className="px-4 py-1.5 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors disabled:opacity-50 flex items-center gap-1.5"
            >
              {addMutation.isPending && <Loader2 className="animate-spin h-3 w-3" />}
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
              <th className="px-4 py-3 w-12" />
            </tr>
          </thead>
          <tbody>
            {loadingSupp ? (
              <tr><td colSpan={4} className="text-center py-8"><Loader2 className="animate-spin text-gray-300 h-5 w-5 inline" /></td></tr>
            ) : suppression.length === 0 ? (
              <tr>
                <td colSpan={4} className="text-center py-10 text-gray-400 text-sm">
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
                      !entry.reason || entry.reason === "Manual" ? "bg-gray-100 text-gray-600" : "",
                    )}>
                      {entry.reason ?? "Manual"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-500">
                    {entry.addedAt ? new Date(entry.addedAt.endsWith("Z") ? entry.addedAt : entry.addedAt + "Z").toLocaleDateString("en-GB") : "—"}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => removeMutation.mutate(entry.email)}
                      disabled={removeMutation.isPending}
                      className="text-gray-400 hover:text-red-500 transition-colors disabled:opacity-50"
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
