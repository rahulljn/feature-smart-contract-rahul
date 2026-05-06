"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/store/auth";
import { cn } from "@/lib/utils";
import { dashboardApi, configApi } from "@/lib/api";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer,
  ComposedChart, Area, Line, CartesianGrid,
} from "recharts";

function greeting(name?: string) {
  const h = new Date().getHours();
  const sal = h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
  return `${sal}${name ? `, ${name.split(" ")[0]}` : ""}`;
}

interface TooltipPayloadItem { name: string; value: number; color: string; }
interface CustomTooltipProps { active?: boolean; payload?: TooltipPayloadItem[]; label?: string; }

function CustomTooltip({ active, payload, label }: CustomTooltipProps) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-white shadow-md rounded-lg px-3 py-2 text-xs border border-gray-100">
      {label && <div className="font-semibold text-gray-700 mb-1">{label}</div>}
      {payload.map((p) => (
        <div key={p.name} className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: p.color }} />
          <span className="text-gray-600">{p.name}:</span>
          <span className="font-bold text-gray-900">
            {typeof p.value === "number" ? p.value.toLocaleString() : p.value}
          </span>
        </div>
      ))}
    </div>
  );
}

function KpiCard({
  icon, iconColor, label, value, desc, clickable, href, borderHover,
}: {
  icon: string; iconColor: string; label: string; value: number | string;
  desc: string; clickable?: boolean; href?: string; borderHover?: string;
}) {
  const router = useRouter();
  return (
    <div
      className={cn(
        "bg-white rounded-xl border border-gray-100 p-4 flex flex-col gap-1 transition-all",
        clickable && "cursor-pointer hover:shadow-sm",
        clickable && borderHover,
      )}
      onClick={clickable && href ? () => router.push(href) : undefined}
    >
      <span className={cn("material-symbols-outlined text-[20px]", iconColor)}>{icon}</span>
      <div className="text-[10px] tracking-widest text-gray-400 font-medium uppercase mt-1">{label}</div>
      <div className={cn("text-3xl font-bold text-gray-900", label === "RECORDS FAILED" && Number(value) > 0 && "text-red-600")}>
        {typeof value === "number" ? value.toLocaleString() : value}
      </div>
      <div className="text-xs text-gray-400 leading-tight">{desc}</div>
      {clickable && href && label === "RECORDS FAILED" && (
        <Link href={href} className="text-red-500 text-xs hover:underline mt-0.5" onClick={(e) => e.stopPropagation()}>
          View exceptions ›
        </Link>
      )}
    </div>
  );
}

