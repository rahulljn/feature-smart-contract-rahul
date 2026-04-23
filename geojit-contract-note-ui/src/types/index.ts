// ─── Auth ─────────────────────────────────────────────────────────────
export interface AuthUser {
  userId: string;
  email: string;
  name: string;
  role: "ADMIN" | "OPS_MANAGER" | "VIEWER";
  organisation?: "ACC" | "GEOJIT";
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

// ─── API Wrapper ──────────────────────────────────────────────────────
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ─── Jobs ─────────────────────────────────────────────────────────────
export type JobStatus =
  | "VALIDATING" | "SPLITTING" | "PROCESSING"
  | "EMAILING" | "COMPLETED" | "FAILED" | "PARTIAL";

export interface Job {
  jobId: string;
  fileName: string;
  rawS3Key: string;
  uploadedAt: string;
  status: JobStatus;
  segmentType: string;
  tradeDate?: string;
  uploadedBy?: { userId: string; name: string; email: string };
  totalRecords?: number;
  processedCount?: number;
  pdfGeneratedCount?: number;
  emailSentCount?: number;
  emailDeliveredCount?: number;
  emailFailedCount?: number;
  emailSkippedCount?: number;
  successCount?: number;
  bounceCount?: number;
  failureCount?: number;
  progressPercent?: number;
  templateId?: string;
}

// ─── Pipeline Stats ───────────────────────────────────────────────────
export interface PipelineStats {
  jobId: string;
  fileName: string;
  status: string;
  uploadCount: number;
  splitCount: number;
  pdfCount: number;
  pdfFailed: number;
  pdfPending: number;
  pdfRate: number;
  emailCount: number;
  emailBounced: number;
  emailFailed: number;
  emailPending: number;
  emailRate: number;
  deliveredCount: number;
  deliveredRate: number;
  medianSeconds: number;
  p95Seconds: number;
  errorRate: number;
}

export interface JobCustomer {
  id: number;
  partyCode: string;
  email: string;
  pdfStatus: "PENDING" | "GENERATED" | "FAILED";
  emailStatus: "PENDING" | "SENT" | "DELIVERED" | "FAILED" | "BOUNCED" | "SKIPPED";
  pdfGeneratedAt?: string;
  emailSentAt?: string;
  deliveredAt?: string;
  bouncedAt?: string;
  bounceType?: string;
  bounceReason?: string;
  sesMessageId?: string;
  pdfS3Key?: string;
  // Enriched fields returned by client search endpoint (joined with job data)
  jobId?: string;
  fileName?: string;
  tradeDate?: string;
  processedAt?: string;
  segment?: string;
}

// ─── Dashboard ────────────────────────────────────────────────────────
export interface DashboardMetrics {
  totalCustomersInPipeline: number;
  totalPdfsGenerated: number;
  totalEmailsSent: number;
  totalDelivered: number;
  totalBounced: number;
  bounceRate: number;
  deliveryRate: number;
  activeJobs: number;
  failedJobs: number;
  hourlyActivity: HourlyActivity[];
  recentActivity: RecentActivity[];
}

export interface HourlyActivity {
  hour: number;
  eventCount: number;
}

export interface RecentActivity {
  eventType: string;
  jobId: string;
  partyCode: string;
  description: string;
  eventTimestamp: string;
}

// ─── Pipeline Event ───────────────────────────────────────────────────
export type PipelineEventType =
  | "JOB_REGISTERED" | "CUSTOMER_REGISTERED"
  | "SPLIT_PROGRESS" | "SPLIT_COMPLETE" | "PDF_TRIGGERED" | "PDF_GENERATED" | "PDF_FAILED"
  | "EMAIL_SENT" | "EMAIL_FAILED" | "EMAIL_SKIPPED"
  | "DELIVERY" | "BOUNCE" | "COMPLAINT" | "RESEND_TRIGGERED";

export interface PipelineEvent {
  eventId: number;
  jobId: string;
  partyCode?: string;
  lambdaName: string;
  eventType: PipelineEventType;
  payload?: Record<string, unknown>;
  eventTimestamp: string;
}

// ─── Suppression ──────────────────────────────────────────────────────
export interface SuppressionEntry {
  id: number;
  email: string;
  clientCode?: string;
  reason?: string;
  addedBy?: string;
  addedAt: string;
}

// ─── Email Template ───────────────────────────────────────────────────
export interface EmailTemplate {
  templateId: string;
  name: string;
  subject: string;
  htmlBody: string;
  segment?: string;
  isActive: boolean;
  createdAt: string;
  lastEditedAt?: string;
  lastEditedBy?: { name: string; email: string };
  // Editable field columns (added V14)
  greetingText?: string;
  bodyIntro?: string;
  bodyColor?: string;
  footerColor?: string;
}

export interface TemplateFieldsRequest {
  subject: string;
  greetingText: string;
  bodyIntro: string;
  bodyColor: string;
  footerColor: string;
}

export interface TemplateValidateResult {
  valid: boolean;
  errors: string[];
  templateId: string;
  templateName: string;
}

// ─── Certificate ──────────────────────────────────────────────────────
export interface Certificate {
  certId: string;
  fileName: string;
  subject?: string;
  issuer?: string;
  validFrom?: string;
  validTo?: string;
  thumbprint?: string;
  secretName: string;
  s3Key?: string;
  isActive: boolean;
  createdAt: string;
  uploadedBy?: { name: string; email: string };
}

// ─── SES Config ───────────────────────────────────────────────────────
export interface SesConfig {
  id: string;
  configSetName: string;
  fromEmail: string;
  fromName?: string;
  domain?: string;
  region: string;
  isActive: boolean;
  createdAt: string;
}

// ─── Audit Log ────────────────────────────────────────────────────────
export type AuditAction =
  | "LOGIN" | "LOGOUT" | "UPLOAD" | "RESEND" | "BULK_RESEND"
  | "SUPPRESS" | "UNSUPPRESS" | "TEMPLATE_EDIT" | "CERT_UPLOAD"
  | "CONFIG_CHANGE" | "USER_CREATE" | "USER_UPDATE"
  | "USER_DEACTIVATE" | "VIEW_PDF" | "DOWNLOAD_REPORT";

export interface AuditLog {
  id: number;
  userId?: string;
  userEmail?: string;
  action: AuditAction;
  targetEntity?: string;
  details?: Record<string, unknown>;
  ipAddress?: string;
  userAgent?: string;
  eventTimestamp: string;
}

// ─── User ─────────────────────────────────────────────────────────────
export type UserRole = "ADMIN" | "OPS_MANAGER" | "VIEWER";
export type UserOrganisation = "ACC" | "GEOJIT";

export interface AppUser {
  userId: string;
  email: string;
  name: string;
  role: UserRole;
  organisation: UserOrganisation;
  isActive: boolean;
  lastLogin?: string;
  createdAt: string;
}

// ─── Email Event ──────────────────────────────────────────────────────
export interface EmailEvent {
  id: number;
  sesMessageId?: string;
  partyCode?: string;
  jobId?: string;
  eventType: "SEND" | "DELIVERY" | "BOUNCE" | "COMPLAINT";
  bounceType?: string;
  bounceSubType?: string;
  recipientEmail?: string;
  eventTimestamp: string;
}

// ─── Client ───────────────────────────────────────────────────────────
export interface ClientProfile {
  partyCode: string;
  clientDetails: Record<string, string>;
  processHistory: JobCustomer[];
  totalContracts: number;
}

// ─── Exception Counts ─────────────────────────────────────────────────
export interface ExceptionCounts {
  pdfFailed: number;
  emailFailed: number;
  bounced: number;
  skipped: number;
}

// ─── CloudWatch Exception ─────────────────────────────────────────────
export interface CloudWatchException {
  timestamp: string;
  logGroup: string;
  logStream: string;
  message: string;
}

export interface CloudWatchLambda {
  lambdaName: string;
  logGroup: string;
  errorCount: number;
  events: CloudWatchException[];
}

// ─── Validation ───────────────────────────────────────────────────────
export interface ValidationResult {
  totalCustomers: number;
  validCustomers: number;
  invalidCustomers: number;
  invalidReasons?: Record<string, number>; // e.g. { "H": 120, "E(got=6)": 45, "NO_HEADER": 10 }
}

// ─── SES Statistics ──────────────────────────────────────────────────
export interface SesDataPoint {
  timestamp: string;
  sends: number;
  bounces: number;
  complaints: number;
  rejects: number;
}

export interface SesStatistics {
  reputationScore: number;
  sendRate: number;
  bounceRate: number;
  complaintRate: number;
  dailySendQuota: number;
  sentLast24h: number;
  remainingSends: number;
  quotaUsedPercent: number;
  dataPoints: SesDataPoint[];
}
