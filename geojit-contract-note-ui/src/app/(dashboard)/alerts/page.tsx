"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { Switch } from "@/components/ui/switch";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { TagInput } from "@/components/ui/tag-input";
import { toast } from "sonner";

const USE_MOCK = true;
void USE_MOCK;

// ─── Mock data ────────────────────────────────────────────────────────────────
const MOCK_ALERTS = [
  { id: "1", severity: "critical", title: "PFX Certificate Expiry",       message: "GEOJIT-PROD-2025 expires in 23 days",              time: "2m ago",  read: false },
  { id: "2", severity: "warning",  title: "High Bounce Rate",             message: "JOB-0428-01 bounce rate 6.2% (threshold: 5%)",    time: "2m ago",  read: false },
  { id: "3", severity: "critical", title: "Job Failure — PNL Segment",    message: "JOB-0428-04 failed after 15m — 3,100 records",    time: "2m ago",  read: false },
  { id: "4", severity: "warning",  title: "SES Quota at 87%",            message: "18,700/21,600 daily sends used",                  time: "5m ago",  read: false },
  { id: "5", severity: "info",     title: "Pipeline completed",           message: "JOB-0428-01 — 26,100 delivered successfully",     time: "10m ago", read: true  },
];

const MOCK_RULES = [
  { id: "r1", name: "Certificate Expiry Alert", type: "Expiry",      severity: "critical", channels: ["email"], enabled: true  },
  { id: "r2", name: "High Bounce Rate",         type: "Bounce Rate", severity: "warning",  channels: ["email", "sms"], enabled: true  },
  { id: "r3", name: "Job Failure",              type: "Job Status",  severity: "critical", channels: ["email"], enabled: true  },
  { id: "r4", name: "SES Quota Warning",        type: "Quota",       severity: "warning",  channels: ["email"], enabled: true  },
  { id: "r5", name: "Lambda Errors",            type: "Error Rate",  severity: "warning",  channels: ["sms"],   enabled: false },
  { id: "r6", name: "Pipeline Completed",       type: "Job Status",  severity: "info",     channels: [],        enabled: false },
];

const SEVERITY_BORDER: Record<string, string> = {
  critical: "border-l-4 border-red-500 bg-red-50/30",
  warning:  "border-l-4 border-amber-500 bg-amber-50/30",
  info:     "border-l-4 border-blue-500 bg-blue-50/30",
};

const SEVERITY_BADGE: Record<string, string> = {
  critical: "bg-red-100 text-red-700",
  warning:  "bg-amber-100 text-amber-700",
  info:     "bg-blue-100 text-blue-700",
};

const SEVERITY_ICON: Record<string, string> = {
  critical: "text-red-500", warning: "text-amber-500", info: "text-blue-500",
};

const CHANNEL_PILL: Record<string, string> = {
  email:    "bg-blue-100 text-blue-600",
  sms:      "bg-green-100 text-green-600",
  whatsapp: "bg-emerald-100 text-emerald-600",
};

const CHANNEL_ICON: Record<string, string> = {
  email: "email", sms: "sms", whatsapp: "chat",
};

