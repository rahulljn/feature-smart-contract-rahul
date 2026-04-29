# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
|------|----------|
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.

---

## Repository Layout

```
smart-contract-platform/
├── geojit-contract-note-ui/      # Next.js 15 frontend dashboard
├── geojit-contract-note-api/     # Spring Boot 3 REST API + AWS integration
├── geojit-contract-notes/        # AWS Lambda functions (PDF generation + email)
└── docs/codebase-map/            # ARCHITECTURE.md, API.md, UI.md, LAMBDA.md, DEPENDENCIES.md
```

This is a **contract note distribution system** for Geojit Securities — it generates PDF contract notes (trade statements) and emails them to ~25,000 customers per run.

---

## Dev Commands

### Frontend (`geojit-contract-note-ui/`)
```bash
npm install
npm run dev      # http://localhost:3000
npm run build
npm run lint
```
Configure `NEXT_PUBLIC_API_BASE_URL` in `.env.local` (default: `http://localhost:8080/api/v1`).

### API (`geojit-contract-note-api/`)
```bash
docker compose up -d                                              # Start PostgreSQL 15
mvn spring-boot:run -Dspring-boot.run.profiles=local             # Run API on :8080
mvn clean package -DskipTests                                    # Build JAR
```
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`
- Default admin: `admin@geojit.com` / `Admin@123`
- DB (local): `localhost:5432`, db `geojit_contract_note`, user `brijesh`

### Lambda (`geojit-contract-notes/`)
```bash
mvn clean package -DskipTests    # Builds shaded uber-JAR for Lambda deployment
```
Deployed to AWS Lambda — not run locally. Pass `input.json` directly to handler classes for local testing.

---

## System Architecture

```
User (Browser)
  │
  ▼
geojit-contract-note-ui  (Next.js 15, React 19, TypeScript, Tailwind CSS 4)
  │  REST/JSON (Bearer JWT)     SSE for real-time updates
  ▼
geojit-contract-note-api  (Spring Boot 3.2.5, Java 17, PostgreSQL 15)
  │  S3 upload       SQS trigger      SNS webhook
  ▼
AWS Pipeline
  ├── Stage 1: split-lambda-geojit       (S3 trigger on raw-s3-geojit)
  ├── Stage 2: invoke-lambda-geojit      (SQS FIFO trigger)
  ├── Stage 3: create-pdf-geojit         (S3 trigger on json-s3-geojit)
  └── email-notification-geojit          (sends via SES)
        │  delivery/bounce/complaint events
        ▼
      SNS → POST /api/v1/webhooks/ses → API updates DB → SSE → UI
