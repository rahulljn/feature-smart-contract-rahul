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
  invalidRecordCount?: number;
  pdfFailedCount?: number;
  hardBounceCount?: number;
  softBounceCount?: number;
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
  totalFailedRecords: number;
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
  logoUrl?: string;
}

export interface S3Template {
  name: string;
  status: 'active' | 'draft';
  s3Key: string;
  lastModified: string;
  size: number;
}

export interface TemplateFieldsRequest {
  subject: string;
  greetingText: string;
  bodyIntro: string;
  logoUrl?: string;
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
export interface SecretsCert {
  secretName: string;
  description: string | null;
  createdDate: string | null;
  lastChangedDate: string | null;
  isActive: boolean;
}

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
  invalidRecords: number;
  hardBounced: number;
  softBounced: number;
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

// ─── Lambda Exception ─────────────────────────────────────────────────
export interface LambdaException {
  id: number;
  jobId: string;
  lambdaName: string;
  recordId?: string;
  errorType?: string;
  errorMessage?: string;
  stackTrace?: string;
  occurredAt: string;
}

// ─── Validation ───────────────────────────────────────────────────────
export interface ValidationResult {
  totalCustomers: number;
  validCustomers: number;
  invalidCustomers: number;
  invalidReasons?: Record<string, number>; // e.g. { "H": 120, "E(got=6)": 45, "NO_HEADER": 10 }
}

// ─── Alerts ───────────────────────────────────────────────────────────
export type AlertTriggerEvent =
  | "PFX_EXPIRY_WARNING" | "PFX_EXPIRY_CRITICAL" | "JOB_FAILED"
  | "HIGH_BOUNCE_RATE" | "PIPELINE_STUCK" | "EMAIL_QUOTA_NEAR_LIMIT"
  | "SES_CONFIG_DEACTIVATED";

export type AlertChannel = "EMAIL" | "SMS" | "WHATSAPP";

export interface AlertRule {
  id: string;
  name: string;
  triggerEvent: AlertTriggerEvent;
  thresholdValue?: number;
  channels: AlertChannel[];
  recipients: { channel: AlertChannel; value: string }[];
  includeDeepLink: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AlertNotification {
  id: string;
  ruleId: string;
  ruleName: string;
  triggeredBy: string;
  channel: AlertChannel;
  recipient: string;
  status: "SENT" | "FAILED" | "PENDING";
  sentAt: string;
  jobId?: string;
}

// ─── Dashboard Charts ─────────────────────────────────────────────────
export interface MonthlyVolumeData {
  month: string;
  delivered: number;
  bounced: number;
  failed: number;
}

export interface DailySuccessRate {
  date: string;
  rate: number;
}

export interface JobBounceBreakdown {
  jobName: string;
  hard: number;
  soft: number;
  complaint: number;
}

// ─── Ops Dashboard ────────────────────────────────────────────────────
export interface OpsTodaySummary {
  total: number;
  completed: number;
  inProgress: number;
  failed: number;
}

export interface OpsHealthStatus {
  sesQuotaUsed: number;
  sesQuotaMax: number;
  activeSesConfig: { name: string; fromEmail: string } | null;
  pfxDaysRemaining: number;
  lambdaErrorsLastHour: number;
  jobsStuckCount: number;
  highBounceJobs: { jobId: string; fileName: string; bounceRate: number }[];
  failedCustomersToday: number;
  invalidRecordsToday: number;
}

// ─── Email Analytics ──────────────────────────────────────────────────
export interface EngagementSummary {
  totalSent: number;
  openRate: number;
  clickRate: number;
  unsubscribeRate: number;
  openRateTrend: number;
  clickRateTrend: number;
  sparklineOpens: number[];
  sparklineClicks: number[];
}

export interface EngagementTimeSeries {
  date: string;
  openRate: number;
  clickRate: number;
  bounceRate: number;
}

export interface SegmentEngagement {
  segment: string;
  openRate: number;
  clickRate: number;
  sent: number;
}

export interface HourHeatmapCell {
  day: number;
  hour: number;
  clickRate: number;
}

export interface CustomerEngagement {
  partyCode: string;
  email: string;
  totalSent: number;
  opened: number;
  clicked: number;
  openRate: number;
  clickRate: number;
  lastOpenedAt?: string;
  lastClickedAt?: string;
  bounceStatus: "NEVER" | "SOFT" | "HARD";
}

// ─── Branding & CMS ───────────────────────────────────────────────────
export interface BrandingConfig {
  appName: string;
  primaryColor: string;
  accentColor: string;
  logoUrl: string | null;
  faviconUrl: string | null;
  updatedAt: string;
}

export interface CmsPage {
  id: string;
  slug: "privacy-policy" | "about-us";
  title: string;
  content: string;
  lastUpdatedAt: string;
  lastUpdatedBy: string;
}

// ─── Template Test ────────────────────────────────────────────────────
export interface TemplateTestResult {
  id: string;
  templateId: string;
  sentTo: string[];
  status: "SENT" | "FAILED";
  errorMessage?: string;
  partyCodeUsed?: string;
  sentByName: string;
  sentAt: string;
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

// ─── Alert History ────────────────────────────────────────────────────
export type AlertSeverity = "CRITICAL" | "WARNING" | "INFO";

export interface AlertHistoryItem {
  id: string;
  title: string;
  message: string;
  severity: AlertSeverity;
  isRead: boolean;
  createdAt: string;
  jobId?: string;
  certName?: string;
}

// ─── Alert Channel Configuration ─────────────────────────────────────
export interface EmailChannelConfig {
  enabled: boolean;
  sesConfigSetName?: string;
  fromAddress?: string;
}

export interface SmsChannelConfig {
  enabled: boolean;
  provider?: string;
  accountSid?: string;
  authToken?: string;
  fromNumber?: string;
}

export interface WhatsAppChannelConfig {
  enabled: boolean;
  provider?: string;
  accountSid?: string;
  authToken?: string;
  fromNumber?: string;
}

export interface ChannelConfig {
  email: EmailChannelConfig;
  sms: SmsChannelConfig;
  whatsApp: WhatsAppChannelConfig;
}

// ─── Ops Dashboard ────────────────────────────────────────────────────
export interface OpsIssue {
  id: string;
  name: string;
  severity: "HIGH" | "MEDIUM" | "LOW";
  count: number;
  description: string;
}

export interface OpsHourlyFailure {
  hour: number;
  appFailures: number;
  infraFailures: number;
}

export interface OpsFailureDistribution {
  name: string;
  value: number;
  color: string;
}

export interface OpsTodayJob {
  jobId: string;
  segment: string;
  records: number;
  status: string;
  duration?: string;
  pdfFail: number;
  emailFail: number;
  delivered: number;
}
