"use client";

import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth";
import { useState, useEffect, useRef, useMemo } from "react";
import { alertsApi, authApi, dashboardApi, configApi } from "@/lib/api";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { toast } from "sonner";

const USE_MOCK = true;

export function Header() {
  const { user, clearUser } = useAuthStore();
  const router = useRouter();

  const [showAlerts, setShowAlerts] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [searchVal, setSearchVal] = useState("");
  const [unreadCount, setUnreadCount] = useState(0);
  const alertsRef = useRef<HTMLDivElement>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    async function fetchUnread() {
      try {
        if (USE_MOCK) {
          if (!cancelled) setUnreadCount(3);
          return;
        }
        const res = await alertsApi.getUnreadCount();
        const count = res?.data?.data ?? res?.data ?? 0;
        if (!cancelled) setUnreadCount(typeof count === "number" ? count : 0);
      } catch { /* ignore */ }
    }
    fetchUnread();
    const id = setInterval(fetchUnread, 60_000);
    return () => { cancelled = true; clearInterval(id); };
  }, []);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (alertsRef.current && !alertsRef.current.contains(e.target as Node)) {
        setShowAlerts(false);
      }
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setShowUserMenu(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: metricsRes } = useQuery<any>({
    queryKey: ["header-metrics"],
    queryFn: () => {
      if (USE_MOCK) return Promise.resolve({ data: { data: { failedJobs: 1, totalBounced: 640, activeJobs: 1 } } });
      return dashboardApi.metrics(
        new Date().toISOString().slice(0, 10),
        new Date().toISOString().slice(0, 10),
      );
    },
    staleTime: 60_000,
    refetchInterval: 60_000,
  });
  const metrics = metricsRes?.data?.data;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: certRes } = useQuery<any>({
    queryKey: ["header-active-cert"],
    queryFn: () => {
      if (USE_MOCK) return Promise.resolve({ data: { data: { fileName: "GEOJIT-PROD-2025.pfx", validTo: new Date(Date.now() + 23 * 86400000).toISOString() } } });
      return configApi.activeCert();
    },
    staleTime: 300_000,
    retry: false,
  });
  const activeCert = certRes?.data?.data;
  const daysToExpiry = useMemo(() => {
    if (!activeCert?.validTo) return null;
    // eslint-disable-next-line react-hooks/purity
    return Math.max(0, Math.ceil((new Date(activeCert.validTo).getTime() - Date.now()) / 86400000));
  }, [activeCert?.validTo]);

  const notifications = [
    ...(metrics?.failedJobs && metrics.failedJobs > 0 ? [{
      id: "failed-jobs", icon: "error", iconBg: "bg-rose-50", iconColor: "text-rose-600",
      title: `${metrics.failedJobs} job${metrics.failedJobs > 1 ? "s" : ""} with failures`,
      desc: "PDF or email errors detected — review in Exceptions", page: "/exceptions",
    }] : []),
    ...(metrics?.totalBounced && metrics.totalBounced > 0 ? [{
      id: "bounces", icon: "mail_off", iconBg: "bg-amber-50", iconColor: "text-amber-600",
      title: `${metrics.totalBounced} bounced email${metrics.totalBounced > 1 ? "s" : ""} today`,
      desc: "Use Bulk Resend to retry delivery", page: "/resend",
    }] : []),
    ...(daysToExpiry !== null && daysToExpiry <= 30 ? [{
      id: "cert-expiry", icon: "security", iconBg: "bg-amber-50", iconColor: "text-amber-600",
      title: `PFX certificate expires in ${daysToExpiry} day${daysToExpiry !== 1 ? "s" : ""}`,
      desc: `${activeCert?.fileName} — upload renewed cert before expiry`, page: "/certificates",
    }] : []),
    ...(metrics?.activeJobs && metrics.activeJobs > 0 ? [{
      id: "active-jobs", icon: "sync", iconBg: "bg-blue-50", iconColor: "text-blue-600",
      title: `${metrics.activeJobs} job${metrics.activeJobs > 1 ? "s" : ""} currently processing`,
      desc: "Pipeline is active — emails being sent", page: "/jobs",
    }] : []),
  ].slice(0, 5);

  const handleSearch = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && searchVal.trim()) {
      router.push(`/clients/${searchVal.trim()}`);
      setSearchVal("");
    }
  };

  const handleLogout = async () => {
    try { await authApi.logout(); } catch { /* ignore */ }
    clearUser();
    document.cookie = "auth-token=; Max-Age=0; path=/";
    router.push("/login");
    toast.success("Logged out successfully");
    setShowUserMenu(false);
  };

  const initials = user?.name?.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2) ?? "?";
  const firstName = user?.name?.split(" ")[0] ?? "";

  return (
    <header className="h-12 flex-shrink-0 bg-white border-b border-gray-200 flex items-center justify-between px-4 z-40">
      {/* LEFT: Search */}
      <div className="relative">
        <span className="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400 text-[18px]">
          search
        </span>
        <input
          value={searchVal}
          onChange={(e) => setSearchVal(e.target.value)}
          onKeyDown={handleSearch}
          placeholder="Search jobs, templates…"
          className="w-64 pl-8 pr-3 py-1.5 bg-gray-100 rounded-lg text-sm text-gray-700 placeholder-gray-400 outline-none focus:ring-2 focus:ring-[#00174b]/20 transition-all"
        />
      </div>

      {/* RIGHT */}
      <div className="flex items-center gap-3">
        {/* Bell */}
        <div className="relative" ref={alertsRef}>
          <button
            onClick={() => setShowAlerts((v) => !v)}
            className="relative p-1.5 text-gray-500 hover:text-[#00174b] hover:bg-gray-100 rounded-lg transition-colors"
          >
            <span className="material-symbols-outlined text-[20px]">notifications</span>
            {unreadCount > 0 && (
              <span className="absolute top-1 right-1 min-w-[16px] h-4 px-1 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center">
                {unreadCount > 9 ? "9+" : unreadCount}
              </span>
            )}
          </button>

          {showAlerts && (
            <div className="absolute right-0 top-full mt-2 w-[380px] max-w-[94vw] max-h-[calc(100vh-5rem)] bg-white z-50 overflow-y-auto shadow-xl rounded-xl border border-gray-200">
              <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
                <div className="font-bold text-sm">Notifications</div>
                <Link
                  href="/alerts"
                  onClick={() => setShowAlerts(false)}
                  className="text-[11px] text-[#497cff] font-semibold hover:underline"
                >
                  View all
                </Link>
              </div>
              <div className="p-2">
                {notifications.length === 0 ? (
                  <div className="p-6 text-center text-gray-400 text-sm">
                    <span className="material-symbols-outlined text-3xl block mb-2 text-gray-300">
                      check_circle
                    </span>
                    No issues — pipeline is healthy
                  </div>
                ) : (
                  notifications.map((n) => (
                    <Link
                      key={n.id}
                      href={n.page}
                      onClick={() => setShowAlerts(false)}
                      className="p-3 rounded-lg hover:bg-gray-50 cursor-pointer flex gap-3"
                    >
                      <div className={`p-1.5 rounded-lg ${n.iconBg} h-fit`}>
                        <span className={`material-symbols-outlined ${n.iconColor} text-base`}>
                          {n.icon}
                        </span>
                      </div>
                      <div className="flex-1">
                        <div className="text-xs font-semibold text-gray-800">{n.title}</div>
                        <div className="text-[11px] text-gray-500 mt-0.5">{n.desc}</div>
                      </div>
                    </Link>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* User avatar + dropdown */}
        <div className="relative" ref={userMenuRef}>
          <button
            onClick={() => setShowUserMenu((v) => !v)}
            className="flex items-center gap-2 px-2 py-1 rounded-lg hover:bg-gray-100 transition-colors"
          >
            <div className="w-8 h-8 rounded-full bg-[#00174b] text-white font-bold text-[11px] flex items-center justify-center flex-shrink-0">
              {initials}
            </div>
            <div className="hidden md:flex flex-col items-start leading-tight">
              <span className="text-sm font-medium text-gray-900 leading-none">{firstName}</span>
              <span className="text-xs text-gray-500 leading-none mt-0.5 truncate max-w-[120px]">{user?.email}</span>
            </div>
            <span className="material-symbols-outlined text-gray-400 text-[16px]">expand_more</span>
          </button>

          {showUserMenu && (
            <div className="absolute right-0 top-full mt-1.5 w-52 bg-white shadow-xl rounded-xl border border-gray-200 z-50 overflow-hidden">
              <div className="px-4 py-3 border-b border-gray-100">
                <div className="text-sm font-semibold text-gray-900">{user?.name}</div>
                <div className="text-xs text-gray-500 mt-0.5">{user?.email}</div>
              </div>
              <div className="p-1">
                <button
                  onClick={() => { setShowUserMenu(false); router.push("/settings"); }}
                  className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 rounded-lg transition-colors text-left"
                >
                  <span className="material-symbols-outlined text-gray-400 text-[18px]">person</span>
                  Profile
                </button>
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-red-600 hover:bg-red-50 rounded-lg transition-colors text-left"
                >
                  <span className="material-symbols-outlined text-red-400 text-[18px]">logout</span>
                  Logout
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
