# Geojit Contract Notes - Architecture Overview

## Table of Contents
1. [System Overview](#system-overview)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Data Flow Pipeline](#data-flow-pipeline)
5. [Model Classes](#model-classes)
6. [Design Patterns](#design-patterns)
7. [AWS Integration](#aws-integration)
8. [Dependencies](#dependencies)
9. [Configuration](#configuration)
10. [Data Formats](#data-formats)

---

## System Overview

**Project Type:** AWS Lambda-based serverless Java application for financial contract note generation

**Technology Stack:**
- Java 1.8
- Spring Boot 2.7.18
- Maven Build System
- AWS Lambda + S3 + SQS
- iText 7 (PDF Generation)

**Purpose:** Automated processing system that converts raw pipe-delimited trading data into professionally formatted PDF contract notes for Geojit Financial Services customers.

---

## Project Structure

```
geojit-contract-notes/
├── pom.xml                              # Maven configuration
├── dependency-reduced-pom.xml           # Shaded JAR dependencies
├── src/
│   ├── main/
│   │   ├── java/com/geojit/contractnotes/
│   │   │   ├── equity_combinemargin/
│   │   │   │   ├── Model/               # 14 domain model classes
│   │   │   │   ├── DTO/                 # Data transfer objects
│   │   │   │   ├── SplitEquityCombineMarginFile.java
│   │   │   │   ├── InvokeEquityCombineMarginFile.java
│   │   │   │   ├── EquityCombineMarginJsonFileBaseTrigger.java
│   │   │   │   ├── DynamicValuePDF.java
│   │   │   │   └── DynamicValuePDF_Shivraj.java
│   │   │   └── utils/
│   │   │       └── HeaderFooterPageEventV2.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── GeoJit_IMG.png          # Company logo
│   │       └── input.json              # Sample test data
│   └── test/                            # Test files
└── target/                               # Build output
```

---

## Core Components

### 1. Lambda Handler Classes (Entry Points)

#### **SplitEquityCombineMarginFile.java**
**File:** `src/main/java/com/geojit/contractnotes/equity_combinemargin/SplitEquityCombineMarginFile.java`

**Role:** Stage 1 - File Splitter and Validator

**Trigger:** S3 upload event to `raw-s3-geojit` bucket

**Responsibilities:**
- Downloads raw contract note files from S3
- Validates data completeness (7 mandatory record types per party)
  - H (Header/Customer)
  - A (Address)
  - D (Detail/Trades)
  - O (Order Summary)
  - F (Footer/Charges)
  - S-CAPITAL (Capital Market Summary)
  - S-FUTURES (Derivatives Summary)
- Splits large files into 256KB chunks by party code
- Uploads valid chunks to `chunks-s3-geojit`
- Routes invalid records to `geojit-error-bucket`
- Publishes chunk metadata to SQS FIFO queue
- Creates process start-time CSV report

#### **InvokeEquityCombineMarginFile.java**
**File:** `src/main/java/com/geojit/contractnotes/equity_combinemargin/InvokeEquityCombineMarginFile.java`

**Role:** Stage 2 - Chunk Processor and JSON Builder

**Trigger:** SQS message from `geojit-map-split-processing-queue.fifo`

**Responsibilities:**
- Polls SQS FIFO queue for chunk metadata
- Downloads chunks from S3
- Parses pipe-delimited records into 13 DTO types
- Groups records by party code (resets on each H record)
- Builds consolidated JSON payloads per customer
- Uploads JSON files to `json-s3-geojit` (triggers PDF generation)
- Implements 11-minute time window with overflow handling
- Rate-limits processing at 118 invocations/second (Guava RateLimiter)
- Appends end-time to process report CSV

#### **EquityCombineMarginJsonFileBaseTrigger.java**
**File:** `src/main/java/com/geojit/contractnotes/equity_combinemargin/EquityCombineMarginJsonFileBaseTrigger.java`

**Role:** Stage 3 - JSON to PDF Converter

**Trigger:** S3 upload event to `json-s3-geojit` bucket

**Responsibilities:**
- Downloads JSON from S3
- Deserializes to EquityDtoV2 object
- Initializes PDF fonts (Calibri regular and bold)
- Invokes PDF generator
- Uploads final PDF to `pdf-s3-geojit/pdfs/{contractNo}/`
- Cleans up temporary files

#### **DynamicValuePDF_Shivraj.java** (Active PDF Generator)
**File:** `src/main/java/com/geojit/contractnotes/equity_combinemargin/DynamicValuePDF_Shivraj.java`

**Role:** PDF Layout Engine (Latest Version)

**Responsibilities:**
- Generates multi-page contract note PDFs (2063 lines of layout logic)
- Creates 3-page document structure:
  - Page 1: Main contract note
  - Page 2: Trade annexure/details
  - Page 3: Margin statement
- Uses iText 7 for advanced table layouts and formatting
- Applies Geojit branding and styling

#### **DynamicValuePDF.java** (Legacy PDF Generator)
**File:** `src/main/java/com/geojit/contractnotes/equity_combinemargin/DynamicValuePDF.java`

**Role:** Earlier PDF generation implementation

**Note:** Contains local testing main() method for development

### 2. Utility Classes

#### **HeaderFooterPageEventV2.java**
**File:** `src/main/java/com/geojit/contractnotes/utils/HeaderFooterPageEventV2.java`

**Role:** PDF Page Event Handler

**Responsibilities:**
- Adds Geojit logo and branding to every page
- Inserts company registration details
- Adds page numbers
- Displays client code on each page

---

## Data Flow Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│           RAW CONTRACT NOTE FILE                            │
│           (Pipe-delimited text file)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │ Upload to S3
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 1: SplitEquityCombineMarginFile                      │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Trigger: S3 Event (raw-s3-geojit)                  │     │
│  │                                                     │     │
│  │ Process:                                            │     │
│  │  1. Download raw file from S3                      │     │
│  │  2. Validate 7 mandatory record types per party    │     │
│  │  3. Split into 256KB chunks by party code          │     │
│  │  4. Create chunk metadata                          │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  Outputs:                                                    │
│   • Valid chunks → chunks-s3-geojit                         │
│   • Invalid records → geojit-error-bucket                   │
│   • Chunk metadata → SQS FIFO queue                         │
│   • Start-time CSV → geojit-report-files-s3                 │
└──────────────────────┬───────────────────────────────────────┘
                       │ SQS FIFO Queue Messages
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 2: InvokeEquityCombineMarginFile                     │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Trigger: SQS Poll (FIFO Queue)                     │     │
│  │                                                     │     │
│  │ Process:                                            │     │
│  │  1. Poll SQS for chunk metadata                    │     │
│  │  2. Download chunk from S3                         │     │
│  │  3. Parse pipe-delimited records                   │     │
│  │  4. Group by party code (13 DTO types)             │     │
│  │  5. Build JSON payload per customer                │     │
│  │  6. Rate-limit at 118 invocations/sec              │     │
│  │  7. Handle 11-minute time window overflow          │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  Outputs:                                                    │
│   • JSON payloads → json-s3-geojit (per party)              │
│   • Overflow chunks → chunks-s3-geojit (requeue)            │
│   • End-time CSV update → geojit-report-files-s3            │
└──────────────────────┬───────────────────────────────────────┘
                       │ S3 Upload Event
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  STAGE 3: EquityCombineMarginJsonFileBaseTrigger            │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Trigger: S3 Event (json-s3-geojit)                 │     │
│  │                                                     │     │
│  │ Process:                                            │     │
│  │  1. Download JSON from S3                          │     │
│  │  2. Deserialize to EquityDtoV2                     │     │
│  │  3. Initialize fonts (Calibri regular/bold)        │     │
│  │  4. Call DynamicValuePDF_Shivraj.generatePDF()     │     │
│  │  5. Upload to S3                                   │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  Output:                                                     │
│   • Final PDF → pdf-s3-geojit/pdfs/{contractNo}/            │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
            ┌──────────────────────┐
            │  CONTRACT NOTE PDF   │
            │  (3-page document)   │
            └──────────────────────┘
```

---

## Model Classes

All models are located in: `src/main/java/com/geojit/contractnotes/equity_combinemargin/Model/`

### Domain Models (14 Classes)

| Class | Record Type | Purpose | Key Fields |
|-------|-------------|---------|------------|
| **CustomerModel.java** | H | Customer information | partycode, clientCode, name, address, contractNo, PAN, date, email, mobile, GST, IRN |
| **DealingOfficeAddress.java** | A | Broker office address | dealingAddress, gstLocation, dealingOfficeNo, gstNo |
| **DHeaderTypeModel.java** | D | Individual trade details | exchange, segment, orderno, trade_no, security_contract_description, buy_sell, qty, market_rate, brokerage, net_rate, net_total, settlementNo |
| **OHeaderTypeModel.java** | O | Order summary by security | segment, securityDescription, buyQty, buyRate, sellQty, sellRate, netQty, netRate, amount, stt |
| **SCapitalHeaderTypeModel.java** | S-CAPITAL | Capital market summary | isin, securityDescription, buyQty, buyWap, buyBrokerage, sellQty, sellWap, netQty, netObligationIsin |
| **SFuturesHeaderTypeModel.java** | S-FUTURES | Derivatives summary | contractDesc, tradeType, tradeQty, tradeWap, tradeBrokerage, tradeNetTotal |
| **FOHeaderTypeModel.java** | F | Futures & Options exchange summary | exchange, segment, product, turnover, noOfTrades, totalBrokerage, netBrokerage, totalStampDuty, netGst |
| **FooterModelV2.java** | F | Exchange-wise financial summary | exchange, segment, instrument, pay_in_pay_out_obligation, securities_transaction_tax, sgst, cgst, igst, tds, exchange_transaction_charges, sebi_fee, stampduty |
| **SSHeaderTypeModel.java** | SS | Stock-wise summary | securityDescription, tradeType, tradeQty, grossRate, grossTotal, brokerage, netRate, netAmount |
| **STTHeaderTypeModel.java** | STT | Securities Transaction Tax | securityDescription, exchange, segment, expDate, futureSale, futureStt, optionSale, optionStt, totalStt |
| **MHeaderTypeModel.java** | M | Margin statement | exchange, segment, date, payInPayOut, mtmProfit, premium, total, debits, balance |
| **PHeaderTypeModel.java** | P | Pledged securities | securityDescription, qty, totalValue, haircutValue, balanceAmount |
| **CHeaderTypeModel.java** | C | Charges breakdown | segment, scrip_name, buy_qty, buy_market_rate, sell_qty, brokerage, stt, other_charges |
| **CAHeaderTypeModel.java** | CA | Corporate actions (optional) | - |

### Data Transfer Object

**EquityDtoV2.java** (`src/main/java/com/geojit/contractnotes/equity_combinemargin/DTO/EquityDtoV2.java`)
- Aggregates all 13 model lists
- Uses Jackson annotations for JSON serialization
- Represents complete customer contract note data

---

## Design Patterns

1. **Pipeline Pattern**
   - Three-stage processing: Split → Parse → Generate
   - Each stage isolated with clear inputs/outputs

2. **Event-Driven Architecture**
   - S3 upload events trigger processing
   - SQS messages enable asynchronous orchestration

3. **Data Transfer Object (DTO)**
   - EquityDtoV2 centralizes all models for JSON transfer
   - Clean separation between domain models and data transfer

4. **Factory Pattern**
   - Font initialization before PDF generation
   - Resource management for PDF components

5. **Batch Processing**
   - 256KB chunks enable distributed processing
   - Parallel Lambda executions for scalability

6. **Rate Limiting**
   - Guava RateLimiter (118 invocations/sec)
   - Prevents downstream throttling and quota exhaustion

7. **Time-Window Processing**
   - 11-minute processing windows
   - Overflow handling for large datasets

8. **FIFO Queue Pattern**
   - Ordered processing per source file
   - Message deduplication for reliability

---

## AWS Integration

### S3 Buckets

| Bucket | Purpose | Lifecycle |
|--------|---------|-----------|
| `raw-s3-geojit` | Raw pipe-delimited input files | Trigger for Stage 1 |
| `chunks-s3-geojit` | 256KB chunks by party code | Intermediate storage |
| `json-s3-geojit` | Per-customer JSON payloads | Trigger for Stage 3 |
| `pdf-s3-geojit` | Final contract note PDFs | Final output |
| `geojit-error-bucket` | Invalid/incomplete records | Error tracking |
| `geojit-report-files-s3` | Process timing CSV reports | Monitoring |

### SQS Queue

- **Queue:** `geojit-map-split-processing-queue.fifo`
- **Type:** FIFO (First-In-First-Out)
- **Purpose:** Ordered chunk processing with deduplication
- **Message Content:** Chunk metadata (S3 keys, party codes)

### Lambda Functions

- **create-pdf-geojit** - PDF generation function
- **invoke-lambda-geojit** - Async invocation handler

### AWS SDK Usage

- **AWS SDK v1 (1.11.522):** S3, Lambda, SQS, SNS, SES, CloudWatch
- **AWS SDK v2 (2.25.26):** SQS
- **AWS SDK v2 (2.15.53):** SESv2

---

## Dependencies

### PDF Generation
- **iText 7:** 7.2.5 (kernel, layout, io) - Main PDF library
- **iText 5:** 5.5.11 - Legacy support
- **Apache PDFBox:** 2.0.29

### Data Processing
- **Jackson:** 2.14.2 - JSON serialization/deserialization
- **OpenCSV:** 5.1 - CSV report generation
- **JSON-Simple:** 1.1 - Lightweight JSON parsing
- **org.json:** 20180130

### Security & Encryption
- **BouncyCastle:** 1.49 (bcprov, bcpkix) - Digital signatures

### Utilities
- **Joda-Time:** 2.10.5 - Date/time handling
- **Apache HttpClient:** 4.5.10
- **OkHttp3:** 4.10.0
- **Guava:** 19.0 - Rate limiting
- **JSoup:** 1.12.1 - HTML parsing

### Framework
- **Spring Boot:** 2.7.18
- **Spring Cloud Function:** 3.2.11

---

## Configuration

### Build Configuration
- **File:** `pom.xml`
- **Build Tool:** Maven
- **Java Version:** 1.8
- **Packaging:** Shaded JAR (uber-JAR with all dependencies)
- **Main Class:** AWS Lambda handler (no explicit main)

### Application Properties
**File:** `src/main/resources/application.properties`

```properties
spring.application.name=GeojitContractNotes
errologBucket=geojit-error-bucket
```

### Resource Files
- `GeoJit_IMG.png` - Company logo for PDF headers
- `calibri-400.ttf` - Regular font
- `calibri-Bold.ttf` - Bold font
- `input.json` - Sample test data

---

## Data Formats

### Input Format
**Type:** Pipe-delimited text file

**Structure:**
```
PartyCode|RecordType|Field1|Field2|...|FieldN
```

**Example Records:**
```
PLV083|H|0003546|JOHN DOE|123 STREET|CN123456|ABCDE1234F|01-Jan-2024|john@example.com|9876543210|29AABCT1234H1Z1|IRN123
PLV083|A|Geojit Office Address|Location|12345|29AABCT1234H1Z1
PLV083|D|NSE|CAPITAL|ORD123|TRD456|RELIANCE|BUY|100|2500.00|10.00|2490.00|249000.00|SETT001
```

### Intermediate Format (JSON)
**Type:** JSON (EquityDtoV2 serialized)

**Structure:**
```json
{
  "customerModelList": [...],
  "dealingOfficeAddressList": [...],
  "dheaderTypeModelList": [...],
  "oheaderTypeModelList": [...],
  "sCapitalHeaderTypeModelList": [...],
  "sFuturesHeaderTypeModelList": [...],
  "foHeaderTypeModelList": [...],
  "footerModelV2List": [...],
  "ssheaderTypeModelList": [...],
  "sttHeaderTypeModelList": [...],
  "mheaderTypeModelList": [...],
  "pheaderTypeModelList": [...],
  "cheaderTypeModelList": [...]
}
```

### Output Format
**Type:** PDF document (3 pages)
- Page 1: Main contract note with customer details and summary
- Page 2: Trade annexure with detailed transaction list
- Page 3: Margin statement and pledged securities

---

## Key Technical Decisions

1. **256KB Chunk Size**
   - Rationale: Balances Lambda payload limits (6MB) with processing efficiency
   - Enables parallel processing of large files

2. **FIFO Queue**
   - Rationale: Ensures ordered processing preserves file integrity
   - Prevents duplicate processing with message deduplication

3. **7-Flag Validation**
   - Rationale: Ensures complete data before expensive PDF generation
   - Prevents incomplete contract notes reaching customers

4. **11-Minute Time Windows**
   - Rationale: Lambda execution time limit is 15 minutes
   - 4-minute buffer for error handling and cleanup

5. **Rate Limiting (118/sec)**
   - Rationale: AWS Lambda concurrency limits and downstream service quotas
   - Prevents throttling and ensures stable throughput

6. **Party-Level Grouping**
   - Rationale: Each customer receives independent PDF
   - Enables parallel generation and selective reprocessing

7. **Overflow Handling**
   - Rationale: Automatically handles files larger than time window
   - No manual intervention needed for large batches

8. **Font Initialization**
   - Rationale: Critical step before PDF generation
   - Prevents runtime errors and ensures consistent formatting

---

## Entry Points

### Lambda Handlers
1. **SplitEquityCombineMarginFile.handleRequest()** - S3 Event
2. **InvokeEquityCombineMarginFile.handleRequest()** - SQS Message
3. **EquityCombineMarginJsonFileBaseTrigger.handleRequest()** - S3 Event

### Local Testing
- **DynamicValuePDF.main()** - Reads `input.json` and generates PDF locally
- Useful for development and debugging without AWS infrastructure

---

## Current Development Status

**Git Status (as of latest commit):**
- **Branch:** main
- **Recent Commit:** `ea6e1de` - Repository setup

**Modified Files:**
- `DynamicValuePDF.java` - Modified and staged
- `DynamicValuePDF_Shivraj.java` - Added and staged (active version)
- `EquityCombineMarginJsonFileBaseTrigger.java` - Modified

**Generated Artifacts:**
- `Geojit_Contract_Note_1237171.pdf` - Sample output
- `input.json` - Test data

---

## Scalability & Performance Characteristics

### Throughput
- **Rate:** 118 invocations/second (Stage 2)
- **Chunk Size:** 256KB per chunk
- **Parallelization:** Multiple Lambda instances process chunks concurrently

### Capacity
- **File Size:** Unlimited (chunking handles arbitrarily large files)
- **Customers:** Unlimited (each party processed independently)
- **Time Window:** 11 minutes per Lambda execution
- **Overflow:** Automatic requeuing for continuation

### Reliability
- **Validation:** 7-flag mandatory record check
- **Error Handling:** Invalid records isolated to error bucket
- **Monitoring:** CSV timing reports for process tracking
- **Idempotency:** FIFO queue deduplication prevents duplicate processing

---

## Future Considerations

### Potential Enhancements
1. Dead Letter Queue (DLQ) for failed Lambda invocations
2. CloudWatch alarms for error bucket uploads
3. Automated notification on PDF generation completion
4. Digital signature integration using BouncyCastle
5. Multi-region deployment for disaster recovery
6. Step Functions orchestration for complex workflows

### Monitoring Recommendations
1. CloudWatch metrics for Lambda duration, errors, throttles
2. S3 event metrics for processing lag
3. SQS queue depth monitoring
4. Custom metrics for validation failure rates
5. PDF generation success/failure tracking

---

**Document Version:** 1.0
**Last Updated:** 2026-02-15
**Generated By:** Claude Code Architecture Analysis
