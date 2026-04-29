"use client";

import React, { useState } from "react";
import { cn } from "@/lib/utils";
import Link from "next/link";
import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from "recharts";

const USE_MOCK = true;

// ─── Mock data ────────────────────────────────────────────────────────────────
const MOCK_SUMMARY = { totalRecords: 49542, inProgress: 1, failures: 3100, delivered: 29300 };

const MOCK_TODAY_JOBS = [
  { id: "JOB-0428-01", segment: "EQUITY-COMBINEMARGIN", records: 28200, status: "COMPLETED",  duration: "1h 30m",  pdfFail: 142,  emailFail: 318,  delivered: 26100 },
  { id: "JOB-0428-02", segment: "COMMODITY",            records: 5842,  status: "EMAILING",   duration: "In prog", pdfFail: 8,    emailFail: 15,   delivered: 3200  },
  { id: "JOB-0428-03", segment: "DP-HOLDING",           records: 12400, status: "PROCESSING", duration: "In prog", pdfFail: 0,    emailFail: 0,    delivered: 0     },
  { id: "JOB-0428-04", segment: "PNL",                  records: 3100,  status: "FAILED",     duration: "15m",     pdfFail: 0,    emailFail: 3100, delivered: 0     },
];

const MOCK_APP_ISSUES = [
  { id: "1", name: "PDF Generation Timeout", severity: "HIGH",   count: 12, description: "Lambda timeout on large PDFs (>2MB)" },
  { id: "2", name: "Email Template Missing", severity: "MEDIUM", count: 3,  description: "Active template not found in S3" },
  { id: "3", name: "Bounce Rate Elevated",   severity: "HIGH",   count: 87, description: "JOB-0428-01 bounce rate at 6.2%" },
  { id: "4", name: "Suppression List Stale", severity: "LOW",    count: 1,  description: "Last sync >24h ago" },
];

const MOCK_INFRA_ISSUES = [
  { id: "5", name: "SES Quota at 87%",    severity: "HIGH",   count: 1,  description: "18,700/21,600 daily sends used today" },
  { id: "6", name: "Lambda Timeout",      severity: "MEDIUM", count: 3,  description: "create-pdf-geojit: 3 timeouts (1h)" },
  { id: "7", name: "S3 Latency Elevated", severity: "LOW",    count: 1,  description: "pdf-s3-geojit: avg 450ms (>200ms)" },
];

const MOCK_HOURLY = Array.from({ length: 24 }, (_, hour) => ({
  label: `${String(hour).padStart(2, "0")}`,
  "App Failures":   hour >= 8 && hour <= 14 ? Math.floor(Math.random() * 15) : 0,
  "Infra Failures": hour >= 9 && hour <= 12 ? Math.floor(Math.random() * 5)  : 0,
}));

const MOCK_DISTRIBUTION = [
  { name: "PDF Failed",        value: 14,  color: "#8b5cf6" },
  { name: "Email Failed",      value: 91,  color: "#ef4444" },
  { name: "Lambda Error",      value: 43,  color: "#f59e0b" },
  { name: "Validation Failed", value: 12,  color: "#64748b" },
];

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

