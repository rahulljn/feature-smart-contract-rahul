# Cross-Service Dependencies

## Overview

This document maps the integration points between the three main services:
1. **UI** (geojit-contract-note-ui)
2. **API** (geojit-contract-note-api)
3. **Lambda** (geojit-contract-notes)

---

## UI → API Dependencies

### Authentication Flow

| UI Component | API Endpoint | Method | Data Contract |
|--------------|--------------|--------|---------------|
| `login/page.tsx` | `/api/auth/login` | POST | Request: `{ email, password }` <br> Response: `{ token, user }` |
| `login/page.tsx` → Dashboard | `/api/auth/me` | GET | Response: `{ id, email, name, role }` |
| Header (logout) | `/api/auth/logout` | POST | Response: `{ success }` |

---

### Dashboard Page

| UI Component | API Endpoint | Method | Response Data |
|--------------|--------------|--------|---------------|
| `dashboard/page.tsx` | `/api/dashboard` | GET | `DashboardMetricsResponse`: <br> - `totalJobs`, `totalEmails`, `totalDelivered`, `totalBounced` <br> - `hourlyActivity[]` <br> - `recentActivity[]` |

---

### Jobs Management

| UI Component | API Endpoint | Method | Request/Response |
|--------------|--------------|--------|------------------|
| `jobs/page.tsx` | `/api/jobs` | GET | Response: `PageResponse<JobResponse>` with pagination |
| `jobs/[jobId]/page.tsx` | `/api/jobs/{id}` | GET | Response: `JobResponse` (single job) |
| `jobs/[jobId]/page.tsx` | `/api/jobs/{id}/customers` | GET | Response: `PageResponse<JobCustomerResponse>` |
| `jobs/[jobId]/page.tsx` (SSE) | `/api/jobs/{id}/stream` | GET | Server-Sent Events: pipeline updates |
| `process/page.tsx` (validate) | `/api/jobs/validate` | POST | Request: `FormData(file)` <br> Response: `ValidationResultResponse` |
| `process/page.tsx` (upload) | `/api/jobs/upload` | POST | Request: `FormData(file, tradeDate)` <br> Response: `JobResponse` |
| `jobs/[jobId]/page.tsx` (resend) | `/api/jobs/{id}/resend/{partyCode}` | POST | Response: `ApiResponse` |
| `resend/page.tsx` | `/api/jobs/{id}/bulk-resend` | POST | Request: `{ status }` <br> Response: `ApiResponse` |

---

### Client Search

| UI Component | API Endpoint | Method | Response Data |
|--------------|--------------|--------|---------------|
| `clients/page.tsx` | `/api/clients/search?q={query}` | GET | Response: `JobCustomer[]` (matching party codes/emails) |
| `clients/[partyCode]/page.tsx` | `/api/clients/{code}/pdfs` | GET | Response: Array of PDF metadata |
| `clients/[partyCode]/page.tsx` | `/api/clients/{code}/reports` | GET | Response: Array of report files |
| `clients/[partyCode]/page.tsx` | `/api/clients/{code}/timeline` | GET | Response: `EmailEvent[]` (delivery history) |
| `clients/[partyCode]/page.tsx` | `/api/clients/{code}/pdf-url?key={s3Key}` | GET | Response: `{ url }` (presigned S3 URL) |
| `clients/[partyCode]/page.tsx` (edit) | `/api/clients/{code}/email` | PUT | Request: `{ email }` <br> Response: `ApiResponse` |

---

### User Management

| UI Component | API Endpoint | Method | Request/Response |
|--------------|--------------|--------|------------------|
| `users/page.tsx` | `/api/users` | GET | Response: `User[]` |
| `users/page.tsx` (create) | `/api/users` | POST | Request: `UserRequest { email, name, role, password }` <br> Response: `User` |
| `users/page.tsx` (update) | `/api/users/{id}` | PUT | Request: `UserRequest` <br> Response: `User` |
| `users/page.tsx` (delete) | `/api/users/{id}` | DELETE | Response: `ApiResponse` |

---

### Email Templates

| UI Component | API Endpoint | Method | Request/Response |
|--------------|--------------|--------|------------------|
| `templates/page.tsx` | `/api/templates` | GET | Response: `EmailTemplate[]` |
| `templates/page.tsx` | `/api/templates/active` | GET | Response: `EmailTemplate` (currently active) |
| `templates/page.tsx` (create) | `/api/templates` | POST | Request: `TemplateRequest { name, subject, htmlContent }` <br> Response: `EmailTemplate` |
| `templates/page.tsx` (update) | `/api/templates/{id}` | PUT | Request: `TemplateRequest` <br> Response: `EmailTemplate` |
| `templates/page.tsx` (activate) | `/api/templates/{id}/activate` | POST | Response: `ApiResponse` |
| `templates/page.tsx` (validate) | `/api/templates/validate` | POST | Request: `{ htmlContent }` <br> Response: `{ valid, errors[] }` |

