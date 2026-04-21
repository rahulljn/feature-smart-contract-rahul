"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";
import { useState } from "react";
import { authApi } from "@/lib/api";
import { toast } from "sonner";
import { useRouter } from "next/navigation";

interface NavItem {
  href: string;
  label: string;
  icon: string;
  badgeQuery?: boolean;
  adminOnly?: boolean;
}

const navSections: { name: string; items: NavItem[] }[] = [
  {
    name: "Operate",
    items: [
      { href: "/dashboard",  label: "Dashboard",    icon: "grid_view" },
      { href: "/process",    label: "Process File", icon: "upload_file" },
      { href: "/jobs",       label: "Runs & Jobs",  icon: "hub" },
      { href: "/clients",    label: "Client 360",   icon: "groups" },
      { href: "/exceptions", label: "Exceptions",   icon: "error" },
      { href: "/resend",     label: "Resend",       icon: "forward_to_inbox" },
    ],
  },
  {
    name: "Configure",
    items: [
      { href: "/templates",    label: "Templates",        icon: "mail" },
      { href: "/ses-config",   label: "Email Config",     icon: "settings" },
      { href: "/certificates", label: "Certificates",     icon: "verified" },
      { href: "/audit",        label: "Audit Log",        icon: "history" },
      { href: "/users",        label: "Users & Roles",    icon: "manage_accounts", adminOnly: true },
    ],
  },
];

export function Sidebar() {
  const pathname   = usePathname();
  const { user, clearUser } = useAuthStore();
  const router     = useRouter();
  const [collapsed, setCollapsed] = useState(false);
  const isAdmin    = user?.role === "ADMIN";

  const handleLogout = async () => {
    try { await authApi.logout(); } catch { /* ignore */ }
    clearUser();
    document.cookie = "auth-token=; Max-Age=0; path=/";
    router.push("/login");
    toast.success("Logged out successfully");
  };

  const initials = user?.name?.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2) ?? "?";
  const roleLabel = user?.role === "ADMIN" ? "Admin" : user?.role === "OPS_MANAGER" ? "Ops Manager" : "Viewer";

  return (
    <aside
      className={cn(
        "flex flex-col h-full bg-[#00174b] text-white transition-all duration-300",
        collapsed ? "w-16" : "w-64"
      )}
    >
      {/* Brand */}
      <div className="flex items-center justify-between px-4 py-4 mb-1">
        <div className={cn("flex items-center gap-2.5 min-w-0 flex-1", collapsed && "justify-center")}>
          <div className="w-9 h-9 rounded-lg bg-white/10 backdrop-blur flex items-center justify-center border border-white/15 flex-shrink-0">
            <span className="material-symbols-outlined text-blue-300 text-xl">account_balance</span>
          </div>
          {!collapsed && (
            <div className="min-w-0">
              <div className="text-white font-bold text-[13px] leading-tight headline">Contract Note</div>
              <div className="text-blue-300/70 text-[9px] uppercase tracking-widest font-semibold mt-0.5">Geojit Ops Console</div>
            </div>
          )}
        </div>
        <button
          onClick={() => setCollapsed(c => !c)}
          className="text-slate-400 hover:text-white p-1.5 rounded-lg transition-colors flex-shrink-0"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          <span className="material-symbols-outlined text-xl">menu</span>
        </button>
      </div>

      {/* Nav Sections */}
      <nav className="flex-1 overflow-y-auto pb-4 space-y-0.5">
        {navSections.map(section => {
          const visibleItems = section.items.filter(item => !item.adminOnly || isAdmin);
          if (visibleItems.length === 0) return null;

          return (
            <div key={section.name}>
              {!collapsed && (
                <div className="px-4 pt-1 pb-0.5 text-[9px] font-bold text-slate-600 uppercase tracking-widest">
                  {section.name}
                </div>
              )}
              <div className="space-y-0">
                {visibleItems.map(({ href, label, icon, badgeQuery }) => {
                  const active = pathname === href || pathname.startsWith(href + "/");
                  const badge: string | undefined = undefined; // badgeQuery reserved for future use
                  return (
                    <Link
                      key={href}
                      href={href}
                      title={collapsed ? label : undefined}
                      className={cn(
                        "flex items-center gap-[0.7rem] py-[0.55rem] text-[0.8125rem] font-medium transition-all duration-150",
                        collapsed ? "px-0 justify-center mx-[0.35rem] rounded-[0.625rem]" : "px-[0.85rem] mx-[0.6rem] rounded-[0.625rem] mb-[0.15rem]",
                        active
                          ? "bg-[rgba(73,124,255,0.18)] text-white font-semibold"
                          : "text-[#94a3b8] hover:text-white hover:bg-white/[0.06]"
                      )}
                    >
                      <span className={cn(
                        "material-symbols-outlined text-xl flex-shrink-0",
                        active ? "text-[#93c5fd]" : ""
                      )}>
                        {icon}
                      </span>
                      {!collapsed && (
                        <>
                          <span className="truncate">{label}</span>
                          {badge && (
                            <span className="ml-auto text-[9px] font-bold bg-rose-500/20 text-rose-300 px-1.5 py-0.5 rounded">
                              {badge}
                            </span>
                          )}
                        </>
                      )}
                    </Link>
                  );
                })}
              </div>
            </div>
          );
        })}
      </nav>

      {/* User Card */}
      <div className="border-t border-white/10 p-3">
        <div className={cn(
          "flex items-center gap-2.5 p-2 rounded-xl bg-white/5",
          collapsed && "justify-center"
        )}>
          <div className="w-9 h-9 rounded-full bg-blue-500/25 flex items-center justify-center text-blue-200 font-bold text-[11px] flex-shrink-0">
            {initials}
          </div>
          {!collapsed && (
            <>
              <div className="overflow-hidden flex-1 min-w-0">
                <div className="text-white text-xs font-semibold truncate">{user?.name}</div>
                <div className="text-slate-400 text-[10px] truncate">{roleLabel}</div>
              </div>
              <button
                onClick={handleLogout}
                className="text-slate-500 hover:text-rose-400 transition-colors flex-shrink-0"
                title="Sign out"
              >
                <span className="material-symbols-outlined text-base">logout</span>
              </button>
            </>
          )}
        </div>
      </div>
    </aside>
  );
}
