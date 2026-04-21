# Session Summary — Lambda-API Bridge + Scale Fixes + UI Analysis

**Date:** 2026-04-16
**Project:** Geojit Contract Note Processing System (3 components)

---

## System Overview

| Component | Project | Stack | Purpose |
|-----------|---------|-------|---------|
| Lambda Pipeline | `geojit-contract-notes` | Java, AWS Lambda, S3, SQS, SES | File split, PDF generation, email delivery |
| Backend API | `geojit-contract-note-api` | Spring Boot, PostgreSQL, Flyway | REST API serving UI |
| Frontend UI | `geojit-contract-note-ui` | Next.js, React Query, Tailwind | Dashboard, jobs, clients, audit, config |

**Core problem solved:** Lambda pipeline and API were disconnected. Lambda wrote to S3, API expected PostgreSQL data. UI got empty responses.

**Solution:** Added a thin status-event SQS layer. Each Lambda sends a small JSON event to a new SQS queue after processing. A new StatusConsumer Lambda reads events and calls the API.

---

## Phase 1: Lambda Pipeline Changes (COMPLETED)

Each Lambda modified to: (a) read/pass jobId from S3 metadata, and (b) send status events to SQS.

| # | Lambda | Changes | Status |
|---|--------|---------|--------|
| 1 | **StatusConsumerLambda.java** | NEW — reads status SQS, calls API endpoint | DONE |
| 2 | **Split.java** | Read jobId from S3 metadata, send JOB_REGISTERED event | DONE |
| 3 | **Invoke.java** | Read/pass jobId, send CUSTOMER_REGISTERED events | DONE |
| 4 | **DynamicValuePdf.java** | jobId in S3 metadata, send PDF_GENERATED/FAILED events | DONE |
| 5 | **EmailNotification.java** | jobId in metajson, send EMAIL_SENT/FAILED events | DONE |
| 6 | **PullBounceSQS.java** | Extract jobId, send BOUNCE events | DONE |
| 7 | **PullDeliverSQS.java** | Extract jobId, send DELIVERY events | DONE |

---

## Phase 2: API Changes (COMPLETED)

| # | Task | File(s) | Status |
|---|------|---------|--------|
| 8 | New PipelineEvent types | `PipelineEvent.java` | DONE |
| 9 | Atomic counters + throttled completion | `PipelineService.java`, `JobRepository.java` | DONE |
| 10 | Internal API key auth for status endpoint | `PipelineController.java`, `SecurityConfig.java` | DONE |
| 11 | JobResponse field alignment with UI | `JobResponse.java` | DONE |
| 12 | JobCustomer segment/fileName columns | `JobCustomer.java` | DONE |
| 13 | Role rename OPERATOR → OPS_MANAGER | `User.java` | DONE |
| 14 | Audit search/date/action filters | `AuditController.java` | DONE |
| 15 | SES statistics from AWS API | `SesStatisticsService.java` (NEW) | DONE |
| 16 | GET /config/ses/statistics endpoint | `ConfigController.java` | DONE |
| 17 | Dashboard totalPdfsGenerated/totalEmailsSent | `DashboardService.java` | DONE |
| 18 | V12 Flyway migration | `V12__add_new_event_types_and_columns.sql` | DONE |

---

## Phase 3: UI Changes (COMPLETED)

| # | Task | File | Status |
|---|------|------|--------|
| 19 | Types aligned with API | `types/index.ts` | DONE |
| 20 | SES statistics + audit filter API calls | `lib/api.ts` | DONE |
| 21 | SES config page uses API data | `ses-config/page.tsx` | DONE |
| 22 | Audit page passes filter params | `audit/page.tsx` | DONE |
| 23 | Users page uses OPS_MANAGER | `users/page.tsx` | DONE |

---

## Phase 4: 10 Scale Fixes for 20k+ Customers (COMPLETED)

| # | Fix | What Changed | Status |
|---|-----|--------------|--------|
| 1 | **Atomic counter increments** | `@Modifying @Query` in JobRepository — no read-modify-write | DONE |
| 2 | **Throttled job completion check** | `checkAndCompleteJob()` runs every 100 events (AtomicInteger) | DONE |
| 3 | **SSE skip replay on connect** | Only sends "connected" event, no full event replay | DONE |
| 4 | **Cap liveEvents to 100** | `.slice(0, 100)` prevents memory flood in browser | DONE |
| 5 | **Debounced SSE invalidation** | 2-second debounce on React Query invalidation | DONE |
| 6 | **Progress % + bar in Job Detail** | Computed from `(delivered+bounced+failed) / total * 100` | DONE |
| 7 | **Jobs page auto-refresh** | 15s polling when running jobs exist | DONE |
| 8 | **SSE auth via query param** | `JwtAuthenticationFilter` reads `?token=` fallback for EventSource | DONE |
| 9 | **API↔UI field name alignment** | `totalRecords`, `successCount`, `bounceCount`, `failureCount` | DONE |
| 10 | **DashboardMetrics enrichment** | `totalPdfsGenerated`, `totalEmailsSent` from job counters | DONE |