```

### End-to-End Data Flow

1. Operator uploads pipe-delimited `.txt` file via UI → `POST /api/v1/jobs/upload`
2. API creates `Job` + `JobCustomer` records, uploads file to `raw-s3-geojit-prod`
3. **Stage 1 Lambda** (`SplitEquityCombineMarginFile`): S3 event → validates 7 mandatory record types → splits into 256 KB chunks by party code → publishes chunk metadata to SQS FIFO
4. **Stage 2 Lambda** (`InvokeEquityCombineMarginFile`): SQS event → parses 20 record types → builds `GeojitStatementDTO` per customer → invokes PDF Lambda (or uploads to `json-s3-geojit` if payload ≥ 256 KB)
5. **Stage 3 Lambda** (`EquityCombineMarginJsonFileBaseTrigger`): S3 event on `json-s3-geojit` → generates 3-page iText PDF → uploads to `pdf-s3-geojit`
6. Email Lambda sends via SES with `metajson` custom header (`{jobid, partycode, tradedate}`)
7. SES delivery/bounce/complaint events → SNS → `POST /api/v1/webhooks/ses` → API updates `JobCustomer.emailStatus`, increments counters
8. `status-consumer-geojit` Lambda sends progress events → `POST /api/v1/pipeline/status-event` → API broadcasts via SSE
9. UI receives SSE events on `/jobs/[jobId]/stream` and `/process` pages

---

## Frontend — Pages & Features

All authenticated routes live under `src/app/(dashboard)/`. Protected by `src/middleware.ts` (checks `auth-token` cookie).

### `/login`
- Email/password form with Zod validation
- On success: stores JWT in Zustand `useAuthStore` (`auth-storage` localStorage) AND `auth-token` cookie (max-age = `expiresIn`)
- Left panel shows brand story: pre-flight validation, live pipeline visibility, bounce reconciliation
- Real-time IST clock (bottom right)

### `/dashboard`
- **6 KPI metric cards**: Total customers in pipeline, PDFs generated, emails sent, delivered, bounced, failed records
- **Date filters**: Today / Last 7d / This month + custom date range
- **Pipeline funnel**: Records uploaded → PDFs generated → Emails sent → Delivered/Bounced/Failed (counts + percentages)
- **Recent activity feed**: Latest 10 pipeline events with timestamps
- **Auto-refresh**: Metrics refetch every 30 seconds
- API: `GET /api/v1/dashboard/metrics?from=&to=`

### `/process` — File Upload Wizard (4 steps)
1. **Upload**: Drag-drop `.txt`/`.csv`, shows file size, recent uploads sidebar
2. **Configure**: Select segment type from 14 options (EQUITY-COMBINEMARGIN, ROS, BILL, COMMODITY, DP-HOLDING, DP-HOLDING-YEARLY, DP-LEDGER-WEEKLY, DP-TRADE-TXN, STT, PNL, AGTS, QS-LEDGER, QS-RETENTION, DMR)
3. **Pre-flight**: Static checks (extension, size, segment) + optional validation scan → returns `{totalCustomers, validCustomers, invalidCustomers, invalidReasons}`
4. **Submitted**: Job ID shown + live SSE stream of pipeline events + 4-stage status bar (File uploaded → Parsing → PDF generation → Email dispatch) + failure summary + quick resend button
- APIs: `POST /jobs/upload`, `POST /jobs/validate`, `GET /jobs/{jobId}/stream` (SSE)
- Polls job status every 5s while processing

### `/jobs` — Runs & Jobs List
- Filter bar: date range (Today/Last 7d/This month), status pills (All/Running/Completed/Partial/Failed)
- **4 summary cards**: Total runs, Running (pulsing dot), Completed, Partial/Failed
- **Table**: File Name (link to detail), Job ID (8-char truncated), Segment chip, Records, Status pill, Uploaded (relative time), Uploaded By
- **CSV export**: Downloads `runs.csv`
- Pagination: Prev/Next + page counter
- Auto-refresh every 15s when any job has status VALIDATING/SPLITTING/PROCESSING/EMAILING
- API: `GET /api/v1/jobs?page=&size=&status=&from=&to=`

### `/jobs/[jobId]` — Job Detail
- **4 tabs**: Overview, Pipeline, Exceptions, Customers
- **Overview**: Job metadata + counters (total, PDF generated, email sent, delivered, bounced, failed, hard/soft bounce)
- **Pipeline**: Real-time SSE event stream (connects to `GET /jobs/{jobId}/stream`), historical pipeline events table
- **Exceptions**: CloudWatch Lambda errors by type (SPLIT/INVOKE/GETJSON/PDF/EMAIL/PULLBOUNCE/PULLDELIVERY), expandable stack traces
- **Customers**: Paginated table with PDF/email status, bounce type, PDF S3 key, per-row resend button, CSV export
- Polls every 5s while job running
- APIs: `GET /jobs/{jobId}`, `GET /jobs/{jobId}/customers`, `GET /jobs/{jobId}/pipeline-stats`, `GET /jobs/{jobId}/exception-counts`, `GET /jobs/{jobId}/cloudwatch-exceptions`, `GET /jobs/{jobId}/exceptions`, `POST /jobs/{jobId}/customers/{partyCode}/resend`

### `/resend` — Bulk Resend Interface
**Configuration panel** — 4 resend scopes:
1. By Job: re-queue all failed/bounced in a single run (select job dropdown)
2. By date range: all jobs within a date window
3. By client code list: paste party codes (comma or newline separated), select source job
4. All bounced: global retry of all BOUNCED customers

Optional template override dropdown. "Queue resend" button.

**Failed & Bounced table**:
- Filter bar: Status pills (ALL/BOUNCED/FAILED), job dropdown, date range, refresh
- Columns: Checkbox, Party Code (monospace), Email, Status, Bounce Type (Permanent/Transient), Job/Segment, Date, Resend button
- Bulk selection: toggle all + individual; selected count shown; "Resend selected" groups by jobId and calls `bulkResendCodes` per group
- Pagination: 50 rows/page
- APIs: `GET /jobs/failed-customers`, `POST /jobs/{jobId}/customers/{partyCode}/resend`, `POST /jobs/{jobId}/bulk-resend-codes`, `POST /jobs/{jobId}/bulk-resend`, `POST /jobs/bulk-resend-all-bounced`

### `/templates` — Email Template Editor
- **List table**: Name, Status (Active/Draft pill), Last Modified, Size (KB), Actions (View HTML / Edit)
- **HTML Viewer**: Raw HTML from S3, scrollable dark view, max-h-[60vh]
- **3-tab inline editor**:
  1. **Edit Fields**: 6 editable fields with color-coded labels — Subject (purple), Greeting Text (yellow), Body Intro (blue), Logo URL (teal), Body Color (orange, with color picker), Footer Color (rose, with color picker). Validate button checks `<html>`, `<body>`, `[NAME]` placeholder. Save/Reset buttons.
  2. **Full HTML**: Live-rendered HTML preview based on current field values, `[NAME]` highlighted in yellow
  3. **Preview**: Rendered email in `<iframe sandbox="allow-same-origin">` (65vh)
- Save calls `PUT /templates/{name}/fields` → API rebuilds full HTML from editable fields + fixed skeleton → syncs to S3 `active/contract-note.html`
- APIs: `GET /templates`, `GET /templates/{name}/content`, `PUT /templates/{name}/fields`, `GET /templates/active`

### `/clients` — Client 360
- **Autocomplete combo**: Fetches all party codes on mount, filters client-side (min 2 chars), recent searches (max 4)
- **Optional filters**: Date range (job processing date), segment dropdown
- **Search results**: Card per client (avatar, name, email, contract count, "View profile" link → `/clients/{partyCode}`)
- **Bounce breakdown card**: Stacked bar chart of bounce types with percentages and color codes
- APIs: `GET /clients/codes`, `GET /clients/suggest?q=`, `GET /clients/search?q=&fromDate=&toDate=&segment=`

### `/clients/[partyCode]`
- PDF list from S3 with download presigned URLs (7-day expiry dev, 24h prod)
- Email event timeline (SEND → DELIVERED/BOUNCED/COMPLAINT) from DB
- Email override: `PUT /clients/{partyCode}/email` (updates all job_customer records)
- APIs: `GET /clients/{partyCode}/pdfs`, `GET /clients/{partyCode}/timeline`, `GET /clients/{partyCode}/pdf-url?key=`, `PUT /clients/{partyCode}/email`

### `/users` — User Management (ADMIN only)
- Table: User (avatar + name + email), Organisation pill, Role pill, Permissions text, Last active, Edit/Deactivate actions
- Role permissions displayed inline: ADMIN "Upload · Override · Bulk resend · Template · PFX · Users" | OPS_MANAGER "Upload · Override · Bulk resend · Template · PFX" | VIEWER "Read-only · Reports · Audit"
- Add/Edit user dialog: Full name, Corporate email, Organisation (ACC/GEOJIT), Role, Password
- Deactivate: confirmation alert dialog
- APIs: `GET /users`, `POST /users`, `PUT /users/{id}`, `DELETE /users/{id}`

### `/exceptions` — Lambda Exception Browser
- 7 tabs: Split, Invoke, GetJson, PDF, Email, Pull Bounce, Pull Delivery
- Job filter dropdown
- Paginated exceptions table: expandable entries with error type, recordId, timestamp, message, stack trace
- API: `GET /exceptions?jobId=&lambdaName=` (drains SQS exceptions queue, persists to DB)

### `/ses-config` — Email Configuration
- SES config set CRUD (configSetName, fromEmail, fromName, domain, region)
- Activate config (deactivates all others)
- SES statistics (bounce rate, complaint rate, send quota)
- APIs: `GET /config/ses`, `POST /config/ses`, `PUT /config/ses/{id}`, `POST /config/ses/{id}/activate`, `GET /config/ses/statistics`

### `/certificates` — PFX Certificate Management
- Upload PFX file (multipart: file + password + label)
- API extracts subject, issuer, validFrom, validTo, SHA-1 thumbprint; stores PFX in S3
- Activate certificate, view expiry
- APIs: `GET /config/certificates`, `GET /config/certificates/active`, `POST /config/certificates/activate?secretName=`, `POST /config/certificates/upload`

### `/audit` — Audit Log (ADMIN only)
- Paginated log filtered by search text, date range, action type
- Actions: LOGIN, LOGOUT, UPLOAD, RESEND, BULK_RESEND, CREATE, UPDATE, DELETE, etc.
- API: `GET /audit?page=&size=&search=&from=&to=&actions=`

---

## Frontend — Implementation Details

### Auth & Routing
- **State**: Zustand `useAuthStore` (`src/store/auth.ts`) persisted to `auth-storage` in localStorage. Fields: `user (AuthUser|null)`, `isAuthenticated`. Actions: `setUser`, `clearUser`.
- **Cookie**: `auth-token` set on login (max-age = `expiresIn`), used by middleware
- **Middleware** (`src/middleware.ts`): Checks `auth-token` cookie. Missing → redirect to `/login?from={pathname}`. On `/login` with token → redirect to `/dashboard`.
- **Dashboard layout**: Client-side `isAuthenticated` re-check; redirects to `/login` if false

### API Client (`src/lib/api.ts`)
- Axios instance with base URL from `NEXT_PUBLIC_API_BASE_URL`
- **Request interceptor**: Reads JWT from Zustand `auth-storage` localStorage, sets `Authorization: Bearer {token}`
- **Response interceptor**: On 401, clears localStorage, redirects to `/login`
- API namespaces: `authApi`, `jobsApi`, `templatesApi`, `clientsApi`, `suppressionApi`, `configApi`, `usersApi`, `dashboardApi`, `pipelineApi`, `lambdaExceptionsApi`

### TypeScript Types (`src/types/index.ts`)
Key types: `AuthUser`, `Job`, `JobCustomer`, `DashboardMetrics`, `PipelineEvent`, `EmailTemplate`, `Certificate`, `SesConfig`, `AppUser`, `CloudWatchLambda`, `LambdaException`, `ApiResponse<T>`, `PageResponse<T>`

### Utilities (`src/lib/utils.ts`)
- `cn()`: Tailwind class merger (clsx + twMerge)
- `fmtDate(d)`: "22 Apr 2026" (IST)
- `fmtDateTime(d)`: "22 Apr 2026, 10:30 PM" (IST)
- `toUtcDate(iso)`: Appends 'Z' to prevent IST shift on ISO strings without timezone
- `downloadCsv(filename, headers, rows)` in `src/lib/export.ts`

### SSE Real-Time Updates
```javascript
const url = `/api/v1/jobs/${jobId}/stream?token=${jwtToken}`;
const source = new EventSource(url);
source.addEventListener('PDF_GENERATED', e => { /* update UI */ });
```
Token passed as query param because `EventSource` API cannot set custom headers. Used on `/process` (step 4) and `/jobs/[jobId]` (Pipeline tab).

### Styling
- Tailwind CSS 4 + shadcn/ui (Radix primitives)
- Fonts: Manrope (headline), Inter (sans), JetBrains Mono (mono)
- Icons: Google Material Symbols Outlined
- Toasts: Sonner (top-right, richColors)
- Custom utility classes: `.card`, `.pill`, `.dot`, `.seg-chip`, `.metric`, `.t-hd`, `.t-row`, `.mono`, `.headline`, `.fade-up`
- Primary color: `#00174b` (dark blue); accent: `#003ea8`, `#497cff`

