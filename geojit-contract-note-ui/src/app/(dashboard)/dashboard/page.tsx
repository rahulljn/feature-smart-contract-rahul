"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "@/lib/api";
import type { DashboardMetrics } from "@/types";
import { Loader2 } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import Link from "next/link";
import { useAuthStore } from "@/store/auth";
import { cn, toUtcDate } from "@/lib/utils";

const statusMeta: Record<string, { bg: string; text: string; dot: string }> = {
  COMPLETED:  { bg: "bg-[#ecfdf5]",  text: "text-[#047857]",  dot: "bg-[#10b981]" },
  PROCESSING: { bg: "bg-[#eff6ff]",  text: "text-[#1d4ed8]",  dot: "bg-[#3b82f6] animate-pulse" },
  EMAILING:   { bg: "bg-[#fffbeb]",  text: "text-[#b45309]",  dot: "bg-[#f59e0b]" },
  SPLITTING:  { bg: "bg-[#f3e8ff]",  text: "text-[#6d28d9]",  dot: "bg-[#a855f7] animate-pulse" },
  FAILED:     { bg: "bg-[#fef2f2]",  text: "text-[#b91c1c]",  dot: "bg-[#ef4444]" },
  PARTIAL:    { bg: "bg-[#fff7ed]",  text: "text-[#92400e]",  dot: "bg-[#f97316]" },
  VALIDATING: { bg: "bg-[#f8fafc]",  text: "text-[#475569]",  dot: "bg-[#64748b]" },
};

function greeting(name?: string) {
  const h = new Date().getHours();
  const salutation = h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
  return `${salutation}${name ? `, ${name.split(" ")[0]}` : ""}`;
}

const activityDot: Record<string, string> = {
  JOB_REGISTERED: "dot-ok", CUSTOMER_REGISTERED: "dot-info",
  PDF_GENERATED: "dot-ok", PDF_FAILED: "dot-err",
  EMAIL_SENT: "dot-ok", EMAIL_FAILED: "dot-err",
  DELIVERY: "dot-ok", BOUNCE: "dot-err", COMPLAINT: "dot-err",
  SPLIT_PROGRESS: "dot-info", SPLIT_COMPLETE: "dot-info", RESEND_TRIGGERED: "dot-info",
  PDF_TRIGGERED: "dot-info", EMAIL_SKIPPED: "dot-warn",
};

const normaliseCode = (code?: string) => code?.split("/")[0] ?? code;

const EVENT_TYPE_LABEL: Record<string, string> = {
  JOB_REGISTERED: "File registered",
  CUSTOMER_REGISTERED: "Record registered",
  SPLIT_PROGRESS: "Reading file",
  SPLIT_COMPLETE: "File read complete",
  PDF_TRIGGERED: "PDF creation started",
  PDF_GENERATED: "PDF created",
  PDF_FAILED: "PDF creation failed",
  EMAIL_SENT: "Email sent",
  EMAIL_FAILED: "Email failed",
  EMAIL_SKIPPED: "Email delivery failed",
  DELIVERY: "Delivered",
  BOUNCE: "Email bounced",
  COMPLAINT: "Spam report",
  RESEND_TRIGGERED: "Resend triggered",
};

