"use client";

import { useState, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { configApi } from "@/lib/api";
import type { Certificate } from "@/types";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";

export default function CertificatesPage() {
  const qc = useQueryClient();
  const [showUploadDialog, setShowUploadDialog] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [pfxPassword, setPfxPassword] = useState("");
  const [certLabel, setCertLabel] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["certificates"],
    queryFn: () => configApi.certList(),
  });
  const _raw = data?.data?.data;
  const certs: Certificate[] = Array.isArray(_raw) ? _raw : [];

  const { mutate: activate } = useMutation({
    mutationFn: (id: string) => configApi.certActivate(id),
    onSuccess: () => { toast.success("Certificate activated"); qc.invalidateQueries({ queryKey: ["certificates"] }); },
    onError: () => toast.error("Failed to activate certificate"),
  });

  const { mutate: uploadCert, isPending: uploading } = useMutation({
    mutationFn: () => {
      if (!selectedFile || !pfxPassword) throw new Error("File and password required");
      const form = new FormData();
      form.append("file", selectedFile);
      form.append("password", pfxPassword);
      if (certLabel.trim()) form.append("label", certLabel.trim());
      return configApi.certUpload(form);
    },
    onSuccess: () => {
      toast.success("Certificate uploaded and parsed successfully");
      setShowUploadDialog(false);
      setSelectedFile(null);
      setPfxPassword("");
      setCertLabel("");
      qc.invalidateQueries({ queryKey: ["certificates"] });
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "Failed to upload certificate";
      toast.error(msg);
    },
  });

  const activeCert = certs.find(c => c.isActive);
  const daysToExpiry = activeCert?.validTo
    ? Math.max(0, Math.ceil((new Date(activeCert.validTo).getTime() - Date.now()) / 86400000))
    : null;

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">PFX Certificates</h2>
          <p className="text-slate-500 text-sm mt-1">Digital signing certificates used to sign every contract-note PDF with a verifiable identity. Only one certificate is active at a time.</p>
        </div>
        <button
          onClick={() => setShowUploadDialog(true)}
          className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5"
        >
          <span className="material-symbols-outlined text-base">upload_file</span>Upload PFX
        </button>
      </div>

      {/* Expiry warning */}
      {activeCert && daysToExpiry !== null && daysToExpiry <= 30 && (
        <div className="card p-5 border-l-4 border-l-amber-500">
          <div className="flex items-center gap-3">
            <span className="material-symbols-outlined text-amber-600 text-2xl">warning</span>
            <div className="flex-1">
              <div className="font-bold text-slate-900">
                Active certificate expires in {daysToExpiry} day{daysToExpiry !== 1 ? "s" : ""}
              </div>
              <div className="text-xs text-slate-500">
                {activeCert.fileName} — upload a renewed PFX and activate it before expiry to avoid signing failures.
              </div>
            </div>
            <button onClick={() => setShowUploadDialog(true)} className="px-4 py-2 bg-amber-600 text-white rounded-xl text-sm font-bold hover:bg-amber-700">
              Upload new PFX
            </button>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
      ) : (
        <div className="grid md:grid-cols-2 gap-4">
          {certs.map(cert => (
            <div key={cert.certId} className={cn("card p-5", cert.isActive && "border-l-4 border-l-emerald-500")}>
              <div className="flex items-start justify-between mb-3">
                <div>
                  <div className="font-bold text-slate-900">{cert.fileName}</div>
                  <div className="text-[11px] text-slate-500 mono mt-0.5">{cert.subject}</div>
                </div>
                <span className={cn("pill", cert.isActive ? "pill-ok" : "pill-neu")}>{cert.isActive ? "Active" : "Archived"}</span>
              </div>
              <div className="space-y-1.5 text-xs">
                <div className="flex justify-between"><span className="text-slate-500">Issuer</span><span className="font-semibold mono">{cert.issuer ?? "—"}</span></div>
                <div className="flex justify-between"><span className="text-slate-500">Valid from</span><span className="font-semibold mono">{cert.validFrom ? new Date(cert.validFrom).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" }) : "—"}</span></div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Valid until</span>
                  <span className={cn("font-semibold mono", cert.isActive && daysToExpiry !== null && daysToExpiry <= 30 && "text-amber-600")}>
                    {cert.validTo ? new Date(cert.validTo).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" }) : "—"}
                  </span>
                </div>
                <div className="flex justify-between"><span className="text-slate-500">Fingerprint (SHA-1)</span><span className="font-semibold mono text-[10px] truncate max-w-[180px]">{cert.thumbprint ?? "—"}</span></div>
              </div>
              {!cert.isActive && (
                <button onClick={() => activate(cert.certId)} className="mt-3 text-[11px] font-bold text-[#003ea8] hover:underline flex items-center gap-1">
                  <span className="material-symbols-outlined text-sm">verified</span>Activate
                </button>
              )}
            </div>
          ))}
          {certs.length === 0 && (
            <div className="card p-12 text-center col-span-2">
              <span className="material-symbols-outlined text-5xl text-slate-200">security</span>
              <div className="text-slate-400 font-semibold mt-3 text-sm">No certificates uploaded yet</div>
              <button onClick={() => setShowUploadDialog(true)} className="mt-3 text-[12px] font-bold text-[#003ea8] hover:underline">Upload your first PFX →</button>
            </div>
          )}
        </div>
      )}

      {/* Upload Dialog */}
      <Dialog open={showUploadDialog} onOpenChange={open => { setShowUploadDialog(open); if (!open) { setSelectedFile(null); setPfxPassword(""); setCertLabel(""); } }}>
        <DialogContent>
          <DialogHeader><DialogTitle>Upload PFX Certificate</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">PFX / P12 File</label>
              <div
                className={cn("border-2 border-dashed rounded-xl p-5 text-center cursor-pointer transition-colors", selectedFile ? "border-emerald-400 bg-emerald-50" : "border-slate-200 hover:border-[#497cff] hover:bg-blue-50/30")}
                onClick={() => fileInputRef.current?.click()}
              >
                <input ref={fileInputRef} type="file" accept=".pfx,.p12" className="hidden" onChange={e => setSelectedFile(e.target.files?.[0] ?? null)} />
                {selectedFile ? (
                  <div className="flex items-center justify-center gap-2 text-sm font-semibold text-emerald-700">
                    <span className="material-symbols-outlined">security</span>{selectedFile.name}
                  </div>
                ) : (
                  <>
                    <span className="material-symbols-outlined text-3xl text-slate-300">upload_file</span>
                    <div className="text-sm text-slate-500 mt-1">Click to select .pfx or .p12 file</div>
                  </>
                )}
              </div>
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Certificate password</label>
              <input
                type="password"
                value={pfxPassword}
                onChange={e => setPfxPassword(e.target.value)}
                placeholder="PFX export password"
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Label (optional)</label>
              <input
                type="text"
                value={certLabel}
                onChange={e => setCertLabel(e.target.value)}
                placeholder="e.g. geojit-signing-2026"
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm"
              />
              <p className="text-[11px] text-slate-400 mt-1">If blank, the filename is used as the label.</p>
            </div>
          </div>
          <DialogFooter>
            <button onClick={() => setShowUploadDialog(false)} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-semibold hover:bg-slate-50">Cancel</button>
            <button
              onClick={() => uploadCert()}
              disabled={uploading || !selectedFile || !pfxPassword}
              className="px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-bold hover:bg-[#003ea8] disabled:opacity-50 flex items-center gap-1.5"
            >
              {uploading ? <Loader2 className="animate-spin h-4 w-4" /> : <span className="material-symbols-outlined text-sm">upload</span>}
              {uploading ? "Uploading..." : "Upload & parse"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
