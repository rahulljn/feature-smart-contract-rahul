"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { configApi, suppressionApi } from "@/lib/api";
import { downloadCsv } from "@/lib/export";
import type { SesConfig, SuppressionEntry, SesStatistics } from "@/types";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

export default function SesConfigPage() {
  const qc = useQueryClient();
  const [suppSearch, setSuppSearch] = useState("");

  const { data: sesData, isLoading: loadingSes } = useQuery({
    queryKey: ["ses-configs"],
    queryFn: () => configApi.sesList(),
  });
  const rawConfigs = sesData?.data?.data;
  const configs: SesConfig[] = Array.isArray(rawConfigs) ? rawConfigs : (rawConfigs?.content ?? []);

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

  const { data: sesStatsData } = useQuery({
    queryKey: ["ses-statistics"],
    queryFn: () => configApi.sesStatistics(),
    staleTime: 5 * 60 * 1000,
  });
  const sesStats: SesStatistics = sesStatsData?.data?.data ?? { reputationScore: 0, sendRate: 0, bounceRate: 0, complaintRate: 0, dailySendQuota: 0, sentLast24h: 0 };

  const activeConfig = configs.find(c => c.isActive);

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div>
        <h2 className="text-2xl font-extrabold text-slate-900 headline">Email Configuration</h2>
        <p className="text-slate-500 text-sm mt-1">Sender reputation, email delivery settings, and suppression management for contract note emails.</p>
      </div>

      {/* Sender reputation metrics */}
      <div className="grid md:grid-cols-4 gap-4">
        <div className="metric"><div className="flex items-start justify-between mb-3"><div className="p-2 bg-emerald-50 rounded-lg"><span className="material-symbols-outlined text-emerald-600">trending_up</span></div></div><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Reputation</div><div className="text-2xl font-extrabold mono text-slate-900 headline">{sesStats.reputationScore.toFixed(1)}%</div><div className={cn("text-[11px] mt-1", sesStats.reputationScore >= 90 ? "text-emerald-600" : "text-amber-600")}>{sesStats.reputationScore >= 90 ? "Healthy" : "Needs attention"}</div></div>
        <div className="metric"><div className="flex items-start justify-between mb-3"><div className="p-2 bg-blue-50 rounded-lg"><span className="material-symbols-outlined text-[#497cff]">bolt</span></div></div><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Send rate</div><div className="text-2xl font-extrabold mono text-slate-900 headline">{sesStats.sendRate.toFixed(0)} / s</div><div className="text-[11px] text-slate-400 mt-1">{sesStats.sentLast24h} / {sesStats.dailySendQuota} today</div></div>
        <div className="metric"><div className="flex items-start justify-between mb-3"><div className="p-2 bg-rose-50 rounded-lg"><span className="material-symbols-outlined text-rose-600">error</span></div></div><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Bounce rate</div><div className="text-2xl font-extrabold mono text-slate-900 headline">{sesStats.bounceRate.toFixed(2)}%</div><div className={cn("text-[11px] mt-1", sesStats.bounceRate < 5 ? "text-emerald-600" : "text-rose-600")}>{sesStats.bounceRate < 5 ? "< 5% limit" : "Above 5% limit!"}</div></div>
        <div className="metric"><div className="flex items-start justify-between mb-3"><div className="p-2 bg-amber-50 rounded-lg"><span className="material-symbols-outlined text-amber-600">flag</span></div></div><div className="text-xs text-slate-500 font-semibold uppercase tracking-wider">Complaint rate</div><div className="text-2xl font-extrabold mono text-slate-900 headline">{sesStats.complaintRate.toFixed(3)}%</div><div className={cn("text-[11px] mt-1", sesStats.complaintRate < 0.1 ? "text-emerald-600" : "text-rose-600")}>{sesStats.complaintRate < 0.1 ? "< 0.1% limit" : "Above 0.1% limit!"}</div></div>
      </div>

      {/* Configuration details */}
      <div className="card p-5">
        <div className="font-bold text-slate-900 headline mb-3">Sender configuration</div>
        <div className="grid md:grid-cols-2 gap-3 text-xs">
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold mb-0.5">Sender address</div><div className="font-semibold mono">{activeConfig?.fromEmail ?? "noreply@geojit.co.in"}</div></div>
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold mb-0.5">Region</div><div className="font-semibold mono">{activeConfig?.region ?? "Mumbai"}</div></div>
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold mb-0.5">Bounce tracking</div><div className="font-semibold text-emerald-700">Enabled · bounced addresses auto-suppressed</div></div>
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold mb-0.5">Delivery tracking</div><div className="font-semibold text-emerald-700">Enabled · confirmed receipt recorded per client</div></div>
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold mb-0.5">Complaint tracking</div><div className="font-semibold text-emerald-700">Enabled · marked-as-spam addresses suppressed</div></div>
        </div>
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
            <button onClick={() => downloadCsv("suppression-list.csv", ["Email","Client Code","Reason","Added"], suppressions.map(s => [s.email ?? "", s.clientCode ?? "", s.reason ?? "", s.addedAt ?? ""]))} className="px-3 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-sm">download</span>Export
            </button>
          </div>
        </div>

        {/* Stats */}
        <div className="grid md:grid-cols-3 gap-3 mb-4">
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold">Total suppressed</div><div className="text-xl font-extrabold mono text-slate-900">{suppressions.length}</div></div>
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold">Added this week</div><div className="text-xl font-extrabold mono text-rose-600">+{suppList.filter(s => { const d = new Date(s.addedAt); const w = new Date(); w.setDate(w.getDate() - 7); return d >= w; }).length}</div></div>
          <div className="p-3 bg-slate-50 rounded-xl"><div className="text-[10px] text-slate-500 uppercase font-bold">Latest added</div><div className="text-xl font-extrabold mono text-slate-600">{suppList.length > 0 ? new Date(suppList.slice().sort((a, b) => new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime())[0].addedAt).toLocaleDateString("en-GB") : "—"}</div></div>
        </div>

        {loadingSupp ? (
          <div className="flex justify-center py-8"><Loader2 className="animate-spin text-[#00174b]" /></div>
        ) : suppressions.length === 0 ? (
          <div className="text-center text-slate-500 py-8 text-sm">No suppressed addresses</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead><tr className="t-hd"><th className="px-4 py-3">Email</th><th className="px-4 py-3">Client code</th><th className="px-4 py-3">Reason</th><th className="px-4 py-3">Added</th><th className="px-4 py-3"></th></tr></thead>
              <tbody>
                {suppressions.map(s => (
                  <tr key={s.id} className="t-row">
                    <td className="px-4 py-3 mono">{s.email}</td>
                    <td className="px-4 py-3 mono">{s.clientCode ?? "—"}</td>
                    <td className="px-4 py-3"><span className={cn("pill", s.reason?.includes("Permanent") ? "pill-err" : s.reason?.includes("Complaint") ? "pill-warn" : "pill-neu")}>{s.reason ?? "Suppressed"}</span></td>
                    <td className="px-4 py-3 text-slate-500">{s.addedAt ? new Date(s.addedAt).toLocaleDateString("en-GB") : "—"}</td>
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
