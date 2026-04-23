"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { configApi, suppressionApi } from "@/lib/api";
import { downloadCsv } from "@/lib/export";
import type { SesConfig, SuppressionEntry, SesStatistics } from "@/types";
import { Loader2, RefreshCw, Info } from "lucide-react";
import { cn, fmtDate, toUtcDate } from "@/lib/utils";
import { toast } from "sonner";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from "recharts";

const AWS_REGION_NAMES: Record<string, string> = {
  "ap-south-1":     "Asia Pacific (Mumbai)",
  "ap-southeast-1": "Asia Pacific (Singapore)",
  "ap-southeast-2": "Asia Pacific (Sydney)",
  "ap-northeast-1": "Asia Pacific (Tokyo)",
  "ap-northeast-2": "Asia Pacific (Seoul)",
  "us-east-1":      "US East (N. Virginia)",
  "us-east-2":      "US East (Ohio)",
  "us-west-1":      "US West (N. California)",
  "us-west-2":      "US West (Oregon)",
  "eu-west-1":      "Europe (Ireland)",
  "eu-west-2":      "Europe (London)",
  "eu-central-1":   "Europe (Frankfurt)",
  "ca-central-1":   "Canada (Central)",
  "sa-east-1":      "South America (São Paulo)",
};

function today() { return new Date().toISOString().slice(0, 10); }
function daysAgo(n: number) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

