"use client";

import { useState, useMemo, useRef, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { clientsApi, bounceReportApi, useSegments } from "@/lib/api";
import type { BounceRecord, Segment } from "@/types";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { downloadCsv } from "@/lib/export";
import { Download, Search } from "lucide-react";
import { toast } from "sonner";
import Link from "next/link";

/** Normalises party codes that arrive as "ZYG063/ZYG063" → "ZYG063" */
const normaliseCode = (code: string) => code?.split("/")[0] ?? code;

export default function ClientsPage() {
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [segment, setSegment] = useState("");
  const [recentSearches, setRecentSearches] = useState<string[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [bounceRecords, setBounceRecords] = useState<BounceRecord[]>([]);
  const [bounceLoading, setBounceLoading] = useState(false);
  const [downloadingBounce, setDownloadingBounce] = useState<string | null>(null);
  const comboRef = useRef<HTMLDivElement>(null);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["client-search", query, fromDate, toDate, segment],
    queryFn: () => clientsApi.search(query, fromDate || undefined, toDate || undefined, segment || undefined),
    enabled: query.trim().length >= 2,
  });

  const { data: allCodesData } = useQuery({
    queryKey: ["client-codes"],
    queryFn: () => clientsApi.allCodes(),
    staleTime: 5 * 60 * 1000,
  });

  const { segments, loading: segmentsLoading } = useSegments();

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (comboRef.current && !comboRef.current.contains(e.target as Node)) {
        setShowDropdown(false);
      }
    }
    if (showDropdown) document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [showDropdown]);

  const allCodes: string[] = useMemo(() => {
    const raw = allCodesData?.data;
    return Array.isArray(raw) ? raw : [];
  }, [allCodesData]);

  const suggestions: string[] = useMemo(() => {
    if (!search.trim()) return allCodes.slice(0, 10);
    const lower = search.toLowerCase();
    return allCodes.filter(c => c.toLowerCase().includes(lower)).slice(0, 10);
  }, [search, allCodes]);

  const bounceBreakdown = useMemo(() => {
    const bounced = bounceRecords.filter(c => c.bounceType);
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

  const handleSearch = () => {
    if (search.trim().length < 2) { toast.warning("Enter at least 2 characters"); return; }
    setQuery(search);
    setRecentSearches(prev => [search, ...prev.filter(s => s !== search)].slice(0, 4));
    setBounceRecords([]);
    toast.dismiss();
  };

  const clearFilters = () => {
    setFromDate("");
    setToDate("");
    setSegment("");
    setBounceRecords([]);
  };

  const handleLoadBounceReport = async () => {
    if (search.trim().length < 2) {
      toast.warning("Enter a client code to load their bounce report");
      return;
    }

    setBounceLoading(true);
    try {
      const res = await bounceReportApi.getClientRecords(
        search.trim(),
        { from: fromDate || undefined, to: toDate || undefined, segment: segment || undefined }
      );
      setBounceRecords(res.data?.data ?? []);
    } catch {
      toast.error("Failed to load bounce records");
    } finally {
      setBounceLoading(false);
    }
  };

  const handleDownloadBounceReport = async () => {
    if (search.trim().length < 2) {
      toast.warning("Enter a client code to download their bounce report");
      return;
    }

    setDownloadingBounce(search.trim());

    try {
      const today = new Date();
      const weekAgo = new Date(today.getTime() - 7 * 86400000);
      const fromDateStr = weekAgo.toISOString().split('T')[0];
      const toDateStr = today.toISOString().split('T')[0];
      const params: Record<string, string> = {
        from: fromDateStr,
        to: toDateStr,
      };
      if (segment) params.segment = segment;

      downloadCsv(`BounceReport_${fromDateStr}_${toDateStr}.csv`,
        ["CLIENT CODE", "CLIENT NAME", "CLIENT EMAIL", "ACTIVITY DATE", "CONTRACT NO", "TRADE DATE", "BO TYPE", "BO REASON", "FILE NAME", "RECORD DATE"],
        bounceRecords.map(r => [r.partyCode, r.clientName, r.clientEmail, r.activityDate, r.contractNo, r.tradeDate, r.bounceType, r.bounceReason, r.fileName, r.recordDate])
      );
      toast.success("Bounce report downloaded");
    } catch {
      toast.error("Failed to download bounce report");
    } finally {
      setDownloadingBounce(null);
    }
  };

  const handleClientCodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearch(e.target.value.toUpperCase());
  };

  const handleSegmentChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSegment(e.target.value);
  };

  const clientProfile = data?.data?.data as { partyCode: string; clientDetails: Record<string, string>; processHistory: { email?: string }[]; totalContracts: number } | undefined;
  const clients = clientProfile ? [clientProfile] : [];

  return (
    <div className="space-y-5 fade-up">
      {/* Sticky header */}
      <div className="bg-white border-b border-slate-200 px-6 py-4 sticky top-0 z-10">
        <h2 className="text-xl font-extrabold text-slate-900 headline">Client 360</h2>
        <p className="text-slate-500 text-[12px] mt-0.5">
          Search by client code to view their full contract note delivery history and bounce reports.
        </p>
      </div>

      <div className="p-6 space-y-6">
        {/* Search card */}
        <div className="card p-5 space-y-4">
          <div className="flex gap-2">
            <div ref={comboRef} className="relative flex-1">
              <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 text-lg">search</span>
              <input
                value={search}
                onChange={handleClientCodeChange}
                onKeyDown={e => { if (e.key === "Enter") { setShowDropdown(false); handleSearch(); } if (e.key === "Escape") setShowDropdown(false); }}
                onFocus={() => setShowDropdown(true)}
                className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#497cff]/30"
                placeholder="Type or select a client code…"
                autoComplete="off"
              />
              {showDropdown && suggestions.length > 0 && (
                <ul className="absolute top-full left-0 right-0 mt-1 bg-white border border-slate-200 rounded-xl shadow-lg z-20 overflow-hidden">
                  {suggestions.map(code => (
                    <li
                      key={code}
                      onMouseDown={() => { setSearch(code); setQuery(code); setShowDropdown(false); setRecentSearches(prev => [code, ...prev.filter(s => s !== code)].slice(0, 4)); }}
                      className="px-4 py-2.5 text-sm mono text-slate-700 hover:bg-blue-50 hover:text-[#003ea8] cursor-pointer flex items-center gap-2"
                    >
                      <span className="material-symbols-outlined text-slate-400 text-base">person</span>
                      {code}
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <button
              onClick={() => { setShowDropdown(false); handleSearch(); }}
              className="px-5 py-3 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-2 flex-shrink-0"
            >
              <span className="material-symbols-outlined text-base">search</span>Search
            </button>
          </div>
          {recentSearches.length > 0 && (
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Recent:</span>
              {recentSearches.map(s => (
                <button
                  key={s}
                  onClick={() => { setSearch(s); setQuery(s); }}
                  className="px-2.5 py-1 bg-slate-100 hover:bg-blue-50 hover:text-[#003ea8] text-slate-600 rounded-lg text-[11px] font-semibold mono"
                >
                  {s}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Optional filters */}
        <div className="border-t border-slate-100 pt-4 grid grid-cols-2 md:grid-cols-4 gap-3 items-end">
          <div className="col-span-2 text-[10px] text-slate-400 -mb-1">
            Date range filters by job processing date (not trade date)
          </div>
          <div>
            <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1.5">Job Date From</label>
            <input type="date" value={fromDate} onChange={e => setFromDate(e.target.value)} className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1.5">Job Date To</label>
            <input type="date" value={toDate} onChange={e => setToDate(e.target.value)} className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest block mb-1.5">Segment</label>
            <select value={segment} onChange={handleSegmentChange} className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium">
              <option value="">All segments</option>
              {segmentsLoading ? <option disabled>Loading...</option> : segments.map((s: Segment) => (
                <option key={s.code} value={s.code}>{s.displayName}</option>
              ))}
            </select>
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleSearch}
              className="flex-1 px-3 py-2.5 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center justify-center gap-1.5"
            >
              <span className="material-symbols-outlined text-base">filter_alt</span>Apply
            </button>
            <button onClick={clearFilters} className="px-3 py-2.5 bg-white border border-slate-200 text-slate-500 rounded-xl text-sm hover:bg-slate-50 flex items-center justify-center">
              <span className="material-symbols-outlined text-sm">close</span>Clear
            </button>
          </div>
        </div>

        {/* Search results */}
        {!query ? (
          <div className="card p-12 text-center">
            <span className="material-symbols-outlined text-5xl text-slate-200">person_search</span>
            <div className="text-slate-400 font-semibold mt-3 text-sm">Search by client code</div>
            <div className="text-[11px] text-slate-300 mt-1">Try: 8000274</div>
          </div>
        ) : isFetching || isLoading ? (
          <div className="flex justify-center py-12"><Loader2 className="animate-spin text-[#00174b]" /></div>
        ) : (
          <div className="space-y-4">
            {clients.length === 0 ? (
              <div className="card p-12 text-center">
                <span className="material-symbols-outlined text-4xl text-slate-300">person_search</span>
                <div className="text-slate-400 font-semibold mt-3 text-sm">No client found for <span className="mono font-bold text-slate-700">{query}</span></div>
              </div>
            ) : (
              clients.map((c: any) => (
                <div key={c.partyCode} className="card p-5 flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-11 h-11 rounded-full bg-gradient-to-br from-[#00174b] to-[#003ea8] text-white flex items-center justify-center font-bold text-sm">
                      {c.partyCode.slice(0, 2).toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-bold text-slate-900">{c.clientDetails?.name ?? c.partyCode}</div>
                      <div className="text-[11px] text-slate-500 mono">{c.clientDetails?.email ?? c.processHistory?.[0]?.email ?? "—"}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="pill pill-ok">{c.totalContracts} contracts</span>
                    <Link href={`/clients/${c.partyCode}`} className="px-3 py-1.5 bg-[#00174b] text-white rounded-lg text-[11px] font-bold hover:bg-[#003ea8]">View profile</Link>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* Bounce breakdown — shows bounce breakdown from search results */}
        {bounceRecords.length > 0 && (
          <div className="card p-6">
            <div className="flex items-center gap-3 mb-5">
              <div className="p-2.5 bg-rose-50 rounded-xl"><span className="material-symbols-outlined text-rose-600">mail_off</span></div>
              <div>
                <div className="font-bold text-slate-900 headline">Bounce & Failure Digest</div>
                <div className="text-[11px] text-slate-500">
                  {bounceBreakdown.length > 0
                    ? `${bounceRecords.filter(c => c.bounceType).length} bounced deliveries recorded for ${search}`
                    : `No bounces recorded for ${search}`}
                </div>
              </div>
            </div>
            {bounceBreakdown.length > 0 ? (
              <div className="space-y-2.5">
                {bounceBreakdown.map(b => (
                  <div key={b.label}>
                    <div className="flex justify-between text-[12px] mb-1">
                      <span className="font-semibold text-slate-800">{b.label}</span>
                      <span className="mono text-slate-600 font-bold">{b.count}</span>
                    </div>
                    <div className="h-2 bg-slate-100 rounded-full">
                      <div className={cn("h-2 rounded-full", b.color)} style={{ width: `${b.pct}%` }} />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center text-slate-400 py-4 text-sm">All deliveries were successful — no bounces recorded</div>
            )}
          </div>
        )}

        {/* Bounce Report Download section */}
        <div className="card p-5 mt-6">
          <div>
            <div className="text-base font-semibold text-slate-900">Bounce Report Download</div>
            <div className="text-xs text-gray-400 mt-0.5">
              Download bounce logs from S3 by date range and segment
            </div>
          </div>
          <div className="flex items-end gap-3 mt-4 flex-wrap">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-widest block mb-1.5">CLIENT CODE</label>
              <input
                type="text"
                placeholder="e.g. ZYR175"
                value={search}
                onChange={handleClientCodeChange}
                className="border rounded-lg px-3 py-1.5 text-sm w-40 font-mono uppercase"
              />
              <p className="text-[10px] text-gray-400 mt-0.5">
                Required for bounce report download
              </p>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-widest block mb-1.5">FROM</label>
              <input
                type="date"
                value={fromDate}
                onChange={e => setFromDate(e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm"
              />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-widest block mb-1.5">TO</label>
              <input
                type="date"
                value={toDate}
                onChange={e => setToDate(e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm"
              />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-widest block mb-1.5">SEGMENT</label>
              <select
                value={segment}
                onChange={handleSegmentChange}
                className="border rounded-lg px-3 py-2 bg-white text-sm"
              >
                <option value="">All segments</option>
                {segmentsLoading ? <option disabled>Loading...</option> : segments.map((s: Segment) => (
                  <option key={s.code} value={s.code}>{s.displayName}</option>
                ))}
              </select>
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleLoadBounceReport}
                disabled={search.trim().length < 2 || bounceLoading}
                className="px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-bold hover:bg-[#003ea8] disabled:opacity-50 flex items-center gap-1.5"
              >
                {bounceLoading ? <Loader2 className="animate-spin h-3.5 w-3.5" /> : <Search className="h-3.5 w-4" />}
                Load Report
              </button>
              <button
                onClick={handleDownloadBounceReport}
                disabled={!!downloadingBounce || bounceRecords.length === 0}
                className="px-4 py-2 border border-[#00174b] text-[#00174b] rounded-lg text-sm font-bold hover:bg-[#00174b]/10 disabled:opacity-50 transition flex items-center gap-1.5"
              >
                {downloadingBounce ? <Loader2 className="animate-spin h-3.5 w-3.5" /> : <Download className="h-3.5 w-4" />}
                Download CSV
              </button>
            </div>
          </div>

          {fromDate && toDate && (toDate as any) - (fromDate as any) > 15 * 86400000 && (
            <p className="text-red-500 text-xs mt-2">
              ⚠ Date range cannot exceed 15 days
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