---

## API — All Endpoints

Base path: `/api/v1`. All authenticated endpoints require `Authorization: Bearer <JWT>`.

### AuthController — `/api/v1/auth`
| Method | Path | Auth | Body | Response |
|--------|------|------|------|----------|
| POST | `/login` | Public | `{email, password}` | `{accessToken, tokenType, expiresIn, email, name, role, userId}` |
| POST | `/logout` | Bearer | — | `{message}` (advisory) |
| GET | `/me` | Bearer | — | `AuthResponse` |

### JobController — `/api/v1/jobs`
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/upload` | ADMIN/OPS_MGR | Multipart: `file, segmentType, tradeDate` |
| POST | `/validate` | ADMIN/OPS_MGR | Pre-flight only; no DB record; returns `{valid, errors[]}` |
| GET | `/` | Any | Params: `page, size, status, from, to` |
| GET | `/{jobId}` | Any | Full job with all counters |
| GET | `/{jobId}/customers` | Any | Paginated; params: `page, size` |
| GET | `/{jobId}/pipeline-stats` | Any | Throughput, P50/P95 latency, error rate |
| GET | `/{jobId}/exception-counts` | Any | `{pdfFailed, emailFailed, bounced, skipped, invalidRecords, hardBounced, softBounced}` |
| GET | `/{jobId}/cloudwatch-exceptions` | Any | Param: `type` (ALL/SPLIT/INVOKE/GETJSON/PDF/EMAIL/PULLBOUNCE/PULLDELIVERY) |
| GET | `/{jobId}/exceptions` | Any | Params: `type, page, size` |
| POST | `/{jobId}/customers/{partyCode}/resend` | ADMIN/OPS_MGR | Body: `{overrideEmail?, templateId?}` |
| POST | `/{jobId}/bulk-resend` | ADMIN/OPS_MGR | Body: `{templateId?}`; returns `{queued: int}` |
| POST | `/{jobId}/bulk-resend-codes` | ADMIN/OPS_MGR | Body: `{partyCodes[], templateId?}` |
| POST | `/bulk-resend-all-bounced` | ADMIN/OPS_MGR | Global retry for all BOUNCED |
| GET | `/failed-customers` | Any | Params: `statuses, jobId, from, to, page, size` |
| GET | `/{jobId}/stream` | Any | SSE; token via `?token=<JWT>` query param |

### PipelineController — `/api/v1/pipeline`
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| GET | `/events` | Any | Param: `jobId`; returns ordered event list |
| POST | `/status-event` | X-Internal-Key header | Internal webhook from `status-consumer-geojit` Lambda |

### ClientController — `/api/v1/clients`
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| GET | `/codes` | Any | All distinct party codes |
| GET | `/suggest` | Any | Param: `q`; returns up to 8 matches |
| GET | `/search` | Any | Params: `q, fromDate, toDate, segment` |
| GET | `/{partyCode}/pdfs` | Any | S3 listing with metadata |
| GET | `/{partyCode}/reports` | Any | Delivery/bounce CSV listing |
| GET | `/{partyCode}/timeline` | Any | Email events from DB |
| GET | `/{partyCode}/pdf-url` | Any | Param: `key`; returns presigned URL |
| PUT | `/{partyCode}/email` | Any | Body: `{newEmail}` |

### TemplateController — `/api/v1/templates`
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| GET | `/` | Any | List from S3 (`active/` + `drafts/`) |
| GET | `/{name}/content` | Any | Raw HTML from S3 |
| PUT | `/{name}/fields` | ADMIN/OPS_MGR | Rebuild HTML + sync to S3 |
| PUT | `/{name}/content` | ADMIN/OPS_MGR | Save full HTML to S3 |
| GET | `/active` | Any | Active template from DB |
| GET | `/{id}` | Any | Template by ID from DB |
| POST | `/` | ADMIN/OPS_MGR | Create in DB |
| PUT | `/{id}` | ADMIN/OPS_MGR | Update full HTML in DB |
| PUT | `/{id}/fields` | ADMIN/OPS_MGR | Update editable fields in DB |
| POST | `/{id}/activate` | ADMIN | Activate; deactivates all others |
| POST | `/{id}/validate` | Any | Checks `<html>`, `<body>`, `[NAME]` |

### SesWebhookController — `/api/v1/webhooks`
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/ses` | None (X-Amz-Sns-Message-Type header) | Handles SubscriptionConfirmation + Delivery/Bounce/Complaint; returns 503 if customer not yet registered (SNS retries) |

