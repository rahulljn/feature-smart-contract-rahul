"use client";

import React, { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { bounceReportApi, useSegments } from "@/lib/api";
import type { BounceRecord, Segment } from "@/types";
import { cn, fmtDate } from "@/lib/utils";
import { downloadCsv } from "@/lib/export";
import { toast } from "sonner";
import { ArrowUpDown, ChevronRight, RefreshCw, Check, Download, Search, Inbox, Send } from "lucide-react";

function BounceBadge({ type }: { type: string }) {
  const map: Record<string, string> = {
    'Permanent': 'bg-red-100 text-red-700',
    'Transient': 'bg-amber-100 text-amber-700',
    'Complaint': 'bg-purple-100 text-purple-700',
  };
  const className = map[type] || 'text-gray-400';
  return (
    <span className={cn("rounded-full px-2 py-0.5 text-xs font-medium", className)}>
      {type || '—'}
    </span>
  );
}

function ResendPageContent() {
  const qc = useQueryClient();
  const { segments } = useSegments();

  const [from, setFrom] = useState(
    new Date(Date.now() - 365 * 86400000).toISOString().split('T')[0]);
  const [to, setTo] = useState(new Date().toISOString().split('T')[0]);
  const [segmentCode, setSegmentCode] = useState('');
  const [clientCodeSearch, setClientCodeSearch] = useState('');
  const [records, setRecords] = useState<BounceRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedRows, setSelectedRows] = useState<Set<string>>(new Set());
  const [expandedRow, setExpandedRow] = useState<string | null>(null);
  const [resendingId, setResendingId] = useState<string | null>(null);
  const [resentIds, setResentIds] = useState<Set<string>>(new Set());
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const dateRangeValid = useMemo(() => {
    const diff = (new Date(to).getTime() - new Date(from).getTime()) / 86400000;
    return diff >= 0;
  }, [from, to]);

  const rowKey = useCallback((r: BounceRecord) => `${r.partyCode}_${r.fileName}`, []);

  const paginatedRecords = useMemo(() => {
    const start = (page - 1) * pageSize;
    return records.slice(start, start + pageSize);
  }, [records, page, pageSize]);

  const totalPages = Math.max(1, Math.ceil(records.length / pageSize));

  const bounceTypeLabel: Record<string, string> = {
    'Permanent': 'bg-red-100 text-red-700',
    'Transient': 'bg-amber-100 text-amber-700',
    'Complaint': 'bg-purple-100 text-purple-700',
  };

  const handleSearch = async () => {
    if (!dateRangeValid) return;
    setLoading(true);
    setError(null);
    try {
      if (clientCodeSearch.trim()) {
        const res = await bounceReportApi.getClientRecords(clientCodeSearch.trim(), { from, to, segment: segmentCode || undefined });
        setRecords(res.data?.data ?? []);
      } else {
        const res = await bounceReportApi.list({ from, to, segment: segmentCode || undefined });
        setRecords(res.data?.data?.records ?? []);
      }
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to load bounce records');
      toast.error('Failed to load bounce records');
    } finally {
      setLoading(false);
    }
  };

  const resendMutation = useMutation({
    mutationFn: async ({ partyCode, fileName }: { partyCode: string; fileName: string }) => {
      return bounceReportApi.resend(partyCode, fileName);
    },
    onSuccess: () => {
      toast.success('Resend queued successfully');
    },
    onError: () => {
      toast.error('Resend failed');
    },
  });

  const resendBulkMutation = useMutation({
    mutationFn: async (recordsToResend: { partyCode: string; fileName: string }[]) => {
      return bounceReportApi.resendBulk(recordsToResend);
    },
    onSuccess: (data: any) => {
      toast.success(`Resend queued: ${data?.data?.data?.queued ?? 0} records`);
    },
    onError: () => {
      toast.error('Bulk resend failed');
    },
  });

  const resendSingle = async (record: BounceRecord) => {
    const rk = rowKey(record);
    setResendingId(rk);
    try {
      await resendMutation.mutateAsync({ partyCode: record.partyCode, fileName: record.fileName });
      setResentIds(prev => new Set([...prev, rk]));
      toast.success(`Resend queued for ${record.partyCode}`);
    } catch {
      toast.error(`Resend failed for ${record.partyCode}`);
    } finally {
      setResendingId(null);
    }
  };

  const resendBulk = async () => {
    if (selectedRows.size === 0) return;
    const recordsToResend = Array.from(selectedRows)
      .map(key => {
        const [partyCode, ...rest] = key.split('_');
        const fileName = rest.join('_');
        return { partyCode, fileName };
      })
      .filter(r => r.partyCode && r.fileName);

    try {
      await resendBulkMutation.mutateAsync(recordsToResend);
      setResentIds(prev => new Set([...prev, ...selectedRows]));
      setSelectedRows(new Set());
    } catch {
      toast.error('Bulk resend failed');
    }
  };

  const resendingBulk = resendBulkMutation.isPending;

  return (
    <>
      {/* Sticky header */}
      <div className="bg-white border-b border-slate-200 px-6 py-4 sticky top-0 z-10">
        <h1 className="text-2xl font-bold text-slate-900">Bulk Resend</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Resend bounced emails — sourced from S3 bounce logs
        </p>
      </div>

      <div className="p-6 space-y-5">
        {/* Filter card */}
        <div className="card p-4 mb-5">
          <div className="flex items-end gap-3 flex-wrap">
            {/* Client Code */}
            <div>
              <label className="text-xs text-slate-500 mb-1 font-bold">CLIENT CODE</label>
              <input
                type="text"
                placeholder="e.g. ZYR175 (optional)"
                value={clientCodeSearch}
                onChange={e => setClientCodeSearch(e.target.value.toUpperCase())}
                className="border rounded-lg px-3 py-1.5 text-sm w-40 font-mono" />
              <p className="text-[10px] text-gray-400 mt-0.5">
                Leave blank to search all clients
              </p>
            </div>

            {/* From date */}
            <div>
              <label className="text-xs text-slate-500 mb-1 font-bold">TRADE DATE FROM</label>
              <input
                type="date"
                value={from}
                onChange={e => setFrom(e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm"
              />
              <p className="text-xs text-gray-400 italic mt-1">Enter trade date range, not today's date</p>
            </div>

            {/* To date */}
            <div>
              <label className="text-xs text-slate-500 mb-1 font-bold">TRADE DATE TO</label>
              <input
                type="date"
                value={to}
                onChange={e => setTo(e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm"
              />
            </div>

            {/* Segment dropdown */}
            <div>
              <label className="text-xs text-slate-500 mb-1 font-bold">SEGMENT</label>
              <select
                value={segmentCode}
                onChange={e => setSegmentCode(e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm bg-white"
              >
                <option value="">All Segments</option>
                {segments.map((s: Segment) => (
                  <option key={s.code} value={s.code}>{s.displayName}</option>
                ))}
              </select>
            </div>

            {/* Search button */}
            <button
              onClick={handleSearch}
              disabled={!dateRangeValid || loading}
              className="bg-[#00174b] text-white rounded-lg px-4 py-2 text-sm flex items-center gap-2 hover:bg-[#003ea8] disabled:opacity-50"
            >
              {loading ? <RefreshCw className="animate-spin h-4 w-4" /> : <Search className="h-4 w-4" />}
              Search
            </button>
          </div>

          {!dateRangeValid && (
            <p className="text-red-500 text-xs mt-2">⚠ "To" date must be on or after "From" date</p>
          )}
        </div>

        {/* Results bar */}
        <div className="flex items-center justify-between flex-wrap gap-2 px-4 py-3 border-t border-slate-100 bg-white rounded-t-xl">
          <p className="text-sm text-slate-500">
            {clientCodeSearch
              ? `Results for ${clientCodeSearch} — ${records.length} record(s)`
              : `${records.length} bounce records found`}
          </p>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={selectedRows.size === paginatedRecords.length && paginatedRecords.length > 0}
              onChange={e => {
                if (e.target.checked) {
                  setSelectedRows(new Set(paginatedRecords.map(rowKey)));
                } else {
                  setSelectedRows(new Set());
                }
              }}
            />
            <label className="text-sm">Select all on page</label>
            <button
              onClick={resendBulk}
              disabled={selectedRows.size === 0 || resendingBulk}
              className="bg-[#00174b] text-white rounded-lg px-3 py-1.5 text-sm flex items-center gap-2 hover:bg-[#003ea8] disabled:opacity-50"
            >
              {resendingBulk ? <RefreshCw className="animate-spin h-3.5 w-3.5" /> : <Send className="h-4 w-4" />}
              Resend Selected ({selectedRows.size})
            </button>
            <button
              onClick={() => {
                const today = new Date();
                const weekAgo = new Date(today.getTime() - 7 * 86400000);
                const fromDateStr = weekAgo.toISOString().split('T')[0];
                const toDateStr = today.toISOString().split('T')[0];
                downloadCsv(`BounceReport_${fromDateStr}_${toDateStr}.csv`,
                  ["CLIENT CODE", "CLIENT NAME", "CLIENT EMAIL", "TRADE DATE", "BO TYPE", "BO REASON", "FILE NAME", "RECORD DATE"],
                  records.map(r => [r.partyCode, r.clientName, r.clientEmail, r.tradeDate, r.bounceType, r.bounceReason, r.fileName, r.recordDate]));
              }}
              className="border border-[#00174b] text-[#00174b] rounded-lg px-3 py-1.5 text-sm hover:bg-[#00174b]/10"
            >
              <Download className="h-4 w-4" />
              Download CSV
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="card rounded-xl border overflow-hidden mt-0">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs uppercase text-slate-500">
                <th className="w-10 px-3 py-3 text-center">□</th>
                <th className="px-4 py-3 text-left">CLIENT</th>
                <th className="px-4 py-3 text-left">EMAIL</th>
                <th className="px-4 py-3 text-left">TRADE DATE</th>
                <th className="px-4 py-3 text-left">BO TYPE</th>
                <th className="px-4 py-3 text-left">FILE</th>
                <th className="px-4 py-3 text-left">ACTIONS</th>
                <th className="w-10 px-3 py-3">›</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i}>
                    <td colSpan={8} className="py-4">
                      <div className="flex justify-center">
                        <RefreshCw className="animate-spin text-slate-400 h-5 w-5" />
                      </div>
                    </td>
                  </tr>
                ))
              ) : records.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12">
                    <div className="flex flex-col items-center gap-3 text-slate-400">
                      <Inbox className="h-12 w-12" />
                      <div className="text-center">
                        <p className="text-slate-500 font-semibold">No bounce records found</p>
                        <p className="text-sm text-slate-400">
                          Adjust date range, client code, or segment filter
                        </p>
                      </div>
                    </div>
                  </td>
                </tr>
              ) : paginatedRecords.map(record => (
                <React.Fragment key={rowKey(record)}>
                  <tr className={cn(
                    "border-b border-slate-100 hover:bg-slate-50 transition-colors",
                    selectedRows.has(rowKey(record)) && "bg-blue-50/40"
                  )}>
                    <td className="px-3 py-3 text-center">
                      <input
                        type="checkbox"
                        checked={selectedRows.has(rowKey(record))}
                        onChange={() => {
                          const ns = new Set(selectedRows);
                          ns.has(rowKey(record)) ? ns.delete(rowKey(record)) : ns.add(rowKey(record));
                          setSelectedRows(ns);
                        }}
                      />
                    </td>
                    <td className="px-4 py-3 font-mono text-sm text-[#497cff]">
                      {record.partyCode}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-600 truncate max-w-[300px]">
                      {record.clientEmail}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      {record.tradeDate}
                    </td>
                    <td className="px-4 py-3">
                      <BounceBadge type={record.bounceType} />
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-400 truncate max-w-[160px]">
                      {record.fileName}
                    </td>
                    <td className="px-4 py-3">
                      {(() => {
                        const rk = rowKey(record);
                        if (resentIds.has(rk)) {
                          return (
                            <span className="text-green-600 text-xs font-medium flex items-center gap-1">
                              <Check className="h-3.5 w-3.5" />
                              Queued
                            </span>
                          );
                        }
                        if (resendingId === rk) {
                          return (
                            <span className="text-xs text-gray-400 flex items-center gap-1">
                              <RefreshCw className="animate-spin h-3 w-3" />
                              Sending…
                            </span>
                          );
                        }
                        return (
                          <button
                            onClick={() => resendSingle(record)}
                            className="border border-[#00174b] text-[#00174b] rounded px-2 py-1 text-xs hover:bg-[#00174b] hover:text-white transition"
                          >
                            Resend
                          </button>
                        );
                      })()}
                    </td>
                    <td className="px-3 py-3 text-center">
                      <button
                        onClick={() => setExpandedRow(expandedRow === rowKey(record) ? null : rowKey(record))}
                        className="text-slate-400 hover:text-slate-600"
                      >
                        <ChevronRight className={cn("h-5 w-5 transition-transform", expandedRow === rowKey(record) && "rotate-90")} />
                      </button>
                    </td>
                  </tr>

                  {expandedRow === rowKey(record) && (
                    <tr>
                      <td colSpan={8} className="p-0">
                        <div className="bg-blue-50/30 px-6 py-4 border-b border-blue-100">
                          <div className="grid grid-cols-3 gap-x-8 gap-y-3 text-sm">
                            <div>
                              <p className="text-xs text-slate-400 mb-0.5">CLIENT NAME</p>
                              <p className="font-medium">{record.clientName || '—'}</p>
                            </div>
                            <div>
                              <p className="text-xs text-slate-400 mb-0.5">ACTIVITY DATE</p>
                              <p>{record.activityDate || '—'}</p>
                            </div>
                            <div>
                              <p className="text-xs text-slate-400 mb-0.5">CONTRACT NO</p>
                              <p className="font-mono">{record.contractNo || '—'}</p>
                            </div>
                            <div>
                              <p className="text-xs text-slate-400 mb-0.5">BO REASON</p>
                              <p>{record.bounceReason || '—'}</p>
                            </div>
                            <div>
                              <p className="text-xs text-slate-400 mb-0.5">SEGMENT</p>
                              <p>{record.segment?.toUpperCase() || '—'}</p>
                            </div>
                            <div>
                              <p className="text-xs text-slate-400 mb-0.5">S3 FILE</p>
                              <p className="font-mono text-xs text-slate-400 truncate" title={record.s3Key}>
                                {record.s3Key.split('/').pop()}
                              </p>
                            </div>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="border-t border-slate-100 p-3 flex justify-between items-center">
          <div className="flex items-center gap-2">
            <span className="text-sm">Rows per page:</span>
            <select
              value={pageSize}
              onChange={e => { setPageSize(Number(e.target.value)); setPage(1); }}
              className="border rounded px-2 py-1 text-sm"
            >
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
            </select>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={page === 1}
              className="px-3 py-1 border rounded text-sm hover:bg-slate-50 disabled:opacity-40"
            >
              Prev
            </button>
            <span className="text-sm text-slate-500">
              Page {page} of {totalPages}
            </span>
            <button
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={page >= totalPages}
              className="px-3 py-1 border rounded text-sm hover:bg-slate-50 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </>
  );
}

export default function ResendPage() {
  return (
    <>
      <ResendPageContent />
    </>
  );
}
