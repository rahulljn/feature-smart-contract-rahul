# API Documentation - geojit-contract-note-api

## Overview

**Framework**: Spring Boot 3.x
**Language**: Java
**Architecture**: Controller → Service → Repository (3-tier)
**Database**: JPA/Hibernate with relational database
**Security**: JWT (RS256) with Spring Security
**AWS Integration**: S3, SQS, Lambda, SES, SNS

## Entry Point

- **Main Class**: `GeojitContractNoteApplication.java`
- **Base Package**: `com.geojit.contractnote`
- **Port**: Configured in `application.properties` (likely 8080)

## Architecture Layers

### 1. Controllers (REST API)
### 2. Services (Business Logic)
### 3. Repositories (Data Access)
### 4. Entities (Database Models)
### 5. DTOs (Request/Response Objects)

---

## REST API Endpoints

### Authentication - AuthController

**Base Path**: `/api/auth`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| POST | `/api/auth/login` | `login()` | User login, returns JWT token |
| POST | `/api/auth/logout` | `logout()` | User logout (invalidate token) |
| GET | `/api/auth/me` | `me()` | Get current user details |

**Request DTOs**: `LoginRequest`
**Response DTOs**: `AuthResponse`

---

### Dashboard - DashboardController

**Base Path**: `/api/dashboard`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| GET | `/api/dashboard` | `getMetrics()` | Dashboard metrics (jobs, emails, hourly activity) |

**Response DTOs**: `DashboardMetricsResponse`

---

### Jobs - JobController

**Base Path**: `/api/jobs`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| POST | `/api/jobs/upload` | `upload()` | Upload CSV file for processing |
| POST | `/api/jobs/validate` | `validateFile()` | Validate CSV structure without processing |
| GET | `/api/jobs` | `listJobs()` | List all jobs with pagination |
| GET | `/api/jobs/{id}` | `getJob()` | Get single job details |
| GET | `/api/jobs/{id}/customers` | `getJobCustomers()` | Get all customers in a job |
| POST | `/api/jobs/{id}/resend/{partyCode}` | `resendCustomer()` | Resend email to specific customer |
| POST | `/api/jobs/{id}/bulk-resend` | `bulkResend()` | Resend to all failed in job |
| POST | `/api/jobs/bulk-resend-codes` | `bulkResendByCodes()` | Resend to specific party codes |
| POST | `/api/jobs/bulk-resend-bounced` | `bulkResendAllBounced()` | Resend to all bounced emails |
| GET | `/api/jobs/{id}/stream` | `UUID()` + `SseEmitter()` | Server-Sent Events for real-time updates |

**Response DTOs**: `JobResponse`, `JobCustomerResponse`, `ValidationResultResponse`

**Real-time**: Uses Server-Sent Events (SSE) via `broadcastEvent()` and `onPipelineStatusEvent()`

---

### Clients - ClientController

**Base Path**: `/api/clients`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| GET | `/api/clients/search` | `search()` | Search clients by party code or email |
| GET | `/api/clients/{code}/pdfs` | `listPdfs()` | List all PDFs for a client |
| GET | `/api/clients/{code}/reports` | `listReports()` | List reports for a client |
| GET | `/api/clients/{code}/timeline` | `getTimeline()` | Email event timeline for client |
| GET | `/api/clients/{code}/pdf-url` | `getPdfUrl()` | Get presigned S3 URL for PDF |
| PUT | `/api/clients/{code}/email` | `updateEmail()` | Update client email address |

---

### Templates - TemplateController

**Base Path**: `/api/templates`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| GET | `/api/templates` | `getAll()` | List all email templates |
| GET | `/api/templates/active` | `getActive()` | Get currently active template |
| GET | `/api/templates/{id}` | `getById()` | Get template by ID |
| POST | `/api/templates` | `create()` | Create new template |
| PUT | `/api/templates/{id}` | `update()` | Update template |
| PATCH | `/api/templates/{id}/fields` | `updateFields()` | Update specific template fields |
| POST | `/api/templates/{id}/activate` | `activate()` | Set template as active |
| POST | `/api/templates/validate` | `validate()` | Validate template HTML |

