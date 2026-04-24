# Lambda Documentation - geojit-contract-notes

## Overview

**Runtime**: Java (AWS Lambda)
**Purpose**: PDF generation and email distribution for contract notes
**Trigger**: SQS messages from Spring Boot API
**Output**: PDFs to S3, emails via AWS SES
**PDF Library**: iText

## Project Structure

This is a Java-based AWS Lambda project with two main versions:
- **v1**: Original implementation (`equity_combinemargin/v1/`)
- **v2**: Updated implementation (`equity_combinemargin/v2/`)

---

## Lambda Functions

### 1. GetJsonLambda
**File**: `geojit_ContractNote/GetJsonLambda.java`
**Trigger**: S3 PUT event
**Purpose**: Read JSON metadata from S3 and initiate processing
**Flow**: S3 → GetJsonLambda → Invoke/Split

---

### 2. Split / Split2
**Files**:
- `geojit_ContractNote/Split.java`
- `geojit_ContractNote/V2/Split2.java`

**Purpose**: Split large customer files into smaller batches
**Input**: Large JSON file with customer data
**Output**: Multiple smaller JSON files for parallel processing
**Use Case**: When a job has 1000+ customers, split into batches of 100

---

### 3. Invoke / Invoke2
**Files**:
- `geojit_ContractNote/Invoke.java`
- `geojit_ContractNote/V2/Invoke2.java`

**Purpose**: Orchestrate PDF generation for a batch of customers
**Input**: SQS message with customer data
**Process**:
1. Read customer metadata
2. For each customer, call PDF generation
3. Upload PDF to S3
4. Send status events back to API

---

### 4. DynamicValuePdf
**File**: `geojit_ContractNote/DynamicValuePdf.java`
**Purpose**: Core PDF generation logic using iText
**Input**: Customer transaction data (JSON)
**Output**: PDF contract note
**Features**:
- Dynamic headers and footers
- Transaction tables
- Margin statements
- Security summaries
- Charge breakdowns

---

### 5. PDF Creation (v1)
**Files**:
- `equity_combinemargin/v1/CreateEquityPdfV10.java`
- `equity_combinemargin/v1/CreateGeoJitEquityPdfV1.java`
- `equity_combinemargin/v1/DynamicValuePDF.java`
- `equity_combinemargin/v1/createpdf.java`

**Purpose**: Version 1 PDF generators
**Process**: Generate equity and margin PDFs from JSON metadata

---

### 6. PDF Creation (v2)
**Files**:
- `equity_combinemargin/v2/TestCN.java`
- `equity_combinemargin/v2/TestAGTS.java`

**Purpose**: Version 2 PDF generators (updated format)
**Improvements**: Enhanced layout, better data handling

---

### 7. AmazonSES
**File**: `geojit_ContractNote/AmazonSES.java`
**Purpose**: Email sending via AWS SES
**Input**: Customer email, PDF S3 key
**Process**:
1. Get active email template from S3
2. Attach PDF from S3
3. Send email via SES
4. Record sesMessageId for tracking

---

### 8. EmailNotification
**File**: `geojit_ContractNote/EmailNotification.java`
**Purpose**: Build and send email notifications
**Features**:
- HTML email templates
- PDF attachments
- Custom metadata for tracking

---

### 9. PullBounceSQS
**File**: `geojit_ContractNote/PullBounceSQS.java`
**Trigger**: SQS queue (bounce events)
**Purpose**: Process email bounce notifications
**Flow**: SES → SNS → SQS → PullBounceSQS → Update status

---

### 10. PullDeliverSQS
**File**: `geojit_ContractNote/PullDeliverSQS.java`
**Trigger**: SQS queue (delivery events)
**Purpose**: Process email delivery confirmations
**Flow**: SES → SNS → SQS → PullDeliverSQS → Update status

---

### 11. StatusConsumerLambda
**File**: `geojit_ContractNote/StatusConsumerLambda.java`
**Trigger**: SQS queue (pipeline events)
**Purpose**: Consume processing status updates
**Process**: Read status events, send to API via HTTP POST

---

### 12. Local Test Runners
**Files**:
- `geojit_ContractNote/LocalTestRunner.java`
- `equity_combinemargin/v1/LocalPDFRunner.java`

