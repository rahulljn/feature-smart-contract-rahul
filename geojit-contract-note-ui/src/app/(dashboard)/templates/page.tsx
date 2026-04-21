"use client";

import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { templatesApi } from "@/lib/api";
import type { EmailTemplate, TemplateFieldsRequest, TemplateValidateResult } from "@/types";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { useAuthStore } from "@/store/auth";

// ── Live preview HTML builder (mirrors EmailTemplateService.buildHtml) ──
function buildPreviewHtml(opts: {
  bodyColor: string; footerColor: string; greetingText: string;
  bodyIntro: string;
}): string {
  const bc = opts.bodyColor   || "#333333";
  const fc = opts.footerColor || "#666666";
  const gr = opts.greetingText || "Warm Greetings from Geojit Investments Ltd !";
  const bi = opts.bodyIntro   || "We hope your experience with Geojit Investments Ltd has been pleasant. We are herewith sending you your digitally signed contract note (PDF Document).";

  return `<!DOCTYPE html>
<html>
<head><meta http-equiv="Content-Type" content="text/html; charset=utf-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 10pt; color: ${bc};">
Dear [NAME],
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

// ── Annotated HTML builder ──────────────────────────────────────────────
// Returns segments: fixed text OR an editable zone marker
type Segment =
  | { type: "fixed"; text: string }
  | { type: "editable"; field: keyof TemplateFieldsRequest; label: string; value: string };

function buildAnnotatedSegments(t: EmailTemplate): Segment[] {
  const bodyColor   = t.bodyColor   || "#333333";
  const footerColor = t.footerColor || "#666666";
  const greeting    = t.greetingText || "Warm Greetings from Geojit Investments Ltd !";
  const bodyIntro   = t.bodyIntro   || "We hope your experience with Geojit Investments Ltd has been pleasant. We are herewith sending you your digitally signed contract note (PDF Document).";
  const segs: Segment[] = [];
  const fix = (text: string) => segs.push({ type: "fixed", text });
  const edit = (field: keyof TemplateFieldsRequest, label: string, value: string) =>
    segs.push({ type: "editable", field, label, value });

  fix(`<!DOCTYPE html>\n<html>\n<head>\n  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">\n</head>\n<body style="font-family: Arial, sans-serif; font-size: 10pt; color: `);
  edit("bodyColor", "Body Color", bodyColor);
  fix(`;">\n`);


  fix(`Dear [NAME],\n<br/><br/>\n`);
  edit("greetingText", "Greeting Text", greeting);
  fix(`\n<br/><br/>\n`);
  edit("bodyIntro", "Body Intro", bodyIntro);
  fix(`\n<br/><br/>\nTo open your attachment you require Adobe Acrobat Reader 6.0 or above or Foxit Reader.\n<br/><br/>\n<strong>Instructions for Opening the attachment:-</strong>\n<br/><br/>\n1. Click on the attachment provided with this mail. If you are prompted for a password, please follow the below steps.\n<br/><br/>\n<strong>INDIVIDUAL</strong> clients may enter the first four characters of your PAN (in CAPITAL letters) followed by first four characters of your DATE OF BIRTH (DOB) [in DDMM format] as the password for the PDF attachment.\n<br/>\nFor Eg. PAN: BDPBV2015Z and DOB: 31.01.1979 then password will be <strong>BDPB3101</strong>\n<br/><br/>\nFor <strong>NON-INDIVIDUAL</strong> clients you may enter your PAN (in CAPITAL letters) as the password for the PDF attachment.\n<br/>\nFor Eg. PAN: BDPBV2015Z then password will be <strong>BDPBV2015Z</strong>\n<br/><br/>\n2. To view details regarding the digital signature, please click on the icon of a pen, on the left hand side frame of Adobe acrobat.\n<br/><br/>\n<strong>Security Notice:</strong> We will never ask for your login ID, password, or OTP. Please refrain from sharing this information with anyone. Your security is our top priority.\n<br/><br/>\nTo download Adobe Reader, please visit <a href="http://get.adobe.com/reader/otherversions">http://get.adobe.com/reader/otherversions</a>\n<br/>\nTo download Foxit Reader, please visit <a href="http://www.foxitsoftware.com/downloads/">http://www.foxitsoftware.com/downloads/</a>\n<br/><br/>\nFor all queries, kindly contact <a href="mailto:customercare@geojit.com">customercare@geojit.com</a>.\n<br/>\nToll Free No: 1800-571-5501, 1800-103-5501. Paid Line: +91-484-3911777\n<br/><br/>\n<pre style="font-family: monospace; font-size: 9pt; color: `);
  edit("footerColor", "Footer Color", footerColor);
  fix(`;">---------------------------------------------------------------------------\nThe information contained in this electronic message and its attachments (the "message")\nis intended solely for the addressees and is confidential and privileged.\n...(legal disclaimer)...\n-----------------------------------------------------------------------</pre>\n</body>\n</html>`);

  return segs;
}

