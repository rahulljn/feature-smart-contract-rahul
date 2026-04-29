"use client";

import { useState, useRef } from "react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

const USE_MOCK = true;
void USE_MOCK;

// ─── Mock data ────────────────────────────────────────────────────────────────
const MOCK_CERTS = [
  {
    id: "1", name: "GEOJIT-PROD-2025", active: true,  daysRemaining: 23,
    subject: "CN=Geojit Financial Services Ltd", issuer: "CN=DigiCert SHA2 Secure CA",
    validFrom: "2025-01-15", validTo: "2026-05-22", thumbprint: "A3:B2:C1:D0:E9:F8",
    algorithm: "SHA-256 with RSA", keySize: 2048, serial: "04:A1:2B:3C",
  },
  {
    id: "2", name: "GEOJIT-DR-2025",   active: false, daysRemaining: 48,
    subject: "CN=Geojit Financial Services DR", issuer: "CN=DigiCert SHA2 Secure CA",
    validFrom: "2025-03-01", validTo: "2026-06-17", thumbprint: "B4:C3:D2:E1:F0:A9",
    algorithm: "SHA-256 with RSA", keySize: 2048, serial: "04:B2:3C:4D",
  },
  {
    id: "3", name: "GEOJIT-DEV-2026",  active: false, daysRemaining: 248,
    subject: "CN=Geojit Financial Services Dev", issuer: "CN=Let's Encrypt Authority X3",
    validFrom: "2026-01-01", validTo: "2027-01-01", thumbprint: "C5:D4:E3:F2:A1:B0",
    algorithm: "SHA-256 with RSA", keySize: 4096, serial: "04:C3:4D:5E",
  },
];

function daysBadge(days: number) {
  if (days <= 30)  return "bg-red-100 text-red-700";
  if (days <= 60)  return "bg-amber-100 text-amber-700";
  return "bg-green-100 text-green-700";
}

function daysBarColor(days: number) {
  if (days <= 30)  return "bg-red-500";
  if (days <= 60)  return "bg-amber-400";
  return "bg-green-500";
}

interface Cert {
  id: string; name: string; active: boolean; daysRemaining: number;
  subject: string; issuer: string; validFrom: string; validTo: string;
  thumbprint: string; algorithm: string; keySize: number; serial: string;
}

function CertCard({ cert, onActivate }: { cert: Cert; onActivate: (id: string) => void }) {
  const [expanded, setExpanded] = useState(false);
  const barPct = Math.min(100, Math.round((cert.daysRemaining / 365) * 100));

  return (
    <div className={cn(
      "bg-white rounded-xl border p-4 flex flex-col gap-3",
      cert.active && "ring-2 ring-[#00174b]/20",
    )}>
      {/* Title row */}
      <div className="flex items-center justify-between gap-2">
        <div className="font-semibold text-gray-900 text-sm truncate">{cert.name}</div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <span className={cn(
            "px-2 py-0.5 rounded-full text-[10px] font-bold",
            cert.active ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500",
          )}>
            {cert.active ? "Active" : "Inactive"}
          </span>
          <span className={cn("px-2 py-0.5 rounded-full text-[10px] font-bold", daysBadge(cert.daysRemaining))}>
            {cert.daysRemaining}d
          </span>
        </div>
      </div>

      {/* Progress bar */}
      <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
        <div
          className={cn("h-full rounded-full", daysBarColor(cert.daysRemaining))}
          style={{ width: `${barPct}%` }}
        />
      </div>

      {/* Details */}
      <div className="space-y-1.5 text-xs">
        {[
          { label: "Days to expiry", value: `${cert.daysRemaining} days` },
          { label: "Subject",        value: cert.subject },
          { label: "Issuer",         value: cert.issuer },
          { label: "Thumbprint",     value: cert.thumbprint },
          { label: "Valid To",       value: cert.validTo },
          { label: "Algorithm",      value: cert.algorithm },
        ].map(({ label, value }) => (
          <div key={label} className="flex items-start justify-between gap-2">
            <span className="text-gray-400 flex-shrink-0">{label}</span>
            <span className="text-gray-700 font-medium text-right truncate">{value}</span>
          </div>
        ))}
      </div>

      {/* Expandable extra */}
      {expanded && (
        <div className="space-y-1.5 text-xs border-t border-gray-100 pt-2">
          {[
            { label: "Valid From", value: cert.validFrom },
            { label: "Key Size",   value: `${cert.keySize} bits` },
            { label: "Serial No.", value: cert.serial },
          ].map(({ label, value }) => (
            <div key={label} className="flex items-start justify-between gap-2">
              <span className="text-gray-400 flex-shrink-0">{label}</span>
              <span className="text-gray-700 font-medium text-right">{value}</span>
            </div>
          ))}
        </div>
      )}

      {/* Footer actions */}
      <div className="flex items-center gap-2 pt-1 border-t border-gray-100">
        <button
          onClick={() => setExpanded((v) => !v)}
          className="text-xs text-[#497cff] hover:underline"
        >
          {expanded ? "Show less" : "Show more"}
        </button>
        {!cert.active && (
          <button
            onClick={() => onActivate(cert.id)}
            className="ml-auto text-xs px-2.5 py-1 bg-[#00174b] text-white rounded-lg hover:bg-[#003ea8] transition-colors"
          >
            Activate
          </button>
        )}
      </div>
    </div>
  );
}