### UserController — `/api/v1/users` (ADMIN only)
`GET /` (paginated), `POST /`, `PUT /{id}`, `DELETE /{id}` (deactivate = set isActive=false)

### ConfigController — `/api/v1/config`
| Method | Path | Auth |
|--------|------|------|
| GET/POST | `/ses` | Any / ADMIN |
| PUT | `/ses/{id}` | ADMIN |
| POST | `/ses/{id}/activate` | ADMIN |
| GET | `/ses/statistics` | Any |
| GET | `/certificates` | Any |
| GET | `/certificates/active` | Any |
| POST | `/certificates/activate` | ADMIN |
| POST | `/certificates/upload` | ADMIN |

### Other Controllers
- **DashboardController**: `GET /dashboard/metrics?from=&to=`
- **AuditController** (ADMIN): `GET /audit?page=&size=&search=&from=&to=&actions=`
- **LambdaExceptionController**: `GET /exceptions?jobId=&lambdaName=` (drains SQS exceptions queue + persists)
- **SuppressionController**: `GET /suppression`, `POST /suppression`, `DELETE /suppression/{email}`, `POST /suppression/push-aws`, `POST /suppression/sync-aws`

---

## API — Security

**Public endpoints** (no JWT required):
- `/api/v1/auth/login`, `/api/v1/auth/logout`
- `/actuator/health`
- `/swagger-ui/**`, `/api-docs/**`
- `/api/v1/webhooks/**` (SNS verified by `X-Amz-Sns-Message-Type` header)
- `/api/v1/pipeline/status-event` (Lambda verified by `X-Internal-Key` header)