export default function SesConfigPage() {
  const qc = useQueryClient();
  const [suppSearch, setSuppSearch] = useState("");
  const [showAddForm, setShowAddForm] = useState(false);
  const [addEmail, setAddEmail] = useState("");
  const [addReason, setAddReason] = useState("bounce");
  const [dateRange, setDateRange] = useState<"7" | "14">("7");

  const startDate = daysAgo(Number(dateRange) - 1);
  const endDate = today();

  const { data: sesData } = useQuery({
    queryKey: ["ses-configs"],
    queryFn: () => configApi.sesList(),
  });
  const rawConfigs = sesData?.data?.data;
  const configs: SesConfig[] = Array.isArray(rawConfigs) ? rawConfigs : (rawConfigs?.content ?? []);
  const activeConfig = configs.find(c => c.isActive);
  const regionCode = activeConfig?.region ?? "";
  const regionName = AWS_REGION_NAMES[regionCode] ?? regionCode;
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
    onError: () => toast.error("Failed to remove address"),
  });

  const { mutate: addSupp, isPending: adding } = useMutation({
    mutationFn: () => suppressionApi.add(addEmail.trim(), addReason),
    onSuccess: () => {
      toast.success("Address added to suppression list and synced to AWS SES");
      setAddEmail(""); setShowAddForm(false);
      qc.invalidateQueries({ queryKey: ["suppression"] });
    },
    onError: () => toast.error("Failed to add address — check it isn't already suppressed"),
  });

  const { mutate: syncAws, isPending: syncing } = useMutation({
    mutationFn: () => suppressionApi.syncFromAws(),
    onSuccess: (res) => {
      const msg: string = res?.data?.message ?? "Synced from AWS SES";
      toast.success(msg);
      qc.invalidateQueries({ queryKey: ["suppression"] });
    },
    onError: () => toast.error("Failed to sync from AWS SES"),
  });

  const { mutate: pushToAws, isPending: pushing } = useMutation({
    mutationFn: () => suppressionApi.pushToAws(),
    onSuccess: (res) => {
      const msg: string = res?.data?.message ?? "Pushed to AWS SES";
      toast.success(msg);
    },
    onError: () => toast.error("Failed to push to AWS SES"),
  });

  const { data: sesStatsData, isLoading: loadingStats, refetch: refetchStats } = useQuery({
    queryKey: ["ses-statistics", startDate, endDate],
    queryFn: () => configApi.sesStatistics(startDate, endDate),
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

  const chartData = sesStats.dataPoints.map(dp => ({
    ...dp,
    bounceRate:    dp.sends > 0 ? Math.round(dp.bounces    / dp.sends * 10000) / 100 : 0,
    rejectRate:    dp.sends > 0 ? Math.round(dp.rejects    / dp.sends * 10000) / 100 : 0,
    complaintRate: dp.sends > 0 ? Math.round(dp.complaints / dp.sends * 10000) / 100 : 0,
  }));

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div>
        <h2 className="text-2xl font-extrabold text-slate-900 headline">Email Configuration</h2>
        <p className="text-slate-500 text-sm mt-1">Sender reputation, email delivery settings, and suppression management for contract note emails.</p>
      </div>

      {/* Section 1 — Daily email usage */}
      <div className="card p-5">
        <div className="flex items-start justify-between mb-1">
          <div className="flex items-center gap-1.5">
            <span className="font-bold text-slate-900 headline text-base">Daily email usage</span>
            <Info className="w-3.5 h-3.5 text-slate-400" />
          </div>
          <button
            onClick={() => refetchStats()}
            disabled={loadingStats}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-50"
            title="Refresh"
          >
            {loadingStats ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
          </button>
        </div>
        <p className="text-[12px] text-slate-500 mb-5">
          Amazon SES recommends checking your daily usage data regularly to ensure that you aren&apos;t approaching your sending limits.
        </p>

        <div className="grid md:grid-cols-3 gap-0 divide-x divide-slate-100">
          <div className="pr-8">
            <div className="text-[13px] text-slate-600 font-medium mb-1">Emails sent</div>
            <div className="text-4xl font-bold text-slate-900 mono">{sesStats.sentLast24h.toLocaleString()}</div>
          </div>
          <div className="px-8">
            <div className="text-[13px] text-slate-600 font-medium mb-1">Remaining sends</div>
            <div className="text-4xl font-bold text-slate-900 mono">{sesStats.remainingSends.toLocaleString()}</div>
          </div>
          <div className="pl-8">
            <div className="text-[13px] text-slate-600 font-medium mb-1">Sending quota used</div>
            <div className="text-4xl font-bold text-slate-900 mono">{sesStats.quotaUsedPercent.toFixed(2)}%</div>
          </div>
        </div>

        {/* Account health strip */}
        <div className={cn(
          "mt-5 pt-4 border-t border-slate-100 flex items-center gap-6 text-[12px]",
        )}>
          <span className="text-slate-500">Daily quota: <span className="font-semibold text-slate-700">{sesStats.dailySendQuota.toLocaleString()}</span> emails / 24 h</span>
          <span className="text-slate-500">Max send rate: <span className="font-semibold text-slate-700">{sesStats.sendRate.toFixed(0)}</span> emails / sec</span>
          <span className={cn("flex items-center gap-1 font-semibold", isHealthy ? "text-emerald-600" : "text-amber-600")}>
            {isHealthy ? "● Healthy" : "⚠ At Risk"}
            <span className="font-normal text-slate-500 ml-1">
              {isHealthy ? "(bounce < 5% · complaint < 0.1%)" : `(bounce ${sesStats.bounceRate.toFixed(2)}% · complaint ${sesStats.complaintRate.toFixed(3)}%)`}
            </span>
          </span>
        </div>
      </div>

      {/* Section 2 — Sending statistics */}
      <div className="card p-5">
        <div className="flex items-start justify-between mb-1">
          <div className="flex items-center gap-1.5">
            <span className="font-bold text-slate-900 headline text-base">Sending statistics</span>
            <Info className="w-3.5 h-3.5 text-slate-400" />
          </div>
        </div>
        <p className="text-[12px] text-slate-500 mb-4">
          The following charts show the number of successful send requests, as well as the rejection, bounce and complaint rates
          {regionName ? ` for ${regionName}` : ""}.
        </p>

        <div className="flex items-center gap-8 mb-5">
          <div>
            <div className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider mb-0.5">Region</div>
            <div className="text-[13px] text-slate-800 font-medium">{regionName || "—"}</div>
          </div>
          <div>
            <div className="text-[11px] text-slate-500 font-semibold uppercase tracking-wider mb-0.5">Date range</div>
            <select
              value={dateRange}
              onChange={e => setDateRange(e.target.value as "7" | "14")}
              className="border border-slate-300 rounded-md px-3 py-1.5 text-[13px] bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
            >
              <option value="7">Last 7 days</option>
              <option value="14">Last 14 days</option>
            </select>
          </div>
        </div>

        {sesStats.dataPoints.length === 0 && !loadingStats ? (
          <div className="text-center text-slate-400 py-12 text-sm">No data for the selected date range</div>
        ) : (
          <div className="grid md:grid-cols-2 gap-4">
            <ChartCard
              title="Sends"
              description="The count of successful send requests."
              yAxisLabel="Count"
              dataKey="sends"
              data={chartData}
            />
            <ChartCard
              title="Rejects"
              description="The percentage of emails rejected by Amazon SES."
              yAxisLabel="Rate"
              dataKey="rejectRate"
              data={chartData}
              isRate
            />
            <ChartCard
              title="Bounces"
              description="The percentage of emails that resulted in a hard bounce."
              yAxisLabel="Rate"
              dataKey="bounceRate"
              data={chartData}
              isRate
            />
            <ChartCard
              title="Complaints"
              description="The percentage of emails reported as spam by the recipients."
              yAxisLabel="Rate"
              dataKey="complaintRate"
              data={chartData}
              isRate
            />
          </div>
        )}
      </div>

      {/* Suppression list */}
      <div className="card p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <div className="font-bold text-slate-900 headline">Suppression List</div>
            <div className="text-[11px] text-slate-500 mt-0.5">{suppList.length} addresses blocked · synced with AWS SES account suppression list</div>
          </div>
          <div className="flex gap-2 flex-wrap">
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-base">search</span>
              <input
                value={suppSearch}
                onChange={e => setSuppSearch(e.target.value)}
                className="pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm w-52"
                placeholder="Search email or client..."
              />
            </div>
            <button
              onClick={() => syncAws()}
              disabled={syncing}
              className="px-3 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5 disabled:opacity-50"
              title="Pull latest entries from AWS SES account suppression list into local DB"
            >
              <span className={cn("material-symbols-outlined text-sm", syncing && "animate-spin")}>sync</span>
              {syncing ? "Syncing…" : "Sync AWS"}
            </button>
            <button
              onClick={() => pushToAws()}
              disabled={pushing}
              className="px-3 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5 disabled:opacity-50"
              title="Push all local suppression entries to AWS SES account suppression list"
            >
              <span className={cn("material-symbols-outlined text-sm", pushing && "animate-spin")}>cloud_upload</span>
              {pushing ? "Pushing…" : "Push to AWS"}
            </button>
            <button
              onClick={() => setShowAddForm(v => !v)}
              className="px-3 py-2 bg-[#00174b] text-white rounded-lg text-sm font-semibold hover:bg-[#003ea8] flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-sm">add</span>Add address
            </button>
            <button onClick={() => downloadCsv("suppression-list.csv", ["Email","Reason","Added"], suppressions.map(s => [s.email ?? "", s.reason ?? "", s.addedAt ?? ""]))} className="px-3 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-sm">download</span>Export
            </button>
          </div>
        </div>

        {/* Inline Add Form */}
        {showAddForm && (
          <div className="mb-4 p-4 bg-slate-50 border border-slate-200 rounded-xl">
            <div className="text-[11px] font-bold text-slate-700 uppercase tracking-wider mb-3">Add to suppression list</div>
            <div className="flex flex-wrap gap-3 items-end">
              <div className="flex flex-col gap-1">
                <label className="text-[10px] text-slate-500 font-semibold uppercase">Email address</label>
                <input
                  type="email"
                  value={addEmail}
                  onChange={e => setAddEmail(e.target.value)}
                  placeholder="client@example.com"
                  className="px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm w-64 focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-[10px] text-slate-500 font-semibold uppercase">Reason</label>
                <select
                  value={addReason}
                  onChange={e => setAddReason(e.target.value)}
                  className="px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
                >
                  <option value="bounce">Permanent bounce</option>
                  <option value="complaint">Complaint / Spam report</option>
                  <option value="manual">Manual suppression</option>
                </select>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => addSupp()}
                  disabled={adding || !addEmail.trim()}
                  className="px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-bold hover:bg-[#003ea8] disabled:opacity-50 flex items-center gap-1.5"
                >
                  {adding ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-sm">block</span>}
                  Suppress & sync to AWS
                </button>
                <button
                  onClick={() => { setShowAddForm(false); setAddEmail(""); }}
                  className="px-4 py-2 bg-white border border-slate-200 text-slate-700 rounded-lg text-sm font-semibold hover:bg-slate-50"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

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
  title, description, yAxisLabel, dataKey, data, isRate = false,
}: {
  title: string;
  description: string;
  yAxisLabel: string;
  dataKey: string;
  data: object[];
  isRate?: boolean;
}) {
  // Show ~7 evenly-spaced ticks across however many 15-min data points there are
  const tickInterval = data.length > 7 ? Math.floor((data.length - 1) / 6) : 0;

  return (
    <div className="border border-slate-200 rounded-xl bg-white p-4">
      <div className="font-bold text-slate-900 text-[14px] mb-0.5">{title}</div>
      <div className="text-[11px] text-slate-500 mb-3">{description}</div>
      <div className="text-[10px] font-semibold text-slate-700 mb-1">{yAxisLabel}</div>
      <ResponsiveContainer width="100%" height={180}>
        <LineChart data={data} margin={{ top: 4, right: 8, left: isRate ? -10 : -20, bottom: 20 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis
            dataKey="timestamp"
            interval={tickInterval}
            tick={{ fontSize: 9, fill: "#94a3b8" }}
            angle={0}
            tickLine={false}
          />
          <YAxis
            tick={{ fontSize: 10, fill: "#94a3b8" }}
            allowDecimals={isRate}
            tickFormatter={isRate ? (v) => `${v}%` : undefined}
          />
          <Tooltip
            contentStyle={{ fontSize: 11, borderRadius: 8, border: "1px solid #e2e8f0" }}
            labelStyle={{ fontWeight: "bold" }}
            formatter={isRate ? (v) => [`${v}%`, "Rate"] : undefined}
          />
          <Line
            type="monotone"
            dataKey={dataKey}
            stroke="#3b82f6"
            strokeWidth={1.5}
            dot={false}
            activeDot={{ r: 4 }}
          />
        </LineChart>
      </ResponsiveContainer>
      <div className="text-[10px] text-center text-slate-400 -mt-3">Time (UTC)</div>
    </div>
  );
}
