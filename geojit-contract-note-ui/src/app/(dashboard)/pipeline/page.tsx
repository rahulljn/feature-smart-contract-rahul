"use client";

import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { jobsApi } from "@/lib/api";
import type { Job, PipelineStats } from "@/types";
import Link from "next/link";
import { Loader2 } from "lucide-react";

const RUNNING = ["PROCESSING", "SPLITTING", "EMAILING", "VALIDATING"];

function fmtCount(n: number): string {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, "") + "M";
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, "") + "k";
  return n.toLocaleString();
}

function fmtSeconds(sec: number): string {
  if (sec <= 0) return "—";
  const m = Math.floor(sec / 60);
  const s = Math.round(sec % 60);
  return m > 0 ? `${m}m ${s}s` : `${s}s`;
}

type NodeState = "idle" | "active" | "done";

function nodeColor(base: string, state: NodeState): string {
  return state === "idle" ? "#cbd5e1" : base;
}

function jobStatusColor(status: string): string {
  if (status === "COMPLETED") return "#10b981";
  if (RUNNING.includes(status)) return "#497cff";
  if (status === "FAILED") return "#ef4444";
  if (status === "PARTIAL") return "#f59e0b";
  return "#94a3b8";
}

const NODES = [
  { key: "upload",    label: "Upload",    icon: "upload_file",     x: 80,  color: "#64748b" },
  { key: "split",     label: "Split",     icon: "call_split",      x: 245, color: "#a855f7" },
  { key: "pdf",       label: "PDF gen",   icon: "picture_as_pdf",  x: 410, color: "#8b5cf6" },
  { key: "email",     label: "Email",     icon: "send",            x: 575, color: "#003ea8" },
  { key: "delivered", label: "Delivered", icon: "mark_email_read", x: 740, color: "#10b981" },
] as const;

const CY = 140;
const R = 38;

function getStates(stats: PipelineStats): Record<string, NodeState> {
  const st = stats.status;
  return {
    upload:    stats.uploadCount  > 0 ? "done"   : "idle",
    split:     stats.splitCount   > 0 ? (st === "SPLITTING"  ? "active" : "done") : "idle",
    pdf:       st === "PROCESSING" ? "active"     : stats.pdfCount   > 0 ? "done" : "idle",
    email:     st === "EMAILING"   ? "active"     : stats.emailCount > 0 ? "done" : "idle",
    delivered: stats.deliveredCount > 0 ? "done" : RUNNING.includes(st) ? "active" : "idle",
  };
}

// ─── SVG Pipeline Diagram ─────────────────────────────────────────────────────