export default function CertificatesPage() {
  const [certs, setCerts]           = useState(MOCK_CERTS);
  const [uploading, setUploading]   = useState(false);
  const [label, setLabel]           = useState("");
  const [password, setPassword]     = useState("");
  const [dragOver, setDragOver]     = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const expiringCerts = certs.filter((c) => c.daysRemaining <= 30);

  function handleActivate(id: string) {
    setCerts((prev) =>
      prev.map((c) => ({ ...c, active: c.id === id })),
    );
    toast.success("Certificate activated");
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) setSelectedFile(file);
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) setSelectedFile(file);
  }

  async function handleUpload() {
    if (!selectedFile || !label.trim() || !password.trim()) {
      toast.error("Please fill all fields and select a .pfx file");
      return;
    }
    setUploading(true);
    await new Promise((r) => setTimeout(r, 1200));
    toast.success(`Certificate "${label}" uploaded successfully`);
    setUploading(false);
    setSelectedFile(null);
    setLabel("");
    setPassword("");
  }

  return (
    <div className="space-y-5 fade-up">

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="font-bold text-2xl text-gray-900">Certificates</h1>
        <button
          onClick={() => fileRef.current?.click()}
          className="flex items-center gap-1.5 px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors"
        >
          <span className="material-symbols-outlined text-[18px]">upload</span>
          Upload Certificate
        </button>
      </div>

      {/* Expiry banner */}
      {expiringCerts.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-red-500 text-[18px]">warning</span>
            <span className="text-sm text-red-800 font-medium">
              {expiringCerts[0].name} expires in {expiringCerts[0].daysRemaining} days
            </span>
          </div>
          <button
            onClick={() => toast.info("Alert sent to administrators")}
            className="text-sm px-3 py-1.5 border border-red-300 text-red-700 rounded-lg hover:bg-red-100 transition-colors"
          >
            Send Alert Now
          </button>
        </div>
      )}

      {/* Cert cards */}
      <div className="grid md:grid-cols-3 gap-4">
        {certs.map((cert) => (
          <CertCard key={cert.id} cert={cert} onActivate={handleActivate} />
        ))}
      </div>

      {/* Upload panel */}
      <div className="bg-white rounded-xl border p-5">
        <div className="text-base font-semibold text-gray-900 mb-4">Upload New Certificate</div>
        <div className="grid md:grid-cols-2 gap-6">
          {/* Drop zone */}
          <div
            className={cn(
              "border-2 border-dashed rounded-xl p-8 flex flex-col items-center justify-center gap-3 cursor-pointer transition-colors",
              dragOver ? "border-[#497cff] bg-blue-50" : "border-gray-300 hover:border-gray-400",
            )}
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            onClick={() => fileRef.current?.click()}
          >
            <span className="material-symbols-outlined text-gray-400 text-[36px]">
              {selectedFile ? "task_alt" : "upload_file"}
            </span>
            {selectedFile ? (
              <div className="text-center">
                <div className="text-sm font-medium text-gray-700">{selectedFile.name}</div>
                <div className="text-xs text-gray-400 mt-0.5">{(selectedFile.size / 1024).toFixed(1)} KB</div>
              </div>
            ) : (
              <div className="text-center">
                <div className="text-sm text-gray-600">Drop .pfx file here</div>
                <div className="text-xs text-gray-400 mt-0.5">or click to browse</div>
              </div>
            )}
            <input
              ref={fileRef}
              type="file"
              accept=".pfx,.p12"
              className="hidden"
              onChange={handleFileChange}
            />
          </div>

          {/* Inputs */}
          <div className="flex flex-col gap-4 justify-center">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-700">Certificate Label</label>
              <input
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder="e.g. GEOJIT-PROD-2026"
                className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-700">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="PFX password"
                className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#00174b]/20"
              />
            </div>
            <button
              onClick={handleUpload}
              disabled={uploading}
              className="flex items-center justify-center gap-1.5 px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-medium hover:bg-[#003ea8] transition-colors disabled:opacity-60"
            >
              {uploading ? (
                <>
                  <span className="material-symbols-outlined text-[16px] animate-spin">refresh</span>
                  Uploading…
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined text-[16px]">upload</span>
                  Upload Certificate
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
