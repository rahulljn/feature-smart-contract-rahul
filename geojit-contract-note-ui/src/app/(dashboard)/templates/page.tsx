"use client";

import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { templatesApi } from "@/lib/api";
import type { S3Template, TemplateFieldsRequest } from "@/types";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { useAuthStore } from "@/store/auth";

// ── Live preview HTML builder (mirrors EmailTemplateService.buildHtml) ──
function buildPreviewHtml(opts: {
  bodyColor: string; footerColor: string; greetingText: string;
  bodyIntro: string; logoUrl: string;
}): string {
  const bc = opts.bodyColor   || "#333333";
  const fc = opts.footerColor || "#666666";
  const gr = opts.greetingText || "Warm Greetings from Geojit Investments Ltd !";
  const bi = opts.bodyIntro   || "We hope your experience with Geojit Investments Ltd has been pleasant. We are herewith sending you your digitally signed contract note (PDF Document).";
  const logo = opts.logoUrl?.trim()
    ? `<div style="text-align:center;margin-bottom:16px;"><img src="${opts.logoUrl}" alt="Geojit Logo" style="max-width:200px;" /></div>\n`
    : "";
  return `<!DOCTYPE html>
<html>
<head><meta http-equiv="Content-Type" content="text/html; charset=utf-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 10pt; color: ${bc};">
${logo}Dear [NAME],
<br/><br/>
${gr}
<br/><br/>
${bi}
<br/><br/>
To open your attachment you require Adobe Acrobat Reader 6.0 or above or Foxit Reader.
<br/><br/>
<strong>Instructions for Opening the attachment:-</strong>
<br/><br/>
1. Click on the attachment provided with this mail. If you are prompted for a password, please follow the below steps.
<br/><br/>
<strong>INDIVIDUAL</strong> clients may enter the first four characters of your PAN (in CAPITAL letters) followed by first four characters of your DATE OF BIRTH (DOB) [in DDMM format] as the password for the PDF attachment.
<br/>
For Eg. PAN: BDPBV2015Z and DOB: 31.01.1979 then password will be <strong>BDPB3101</strong>
<br/><br/>
For <strong>NON-INDIVIDUAL</strong> clients you may enter your PAN (in CAPITAL letters) as the password for the PDF attachment.
<br/>
For Eg. PAN: BDPBV2015Z then password will be <strong>BDPBV2015Z</strong>
<br/><br/>
2. To view details regarding the digital signature, please click on the icon of a pen, on the left hand side frame of Adobe acrobat.
<br/><br/>
<strong>Security Notice:</strong> We will never ask for your login ID, password, or OTP. Please refrain from sharing this information with anyone. Your security is our top priority.
<br/><br/>
To download Adobe Reader, please visit <a href="http://get.adobe.com/reader/otherversions">http://get.adobe.com/reader/otherversions</a>
<br/>
To download Foxit Reader, please visit <a href="http://www.foxitsoftware.com/downloads/">http://www.foxitsoftware.com/downloads/</a>
<br/><br/>
For all queries, kindly contact <a href="mailto:customercare@geojit.com">customercare@geojit.com</a>.
<br/>
Toll Free No: 1800-571-5501, 1800-103-5501. Paid Line: +91-484-3911777
<br/><br/>
<pre style="font-family: monospace; font-size: 9pt; color: ${fc};">---------------------------------------------------------------------------
The information contained in this electronic message and its attachments (the "message")
is intended solely for the addressees and is confidential and privileged.
If you are not the intended recipient, please notify the sender by reply e-mail
and then destroy the message. Any dissemination, distribution, forwarding, copying,
printing or disclosure, either whole or partial, is prohibited and may be unlawful.
Equity/Mutual Fund investments are subject to market risks.
Past performance does not guarantee future returns.
We do not offer any product which gives guaranteed returns.
WARNING: Computer viruses can be transmitted via email. The recipient should check
this email and any attachments for the presence of viruses. The company accepts no
liability for any damage caused by any virus transmitted by this email.
-----------------------------------------------------------------------</pre>
</body>
</html>`;
}

const FIELD_COLORS: Record<string, string> = {
  subject:      "bg-purple-100 text-purple-900 border-purple-300",
  greetingText: "bg-yellow-100 text-yellow-900 border-yellow-300",
  bodyIntro:    "bg-blue-100   text-blue-900   border-blue-300",
  logoUrl:      "bg-teal-100   text-teal-900   border-teal-300",
  bodyColor:    "bg-orange-100 text-orange-900 border-orange-300",
  footerColor:  "bg-rose-100   text-rose-900   border-rose-300",
};

