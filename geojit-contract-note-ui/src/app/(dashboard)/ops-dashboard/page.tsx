"use client";

import React, { useState, useEffect } from "react";
import { cn } from "@/lib/utils";
import Link from "next/link";
import { toast } from "sonner";
import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from "recharts";
import { opsApi } from "@/lib/api";
import type { OpsTodaySummary, OpsTodayJob, OpsIssue, OpsHourlyFailure, OpsFailureDistribution } from "@/types";

// ─── Helpers ─────────────────────────────────────────────────────────────────

// ─── Helpers ─────────────────────────────────────────────────────────────────
const STATUS_STYLES: Record<string, string> = {
  COMPLETED:  "border border-green-500  text-green-700  bg-green-50",
  EMAILING:   "border border-blue-500   text-blue-700   bg-blue-50",
  PROCESSING: "border border-purple-500 text-purple-700 bg-purple-50",
  FAILED:     "border border-red-500    text-red-700    bg-red-50",
  PARTIAL:    "border border-amber-500  text-amber-700  bg-amber-50",
};

function StatusBadge({ status }: { status: string }) {
  const cls = STATUS_STYLES[status] ?? "border border-gray-300 text-gray-500 bg-gray-50";
  const isActive = ["EMAILING", "PROCESSING"].includes(status);
  return (
    <span className={cn("px-2 py-0.5 rounded-full text-[10px] font-bold inline-flex items-center gap-1", cls)}>
      {isActive && <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse" />}
      {status}
    </span>
  );
}

const SEVERITY_DOT: Record<string, string> = {
  HIGH: "bg-red-500", MEDIUM: "bg-amber-400", LOW: "bg-blue-500",
};

interface TooltipPayloadItem { dataKey: string; name: string; value: number; color: string; payload?: any }
interface CTooltipProps { active?: boolean; payload?: TooltipPayloadItem[]; label?: string }
function CustomTooltip({ active, payload, label }: CTooltipProps) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-white border border-gray-200 rounded-xl shadow-lg px-3 py-2 text-xs">
      {label && <div className="font-semibold text-gray-700 mb-1">{label}</div>}
      {payload.map((p) => (
        <div key={p.dataKey} className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: p.color }} />
          <span className="text-gray-600">{p.payload?.type ?? p.name}:</span>
          <span className="font-bold text-gray-900">{p.value?.toLocaleString()}</span>
        </div>
      ))}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────