**Purpose**: Test Lambda functions locally without deploying
**Use Case**: Development and debugging

---

## Data Models (DTOs)

### Top-Level DTO
**File**: `DTO/GeojitStatementDTO.java`
**Purpose**: Main contract note data structure

### Cash Segment Models
- `CashSegmentDto.java` - Cash market transactions
- `CashSegmentTotalDto.java` - Cash totals

### Derivative Models
- `DerivativeSegmentDto.java` - Futures & options transactions
- `EquitySegmentDto.java` - Equity transactions

### Margin Models
- `DailyMarginDto.java` - Daily margin requirements
- `DailyMarginTotalDto.java` - Margin totals
- `MarginPledgeDto.java` - Pledged securities
- `MarginPledgeTotalDto.java` - Pledge totals

### Obligation Models
- `NetObligationDto.java` - Net obligations per segment
- `NetObligationTotalDto.java` - Total obligations
- `PayInPayOutDto.java` - Payment obligations

### Transaction Models
- `SecurityTransactionDto.java` - Individual trades
- `SecurityTransactionTotalDto.java` - Trade totals
- `ScripSummaryDto.java` - Script-wise summary
- `PositionSummaryDto.java` - Position summary

### Header/Footer Models
- `HeaderDto.java` - Client and broker details
- `DatePlaceDto.java` - Date and place information
- `ExchangeClearingDto.java` - Exchange and clearing info
- `NameClearingCorporationDto.java` - Clearing corporation details
- `NameAndExchangeTotalDto.java` - Exchange totals

---

## V1 Models (equity_combinemargin/v1/Model/)

### Header Types
- `CAHeaderTypeModel.java` - Corporate action header
- `CHeaderTypeModel.java` - Cash segment header
- `DHeaderTypeModel.java` - Derivative header
- `FOHeaderTypeModel.java` - F&O header
- `MHeaderTypeModel.java` - Margin header
- `OHeaderTypeModel.java` - Option header
- `PHeaderTypeModel.java` - Position header
- `SCapitalHeaderTypeModel.java` - Capital market header
- `SFuturesHeaderTypeModel.java` - Futures header
- `SSHeaderTypeModel.java` - Scrip summary header
- `STTHeaderTypeModel.java` - STT (Securities Transaction Tax) header

### Other V1 Models
- `CustomerModel.java` - Customer information
- `DealingOfficeAddress.java` - Branch office details
- `FooterModelV2.java` - PDF footer data

---

## V2 Models (equity_combinemargin/v2/MODEL/)

Updated models with improved structure:

- `CNContractNoteDTO.java` - Main contract note DTO (v2)
- `CNAddressDTO.java` - Address details
- `CNChargesDTO.java` - Brokerage and charges
- `CNFinancialSummaryDTO.java` - Financial summary
- `CNHeaderDTO.java` - Header information
- `CNMarginStatementDTO.java` - Margin statement
- `CNNetObligationDTO.java` - Net obligations
- `CNPledgeSecuritiesDTO.java` - Pledged securities
- `CNSTTDTO.java` - STT details
- `CNScripSummaryDTO.java` - Scrip-wise summary
- `CNSecuritySummaryDTO.java` - Security summary
- `CNTradeDetailsDTO.java` - Trade details

---

## PDF Generation Flow

### Standard Flow

```
1. API sends message to SQS
   ↓
2. Invoke/Invoke2 Lambda triggered
   ↓
3. Read customer data from message
   ↓
4. For each customer:
   a. Load transaction data
   b. Call DynamicValuePdf/CreatePdf
   c. Generate PDF using iText
   d. Upload to S3 (bucket/jobId/partyCode.pdf)
   e. Send status event to API
   ↓
5. AmazonSES Lambda triggered
   ↓
6. Load email template from S3
   ↓
7. Attach PDF and send via SES
   ↓
8. SES sends email
   ↓
9. Email events (bounce/delivery) → SNS → SQS
   ↓
10. PullBounceSQS / PullDeliverSQS process events
    ↓
11. Update job status in API database
```

---

## AWS Services Used