**Request DTOs**: `TemplateRequest`, `TemplateFieldsRequest`

---

### Configuration - ConfigController

**Base Path**: `/api/config`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| GET | `/api/config/ses` | `getSesConfigs()` | List SES configurations |
| POST | `/api/config/ses` | `createSesConfig()` | Create SES config |
| PUT | `/api/config/ses/{id}` | `updateSesConfig()` | Update SES config |
| POST | `/api/config/ses/{id}/activate` | `activateSesConfig()` | Activate SES config |
| GET | `/api/config/ses/statistics` | `getSesStatistics()` | Get SES sending statistics |
| GET | `/api/config/certificates` | `getCertificates()` | List all certificates |
| GET | `/api/config/certificates/active` | `getActiveCert()` | Get active certificate |
| POST | `/api/config/certificates/{id}/activate` | `activateCertificate()` | Activate certificate |
| POST | `/api/config/certificates` | `uploadCertificate()` | Upload new certificate |

**Response DTOs**: `SesStatisticsResponse`

---

### Users - UserController

**Base Path**: `/api/users`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| GET | `/api/users` | `getAll()` | List all users |
| POST | `/api/users` | `create()` | Create new user |
| PUT | `/api/users/{id}` | `update()` | Update user details |
| DELETE | `/api/users/{id}` | `deactivate()` | Deactivate user |

**Request DTOs**: `UserRequest`

---

### Audit Log - AuditController

**Base Path**: `/api/audit`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| GET | `/api/audit` | `getAuditLog()` | Get audit log with filters |

---

### Suppression List - SuppressionController

**Base Path**: `/api/suppression`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| GET | `/api/suppression` | `getAll()` | List all suppressed emails |
| POST | `/api/suppression` | `add()` | Add email to suppression list |
| DELETE | `/api/suppression/{id}` | `remove()` | Remove email from suppression |

**Request DTOs**: `SuppressionRequest`

---

### SES Webhook - SesWebhookController

**Base Path**: `/api/webhooks/ses`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| POST | `/api/webhooks/ses` | `handleSns()` | Receive SNS notifications from AWS SES |

**Internal Methods**:
- `confirmSubscription()` - Confirm SNS subscription
- `processNotification()` - Process bounce/delivery/complaint events
- `resolveCustomerViaMetaJson()` - Map email event to customer record

---

### Pipeline Events - PipelineController

**Base Path**: `/api/pipeline`

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| GET | `/api/pipeline/events` | `getEvents()` | Get pipeline events for a job |
| POST | `/api/pipeline/status` | `receiveStatusEvent()` | Receive status updates from Lambda |

---

## Services

### Core Business Logic

| Service | Responsibility |
|---------|---------------|
| **AuthService** | User authentication, JWT token generation |
| **AuditService** | Log all user actions for compliance |
| **ClientService** | Client search, PDF listing, email updates |
| **DashboardService** | Build metrics, hourly activity, recent activity |
| **EmailTemplateService** | Template CRUD, validation, activation |
| **FileValidationService** | Validate CSV structure and content |
| **JobService** | Job creation, listing, customer management |
| **JobTimeoutService** | Auto-complete stuck jobs (scheduled task) |
| **PipelineService** | Process pipeline events, update job status |
| **ResendService** | Resend failed/bounced emails |
| **S3Service** | S3 operations (upload, download, presigned URLs) |
| **SqsService** | Send messages to SQS queues |
| **SesStatisticsService** | Fetch SES sending statistics |
| **SuppressionService** | Manage email suppression list |
| **UserService** | User CRUD operations |

### AWS Integration Services

| Service | AWS Service | Operations |
|---------|-------------|------------|
| **S3Service** | S3 | Upload PDFs, download files, presigned URLs, template storage |
| **SqsService** | SQS | Send messages to processing queues |
| **LambdaService** (via AwsConfig) | Lambda | Invoke Lambda functions |
| **SesService** (via SesStatisticsService) | SES | Get send statistics, manage configurations |

---

## Repositories (Data Access)

### JPA Repositories