export default function OpsDashboardPage() {
  const [expandedJob, setExpandedJob] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [summary, setSummary] = useState<OpsTodaySummary | null>(null);
  const [todayJobs, setTodayJobs] = useState<OpsTodayJob[]>([]);
  const [appIssues, setAppIssues] = useState<OpsIssue[]>([]);
  const [infraIssues, setInfraIssues] = useState<OpsIssue[]>([]);
  const [hourlyData, setHourlyData] = useState<OpsHourlyFailure[]>([]);
  const [distData, setDistData] = useState<OpsFailureDistribution[]>([]);
  const [health, setHealth] = useState<any>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [summaryRes, jobsRes, issuesRes, hourlyRes, distRes, healthRes] = await Promise.all([
        opsApi.getTodaySummary(),
        opsApi.getTodayJobs(),
        opsApi.getIssues(),
        opsApi.getHourlyFailures(),
        opsApi.getFailureDistribution(),
        opsApi.getHealthStatus(),
      ]);
      setSummary(summaryRes.data.data);
      setTodayJobs(jobsRes.data.data ?? []);
      setAppIssues(issuesRes.data.data?.appIssues ?? []);
      setInfraIssues(issuesRes.data.data?.infraIssues ?? []);
      setHourlyData(hourlyRes.data.data ?? []);
      setDistData(distRes.data.data ?? []);
      setHealth(healthRes.data.data);
    } catch (err) {
      setError("Failed to load dashboard data");
      toast.error("Failed to load dashboard data");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [refreshKey]);

  const totalDist = distData.reduce((s, d) => s + (d.count ?? 0), 0);

  if (error) {
    return (
      <div className="space-y-6 fade-up">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="font-bold text-2xl text-gray-900">Ops Dashboard</h1>
        </div>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">{error}</div>
      </div>
    );
  }

  return (
    <div className="space-y-6 fade-up">

      {/* ── Header ─────────────────────────────────────────────────────── */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-bold text-2xl text-gray-900">Ops Dashboard</h1>
        <div className="flex items-center gap-2">
          <span className="flex items-center gap-1.5 px-2.5 py-1 bg-green-100 text-green-700 text-xs font-medium rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
            Live
          </span>
          <button
            onClick={() => setRefreshKey((k) => k + 1)}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-1.5 border border-gray-200 rounded-lg text-sm text-gray-600 hover:bg-gray-50 transition-colors disabled:opacity-50"
          >
            <span className={cn("material-symbols-outlined text-[16px]", loading && "animate-spin")}>refresh</span>
            Refresh
          </button>
        </div>
      </div>

      {/* ── KPI Cards (grid-cols-4) ─────────────────────────────────────── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl border border-gray-100 p-4">
          <div className="p-2 rounded-xl bg-blue-50 w-fit mb-3">
            <span className="material-symbols-outlined text-blue-500">storage</span>
          </div>
          <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-0.5">Total Records</div>
          {loading ? (
            <div className="text-3xl font-bold text-gray-300">…</div>
          ) : (
            <div className="text-3xl font-bold text-gray-900">{(summary?.total ?? 0).toLocaleString()}</div>
          )}
          <div className="text-xs text-gray-400 mt-1">Records processed today</div>
        </div>

        <div className="bg-white rounded-xl border border-gray-100 p-4">
          <div className="p-2 rounded-xl bg-indigo-50 w-fit mb-3 flex items-center gap-1">
            <span className="material-symbols-outlined text-indigo-500">pending</span>
            {(summary?.inProgress ?? 0) > 0 && !loading && <span className="w-2 h-2 rounded-full bg-indigo-500 animate-pulse" />}
          </div>
          <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-0.5">In Progress</div>
          {loading ? (
            <div className="text-3xl font-bold text-gray-300">…</div>
          ) : (
            <div className={cn("text-3xl font-bold", (summary?.inProgress ?? 0) > 0 ? "text-indigo-600" : "text-gray-900")}>
              {summary?.inProgress ?? 0}
            </div>
          )}
          <div className="text-xs text-gray-400 mt-1">Jobs currently running</div>
        </div>

        <div className="bg-white rounded-xl border border-gray-100 p-4 cursor-pointer hover:border-red-200 transition-colors group">
          <div className="p-2 rounded-xl bg-red-50 w-fit mb-3">
            <span className="material-symbols-outlined text-red-500">error</span>
          </div>
          <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-0.5">Failures</div>
          {loading ? (
            <div className="text-3xl font-bold text-gray-300">…</div>
          ) : (
            <div className={cn("text-3xl font-bold", (summary?.failures ?? 0) > 0 ? "text-red-600" : "text-gray-900")}>
              {(summary?.failures ?? 0).toLocaleString()}
            </div>
          )}
          <div className="text-xs text-gray-400 mt-1">
            <Link href="/exceptions" className="text-red-500 hover:underline">View exceptions ›</Link>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-100 p-4">
          <div className="p-2 rounded-xl bg-green-50 w-fit mb-3">
            <span className="material-symbols-outlined text-green-500">done_all</span>
          </div>
          <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-0.5">Delivered</div>
          {loading ? (
            <div className="text-3xl font-bold text-gray-300">…</div>
          ) : (
            <div className="text-3xl font-bold text-green-600">{(summary?.delivered ?? 0).toLocaleString()}</div>
          )}
          <div className="text-xs text-gray-400 mt-1">Confirmed email deliveries</div>
        </div>
      </div>

      {/* ── Today's Jobs table ─────────────────────────────────────────── */}
      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100">
          <div className="text-base font-semibold text-gray-900">Today&apos;s Jobs</div>
        </div>
        <div className="overflow-x-auto">
          {loading ? (
            <div className="py-12 text-center text-gray-400">
              <span className="material-symbols-outlined text-3xl animate-spin mb-2">sync</span>
              <p>Loading jobs…</p>
            </div>
          ) : todayJobs.length === 0 ? (
            <div className="py-12 text-center text-gray-400">
              <span className="material-symbols-outlined text-3xl mb-2">assignment</span>
              <p>No jobs processed today</p>
            </div>
          ) : (
            <table className="w-full text-left">
              <thead>
                <tr className="t-hd">
                  <th className="px-3 py-3 w-8" />
                  <th className="px-3 py-3">JOB ID</th>
                  <th className="px-3 py-3">SEGMENT</th>
                  <th className="px-3 py-3 text-right">RECORDS</th>
                  <th className="px-3 py-3">STATUS</th>
                  <th className="px-3 py-3">DURATION</th>
                  <th className="px-3 py-3 text-right">PDF FAIL</th>
                  <th className="px-3 py-3 text-right">EMAIL FAIL</th>
                  <th className="px-3 py-3 text-right">DELIVERED</th>
                </tr>
              </thead>
              <tbody>
                {todayJobs.map((j) => (
                  <React.Fragment key={j.jobId}>
                    <tr className="t-row">
                      <td className="px-3 py-3">
                        <button
                          onClick={() => setExpandedJob(expandedJob === j.jobId ? null : j.jobId)}
                          className="text-gray-400 hover:text-gray-700"
                        >
                          <span className={cn(
                            "material-symbols-outlined text-sm transition-transform duration-200",
                            expandedJob === j.jobId ? "rotate-90" : "",
                          )}>
                            chevron_right
                          </span>
                        </button>
                      </td>
                      <td className="px-3 py-3 mono text-xs font-bold text-gray-700">{j.jobId}</td>
                      <td className="px-3 py-3"><span className="seg-chip">{j.segment}</span></td>
                      <td className="px-3 py-3 text-right mono text-xs">{j.records.toLocaleString()}</td>
                      <td className="px-3 py-3"><StatusBadge status={j.status} /></td>
                      <td className="px-3 py-3 text-xs text-gray-500 mono">{j.duration ?? "—"}</td>
                      <td className="px-3 py-3 text-right text-xs">
                        {j.pdfFail > 0 ? <Link href="/exceptions" className="text-red-600 font-bold hover:underline">{j.pdfFail}</Link> : <span className="text-gray-300">—</span>}
                      </td>
                      <td className="px-3 py-3 text-right text-xs">
                        {j.emailFail > 0 ? <Link href="/exceptions" className="text-red-600 font-bold hover:underline">{j.emailFail.toLocaleString()}</Link> : <span className="text-gray-300">—</span>}
                      </td>
                      <td className="px-3 py-3 text-right text-xs font-semibold text-green-700">
                        {j.delivered > 0 ? j.delivered.toLocaleString() : <span className="text-gray-300">—</span>}
                      </td>
                    </tr>
                    {expandedJob === j.jobId && (
                      <tr className="bg-gray-50">
                        <td colSpan={9} className="px-6 py-3">
                          <div className="flex items-center gap-6 text-xs text-gray-600 flex-wrap">
                            <span>Full Job ID: <span className="mono font-semibold text-gray-800">{j.jobId}</span></span>
                            <span>Records: <span className="font-semibold">{j.records.toLocaleString()}</span></span>
                            <span>Delivered: <span className="font-semibold text-green-700">{j.delivered.toLocaleString()}</span></span>
                            <Link href={`/jobs/${j.jobId}`} className="text-[#497cff] font-semibold hover:underline ml-auto flex items-center gap-0.5">
                              View full detail <span className="material-symbols-outlined text-sm">arrow_forward</span>
                            </Link>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* ── Issue Tables ───────────────────────────────────────────────── */}
      <div className="grid md:grid-cols-2 gap-5">
        {[
          { title: "Application Issues", issues: appIssues, accent: "bg-red-100 text-red-700" },
          { title: "Infrastructure Issues", issues: infraIssues, accent: "bg-amber-100 text-amber-700" },
        ].map(({ title, issues, accent }) => (
          <div key={title} className="bg-white rounded-xl border overflow-hidden">
            <div className="px-5 py-4 border-b border-gray-100 flex items-center gap-2">
              <div className="text-base font-semibold text-gray-900">{title}</div>
              <span className={cn("ml-auto px-2 py-0.5 rounded-full text-[10px] font-bold border", accent)}>
                {loading ? "…" : `${issues.length} issue${issues.length !== 1 ? "s" : ""}`}
              </span>
            </div>
            {loading ? (
              <div className="py-8 text-center text-gray-400 text-xs">
                <span className="material-symbols-outlined text-2xl animate-spin mb-1">sync</span>
                <p>Loading…</p>
              </div>
            ) : issues.length === 0 ? (
              <div className="py-8 text-center text-gray-400 text-xs">
                <p>No issues</p>
              </div>
            ) : (
              <table className="w-full text-[12px]">
                <tbody>
                  {issues.map((issue) => (
                    <tr key={issue.id} className="t-row">
                      <td className="px-4 py-3 w-6">
                        <span className={cn("w-2 h-2 rounded-full block", SEVERITY_DOT[issue.severity] ?? "bg-gray-400")} />
                      </td>
                      <td className="px-2 py-3 flex-1">
                        <div className="font-semibold text-gray-800">{issue.name}</div>
                        <div className="text-[10px] text-gray-400 mt-0.5">{issue.description}</div>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <span className="px-2 py-0.5 bg-gray-100 text-gray-600 rounded-full text-[10px] font-bold">
                          {issue.count.toLocaleString()}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        ))}
      </div>

      {/* ── System Health ──────────────────────────────────────────────── */}
      <div className="bg-white rounded-xl border p-4">
        <div className="text-base font-semibold text-gray-900 mb-4">System Health</div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="flex flex-col gap-1">
            <div className="text-xs text-gray-400">Active SES Config</div>
            <div className="text-sm font-medium text-gray-900">{health?.activeSesConfig?.name ?? "None"}</div>
          </div>
          <div className="flex flex-col gap-1">
            <div className="text-xs text-gray-400">PFX Days Remaining</div>
            <div className={cn("text-sm font-medium", (health?.pfxDaysRemaining ?? 0) <= 30 ? "text-red-600" : "text-gray-900")}>
              {health?.pfxDaysRemaining ?? 0} days
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <div className="text-xs text-gray-400">Stuck Jobs</div>
            <div className={cn("text-sm font-medium", (health?.jobsStuckCount ?? 0) > 0 ? "text-red-600" : "text-green-600")}>
              {health?.jobsStuckCount ?? 0}
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <div className="text-xs text-gray-400">High Bounce Jobs</div>
            <div className={cn("text-sm font-medium", (health?.highBounceJobs?.length ?? 0) > 0 ? "text-amber-600" : "text-green-600")}>
              {health?.highBounceJobs?.length ?? 0}
            </div>
          </div>
        </div>
      </div>

      {/* ── Charts Row ────────────────────────────────────────────────── */}
      <div className="grid md:grid-cols-2 gap-5">
        <div className="bg-white rounded-xl border p-5">
          <div className="text-base font-semibold text-gray-900 mb-4">Hourly Failures</div>
          {loading ? (
            <div className="h-[200px] flex items-center justify-center text-gray-400">
              <span className="material-symbols-outlined text-3xl animate-spin">sync</span>
            </div>
          ) : hourlyData.length === 0 ? (
            <div className="h-[200px] flex items-center justify-center text-gray-400 text-sm">
              No failure data
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={hourlyData.map((d) => ({ label: d.hour, "App Failures": d.appFailures, "Infra Failures": d.infraFailures }))} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                <XAxis dataKey="label" tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip content={<CustomTooltip />} />
                <Legend iconSize={8} wrapperStyle={{ fontSize: 11, paddingTop: 8 }} />
                <Bar dataKey="App Failures"   stackId="a" fill="#497cff" />
                <Bar dataKey="Infra Failures" stackId="a" fill="#ef4444" radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="bg-white rounded-xl border p-5">
          <div className="text-base font-semibold text-gray-900 mb-4">Failure Distribution</div>
          {loading ? (
            <div className="h-[200px] flex items-center justify-center text-gray-400">
              <span className="material-symbols-outlined text-3xl animate-spin">sync</span>
            </div>
          ) : distData.length === 0 ? (
            <div className="h-[200px] flex items-center justify-center text-gray-400 text-sm">
              No distribution data
            </div>
          ) : (
            <div className="relative">
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie data={distData} cx="50%" cy="50%" innerRadius={55} outerRadius={80} dataKey="count" paddingAngle={2}>
                    {distData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                  <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} formatter={(value, entry: any) => entry?.payload?.type ?? value} />
                </PieChart>
              </ResponsiveContainer>
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none" style={{ marginTop: -20 }}>
                <div className="text-center">
                  <div className="text-xl font-extrabold text-gray-900">{totalDist || 0}</div>
                  <div className="text-[10px] text-gray-400">total</div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
