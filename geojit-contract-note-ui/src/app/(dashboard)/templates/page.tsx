"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { templatesApi } from "@/lib/api";
import type { S3Template } from "@/types";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

export default function TemplatesPage() {
  const [selectedName, setSelectedName] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["templates"],
    queryFn: () => templatesApi.list(),
    select: (res) => {
      const raw = res?.data?.data;
      const list: S3Template[] = Array.isArray(raw) ? raw : [];
      return list.sort((a) => (a.status === "active" ? -1 : 1));
    },
  });

  const { data: contentData, isLoading: contentLoading } = useQuery({
    queryKey: ["template-content", selectedName],
    queryFn: () => templatesApi.content(selectedName!),
    enabled: !!selectedName,
    select: (res) => res?.data?.data as string,
  });

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
        {!data || data.length === 0 ? (
          <div className="p-10 text-center text-slate-400 text-sm">
            No templates found in S3 bucket.
          </div>
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
              {data.map(t => (
                <tr key={t.s3Key} className="hover:bg-slate-50/60 transition-colors">
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
                    <button
                      onClick={() => setSelectedName(selectedName === t.name ? null : t.name)}
                      className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-[12px] font-semibold hover:bg-[#003ea8] flex items-center gap-1.5 ml-auto"
                    >
                      <span className="material-symbols-outlined text-sm">
                        {selectedName === t.name ? "expand_less" : "code"}
                      </span>
                      {selectedName === t.name ? "Close" : "View HTML"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* HTML Viewer */}
      {selectedName && (
        <div className="card p-0 overflow-hidden">
          <div className="px-5 py-3 bg-slate-50 border-b border-slate-100 flex items-center gap-2">
            <span className="material-symbols-outlined text-slate-400 text-sm">code</span>
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest flex-1">
              HTML Source — {selectedName}.html
            </span>
            <button
              onClick={() => setSelectedName(null)}
              className="text-slate-400 hover:text-slate-600 transition-colors"
            >
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

    </div>
  );
}