| Repository | Entity | Custom Queries |
|------------|--------|----------------|
| **AuditLogRepository** | AuditLog | `findByUserId()`, `findByAction()`, `findByDateRange()` |
| **CertificateRepository** | Certificate | `findByIsActiveTrue()` |
| **EmailEventRepository** | EmailEvent | `findByPartyCode()`, `findBySesMessageId()`, `countByEventType()` |
| **EmailTemplateRepository** | EmailTemplate | `findByIsActiveTrue()`, `existsByName()` |
| **JobRepository** | Job | `findTodaysJobs()`, `findByCreatedAtBetween()`, `findStuckJobs()` |
| **JobCustomerRepository** | JobCustomer | `findByJob_JobId()`, `findByPartyCode()`, `findAllBounced()` |
| **PipelineEventRepository** | PipelineEvent | `findByJobId()`, `findHourlyEventCounts()` |
| **SesConfigRepository** | SesConfig | `findByIsActiveTrueOrderByConfigSetNameAsc()` |
| **SuppressionListRepository** | SuppressionList | `existsByEmail()`, `findAllByOrderByAddedAtDesc()` |
| **UserRepository** | User | `findByEmail()`, `existsByEmail()` |

---

## Database Entities

### Core Entities

| Entity | Description | Key Fields |
|--------|-------------|-----------|
| **User** | System users | `email`, `password`, `role` (ADMIN/OPERATOR), `isActive` |
| **Job** | Batch processing jobs | `jobId`, `fileName`, `status`, `tradeDate`, counters (processed, sent, delivered, bounced) |
| **JobCustomer** | Individual customer records | `partyCode`, `email`, `pdfStatus`, `emailStatus`, `sesMessageId` |
| **EmailEvent** | SES email events | `eventType` (SENT, DELIVERED, BOUNCED, COMPLAINT), `eventTimestamp`, `sesMessageId` |
| **EmailTemplate** | Email templates | `name`, `subject`, `htmlContent`, `isActive` |
| **Certificate** | SSL/TLS certificates | `name`, `s3Key`, `isActive`, `uploadedAt` |
| **SesConfig** | SES configuration sets | `configSetName`, `fromEmail`, `isActive` |
| **SuppressionList** | Blocked emails | `email`, `reason`, `addedAt` |
| **AuditLog** | User action logs | `userId`, `action`, `entity`, `details`, `eventTimestamp` |
| **PipelineEvent** | Processing events | `jobId`, `partyCode`, `eventType`, `eventTimestamp` |

### Enums

- **Job.JobStatus**: `UPLOADED`, `PROCESSING`, `COMPLETED`, `FAILED`
- **JobCustomer.PdfStatus**: `PENDING`, `GENERATED`, `FAILED`
- **JobCustomer.EmailStatus**: `PENDING`, `SENT`, `DELIVERED`, `BOUNCED`, `FAILED`, `SKIPPED`
- **EmailEvent.EventType**: `SENT`, `DELIVERED`, `BOUNCED`, `COMPLAINT`, `REJECTED`
- **PipelineEvent.EventType**: `FILE_UPLOADED`, `PDF_GENERATED`, `EMAIL_SENT`, etc.
- **AuditLog.AuditAction**: `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`
- **User.Role**: `ADMIN`, `OPERATOR`

---

## DTOs (Request/Response)

### Request DTOs

- **LoginRequest**: `{ email, password }`
- **UserRequest**: `{ email, name, role }`
- **TemplateRequest**: `{ name, subject, htmlContent }`
- **TemplateFieldsRequest**: `{ fields: {} }`
- **ResendRequest**: `{ partyCode, jobId }`
- **SuppressionRequest**: `{ email, reason }`

### Response DTOs

- **ApiResponse<T>**: `{ success, message, data }`
- **AuthResponse**: `{ token, user }`
- **JobResponse**: `{ jobId, fileName, status, ... }`
- **JobCustomerResponse**: `{ partyCode, email, pdfStatus, emailStatus, ... }`
- **DashboardMetricsResponse**: `{ totalJobs, totalEmails, hourlyActivity[], recentActivity[] }`
- **SesStatisticsResponse**: `{ sent, delivered, bounced, ... }`
- **ValidationResultResponse**: `{ valid, errors[], warnings[] }`
- **PageResponse<T>**: `{ content[], totalElements, totalPages, ... }`