export default function DashboardPage() {
  const user = useAuthStore(s => s.user);
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  const { data: metricsRes, isLoading: loadingMetrics } = useQuery({
    queryKey: ["dashboard-metrics", dateFrom, dateTo],
    queryFn: () => dashboardApi.metrics(dateFrom || undefined, dateTo || undefined),
    refetchInterval: 30_000,
  });

  const metrics: DashboardMetrics | undefined = metricsRes?.data?.data;
  const today = new Date().toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long", year: "numeric" });


  return (
    <div className="p-6 space-y-6 max-w-[1600px] mx-auto w-full fade-up">
      {/* Page header */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          {loadingMetrics
            ? <div className="h-9 w-64 bg-slate-100 animate-pulse rounded-lg" />
            : <h2 className="text-[1.7rem] font-extrabold text-slate-900 headline">
                {greeting(user?.name)}
              </h2>
          }
          <p className="text-slate-500 text-sm mt-1">
            Today&apos;s contract-note pipeline · <span className="font-semibold text-slate-700">{today}</span>
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <Link
            href="/process"
            className="flex items-center gap-1.5 px-4 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] transition-all shadow-lg shadow-blue-900/15"
          >
            <span className="material-symbols-outlined text-base">upload_file</span>
            Process File
          </Link>
          <Link
            href="/jobs"
            className="flex items-center gap-1.5 px-3.5 py-2.5 bg-white text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 transition-all border border-slate-200"
          >
            <span className="material-symbols-outlined text-base">history</span>
            View Runs
          </Link>
        </div>
      </div>

      {/* Date filter */}
      <div className="card p-3">
        <div className="flex flex-wrap items-center gap-3">
          <span className="material-symbols-outlined text-slate-400 text-base">filter_list</span>
          <div className="flex items-center gap-2">
            <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-sm" />
            <span className="text-slate-400 text-xs">to</span>
            <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-sm" />
          </div>
          <div className="flex gap-1">
            <button onClick={() => { setDateFrom(new Date().toISOString().slice(0, 10)); setDateTo(new Date().toISOString().slice(0, 10)); }} className={cn("px-2.5 py-1 rounded-lg text-[11px] font-semibold", !dateFrom && !dateTo ? "bg-[#00174b] text-white" : "bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-[#003ea8]")}>Today</button>
            <button onClick={() => { const d = new Date(); setDateFrom(new Date(d.getTime() - 7 * 86400000).toISOString().slice(0, 10)); setDateTo(d.toISOString().slice(0, 10)); }} className={cn("px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-[#003ea8]")}>Last 7d</button>
            <button onClick={() => { const d = new Date(); setDateFrom(new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)); setDateTo(d.toISOString().slice(0, 10)); }} className={cn("px-2.5 py-1 rounded-lg text-[11px] font-semibold bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-[#003ea8]")}>This month</button>
          </div>
          {(dateFrom || dateTo) && (
            <button onClick={() => { setDateFrom(""); setDateTo(""); }} className="p-1 text-slate-400 hover:text-slate-700">
              <span className="material-symbols-outlined text-sm">close</span>
            </button>
          )}
          <div className="flex-1" />
          <span className="text-[11px] text-slate-400">{!dateFrom && !dateTo ? "Showing today" : `${dateFrom || "..."} → ${dateTo || "..."}`}</span>
        </div>
      </div>

      {/* 6 Metric Cards */}
      {loadingMetrics ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {Array(6).fill(0).map((_, i) => (
            <div key={i} className="h-32 bg-slate-100 animate-pulse rounded-[1rem]" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <MetricCard
            title="Total customers"
            value={metrics?.totalCustomersInPipeline ?? 0}
            icon="groups"
            iconBg="bg-blue-50"
            iconColor="text-[#497cff]"
            badge={{ label: `${metrics?.activeJobs ?? 0} active`, ok: true }}
            sub="Unique clientCodes in pipeline"
          />
          <MetricCard
            title="Emails sent"
            value={metrics?.totalEmailsSent ?? 0}
            icon="task_alt"
            iconBg="bg-emerald-50"
            iconColor="text-emerald-600"
            sub="Submitted to SES for delivery"
          />
          <MetricCard
            title="Email delivered"
            value={metrics?.totalDelivered ?? 0}
            icon="mark_email_read"
            iconBg="bg-emerald-50"
            iconColor="text-emerald-600"
            badge={{ label: `${metrics?.totalEmailsSent ? Math.round((metrics.totalDelivered ?? 0) / metrics.totalEmailsSent * 100) : 0}% of sent`, ok: true }}
            sub="SNS-confirmed inbox delivery"
          />
          <MetricCard
            title="Records failed"
            value={metrics?.totalFailedRecords ?? 0}
            icon="error"
            iconBg="bg-rose-50"
            iconColor="text-rose-600"
            sub="Records rejected in Split or Invoke — never entered pipeline"
            subErr={!!metrics?.totalFailedRecords}
          />
          <MetricCard
            title="PDFs generated"
            value={metrics?.totalPdfsGenerated ?? 0}
            icon="picture_as_pdf"
            iconBg="bg-purple-50"
            iconColor="text-purple-600"
            sub="Contract notes ready"
          />
          <MetricCard
            title="Bounced emails"
            value={metrics?.totalBounced ?? 0}
            icon="mail_off"
            iconBg="bg-amber-50"
            iconColor="text-amber-600"
            badge={{ label: `${metrics?.bounceRate ?? 0}% rate`, ok: metrics?.bounceRate === 0 }}
            sub="Invalid or inactive email address"
            subErr={!!metrics?.totalBounced}
          />
        </div>
      )}

      {/* Empty state — no data today */}
      {!loadingMetrics && metrics && metrics.totalCustomersInPipeline === 0 && !dateFrom && !dateTo && (
        <div className="flex items-center gap-3 px-4 py-3 bg-amber-50 border border-amber-100 rounded-xl text-sm text-amber-700">
          <span className="material-symbols-outlined text-amber-400 text-base flex-shrink-0">info</span>
          <span>No contract notes processed today.</span>
          <button
            onClick={() => {
              const d = new Date();
              setDateFrom(new Date(d.getTime() - 7 * 86400000).toISOString().slice(0, 10));
              setDateTo(d.toISOString().slice(0, 10));
            }}
            className="ml-auto text-xs font-semibold text-[#003ea8] hover:underline flex-shrink-0"
          >
            View last 7 days
          </button>
        </div>
      )}

      {/* Pipeline Funnel + Recent Activity */}
      <div className="grid lg:grid-cols-3 gap-5">

        {/* Pipeline Stage Breakdown */}
        <div className="lg:col-span-2 card p-5">
          <div className="mb-5">
            <div className="font-bold text-slate-900 headline">Pipeline Breakdown</div>
            <div className="text-xs text-slate-500 mt-0.5">Where every record went — from upload to final delivery</div>
          </div>

          {metrics ? (() => {
            const total     = metrics.totalCustomersInPipeline;
            const pdfs      = metrics.totalPdfsGenerated;
            const sent      = metrics.totalEmailsSent;
            const bounced   = metrics.totalBounced;
            const snsConfirmed = metrics.totalDelivered ?? 0; // SNS-confirmed delivery receipts
            const failed   = metrics.totalFailedRecords;
            return (
              <div className="space-y-1">

                {/* Stage 1 — Upload */}
                <FunnelRow icon="upload_file" label="Records uploaded" value={total} total={total}
                  color="bg-slate-400" note="Raw records received from the trading system file" />

                {/* Stage 2 — PDF generation */}
                <FunnelRow icon="picture_as_pdf" label="PDFs generated" value={pdfs} total={total}
                  color="bg-purple-500"
                  note="Contract note PDF created — password-protected and digitally signed" />

                {/* Stage 3 — Email dispatch */}
                <FunnelRow icon="send" label="Emails sent" value={sent} total={total}
                  color="bg-indigo-400" note="Contract note PDF emailed to client" />

                {/* Delivery outcomes — indent */}
                <div className="ml-6 border-l-2 border-slate-200 pl-4 pt-1 pb-1 space-y-1">

                  <FunnelRow icon="mark_email_read" label="Delivered" value={snsConfirmed} total={total}
                    color="bg-emerald-500"
                    note={snsConfirmed > 0 ? `${snsConfirmed.toLocaleString()} server-confirmed delivery receipts` : "Awaiting server delivery receipts"} />

                  <div className="flex items-center gap-2 py-1.5 px-3 bg-amber-50 border border-amber-100 rounded-xl">
                    <span className="material-symbols-outlined text-amber-500 text-[16px]">unsubscribe</span>
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[11px] font-semibold text-amber-700">Bounced</span>
                        <span className="text-[11px] font-bold text-amber-600 mono">{bounced.toLocaleString()} · {sent > 0 ? ((bounced/sent)*100).toFixed(1) : 0}% of sent</span>
                      </div>
                      <div className="text-[10px] text-amber-500 mt-0.5">Email rejected by recipient mail server — invalid or inactive address. Use Resend tab to retry.</div>
                    </div>
                  </div>

                  {failed > 0 && (
                    <div className="flex items-center gap-2 py-1.5 px-3 bg-rose-50 border border-rose-100 rounded-xl">
                      <span className="material-symbols-outlined text-rose-400 text-[16px]">error</span>
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span className="text-[11px] font-semibold text-rose-700">Failed records</span>
                          <span className="text-[11px] font-bold text-rose-600 mono">{failed.toLocaleString()} · {total > 0 ? ((failed/total)*100).toFixed(1) : 0}%</span>
                        </div>
                        <div className="text-[10px] text-rose-400 mt-0.5">Could not generate PDF or send email. Use Resend tab to retry.</div>
                      </div>
                    </div>
                  )}

                </div>
              </div>
            );
          })() : (
            <div className="space-y-3">
              {Array(6).fill(0).map((_, i) => (
                <div key={i} className="h-10 bg-slate-100 animate-pulse rounded-xl" />
              ))}
            </div>
          )}
        </div>

        {/* Recent Pipeline Events */}
        <div className="card p-5">
          <div className="flex items-center justify-between mb-3">
            <div>
              <div className="font-bold text-slate-900 headline">Recent activity</div>
              <div className="text-[11px] text-slate-500 mt-0.5">Latest events today</div>
            </div>
            <Link href="/jobs" className="text-[11px] font-semibold text-[#003ea8] hover:underline">View all runs</Link>
          </div>
          <div className="space-y-2">
            {(metrics?.recentActivity ?? []).length > 0 ? (metrics?.recentActivity ?? []).map((item, i) => (
              <Link
                key={i}
                href={item.jobId ? `/jobs/${item.jobId}` : "#"}
                className={cn(
                  "flex items-start gap-2.5 p-2.5 bg-slate-50 rounded-xl hover:bg-blue-50 cursor-pointer transition-colors group",
                  !item.jobId && "pointer-events-none"
                )}
              >
                <span className={cn("dot mt-1.5 flex-shrink-0", activityDot[item.eventType] ?? "dot-info")} />
                <div className="min-w-0 flex-1">
                  <div className="text-xs font-semibold text-slate-800">{item.description.replace(/([A-Z0-9]+)\/\1/g, "$1")}</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">
                    {EVENT_TYPE_LABEL[item.eventType] ?? item.eventType.replace(/_/g, " ")} · {formatDistanceToNow(toUtcDate(item.eventTimestamp) ?? new Date(), { addSuffix: true })}
                  </div>
                </div>
                {item.jobId && <span className="material-symbols-outlined text-slate-300 group-hover:text-[#497cff] text-sm">chevron_right</span>}
              </Link>
            )) : (
              <div className="text-center text-slate-400 text-sm py-8">No pipeline events today</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function FunnelRow({
  label, icon, value, total, color, note, pct, warn, err,
}: {
  label: string; icon: string; value: number; total: number;
  color: string; note: string; pct?: number; warn?: boolean; err?: boolean;
}) {
  const barPct = pct ?? (total > 0 ? Math.round((value / total) * 100 * 10) / 10 : 0);
  const displayPct = total > 0 ? Math.round((value / total) * 1000) / 10 : 0;
  return (
    <div className="flex items-center gap-3">
      <span className={cn(
        "material-symbols-outlined text-[18px] flex-shrink-0 w-5 text-center",
        err ? "text-rose-400" : warn ? "text-amber-400" : "text-slate-400"
      )}>{icon}</span>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between mb-1">
          <span className="text-[12px] font-semibold text-slate-700">{label}</span>
          <div className="flex items-center gap-2">
            <span className={cn(
              "text-[11px] font-bold mono",
              err ? "text-rose-600" : warn ? "text-amber-600" : "text-slate-900"
            )}>{value.toLocaleString()}</span>
            <span className="text-[10px] text-slate-400 w-10 text-right">{displayPct}%</span>
          </div>
        </div>
        <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden">
          <div
            className={cn("h-full rounded-full transition-all", color)}
            style={{ width: `${barPct}%` }}
          />
        </div>
        <div className="text-[10px] text-slate-400 mt-0.5">{note}</div>
      </div>
    </div>
  );
}

function MetricCard({
  title, value, icon, iconBg, iconColor, sub, badge, subErr
}: {
  title: string; value: number; icon: string;
  iconBg?: string; iconColor?: string;
  sub?: string; badge?: { label: string; ok: boolean };
  subErr?: boolean;
}) {
  return (
    <div className="metric">
      <div className="flex items-start justify-between mb-3">
        <div className={cn("p-2 rounded-xl flex-shrink-0", iconBg ?? "bg-blue-50")}>
          <span className={cn("material-symbols-outlined", iconColor ?? "text-[#497cff]")}>{icon}</span>
        </div>
        {badge && (
          <span className={cn(
            "pill",
            badge.ok ? "pill-ok" : "pill-err"
          )}>
            {badge.label}
          </span>
        )}
      </div>
      <div className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider mb-0.5">{title}</div>
      <div className="text-[1.9rem] font-extrabold tabular text-slate-900 headline">
        {value.toLocaleString()}
      </div>
      {sub && (
        <div className={cn("text-[11px] mt-1", subErr ? "text-rose-500 font-semibold" : "text-slate-400")}>
          {sub}
        </div>
      )}
    </div>
  );
}