**JWT details**: RS256 algorithm; keys in `src/main/resources/keys/private.pem` + `public.pem`; 24h expiry (dev), 8h expiry (prod); issuer `geojit-contract-note-api`; claims: `email`, `role`, `userId`

**Role hierarchy**:
- `ADMIN`: Full access including user management, audit log, SES/certificate config, template activation
- `OPS_MANAGER`: Upload, validate, resend, template editing, suppression management
- `VIEWER`: Read-only (dashboard, jobs, clients, exceptions)

**CORS**: `localhost:3000` and `localhost:3001` allowed in dev; `Authorization` header exposed

---

## API — Database Schema

### User
```
PK: user_id (UUID)
email (unique, 500), name, password (bcrypt), role (ADMIN/OPS_MANAGER/VIEWER)
organisation (ACC/GEOJIT), isActive, lastLogin, createdAt, updatedAt
```

### Job
```
PK: job_id (UUID)
fileName, rawS3Key, uploadedBy (FK→User), uploadedAt
status: VALIDATING | SPLITTING | PROCESSING | EMAILING | COMPLETED | FAILED | PARTIAL
segmentType, tradeDate (LocalDate)
Counters: totalCustomers, processedCount, pdfGeneratedCount, emailSentCount,
          emailDeliveredCount, emailBouncedCount, emailFailedCount, emailSkippedCount,
          failedCount, invalidRecordCount, pdfFailedCount, hardBounceCount, softBounceCount
createdAt, updatedAt
Indices: status, uploadedAt DESC, tradeDate, uploadedBy
```