---

## Security Configuration

### JWT Authentication

**Class**: `JwtTokenProvider`
**Algorithm**: RS256 (asymmetric)
**Token Source**: Private/Public key pair from certificates
**Filter**: `JwtAuthenticationFilter` (validates token on each request)

### Spring Security

**Class**: `SecurityConfig`

**Protected Endpoints**: All `/api/**` except `/api/auth/login`
**Public Endpoints**: `/api/auth/login`, `/api/webhooks/**`
**CORS**: Configured via `CorsConfigurationSource`
**Password Encoding**: BCrypt

**UserDetailsService**: `UserDetailsServiceImpl` loads user from database

---

## AWS Configuration

### AWS Clients

**Class**: `AwsConfig` (production) / `LocalMockAwsConfig` (development)

| Bean | AWS Service | Purpose |
|------|-------------|---------|
| `AmazonS3` | S3 | PDF and file storage |
| `AmazonSQS` | SQS | Message queues for Lambda triggers |
| `AWSLambda` | Lambda | Invoke PDF generation functions |
| `AmazonSimpleEmailService` | SES | Email delivery and stats |

### Application Properties

**Class**: `AppProperties`

**Sections**:
- `aws.s3`: Bucket names, regions
- `aws.sqs`: Queue URLs
- `aws.lambda`: Function ARNs
- `jwt`: Token expiration, certificate paths

---

## Async Processing

### AsyncConfig

**Thread Pool**: Configured for async operations
**Exception Handler**: `AsyncUncaughtExceptionHandler` for async errors

**Async Methods**:
- `@Async` on service methods for background processing
- Used in `JobService`, `ResendService`, `PipelineService`

---

## Scheduled Tasks

### JobTimeoutService

**Method**: `autoCompleteStuckJobs()`
**Schedule**: Runs periodically (e.g., every 5 minutes)
**Purpose**: Find jobs stuck in PROCESSING state and auto-complete them

---

## Exception Handling

### GlobalExceptionHandler

Handles all exceptions globally:

| Exception | HTTP Status | Response |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | `{ message: "Resource not found" }` |
| `ValidationException` | 400 | `{ message: "Validation error" }` |
| `S3OperationException` | 500 | `{ message: "S3 error" }` |
| `AccessDeniedException` | 403 | `{ message: "Access denied" }` |
| `BadCredentialsException` | 401 | `{ message: "Invalid credentials" }` |
| `MaxUploadSizeExceededException` | 413 | `{ message: "File too large" }` |
| `MethodArgumentNotValidException` | 400 | Validation errors |
| `Exception` (generic) | 500 | `{ message: "Internal error" }` |

---

## OpenAPI/Swagger

**Class**: `OpenApiConfig`

**Endpoint**: `/swagger-ui.html` (likely)
**API Docs**: `/v3/api-docs`

**Security Scheme**: Bearer JWT token

---

## Key Business Flows

### 1. Job Upload and Processing

1. User uploads CSV → `JobController.upload()`
2. `JobService.createJob()` creates Job entity
3. File parsed and split into JobCustomer records
4. Messages sent to SQS queue → `SqsService.sendMessage()`
5. Lambda consumes queue and generates PDFs
6. Lambda sends status events → `PipelineController.receiveStatusEvent()`
7. `PipelineService.processStatusEvent()` updates JobCustomer
8. When all processed, `PipelineService.checkAndCompleteJob()` marks Job as COMPLETED

### 2. Email Event Processing

1. SES sends email
2. Email event (bounce/delivery) sent to SNS
3. SNS triggers webhook → `SesWebhookController.handleSns()`
4. `processNotification()` extracts event details
5. `resolveCustomerViaMetaJson()` finds JobCustomer by sesMessageId
6. Updates JobCustomer.emailStatus and creates EmailEvent
7. Updates Job counters (delivered, bounced, etc.)

### 3. Resend Failed Emails

1. User requests resend → `JobController.resendCustomer()`
2. `ResendService.resendForCustomer()` checks if resendable
3. Checks suppression list → `SuppressionService.isSuppressed()`
4. Sends message to SQS → `SqsService.sendMessage()`
5. Lambda processes and sends email

