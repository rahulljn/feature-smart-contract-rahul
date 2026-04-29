"use client";

import React, { useState } from "react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

const USE_MOCK = true;
void USE_MOCK;

// ─── Types ────────────────────────────────────────────────────────────────────
interface ExceptionEntry {
  id: string;
  jobId: string;
  errorType: string;
  message: string;
  timestamp: string;
  stackTrace: string;
}

// ─── Mock data ────────────────────────────────────────────────────────────────
const MOCK_EXCEPTIONS: Record<string, ExceptionEntry[]> = {
  Split:        [],
  Invoke:       [],
  GetJson:      [],
  PDF: [
    {
      id: "pdf-1", jobId: "a1b2c3d4",
      errorType: "PDFGenerationException",
      message: "Timeout after 15s — payload exceeded Lambda limit. Party: ZYR175",
      timestamp: "2026-04-29 09:14:23",
      stackTrace: `com.geojit.pdf.PDFGenerationException: Timeout after 15s
\tat com.geojit.lambda.DynamicValuePdf.generatePDF(DynamicValuePdf.java:342)
\tat com.geojit.lambda.InvokeEquityCombineMarginFile.handleRequest(InvokeEquityCombineMarginFile.java:218)
Caused by: java.util.concurrent.TimeoutException: Task timed out after 15 seconds
\tat java.util.concurrent.ForkJoinPool.awaitQuiescence(ForkJoinPool.java:837)`,
    },
    {
      id: "pdf-2", jobId: "a1b2c3d4",
      errorType: "NullPointerException",
      message: "HeaderDto.partyCode is null — record type H missing for party ABX123",
      timestamp: "2026-04-29 09:22:11",
      stackTrace: `java.lang.NullPointerException: Cannot read field "partyCode"
\tat com.geojit.pdf.renderer.PDFHeaderRenderer.render(PDFHeaderRenderer.java:87)
\tat com.geojit.pdf.DynamicValuePdf.buildPage1(DynamicValuePdf.java:156)`,
    },
    {
      id: "pdf-3", jobId: "b2c3d4e5",
      errorType: "CertificateException",
      message: "PKCS#12 certificate could not be loaded — password mismatch or expired cert",
      timestamp: "2026-04-29 10:05:44",
      stackTrace: `java.security.cert.CertificateException: Could not load PKCS12 keystore
\tat com.geojit.pdf.signing.CertificateSigner.loadKeystore(CertificateSigner.java:44)
\tat com.geojit.pdf.signing.CertificateSigner.sign(CertificateSigner.java:92)`,
    },
  ],
  Email: [
    {
      id: "email-1", jobId: "a1b2c3d4",
      errorType: "SESThrottlingException",
      message: "Sending rate exceeded — 14 messages queued for retry",
      timestamp: "2026-04-29 09:45:02",
      stackTrace: `com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException: Throttling
\tat com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient.sendRawEmail(AmazonSimpleEmailServiceClient.java:1293)
\tat com.geojit.email.EmailNotificationHandler.sendEmail(EmailNotificationHandler.java:167)`,
    },
    {
      id: "email-2", jobId: "b2c3d4e5",
      errorType: "InvalidParameterException",
      message: "Invalid From address — SES identity not verified for noreply@geojit.com",
      timestamp: "2026-04-29 11:12:38",
      stackTrace: `com.amazonaws.services.simpleemail.model.InvalidParameterException: Invalid parameter
\tat com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient.sendEmail(AmazonSimpleEmailServiceClient.java:918)`,
    },
  ],
  "Pull Bounce":   [],
  "Pull Delivery": [],
};

const LAMBDA_TABS = ["Split", "Invoke", "GetJson", "PDF", "Email", "Pull Bounce", "Pull Delivery"];

export default function ExceptionsPage() {
  const [activeTab, setActiveTab]   = useState("PDF");
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const exceptions = MOCK_EXCEPTIONS[activeTab] ?? [];

  function copyTrace(trace: string) {
    navigator.clipboard.writeText(trace).then(() => toast.success("Copied to clipboard"));
  }

  return (
    <div className="space-y-5 fade-up">

      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="font-bold text-2xl text-gray-900">Exceptions</h1>
        <button className="flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors">
          <span className="material-symbols-outlined text-[16px]">refresh</span>
          Refresh
        </button>
      </div>

      {/* Lambda tabs */}
      <div className="flex gap-6 border-b border-gray-200 overflow-x-auto">
        {LAMBDA_TABS.map((tab) => {
          const count = (MOCK_EXCEPTIONS[tab] ?? []).length;
          return (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                "flex items-center gap-1.5 pb-2.5 text-sm font-medium transition-colors -mb-px whitespace-nowrap",
                activeTab === tab
                  ? "border-b-2 border-[#00174b] text-[#00174b]"
                  : "text-gray-500 hover:text-gray-900",
              )}
            >
              {tab}
              {count > 0 && (
                <span className="px-1.5 py-0.5 rounded-full bg-red-100 text-red-600 text-[10px] font-bold">
                  {count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Table / Empty state */}
      <div className="bg-white rounded-xl border overflow-hidden">
        {exceptions.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-2">
            <span className="material-symbols-outlined text-green-400 text-[36px]">check_circle</span>
            <p className="text-gray-500 text-sm">No exceptions for this Lambda</p>
          </div>
        ) : (
          <table className="w-full text-left">
            <thead>
              <tr className="t-hd">
                <th className="px-4 py-3">JOB ID</th>
                <th className="px-4 py-3">ERROR TYPE</th>
                <th className="px-4 py-3">MESSAGE</th>
                <th className="px-4 py-3">TIMESTAMP</th>
                <th className="px-4 py-3 w-8" />
              </tr>
            </thead>
            <tbody>
              {exceptions.map((ex) => (
                <React.Fragment key={ex.id}>
                  <tr
                    className={cn("t-row cursor-pointer", expandedId === ex.id && "bg-gray-50")}
                    onClick={() => setExpandedId(expandedId === ex.id ? null : ex.id)}
                  >
                    <td className="px-4 py-3 mono text-xs font-bold text-gray-700">{ex.jobId}</td>
                    <td className="px-4 py-3 text-red-500 font-medium text-xs">{ex.errorType}</td>
                    <td className="px-4 py-3 text-gray-600 text-xs max-w-[320px] truncate">{ex.message}</td>
                    <td className="px-4 py-3 text-xs text-gray-400 mono">{ex.timestamp}</td>
                    <td className="px-4 py-3">
                      <span className={cn(
                        "material-symbols-outlined text-gray-400 text-sm transition-transform duration-200",
                        expandedId === ex.id ? "rotate-90" : "",
                      )}>
                        chevron_right
                      </span>
                    </td>
                  </tr>
                  {expandedId === ex.id && (
                    <tr>
                      <td colSpan={5} className="p-0">
                        <div className="bg-gray-950 rounded-lg m-4 p-4">
                          <div className="flex justify-between items-center mb-2">
                            <span className="text-green-400 text-xs font-mono">Stack Trace</span>
                            <button
                              onClick={(e) => { e.stopPropagation(); copyTrace(ex.stackTrace); }}
                              className="text-xs text-gray-400 hover:text-white border border-gray-700 rounded px-2 py-0.5"
                            >
                              Copy
                            </button>
                          </div>
                          <pre className="text-green-400 text-xs font-mono overflow-x-auto whitespace-pre-wrap">
                            {ex.stackTrace}
                          </pre>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