---

### Configuration

| UI Component | API Endpoint | Method | Response Data |
|--------------|--------------|--------|---------------|
| `ses-config/page.tsx` | `/api/config/ses` | GET | Response: `SesConfig[]` |
| `ses-config/page.tsx` | `/api/config/ses/statistics` | GET | Response: `SesStatisticsResponse` (sent, delivered, bounced) |
| `ses-config/page.tsx` (create) | `/api/config/ses` | POST | Request: `{ configSetName, fromEmail }` |
| `ses-config/page.tsx` (activate) | `/api/config/ses/{id}/activate` | POST | Response: `ApiResponse` |
| `certificates/page.tsx` | `/api/config/certificates` | GET | Response: `Certificate[]` |
| `certificates/page.tsx` | `/api/config/certificates/active` | GET | Response: `Certificate` |
| `certificates/page.tsx` (upload) | `/api/config/certificates` | POST | Request: `FormData(file, name)` <br> Response: `Certificate` |
| `certificates/page.tsx` (activate) | `/api/config/certificates/{id}/activate` | POST | Response: `ApiResponse` |

---

### Audit and Suppression

| UI Component | API Endpoint | Method | Response Data |
|--------------|--------------|--------|---------------|
| `audit/page.tsx` | `/api/audit` | GET | Response: `PageResponse<AuditLog>` |
| `exceptions/page.tsx` | `/api/suppression` | GET | Response: `SuppressionList[]` |
| `exceptions/page.tsx` (add) | `/api/suppression` | POST | Request: `{ email, reason }` <br> Response: `SuppressionList` |
| `exceptions/page.tsx` (remove) | `/api/suppression/{id}` | DELETE | Response: `ApiResponse` |

---

## API → Lambda Dependencies

### Trigger: SQS Messages

The API sends messages to SQS queues, which trigger Lambda functions.

| API Service | SQS Queue | Lambda Function | Message Payload |
|-------------|-----------|-----------------|-----------------|
| `JobService.createJob()` | `pdf-generation-queue` | `Invoke/Invoke2` | `{ jobId, customers: [{ partyCode, email, metadataS3Key }] }` |
| `ResendService.resendForCustomer()` | `email-send-queue` | `AmazonSES` | `{ jobId, partyCode, email, pdfS3Key, templateId }` |

---

### Callbacks: Lambda → API

Lambda functions call API endpoints for status updates.

| Lambda Function | API Endpoint | Method | Payload |
|-----------------|--------------|--------|---------|
| `StatusConsumerLambda` | `/api/pipeline/status` | POST | `{ jobId, partyCode, eventType, status, timestamp, s3Key }` |
| `Invoke/Invoke2` (PDF complete) | `/api/pipeline/status` | POST | `{ jobId, partyCode, eventType: 'PDF_GENERATED', s3Key }` |
| `AmazonSES` (email sent) | `/api/pipeline/status` | POST | `{ jobId, partyCode, eventType: 'EMAIL_SENT', sesMessageId }` |

---

## Lambda → AWS Services

### S3 Dependencies

| Lambda Function | S3 Operation | Purpose |
|-----------------|--------------|---------|
| `GetJsonLambda` | `s3.getObject()` | Read JSON metadata from S3 |
| `Invoke/Invoke2` | `s3.getObject()` | Read customer data |
| `DynamicValuePdf` | `s3.putObject()` | Upload generated PDF |
| `AmazonSES` | `s3.getObject()` | Read email template HTML |
| `AmazonSES` | `s3.getObject()` | Read PDF for attachment |

**S3 Bucket**: `contract-notes-bucket` (configured via environment variable)

**Paths**:
- Input JSON: `s3://bucket/input/{jobId}/customers.json`
- Output PDFs: `s3://bucket/output/{jobId}/{partyCode}.pdf`
- Templates: `s3://bucket/templates/template-{id}.html`

---

### SES Dependencies

| Lambda Function | SES Operation | Purpose |
|-----------------|---------------|---------|
| `AmazonSES` | `ses.sendEmail()` | Send email with PDF attachment |
| `EmailNotification` | `ses.sendEmail()` | Send notification emails |