| Service | Purpose |
|---------|---------|
| **S3** | Store input JSON, generated PDFs, email templates |
| **SQS** | Message queues for triggering Lambdas |
| **Lambda** | Serverless execution of PDF generation |
| **SES** | Email delivery with bounce/complaint tracking |
| **SNS** | Email event notifications (bounce, delivery, complaint) |
| **CloudWatch** | Logs and monitoring |
| **IAM** | Permissions for Lambda to access S3, SQS, SES |

---

## S3 Bucket Structure

```
contract-notes-bucket/
├── input/                    # Input JSON files
│   └── {jobId}/
│       └── customers.json
├── output/                   # Generated PDFs
│   └── {jobId}/
│       ├── {partyCode1}.pdf
│       ├── {partyCode2}.pdf
│       └── ...
├── templates/                # Email templates
│   └── template-{id}.html
└── reports/                  # Daily/monthly reports
    └── {date}/
```

---

## SQS Queues

1. **pdf-generation-queue**
   - Trigger: API sends message when job created
   - Consumer: Invoke/Invoke2 Lambda
   - Message: `{ jobId, customers: [...] }`

2. **email-send-queue**
   - Trigger: PDF generation complete
   - Consumer: AmazonSES Lambda
   - Message: `{ jobId, partyCode, email, pdfKey }`

3. **bounce-queue**
   - Trigger: SNS (SES bounce notification)
   - Consumer: PullBounceSQS Lambda
   - Message: SES bounce event JSON

4. **delivery-queue**
   - Trigger: SNS (SES delivery notification)
   - Consumer: PullDeliverSQS Lambda
   - Message: SES delivery event JSON

5. **status-queue**
   - Trigger: Lambda status updates
   - Consumer: StatusConsumerLambda
   - Message: `{ jobId, partyCode, status, timestamp }`

---

## Environment Variables

Lambda functions expect these environment variables:

- `S3_BUCKET` - S3 bucket name
- `API_ENDPOINT` - Spring Boot API URL for callbacks
- `SES_FROM_EMAIL` - Sender email address
- `SES_CONFIG_SET` - SES configuration set name
- `REGION` - AWS region (e.g., ap-south-1)

---

## Error Handling

### Retry Logic
- Lambda retries on failure (up to 3 times)
- Dead Letter Queue (DLQ) for failed messages
- API receives failure events via StatusConsumerLambda

### Error Types
1. **PDF Generation Error**: Invalid data, missing fields
2. **S3 Upload Error**: Permissions, network issues
3. **Email Send Error**: Invalid email, SES limits, suppression list
4. **Data Parsing Error**: Malformed JSON

---

## Performance Optimization

### Batch Processing
- Split large jobs into batches of 100 customers
- Parallel Lambda invocations for faster processing

### Memory Allocation
- PDF generation: 1024 MB (iText is memory-intensive)
- Email sending: 512 MB
- Status updates: 256 MB

### Timeout
- PDF generation: 5 minutes
- Email sending: 1 minute
- Status updates: 30 seconds

---

## File Structure

```
geojit-contract-notes/
├── src/main/java/com/geojit/contractnotes/
│   ├── DTO/
│   │   └── GeojitStatementDTO.java
│   ├── Model/
│   │   ├── CashSegmentDto.java
│   │   ├── DerivativeSegmentDto.java
│   │   ├── EquitySegmentDto.java
│   │   ├── DailyMarginDto.java
│   │   ├── SecurityTransactionDto.java
│   │   ├── HeaderDto.java
│   │   └── ... (20+ model files)
│   ├── geojit_ContractNote/
│   │   ├── GetJsonLambda.java
│   │   ├── Invoke.java
│   │   ├── Split.java
│   │   ├── DynamicValuePdf.java
│   │   ├── AmazonSES.java
│   │   ├── EmailNotification.java
│   │   ├── PullBounceSQS.java
│   │   ├── PullDeliverSQS.java
│   │   ├── StatusConsumerLambda.java
│   │   ├── LocalTestRunner.java
│   │   └── V2/
│   │       ├── Invoke2.java
│   │       ├── Split2.java
│   │       └── CreatePdf.java
│   ├── equity_combinemargin/
│   │   ├── v1/
│   │   │   ├── CreateEquityPdfV10.java
│   │   │   ├── CreateGeoJitEquityPdfV1.java
│   │   │   ├── DynamicValuePDF.java
│   │   │   ├── EquityCombineMarginJsonFileBaseTrigger.java
│   │   │   ├── InvokeEquityCombineMarginFile.java
│   │   │   ├── SplitEquityCombineMarginFile.java
│   │   │   ├── LocalPDFRunner.java
│   │   │   ├── createpdf.java
│   │   │   ├── DTO/
│   │   │   │   └── EquityDtoV2.java
│   │   │   └── Model/
│   │   │       ├── CAHeaderTypeModel.java
│   │   │       ├── CHeaderTypeModel.java
│   │   │       ├── CustomerModel.java
│   │   │       ├── DealingOfficeAddress.java
│   │   │       ├── FooterModelV2.java
│   │   │       └── ... (11 header models)
│   │   └── v2/
│   │       ├── TestCN.java
│   │       ├── TestAGTS.java
│   │       ├── DTO/
│   │       │   └── CNContractNoteDTO.java
│   │       └── MODEL/
│   │           ├── CNAddressDTO.java
│   │           ├── CNChargesDTO.java
│   │           ├── CNFinancialSummaryDTO.java
│   │           ├── CNHeaderDTO.java
│   │           ├── CNTradeDetailsDTO.java
│   │           └── ... (11 CN models)
│   └── utils/
│       └── HeaderFooterPageEventV2.java
└── src/test/java/com/geojit/contractnotes/
    └── GeojitContractNotesApplicationTests.java
```