function PipelineSVG({ stats }: { stats: PipelineStats }) {
  const states = getStates(stats);

  const counts: Record<string, number> = {
    upload:    stats.uploadCount,
    split:     stats.splitCount,
    pdf:       stats.pdfCount,
    email:     stats.emailCount,
    delivered: stats.deliveredCount,
  };

  const rates: Record<string, number> = {
    pdf:       stats.pdfRate,
    email:     stats.emailRate,
    delivered: stats.deliveredRate,
  };

  // originalIdx preserved so path direction stays consistent when some branches are filtered
  const ALL_SIDE_OUTS = [
    { from: "pdf",   label: "Failed",  icon: "error",         count: stats.pdfFailed,    color: "#ef4444", dy: -75, oi: 0 },
    { from: "email", label: "Bounced", icon: "unsubscribe",   count: stats.emailBounced,  color: "#f59e0b", dy:  75, oi: 1 },
    { from: "email", label: "Pending", icon: "hourglass_top", count: stats.emailPending,  color: "#94a3b8", dy: -75, oi: 2 },
  ];
  const sideOuts = ALL_SIDE_OUTS.filter(s => s.count > 0);

  return (
    <div className="w-full" style={{ paddingBottom: 8 }}>
      <style>{`
        @keyframes flowDash { to { stroke-dashoffset: -40; } }
        @keyframes pulse { 0%,100% { opacity:.4; transform:scale(1); } 50% { opacity:1; transform:scale(1.15); } }
        .flow-line { stroke-dasharray: 6 6; animation: flowDash 1.6s linear infinite; }
      `}</style>

      <svg viewBox="0 0 860 280" width="100%" height={280} style={{ overflow: "visible", display: "block" }}>

        {/* Trunk lines */}
        {NODES.slice(0, -1).map((node, i) => {
          const next = NODES[i + 1];
          const x1 = node.x + R + 2;
          const x2 = next.x - R - 2;
          const c = nodeColor(next.color, states[next.key]);
          return (
            <g key={node.key}>
              <line x1={x1} y1={CY} x2={x2} y2={CY} stroke="#e2e8f0" strokeWidth={14} strokeLinecap="round" />
              <line x1={x1} y1={CY} x2={x2} y2={CY} stroke={c} strokeWidth={3}
                strokeLinecap="round" className="flow-line" opacity={0.9} />
            </g>
          );
        })}

        {/* Side-branch curved paths */}
        {sideOuts.map((s) => {
          const src = NODES.find(n => n.key === s.from)!;
          const y2 = CY + s.dy;
          const x2 = src.x + 30 + (s.oi % 2 === 0 ? -30 : 30);
          const qy = y2 + (s.dy > 0 ? -18 : 18);
          return (
            <path key={s.label + s.from}
              d={`M ${src.x + 30} ${CY} Q ${src.x + 30} ${qy} ${x2 + 40} ${y2}`}
              stroke={s.color} strokeWidth={2} fill="none" strokeDasharray="4 4" opacity={0.6}
            />
          );
        })}

        {/* Stage nodes */}
        {NODES.map((node) => {
          const state = states[node.key];
          const color = nodeColor(node.color, state);
          const count = counts[node.key] ?? 0;
          const rate = rates[node.key] ?? 0;

          return (
            <g key={node.key} transform={`translate(${node.x}, ${CY})`}>
              <circle r={R} fill="#fff" stroke={color} strokeWidth={2.5} />
              <circle r={R} fill={color} opacity={0.08} />
              <foreignObject x={-14} y={-14} width={28} height={28}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "center", color, width: "100%", height: "100%" }}>
                  <span className="material-symbols-outlined" style={{ fontSize: 22 }}>{node.icon}</span>
                </div>
              </foreignObject>
              {/* Label */}
              <text x={0} y={R + 18} textAnchor="middle" fontSize={11} fontWeight="700" fill="#475569">
                {node.label}
              </text>
              {/* Count */}
              <text x={0} y={R + 36} textAnchor="middle" fontSize={17} fontWeight="800"
                fill={state === "idle" ? "#94a3b8" : "#0f172a"} fontFamily="ui-monospace,monospace">
                {count > 0 ? fmtCount(count) : "—"}
              </text>
              {/* Rate badge */}
              {rate > 0 && (
                <g>
                  <rect x={-30} y={R + 40} width={60} height={16} rx={8} fill={color} opacity={0.12} />
                  <text x={0} y={R + 51} textAnchor="middle" fontSize={9.5} fontWeight="700"
                    fill={color} fontFamily="ui-monospace,monospace">
                    ↑ {fmtCount(rate)}/min
                  </text>
                </g>
              )}
            </g>
          );
        })}

        {/* Side-branch label boxes */}
        {sideOuts.map((s) => {
          const src = NODES.find(n => n.key === s.from)!;
          const x = src.x + 80;
          const y = CY + s.dy - 18;
          return (
            <foreignObject key={s.label + s.from + "label"} x={x - 52} y={y - 6} width={130} height={36}>
              <div style={{
                display: "flex", alignItems: "center", gap: 5,
                background: "#fff",
                border: `1px solid ${s.color}44`,
                borderRadius: 9, padding: "3px 8px",
                boxShadow: "0 1px 2px rgba(0,0,0,.05)",
                fontSize: 11, whiteSpace: "nowrap",
              }}>
                <span className="material-symbols-outlined"
                  style={{ fontSize: 13, color: s.color, flexShrink: 0 }}>{s.icon}</span>
                <span style={{ color: "#334155", fontWeight: 600, flex: 1 }}>{s.label}</span>
                <span style={{ fontWeight: 800, color: "#0f172a", fontVariantNumeric: "tabular-nums" }}>
                  {s.count.toLocaleString()}
                </span>
              </div>
            </foreignObject>
          );
        })}
      </svg>

      {/* Bottom stats pill */}
      <div className="flex justify-center mt-3">
        <div className="inline-flex items-center gap-3 text-[11.5px] text-slate-500 bg-white border border-slate-200 rounded-full px-4 py-1.5 shadow-sm flex-wrap justify-center">
          <span>End-to-end median: <b className="text-slate-900">{fmtSeconds(stats.medianSeconds)}</b></span>
          <span className="text-slate-300">·</span>
          <span>p95: <b className="text-slate-900">{fmtSeconds(stats.p95Seconds)}</b></span>
          <span className="text-slate-300">·</span>
          <span>
            Error rate:{" "}
            <b className={stats.errorRate > 1 ? "text-rose-600" : "text-emerald-600"}>
              {stats.errorRate.toFixed(2)}%
            </b>
          </span>
          {stats.fileName && (
            <>
              <span className="text-slate-300">·</span>
              <span className="text-slate-400 truncate max-w-[180px] text-[10.5px]" title={stats.fileName}>
                {stats.fileName}
              </span>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── No active pipeline ───────────────────────────────────────────────────────

function NoActivePipeline() {
  return (
    <div className="flex flex-col items-center justify-center py-12 space-y-4">
      <svg viewBox="0 0 860 100" width="100%" height={100} style={{ opacity: 0.25 }}>
        {NODES.slice(0, -1).map((node, i) => {
          const next = NODES[i + 1];
          return (
            <line key={node.key}
              x1={node.x + 30} y1={50} x2={next.x - 30} y2={50}
              stroke="#cbd5e1" strokeWidth={8} strokeLinecap="round" />
          );
        })}
        {NODES.map(node => (
          <g key={node.key} transform={`translate(${node.x}, 50)`}>
            <circle r={28} fill="#fff" stroke="#cbd5e1" strokeWidth={2} />
            <foreignObject x={-10} y={-10} width={20} height={20}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "center", color: "#cbd5e1" }}>
                <span className="material-symbols-outlined" style={{ fontSize: 17 }}>{node.icon}</span>
              </div>
            </foreignObject>
          </g>
        ))}
      </svg>
      <div className="text-center">
        <div className="text-base font-bold text-slate-700">No pipeline running</div>
        <div className="text-sm text-slate-400 mt-1">Start a new run to see the live flow here</div>
      </div>
      <Link href="/process"
        className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5">
        <span className="material-symbols-outlined text-base">add</span>Process file
      </Link>
    </div>
  );
}

