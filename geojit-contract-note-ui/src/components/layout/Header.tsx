"use client";

import { usePathname, useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth";
import { useState, useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { dashboardApi, configApi } from "@/lib/api";
import { cn } from "@/lib/utils";
import { formatDistanceToNow } from "date-fns";
import Link from "next/link";

const titles: Record<string, string> = {
  "/dashboard":    "Dashboard",
  "/process":      "Process File",
  "/jobs":         "Runs & Jobs",
  "/exceptions":   "Exceptions",
  "/clients":      "Client 360",
  "/resend":       "Resend",
  "/audit":        "Audit Log",
  "/templates":    "Email Templates",
  "/certificates": "PFX Certificates",
  "/ses-config":   "Email Configuration",
  "/users":        "Users & Roles",
};

export function Header() {
  const pathname = usePathname();
  const { user }  = useAuthStore();
  const router = useRouter();

  const base    = "/" + (pathname.split("/")[1] ?? "");
  const title   = titles[base] ?? "Geojit Contract Note";

  const [showAlerts, setShowAlerts] = useState(false);
  const [clock, setClock] = useState("");
  const [searchVal, setSearchVal] = useState("");
  const alertsRef = useRef<HTMLDivElement>(null);

  // Clock
  useEffect(() => {
    const tick = () => setClock(new Date().toLocaleTimeString("en-GB", { timeZone: "Asia/Kolkata", hour12: false }));
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, []);

  // Close alert popover on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (alertsRef.current && !alertsRef.current.contains(e.target as Node)) {
        setShowAlerts(false);
      }
    }
    if (showAlerts) document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [showAlerts]);

  // Real pipeline metrics for status badge and notifications
  const { data: metricsRes, refetch: refetchMetrics } = useQuery({
    queryKey: ["header-metrics"],
    queryFn: () => dashboardApi.metrics(
      new Date().toISOString().slice(0, 10),
      new Date().toISOString().slice(0, 10)
    ),
    staleTime: 60_000,
    refetchInterval: 60_000,
  });
  const metrics = metricsRes?.data?.data;

  // Active certificate for expiry warning
  const { data: certRes } = useQuery({
    queryKey: ["header-active-cert"],
    queryFn: () => configApi.activeCert(),
    staleTime: 300_000,
    retry: false,
  });
  const activeCert = certRes?.data?.data;
  const daysToExpiry = activeCert?.validTo
    ? Math.max(0, Math.ceil((new Date(activeCert.validTo).getTime() - Date.now()) / 86400000))
    : null;

  // Build real notifications from live data
  const notifications = [
    ...(metrics?.failedJobs && metrics.failedJobs > 0 ? [{
      id: "failed-jobs",
      icon: "error",
      iconBg: "bg-rose-50",
      iconColor: "text-rose-600",
      title: `${metrics.failedJobs} job${metrics.failedJobs > 1 ? "s" : ""} with failures`,
      desc: "PDF or email errors detected — review in Exceptions",
      page: "/exceptions",
    }] : []),
    ...(metrics?.totalBounced && metrics.totalBounced > 0 ? [{
      id: "bounces",
      icon: "mail_off",
      iconBg: "bg-amber-50",
      iconColor: "text-amber-600",
      title: `${metrics.totalBounced} email bounce${metrics.totalBounced > 1 ? "s" : ""} today`,
      desc: `Bounce rate: ${metrics.bounceRate ?? 0}% — review email configuration`,
      page: "/ses-config",
    }] : []),
    ...(daysToExpiry !== null && daysToExpiry <= 30 ? [{
      id: "cert-expiry",
      icon: "security",
      iconBg: "bg-amber-50",
      iconColor: "text-amber-600",
      title: `PFX certificate expires in ${daysToExpiry} day${daysToExpiry !== 1 ? "s" : ""}`,
      desc: `${activeCert?.fileName} — upload renewed cert before expiry`,
      page: "/certificates",
    }] : []),
    ...(metrics?.activeJobs && metrics.activeJobs > 0 ? [{
      id: "active-jobs",
      icon: "sync",
      iconBg: "bg-blue-50",
      iconColor: "text-blue-600",
      title: `${metrics.activeJobs} job${metrics.activeJobs > 1 ? "s" : ""} currently processing`,
      desc: "Pipeline is active — emails being sent",
      page: "/jobs",
    }] : []),
    // Show recent activity
    ...(metrics?.recentActivity?.slice(0, 2).map((a: import("@/types").RecentActivity, i: number) => ({
      id: `activity-${i}`,
      icon: "history",
      iconBg: "bg-slate-50",
      iconColor: "text-slate-600",
      title: a.description.replace(/([A-Z0-9]+)\/\1/g, "$1"),
      desc: `${a.eventType} · ${formatDistanceToNow(new Date(a.eventTimestamp.endsWith("Z") ? a.eventTimestamp : a.eventTimestamp + "Z"), { addSuffix: true })}`,
      page: a.jobId ? `/jobs/${a.jobId}` : "/dashboard",
    })) ?? []),
  ].slice(0, 5); // max 5 notifications

  const unreadCount = notifications.length;

  const handleSearch = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && searchVal.trim()) {
      router.push(`/clients/${searchVal.trim()}`);
      setSearchVal("");
    }
  };

  const initials = user?.name?.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2) ?? "?";
  const firstName = user?.name?.split(" ")[0] ?? "";

  return (
    <header className="h-14 flex-shrink-0 bg-white/90 backdrop-blur-xl border-b border-slate-200/60 flex items-center justify-between px-5 z-40">
      <div className="flex items-center gap-3">
        <div className="text-base font-bold text-slate-800 headline">{title}</div>
      </div>

      <div className="flex items-center gap-1.5">
        {/* Global search → navigates to client profile */}
        <div className="relative hidden lg:block">
          <span className="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400 text-base">search</span>
          <input
            value={searchVal}
            onChange={e => setSearchVal(e.target.value)}
            onKeyDown={handleSearch}
            className="pl-8 pr-3 py-1.5 bg-slate-100 border-none rounded-full text-sm w-52 focus:w-64 transition-all focus:outline-none"
            placeholder="Client code or PAN…"
            title="Press Enter to open client profile"
          />
        </div>

        {/* Notifications bell */}
        <div className="relative" ref={alertsRef}>
          <button
            onClick={() => setShowAlerts(v => !v)}
            className="relative p-2 text-slate-500 hover:text-[#00174b] hover:bg-slate-100 rounded-lg transition-colors"
          >
            <span className="material-symbols-outlined text-xl">notifications</span>
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full border-2 border-white" />
            )}
          </button>

          {showAlerts && (
            <div className="absolute right-0 top-full mt-2 w-[400px] max-w-[94vw] max-h-[calc(100vh-5rem)] bg-white z-[91] overflow-y-auto shadow-[0_12px_44px_rgba(0,0,0,.16)] rounded-[1rem] border border-slate-200/60">
              <div className="px-4 py-3 border-b border-slate-100 flex items-center justify-between">
                <div className="font-bold text-sm headline">Notifications</div>
                <span className="text-[10px] text-slate-400">{unreadCount === 0 ? "All clear" : `${unreadCount} active`}</span>
              </div>
              <div className="p-2">
                {notifications.length === 0 ? (
                  <div className="p-6 text-center text-slate-400 text-sm">
                    <span className="material-symbols-outlined text-3xl block mb-2 text-slate-300">check_circle</span>
                    No issues — pipeline is healthy
                  </div>
                ) : notifications.map(n => (
                  <Link
                    key={n.id}
                    href={n.page}
                    onClick={() => setShowAlerts(false)}
                    className="p-3 rounded-lg hover:bg-slate-50 cursor-pointer flex gap-3"
                  >
                    <div className={`p-1.5 rounded-lg ${n.iconBg} h-fit`}>
                      <span className={`material-symbols-outlined ${n.iconColor} text-base`}>{n.icon}</span>
                    </div>
                    <div className="flex-1">
                      <div className="text-xs font-semibold text-slate-800">{n.title}</div>
                      <div className="text-[11px] text-slate-500 mt-0.5">{n.desc}</div>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Refresh */}
        <button
          onClick={() => { refetchMetrics(); }}
          className="p-2 text-slate-500 hover:text-[#00174b] hover:bg-slate-100 rounded-lg transition-colors"
          title="Refresh metrics"
        >
          <span className="material-symbols-outlined text-xl">refresh</span>
        </button>

        <div className="text-[11px] text-slate-500 mono tabular px-2">{clock}</div>
        <div className="h-7 w-px bg-slate-200 mx-1" />
        <div className="flex items-center gap-2 pr-2">
          <div className="w-7 h-7 rounded-full bg-[#00174b] text-blue-200 font-bold text-[10px] flex items-center justify-center">
            {initials}
          </div>
          <span className="text-xs font-semibold text-slate-700 hidden md:inline">{firstName}</span>
        </div>
      </div>
    </header>
  );
}