// ── field colors for annotation highlights ─────────────────────────────
const FIELD_COLORS: Record<string, string> = {
  subject:      "bg-purple-100 text-purple-900 border-purple-300",
  greetingText: "bg-yellow-100 text-yellow-900 border-yellow-300",
  bodyIntro:    "bg-blue-100   text-blue-900   border-blue-300",

  bodyColor:    "bg-orange-100 text-orange-900 border-orange-300",
  footerColor:  "bg-rose-100   text-rose-900   border-rose-300",
};

type TabId = "fields" | "html" | "preview";

export default function TemplatesPage() {
  const qc = useQueryClient();
  const { user } = useAuthStore();
  const isEditor = user?.role === "ADMIN" || user?.role === "OPS_MANAGER";

  const [activeTab, setActiveTab] = useState<TabId>("fields");

  // Edit form state
  const [subject,      setSubject]      = useState("");
  const [greetingText, setGreetingText] = useState("");
  const [bodyIntro,    setBodyIntro]    = useState("");
  const [bodyColor,    setBodyColor]    = useState("#333333");
  const [footerColor,  setFooterColor]  = useState("#666666");

  const { data, isLoading } = useQuery({
    queryKey: ["templates"],
    queryFn: () => templatesApi.list(),
    select: (res) => {
      const raw = res?.data?.data;
      const list: EmailTemplate[] = Array.isArray(raw) ? raw : [];
      // Return the active template first, then others
      return list.sort((a, b) => (b.isActive ? 1 : 0) - (a.isActive ? 1 : 0));
    },
  });

  const template = data?.[0] ?? null;

  const [initialized, setInitialized] = useState(false);

  // Initialize form once when template data arrives
  useEffect(() => {
    if (template && !initialized) {
      setInitialized(true);
      setSubject(template.subject ?? "Contract Note - Geojit Investments Ltd");
      setGreetingText(template.greetingText ?? "Warm Greetings from Geojit Investments Ltd !");
      setBodyIntro(template.bodyIntro ?? "We hope your experience with Geojit Investments Ltd has been pleasant. We are herewith sending you your digitally signed contract note (PDF Document).");
      setBodyColor(template.bodyColor ?? "#333333");
      setFooterColor(template.footerColor ?? "#666666");
    }
  }, [template, initialized]);

  const { mutate: saveFields, isPending: saving } = useMutation({
    mutationFn: () => {
      if (!template) throw new Error("No template loaded");
      const req: TemplateFieldsRequest = { subject, greetingText, bodyIntro, bodyColor, footerColor };
      return templatesApi.updateFields(template.templateId, req);
    },
    onSuccess: () => {
      toast.success("Template saved successfully");
      qc.invalidateQueries({ queryKey: ["templates"] });
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "Save failed";
      toast.error(msg);
    },
  });

  const { mutate: runValidate, isPending: validating } = useMutation({
    mutationFn: () => {
      if (!template) throw new Error("No template loaded");
      return templatesApi.validate(template.templateId);
    },
    onSuccess: (res) => {
      const result: TemplateValidateResult = res?.data?.data;
      if (result.valid) {
        toast.success("Template is valid — HTML structure and [NAME] placeholder verified");
      } else {
        toast.error(`Validation failed: ${result.errors.join("; ")}`);
      }
    },
    onError: () => toast.error("Validation request failed"),
  });

  const TABS: { id: TabId; label: string; icon: string }[] = [
    { id: "fields",  label: "Edit Fields",  icon: "edit" },
    { id: "html",    label: "Full HTML",     icon: "code" },
    { id: "preview", label: "Preview",       icon: "visibility" },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="animate-spin text-[#00174b] w-8 h-8" />
      </div>
    );
  }

  if (!template) {
    return (
      <div className="p-6 text-center text-slate-500">
        No email template found. Contact your administrator.
      </div>
    );
  }

  const liveTemplate: EmailTemplate = { ...template, subject, greetingText, bodyIntro, bodyColor, footerColor };
  const segs = buildAnnotatedSegments(liveTemplate);

  return (
    <div className="p-6 space-y-5 max-w-[1400px] mx-auto w-full fade-up">

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Email Templates</h2>
          <p className="text-slate-500 text-sm mt-1">
            Manage the email template used for contract note delivery. Only highlighted fields are editable — the rest of the HTML structure is fixed.
          </p>
        </div>
      </div>

      {/* Template meta card */}
      <div className="card p-4 flex items-center gap-4 flex-wrap">
        <div className="p-2.5 bg-[#eff6ff] rounded-xl flex-shrink-0">
          <span className="material-symbols-outlined text-[#00174b]">edit_note</span>
        </div>
        <div className="flex-1 min-w-0">
          <div className="font-bold text-slate-900">{template.name}</div>
          <div className="text-[11px] text-slate-500 mono">{template.templateId}</div>
        </div>
        <div className="flex items-center gap-3 flex-wrap">
          <span className={cn("pill", template.isActive ? "pill-ok" : "pill-neu")}>
            {template.isActive ? "Active" : "Draft"}
          </span>
          {template.lastEditedAt && (
            <span className="text-[11px] text-slate-500">
              Last edited {new Date(template.lastEditedAt).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })}
              {template.lastEditedBy ? ` by ${template.lastEditedBy.name}` : ""}
            </span>
          )}
        </div>
      </div>

      {/* Tabs */}
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
        </div>

        {/* Tab: Edit Fields */}
        {activeTab === "fields" && (
          <div className="p-6 space-y-5">
            {!isEditor && (
              <div className="px-4 py-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-sm flex items-center gap-2">
                <span className="material-symbols-outlined text-base">lock</span>
                You have read-only access. Contact an Admin or Ops Manager to edit the template.
              </div>
            )}

            {/* Legend */}
            <div className="p-3 bg-blue-50/60 border border-blue-100 rounded-xl">
              <div className="text-[10px] font-bold text-blue-800 uppercase tracking-widest mb-2">Editable fields</div>
              <div className="flex flex-wrap gap-2">
                {Object.entries(FIELD_COLORS).map(([field, cls]) => (
                  <span key={field} className={cn("px-2 py-0.5 rounded border text-[11px] font-medium", cls)}>
                    {field === "greetingText" ? "Greeting Text"
                     : field === "bodyIntro"   ? "Body Intro"
                     : field === "logoUrl"     ? null
                     : field === "bodyColor"   ? "Body Color"
                     : field === "footerColor" ? "Footer Color"
                     : "Subject"}
                  </span>
                ))}
              </div>
              <p className="text-[11px] text-blue-700 mt-2">Switch to <strong>Full HTML</strong> tab to see exactly which parts of the email HTML these fields control.</p>
            </div>

            {/* Field: Subject */}
            <div>
              <label className="field-label">
                <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.subject)}>Subject</span>
                Email subject line <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={subject}
                onChange={e => setSubject(e.target.value)}
                disabled={!isEditor}
                placeholder="Contract Note - Geojit Investments Ltd"
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-[#497cff]/20"
              />
            </div>

            {/* Field: Greeting Text */}
            <div>
              <label className="field-label">
                <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.greetingText)}>Greeting Text</span>
                Opening greeting line shown after "Dear [NAME],"
              </label>
              <input
                type="text"
                value={greetingText}
                onChange={e => setGreetingText(e.target.value)}
                disabled={!isEditor}
                placeholder="Warm Greetings from Geojit Investments Ltd !"
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-[#497cff]/20"
              />
            </div>

            {/* Field: Body Intro */}
            <div>
              <label className="field-label">
                <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.bodyIntro)}>Body Intro</span>
                Opening paragraph explaining the purpose of the email
              </label>
              <textarea
                value={bodyIntro}
                onChange={e => setBodyIntro(e.target.value)}
                disabled={!isEditor}
                rows={3}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-[#497cff]/20 resize-y"
              />
            </div>

            {/* Logo URL removed — original template has no logo */}
            <div className="hidden">
              <input
                type="url"
                disabled
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-[#497cff]/20"
              />
            </div>

            {/* Color pickers */}
            <div className="grid md:grid-cols-2 gap-4">
              <div>
                <label className="field-label">
                  <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.bodyColor)}>Body Color</span>
                  Main text color for the email body
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="color"
                    value={bodyColor}
                    onChange={e => setBodyColor(e.target.value)}
                    disabled={!isEditor}
                    className="w-10 h-10 rounded-lg border border-slate-200 cursor-pointer disabled:cursor-not-allowed p-0.5"
                  />
                  <input
                    type="text"
                    value={bodyColor}
                    onChange={e => setBodyColor(e.target.value)}
                    disabled={!isEditor}
                    placeholder="#333333"
                    className="flex-1 px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm mono disabled:opacity-60 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-[#497cff]/20"
                  />
                </div>
              </div>
              <div>
                <label className="field-label">
                  <span className={cn("px-1.5 py-0.5 rounded border text-[10px] font-bold mr-1.5", FIELD_COLORS.footerColor)}>Footer Color</span>
                  Text color for the legal disclaimer footer
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="color"
                    value={footerColor}
                    onChange={e => setFooterColor(e.target.value)}
                    disabled={!isEditor}
                    className="w-10 h-10 rounded-lg border border-slate-200 cursor-pointer disabled:cursor-not-allowed p-0.5"
                  />
                  <input
                    type="text"
                    value={footerColor}
                    onChange={e => setFooterColor(e.target.value)}
                    disabled={!isEditor}
                    placeholder="#666666"
                    className="flex-1 px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm mono disabled:opacity-60 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-[#497cff]/20"
                  />
                </div>
              </div>
            </div>

            {/* Action buttons */}
            {isEditor && (
              <div className="flex items-center gap-3 pt-2 border-t border-slate-100">
                <button
                  onClick={() => runValidate()}
                  disabled={validating || saving}
                  className="px-4 py-2.5 bg-white border border-slate-200 text-slate-700 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5 disabled:opacity-50"
                >
                  {validating ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-sm">check_circle</span>}
                  Validate
                </button>
                <button
                  onClick={() => saveFields()}
                  disabled={saving || validating}
                  className="px-5 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5 disabled:opacity-50"
                >
                  {saving ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-sm">save</span>}
                  Save Changes
                </button>
                <button
                  onClick={() => {
                    if (!template) return;
                    setSubject(template.subject ?? "");
                    setGreetingText(template.greetingText ?? "Warm Greetings from Geojit Investments Ltd !");
                    setBodyIntro(template.bodyIntro ?? "We hope your experience with Geojit Investments Ltd has been pleasant. We are herewith sending you your digitally signed contract note (PDF Document).");
                    setBodyColor(template.bodyColor ?? "#333333");
                    setFooterColor(template.footerColor ?? "#666666");
                  }}
                  disabled={saving}
                  className="px-4 py-2.5 bg-white border border-slate-200 text-slate-600 rounded-xl text-sm font-semibold hover:bg-slate-50 flex items-center gap-1.5 disabled:opacity-50"
                >
                  <span className="material-symbols-outlined text-sm">restart_alt</span>
                  Reset
                </button>
              </div>
            )}
          </div>
        )}

        {/* Tab: Full HTML */}
        {activeTab === "html" && (
          <div className="p-6 space-y-3">
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-[12px] flex items-start gap-2">
              <span className="material-symbols-outlined text-base flex-shrink-0 mt-0.5">info</span>
              <span>
                This is the <strong>full HTML email template</strong>. Highlighted sections correspond to editable fields — switch to the <strong>Edit Fields</strong> tab to modify them. The rest of the HTML structure is fixed and cannot be changed.
              </span>
            </div>

            {/* Legend */}
            <div className="flex flex-wrap gap-2 text-[11px]">
              {Object.entries(FIELD_COLORS).map(([field, cls]) => (
                <span key={field} className={cn("px-2 py-0.5 rounded border font-medium", cls)}>
                  ■ {field === "greetingText" ? "Greeting Text"
                     : field === "bodyIntro"   ? "Body Intro"
                     : field === "logoUrl"     ? null
                     : field === "bodyColor"   ? "Body Color"
                     : field === "footerColor" ? "Footer Color"
                     : "Subject"}
                </span>
              ))}
            </div>

            <div className="border border-slate-200 rounded-xl overflow-hidden">
              <div className="px-4 py-2 bg-slate-50 border-b border-slate-100 flex items-center gap-2">
                <span className="material-symbols-outlined text-slate-400 text-sm">code</span>
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">HTML Source — editable zones are highlighted</span>
              </div>
              <pre className="p-5 text-[11.5px] leading-relaxed overflow-auto max-h-[60vh] bg-slate-900 text-slate-200 font-mono whitespace-pre-wrap break-words">
                {segs.map((seg, i) =>
                  seg.type === "fixed" ? (
                    <span key={i}>{seg.text}</span>
                  ) : (
                    <mark
                      key={i}
                      className={cn(
                        "rounded px-0.5 border font-semibold not-italic cursor-default",
                        FIELD_COLORS[seg.field]
                      )}
                      title={`Editable: ${seg.label}`}
                    >
                      {seg.value}
                    </mark>
                  )
                )}
              </pre>
            </div>
          </div>
        )}

        {/* Tab: Preview */}
        {activeTab === "preview" && (
          <div className="p-6 space-y-3">
            <div className="p-3 bg-blue-50 border border-blue-100 rounded-xl text-blue-800 text-[12px] flex items-center gap-2">
              <span className="material-symbols-outlined text-base">preview</span>
              Live preview reflects your current form values (unsaved changes included). <strong>[NAME]</strong> will be replaced with the actual customer name at send time.
            </div>
            <div className="border border-slate-200 rounded-xl overflow-hidden">
              <div className="px-4 py-2 bg-slate-50 border-b border-slate-100 text-[10px] font-bold text-slate-500 uppercase tracking-widest">
                Rendered email preview
              </div>
              <iframe
                srcDoc={buildPreviewHtml({ bodyColor, footerColor, greetingText, bodyIntro })}
                className="w-full h-[65vh] border-0"
                sandbox="allow-same-origin"
                title="Template preview"
              />
            </div>
          </div>
        )}
      </div>

    </div>
  );
}