---

## Key Errors Fixed During Implementation

### 1. Lombok/JDK 25 Incompatibility
- **Error:** `com.sun.tools.javac.code.TypeTag :: UNKNOWN`
- **Cause:** Maven used Homebrew JDK 25 instead of Temurin JDK 17
- **Fix:** `JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run`

### 2. AWS SDK Method Name Wrong
- **Error:** `SendDataPoint.getDelivers()` does not exist
- **Investigation:** Used `javap -p` on the actual AWS SDK jar to find correct method
- **Fix:** Changed to `getDeliveryAttempts()`

### 3. Flyway V1 Checksum Mismatch
- **Error:** `Applied: 1863193, Resolved: -1927539704`
- **Cause:** Modified V1 migration file after it was already applied to DB
- **Fix:** Reverted V1 to original, updated flyway_schema_history checksum via SQL

### 4. V12 Migration CHECK Constraint Violation
- **Error:** `new row for relation "users" violates check constraint "users_role_check"`
- **Cause:** V12 tried to INSERT/UPDATE to OPS_MANAGER before dropping old constraint
- **Fix:** Reordered migration: DROP constraint → UPDATE values → ADD new constraint

### 5. Wrong Dashboard Counts
- **Error:** Dashboard showed 2, 8, 8, 8 instead of 1, 1, 0, 1
- **Cause:** Dashboard used `emailEventRepository.countByEventType()` counting ALL historical events
- **Fix:** Rewrote DashboardService to use job-level counters, scoped to today's jobs only

---

## Scale Analysis for 20k+ Customers

### Lambda Pipeline (Works Independently)
- Split, Invoke, PDF, Email Lambdas all scale via AWS concurrency
- Core processing has no shared state — each customer processed independently
- PDF Lambda: ~2s per customer → 20k customers = ~7 minutes at 50 concurrent
- Email Lambda: SES rate-limited, but async SQS trigger handles backpressure

### Status SQS FIFO Bottleneck
- **FIFO queue limit: 300 TPS** without batching
- 20k customers = ~20k+ events (CUSTOMER_REGISTERED + PDF_GENERATED + EMAIL_SENT + DELIVERY = ~4 events each = ~80k events)
- At 300 TPS, draining 80k events takes ~4.5 minutes
- **Fix for 100k+:** Switch to standard queue, use sendMessageBatch

### API Throughput
- StatusConsumer calls `POST /pipeline/status-event` for each event
- With atomic counters (no read-modify-write), each call = 1 UPDATE query
- PostgreSQL handles 1000+ simple UPDATEs/sec easily
- HikariCP default pool (10) may bottleneck at 200+ req/s — increase to 25-50

### UI Real-Time Updates
- SSE stream pushes new events to connected browsers
- With throttled completion check (every 100 events), API load is manageable
- React Query 15s polling for jobs list, debounced invalidation for job detail

---

## Pending: UI Section Removals (Awaiting Confirmation)

5 UI sections identified as having no real data source. User said "wait for me to say yes to implement."

### 1. Dashboard → Segments Strip
- **Location:** `dashboard/page.tsx` lines ~291-312
- **Issue:** 12 hardcoded named pipelines (Equity v6, ROS v2, etc.) with fake statuses/counts
- **No data source:** System has no concept of named processing pipelines

### 2. Resend → "Recent Bulk Resends" Table
- **Location:** `resend/page.tsx`
- **Issue:** 3 fake rows (RSND-00412, RSND-00411, RSND-00410) with hardcoded dates/statuses
- **No data source:** No `resend_history` table or API endpoint exists

### 3. Resend → Hardcoded Suppression Warning
- **Location:** `resend/page.tsx`
- **Issue:** "541 recipients are in the suppression list" — hardcoded count
- **No data source:** Suppression count exists but not linked to resend context

### 4. Resend → Hardcoded "312 Eligible" Bounced Count
- **Location:** `resend/page.tsx`
- **Issue:** "312 eligible for resend" — hardcoded number
- **No data source:** Bounce count per-job exists but not pre-computed for resend