export default function AlertsPage() {
  const [tab, setTab]             = useState<"history" | "rules" | "channels">("history");
  const [severityFilter, setSeverityFilter] = useState("All");
  const [alerts, setAlerts]       = useState(MOCK_ALERTS);
  const [rules, setRules]         = useState(MOCK_RULES);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [newRuleName, setNewRuleName] = useState("");
  const [newTrigger, setNewTrigger]   = useState("Job Status");
  const [newSeverity, setNewSeverity] = useState("warning");
  const [newRecipients, setNewRecipients] = useState<string[]>([]);
  const [includeDeepLink, setIncludeDeepLink] = useState(true);
  const [channelEmail, setChannelEmail]     = useState(true);
  const [channelSms, setChannelSms]         = useState(false);
  const [channelWhatsapp, setChannelWhatsapp] = useState(false);

  const unreadCount = alerts.filter((a) => !a.read).length;

  function markAllRead() {
    setAlerts((prev) => prev.map((a) => ({ ...a, read: true })));
    toast.success("All alerts marked as read");
  }

  function toggleRule(id: string) {
    setRules((prev) => prev.map((r) => r.id === id ? { ...r, enabled: !r.enabled } : r));
  }

  function deleteRule(id: string) {
    setRules((prev) => prev.filter((r) => r.id !== id));
    toast.success("Rule deleted");
  }

  function saveRule() {
    if (!newRuleName.trim()) return;
    const channels = [
      ...(channelEmail ? ["email"] : []),
      ...(channelSms ? ["sms"] : []),
      ...(channelWhatsapp ? ["whatsapp"] : []),
    ];
    setRules((prev) => [
      ...prev,
      { id: String(Date.now()), name: newRuleName, type: newTrigger, severity: newSeverity, channels, enabled: true },
    ]);
    setDrawerOpen(false);
    setNewRuleName("");
    toast.success("Alert rule created");
  }

  const filteredAlerts = severityFilter === "All"
    ? alerts
    : alerts.filter((a) => a.severity === severityFilter.toLowerCase());

  return (
    <div className="space-y-5 fade-up">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h1 className="font-bold text-2xl text-gray-900">Alerts</h1>
          {unreadCount > 0 && (
            <span className="px-2 py-0.5 bg-red-100 text-red-700 text-xs font-bold rounded-full">
              {unreadCount} unread
            </span>
          )}
        </div>
        <button
          onClick={markAllRead}
          className="px-3 py-1.5 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors"
        >
          Mark all read
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-6 border-b border-gray-200">
        {([
          { key: "history",  label: "Alert History" },
          { key: "rules",    label: "Alert Rules" },
          { key: "channels", label: "Channel Configuration" },
        ] as const).map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={cn(
              "pb-2.5 text-sm font-medium transition-colors -mb-px",
              tab === key
                ? "border-b-2 border-[#00174b] text-[#00174b]"
                : "text-gray-500 hover:text-gray-900",
            )}
          >
            {label}
          </button>
        ))}
      </div>

      {/* ── Alert History ─────────────────────────────────────────────── */}
      {tab === "history" && (
        <div className="space-y-4">
          {/* Severity filter pills */}
          <div className="flex items-center gap-2">
            {["All", "Critical", "Warning", "Info"].map((s) => (
              <button
                key={s}
                onClick={() => setSeverityFilter(s)}
                className={cn(
                  "px-3 py-1 rounded-full text-sm font-medium transition-colors border",
                  severityFilter === s
                    ? "bg-[#00174b] text-white border-[#00174b]"
                    : "border-gray-200 text-gray-600 hover:bg-gray-50",
                )}
              >
                {s}
              </button>
            ))}
          </div>

          {/* Alert cards */}
          <div className="space-y-2">
            {filteredAlerts.map((alert) => (
              <div
                key={alert.id}
                className={cn(
                  "rounded-xl p-4 flex items-start gap-3 border",
                  SEVERITY_BORDER[alert.severity],
                  !alert.read && "shadow-sm",
                )}
              >
                <span className={cn("material-symbols-outlined text-[20px] flex-shrink-0 mt-0.5", SEVERITY_ICON[alert.severity])}>
                  {alert.severity === "critical" ? "error" : alert.severity === "warning" ? "warning" : "info"}
                </span>
                <div className="flex-1">
                  <div className="font-semibold text-gray-900 text-sm">{alert.title}</div>
                  <div className="text-xs text-gray-600 mt-0.5">{alert.message}</div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  {!alert.read && (
                    <span className="w-2 h-2 rounded-full bg-blue-500" />
                  )}
                  <span className="text-xs text-gray-400">{alert.time}</span>
                </div>
              </div>
            ))}
            {filteredAlerts.length === 0 && (
              <div className="text-center py-12 text-gray-400 text-sm">No alerts for this severity</div>
            )}
          </div>
        </div>
      )}

      {/* ── Alert Rules ───────────────────────────────────────────────── */}
      {tab === "rules" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="text-sm font-semibold text-gray-700">Alert Rules ({rules.length})</div>
            <button
              onClick={() => setDrawerOpen(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors"
            >
              <span className="material-symbols-outlined text-[16px]">add</span>
              New Rule
            </button>
          </div>

          <div className="bg-white rounded-xl border overflow-hidden">
            <table className="w-full text-left">
              <thead>
                <tr className="t-hd">
                  <th className="px-4 py-3">RULE NAME</th>
                  <th className="px-4 py-3">TYPE</th>
                  <th className="px-4 py-3">SEVERITY</th>
                  <th className="px-4 py-3">CHANNELS</th>
                  <th className="px-4 py-3 text-center">ENABLED</th>
                  <th className="px-4 py-3 w-20" />
                </tr>
              </thead>
              <tbody>
                {rules.map((rule) => (
                  <tr key={rule.id} className="t-row">
                    <td className="px-4 py-3 text-sm font-medium text-gray-800">{rule.name}</td>
                    <td className="px-4 py-3 text-xs text-gray-500">{rule.type}</td>
                    <td className="px-4 py-3">
                      <span className={cn("px-2 py-0.5 rounded-full text-[10px] font-bold capitalize", SEVERITY_BADGE[rule.severity])}>
                        {rule.severity}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
                        {rule.channels.length === 0 ? (
                          <span className="text-xs text-gray-300">—</span>
                        ) : (
                          rule.channels.map((ch) => (
                            <span
                              key={ch}
                              className={cn("flex items-center gap-0.5 px-1.5 py-0.5 rounded-full text-[10px] font-medium", CHANNEL_PILL[ch] ?? "bg-gray-100 text-gray-400")}
                              title={ch}
                            >
                              <span className="material-symbols-outlined text-[12px]">{CHANNEL_ICON[ch] ?? "notifications"}</span>
                            </span>
                          ))
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <Switch checked={rule.enabled} onCheckedChange={() => toggleRule(rule.id)} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1 justify-end">
                        <button
                          onClick={() => deleteRule(rule.id)}
                          className="p-1 text-gray-400 hover:text-red-500 transition-colors"
                          title="Delete rule"
                        >
                          <span className="material-symbols-outlined text-[16px]">delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* New Rule Sheet */}
          <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
            <SheetContent side="right" className="w-[480px]">
              <SheetHeader>
                <SheetTitle>New Alert Rule</SheetTitle>
              </SheetHeader>
              <div className="flex flex-col gap-4 mt-4 overflow-y-auto max-h-[calc(100vh-120px)] pr-1">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-medium text-gray-700">Alert Name</label>
                  <input
                    value={newRuleName}
                    onChange={(e) => setNewRuleName(e.target.value)}
                    placeholder="e.g. High Bounce Rate Alert"
                    className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-medium text-gray-700">Trigger</label>
                  <select
                    value={newTrigger}
                    onChange={(e) => setNewTrigger(e.target.value)}
                    className="border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none"
                  >
                    {["Job Status", "Bounce Rate", "Quota", "Expiry", "Error Rate", "Delivery Rate", "Lambda Timeout"].map((t) => (
                      <option key={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-medium text-gray-700">Severity</label>
                  <select
                    value={newSeverity}
                    onChange={(e) => setNewSeverity(e.target.value)}
                    className="border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none"
                  >
                    <option value="critical">Critical</option>
                    <option value="warning">Warning</option>
                    <option value="info">Info</option>
                  </select>
                </div>
                <div className="flex flex-col gap-2">
                  <label className="text-xs font-medium text-gray-700">Notification Channels</label>
                  {[
                    { key: "email", label: "Email", icon: "email", checked: channelEmail, set: setChannelEmail },
                    { key: "sms", label: "SMS", icon: "sms", checked: channelSms, set: setChannelSms },
                    { key: "whatsapp", label: "WhatsApp", icon: "chat", checked: channelWhatsapp, set: setChannelWhatsapp },
                  ].map((ch) => (
                    <label key={ch.key} className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
                      <span className="material-symbols-outlined text-gray-500 text-[20px]">{ch.icon}</span>
                      <span className="text-sm text-gray-700 flex-1">{ch.label}</span>
                      <Switch checked={ch.checked} onCheckedChange={(v) => ch.set(v)} />
                    </label>
                  ))}
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-medium text-gray-700">Recipients</label>
                  <TagInput
                    value={newRecipients}
                    onChange={setNewRecipients}
                    placeholder="Add email addresses…"
                  />
                </div>
                <label className="flex items-center gap-3 cursor-pointer">
                  <Switch checked={includeDeepLink} onCheckedChange={setIncludeDeepLink} />
                  <span className="text-sm text-gray-700">Include deep link in notification</span>
                </label>
                <div className="flex gap-2 pt-2">
                  <button
                    onClick={saveRule}
                    className="flex-1 py-2 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors"
                  >
                    Save Rule
                  </button>
                  <button
                    onClick={() => setDrawerOpen(false)}
                    className="flex-1 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      )}

      {/* ── Channel Configuration ─────────────────────────────────────── */}
      {tab === "channels" && (
        <div className="space-y-3">
          {[
            { label: "Email",    icon: "email",   fields: [{ label: "SMTP Host", placeholder: "smtp.example.com" }, { label: "From Address", placeholder: "noreply@geojit.com" }] },
            { label: "SMS",      icon: "sms",     fields: [{ label: "Provider",  placeholder: "Twilio / AWS SNS" }, { label: "API Key", placeholder: "sk_..." }] },
            { label: "WhatsApp", icon: "chat",    fields: [{ label: "Business ID", placeholder: "1234567890" }, { label: "Access Token", placeholder: "EAAx..." }] },
          ].map((channel) => (
            <ChannelSection key={channel.label} {...channel} />
          ))}
        </div>
      )}
    </div>
  );
}

function ChannelSection({
  label, icon, fields,
}: {
  label: string;
  icon: string;
  fields: { label: string; placeholder: string }[];
}) {
  const [enabled, setEnabled] = useState(label === "Email");
  const [open, setOpen]       = useState(label === "Email");

  return (
    <div className="bg-white rounded-xl border overflow-hidden">
      <div
        className="w-full flex items-center gap-3 px-5 py-4 text-left hover:bg-gray-50 transition-colors cursor-pointer"
        onClick={() => setOpen((v) => !v)}
      >
        <span className="material-symbols-outlined text-gray-500 text-[20px]">{icon}</span>
        <span className="text-sm font-semibold text-gray-900 flex-1">{label}</span>
        <Switch
          checked={enabled}
          onCheckedChange={(v) => { setEnabled(v); }}
          onClick={(e) => e.stopPropagation()}
        />
        <span className={cn(
          "material-symbols-outlined text-gray-400 text-[18px] transition-transform duration-200",
          open ? "rotate-180" : "",
        )}>
          expand_more
        </span>
      </div>
      {open && (
        <div className="px-5 pb-5 border-t border-gray-100 pt-4">
          <div className="grid md:grid-cols-2 gap-4 mb-4">
            {fields.map((f) => (
              <div key={f.label} className="flex flex-col gap-1">
                <label className="text-xs text-gray-600 font-medium">{f.label}</label>
                <input
                  placeholder={f.placeholder}
                  disabled={!enabled}
                  className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#00174b]/20 disabled:opacity-50 disabled:cursor-not-allowed"
                />
              </div>
            ))}
          </div>
          <button
            disabled={!enabled}
            className="px-4 py-1.5 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            Test {label}
          </button>
        </div>
      )}
    </div>
  );
}