interface TooltipPayloadItem { dataKey: string; name: string; value: number; color: string; }
interface CTooltipProps { active?: boolean; payload?: TooltipPayloadItem[]; label?: string; }
function CustomTooltip({ active, payload, label }: CTooltipProps) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-white border border-gray-200 rounded-xl shadow-lg px-3 py-2 text-xs">
      {label && <div className="font-semibold text-gray-700 mb-1">{label}</div>}
      {payload.map((p) => (
        <div key={p.dataKey} className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: p.color }} />
          <span className="text-gray-600">{p.name}:</span>
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

  const summary = MOCK_SUMMARY;
  const todayJobs = MOCK_TODAY_JOBS;
  const appIssues = MOCK_APP_ISSUES;
  const infraIssues = MOCK_INFRA_ISSUES;
  const distData = MOCK_DISTRIBUTION;
  const totalDist = distData.reduce((s, d) => s + d.value, 0);

  void refreshKey; // used to trigger re-render on refresh

  return (
    <div className="space-y-6 w-full fade-up">

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
            className="flex items-center gap-1.5 px-3 py-1.5 border border-gray-200 rounded-lg text-sm text-gray-600 hover:bg-gray-50 transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">refresh</span>
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
          <div className="text-3xl font-bold text-gray-900">{summary.totalRecords.toLocaleString()}</div>
          <div className="text-xs text-gray-400 mt-1">Records processed today</div>
        </div>

        <div className="bg-white rounded-xl border border-gray-100 p-4">
          <div className="p-2 rounded-xl bg-indigo-50 w-fit mb-3 flex items-center gap-1">
            <span className="material-symbols-outlined text-indigo-500">pending</span>
            {summary.inProgress > 0 && <span className="w-2 h-2 rounded-full bg-indigo-500 animate-pulse" />}
          </div>
          <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-0.5">In Progress</div>
          <div className={cn("text-3xl font-bold", summary.inProgress > 0 ? "text-indigo-600" : "text-gray-900")}>
            {summary.inProgress}
          </div>
          <div className="text-xs text-gray-400 mt-1">Jobs currently running</div>
        </div>

        <div className="bg-white rounded-xl border border-gray-100 p-4 cursor-pointer hover:border-red-200 transition-colors group">
          <div className="p-2 rounded-xl bg-red-50 w-fit mb-3">
            <span className="material-symbols-outlined text-red-500">error</span>
          </div>
          <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-0.5">Failures</div>
          <div className={cn("text-3xl font-bold", summary.failures > 0 ? "text-red-600" : "text-gray-900")}>
            {summary.failures.toLocaleString()}
          </div>
          <div className="text-xs text-gray-400 mt-1">
            <Link href="/exceptions" className="text-red-500 hover:underline">View exceptions ›</Link>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-100 p-4">
          <div className="p-2 rounded-xl bg-green-50 w-fit mb-3">
            <span className="material-symbols-outlined text-green-500">done_all</span>
          </div>
          <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium mb-0.5">Delivered</div>
          <div className="text-3xl font-bold text-green-600">{summary.delivered.toLocaleString()}</div>
          <div className="text-xs text-gray-400 mt-1">Confirmed email deliveries</div>
        </div>
      </div>

      {/* ── Today's Jobs table ─────────────────────────────────────────── */}
      <div className="bg-white rounded-xl border overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100">
          <div className="text-base font-semibold text-gray-900">Today&apos;s Jobs</div>
        </div>
        <div className="overflow-x-auto">
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
                <React.Fragment key={j.id}>
                  <tr className="t-row">
                    <td className="px-3 py-3">
                      <button
                        onClick={() => setExpandedJob(expandedJob === j.id ? null : j.id)}
                        className="text-gray-400 hover:text-gray-700"
                      >
                        <span className={cn(
                          "material-symbols-outlined text-sm transition-transform duration-200",
                          expandedJob === j.id ? "rotate-90" : "",
                        )}>
                          chevron_right
                        </span>
                      </button>
                    </td>
                    <td className="px-3 py-3 mono text-xs font-bold text-gray-700">{j.id}</td>
                    <td className="px-3 py-3"><span className="seg-chip">{j.segment}</span></td>
                    <td className="px-3 py-3 text-right mono text-xs">{j.records.toLocaleString()}</td>
                    <td className="px-3 py-3"><StatusBadge status={j.status} /></td>
                    <td className="px-3 py-3 text-xs text-gray-500 mono">{j.duration}</td>
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
                  {expandedJob === j.id && (
                    <tr className="bg-gray-50">
                      <td colSpan={9} className="px-6 py-3">
                        <div className="flex items-center gap-6 text-xs text-gray-600 flex-wrap">
                          <span>Full Job ID: <span className="mono font-semibold text-gray-800">{j.id}</span></span>
                          <span>Records: <span className="font-semibold">{j.records.toLocaleString()}</span></span>
                          <span>Delivered: <span className="font-semibold text-green-700">{j.delivered.toLocaleString()}</span></span>
                          <Link href={`/jobs`} className="text-[#497cff] font-semibold hover:underline ml-auto flex items-center gap-0.5">
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
                {issues.length} issue{issues.length !== 1 ? "s" : ""}
              </span>
            </div>
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
          </div>
        ))}
      </div>

      {/* ── Charts Row ────────────────────────────────────────────────── */}
      <div className="grid md:grid-cols-2 gap-5">
        <div className="bg-white rounded-xl border p-5">
          <div className="text-base font-semibold text-gray-900 mb-4">Hourly Failures</div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={MOCK_HOURLY} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
              <XAxis dataKey="label" tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} allowDecimals={false} />
              <Tooltip content={<CustomTooltip />} />
              <Legend iconSize={8} wrapperStyle={{ fontSize: 11, paddingTop: 8 }} />
              <Bar dataKey="App Failures"   stackId="a" fill="#497cff" />
              <Bar dataKey="Infra Failures" stackId="a" fill="#ef4444" radius={[3, 3, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-xl border p-5">
          <div className="text-base font-semibold text-gray-900 mb-4">Failure Distribution</div>
          <div className="relative">
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={distData} cx="50%" cy="50%" innerRadius={55} outerRadius={80} dataKey="value" paddingAngle={2}>
                  {distData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
                <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} />
              </PieChart>
            </ResponsiveContainer>
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none" style={{ marginTop: -20 }}>
              <div className="text-center">
                <div className="text-xl font-extrabold text-gray-900">{totalDist}</div>
                <div className="text-[10px] text-gray-400">total</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