### JobCustomer
```
PK: id (BIGSERIAL)
FK: job_id (UUID, CASCADE DELETE)
UNIQUE: (job_id, party_code)
partyCode, contractNoteNo, tradeDate, email, segment, fileName
pdfStatus: PENDING | GENERATED | FAILED
pdfS3Key, pdfGeneratedAt
emailStatus: PENDING | SENT | BOUNCED | DELIVERED | FAILED | SKIPPED
sesMessageId, bounceType, bounceReason, emailSentAt, deliveredAt, bouncedAt
createdAt, updatedAt
Indices: job_id, party_code, email_status, pdf_status, ses_message_id
```

### PipelineEvent
```
PK: event_id (BIGSERIAL)
jobId (UUID), partyCode, lambdaName (nullable)
eventType: JOB_REGISTERED | CUSTOMER_REGISTERED | SPLIT_PROGRESS | SPLIT_COMPLETE |
           PDF_TRIGGERED | PDF_GENERATED | PDF_FAILED | EMAIL_SENT | EMAIL_FAILED |
           EMAIL_SKIPPED | DELIVERY | BOUNCE | COMPLAINT | RESEND_TRIGGERED
payload (JSONB), eventTimestamp
Indices: job_id, party_code, event_type, event_timestamp DESC, (job_id, party_code)
```

### EmailEvent
```
PK: id (BIGSERIAL)
sesMessageId, partyCode, jobId (UUID)
eventType: SEND | DELIVERY | BOUNCE | COMPLAINT
bounceType, bounceSubType, recipientEmail, eventTimestamp
Indices: ses_message_id, party_code, job_id, event_timestamp DESC
```

### AuditLog
```
PK: id (BIGSERIAL)
userId (FK→User), userEmail
action: LOGIN | LOGOUT | UPLOAD | RESEND | BULK_RESEND | CREATE | UPDATE | DELETE | ...
targetEntity, details (JSONB), ipAddress, userAgent, eventTimestamp
```

### EmailTemplate
```
PK: templateId (UUID)
name (unique), subject, htmlBody (TEXT), segment, isActive
lastEditedBy (FK→User), createdAt, lastEditedAt
Editable fields: greetingText, bodyIntro, logoUrl, bodyColor, footerColor
```

### SesConfig
```
PK: id (UUID)
configSetName (unique), fromEmail, fromName, domain, region, isActive, createdAt
```

### SuppressionList
```
PK: id (BIGSERIAL)
email (unique, case-insensitive), reason, addedBy (FK→User), addedAt
```

### Certificate
```
PK: id (UUID)
fileName, subject, issuer, validFrom, validTo, thumbprint (SHA-1)
secretName, s3Key, isActive, uploadedBy (FK→User), createdAt
```

### LambdaException
```
PK: id (BIGSERIAL)
jobId, lambdaName, recordId, errorType, errorMessage, stackTrace
environment, occurredAt (Instant)
```

---

## API — Key Business Logic

### ResendService — Two-path resend
- **Path A** (PDF exists in S3): Downloads PDF bytes from `pdf-s3-geojit`, constructs MIME message with PDF attachment, sends directly via `AmazonSES.sendRawEmail()`
- **Path B** (no PDF): Resets `pdfStatus=PENDING`, `emailStatus=PENDING`, pushes to SQS `geojit-map-split-processing-queue.fifo` → Lambda re-processes the customer
- Both paths: Check suppression list before sending; add `metajson` custom header to email for SNS fallback (`{jobid, partycode, tradedate}`); create `RESEND_TRIGGERED` pipeline event; log to audit

### SES Webhook — SNS Fallback Resolution
When SES delivers/bounces an email and the API cannot find the `JobCustomer` by `sesMessageId`, it reads the `metajson` custom header from the SNS notification to look up by `(jobId, partyCode)`. If still not found, returns HTTP 503 so SNS retries.

### partyCode Normalization
`ZYR175/ZYR175` is normalized to `ZYR175` across all repository queries. The `/` separator pattern appears in some source data.

### Job Completion Detection
`PipelineService.processStatusEvent()` checks after every event whether all `JobCustomer` records have a terminal `emailStatus`. If yes, transitions `Job.status` to `COMPLETED` (all delivered), `PARTIAL` (some bounced/failed), or `FAILED` (all failed).

### Atomic Counters
Job counter fields (e.g., `pdfGeneratedCount`, `emailSentCount`) are incremented via native SQL `UPDATE jobs SET count = count + 1 WHERE job_id = ?` — not JPA `save()` — to avoid lost updates under concurrent Lambda invocations.