---

## Key Libraries

- **iText 7** - PDF generation
- **AWS Lambda Java Core** - Lambda runtime
- **AWS SDK for Java** - S3, SES, SQS integration
- **Jackson** - JSON parsing
- **Log4j** - Logging

---

## Deployment

### Build
```bash
mvn clean package
```

### Deploy
```bash
aws lambda update-function-code \
  --function-name InvokePdfGeneration \
  --zip-file fileb://target/contract-notes.jar
```

### Testing Locally
```bash
java -cp target/contract-notes.jar \
  com.geojit.contractnotes.geojit_ContractNote.LocalTestRunner
```

---

## Monitoring and Logging

### CloudWatch Logs
- Log group: `/aws/lambda/{function-name}`
- Retention: 7 days
- Log level: INFO (configurable via environment)

### Metrics
- Invocation count
- Duration
- Error count
- Throttles
- Concurrent executions

### Alarms
- High error rate (>5%)
- Long duration (>3 minutes for PDF generation)
- Throttling events

---

## Business Logic Highlights

### PDF Content Sections

1. **Header**: Client name, PAN, address, broker details
2. **Trade Summary**: All transactions for the day
3. **Segment-wise breakdown**: Cash, F&O, Derivatives
4. **Margin Statement**: Margin requirements and pledges
5. **Charges**: Brokerage, STT, GST, exchange fees
6. **Net Obligation**: Amount payable/receivable
7. **Footer**: Terms and conditions, digital signature

### Email Template Variables

Templates support these placeholders:
- `{{clientName}}` - Customer name
- `{{partyCode}}` - Party code
- `{{tradeDate}}` - Trade date
- `{{pdfUrl}}` - Link to download PDF
- `{{supportEmail}}` - Support contact

---

## Security Considerations

1. **IAM Roles**: Lambda execution role with minimum required permissions
2. **S3 Bucket Policy**: Restrict access to Lambda functions only
3. **SES Sandbox**: Use production mode for unrestricted sending
4. **Encryption**: S3 objects encrypted at rest (SSE-S3 or KMS)
5. **VPC**: Lambda functions may be in VPC for database access

---

## Known Issues and Limitations

1. **iText License**: Check licensing for commercial use
2. **Lambda Timeout**: 15-minute max (PDF generation can be slow for large statements)
3. **SES Limits**: 50 emails/second (use SES sending pool or increase limits)
4. **Cold Start**: First invocation can be slow (~3-5 seconds)
5. **Memory**: Large PDFs (100+ pages) require 1.5GB+ memory

---

## Future Improvements

1. Use Lambda Layers for shared libraries (iText, AWS SDK)
2. Implement Step Functions for complex workflows
3. Add retry logic with exponential backoff
4. Cache email templates in Lambda /tmp
5. Use S3 Transfer Acceleration for large files
6. Implement batching for SES (send multiple emails in one call)