### 5. Clients → Two Alert Cards
- **Location:** `clients/page.tsx`
- **Issue:** "Complaint rate approaching threshold" and "DP Trade T+1 at risk" — fabricated scenarios
- **No data source:** No complaint threshold tracking or DP Trade monitoring system exists
- **Keep:** Bounce breakdown chart (data comes from job_customers table)

---

## Phase 5: Infrastructure (MANUAL — Needs AWS Console)

| # | Task | Status |
|---|------|--------|
| 24 | Create SQS queue: `geojit-pipeline-status-queue.fifo` | PENDING |
| 25 | Create Lambda: `status-consumer-geojit` | PENDING |
| 26 | Add SQS permissions to existing Lambdas | PENDING |

---

## Remaining Scale Concerns (100k+ Customers)

| Concern | Fix |
|---------|-----|
| FIFO 300 TPS limit | Switch to standard SQS queue |
| Individual SQS sends | Batch with sendMessageBatch (10 messages/call) |
| HikariCP pool (10 default) | Increase to 25-50 connections |
| pipeline_events table growth | Partition by month or jobId range |
| Dashboard aggregation queries | Add materialized views or cache |

---

## Files Modified (Complete List)

### Lambda Project (`geojit-contract-notes`)
| File | Change |
|------|--------|
| `StatusConsumerLambda.java` | NEW — SQS consumer calling API |
| `Split.java` | Read jobId, send JOB_REGISTERED |
| `Invoke.java` | Read/pass jobId, send CUSTOMER_REGISTERED |
| `DynamicValuePdf.java` | jobId in S3 metadata, send PDF_GENERATED/FAILED |
| `EmailNotification.java` | jobId in metajson, send EMAIL_SENT/FAILED |
| `PullBounceSQS.java` | Extract jobId, send BOUNCE |
| `PullDeliverSQS.java` | Extract jobId, send DELIVERY |

### API Project (`geojit-contract-note-api`)
| File | Change |
|------|--------|
| `PipelineEvent.java` | Added JOB_REGISTERED, CUSTOMER_REGISTERED |
| `PipelineService.java` | Atomic counters, throttled completion, new event handling |
| `JobRepository.java` | 6 `@Modifying @Query` atomic increment methods |
| `PipelineController.java` | X-Internal-Key auth for status endpoint |
| `SecurityConfig.java` | Internal endpoint public access + key filter |
| `JobResponse.java` | Field alignment (totalRecords, progressPercent, uploadedBy object) |
| `JobCustomer.java` | Added segment, fileName columns |
| `User.java` | Renamed OPERATOR → OPS_MANAGER |
| `AuditController.java` | Added search, from, to, action filters |
| `SesStatisticsService.java` | NEW — real AWS SES API metrics |
| `ConfigController.java` | GET /config/ses/statistics endpoint |
| `DashboardService.java` | Job-level counter aggregation (no emailEventRepository) |
| `DashboardMetricsResponse.java` | Added totalPdfsGenerated, totalEmailsSent |
| `JwtAuthenticationFilter.java` | Query param token fallback for SSE |
| `JobController.java` | SSE: skip replay, only send "connected" |
| `V12__add_new_event_types_and_columns.sql` | Event types, role rename, new columns |

### UI Project (`geojit-contract-note-ui`)
| File | Change |
|------|--------|
| `types/index.ts` | Aligned Job, UserRole, added DashboardMetrics fields |
| `api.ts` | Added sesStatistics, audit filter params |
| `ses-config/page.tsx` | Real metrics from API |
| `audit/page.tsx` | Filter params passed to API |
| `users/page.tsx` | OPS_MANAGER role, form reset fix |
| `dashboard/page.tsx` | Fixed metric card data sources |
| `jobs/page.tsx` | Auto-refresh (15s), field name alignment |
| `jobs/[jobId]/page.tsx` | SSE debouncing, event cap, progress bar |

---

## How to Run Locally

```bash
# API (from geojit-contract-note-api/)
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run -Dspring-boot.run.profiles=local

# UI (from geojit-contract-note-ui/)
npm run dev
```

## Mock Data SQL (for local testing)

Used for loading IUP090 customer data:
```sql
-- Insert into jobs table with all counters populated
-- Insert into job_customers with PDF_GENERATED, EMAIL_SENT, DELIVERED statuses
-- Insert into pipeline_events with full event timeline
```

---

*End of session summary. Next steps: (1) User confirms UI section removals, (2) AWS infrastructure setup, (3) End-to-end testing with real Lambda pipeline.*