### Template HTML Rebuild
`EmailTemplateService.buildHtml(template)` constructs full HTML from the 6 editable fields + a fixed skeleton (logo image, greeting, body intro, PAN instructions, disclaimer footer). Called on every field update. The rebuilt HTML is synced to S3 `active/contract-note.html` which Email Lambda caches with a 15-minute TTL.

### SKIPPED emails
Customers where the email address is in the suppression list get `emailStatus=SKIPPED`. In UI display, SKIPPED folds into the FAILED count.

### JobTimeoutService
Scheduled Spring task that auto-completes jobs stuck in `PROCESSING` or `EMAILING` status after a configurable cutoff period (prevents jobs from getting permanently stuck if Lambda events are missed).

---

## API — Flyway Migrations

| Version | Schema Changes |
|---------|---------------|
| V1 | `users` + admin seed (`admin@geojit.com` / `Admin@123`) |
| V2 | `jobs` |
| V3 | `job_customers` (unique constraint on `job_id + party_code`) |
| V4 | `pipeline_events` (JSONB payload) |
| V5 | `email_events` |
| V6 | `audit_log` (JSONB details, IP, user agent) |
| V7 | `email_templates` + live template seed |
| V8 | `certificates` + placeholder seed |
| V9 | `ses_config` (15 configs) + `suppression_list` |
| V10–V22 | Performance indexes, enum additions (`RESEND_TRIGGERED`, `JOB_REGISTERED`, etc.), template editable fields columns, `organisation` column on users, timezone handling fixes, semantic counter columns (`hardBounceCount`, `softBounceCount`) |

**Flyway owns all schema changes.** Never modify schema directly.

---

## Lambda Functions

### Stage 1 — `SplitEquityCombineMarginFile`
**Trigger**: S3 PUT event on `raw-s3-geojit-prod`

**Logic**:
1. Download raw file from S3
2. Validate 7 mandatory record types per customer: H (Header), D (Derivatives), O (Options), F (Futures), A (Allocation), S_CAPITAL (Capital segment), S_FUTURES (Futures segment)
3. Invalid customers → `geojit-error-bucket-prod` (date-partitioned)
4. Valid customers → split into 256 KB chunks keyed by party code → upload to `chunks-s3-geojit-prod`
5. Publish chunk metadata to `geojit-map-split-processing-queue.fifo` with SHA-256 dedup ID
6. Write process start-time CSV to `geojit-report-files-s3-prod`

### Stage 2 — `InvokeEquityCombineMarginFile`
**Trigger**: SQS FIFO message from `geojit-map-split-processing-queue.fifo`

**Logic**:
1. Parse pipe-delimited records; process 20 record types: `H, E, P, V, D, O, F, N, M, T, U, C, R, A, L, G, J, K, Q`
2. Clear all DTO lists on each `H` record (party code boundary)
3. Build `GeojitStatementDTO` per customer
4. If payload < 256 KB → invoke `DynamicValuePdf` Lambda directly
5. If payload ≥ 256 KB → upload JSON to `json-s3-geojit` → triggers Stage 3 via S3 event
6. Rate limit: 118 invocations/second (Guava `RateLimiter`)
7. 11-minute time window: lines past the window written to overflow file, re-enqueued to SQS
8. Append end-time row to report CSV

### Stage 3 — `EquityCombineMarginJsonFileBaseTrigger`
**Trigger**: S3 PUT event on `json-s3-geojit`

**Logic**: Download JSON → deserialize `EquityDtoV2` → call `DynamicValuePDF_Shivraj.generatePDF()` → upload 3-page PDF to `pdf-s3-geojit-prod`

### PDF Renderers
- **`DynamicValuePdf`** (current): Zero aggregation — all totals pre-computed in DTO. iText 7 renderer. Features: dynamic headers/footers, transaction tables, QR code (ZXing), PDF encryption, dual certificate signing. Reports status events back to API.
- **`DynamicValuePDF_Shivraj`** (V1 legacy): Used by Stage 3. Separate font initialization required.

### `GeojitStatementDTO` — 22 section DTOs
`HeaderDto`, `ExchangeClearingDto`, `EquitySegmentDto`, `DerivativeSegmentDto`, `NameClearingCorporationDto`, `PayInPayOutDto`, `DatePlaceDto`, `NameAndExchangeTotalDto`, `NetObligationDto`, `NetObligationTotalDto`, `ScripSummaryDto`, `SecurityTransactionDto`, `SecurityTransactionTotalDto`, `CashSegmentTotalDto`, `CashSegmentDto`, `DailyMarginDto`, `DailyMarginTotalDto`, `MarginPledgeDto`, `MarginPledgeTotalDto`

All DTOs are plain JavaBeans. No calculation logic — data arrives pre-computed from the parsing stage.

