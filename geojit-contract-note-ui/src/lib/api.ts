import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type {
  ApiResponse, Segment, BounceRecord, BounceListResponse, ResendBounceResult,
  JobCustomer, Job, DashboardMetrics, PipelineEvent, JobStatus, PageResponse,
  EmailTemplate, S3Template, TemplateFieldsRequest, TemplateValidateResult,
  Certificate, SesConfig, AppUser, EmailEvent, ClientProfile,
  ExceptionCounts, CloudWatchLambda, LambdaException, ValidationResult, AuditLog
} from "@/types";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export const api = axios.create({
  baseURL: API_BASE,
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

// ── Request interceptor: attach JWT token ────────────────────────────
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (typeof window !== "undefined") {
    const raw = localStorage.getItem("auth-storage");
    if (raw) {
      try {
        const state = JSON.parse(raw);
        const token: string | undefined = state?.state?.user?.accessToken;
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      } catch {
        // ignore parse errors
      }
    }
  }
  return config;
});

// ── Response interceptor: handle 401 globally ────────────────────────
api.interceptors.response.use(
  (res) => res,
  (error: AxiosError) => {
    if (error.response?.status === 401 && typeof window !== "undefined") {
      localStorage.removeItem("auth-storage");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

// ─── API helpers ────────────────────────────────────────────────────

export const authApi = {
  login: (email: string, password: string) =>
    api.post("/auth/login", { email, password }),
  logout: () => api.post("/auth/logout"),
  me: () => api.get("/auth/me"),
};

export const jobsApi = {
  list: (page = 0, size = 20, status?: string, from?: string, to?: string) =>
    api.get("/jobs", { params: { page, size, status: status || undefined, from: from || undefined, to: to || undefined } }),
  get: (jobId: string) => api.get(`/jobs/${jobId}`),
  customers: (jobId: string, page = 0, size = 50) =>
    api.get(`/jobs/${jobId}/customers`, { params: { page, size } }),
  exceptions: (jobId: string, type = "ALL", page = 0, size = 50) =>
    api.get(`/jobs/${jobId}/exceptions`, { params: { type, page, size } }),
  upload: (form: FormData) =>
    api.post("/jobs/upload", form, { headers: { "Content-Type": "multipart/form-data" } }),
  validate: (form: FormData) =>
    api.post("/jobs/validate", form, { headers: { "Content-Type": "multipart/form-data" }, timeout: 0 }),
  resend: (jobId: string, partyCode: string, overrideEmail?: string, templateId?: string) =>
    api.post(`/jobs/${jobId}/customers/${partyCode}/resend`,
      (overrideEmail || templateId) ? { overrideEmail, templateId } : undefined),
  bulkResend: (jobId: string, templateId?: string) =>
    api.post(`/jobs/${jobId}/bulk-resend`, templateId ? { templateId } : undefined),
  bulkResendCodes: (jobId: string, partyCodes: string[], templateId?: string) =>
    api.post(`/jobs/${jobId}/bulk-resend-codes`, { partyCodes, ...(templateId ? { templateId } : {}) }),
  bulkResendAllBounced: (templateId?: string) =>
    api.post("/jobs/bulk-resend-all-bounced", templateId ? { templateId } : undefined),
  failedCustomers: (page = 0, size = 50, statuses?: string, jobId?: string, from?: string, to?: string) =>
    api.get("/jobs/failed-customers", {
      params: { page, size,
        statuses: statuses || undefined,
        jobId: jobId || undefined,
        from: from || undefined,
        to: to || undefined,
      },
    }),
  pipelineStats: (jobId: string) => api.get(`/jobs/${jobId}/pipeline-stats`),
  exceptionCounts: (jobId: string) => api.get(`/jobs/${jobId}/exception-counts`),
  cloudwatchExceptions: (jobId: string, type = "ALL") =>
    api.get(`/jobs/${jobId}/cloudwatch-exceptions`, { params: { type } }),
  streamUrl: (jobId: string) => `${API_BASE}/jobs/${jobId}/stream`,
};

export const lambdaExceptionsApi = {
  list: (jobId?: string, lambdaName?: string) =>
    api.get("/exceptions", { params: { jobId: jobId || undefined, lambdaName: lambdaName || undefined } }),
};

const localDateStr = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

export const dashboardApi = {
  metrics: (from?: Date, to?: Date) =>
    api.get("/dashboard/metrics", { params: { from: from ? localDateStr(from) : undefined, to: to ? localDateStr(to) : undefined } }),
  getMonthlyVolume: (year: number) =>
    api.get("/dashboard/monthly-volume", { params: { year } }),
  getDailySuccessRate: (days: 30 | 90) =>
    api.get("/dashboard/daily-success-rate", { params: { days } }),
  getBounceBreakdown: () =>
    api.get("/dashboard/bounce-breakdown"),
};

export const pipelineApi = {
  events: (jobId: string) => api.get("/pipeline/events", { params: { jobId } }),
};

export const clientsApi = {
  allCodes: () =>
    api.get<string[]>("/clients/codes"),
  suggest: (q: string) =>
    api.get<string[]>("/clients/suggest", { params: { q } }),
  search: (q: string, fromDate?: string, toDate?: string, segment?: string) =>
    api.get("/clients/search", { params: { q, fromDate: fromDate || undefined, toDate: toDate || undefined, segment: segment || undefined } }),
  pdfs: (partyCode: string) => api.get(`/clients/${partyCode}/pdfs`),
  reports: (partyCode: string) => api.get(`/clients/${partyCode}/reports`),
  timeline: (partyCode: string) => api.get(`/clients/${partyCode}/timeline`),
  pdfUrl: (partyCode: string, s3Key: string) =>
    api.get(`/clients/${partyCode}/pdf-url`, { params: { key: s3Key } }),
  updateEmail: (partyCode: string, newEmail: string) =>
    api.put(`/clients/${partyCode}/email`, { newEmail }),
};

export const suppressionApi = {
  list: (page = 0, size = 20) => api.get("/suppression", { params: { page, size } }),
  add: (email: string, reason?: string) => api.post("/suppression", { email, reason }),
  remove: (email: string) => api.delete(`/suppression/${encodeURIComponent(email)}`),
  syncFromAws: () => api.post("/suppression/sync-aws"),
  pushToAws: () => api.post("/suppression/push-aws"),
};

export const templatesApi = {
  list: () => api.get("/templates"),
  get: (id: string) => api.get(`/templates/${id}`),
  getActive: () => api.get("/templates/active"),
  content: (name: string) => api.get(`/templates/${name}/content`),
  saveContent: (name: string, content: string) =>
    api.put(`/templates/${name}/content`, { content }),
  saveFields: (name: string, data: object) =>
    api.put(`/templates/${name}/fields`, data),
  create: (data: object) => api.post("/templates", data),
  update: (id: string, data: object) => api.put(`/templates/${id}`, data),
  updateFields: (id: string, data: object) => api.put(`/templates/${id}/fields`, data),
  activate: (id: string) => api.post(`/templates/${id}/activate`),
  validate: (id: string) => api.post(`/templates/${id}/validate`),
  sendTestEmail: (templateId: string, data: { recipients: string[]; partyCode?: string }) =>
    api.post(`/templates/${templateId}/test`, data),
  getTestHistory: (templateId: string) =>
    api.get(`/templates/${templateId}/test-history`),
};

export const configApi = {
  sesList: () => api.get("/config/ses"),
  sesCreate: (data: object) => api.post("/config/ses", data),
  sesUpdate: (id: string, data: object) => api.put(`/config/ses/${id}`, data),
  sesActivate: (id: string) => api.post(`/config/ses/${id}/activate`),
  certList: () => api.get("/config/certificates"),
  activeCert: () => api.get("/config/certificates/active"),
  certActivate: (secretName: string) =>
    api.post(`/config/certificates/activate?secretName=${encodeURIComponent(secretName)}`),
  certUpload: (form: FormData) =>
    api.post("/config/certificates/upload", form, { headers: { "Content-Type": "multipart/form-data" } }),
  sesStatistics: (startDate?: string, endDate?: string) =>
    api.get("/config/ses/statistics", { params: { startDate: startDate || undefined, endDate: endDate || undefined } }),
  getActiveSesConfig: () => api.get("/config/ses/active"),
  getBranding: () => api.get("/config/branding"),
  updateBranding: (data: object) => api.put("/config/branding", data),
  uploadLogo: (file: File) => {
    const fd = new FormData(); fd.append("file", file);
    return api.post("/config/branding/logo", fd, { headers: { "Content-Type": "multipart/form-data" } });
  },
  getCmsPages: () => api.get("/config/cms"),
  updateCmsPage: (slug: string, data: { title: string; content: string }) =>
    api.put(`/config/cms/${slug}`, data),
};

export const usersApi = {
  list: (page = 0, size = 20) => api.get("/users", { params: { page, size } }),
  get: (id: string) => api.get(`/users/${id}`),
  create: (data: object) => api.post("/users", data),
  update: (id: string, data: object) => api.put(`/users/${id}`, data),
  deactivate: (id: string) => api.delete(`/users/${id}`),
};

export const auditApi = {
  list: (page = 0, size = 20, search?: string, from?: string, to?: string, actions?: string) =>
    api.get<PageResponse<AuditLog>>("/audit", { params: { page, size, search, from, to, actions } }),
};

export const alertsApi = {
  getRules: () => api.get("/alerts/rules"),
  createRule: (data: object) => api.post("/alerts/rules", data),
  updateRule: (id: string, data: object) => api.put(`/alerts/rules/${id}`, data),
  deleteRule: (id: string) => api.delete(`/alerts/rules/${id}`),
  toggleRule: (id: string, isActive: boolean) =>
    api.patch(`/alerts/rules/${id}/toggle`, { isActive }),
  getHistory: (params: {
    from?: string; to?: string; channel?: string;
    status?: string; page?: number; size?: number;
  }) => api.get("/alerts/history", { params }),
  resendNotification: (id: string) => api.post(`/alerts/history/${id}/resend`),
  getUnreadCount: () => api.get("/alerts/unread-count"),
  markAllRead: () => api.post("/alerts/mark-all-read"),
  getChannelConfig: () => api.get("/alerts/channels"),
  updateChannelConfig: (data: object) => api.put("/alerts/channels", data),
};

export const opsApi = {
  getTodaySummary: () => api.get("/ops/today-summary"),
  getTodayJobs: () => api.get("/ops/today-jobs"),
  getHealthStatus: () => api.get("/ops/health"),
  getHourlyFailures: () => api.get("/ops/hourly-failures"),
  getFailureDistribution: () => api.get("/ops/failure-distribution"),
  getIssues: () => api.get("/ops/issues"),
};

export const segmentsApi = {
  list: () => api.get<ApiResponse<Segment[]>>("/segments"),
};

export const bounceReportApi = {
  list: (params: { from?: string; to?: string; segment?: string }) =>
    api.get<ApiResponse<BounceListResponse>>("/bounce-report", { params }),

  getClientRecords: (partyCode: string,
                     params: { from?: string; to?: string; segment?: string }) =>
    api.get<ApiResponse<BounceRecord[]>>(
      `/bounce-report/client/${encodeURIComponent(partyCode)}`, { params }),

  getDownloadUrl: (partyCode: string, s3Key: string) =>
    api.get<ApiResponse<{ url: string; expiresInSeconds: number }>>(
      `/bounce-report/client/${encodeURIComponent(partyCode)}/download-url`,
      { params: { s3Key } }),

  downloadAll: (params: { from?: string; to?: string; segment?: string }) =>
    api.get("/bounce-report/download-all", { params, responseType: "blob" }),

  resend: (partyCode: string, fileName: string) =>
    api.post<ApiResponse<ResendBounceResult>>(
      "/bounce-report/resend", { partyCode, fileName }),

  resendBulk: (records: { partyCode: string; fileName: string }[]) =>
    api.post<ApiResponse<{
      queued: number; failed: number; results: ResendBounceResult[]
    }>>("/bounce-report/resend-bulk", { records }),
};

export const analyticsApi = {
  getEngagementSummary: (params: object) =>
    api.get("/analytics/engagement/summary", { params }),
  getEngagementTimeSeries: (params: object) =>
    api.get("/analytics/engagement/timeseries", { params }),
  getSegmentEngagement: (params: object) =>
    api.get("/analytics/engagement/by-segment", { params }),
  getHourHeatmap: (params: object) =>
    api.get("/analytics/engagement/heatmap", { params }),
  getCustomerEngagement: (params: object) =>
    api.get("/analytics/engagement/customers", { params }),
};

// ─── React hooks ───────────────────────────────────────────────────────────
import { useQuery } from "@tanstack/react-query";

export function useSegments() {
  const { data, isLoading } = useQuery({
    queryKey: ["segments"],
    queryFn: async () => {
      const res = await segmentsApi.list();
      return res.data.data as Segment[];
    },
    staleTime: 10 * 60 * 1000, // 10 minutes
  });

  return {
    segments: data ?? [],
    loading: isLoading,
  };
}
