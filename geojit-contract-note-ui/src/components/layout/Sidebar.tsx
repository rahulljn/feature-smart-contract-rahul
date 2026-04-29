"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";
import { useState, useEffect } from "react";
import { alertsApi } from "@/lib/api";

const USE_MOCK = true;

interface NavItem {
  href: string;
  label: string;
  icon: string;
  adminOnly?: boolean;
  opsAndAdmin?: boolean;
  showUnreadBadge?: boolean;
}

interface NavSection {
  name: string;
  items: NavItem[];
}

const navSections: NavSection[] = [
  {
    name: "OVERVIEW",
    items: [
      { href: "/dashboard",     label: "Dashboard",      icon: "dashboard" },
      { href: "/ops-dashboard", label: "Ops Dashboard",  icon: "monitor_heart", opsAndAdmin: true },
    ],
  },
  {
    name: "PIPELINE",
    items: [
      { href: "/process", label: "Upload & Process", icon: "upload" },
      { href: "/jobs",    label: "Jobs",             icon: "work" },
      { href: "/resend",  label: "Bulk Resend",      icon: "forward_to_inbox" },
    ],
  },
  {
    name: "CONFIGURATION",
    items: [
      { href: "/templates",       label: "Templates",      icon: "description" },
      { href: "/clients",         label: "Client Lookup",  icon: "person_search" },
      { href: "/ses-config",      label: "SES Config",     icon: "mail" },
      { href: "/certificates",    label: "Certificates",   icon: "verified_user" },
    ],
  },
  {
    name: "SYSTEM",
    items: [
      { href: "/exceptions", label: "Exceptions",  icon: "bug_report" },
      { href: "/users",      label: "Users",       icon: "group",         adminOnly: true },
      { href: "/audit",      label: "Audit Log",   icon: "history",       adminOnly: true },
      { href: "/alerts",     label: "Alerts",      icon: "notifications", opsAndAdmin: true, showUnreadBadge: true },
      { href: "/settings",   label: "Settings",    icon: "settings",      adminOnly: true },
    ],
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user } = useAuthStore();
  const [unreadCount, setUnreadCount] = useState(0);
  const [openSections, setOpenSections] = useState<Record<string, boolean>>({
    OVERVIEW: true,
    PIPELINE: true,
    CONFIGURATION: true,
    SYSTEM: true,
  });

  const isAdmin = user?.role === "ADMIN";
  const isOpsOrAdmin = user?.role === "ADMIN" || user?.role === "OPS_MANAGER";

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
      } catch {
        // silently fail
      }
    }
    fetchUnread();
    const id = setInterval(fetchUnread, 60_000);
    return () => { cancelled = true; clearInterval(id); };
  }, []);

  function toggleSection(name: string) {
    setOpenSections((prev) => ({ ...prev, [name]: !prev[name] }));
  }

  return (
    <aside className="flex flex-col h-full bg-[#00174b] text-white flex-shrink-0 w-44">
      {/* Logo area */}
      <div className="flex items-center gap-2.5 p-4 border-b border-white/10">
        <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center flex-shrink-0">
          <span className="material-symbols-outlined text-white text-[18px]">account_balance</span>
        </div>
        <span className="text-sm font-semibold text-white leading-tight truncate">ContractNote Pro</span>
      </div>

      {/* Search bar */}
      <div className="px-3 pt-3 pb-1">
        <div className="relative">
          <span className="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-white/40 text-[16px]">
            search
          </span>
          <input
            type="text"
            placeholder="Search jobs, templates…"
            className="w-full bg-white/10 rounded-lg pl-8 pr-3 py-1.5 text-sm text-white/70 placeholder-white/40 outline-none focus:bg-white/15 transition-colors"
          />
        </div>
      </div>

      {/* Nav sections */}
      <nav className="flex-1 overflow-y-auto px-2 py-2 space-y-0">
        {navSections.map((section) => {
          const visibleItems = section.items.filter(
            (item) =>
              (!item.adminOnly || isAdmin) &&
              (!item.opsAndAdmin || isOpsOrAdmin),
          );
          if (visibleItems.length === 0) return null;
          const isOpen = openSections[section.name] ?? true;

          return (
            <div key={section.name} className="mb-1">
              <button
                onClick={() => toggleSection(section.name)}
                className="flex items-center justify-between w-full px-2 pt-3 pb-1 group"
              >
                <span className="text-white/50 text-[10px] uppercase tracking-widest font-semibold">
                  {section.name}
                </span>
                <span
                  className={cn(
                    "material-symbols-outlined text-white/30 text-[14px] transition-transform duration-200",
                    isOpen ? "rotate-0" : "-rotate-90",
                  )}
                >
                  expand_more
                </span>
              </button>

              {isOpen && (
                <div className="space-y-0.5">
                  {visibleItems.map(({ href, label, icon, showUnreadBadge }) => {
                    const active =
                      pathname === href || pathname.startsWith(href + "/");
                    const badge =
                      showUnreadBadge && unreadCount > 0 ? unreadCount : 0;
                    return (
                      <Link
                        key={href}
                        href={href}
                        className={cn(
                          "flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm transition-all duration-150",
                          active
                            ? "bg-white/15 text-white font-medium"
                            : "text-white/70 hover:bg-white/10 hover:text-white",
                        )}
                      >
                        <span
                          className={cn(
                            "material-symbols-outlined text-[18px] flex-shrink-0",
                            active ? "text-white" : "text-white/50",
                          )}
                        >
                          {icon}
                        </span>
                        <span className="truncate flex-1 text-[0.8rem]">{label}</span>
                        {badge > 0 && (
                          <span className="ml-auto text-[9px] font-bold bg-rose-500 text-white px-1.5 py-0.5 rounded-full min-w-[18px] text-center flex-shrink-0">
                            {badge > 99 ? "99+" : badge}
                          </span>
                        )}
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </nav>

      {/* Bottom links */}
      <div className="mt-auto border-t border-white/10 px-3 py-3 flex gap-4">
        <Link
          href="/privacy-policy"
          className="text-white/50 text-xs hover:text-white/80 transition-colors"
        >
          Privacy Policy
        </Link>
        <Link
          href="/about-us"
          className="text-white/50 text-xs hover:text-white/80 transition-colors"
        >
          About Us
        </Link>
      </div>
    </aside>
  );
}
