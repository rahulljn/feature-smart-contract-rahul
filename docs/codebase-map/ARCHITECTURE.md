# System Architecture

## Overview

This is a **contract note distribution system** built as a 3-tier monorepo:

1. **geojit-contract-note-ui** (React/Next.js UI)
2. **geojit-contract-note-api** (Spring Boot REST API)
3. **geojit-contract-notes** (Java-based AWS Lambda functions for PDF generation)

## High-Level Data Flow

```
User Upload (CSV)
    → UI (Next.js)
    → API (Spring Boot)
    → Lambda (PDF Generation + Email via SES)
    → S3 Storage
    → Email Delivery (AWS SES)
    → Webhook Processing (SNS → API)
```

## Architecture Components

### 1. Frontend Layer (geojit-contract-note-ui)
- **Framework**: Next.js 15 with TypeScript, React 19
- **State**: React hooks (useState, useEffect)
- **Styling**: Tailwind CSS
- **API Communication**: Fetch API to Spring Boot endpoints
- **Entry Point**: `src/app/page.tsx`

### 2. API Layer (geojit-contract-note-api)
- **Framework**: Spring Boot 3.x
- **Architecture**: Controller → Service → Repository
- **Database**: JPA/Hibernate (likely PostgreSQL)
- **Security**: JWT authentication with RS256 signing
- **AWS Integration**: S3, SQS, Lambda, SES
- **Entry Point**: `GeojitContractNoteApplication.java`

### 3. Lambda Layer (geojit-contract-notes)
- **Runtime**: Java (AWS Lambda)
- **Purpose**: PDF generation from JSON metadata
- **Libraries**: iText for PDF creation
- **Triggers**: SQS messages from API
- **Output**: PDFs to S3, email via SES

## Communication Flow

### Job Processing Pipeline

1. **Upload Phase**
   - UI uploads CSV via `POST /api/jobs/upload`
   - API validates file structure
   - Stores metadata in database (Job entity)

2. **Processing Phase**
   - API splits CSV into customer records (JobCustomer entities)
   - Sends messages to SQS queue
   - Lambda consumes queue and generates PDFs
   - PDFs uploaded to S3

3. **Distribution Phase**
   - Lambda sends emails via AWS SES
   - Email events (delivery, bounce, complaint) sent to SNS
   - API webhook receives SNS notifications
   - Updates JobCustomer status in database

4. **Monitoring Phase**
   - Real-time updates via Server-Sent Events (SSE)
   - Dashboard displays metrics and job status
   - Audit logs track all actions

## AWS Services Used

| Service | Purpose |
|---------|---------|
| **S3** | PDF storage, template storage |
| **SQS** | Message queue for PDF generation tasks |
| **Lambda** | Serverless PDF generation |
| **SES** | Email delivery (bounces, complaints, deliveries) |
| **SNS** | Email event notifications to API webhook |

## Database Schema (Entities)

- **User**: System users with roles (ADMIN, OPERATOR)
- **Job**: Batch upload jobs with status tracking
- **JobCustomer**: Individual customer records per job
- **EmailEvent**: SES delivery events (sent, delivered, bounced, etc.)
- **EmailTemplate**: Email templates with HTML content
- **Certificate**: SSL/TLS certificates for signing
- **SesConfig**: SES configuration sets
- **SuppressionList**: Blocked email addresses
- **AuditLog**: All user actions
- **PipelineEvent**: Processing pipeline events

## Key Entry Points

### UI Entry Points
- `src/app/page.tsx` → Home redirect
- `src/app/login/page.tsx` → Login page
- `src/app/(dashboard)/layout.tsx` → Main dashboard layout
- `src/app/(dashboard)/jobs/page.tsx` → Jobs management
- `src/app/(dashboard)/clients/[id]/page.tsx` → Client detail view

### API Entry Points (Controllers)
- `AuthController` → `/api/auth/*` - Authentication
- `JobController` → `/api/jobs/*` - Job management
- `ClientController` → `/api/clients/*` - Client search and PDFs
- `DashboardController` → `/api/dashboard` - Metrics
- `TemplateController` → `/api/templates/*` - Email templates
- `ConfigController` → `/api/config/*` - SES and certificates
- `SesWebhookController` → `/api/webhooks/ses` - SNS notifications
- `UserController` → `/api/users/*` - User management

### Lambda Entry Points
- `GetJsonLambda` → S3 trigger for JSON processing
- `Split` / `Split2` → Splitting large files
- `Invoke` / `Invoke2` → PDF generation orchestration
- `DynamicValuePdf` → PDF creation with iText
- `PullBounceSQS` → SQS consumer for bounced emails
- `PullDeliverSQS` → SQS consumer for delivered emails
- `StatusConsumerLambda` → Pipeline status updates
- `AmazonSES` → SES integration
- `EmailNotification` → Email sending logic

## Communities (Graph Analysis)

The codebase is organized into 19 detected communities:

| Community | Size | Purpose |
|-----------|------|---------|
| **model-type** | 1028 nodes | DTOs, models, types (largest community) |
| **service-find** | 311 nodes | Spring Boot services, controllers, repos |
| **ui-dropdown** | 77 nodes | UI components and pages |
| **service-token** | 22 nodes | JWT token handling |
| **jobid-job** | 16 nodes | Job-related logic |
| **clients-page** | 15 nodes | Client management UI |
| **layout-handle** | 13 nodes | UI layout components |

## Critical Execution Flows

Top flows by criticality:

1. **UsersPage** (criticality: 0.67) - User management
2. **LoginPage** (criticality: 0.62) - Authentication
3. **validateFile** (criticality: 0.61) - File validation
4. **CertificatesPage** (criticality: 0.595) - Certificate management
5. **DashboardLayout** (criticality: 0.595) - Main layout

## Technology Stack

### Frontend
- Next.js 15
- React 19
- TypeScript
- Tailwind CSS
- Heroicons

### Backend
- Spring Boot 3.x
- Spring Security (JWT)
- Spring Data JPA
- AWS SDK for Java
- OpenAPI/Swagger

### Lambda
- Java 11/17
- iText PDF library
- AWS Lambda SDK
- Jackson JSON

### Infrastructure
- AWS S3, SQS, Lambda, SES, SNS
- Database (likely PostgreSQL/MySQL)

## Key Design Patterns

1. **MVC Pattern**: Controllers → Services → Repositories
2. **Event-Driven**: SNS/SQS for async processing
3. **Microservices**: Lambda functions for specific tasks
4. **JWT Auth**: Stateless authentication
5. **Repository Pattern**: Data access abstraction
6. **DTO Pattern**: Request/response objects

## Cross-Cutting Concerns

- **Security**: JWT authentication, CORS configuration
- **Error Handling**: Global exception handler
- **Logging**: Audit logs for compliance
- **Async Processing**: SQS + Lambda
- **File Validation**: CSV structure validation
- **Email Suppression**: Bounce/complaint handling
