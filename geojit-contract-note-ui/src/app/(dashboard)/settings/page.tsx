"use client";

import { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth";
import { cn } from "@/lib/utils";
import { BrandingPreview } from "@/components/ui/branding-preview";
import { toast } from "sonner";

const USE_MOCK = true;
void USE_MOCK;

const PRESET_COLORS = ["#00174b", "#1e3a8a", "#065f46", "#9f1239", "#334155", "#0f766e", "#92400e", "#1d4ed8"];
const ACCENT_PRESETS = ["#497cff", "#0ea5e9", "#8b5cf6", "#ec4899", "#10b981", "#f59e0b", "#ef4444", "#06b6d4"];

const DEFAULT_BRANDING = {
  appName: "Contract Note Platform",
  primaryColor: "#00174b",
  accentColor: "#497cff",
  logoUrl: "",
};

const CMS_DEFAULTS: Record<string, string> = {
  "privacy-policy": `<h2>Privacy Policy</h2><p>Geojit Financial Services Ltd respects your privacy. This document describes how we collect, use, and protect your personal information in connection with our contract note distribution services.</p><h3>Data Collection</h3><p>We collect trade-related data, contact information, and email addresses to generate and deliver contract notes as required by SEBI regulations.</p>`,
  "about-us": `<h2>About Us</h2><p>Geojit Financial Services Ltd is a leading financial services company operating across India. Our contract note platform ensures timely, secure, and regulatory-compliant delivery of trade confirmations to clients.</p><h3>Our Mission</h3><p>To provide transparent, efficient, and technology-driven financial services to every investor.</p>`,
};

export default function SettingsPage() {
  const user   = useAuthStore((s) => s.user);
  const router = useRouter();

  useEffect(() => {
    if (user && user.role !== "ADMIN") router.replace("/dashboard");
  }, [user, router]);

  const [branding, setBranding] = useState(DEFAULT_BRANDING);
  const [saved, setSaved]       = useState(false);
  const [cmsTab, setCmsTab]     = useState<"privacy-policy" | "about-us">("privacy-policy");
  const [cmsMode, setCmsMode]   = useState<"edit" | "preview">("edit");
  const [cmsContent, setCmsContent] = useState(CMS_DEFAULTS);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const logoRef = useRef<HTMLInputElement>(null);

  if (user?.role !== "ADMIN") return null;

  function handleSave() {
    document.documentElement.style.setProperty("--color-primary", branding.primaryColor);
    document.documentElement.style.setProperty("--color-accent", branding.accentColor);
    setSaved(true);
    toast.success("Settings saved");
    setTimeout(() => setSaved(false), 2000);
  }

  function handleReset() {
    setBranding(DEFAULT_BRANDING);
    toast.info("Reset to defaults");
  }

  function handleLogoChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 512 * 1024) { toast.error("Logo must be ≤ 512 KB"); return; }
    setLogoFile(file);
  }

  function saveCms() {
    toast.success(`${cmsTab === "privacy-policy" ? "Privacy Policy" : "About Us"} saved`);
  }

  return (
    <div className="space-y-5 fade-up max-w-5xl">

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="font-bold text-2xl text-gray-900">Settings</h1>
        <div className="flex gap-2">
          <button
            onClick={handleReset}
            className="px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Reset defaults
          </button>
          <button
            onClick={handleSave}
            className={cn(
              "flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-all",
              saved
                ? "bg-green-600 text-white"
                : "bg-[#00174b] text-white hover:bg-[#003ea8]",
            )}
          >
            <span className="material-symbols-outlined text-[16px]">
              {saved ? "check_circle" : "save"}
            </span>
            {saved ? "Saved!" : "Save Changes"}
          </button>
        </div>
      </div>

      {/* Branding section */}
      <div className="bg-white rounded-xl border p-5">
        <div className="text-base font-semibold text-gray-900 mb-4">Branding</div>
        <div className="grid md:grid-cols-2 gap-8">

          {/* Controls */}
          <div className="space-y-5">
            {/* App Name */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-700">App Name</label>
              <input
                value={branding.appName}
                onChange={(e) => setBranding((b) => ({ ...b, appName: e.target.value }))}
                className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
              />
            </div>

            {/* Logo */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-700">Logo</label>
              {logoFile ? (
                <div className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg">
                  <span className="material-symbols-outlined text-gray-400 text-[24px]">image</span>
                  <span className="text-sm text-gray-700 flex-1 truncate">{logoFile.name}</span>
                  <button
                    onClick={() => setLogoFile(null)}
                    className="text-xs text-red-500 hover:underline"
                  >
                    Remove
                  </button>
                </div>
              ) : (
                <div
                  className="border-2 border-dashed border-gray-300 rounded-xl p-5 flex flex-col items-center gap-2 cursor-pointer hover:border-gray-400 transition-colors"
                  onClick={() => logoRef.current?.click()}
                >
                  <span className="material-symbols-outlined text-gray-400 text-[28px]">upload_file</span>
                  <span className="text-xs text-gray-500">Drop PNG/SVG here or <span className="text-[#497cff]">click to browse</span></span>
                  <span className="text-[10px] text-gray-400">Max 512 KB</span>
                  <input ref={logoRef} type="file" accept="image/png,image/svg+xml" className="hidden" onChange={handleLogoChange} />
                </div>
              )}
            </div>

            {/* Primary Color */}
            <ColorField
              label="Primary Color"
              value={branding.primaryColor}
              presets={PRESET_COLORS}
              onChange={(v) => setBranding((b) => ({ ...b, primaryColor: v }))}
            />

            {/* Accent Color */}
            <ColorField
              label="Accent Color"
              value={branding.accentColor}
              presets={ACCENT_PRESETS}
              onChange={(v) => setBranding((b) => ({ ...b, accentColor: v }))}
            />
          </div>

          {/* Live preview */}
          <div className="flex flex-col gap-2">
            <span className="text-[10px] uppercase tracking-widest text-gray-400 font-medium">Live Preview</span>
            <BrandingPreview
              primaryColor={branding.primaryColor}
              accentColor={branding.accentColor}
              appName={branding.appName}
            />
          </div>
        </div>
      </div>

      {/* CMS section */}
      <div className="bg-white rounded-xl border p-5">
        <div className="text-base font-semibold text-gray-900 mb-4">Content Pages</div>

        {/* Tab selector */}
        <div className="flex gap-4 border-b border-gray-200 mb-4">
          {(["privacy-policy", "about-us"] as const).map((slug) => (
            <button
              key={slug}
              onClick={() => setCmsTab(slug)}
              className={cn(
                "pb-2.5 text-sm font-medium transition-colors -mb-px",
                cmsTab === slug
                  ? "border-b-2 border-[#00174b] text-[#00174b]"
                  : "text-gray-500 hover:text-gray-900",
              )}
            >
              {slug === "privacy-policy" ? "Privacy Policy" : "About Us"}
            </button>
          ))}
          <div className="ml-auto flex items-center gap-1 pb-1">
            <button
              onClick={() => setCmsMode("edit")}
              className={cn("px-3 py-1 text-xs rounded-lg", cmsMode === "edit" ? "bg-gray-200 font-medium" : "hover:bg-gray-100")}
            >
              Edit
            </button>
            <button
              onClick={() => setCmsMode("preview")}
              className={cn("px-3 py-1 text-xs rounded-lg", cmsMode === "preview" ? "bg-gray-200 font-medium" : "hover:bg-gray-100")}
            >
              Preview
            </button>
          </div>
        </div>

        {cmsMode === "edit" ? (
          <textarea
            value={cmsContent[cmsTab]}
            onChange={(e) => setCmsContent((c) => ({ ...c, [cmsTab]: e.target.value }))}
            rows={12}
            className="w-full border border-gray-200 rounded-lg p-3 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-[#00174b]/20 resize-none"
          />
        ) : (
          <div
            className="min-h-[200px] bg-gray-50 rounded-lg p-4 prose prose-sm max-w-none text-gray-700"
            dangerouslySetInnerHTML={{ __html: cmsContent[cmsTab] }}
          />
        )}

        <div className="mt-3 flex justify-end">
          <button
            onClick={saveCms}
            className="px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors"
          >
            Save Changes
          </button>
        </div>
      </div>
    </div>
  );
}

function ColorField({
  label, value, presets, onChange,
}: {
  label: string;
  value: string;
  presets: string[];
  onChange: (v: string) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      <label className="text-xs font-medium text-gray-700">{label}</label>
      <div className="flex items-center gap-2">
        <input
          type="color"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-8 h-8 rounded-lg border border-gray-200 cursor-pointer overflow-hidden p-0.5"
        />
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          maxLength={7}
          className="w-20 border border-gray-200 rounded-lg px-2 py-1 text-xs font-mono focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
        />
      </div>
      <div className="flex gap-1.5 flex-wrap">
        {presets.map((c) => (
          <button
            key={c}
            onClick={() => onChange(c)}
            className={cn(
              "w-4 h-4 rounded-full border-2 transition-transform hover:scale-125",
              value === c ? "border-gray-700 scale-125" : "border-transparent",
            )}
            style={{ backgroundColor: c }}
            title={c}
          />
        ))}
      </div>
    </div>
  );
}