---

## File Structure

```
geojit-contract-note-api/
├── src/main/java/com/geojit/contractnote/
│   ├── GeojitContractNoteApplication.java   # Main entry point
│   ├── config/
│   │   ├── AppProperties.java               # Application configuration
│   │   ├── AsyncConfig.java                 # Async processing
│   │   ├── AwsConfig.java                   # AWS SDK beans
│   │   ├── LocalMockAwsConfig.java          # Local dev mocks
│   │   ├── OpenApiConfig.java               # Swagger/OpenAPI
│   │   └── SecurityConfig.java              # Spring Security + JWT
│   ├── controller/
│   │   ├── AuditController.java
│   │   ├── AuthController.java
│   │   ├── ClientController.java
│   │   ├── ConfigController.java
│   │   ├── DashboardController.java
│   │   ├── JobController.java
│   │   ├── PipelineController.java
│   │   ├── SesWebhookController.java
│   │   ├── SuppressionController.java
│   │   ├── TemplateController.java
│   │   └── UserController.java
│   ├── service/
│   │   ├── AuditService.java
│   │   ├── AuthService.java
│   │   ├── ClientService.java
│   │   ├── DashboardService.java
│   │   ├── EmailTemplateService.java
│   │   ├── FileValidationService.java
│   │   ├── JobService.java
│   │   ├── JobTimeoutService.java
│   │   ├── PipelineService.java
│   │   ├── ResendService.java
│   │   ├── S3Service.java
│   │   ├── SesStatisticsService.java
│   │   ├── SqsService.java
│   │   ├── SuppressionService.java
│   │   └── UserService.java
│   ├── repository/
│   │   ├── AuditLogRepository.java
│   │   ├── CertificateRepository.java
│   │   ├── EmailEventRepository.java
│   │   ├── EmailTemplateRepository.java
│   │   ├── JobCustomerRepository.java
│   │   ├── JobRepository.java
│   │   ├── PipelineEventRepository.java
│   │   ├── SesConfigRepository.java
│   │   ├── SuppressionListRepository.java
│   │   └── UserRepository.java
│   ├── entity/
│   │   ├── AuditLog.java
│   │   ├── Certificate.java
│   │   ├── EmailEvent.java
│   │   ├── EmailTemplate.java
│   │   ├── Job.java
│   │   ├── JobCustomer.java
│   │   ├── PipelineEvent.java
│   │   ├── SesConfig.java
│   │   ├── SuppressionList.java
│   │   └── User.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java
│   │   │   ├── ResendRequest.java
│   │   │   ├── SuppressionRequest.java
│   │   │   ├── TemplateFieldsRequest.java
│   │   │   ├── TemplateRequest.java
│   │   │   └── UserRequest.java
│   │   └── response/
│   │       ├── ApiResponse.java
│   │       ├── AuthResponse.java
│   │       ├── DashboardMetricsResponse.java
│   │       ├── JobCustomerResponse.java
│   │       ├── JobResponse.java
│   │       ├── PageResponse.java
│   │       ├── SesStatisticsResponse.java
│   │       └── ValidationResultResponse.java
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java     # JWT token validation filter
│   │   ├── JwtTokenProvider.java            # Token generation/validation
│   │   └── UserDetailsServiceImpl.java      # Load user for auth
│   └── exception/
│       ├── GlobalExceptionHandler.java      # Global exception handling
│       ├── ResourceNotFoundException.java
│       ├── S3OperationException.java
│       └── ValidationException.java
└── src/main/resources/
    ├── application.properties               # Configuration
    └── application-dev.properties           # Dev config
```

---

## Testing

- **Test Nodes**: 15 tests detected in graph
- Location: Likely in `src/test/java/com/geojit/contractnote/`
- Coverage: Unit tests for services, integration tests for controllers

---

## Dependencies (Maven/Gradle)

Key dependencies:

- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Database
- `spring-boot-starter-security` - Security
- `aws-java-sdk-*` - AWS services
- `jsonwebtoken` (jjwt) - JWT tokens
- `springdoc-openapi` - Swagger docs
- Database driver (PostgreSQL/MySQL)
