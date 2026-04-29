"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/store/auth";
import { cn } from "@/lib/utils";
import { SparklineChart } from "@/components/ui/sparkline";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer,
  ComposedChart, Area, Line, CartesianGrid,
} from "recharts";

const USE_MOCK = true;

// ─── Mock data ────────────────────────────────────────────────────────────────
const MOCK_METRICS = {
  totalCustomers: 28200,
  emailSentCount: 27560,
  emailDeliveredCount: 26190,
  failedCount: 318,
  pdfGeneratedCount: 27800,
  emailBouncedCount: 640,
};

const MOCK_MONTHLY = [
  { month: "Jan", delivered: 22400, bounced: 580, failed: 290 },
  { month: "Feb", delivered: 24100, bounced: 620, failed: 310 },
  { month: "Mar", delivered: 21800, bounced: 540, failed: 260 },
  { month: "Apr", delivered: 25300, bounced: 710, failed: 350 },
  { month: "May", delivered: 23600, bounced: 590, failed: 280 },
  { month: "Jun", delivered: 26100, bounced: 680, failed: 320 },
  { month: "Jul", delivered: 24800, bounced: 640, failed: 300 },
  { month: "Aug", delivered: 25900, bounced: 720, failed: 380 },
  { month: "Sep", delivered: 22700, bounced: 560, failed: 270 },
  { month: "Oct", delivered: 27100, bounced: 750, failed: 400 },
  { month: "Nov", delivered: 26400, bounced: 690, failed: 340 },
  { month: "Dec", delivered: 28200, bounced: 640, failed: 318 },
];

const MOCK_RATE = [
  { month: "Jan", openRate: 58.2, bounceRate: 1.8 },
  { month: "Feb", openRate: 60.1, bounceRate: 1.6 },
  { month: "Mar", openRate: 57.8, bounceRate: 2.1 },
  { month: "Apr", openRate: 61.4, bounceRate: 1.7 },
  { month: "May", openRate: 59.3, bounceRate: 1.9 },
  { month: "Jun", openRate: 62.7, bounceRate: 1.5 },
  { month: "Jul", openRate: 60.5, bounceRate: 1.8 },
  { month: "Aug", openRate: 58.9, bounceRate: 2.0 },
  { month: "Sep", openRate: 63.1, bounceRate: 1.4 },
  { month: "Oct", openRate: 61.8, bounceRate: 1.7 },
  { month: "Nov", openRate: 59.6, bounceRate: 1.9 },
  { month: "Dec", openRate: 58.3, bounceRate: 2.5 },
];

const MOCK_EMAIL_TILES = [
  { label: "SENDS 7D",       value: "4,218", unit: "/7d", color: "#497cff", sparkline: [380, 420, 390, 450, 410, 480, 420] },
  { label: "OPEN RATE",      value: "58.3",  unit: "%",   color: "#22c55e", sparkline: [55, 57, 56, 59, 58, 60, 58] },
  { label: "REJECT RATE",    value: "0.4",   unit: "%",   color: "#ef4444", sparkline: [0.3, 0.5, 0.4, 0.3, 0.4, 0.5, 0.4] },
  { label: "BOUNCE RATE",    value: "1.9",   unit: "%",   color: "#f59e0b", sparkline: [1.8, 2.1, 1.9, 2.0, 1.8, 1.9, 1.9] },
  { label: "COMPLAINT RATE", value: "0.02",  unit: "%",   color: "#a855f7", sparkline: [0.01, 0.02, 0.02, 0.01, 0.02, 0.02, 0.01] },
];

const MOCK_ACTIVITY = [
  { icon: "check_circle",   iconColor: "text-green-500",  desc: "JOB-0428-01 completed — 26,100 delivered",   time: "2m ago" },
  { icon: "picture_as_pdf", iconColor: "text-purple-500", desc: "PDF batch ready — 28,200 files",             time: "8m ago" },
  { icon: "send",           iconColor: "text-blue-500",   desc: "Email dispatch started — JOB-0428-02",       time: "12m ago" },
  { icon: "warning",        iconColor: "text-amber-500",  desc: "3 bounce events on JOB-0427-01",             time: "1h ago" },
  { icon: "upload_file",    iconColor: "text-gray-500",   desc: "JOB-0427-01 uploaded by admin@geojit.com",   time: "3h ago" },
];

const PIPELINE_ROWS = [
  { label: "Records Uploaded", pct: 100,  bar: "bg-blue-500",   indent: false },
  { label: "PDFs Generated",   pct: 98.6, bar: "bg-purple-500", indent: false },
  { label: "Emails Sent",      pct: 97.5, bar: "bg-green-500",  indent: false },
  { label: "Delivered",        pct: 94.9, bar: "bg-teal-500",   indent: false },
  { label: "↳ Bounced",        pct: 3.4,  bar: "bg-amber-400",  indent: true  },
];

// ─── Helpers ─────────────────────────────────────────────────────────────────
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