// ─── Today's timeline ────────────────────────────────────────────────────────

function Timeline({ jobs }: { jobs: Job[] }) {
  const now = new Date();
  const dayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
  const totalMs = 24 * 60 * 60 * 1000;
  const VW = 700;
  const nowX = Math.min(VW - 1, ((now.getTime() - dayStart.getTime()) / totalMs) * VW);

  const toX = (dateStr: string) => {
    const ms = new Date(dateStr).getTime() - dayStart.getTime();
    return Math.max(2, Math.min(VW - 2, (ms / totalMs) * VW));
  };

  const lanes = jobs.slice(0, 5);

  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-4">
      <div className="flex items-center mb-3">
        <span className="text-[13px] font-bold text-slate-800">Today&apos;s timeline</span>
        <div className="flex-1" />
        <div className="flex flex-wrap gap-3 text-[10.5px] text-slate-400">
          {[
            ["#10b981", "Completed"],
            ["#497cff", "Running"],
            ["#f59e0b", "Partial"],
            ["#ef4444", "Failed"],
          ].map(([c, l]) => (
            <span key={l} className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: c }} />
              {l}
            </span>
          ))}
        </div>
      </div>
      <svg viewBox={`0 0 ${VW} 130`} width="100%" height={130}>
        {/* Axis */}
        <line x1={0} y1={100} x2={VW} y2={100} stroke="#e2e8f0" strokeWidth={1} />
        {/* Hour marks every 2 hours */}
        {Array.from({ length: 13 }, (_, i) => {
          const x = (i / 12) * VW;
          const hr = i * 2;
          return (
            <g key={i}>
              <line x1={x} y1={97} x2={x} y2={104} stroke="#cbd5e1" strokeWidth={1} />
              <text x={x} y={120} textAnchor="middle" fontSize={9} fill="#94a3b8"
                fontFamily="ui-monospace,monospace">
                {`${String(hr).padStart(2, "0")}:00`}
              </text>
            </g>
          );
        })}
        {/* Job event lollipops */}
        {lanes.map((job, idx) => {
          const y = 14 + idx * 17;
          const sx = toX(job.uploadedAt);
          const color = jobStatusColor(job.status);
          const isRunning = RUNNING.includes(job.status);
          return (
            <g key={job.jobId}>
              <title>{`${job.jobId.slice(-6).toUpperCase()} · ${job.segmentType ?? ""} · ${job.status}`}</title>
              <line x1={sx} y1={y} x2={sx} y2={100} stroke={color} strokeWidth={1.5} opacity={0.35} />
              <circle cx={sx} cy={y} r={isRunning ? 5.5 : 4} fill={color} stroke="#fff" strokeWidth={1.5} />
              <text x={sx + (sx > VW - 60 ? -8 : 9)} y={y + 4}
                textAnchor={sx > VW - 60 ? "end" : "start"}
                fontSize={8.5} fill={color} fontWeight="600" opacity={0.85}>
                {job.jobId.slice(-5).toUpperCase()}
              </text>
            </g>
          );
        })}
        {/* NOW indicator */}
        {nowX > 14 && nowX < VW - 14 && (
          <>
            <line x1={nowX} y1={0} x2={nowX} y2={100} stroke="#497cff" strokeWidth={1.5} strokeDasharray="3 3" />
            <rect x={nowX - 14} y={0} width={28} height={13} rx={3} fill="#497cff" />
            <text x={nowX} y={9.5} textAnchor="middle" fontSize={8.5} fontWeight={700} fill="#fff">NOW</text>
          </>
        )}
      </svg>
      {lanes.length === 0 && (
        <div className="flex justify-center py-3 text-[12px] text-slate-400">No jobs today</div>
      )}
    </div>
  );
}