**Configuration**:
- From Email: Configured in `SesConfig` (retrieved from API)
- Configuration Set: For tracking bounces/deliveries

---

### SQS Dependencies (Consumers)

| Lambda Function | SQS Queue | Event Type |
|-----------------|-----------|------------|
| `Invoke/Invoke2` | `pdf-generation-queue` | Process customer batch for PDF generation |
| `AmazonSES` | `email-send-queue` | Send email to customer |
| `PullBounceSQS` | `bounce-queue` | Process bounce notifications from SNS |
| `PullDeliverSQS` | `delivery-queue` | Process delivery confirmations from SNS |
| `StatusConsumerLambda` | `status-queue` | Aggregate status updates |

---

## AWS SNS → API Webhooks

### Email Event Flow

1. **SES** sends email
2. Email event occurs (delivery, bounce, complaint)
3. **SNS** receives event from SES
4. **SNS** sends POST to API webhook

| SNS Topic | API Endpoint | Handler |
|-----------|--------------|---------|
| `ses-bounce-topic` | `/api/webhooks/ses` | `SesWebhookController.handleSns()` |
| `ses-delivery-topic` | `/api/webhooks/ses` | `SesWebhookController.handleSns()` |
| `ses-complaint-topic` | `/api/webhooks/ses` | `SesWebhookController.handleSns()` |

**Webhook Payload** (SNS):
```json
{
  "Type": "Notification",
  "MessageId": "...",
  "TopicArn": "arn:aws:sns:...",
  "Message": "{...SES event JSON...}",
  "Timestamp": "...",
  "SignatureVersion": "1",
  "Signature": "..."
}
```

**API Processing**:
1. `handleSns()` receives SNS POST
2. `confirmSubscription()` if Type=SubscriptionConfirmation
3. `processNotification()` parses SES event
4. `resolveCustomerViaMetaJson()` finds customer by `sesMessageId`
5. Updates `JobCustomer.emailStatus` and creates `EmailEvent`
6. Updates `Job` counters (delivered, bounced, etc.)

---

## Shared Data Contracts

### Job Status Enum
**Defined in**: API (`Job.JobStatus`)
**Used by**: UI, Lambda (via status events)

Values: `UPLOADED`, `PROCESSING`, `COMPLETED`, `FAILED`

---

### Email Status Enum
**Defined in**: API (`JobCustomer.EmailStatus`)
**Used by**: UI, Lambda, SNS webhooks

Values: `PENDING`, `SENT`, `DELIVERED`, `BOUNCED`, `FAILED`, `SKIPPED`

---

### PDF Status Enum
**Defined in**: API (`JobCustomer.PdfStatus`)
**Used by**: UI, Lambda

Values: `PENDING`, `GENERATED`, `FAILED`

---

### Event Type Enum
**Defined in**: API (`PipelineEvent.EventType`)
**Used by**: Lambda (status updates)

Values:
- `FILE_UPLOADED`
- `VALIDATION_COMPLETED`
- `PDF_GENERATION_STARTED`
- `PDF_GENERATED`
- `EMAIL_QUEUED`
- `EMAIL_SENT`
- `EMAIL_DELIVERED`
- `EMAIL_BOUNCED`
- `EMAIL_FAILED`

---

## Authentication Flow

### JWT Token Usage

1. **UI Login** → API: `POST /api/auth/login`
2. API returns JWT token (RS256 signed)
3. UI stores token in `localStorage`
4. **Every UI request** includes token in header:
   ```
   Authorization: Bearer {token}
   ```
5. API validates token via `JwtAuthenticationFilter`
6. Token contains: `{ userId, email, role, exp }`

**Certificate Dependency**:
- API uses active certificate from `Certificate` table
- Private key signs tokens
- Public key validates tokens
- UI does NOT need certificate (stateless validation)

---

## Real-Time Updates (SSE)

### Job Detail Updates

1. **UI** connects to SSE: `GET /api/jobs/{id}/stream`
2. **API** creates `SseEmitter` for client
3. **Lambda** sends status event → API: `POST /api/pipeline/status`
4. **API** `onPipelineStatusEvent()` broadcasts to all connected clients
5. **UI** receives update and refreshes job customer table

**Event Format**:
```json
{
  "type": "PDF_GENERATED",
  "partyCode": "ABC123",
  "status": "COMPLETED",
  "timestamp": "2026-04-22T10:30:00Z"
}
```

---

## Error Propagation

### UI Error Handling

All API errors follow this format:
```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2026-04-22T10:30:00Z"
}
```