// ─── KPI Card ────────────────────────────────────────────────────────────────
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
      <div
        className={cn(
          "text-3xl font-bold text-gray-900",
          label === "RECORDS FAILED" && Number(value) > 0 && "text-red-600",
        )}
      >
        {typeof value === "number" ? value.toLocaleString() : value}
      </div>
      <div className="text-xs text-gray-400 leading-tight">{desc}</div>
      {clickable && href && label === "RECORDS FAILED" && (
        <Link
          href={href}
          className="text-red-500 text-xs hover:underline mt-0.5"
          onClick={(e) => e.stopPropagation()}
        >
          View exceptions ›
        </Link>
      )}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [filter, setFilter] = useState<"today" | "7d" | "month">("today");
  const [chartYear, setChartYear] = useState(new Date().getFullYear());
  const [chartType, setChartType] = useState<"bar" | "line">("bar");

  const metrics = MOCK_METRICS;

  const today = new Date().toLocaleDateString("en-GB", {
    weekday: "long", day: "numeric", month: "long", year: "numeric",
    timeZone: "Asia/Kolkata",
  });

  const filterLabel =
    filter === "today" ? "Today" : filter === "7d" ? "Last 7 days" : "This month";

  void chartType; // used for toggle state, chart type switching could be extended

  return (
    <div className="space-y-5 max-w-[1600px] mx-auto w-full fade-up">

      {/* ── A) Header row ───────────────────────────────────────────────── */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-bold text-2xl text-gray-900">{greeting(user?.name)}</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Today&apos;s contract-note pipeline · {today}
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            href="/process"
            className="flex items-center gap-1.5 px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors"
          >
            <span className="material-symbols-outlined text-[18px]">upload_file</span>
            Process File
          </Link>
          <Link
            href="/jobs"
            className="flex items-center gap-1.5 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50 transition-colors"
          >
            <span className="material-symbols-outlined text-[18px]">history</span>
            View Runs
          </Link>
        </div>
      </div>

      {/* ── B) Filter bar ───────────────────────────────────────────────── */}
      <div className="bg-white rounded-xl border p-2 flex items-center gap-2">
        <span className="material-symbols-outlined text-gray-400 text-[16px]">filter_list</span>
        <span className="text-sm text-gray-500">View:</span>
        {(["today", "7d", "month"] as const).map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={cn(
              "px-3 py-1 rounded-lg text-sm font-medium transition-colors",
              filter === f ? "bg-[#00174b] text-white" : "text-gray-600 hover:bg-gray-100",
            )}
          >
            {f === "today" ? "Today" : f === "7d" ? "Last 7d" : "This month"}
          </button>
        ))}
        <span className="ml-auto text-sm text-gray-400 italic">
          Showing data for: {filterLabel}
        </span>
      </div>

      {/* ── C) 6 KPI cards ─────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <KpiCard icon="group"           iconColor="text-blue-500"   label="TOTAL RECORDS"   value={metrics.totalCustomers}     desc="Total records in current pipeline run" />
        <KpiCard icon="send"            iconColor="text-green-500"  label="EMAILS SENT"     value={metrics.emailSentCount}     desc="Submitted to SES for delivery" />
        <KpiCard icon="mark_email_read" iconColor="text-teal-500"   label="EMAIL DELIVERED" value={metrics.emailDeliveredCount} desc="SNS-confirmed inbox delivery" />
        <KpiCard icon="error"           iconColor="text-red-500"    label="RECORDS FAILED"  value={metrics.failedCount}        desc="Records rejected — validation or send failure" clickable href="/exceptions" borderHover="hover:border-red-200" />
        <KpiCard icon="picture_as_pdf"  iconColor="text-purple-500" label="PDFS GENERATED"  value={metrics.pdfGeneratedCount}  desc="Contract notes ready for dispatch" />
        <KpiCard icon="cancel"          iconColor="text-orange-500" label="BOUNCED EMAILS"  value={metrics.emailBouncedCount}  desc="Invalid or inactive recipient address" clickable href="/resend?status=BOUNCED" borderHover="hover:border-orange-200" />
      </div>

      {/* ── D) Amber banner ─────────────────────────────────────────────── */}
      {metrics.totalCustomers === 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-amber-500 text-[18px]">info</span>
            <span className="text-sm text-amber-800">No contract notes processed today.</span>
          </div>
          <button
            onClick={() => setFilter("7d")}
            className="text-amber-600 text-sm font-medium underline"
          >
            View last 7 days
          </button>
        </div>
      )}

      {/* ── E) Charts row ──────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">

        {/* Monthly Volume */}
        <div className="bg-white rounded-xl border p-4">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="text-base font-semibold text-gray-900">Monthly Volume</div>
              <div className="text-xs text-gray-400 mt-0.5">
                Delivered, bounced and failed — absolute counts
              </div>
            </div>
            <div className="flex items-center gap-1.5">
              <select
                value={chartYear}
                onChange={(e) => setChartYear(Number(e.target.value))}
                className="px-2 py-1 border border-gray-200 rounded-lg text-xs font-medium bg-white"
              >
                {[2024, 2025, 2026].map((y) => <option key={y} value={y}>{y}</option>)}
              </select>
              <button
                onClick={() => setChartType("bar")}
                className={cn("p-1.5 rounded-lg", chartType === "bar" ? "bg-gray-200" : "hover:bg-gray-100")}
                title="Bar chart"
              >
                <span className="material-symbols-outlined text-[16px] text-gray-600">bar_chart</span>
              </button>
              <button
                onClick={() => setChartType("line")}
                className={cn("p-1.5 rounded-lg", chartType === "line" ? "bg-gray-200" : "hover:bg-gray-100")}
                title="Line chart"
              >
                <span className="material-symbols-outlined text-[16px] text-gray-600">show_chart</span>
              </button>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={MOCK_MONTHLY} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
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
          </ResponsiveContainer>
        </div>

        {/* Open Rate & Bounce Rate */}
        <div className="bg-white rounded-xl border p-4">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="text-base font-semibold text-gray-900">Open Rate &amp; Bounce Rate</div>
              <div className="text-xs text-gray-400 mt-0.5">
                Engagement and reputation health — % rates over 12 months
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-green-500 flex-shrink-0" />
                <span className="text-xs text-gray-500">Open rate %</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-5 inline-block border-t-2 border-dashed border-amber-400" />
                <span className="text-xs text-gray-500">Bounce rate %</span>
              </div>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <ComposedChart data={MOCK_RATE} margin={{ top: 4, right: 16, left: -20, bottom: 0 }}>
              <CartesianGrid stroke="#f3f4f6" vertical={false} />
              <XAxis dataKey="month" tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
              <YAxis
                yAxisId="left" domain={[40, 70]}
                tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false}
                tickFormatter={(v: number) => `${v}%`}
              />
              <YAxis
                yAxisId="right" orientation="right" domain={[0, 5]}
                tick={{ fontSize: 10, fill: "#9ca3af" }} axisLine={false} tickLine={false}
                tickFormatter={(v: number) => `${v}%`}
              />
              <Tooltip content={<CustomTooltip />} />
              <Area
                yAxisId="left" type="monotone" dataKey="openRate" name="Open Rate"
                stroke="#22c55e" fill="#22c55e" fillOpacity={0.1} strokeWidth={2} dot={false}
              />
              <Line
                yAxisId="right" type="monotone" dataKey="bounceRate" name="Bounce Rate"
                stroke="#f59e0b" strokeDasharray="4 4" strokeWidth={2} dot={false}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* ── F) Email Analytics Band ─────────────────────────────────────── */}
      <div className="bg-white rounded-xl border p-4">
        <div className="flex items-center justify-between mb-3">
          <div>
            <div className="text-base font-semibold text-gray-900">Email Analytics</div>
            <div className="text-xs text-gray-400 mt-0.5">
              SES sending statistics — Asia Pacific (Mumbai) · last 7 days
            </div>
          </div>
          <Link href="/email-analytics" className="text-sm text-[#497cff] hover:underline font-medium">
            Full analytics →
          </Link>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mt-3">
          {MOCK_EMAIL_TILES.map((tile) => (
            <div key={tile.label} className="flex flex-col gap-1">
              <div className="text-[10px] uppercase tracking-widest text-gray-400 font-medium">
                {tile.label}
              </div>
              <div className="text-xl font-bold text-gray-900">
                {tile.value}
                <span className="text-xs text-gray-400 font-normal ml-0.5">{tile.unit}</span>
              </div>
              <SparklineChart data={tile.sparkline} color={tile.color} height={40} />
            </div>
          ))}
        </div>
      </div>

      {/* ── G) Pipeline Breakdown + Recent Activity ─────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-5">

        {/* Pipeline Breakdown (col-span-3) */}
        <div className="lg:col-span-3 bg-white rounded-xl border p-4">
          <div className="text-base font-semibold text-gray-900 mb-4">Pipeline Breakdown</div>
          <div className="space-y-4">
            {PIPELINE_ROWS.map((row) => (
              <div key={row.label} className={cn(row.indent && "ml-4")}>
                <div className="flex items-center justify-between mb-1">
                  <span className={cn("text-sm text-gray-700", row.indent && "text-xs text-gray-500")}>
                    {row.label}
                  </span>
                  <span className="text-sm text-gray-700 font-medium">{row.pct}%</span>
                </div>
                <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className={cn("h-full rounded-full", row.bar)}
                    style={{ width: `${row.pct}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Recent Activity (col-span-2) */}
        <div className="lg:col-span-2 bg-white rounded-xl border p-4">
          <div className="text-base font-semibold text-gray-900 mb-3">Recent Activity</div>
          <div className="space-y-3">
            {MOCK_ACTIVITY.map((item, i) => (
              <div key={i} className="flex items-start gap-2.5">
                <span
                  className={cn(
                    "material-symbols-outlined text-[18px] flex-shrink-0 mt-0.5",
                    item.iconColor,
                  )}
                >
                  {item.icon}
                </span>
                <div className="flex-1 min-w-0">
                  <div className="text-sm text-gray-700">{item.desc}</div>
                </div>
                <span className="text-xs text-gray-400 flex-shrink-0">{item.time}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