const FIELD_LABELS: Record<string, string> = {
  subject: "Subject", greetingText: "Greeting Text", bodyIntro: "Body Intro",
  logoUrl: "Logo URL", bodyColor: "Body Color", footerColor: "Footer Color",
};

type TabId = "fields" | "html" | "preview";

const DEFAULT_GREETING = "Warm Greetings from Geojit Investments Ltd !";
const DEFAULT_INTRO    = "We hope your experience with Geojit Investments Ltd has been pleasant. We are herewith sending you your digitally signed contract note (PDF Document).";

export default function TemplatesPage() {
  const qc = useQueryClient();
  const { user } = useAuthStore();
  const isEditor = user?.role === "ADMIN" || user?.role === "OPS_MANAGER";

  // List state
  const [viewingName, setViewingName]   = useState<string | null>(null);
  const [editingName, setEditingName]   = useState<string | null>(null);
  const [activeTab,   setActiveTab]     = useState<TabId>("fields");

  // Edit form state
  const [subject,      setSubject]      = useState("Contract Note");
  const [greetingText, setGreetingText] = useState(DEFAULT_GREETING);
  const [bodyIntro,    setBodyIntro]    = useState(DEFAULT_INTRO);
  const [logoUrl,      setLogoUrl]      = useState("");
  const [bodyColor,    setBodyColor]    = useState("#333333");
  const [footerColor,  setFooterColor]  = useState("#666666");

  // Saved-to-state snapshot for Reset
  const [savedFields, setSavedFields] = useState<TemplateFieldsRequest | null>(null);

  // List query
  const { data: templates, isLoading } = useQuery({
    queryKey: ["templates"],
    queryFn: () => templatesApi.list(),
    select: (res) => {
      const raw = res?.data?.data;
      const list: S3Template[] = Array.isArray(raw) ? raw : [];
      return list.sort((a) => (a.status === "active" ? -1 : 1));
    },
  });

  // View HTML query
  const { data: contentData, isLoading: contentLoading } = useQuery({
    queryKey: ["template-content", viewingName],
    queryFn: () => templatesApi.content(viewingName!),
    enabled: !!viewingName,
    select: (res) => res?.data?.data as string,
  });

  // Try to load active DB template fields for pre-population (best-effort)
  const { data: activeDbTemplate } = useQuery({
    queryKey: ["templates-active"],
    queryFn: () => templatesApi.getActive(),
    retry: false,
    select: (res) => res?.data?.data as {
      subject?: string; greetingText?: string; bodyIntro?: string;
      logoUrl?: string; bodyColor?: string; footerColor?: string;
    } | null,
  });

  // When edit opens, seed form from DB template (if available) or defaults
  useEffect(() => {
    if (!editingName) return;
    const src = activeDbTemplate;
    setSubject(src?.subject || "Contract Note");
    setGreetingText(src?.greetingText || DEFAULT_GREETING);
    setBodyIntro(src?.bodyIntro || DEFAULT_INTRO);
    setLogoUrl(src?.logoUrl || "");
    setBodyColor(src?.bodyColor || "#333333");
    setFooterColor(src?.footerColor || "#666666");
    setSavedFields(null);
    setActiveTab("fields");
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editingName]);

  const { mutate: saveFields, isPending: saving } = useMutation({
    mutationFn: () => {
      const req: TemplateFieldsRequest = { subject, greetingText, bodyIntro, logoUrl, bodyColor, footerColor };
      return templatesApi.saveFields(editingName!, req);
    },
    onSuccess: () => {
      toast.success("Template saved successfully");
      setSavedFields({ subject, greetingText, bodyIntro, logoUrl, bodyColor, footerColor });
      qc.invalidateQueries({ queryKey: ["templates"] });
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "Save failed";
      toast.error(msg);
    },
  });

  function handleValidate() {
    const html = buildPreviewHtml({ bodyColor, footerColor, greetingText, bodyIntro, logoUrl });
    const errors: string[] = [];
    if (!html.includes("<html")) errors.push("Missing <html> tag");
    if (!html.includes("<body")) errors.push("Missing <body> tag");
    if (!html.includes("[NAME]")) errors.push("Missing [NAME] placeholder");
    if (errors.length) toast.error(`Validation failed: ${errors.join("; ")}`);
    else toast.success("Template is valid — HTML structure and [NAME] placeholder verified");
  }

  function handleReset() {
    const src = savedFields ?? activeDbTemplate ?? null;
    setSubject(src?.subject || "Contract Note");
    setGreetingText(src?.greetingText || DEFAULT_GREETING);
    setBodyIntro(src?.bodyIntro || DEFAULT_INTRO);
    setLogoUrl(src?.logoUrl || "");
    setBodyColor(src?.bodyColor || "#333333");
    setFooterColor(src?.footerColor || "#666666");
  }

  const TABS: { id: TabId; label: string; icon: string }[] = [
    { id: "fields",  label: "Edit Fields", icon: "edit" },
    { id: "html",    label: "Full HTML",   icon: "code" },
    { id: "preview", label: "Preview",     icon: "visibility" },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="animate-spin text-[#00174b] w-8 h-8" />
      </div>
    );
  }

  return (
    <div className="p-6 space-y-5 max-w-[1400px] mx-auto w-full fade-up">

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Email Templates</h2>
          <p className="text-slate-500 text-sm mt-1">
            Email templates stored in S3. The active template is used by the Lambda email pipeline.
          </p>
        </div>
      </div>

      {/* List */}
      <div className="card overflow-hidden">
        {!templates || templates.length === 0 ? (
          <div className="p-10 text-center text-slate-400 text-sm">No templates found in S3 bucket.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50 text-left">
                <th className="px-5 py-3.5 font-semibold text-slate-500 text-[11px] uppercase tracking-widest">Name</th>
                <th className="px-5 py-3.5 font-semibold text-slate-500 text-[11px] uppercase tracking-widest">Status</th>
                <th className="px-5 py-3.5 font-semibold text-slate-500 text-[11px] uppercase tracking-widest">Last Modified</th>
                <th className="px-5 py-3.5 font-semibold text-slate-500 text-[11px] uppercase tracking-widest">Size</th>
                <th className="px-5 py-3.5 font-semibold text-slate-500 text-[11px] uppercase tracking-widest text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {templates.map(t => (
                <tr key={t.s3Key} className={cn("hover:bg-slate-50/60 transition-colors", editingName === t.name && "bg-blue-50/40")}>
                  <td className="px-5 py-4">
                    <div className="font-semibold text-slate-900">{t.name}</div>
                    <div className="text-[11px] text-slate-400 mono mt-0.5">{t.s3Key}</div>
                  </td>
                  <td className="px-5 py-4">
                    <span className={cn("pill", t.status === "active" ? "pill-ok" : "pill-neu")}>
                      {t.status === "active" ? "Active" : "Draft"}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-slate-500 text-[12px]">
                    {t.lastModified
                      ? new Date(t.lastModified).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" })
                      : <span className="text-slate-300">—</span>
                    }
                  </td>
                  <td className="px-5 py-4 text-slate-500 text-[12px] mono">
                    {(t.size / 1024).toFixed(1)} KB
                  </td>
                  <td className="px-5 py-4 text-right">
                    <div className="flex items-center gap-2 justify-end">
                      <button
                        onClick={() => setViewingName(viewingName === t.name ? null : t.name)}
                        className="px-3 py-1.5 bg-slate-100 text-slate-700 rounded-lg text-[12px] font-semibold hover:bg-slate-200 flex items-center gap-1"
                      >
                        <span className="material-symbols-outlined text-sm">
                          {viewingName === t.name ? "expand_less" : "code"}
                        </span>
                        {viewingName === t.name ? "Close" : "View HTML"}
                      </button>
                      {isEditor && (
                        <button
                          onClick={() => setEditingName(editingName === t.name ? null : t.name)}
                          className={cn(
                            "px-3 py-1.5 rounded-lg text-[12px] font-semibold flex items-center gap-1",
                            editingName === t.name
                              ? "bg-slate-200 text-slate-700 hover:bg-slate-300"
                              : "bg-[#00174b] text-white hover:bg-[#003ea8]"
                          )}
                        >
                          <span className="material-symbols-outlined text-sm">
                            {editingName === t.name ? "expand_less" : "edit"}
                          </span>
                          {editingName === t.name ? "Close" : "Edit"}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* HTML Viewer */}
      {viewingName && (
        <div className="card p-0 overflow-hidden">
          <div className="px-5 py-3 bg-slate-50 border-b border-slate-100 flex items-center gap-2">
            <span className="material-symbols-outlined text-slate-400 text-sm">code</span>
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest flex-1">
              HTML Source — {viewingName}.html
            </span>
            <button onClick={() => setViewingName(null)} className="text-slate-400 hover:text-slate-600 transition-colors">
              <span className="material-symbols-outlined text-base">close</span>
            </button>
          </div>
          {contentLoading ? (
            <div className="flex items-center justify-center h-32">
              <Loader2 className="animate-spin text-[#00174b] w-6 h-6" />
            </div>
          ) : (
            <pre className="p-5 text-[11.5px] leading-relaxed overflow-auto max-h-[60vh] bg-slate-900 text-slate-200 font-mono whitespace-pre-wrap break-words">
              {contentData ?? ""}
            </pre>
          )}
        </div>
      )}

      {/* Inline 3-Tab Editor */}
      {editingName && (
        <div className="card p-0 overflow-hidden">

          {/* Tab bar */}
          <div className="flex border-b border-slate-200 bg-slate-50">
            {TABS.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={cn(
                  "flex items-center gap-1.5 px-5 py-3.5 text-[13px] font-semibold border-b-2 transition-colors",
                  activeTab === tab.id
                    ? "border-[#00174b] text-[#00174b] bg-white"
                    : "border-transparent text-slate-500 hover:text-slate-800 hover:bg-white/60"
                )}
              >
                <span className="material-symbols-outlined text-base">{tab.icon}</span>
                {tab.label}
              </button>
            ))}
            <div className="flex-1" />
            <div className="flex items-center gap-2 px-4">
              <span className="text-[11px] text-slate-500 font-medium">Editing: <span className="font-bold text-slate-700">{editingName}</span></span>
            </div>
          </div>

          {/* Tab: Edit Fields */}
          {activeTab === "fields" && (
            <div className="p-6 space-y-5">

              {/* Legend */}
              <div className="p-3 bg-blue-50/60 border border-blue-100 rounded-xl">
                <div className="text-[10px] font-bold text-blue-800 uppercase tracking-widest mb-2">Editable fields — the rest of the HTML structure is fixed</div>
                <div className="flex flex-wrap gap-2">
                  {Object.entries(FIELD_COLORS).map(([field, cls]) => (
                    <span key={field} className={cn("px-2 py-0.5 rounded border text-[11px] font-medium", cls)}>
                      {FIELD_LABELS[field]}
                    </span>
                  ))}
                </div>
                <p className="text-[11px] text-blue-700 mt-2">Switch to <strong>Full HTML</strong> to see which parts of the email these fields control.</p>
              </div>

              {/* Subject */}
              <div>
                <label className="field-label">
                  <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.subject)}>Subject</span>
                  Email subject line <span className="text-red-500">*</span>
                </label>
                <input type="text" value={subject} onChange={e => setSubject(e.target.value)}
                  className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#497cff]/20" />
              </div>

              {/* Greeting Text */}
              <div>
                <label className="field-label">
                  <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.greetingText)}>Greeting Text</span>
                  Opening line shown after "Dear [NAME],"
                </label>
                <input type="text" value={greetingText} onChange={e => setGreetingText(e.target.value)}
                  placeholder={DEFAULT_GREETING}
                  className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#497cff]/20" />
              </div>

              {/* Body Intro */}
              <div>
                <label className="field-label">
                  <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.bodyIntro)}>Body Intro</span>
                  Opening paragraph explaining the purpose of the email
                </label>
                <textarea value={bodyIntro} onChange={e => setBodyIntro(e.target.value)} rows={3}
                  className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#497cff]/20 resize-y" />
              </div>

              {/* Logo URL */}
              <div>
                <label className="field-label">
                  <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.logoUrl)}>Logo URL</span>
                  Optional — image URL shown at the top of the email (leave blank to hide)
                </label>
                <input type="url" value={logoUrl} onChange={e => setLogoUrl(e.target.value)}
                  placeholder="https://example.com/logo.png"
                  className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#497cff]/20" />
              </div>

              {/* Color pickers */}
              <div className="grid md:grid-cols-2 gap-4">
                <div>
                  <label className="field-label">
                    <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.bodyColor)}>Body Color</span>
                    Main text color
                  </label>
                  <div className="flex items-center gap-2">
                    <input type="color" value={bodyColor} onChange={e => setBodyColor(e.target.value)}
                      className="w-10 h-10 rounded-lg border border-slate-200 cursor-pointer p-0.5" />
                    <input type="text" value={bodyColor} onChange={e => setBodyColor(e.target.value)}
                      placeholder="#333333"
                      className="flex-1 px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm mono focus:outline-none focus:ring-2 focus:ring-[#497cff]/20" />
                  </div>
                </div>
                <div>
                  <label className="field-label">
                    <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.footerColor)}>Footer Color</span>
                    Legal disclaimer footer text color
                  </label>
                  <div className="flex items-center gap-2">
                    <input type="color" value={footerColor} onChange={e => setFooterColor(e.target.value)}
                      className="w-10 h-10 rounded-lg border border-slate-200 cursor-pointer p-0.5" />
                    <input type="text" value={footerColor} onChange={e => setFooterColor(e.target.value)}
                      placeholder="#666666"
                      className="flex-1 px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm mono focus:outline-none focus:ring-2 focus:ring-[#497cff]/20" />
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-3 pt-2 border-t border-slate-100">
                <button onClick={handleValidate} disabled={saving}
                  className="px-4 py-2.5 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5 disabled:opacity-50">
                  <span className="material-symbols-outlined text-sm">check_circle</span>Validate
                </button>
                <button onClick={() => saveFields()} disabled={saving || !subject.trim()}
                  className="px-5 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50">
                  {saving ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-sm">save</span>}
                  {saving ? "Saving…" : "Save Changes"}
                </button>
                <button onClick={handleReset} disabled={saving}
                  className="px-4 py-2.5 bg-white border border-slate-200 text-slate-600 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5 disabled:opacity-50">
                  <span className="material-symbols-outlined text-sm">restart_alt</span>Reset
                </button>
              </div>
            </div>
          )}

          {/* Tab: Full HTML */}
          {activeTab === "html" && (
            <div className="p-6 space-y-3">
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-[12px] flex items-start gap-2">
                <span className="material-symbols-outlined text-base flex-shrink-0 mt-0.5">info</span>
                <span>This is the <strong>live-rendered HTML</strong> based on your current field values. Switch to <strong>Edit Fields</strong> to modify highlighted sections.</span>
              </div>
              <div className="flex flex-wrap gap-2 text-[11px]">
                {Object.entries(FIELD_COLORS).map(([field, cls]) => (
                  <span key={field} className={cn("px-2 py-0.5 rounded border font-medium", cls)}>
                    ■ {FIELD_LABELS[field]}
                  </span>
                ))}
              </div>
              <div className="border border-slate-200 rounded-xl overflow-hidden">
                <div className="px-4 py-2 bg-slate-50 border-b border-slate-100 flex items-center gap-2">
                  <span className="material-symbols-outlined text-slate-400 text-sm">code</span>
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">HTML Preview — live field values highlighted</span>
                </div>
                <pre className="p-5 text-[11.5px] leading-relaxed overflow-auto max-h-[60vh] bg-slate-900 text-slate-200 font-mono whitespace-pre-wrap break-words">
                  {buildPreviewHtml({ bodyColor, footerColor, greetingText, bodyIntro, logoUrl })
                    .split(/(Dear \[NAME\]|Warm Greetings from Geojit Investments Ltd !|We hope your experience)/g)
                    .map((part, i) => {
                      if (part === "Dear [NAME]") return <mark key={i} className="bg-yellow-200 text-yellow-900 rounded px-0.5">{part}</mark>;
                      return <span key={i}>{part}</span>;
                    })
                  }
                </pre>
              </div>
            </div>
          )}

          {/* Tab: Preview */}
          {activeTab === "preview" && (
            <div className="p-6 space-y-3">
              <div className="p-3 bg-blue-50 border border-blue-100 rounded-xl text-blue-800 text-[12px] flex items-center gap-2">
                <span className="material-symbols-outlined text-base">preview</span>
                Live preview reflects your current field values (unsaved changes included). <strong>[NAME]</strong> will be replaced with the customer name at send time.
              </div>
              <div className="border border-slate-200 rounded-xl overflow-hidden">
                <div className="px-4 py-2 bg-slate-50 border-b border-slate-100 text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                  Rendered email preview
                </div>
                <iframe
                  srcDoc={buildPreviewHtml({ bodyColor, footerColor, greetingText, bodyIntro, logoUrl })}
                  className="w-full h-[65vh] border-0"
                  sandbox="allow-same-origin"
                  title="Template preview"
                />
              </div>
            </div>
          )}

        </div>
      )}

    </div>
  );
}