**HTTP Status Codes**:
- `400` - Validation error
- `401` - Unauthorized (invalid token)
- `403` - Forbidden (insufficient permissions)
- `404` - Resource not found
- `413` - File too large
- `500` - Internal server error

---

### Lambda Error Handling

**Lambda Errors** → API:

1. Lambda catches exception
2. Sends status event: `{ eventType: 'PDF_FAILED', error: '...' }`
3. API updates `JobCustomer.pdfStatus = FAILED`
4. API increments `Job.failedCount`
5. UI displays error in job detail table

**SQS DLQ (Dead Letter Queue)**:
- Failed messages moved to DLQ after 3 retries
- CloudWatch alarm triggers on DLQ messages
- Manual investigation required

---

## S3 Presigned URLs

### PDF Download Flow

1. **UI** requests PDF download
2. **UI** → API: `GET /api/clients/{code}/pdf-url?key={s3Key}`
3. **API** `S3Service.generatePresignedUrl()` creates temporary URL (expires in 5 minutes)
4. **API** returns: `{ url: "https://s3.amazonaws.com/...?X-Amz-Signature=..." }`
5. **UI** opens URL in new tab or downloads file

**Security**: URLs expire after 5 minutes, no authentication required for presigned URLs

---

## Dependency Summary Table

| Source | Target | Integration Method | Data Format |
|--------|--------|-------------------|-------------|
| UI | API | REST (JSON) | Request/Response DTOs |
| UI | API | SSE | Text events |
| API | Lambda | SQS | JSON messages |
| Lambda | API | HTTP POST | JSON status events |
| Lambda | S3 | AWS SDK | Binary (PDFs), JSON (metadata) |
| Lambda | SES | AWS SDK | MIME email |
| SES | SNS | AWS Event | JSON event |
| SNS | API | HTTP POST (webhook) | SNS envelope + SES event |
| API | S3 | AWS SDK | Binary, presigned URLs |

---

## Database Dependencies

### API Database Entities Referenced by UI

All UI pages rely on these database entities (via API):

- **User** - Authentication, user management
- **Job** - Job listing, detail
- **JobCustomer** - Customer records, resend operations
- **EmailEvent** - Timeline, delivery tracking
- **EmailTemplate** - Template management
- **Certificate** - Certificate management
- **SesConfig** - SES configuration
- **SuppressionList** - Email blocking
- **AuditLog** - Audit trail
- **PipelineEvent** - Dashboard metrics

---

## Configuration Dependencies

### Environment Variables

**UI** (Next.js):
- `NEXT_PUBLIC_API_URL` - API base URL (e.g., `http://localhost:8080`)

**API** (Spring Boot):
- `AWS_REGION` - AWS region
- `S3_BUCKET_NAME` - S3 bucket for PDFs
- `SQS_QUEUE_URL` - SQS queue for Lambda triggers
- `DATABASE_URL` - Database connection
- `JWT_PRIVATE_KEY_PATH` - Path to private key
- `JWT_PUBLIC_KEY_PATH` - Path to public key

**Lambda**:
- `S3_BUCKET` - S3 bucket name
- `API_ENDPOINT` - API URL for callbacks (e.g., `https://api.example.com`)
- `SES_FROM_EMAIL` - Default sender email
- `SES_CONFIG_SET` - SES configuration set
- `REGION` - AWS region

---

## Key Integration Points Summary

1. **File Upload**: UI → API → Database → SQS → Lambda
2. **PDF Generation**: Lambda → S3 → Status Event → API → Database → SSE → UI
3. **Email Sending**: Lambda → SES → SNS → API Webhook → Database
4. **Client Search**: UI → API → Database → S3 (presigned URLs)
5. **Real-time Updates**: Lambda → API → SSE → UI
6. **Authentication**: UI → API → Database (JWT validation)

---

## Critical Dependencies

These dependencies are **required** for the system to function:

1. **AWS S3** - Without S3, PDFs cannot be stored
2. **AWS SQS** - Without SQS, Lambda cannot be triggered
3. **AWS SES** - Without SES, emails cannot be sent
4. **Database** - Without DB, no job/customer tracking
5. **JWT Certificate** - Without cert, authentication fails
6. **Active Email Template** - Without template, emails cannot be sent

---

## Optional Dependencies

These improve the system but are not critical:

1. **SNS Webhooks** - System works without bounce tracking
2. **CloudWatch** - Monitoring is optional
3. **SSE** - UI can poll instead of real-time updates
4. **Audit Logs** - System works without compliance logging
5. **Suppression List** - Can skip checking and send to all
