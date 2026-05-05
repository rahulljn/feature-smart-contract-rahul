'use client';

import { useState } from 'react';
import { bounceReportApi, useSegments } from '@/lib/api';
import { Download, Search } from 'lucide-react';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';

export default function BounceReportSection({ partyCode }: { partyCode: string }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [segment, setSegment] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [records, setRecords] = useState<any[]>([]);

  const fetchRecords = async () => {
    setLoading(true);
    setError(null);
    try {
      const params: any = {};
      if (from) params.from = from;
      if (to) params.to = to;
      if (segment) params.segment = segment;

      const res = await bounceReportApi.getClientRecords(partyCode, params);
      setRecords(res.data.data || []);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to load bounce report: ' + (err?.message ?? 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = () => {
    if (records.length === 0) {
      toast.info('No records to download');
      return;
    }

    const headers = ['CLIENT', 'EMAIL', 'ACTIVITY DATE', 'CONTRACT NO', 'FILE NAME', 'BO TYPE', 'SEGMENT'];
    const rows = records.map(r => [
      r.partyCode || '',
      r.clientEmail || '',
      r.activityDate || '',
      r.contractNo || '',
      r.fileName || '',
      r.bounceType || '',
      r.bounceReason || ''
    ]);

    const csvContent = [headers.join(','), ...rows.map(row => row.map(cell => `"${cell}"`).join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `bounce-report-${partyCode}.csv`;
    document.body.appendChild(a);
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Download complete');
  };

  const { segments: segmentOptions } = useSegments();
  const segmentLabels = segmentOptions.map(s => ({ code: s.code, displayName: s.displayName }));

  return (
    <div className="space-y-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="font-bold text-xl text-slate-900">Bounce Report</h2>
        <p className="text-sm text-slate-500">{partyCode} — Client Bounce Analysis</p>
      </div>

      {/* Controls */}
      <div className="flex gap-2 mb-4">
        <button onClick={handleDownload} className="flex items-center gap-2 px-3 py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors">
          <Download className="h-4 w-4" />
          <span className="font-medium text-sm">Download CSV</span>
        </button>
        <div className="flex gap-2 bg-white rounded-lg p-1">
          <Search className="h-4 w-4" />
          <select
            value={segment}
            onChange={(e) => setSegment(e.target.value)}
            className="border border-slate-200 rounded-lg px-3 py-1.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-slate-900/20"
          >
            <option value="">All segments</option>
            {segmentOptions.map(s => (
              <option key={s.code} value={s.code}>{s.displayName}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-slate-100 p-4 mb-4">
        <div className="flex gap-3 mb-4">
          <div className="flex-1/2 items-center gap-2">
            <label className="text-sm font-medium text-slate-600">From</label>
            <input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="border border-slate-200 rounded-lg px-3 py-1.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-slate-900/20"
            />
          </div>
          <div className="flex-1/2 items-center gap-2">
            <label className="text-sm font-medium text-slate-600">To</label>
            <input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="border border-slate-200 rounded-lg px-3 py-1.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-slate-900/20"
            />
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex justify-end">
        <button
          onClick={() => fetchRecords()}
          className="flex items-center gap-2 px-4 py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors"
        >
          <Search className="h-4 w-4" />
          <span className="font-medium text-sm">Load Report</span>
        </button>
      </div>

      {/* Summary Card */}
      <div className="bg-white rounded-xl border border-slate-100 p-5">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900 mb-3">Summary</h3>
          <div className="text-sm text-slate-500">
            {records.length} records loaded
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
          <div className="flex items-center gap-2 p-3 bg-blue-50 rounded-lg">
            <div className="text-2xl font-bold text-slate-900">{records.length}</div>
            <div className="text-xs text-slate-600 font-medium">Total Records</div>
          </div>
          <div className="flex items-center gap-2 p-3 bg-amber-50 rounded-lg">
            <div className="text-2xl font-bold text-amber-900">{records.filter(r => r.bounceType === 'Permanent').length}</div>
            <div className="text-xs text-slate-600 font-medium">Permanent Bounces</div>
          </div>
          <div className="flex items-center gap-2 p-3 bg-red-50 rounded-lg">
            <div className="text-2xl font-bold text-red-900">{records.filter(r => r.bounceType === 'Complaint').length}</div>
            <div className="text-xs text-slate-600 font-medium">Complaints</div>
          </div>
          <div className="flex items-center gap-2 p-3 bg-purple-50 rounded-lg">
            <div className="text-2xl font-bold text-purple-900">{records.filter(r => r.bounceType === 'Transient').length}</div>
            <div className="text-xs text-slate-600 font-medium">Transient Bounces</div>
          </div>
        </div>
      </div>

      {/* Records Table */}
      {loading ? (
        <div className="bg-white rounded-xl border p-12 text-center">
          <Search className="animate-spin h-8 w-8 text-slate-400" />
          <p className="text-slate-600 mt-4">Loading bounce report data...</p>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 rounded-xl p-12 text-center">
          <span className="material-symbols-outlined text-4xl text-red-500">error</span>
          <p className="text-red-800 mt-2">{error}</p>
          <button
            onClick={() => fetchRecords()}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
          >
            Retry
          </button>
        </div>
      ) : records.length === 0 ? (
        <div className="bg-white rounded-xl border p-12 text-center">
          <span className="material-symbols-outlined text-4xl text-slate-400">inbox</span>
          <p className="text-slate-600 mt-2">No bounce records found for this client</p>
          <p className="text-slate-400 text-sm">Adjust date range or check for bounced deliveries</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl border overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">CLIENT</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">EMAIL</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">ACTIVITY DATE</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">CONTRACT NO</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">FILE NAME</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase">BO TYPE</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase">SEGMENT</th>
              </tr>
            </thead>
            <tbody>
              {records.map((record, idx) => (
                <tr key={record.partyCode + '-' + record.id || idx} className="hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-3 text-xs font-medium text-slate-600">{record.partyCode}</td>
                  <td className="px-4 py-3 text-xs text-slate-500 truncate max-w-[200px]">
                    {record.clientEmail}
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-500 truncate max-w-[300px]">
                    {record.activityDate}
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-500">
                    {record.contractNo}
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-400 truncate max-w-[160px]">
                    {record.fileName}
                  </td>
                  <td className={cn(
                    'px-4 py-3 text-xs font-medium',
                    record.bounceType === 'Permanent' && 'bg-red-100 text-red-700',
                    record.bounceType === 'Complaint' && 'bg-red-50 text-red-700',
                    record.bounceType === 'Transient' && 'bg-purple-100 text-purple-700',
                    !record.bounceType && 'text-slate-400'
                  )}>
                    {record.bounceType || '—'}
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-500 truncate max-w-[300px]">
                    {record.bounceReason}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