const eventStyle = (type: string): { icon: string; color: string } =>
  ({
    EMAIL_SENT:          { icon: "send",            color: "text-green-500"  },
    DELIVERY:            { icon: "mark_email_read",  color: "text-teal-500"  },
    EMAIL_FAILED:        { icon: "error",            color: "text-red-500"   },
    BOUNCE:              { icon: "mail_off",          color: "text-amber-500" },
    COMPLAINT:           { icon: "report",            color: "text-red-500"   },
    PDF_GENERATED:       { icon: "picture_as_pdf",   color: "text-purple-500"},
    PDF_FAILED:          { icon: "broken_image",     color: "text-red-500"   },
    PDF_TRIGGERED:       { icon: "picture_as_pdf",   color: "text-purple-400"},
    RESEND_TRIGGERED:    { icon: "refresh",           color: "text-blue-500"  },
    SPLIT_PROGRESS:      { icon: "splitscreen",       color: "text-gray-400"  },
    SPLIT_COMPLETE:      { icon: "splitscreen",       color: "text-gray-500"  },
    JOB_REGISTERED:      { icon: "work",              color: "text-blue-400"  },
    CUSTOMER_REGISTERED: { icon: "person_add",        color: "text-blue-400"  },
    EMAIL_SKIPPED:       { icon: "block",             color: "text-gray-400"  },
  } as Record<string, { icon: string; color: string }>)[type] ?? { icon: "info", color: "text-gray-400" };

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [filter, setFilter] = useState<"today" | "7d" | "month">("today");
  const [chartYear, setChartYear] = useState(new Date().getFullYear());
  const [chartType, setChartType] = useState<"bar" | "line">("bar");
  const [metrics, setMetrics] = useState<any>(null);
  const [monthlyData, setMonthlyData] = useState<any[]>([]);
  const [rateData, setRateData] = useState<any[]>([]);
  const [sesStats, setSesStats] = useState<any>(null);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        let from = new Date(), to = new Date();
        if (filter === "today") {
          from.setHours(0, 0, 0, 0);
          to.setHours(23, 59, 59, 999);
        } else if (filter === "7d") {
          from.setDate(from.getDate() - 7);
          from.setHours(0, 0, 0, 0);
          to.setHours(23, 59, 59, 999);
        } else {
          from.setDate(1);
          from.setHours(0, 0, 0, 0);
          to.setHours(23, 59, 59, 999);
        }
        const [metricsRes, monthlyRes, rateRes, sesRes] = await Promise.all([
          dashboardApi.metrics(from, to),
          dashboardApi.getMonthlyVolume(chartYear),
          dashboardApi.getDailySuccessRate(30),
          configApi.sesStatistics(),
        ]);
        setMetrics(metricsRes.data.data);
        setMonthlyData(monthlyRes.data.data ?? []);
        setRateData(rateRes.data.data ?? []);
        setSesStats(sesRes.data.data);
      } catch (err) {
        console.error("Dashboard fetch error:", err);
      }
    };
    fetchAll();
  }, [filter, chartYear]);

  const m = metrics ?? {
    totalCustomersInPipeline: 0, totalEmailsSent: 0, totalDelivered: 0,
    totalFailedRecords: 0, totalPdfsGenerated: 0, totalBounced: 0, recentActivity: [],
  };

  const today = new Date().toLocaleDateString("en-GB", {
    weekday: "long", day: "numeric", month: "long", year: "numeric",
    timeZone: "Asia/Kolkata",
  });

  const filterLabel = filter === "today" ? "Today" : filter === "7d" ? "Last 7 days" : "This month";

  return (
    <div className="space-y-5 fade-up">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-bold text-2xl text-gray-900">{greeting(user?.name)}</h1>
          <p className="text-sm text-gray-500 mt-0.5">Today&apos;s contract-note pipeline · {today}</p>
        </div>
        <div className="flex gap-2">
          <Link href="/process" className="flex items-center gap-1.5 px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors">
            <span className="material-symbols-outlined text-[18px]">upload_file</span>
            Process File
          </Link>
          <Link href="/jobs" className="flex items-center gap-1.5 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50 transition-colors">
            <span className="material-symbols-outlined text-[18px]">history</span>
            View Runs
          </Link>
        </div>
      </div>

      <div className="bg-white rounded-xl border p-2 flex items-center gap-2">
        <span className="material-symbols-outlined text-gray-400 text-[16px]">filter_list</span>
        <span className="text-sm text-gray-500">View:</span>
        {(["today", "7d", "month"] as const).map((f) => (
          <button key={f} onClick={() => setFilter(f)}
            className={cn("px-3 py-1 rounded-lg text-sm font-medium transition-colors",
              filter === f ? "bg-[#00174b] text-white" : "text-gray-600 hover:bg-gray-100")}>
            {f === "today" ? "Today" : f === "7d" ? "Last 7d" : "This month"}
          </button>
        ))}
        <span className="ml-auto text-sm text-gray-400 italic">Showing data for: {filterLabel}</span>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <KpiCard icon="group"           iconColor="text-blue-500"   label="TOTAL RECORDS"   value={m.totalCustomersInPipeline} desc="Total records in current pipeline run" />
        <KpiCard icon="send"            iconColor="text-green-500"  label="EMAILS SENT"     value={m.totalEmailsSent}          desc="Submitted to SES for delivery" />
        <KpiCard icon="mark_email_read" iconColor="text-teal-500"   label="EMAIL DELIVERED" value={m.totalDelivered}           desc="SNS-confirmed inbox delivery" />
        <KpiCard icon="error"           iconColor="text-red-500"    label="RECORDS FAILED"  value={m.totalFailedRecords}       desc="Records rejected — validation or send failure" clickable href="/exceptions" borderHover="hover:border-red-200" />
        <KpiCard icon="picture_as_pdf"  iconColor="text-purple-500" label="PDFS GENERATED"  value={m.totalPdfsGenerated}       desc="Contract notes ready for dispatch" />
        <KpiCard icon="cancel"          iconColor="text-orange-500" label="BOUNCED EMAILS"  value={m.totalBounced}             desc="Invalid or inactive recipient address" clickable href="/resend?status=BOUNCED" borderHover="hover:border-orange-200" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <div className="bg-white rounded-xl border p-4">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="text-base font-semibold text-gray-900">Monthly Volume</div>
              <div className="text-xs text-gray-400 mt-0.5">Delivered, bounced and failed — absolute counts</div>
            </div>
            <div className="flex items-center gap-1.5">
              <select value={chartYear} onChange={(e) => setChartYear(Number(e.target.value))}
                className="px-2 py-1 border border-gray-200 rounded-lg text-xs font-medium bg-white">
                {[2024, 2025, 2026].map((y) => <option key={y} value={y}>{y}</option>)}
              </select>
              <button onClick={() => setChartType("bar")} className={cn("p-1.5 rounded-lg", chartType === "bar" ? "bg-gray-200" : "hover:bg-gray-100")}>
                <span className="material-symbols-outlined text-[16px] text-gray-600">bar_chart</span>
              </button>
              <button onClick={() => setChartType("line")} className={cn("p-1.5 rounded-lg", chartType === "line" ? "bg-gray-200" : "hover:bg-gray-100")}>
                <span className="material-symbols-outlined text-[16px] text-gray-600">show_chart</span>
              </button>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            {chartType === "bar" ? (
              <BarChart data={monthlyData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid stroke="#f3f4f6" vertical={false} />
                <XAxis dataKey="month" tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false}
                  tickFormatter={(v: number) => v >= 1000 ? `${(v / 1000).toFixed(0)}k` : String(v)} />
                <Tooltip content={<CustomTooltip />} />
                <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="delivered" name="Delivered" stackId="a" fill="#22c55e" />
                <Bar dataKey="bounced"   name="Bounced"   stackId="a" fill="#f59e0b" />
                <Bar dataKey="failed"    name="Failed"    stackId="a" fill="#ef4444" radius={[2, 2, 0, 0]} />
              </BarChart>
            ) : (
              <ComposedChart data={monthlyData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid stroke="#f3f4f6" vertical={false} />
                <XAxis dataKey="month" tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false}
                  tickFormatter={(v: number) => v >= 1000 ? `${(v / 1000).toFixed(0)}k` : String(v)} />
                <Tooltip content={<CustomTooltip />} />
                <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} />
                <Area type="monotone" dataKey="delivered" name="Delivered" stroke="#22c55e" fill="#22c55e" fillOpacity={0.1} strokeWidth={2} dot={false} />
                <Area type="monotone" dataKey="bounced"   name="Bounced"   stroke="#f59e0b" fill="#f59e0b" fillOpacity={0.1} strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="failed"    name="Failed"    stroke="#ef4444" strokeWidth={2} dot={false} />
              </ComposedChart>
            )}
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-xl border p-4">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="text-base font-semibold text-gray-900">Delivery Rate &amp; Bounce Rate</div>
              <div className="text-xs text-gray-400 mt-0.5">Delivery and reputation health — % rates over 30 days</div>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-green-500 flex-shrink-0" />
                <span className="text-xs text-gray-500">Delivery rate %</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-5 inline-block border-t-2 border-dashed border-amber-400" />
                <span className="text-xs text-gray-500">Bounce rate %</span>
              </div>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <ComposedChart data={rateData} margin={{ top: 4, right: 16, left: -20, bottom: 0 }}>
              <CartesianGrid stroke="#f3f4f6" vertical={false} />
              <XAxis dataKey="month" tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
              <YAxis yAxisId="left" domain={[0, 100]} tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} tickFormatter={(v: number) => `${v}%`} />
              <YAxis yAxisId="right" orientation="right" domain={[0, "auto"]} tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} tickFormatter={(v: number) => `${v}%`} />
              <Tooltip content={<CustomTooltip />} />
              <Area yAxisId="left" type="monotone" dataKey="openRate" name="Delivery Rate" stroke="#22c55e" fill="#22c55e" fillOpacity={0.1} strokeWidth={2} dot={false} />
              <Line yAxisId="right" type="monotone" dataKey="bounceRate" name="Bounce Rate" stroke="#f59e0b" strokeDasharray="4 4" strokeWidth={2} dot={false} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="bg-white rounded-xl border p-4">
        <div className="flex items-center justify-between mb-3">
          <div>
            <div className="text-base font-semibold text-gray-900">Email Analytics</div>
            <div className="text-xs text-gray-400 mt-0.5">
              SES sending statistics · Asia Pacific (Mumbai) · last 24h
            </div>
          </div>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mt-3">
          {[
            { label: "SENDS 24H", value: sesStats?.sentLast24h ?? 0, unit: "" },
            { label: "BOUNCE RATE", value: sesStats?.bounceRate ?? 0, unit: "%" },
            { label: "COMPLAINT RATE", value: sesStats?.complaintRate ?? 0, unit: "%" },
            { label: "QUOTA USED", value: sesStats?.quotaUsedPercent ?? 0, unit: "%" },
            { label: "REMAINING", value: sesStats?.remainingSends ?? 0, unit: "" },
          ].map((tile) => (
            <div key={tile.label} className="flex flex-col gap-1">
              <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium">
                {tile.label}
              </div>
              <div className="text-xl font-bold text-gray-900">
                {tile.value}
                <span className="text-xs text-gray-400 font-normal ml-0.5">{tile.unit}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-5">
        <div className="lg:col-span-3 bg-white rounded-xl border p-4">
          <div className="text-base font-semibold text-gray-900 mb-4">Pipeline Breakdown</div>
          <div className="space-y-4">
            {(() => {
              const total = m.totalCustomersInPipeline || 1;
              const rows = [
                { label: "Records Uploaded", pct: m.totalCustomersInPipeline > 0 ? 100 : 0, bar: "bg-blue-500", indent: false },
                { label: "PDFs Generated", pct: m.totalCustomersInPipeline > 0 ? Math.round(m.totalPdfsGenerated / m.totalCustomersInPipeline * 1000) / 10 : 0, bar: "bg-purple-500", indent: false },
                { label: "Emails Sent", pct: m.totalCustomersInPipeline > 0 ? Math.round(m.totalEmailsSent / m.totalCustomersInPipeline * 1000) / 10 : 0, bar: "bg-green-500", indent: false },
                { label: "Delivered", pct: m.totalCustomersInPipeline > 0 ? Math.round(m.totalDelivered / m.totalCustomersInPipeline * 1000) / 10 : 0, bar: "bg-teal-500", indent: false },
                { label: "↳ Bounced", pct: m.totalCustomersInPipeline > 0 ? Math.round(m.totalBounced / m.totalCustomersInPipeline * 1000) / 10 : 0, bar: "bg-amber-400", indent: true },
              ];
              return rows.map((row) => (
                <div key={row.label} className={cn(row.indent && "ml-4")}>
                  <div className="flex items-center justify-between mb-1">
                    <span className={cn("text-sm text-gray-700", row.indent && "text-xs text-gray-500")}>{row.label}</span>
                    <span className="text-sm text-gray-700 font-medium">{row.pct}%</span>
                  </div>
                  <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                    <div className={cn("h-full rounded-full", row.bar)} style={{ width: `${row.pct}%` }} />
                  </div>
                </div>
              ));
            })()}
          </div>
        </div>

        <div className="lg:col-span-2 bg-white rounded-xl border p-4">
          <div className="text-base font-semibold text-gray-900 mb-3">Recent Activity</div>
          <div className="space-y-3">
            {(m.recentActivity ?? []).length === 0 ? (
              <div className="text-sm text-gray-400 text-center py-6">No recent activity</div>
            ) : (
              (m.recentActivity ?? []).map((item: any, i: number) => (
                <div key={i} className="flex items-start gap-2.5">
                  <span className={cn("material-symbols-outlined text-[18px] flex-shrink-0 mt-0.5", eventStyle(item.eventType).color)}>
                    {eventStyle(item.eventType).icon}
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm text-gray-700">{item.description}</div>
                  </div>
                  <span className="text-xs text-gray-400 flex-shrink-0">
                    {new Date(item.eventTimestamp).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
