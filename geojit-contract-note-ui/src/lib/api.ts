import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";

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
  pipelineStats: (jobId: string) => api.get(`/jobs/${jobId}/pipeline-stats`),
  exceptionCounts: (jobId: string) => api.get(`/jobs/${jobId}/exception-counts`),
  cloudwatchExceptions: (jobId: string, type = "ALL") =>
    api.get(`/jobs/${jobId}/cloudwatch-exceptions`, { params: { type } }),
  streamUrl: (jobId: string) => `${API_BASE}/jobs/${jobId}/stream`,
};

export const dashboardApi = {
  metrics: (from?: string, to?: string) =>
    api.get("/dashboard/metrics", { params: { from: from || undefined, to: to || undefined } }),
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

export const auditApi = {
  list: (page = 0, size = 50, search?: string, from?: string, to?: string, actions?: string) =>
    api.get("/audit", { params: { page, size, search, from, to, actions } }),
};

export const templatesApi = {
  list: () => api.get("/templates"),
  get: (id: string) => api.get(`/templates/${id}`),
  create: (data: object) => api.post("/templates", data),
  update: (id: string, data: object) => api.put(`/templates/${id}`, data),
  updateFields: (id: string, data: object) => api.put(`/templates/${id}/fields`, data),
  activate: (id: string) => api.post(`/templates/${id}/activate`),
  validate: (id: string) => api.post(`/templates/${id}/validate`),
};

export const configApi = {
  sesList: () => api.get("/config/ses"),
  sesCreate: (data: object) => api.post("/config/ses", data),
  sesUpdate: (id: string, data: object) => api.put(`/config/ses/${id}`, data),
  sesActivate: (id: string) => api.post(`/config/ses/${id}/activate`),
  certList: () => api.get("/config/certificates"),
  activeCert: () => api.get("/config/certificates/active"),
  certActivate: (id: string) => api.post(`/config/certificates/${id}/activate`),
  certUpload: (form: FormData) =>
    api.post("/config/certificates/upload", form, { headers: { "Content-Type": "multipart/form-data" } }),
  sesStatistics: (startDate?: string, endDate?: string) =>
    api.get("/config/ses/statistics", { params: { startDate: startDate || undefined, endDate: endDate || undefined } }),
};

export const usersApi = {
  list: (page = 0, size = 20) => api.get("/users", { params: { page, size } }),
  get: (id: string) => api.get(`/users/${id}`),
  create: (data: object) => api.post("/users", data),
  update: (id: string, data: object) => api.put(`/users/${id}`, data),
  deactivate: (id: string) => api.delete(`/users/${id}`),
};