### Supporting Lambdas
- **`email-notification-geojit`**: Sends emails via SES with PDF attachment + `metajson` custom header
- **`pull-bounce-geojit`**: Retrieves bounce events from SES
- **`pull-delivery-geojit`**: Retrieves delivery confirmations from SES
- **`status-consumer-geojit`**: Consumes pipeline status events from SQS and posts to `POST /api/v1/pipeline/status-event`

---

## AWS Resource Map

| Service | Resource Name | Purpose |
|---------|--------------|---------|
| S3 | `raw-s3-geojit-prod` | Raw uploaded files → triggers Stage 1 |
| S3 | `chunks-s3-geojit-prod` | 256 KB split chunks (intermediate) |
| S3 | `json-s3-geojit` | JSON payloads → triggers Stage 3 |
| S3 | `pdf-s3-geojit-prod` | Final generated PDFs |
| S3 | `geojit-error-bucket-prod` | Invalid records from Stage 1 |
| S3 | `geojit-report-files-s3-prod` | Processing timing reports (CSV) |
| S3 | `geojit-email-templates-dev` | Email templates (`active/`, `drafts/` prefixes) |
| SQS | `geojit-map-split-processing-queue.fifo` | Stage 1→2 hand-off + resend triggers |
| SQS | `geojit-pipeline-status-queue.fifo` | Pipeline status events to API |
| SQS | `geojit-sent-email-queue.fifo` | Sent email tracking |
| SQS | exceptions queue | Lambda error messages; drained by `/api/v1/exceptions` |
| Lambda | `split-lambda-geojit` | Stage 1 splitter (S3 trigger) |
| Lambda | `invoke-lambda-geojit` | Stage 2 orchestrator (SQS trigger) |
| Lambda | `create-pdf-geojit` | PDF renderer (direct invoke) |
| Lambda | `json-lambda-geojit` | JSON→PDF trigger |
| Lambda | `email-notification-geojit` | Email sending via SES |
| Lambda | `pull-bounce-geojit` | Bounce event retrieval |
| Lambda | `pull-delivery-geojit` | Delivery confirmation retrieval |
| Lambda | `status-consumer-geojit` | Status events → API `/pipeline/status-event` |
| SES | 15 config sets | Email delivery configuration (one active at a time) |
| SNS | ses-topic | SES events → `POST /api/v1/webhooks/ses` |
| CloudWatch | `/aws/lambda/{name}` | 8 log groups, one per Lambda |
| Secrets Manager | cert secrets | PFX certificate storage |

---

## Configuration Reference

### application.yml key properties
```yaml
app:
  jwt:
    expiration-ms: 86400000        # 24h dev, 28800000 = 8h prod
    issuer: geojit-contract-note-api
  aws:
    region: ap-south-1
    presigned-url-expiry-hours: 168  # 7 days dev, 24 prod
    s3:
      raw-bucket:      raw-s3-geojit-prod
      chunks-bucket:   chunks-s3-geojit-prod
      pdf-bucket:      pdf-s3-geojit-prod
      report-bucket:   geojit-report-files-s3-prod
      error-bucket:    geojit-error-bucket-prod
      template-bucket: geojit-email-templates-dev
    sqs:
      pipeline-status-queue: geojit-pipeline-status-queue.fifo
      map-split-queue:       geojit-map-split-processing-queue.fifo
      exceptions-queue-url:  ${EXCEPTIONS_QUEUE_URL}
    cloudwatch:
      split-lambda-log-group:   /aws/lambda/split-lambda-geojit
      invoke-lambda-log-group:  /aws/lambda/invoke-lambda-geojit
      pdf-lambda-log-group:     /aws/lambda/create-pdf-geojit
      email-lambda-log-group:   /aws/lambda/email-notification-geojit
      bounce-lambda-log-group:  /aws/lambda/pull-bounce-geojit
      delivery-lambda-log-group: /aws/lambda/pull-delivery-geojit
      status-consumer-log-group: /aws/lambda/status-consumer-geojit
  internal-api-key: ${INTERNAL_API_KEY}   # verified on /pipeline/status-event
  cors.allowed-origins: http://localhost:3000,http://localhost:3001
spring:
  jpa.hibernate.ddl-auto: validate   # Flyway owns schema
  servlet.multipart.max-file-size: 10GB
```

### Profiles
- **local**: `~/.aws/credentials` for AWS; `localhost:5432` PostgreSQL; DEBUG logging; Swagger enabled
- **prod**: `DB_URL/DB_USERNAME/DB_PASSWORD` env vars; IAM role for AWS; WARN logging; Swagger disabled; actuator on `/internal/actuator`

### Lambda `application.properties`
```properties
spring.application.name=GeojitContractNotes
errologBucket=geojit-error-bucket
geojitSentEmailSqs=geojit-sent-email-queue.fifo
```