// ─── Quick facts ──────────────────────────────────────────────────────────────

function QuickFacts({ stats, todayJobs }: { stats: PipelineStats | null; todayJobs: Job[] }) {
  const activeCount = todayJobs.filter(j => RUNNING.includes(j.status)).length;

  const rows = stats
    ? [
        { label: "End-to-end median",    value: fmtSeconds(stats.medianSeconds),                               color: "#10b981" },
        { label: "Emails / hour",        value: fmtCount(stats.emailRate * 60),                                color: "#497cff" },
        { label: "Active jobs",          value: String(activeCount),                                           color: "#003ea8" },
        { label: "Queue depth",          value: (stats.emailPending + stats.pdfPending).toLocaleString(),      color: "#f59e0b" },
        { label: "SES quota used today", value: `${fmtCount(stats.emailCount)} / 100k`,                        color: "#64748b" },
      ]
    : [
        { label: "Jobs today",  value: String(todayJobs.length),                                               color: "#003ea8" },
        { label: "Active now",  value: String(activeCount),                                                    color: "#497cff" },
        { label: "Completed",   value: String(todayJobs.filter(j => j.status === "COMPLETED").length),         color: "#10b981" },
        { label: "Partial",     value: String(todayJobs.filter(j => j.status === "PARTIAL").length),           color: "#f59e0b" },
        { label: "Failed",      value: String(todayJobs.filter(j => j.status === "FAILED").length),            color: "#ef4444" },
      ];

  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-4">
      <div className="text-[13px] font-bold text-slate-800 mb-1">Quick facts</div>
      {rows.map((r, idx) => (
        <div key={r.label}
          className={`flex items-center py-2.5 ${idx > 0 ? "border-t border-slate-100" : ""}`}>
          <span className="w-1.5 h-1.5 rounded-full flex-shrink-0 mr-2.5" style={{ background: r.color }} />
          <span className="text-[12px] text-slate-600 flex-1">{r.label}</span>
          <span className="text-[13px] font-bold text-slate-900 tabular-nums">{r.value}</span>
        </div>
      ))}
    </div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function PipelinePage() {
  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  const today = new Date().toISOString().slice(0, 10);

  const { data: jobsRes, isLoading: loadingJobs } = useQuery({
    queryKey: ["pipeline-jobs-running"],
    queryFn: () => jobsApi.list(0, 10),
    refetchInterval: 10_000,
  });

  const { data: todayJobsRes } = useQuery({
    queryKey: ["pipeline-today-jobs", today],
    queryFn: () => jobsApi.list(0, 20, undefined, today, today),
    refetchInterval: 30_000,
  });

  useEffect(() => {
    const jobs: Job[] = jobsRes?.data?.data?.content ?? [];
    const running = jobs.find(j => RUNNING.includes(j.status));
    setActiveJobId(running?.jobId ?? null);
  }, [jobsRes]);

  const isActive = activeJobId !== null;

  const { data: statsRes, isLoading: loadingStats } = useQuery({
    queryKey: ["pipeline-stats", activeJobId],
    queryFn: () => jobsApi.pipelineStats(activeJobId!),
    enabled: !!activeJobId,
    refetchInterval: isActive ? 5_000 : false,
  });

  const stats: PipelineStats | null = statsRes?.data?.data ?? null;
  const todayJobs: Job[] = todayJobsRes?.data?.data?.content ?? [];
  const loading = loadingJobs || (isActive && loadingStats);

  return (
    <div className="p-6 space-y-4 max-w-[1400px] mx-auto w-full fade-up">

      {/* Header */}
      <div className="flex items-end justify-between gap-4">
        <div>
          <div className="text-[11px] font-bold text-slate-400 uppercase tracking-widest mb-1">Live Pipeline</div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Contract Note Flow</h2>
          <div className="flex items-center gap-2 mt-1.5">
            {isActive ? (
              <>
                <span style={{ width: 7, height: 7, borderRadius: "50%", background: "#10b981", animation: "pulseDot 1.4s ease-in-out infinite", display: "inline-block", flexShrink: 0 }} />
                <span className="text-sm text-slate-500">
                  Streaming &middot;{" "}
                  <span className="font-semibold text-slate-700">{(stats?.uploadCount ?? 0).toLocaleString()}</span>{" "}
                  records being processed right now
                </span>
              </>
            ) : (
              <>
                <span className="w-2 h-2 rounded-full bg-slate-300 flex-shrink-0" />
                <span className="text-sm text-slate-400">No active pipeline</span>
              </>
            )}
          </div>
        </div>
        <div className="flex gap-2 flex-shrink-0">
          {isActive && (
            <Link href={`/jobs/${activeJobId}`}
              className="px-3.5 py-2 bg-white text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 border border-slate-200 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-base">history</span>Event log
            </Link>
          )}
          <Link href="/process"
            className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5">
            <span className="material-symbols-outlined text-base">add</span>Process file
          </Link>
        </div>
      </div>

      {/* Pipeline card */}
      <div className="card p-6 overflow-x-auto">
        {loading ? (
          <div className="flex justify-center py-16">
            <Loader2 className="animate-spin text-[#00174b]" />
          </div>
        ) : isActive && stats ? (
          <PipelineSVG stats={stats} />
        ) : (
          <NoActivePipeline />
        )}
      </div>

      {/* Bottom: timeline + quick facts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2">
          <Timeline jobs={todayJobs} />
        </div>
        <QuickFacts stats={isActive ? stats : null} todayJobs={todayJobs} />
      </div>
    </div>
  );
}
