"use client";

import { useState, useMemo } from "react";
import { bounceReportApi, useSegments } from "@/lib/api";
import type { BounceRecord, Segment } from "@/types";
import { Loader2, ChevronRight, Download, Search, Inbox } from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

function BounceBadge({ type }: { type: string }) {
  const map: Record<string, string> = {
    Permanent: "bg-red-100 text-red-700",
    Transient: "bg-amber-100 text-amber-700",
    Complaint: "bg-purple-100 text-purple-700",
  };
  return (
    <span className={cn("rounded-full px-2 py-0.5 text-xs font-medium", map[type] || "text-gray-400")}>
      {type || "—"}
    </span>
  );
}

export default function ClientsPage() {
  const [search, setSearch] = useState("");
  const [fromDate, setFromDate] = useState(
    new Date(Date.now() - 365 * 86400000).toISOString().split("T")[0]
  );
  const [toDate, setToDate] = useState(new Date().toISOString().split("T")[0]);
  const [segment, setSegment] = useState("");
  const [bounceRecords, setBounceRecords] = useState<BounceRecord[]>([]);
  const [bounceLoading, setBounceLoading] = useState(false);
  const [downloadingBounce, setDownloadingBounce] = useState(false);
  const [searched, setSearched] = useState(false);
  const [expandedRow, setExpandedRow] = useState<string | null>(null);

  const { segments, loading: segmentsLoading } = useSegments();

  const dateRangeValid = useMemo(() => {
    if (!fromDate || !toDate) return true;
    const diff = (new Date(toDate).getTime() - new Date(fromDate).getTime()) / 86400000;
    return diff >= 0;
  }, [fromDate, toDate]);

  const rowKey = (r: BounceRecord) => `${r.partyCode}_${r.fileName}`;

  const bounceBreakdown = useMemo(() => {
    const bounced = bounceRecords.filter((c) => c.bounceType);
    if (bounced.length === 0) return [];
    const grouped: Record<string, number> = {};
    for (const c of bounced) {
      const key = c.bounceType ?? "Unknown";
      grouped[key] = (grouped[key] || 0) + 1;
    }
    const total = bounced.length;
    const colors = ["bg-rose-500", "bg-orange-500", "bg-amber-400", "bg-amber-600", "bg-rose-300"];
    return Object.entries(grouped)
      .sort((a, b) => b[1] - a[1])
      .map(([label, count], i) => ({
        label,
        count,
        pct: Math.round((count / total) * 100),
        color: colors[i % colors.length],
      }));
  }, [bounceRecords]);

  const handleLoadBounceReport = async () => {
    if (!dateRangeValid) return;
    setBounceLoading(true);
    setSearched(false);
    try {
      if (search.trim().length >= 2) {
        const res = await bounceReportApi.getClientRecords(search.trim(), {
          from: fromDate || undefined,
          to: toDate || undefined,
          segment: segment || undefined,
        });
        setBounceRecords(res.data?.data ?? []);
      } else {
        const res = await bounceReportApi.list({
          from: fromDate || undefined,
          to: toDate || undefined,
          segment: segment || undefined,
        });
        setBounceRecords(res.data?.data?.records ?? []);
      }
      setSearched(true);
    } catch {
      toast.error("Failed to load bounce records");
    } finally {
      setBounceLoading(false);
    }
  };

  const handleDownloadBounceReport = async () => {
    setDownloadingBounce(true);
    try {
      const res = await bounceReportApi.downloadAll({
        from: fromDate || undefined,
        to: toDate || undefined,
        segment: segment || undefined,
      });
      const url = URL.createObjectURL(new Blob([res.data as BlobPart], { type: "text/csv" }));
      const a = document.createElement("a");
      a.href = url;
      a.download = `BounceReport_${fromDate || "all"}_${toDate || "all"}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success("Bounce report downloaded");
    } catch {
      toast.error("Failed to download bounce report");
    } finally {
      setDownloadingBounce(false);
    }
  };

  return (
    <div className="space-y-5 fade-up">
      {/* Sticky header */}
      <div className="bg-white border-b border-slate-200 px-6 py-4 sticky top-0 z-10">
        <h2 className="text-xl font-extrabold text-slate-900 headline">Client 360</h2>
        <p className="text-slate-500 text-[12px] mt-0.5">
          Search bounce logs from S3 by date range, client code, and segment.
        </p>
      </div>

      <div className="p-6 space-y-6">
        {/* Bounce Report Download section */}
        <div className="card p-5">
          <div>
            <div className="text-base font-semibold text-slate-900">Bounce Report</div>
            <div className="text-xs text-gray-400 mt-0.5">
              Load bounce logs from S3 by date range and segment
            </div>
          </div>
          <div className="flex items-end gap-3 mt-4 flex-wrap">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-widest block mb-1.5">
                CLIENT CODE
              </label>
              <input
                type="text"
                placeholder="e.g. ZYR175"
                value={search}
                onChange={(e) => setSearch(e.target.value.toUpperCase())}
                onKeyDown={(e) => { if (e.key === "Enter") handleLoadBounceReport(); }}
                className="border rounded-lg px-3 py-1.5 text-sm w-40 font-mono uppercase"
              />
              <p className="text-[10px] text-gray-400 mt-0.5">
                Optional — leave blank to search all clients
              </p>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-widest block mb-1.5">
                TRADE DATE FROM
              </label>
              <input
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm"
              />
              <p className="text-xs text-gray-400 italic mt-1">Enter trade date range, not today's date</p>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-widest block mb-1.5">
                TRADE DATE TO
              </label>
              <input
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm"
              />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-widest block mb-1.5">
                SEGMENT
              </label>
              <select
                value={segment}
                onChange={(e) => setSegment(e.target.value)}
                className="border rounded-lg px-3 py-2 bg-white text-sm"
              >
                <option value="">All Segments</option>
                {segmentsLoading ? (
                  <option disabled>Loading...</option>
                ) : (
                  segments.map((s: Segment) => (
                    <option key={s.code} value={s.code}>
                      {s.displayName}
                    </option>
                  ))
                )}
              </select>
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleLoadBounceReport}
                disabled={bounceLoading || !dateRangeValid}
                className="px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-bold hover:bg-[#003ea8] disabled:opacity-50 flex items-center gap-1.5"
              >
                {bounceLoading ? (
                  <Loader2 className="animate-spin h-3.5 w-3.5" />
                ) : (
                  <Search className="h-3.5 w-4" />
                )}
                Load Report
              </button>
              <button
                onClick={handleDownloadBounceReport}
                disabled={downloadingBounce || !dateRangeValid}
                className="px-4 py-2 border border-[#00174b] text-[#00174b] rounded-lg text-sm font-bold hover:bg-[#00174b]/10 disabled:opacity-50 transition flex items-center gap-1.5"
              >
                {downloadingBounce ? (
                  <Loader2 className="animate-spin h-3.5 w-3.5" />
                ) : (
                  <Download className="h-3.5 w-4" />
                )}
                Download CSV
              </button>
            </div>
          </div>

          {!dateRangeValid && (
            <p className="text-red-500 text-xs mt-2">⚠ "To" date must be on or after "From" date</p>
          )}
        </div>

        {/* Bounce breakdown card — shown after loading */}
        {bounceRecords.length > 0 && (
          <div className="card p-6">
            <div className="flex items-center gap-3 mb-5">
              <div className="p-2.5 bg-rose-50 rounded-xl">
                <span className="material-symbols-outlined text-rose-600">mail_off</span>
              </div>
              <div>
                <div className="font-bold text-slate-900 headline">Bounce & Failure Digest</div>
                <div className="text-[11px] text-slate-500">
                  {bounceBreakdown.length > 0
                    ? `${bounceRecords.filter((c) => c.bounceType).length} bounced deliveries recorded${search ? ` for ${search}` : ""}`
                    : `No bounces recorded${search ? ` for ${search}` : ""}`}
                </div>
              </div>
            </div>
            {bounceBreakdown.length > 0 ? (
              <div className="space-y-2.5">
                {bounceBreakdown.map((b) => (
                  <div key={b.label}>
                    <div className="flex justify-between text-[12px] mb-1">
                      <span className="font-semibold text-slate-800">{b.label}</span>
                      <span className="mono text-slate-600 font-bold">{b.count}</span>
                    </div>
                    <div className="h-2 bg-slate-100 rounded-full">
                      <div
                        className={cn("h-2 rounded-full", b.color)}
                        style={{ width: `${b.pct}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center text-slate-400 py-4 text-sm">
                All deliveries were successful — no bounces recorded
              </div>
            )}
          </div>
        )}

        {/* Results table */}
        {(searched || bounceRecords.length > 0) && (
          <div className="card rounded-xl border overflow-hidden">
            <div className="px-4 py-3 border-b border-slate-100 bg-slate-50 flex items-center justify-between">
              <p className="text-sm text-slate-500">
                {search
                  ? `Results for ${search} — ${bounceRecords.length} record(s)`
                  : `${bounceRecords.length} bounce records found`}
              </p>
            </div>
            <table className="w-full">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-xs uppercase text-slate-500">
                  <th className="px-4 py-3 text-left">CLIENT</th>
                  <th className="px-4 py-3 text-left">EMAIL</th>
                  <th className="px-4 py-3 text-left">TRADE DATE</th>
                  <th className="px-4 py-3 text-left">BO TYPE</th>
                  <th className="px-4 py-3 text-left">FILE</th>
                  <th className="w-10 px-3 py-3">›</th>
                </tr>
              </thead>
              <tbody>
                {bounceLoading ? (
                  [...Array(4)].map((_, i) => (
                    <tr key={i}>
                      <td colSpan={6} className="py-4">
                        <div className="flex justify-center">
                          <Loader2 className="animate-spin text-slate-400 h-5 w-5" />
                        </div>
                      </td>
                    </tr>
                  ))
                ) : bounceRecords.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-12">
                      <div className="flex flex-col items-center gap-3 text-slate-400">
                        <Inbox className="h-10 w-10" />
                        <div className="text-center">
                          <p className="text-slate-500 font-semibold">No bounce records found</p>
                          <p className="text-sm text-slate-400">
                            Try adjusting the date range, client code, or segment
                          </p>
                        </div>
                      </div>
                    </td>
                  </tr>
                ) : (
                  bounceRecords.map((record) => (
                    <>
                      <tr
                        key={rowKey(record)}
                        className="border-b border-slate-100 hover:bg-slate-50 transition-colors"
                      >
                        <td className="px-4 py-3 font-mono text-sm text-[#497cff]">
                          {record.partyCode}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-600 truncate max-w-[260px]">
                          {record.clientEmail}
                        </td>
                        <td className="px-4 py-3 text-sm">{record.tradeDate}</td>
                        <td className="px-4 py-3">
                          <BounceBadge type={record.bounceType} />
                        </td>
                        <td className="px-4 py-3 text-xs text-slate-400 truncate max-w-[160px]">
                          {record.fileName}
                        </td>
                        <td className="px-3 py-3 text-center">
                          <button
                            onClick={() =>
                              setExpandedRow(expandedRow === rowKey(record) ? null : rowKey(record))
                            }
                            className="text-slate-400 hover:text-slate-600"
                          >
                            <ChevronRight
                              className={cn(
                                "h-5 w-5 transition-transform",
                                expandedRow === rowKey(record) && "rotate-90"
                              )}
                            />
                          </button>
                        </td>
                      </tr>

                      {expandedRow === rowKey(record) && (
                        <tr key={`${rowKey(record)}-expanded`}>
                          <td colSpan={6} className="p-0">
                            <div className="bg-blue-50/30 px-6 py-4 border-b border-blue-100">
                              <div className="grid grid-cols-3 gap-x-8 gap-y-3 text-sm">
                                <div>
                                  <p className="text-xs text-slate-400 mb-0.5">CLIENT NAME</p>
                                  <p className="font-medium">{record.clientName || "—"}</p>
                                </div>
                                <div>
                                  <p className="text-xs text-slate-400 mb-0.5">ACTIVITY DATE</p>
                                  <p>{record.activityDate || "—"}</p>
                                </div>
                                <div>
                                  <p className="text-xs text-slate-400 mb-0.5">CONTRACT NO</p>
                                  <p className="font-mono">{record.contractNo || "—"}</p>
                                </div>
                                <div>
                                  <p className="text-xs text-slate-400 mb-0.5">BO REASON</p>
                                  <p>{record.bounceReason || "—"}</p>
                                </div>
                                <div>
                                  <p className="text-xs text-slate-400 mb-0.5">SEGMENT</p>
                                  <p>{record.segment?.toUpperCase() || "—"}</p>
                                </div>
                                <div>
                                  <p className="text-xs text-slate-400 mb-0.5">S3 FILE</p>
                                  <p
                                    className="font-mono text-xs text-slate-400 truncate"
                                    title={record.s3Key}
                                  >
                                    {record.s3Key?.split("/").pop() || "—"}
                                  </p>
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
