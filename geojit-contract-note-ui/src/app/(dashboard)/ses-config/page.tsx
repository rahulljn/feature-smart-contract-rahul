"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { configApi, suppressionApi } from "@/lib/api";
import { downloadCsv } from "@/lib/export";
import type { SesConfig, SuppressionEntry, SesStatistics } from "@/types";
import { Loader2, RefreshCw } from "lucide-react";
import { cn, fmtDate, toUtcDate } from "@/lib/utils";
import { toast } from "sonner";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from "recharts";

function today() { return new Date().toISOString().slice(0, 10); }
function daysAgo(n: number) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

export default function SesConfigPage() {
  const qc = useQueryClient();
  const [suppSearch, setSuppSearch] = useState("");
  const [startDate, setStartDate] = useState(daysAgo(6));
  const [endDate, setEndDate] = useState(today());
  const [appliedStart, setAppliedStart] = useState(daysAgo(6));
  const [appliedEnd, setAppliedEnd] = useState(today());

  const { data: sesData } = useQuery({
    queryKey: ["ses-configs"],
    queryFn: () => configApi.sesList(),
  });
  const rawConfigs = sesData?.data?.data;
  const configs: SesConfig[] = Array.isArray(rawConfigs) ? rawConfigs : (rawConfigs?.content ?? []);
  const activeConfig = configs.find(c => c.isActive);

  const { data: suppData, isLoading: loadingSupp } = useQuery({
    queryKey: ["suppression"],
    queryFn: () => suppressionApi.list(0, 500),
  });
  const rawSupp = suppData?.data?.data;
  const suppList: SuppressionEntry[] = Array.isArray(rawSupp) ? rawSupp : (rawSupp?.content ?? []);
  const suppressions: SuppressionEntry[] = suppList.filter(
    (s: SuppressionEntry) => !suppSearch || s.email?.toLowerCase().includes(suppSearch.toLowerCase()) || s.clientCode?.toLowerCase().includes(suppSearch.toLowerCase())
  );

  const { mutate: removeSupp } = useMutation({
    mutationFn: (email: string) => suppressionApi.remove(email),
    onSuccess: () => { toast.success("Address removed from suppression list"); qc.invalidateQueries({ queryKey: ["suppression"] }); },
  });

  const { data: sesStatsData, isLoading: loadingStats, refetch: refetchStats } = useQuery({
    queryKey: ["ses-statistics", appliedStart, appliedEnd],
    queryFn: () => configApi.sesStatistics(appliedStart, appliedEnd),
    staleTime: 2 * 60 * 1000,
  });
  const _raw = sesStatsData?.data?.data ?? {};
  const sesStats: SesStatistics = {
    reputationScore:  _raw.reputationScore  ?? 0,
    sendRate:         _raw.sendRate         ?? 0,
    bounceRate:       _raw.bounceRate       ?? 0,
    complaintRate:    _raw.complaintRate    ?? 0,
    dailySendQuota:   _raw.dailySendQuota   ?? 0,
    sentLast24h:      _raw.sentLast24h      ?? 0,
    remainingSends:   _raw.remainingSends   ?? 0,
    quotaUsedPercent: _raw.quotaUsedPercent ?? 0,
    dataPoints:       _raw.dataPoints       ?? [],
  };

  const isHealthy = sesStats.bounceRate < 5 && sesStats.complaintRate < 0.1;

  const applyDateRange = () => { setAppliedStart(startDate); setAppliedEnd(endDate); };

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div>
        <h2 className="text-2xl font-extrabold text-slate-900 headline">Email Configuration</h2>
        <p className="text-slate-500 text-sm mt-1">Sender reputation, email delivery settings, and suppression management for contract note emails.</p>
      </div>

      {/* Section 1 — Account Dashboard */}
      <div className="card p-5">
        <div className="font-bold text-slate-900 headline mb-4">Account Dashboard</div>
        <div className="grid md:grid-cols-4 gap-4">
          <div className="p-4 bg-slate-50 rounded-xl">
            <div className="text-[10px] text-slate-500 uppercase font-bold mb-1">Daily Sending Quota</div>
            <div className="text-xl font-extrabold mono text-slate-900">{sesStats.dailySendQuota.toLocaleString()}</div>
            <div className="text-[11px] text-slate-400 mt-0.5">emails per 24-hour period</div>
          </div>
          <div className="p-4 bg-slate-50 rounded-xl">
            <div className="text-[10px] text-slate-500 uppercase font-bold mb-1">Maximum Send Rate</div>
            <div className="text-xl font-extrabold mono text-slate-900">{sesStats.sendRate.toFixed(0)}</div>
            <div className="text-[11px] text-slate-400 mt-0.5">email(s) per second</div>
          </div>
          <div className="p-4 bg-slate-50 rounded-xl">
            <div className="text-[10px] text-slate-500 uppercase font-bold mb-1">Account Health</div>
            <div className={cn("text-xl font-extrabold mt-1", isHealthy ? "text-emerald-600" : "text-amber-600")}>
              {isHealthy ? "● Healthy" : "⚠ At Risk"}
            </div>
            <div className="text-[11px] text-slate-400 mt-0.5">
              {isHealthy ? "Bounce &lt;5% · Complaint &lt;0.1%" : `Bounce ${sesStats.bounceRate.toFixed(2)}% · Complaint ${sesStats.complaintRate.toFixed(3)}%`}
            </div>
          </div>
          <div className="p-4 bg-slate-50 rounded-xl">
            <div className="text-[10px] text-slate-500 uppercase font-bold mb-1">Region</div>
            <div className="text-xl font-extrabold mono text-slate-900">{activeConfig?.region ?? "—"}</div>
            <div className="text-[11px] text-slate-400 mt-0.5 mono">{activeConfig?.fromEmail ?? "—"}</div>
          </div>
        </div>
      </div>

      {/* Section 2 — Daily Email Usage */}
      <div className="card p-5">
        <div className="font-bold text-slate-900 headline mb-4">Daily Email Usage</div>
        <div className="grid md:grid-cols-3 gap-4">
          <div className="p-4 bg-slate-50 rounded-xl">
            <div className="text-[10px] text-slate-500 uppercase font-bold mb-1">Emails sent (last 24h)</div>
            <div className="text-2xl font-extrabold mono text-slate-900">{sesStats.sentLast24h.toLocaleString()}</div>
          </div>
          <div className="p-4 bg-slate-50 rounded-xl">
            <div className="text-[10px] text-slate-500 uppercase font-bold mb-1">Remaining sends</div>
            <div className="text-2xl font-extrabold mono text-emerald-700">{sesStats.remainingSends.toLocaleString()}</div>
          </div>
          <div className="p-4 bg-slate-50 rounded-xl">
            <div className="text-[10px] text-slate-500 uppercase font-bold mb-1">Sending quota used</div>
            <div className="text-2xl font-extrabold mono text-slate-900">{sesStats.quotaUsedPercent.toFixed(1)}%</div>
            <div className="mt-2 h-2 bg-slate-200 rounded-full overflow-hidden">
              <div
                className={cn("h-full rounded-full transition-all", sesStats.quotaUsedPercent > 80 ? "bg-rose-500" : sesStats.quotaUsedPercent > 50 ? "bg-amber-400" : "bg-emerald-500")}
                style={{ width: `${Math.min(sesStats.quotaUsedPercent, 100)}%` }}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Section 3 — Sending Statistics charts */}
      <div className="card p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <div className="font-bold text-slate-900 headline">Sending Statistics</div>
            <div className="text-[11px] text-slate-400 mt-0.5">Data from AWS SES · up to 30 min delay · max 14 days history</div>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={startDate}
              onChange={e => setStartDate(e.target.value)}
              max={endDate}
              min={daysAgo(14)}
              className="border border-slate-200 rounded-lg px-3 py-1.5 text-sm"
            />
            <span className="text-slate-400 text-sm">to</span>
            <input
              type="date"
              value={endDate}
              onChange={e => setEndDate(e.target.value)}
              max={today()}
              min={startDate}
              className="border border-slate-200 rounded-lg px-3 py-1.5 text-sm"
            />
            <button
              onClick={applyDateRange}
              className="px-3 py-1.5 bg-[#00174b] text-white rounded-lg text-sm font-semibold hover:bg-[#003ea8]"
            >
              Apply
            </button>
            <button
              onClick={() => refetchStats()}
              disabled={loadingStats}
              className="p-1.5 border border-slate-200 rounded-lg hover:bg-slate-50 text-slate-500"
              title="Refresh"
            >
              {loadingStats ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {sesStats.dataPoints.length === 0 && !loadingStats ? (
          <div className="text-center text-slate-400 py-12 text-sm">No data for the selected date range</div>
        ) : (
          <div className="grid md:grid-cols-2 gap-6">
            <ChartCard title="Sends" description="Count of successful send requests" dataKey="sends" data={sesStats.dataPoints} color="#3b82f6" />
            <ChartCard title="Rejects" description="Emails rejected by Amazon SES" dataKey="rejects" data={sesStats.dataPoints} color="#f59e0b" />
            <ChartCard title="Bounces" description="Emails resulting in a hard bounce" dataKey="bounces" data={sesStats.dataPoints} color="#ef4444" />
            <ChartCard title="Complaints" description="Emails reported as spam" dataKey="complaints" data={sesStats.dataPoints} color="#8b5cf6" />
          </div>
        )}
      </div>

      {/* Suppression list */}
      <div className="card p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <div className="font-bold text-slate-900 headline">Suppression List</div>
            <div className="text-[11px] text-slate-500 mt-0.5">{suppressions.length} addresses blocked · email permanently rejected or marked as spam</div>
          </div>
          <div className="flex gap-2">
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-base">search</span>
              <input
                value={suppSearch}
                onChange={e => setSuppSearch(e.target.value)}
                className="pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm w-52"
                placeholder="Search email or client..."
              />
            </div>
            <button onClick={() => downloadCsv("suppression-list.csv", ["Email","Reason","Added"], suppressions.map(s => [s.email ?? "", s.reason ?? "", s.addedAt ?? ""]))} className="px-3 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-sm">download</span>Export
            </button>
          </div>
        </div>

        <div className="grid md:grid-cols-3 gap-3 mb-4">
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold">Total suppressed</div><div className="text-xl font-extrabold mono text-slate-900">{suppressions.length}</div></div>
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold">Added this week</div><div className="text-xl font-extrabold mono text-rose-600">+{suppList.filter(s => { const d = toUtcDate(s.addedAt); const w = new Date(); w.setDate(w.getDate() - 7); return d != null && d >= w; }).length}</div></div>
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold">Latest added</div><div className="text-xl font-extrabold mono text-slate-600">{suppList.length > 0 ? fmtDate(suppList.slice().sort((a, b) => (toUtcDate(b.addedAt)?.getTime() ?? 0) - (toUtcDate(a.addedAt)?.getTime() ?? 0))[0].addedAt) : "—"}</div></div>
        </div>

        {loadingSupp ? (
          <div className="flex justify-center py-8"><Loader2 className="animate-spin text-[#00174b]" /></div>
        ) : suppressions.length === 0 ? (
          <div className="text-center text-slate-500 py-8 text-sm">No suppressed addresses</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead><tr className="t-hd"><th className="px-4 py-3">Email</th><th className="px-4 py-3">Reason</th><th className="px-4 py-3">Added</th><th className="px-4 py-3"></th></tr></thead>
              <tbody>
                {suppressions.map(s => (
                  <tr key={s.id} className="t-row">
                    <td className="px-4 py-3 mono">{s.email}</td>
                    <td className="px-4 py-3"><span className={cn("pill", s.reason?.toLowerCase().includes("permanent") ? "pill-err" : s.reason?.toLowerCase().includes("complaint") ? "pill-warn" : "pill-neu")}>{s.reason ?? "Suppressed"}</span></td>
                    <td className="px-4 py-3 text-slate-500">{fmtDate(s.addedAt)}</td>
                    <td className="px-4 py-3">
                      <button onClick={() => removeSupp(s.email)} className="text-[11px] font-bold text-[#003ea8] hover:underline">Remove</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function ChartCard({
  title, description, dataKey, data, color,
}: {
  title: string;
  description: string;
  dataKey: string;
  data: Record<string, unknown>[];
  color: string;
}) {
  return (
    <div className="p-4 bg-slate-50 rounded-xl">
      <div className="text-sm font-bold text-slate-900 mb-0.5">{title}</div>
      <div className="text-[11px] text-slate-500 mb-3">{description}</div>
      <ResponsiveContainer width="100%" height={160}>
        <LineChart data={data} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="timestamp" tick={{ fontSize: 10, fill: "#94a3b8" }} />
          <YAxis tick={{ fontSize: 10, fill: "#94a3b8" }} allowDecimals={false} />
          <Tooltip
            contentStyle={{ fontSize: 11, borderRadius: 8, border: "1px solid #e2e8f0" }}
            labelStyle={{ fontWeight: "bold" }}
          />
          <Line type="monotone" dataKey={dataKey} stroke={color} strokeWidth={2} dot={{ r: 3 }} activeDot={{ r: 5 }} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
