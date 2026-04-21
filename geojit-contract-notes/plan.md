# V2 Contract Note Processing System - Implementation Plan

---

## 1. Objective

### What V2 Exists to Solve

V2 provides a clean, locally-executable implementation of the contract note processing pipeline that:

- Eliminates AWS dependencies for local development and testing
- Simplifies the processing flow to three core stages
- Provides deterministic, repeatable PDF generation from sample data
- Serves as reference implementation for correctness verification
- Enables rapid iteration without infrastructure overhead

### Why It Is Isolated from V1

Complete isolation under `com.geojit.contractnotes.equity_combinemargin.v2` ensures:

- No interference with production V1 Lambda handlers
- Independent evolution of processing logic
- Safe refactoring without regression risk
- Clear migration path when V2 matures
- Ability to run both implementations side-by-side for comparison

---

## 2. Scope

### In Scope

**Data Processing:**
- Parse pipe-delimited text files (format: `CN_PLV083 1.txt`)
- Validate record types: H, A, S, F, D, O, SS, STT, M, P, C
- Map to 13 DTO types matching V1 model classes
- Aggregate into `EquityCombineMarginDtoV2` structure
- Serialize to JSON

**PDF Generation:**
- 5-page layout matching `GeoJit.PDF`:
  - Page 1-2: Contract Note with header, equity/derivative segments, financial summary
  - Page 3: Trade Details Annexure, Net Obligation, Scrip Summary, STT Statement
  - Page 4-5: Daily Margin Statement, Pledged Securities Details
- Geojit branding (logo, SEBI details, GST invoice format)
- Tables with exact column structure from template
- Page numbering and signatures

**Local Execution:**
- Main method accepting file path argument
- Console output showing pipeline progress
- File-based JSON intermediate output
- PDF output to specified directory

### Out of Scope

- AWS S3/SQS/Lambda integration (deferred to future wiring)
- Multi-client batch processing (V2 handles single client)
- Chunking logic (not needed for local execution)
- Rate limiting and time-window processing
- Error bucket uploads
- CSV process reports
- Email/notification triggers
- Production deployment configuration

---

## 3. Input Analysis

### Record Types Found in `CN_PLV083 1.txt`

Based on actual data file:

| Line | Record Type | Purpose | Occurrences |
|------|-------------|---------|-------------|
| 1 | H | Customer header information | 1 |
| 2 | A | Dealing office address | 1 |
| 3 | S-CAPITAL | Capital market summary by ISIN | 1 |
| 4-5 | S-FUTURES | Futures/Options summary by contract | 2 |
| 6-7 | F | Exchange-wise financial summary (footer) | 2 |
| 8-10 | D | Individual trade details | 3 |
| 11 | O | Order summary by security | 1 |
| 12 | SS | Stock-wise summary | 1 |
| 13-14 | STT | Securities Transaction Tax details | 2 |
| 15-21 | M | Margin statement by exchange/segment | 7 |
| 22-27 | P | Pledged securities portfolio | 6 |
| 28-29 | C | Charges breakdown | 2 |

### Role of Placeholder File

`CN_PLV083_PLACEHOLDER 1.txt` serves as schema definition:

- Documents expected field names for each record type
- Uses `<<FIELD_NAME>>` convention for placeholders
- Defines field ordering (critical for pipe-delimited parsing)
- Maps to DTO property names
- Provides validation reference for mandatory fields

Example H record mapping:
```
<<TRADE_CODE>>|H|<<UCC_CODE>>|<<CLIENT_NAME>>|...
```
Maps to `CustomerModel` properties: `partycode`, `clientCode`, `name`, etc.

### Mandatory vs Optional Records

**Mandatory (per client):**
- H (1 instance) - Client identification
- A (1 instance) - Dealing office address
- F (≥1 instances) - Financial summary per exchange

**Conditional (presence depends on trading activity):**
- S-CAPITAL - Only if equity trades exist
- S-FUTURES - Only if F&O trades exist
- D - Only if trades occurred
- O - Only if orders placed
- SS - Only if equity trades exist
- STT - Only if STT applicable
- M - Required for margin reporting
- P - Only if pledged securities exist
- C - Only if charges apply

**Optional:**
- CA (Corporate Actions) - Not present in sample data

**Validation Rule:**
Minimum viable contract note requires: H + A + F (at least one exchange summary)

---

## 4. DTO Strategy

### Record-Type to DTO Mapping

| Record Type | Segment Discriminator | V1 Model Class | DTO List Property | Field Count |
|-------------|----------------------|----------------|-------------------|-------------|
| H | - | CustomerModel | customerList | 15 |
| A | - | DealingOfficeAddress | dealingOfficeAddressList | 4 |
| S | CAPITAL | SCapitalHeaderTypeModel | sCapitalHeaderTypeList | 17 |
| S | FUTURES/OPTIONS | SFuturesHeaderTypeModel | sFuturesHeaderTypeList | 13 |
| F | - | FooterModelV2 | footerList | 16 |
| D | - | DHeaderTypeModel | dHeaderTypeList | 23 |
| O | - | OHeaderTypeModel | oHeaderTypeList | 13 |
| SS | - | SSHeaderTypeModel | ssHeaderTypeList | 10 |
| STT | - | STTHeaderTypeModel | sttHeaderTypeList | Variable (10-17) |
| M | - | MHeaderTypeModel | mHeaderTypeList | 18 |
| P | - | PHeaderTypeModel | pHeaderTypeList | 5 |
| C | - | CHeaderTypeModel | cHeaderTypeList | Variable (4-12) |
| CA | - | CAHeaderTypeModel | caHeaderTypeList | Unknown |

### Aggregation into `EquityCombineMarginDtoV2`

V2 will reuse existing `EquityDtoV2` class from:
```
com.geojit.contractnotes.equity_combinemargin.DTO.EquityDtoV2
```

Structure:
```
EquityDtoV2
├── List<CustomerModel> customerList
├── List<DealingOfficeAddress> dealingOfficeAddressList
├── List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList
├── List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList
├── List<FOHeaderTypeModel> foHeaderTypeList
├── List<DHeaderTypeModel> dHeaderTypeList
├── List<OHeaderTypeModel> oHeaderTypeList
├── List<SSHeaderTypeModel> ssHeaderTypeList
├── List<STTHeaderTypeModel> sttHeaderTypeList
├── List<MHeaderTypeModel> mHeaderTypeList
├── List<PHeaderTypeModel> pHeaderTypeList
├── List<CHeaderTypeModel> cHeaderTypeList
├── List<CAHeaderTypeModel> caHeaderTypeList
└── List<FooterModelV2> footerList
```

### Serialization Boundaries

**Input Boundary:**
Pipe-delimited text → DTO parsing:
- Split on `|` delimiter
- Map positional fields to DTO properties
- Type conversion (String → Integer/Double/Date)
- Null handling for optional fields

**Intermediate Boundary:**
DTO → JSON:
- Jackson serialization using `@JsonProperty` annotations
- Pretty-print format for debugging
- File output: `{contractNo}.json`

**Output Boundary:**
JSON + DTO → PDF:
- Deserialize JSON to `EquityDtoV2`
- Extract data from DTO lists
- Render to iText 7 PDF components
- File output: `{contractNo}.pdf`

---

## 5. Split Phase Design (V2)

### Responsibilities

Unlike V1 (which splits by 256KB chunks for Lambda), V2 Split phase:

- Reads entire text file into memory
- Validates presence of mandatory record types (H, A, F)
- Groups records by party code (single client expected in V2)
- Identifies record type from second pipe-delimited field
- Routes each line to appropriate DTO parser
- Accumulates DTOs into lists

### Validation Rules

**Pre-Processing Validation:**
1. File exists and is readable
2. File is non-empty
3. At least one H record exists
4. Party code is consistent across all records

**Record-Level Validation:**
1. Minimum field count met for record type
2. Record type is recognized (H|A|S|F|D|O|SS|STT|M|P|C|CA)
3. For S records: segment field must be CAPITAL|FUTURES|OPTIONS
4. Numeric fields parse successfully
5. Date fields match expected format (dd.MM.yyyy)

**Post-Processing Validation:**
1. Exactly one H record processed
2. Exactly one A record processed
3. At least one F record processed
4. All lists initialized (empty lists acceptable)

### Grouping Logic

V2 grouping simplified (single-client mode):

```
FOR each line in input file:
  Extract party_code (field 0)
  Extract record_type (field 1)
  Extract segment (field 2, if applicable)

  IF party_code != expected_party_code:
    THROW validation error

  PARSE line based on record_type:
    CASE 'H': parse to CustomerModel → add to customerList
    CASE 'A': parse to DealingOfficeAddress → add to dealingOfficeAddressList
    CASE 'S':
      IF segment == 'CAPITAL': parse to SCapitalHeaderTypeModel
      ELSE: parse to SFuturesHeaderTypeModel
    CASE 'F': parse to FooterModelV2 → add to footerList
    CASE 'D': parse to DHeaderTypeModel → add to dHeaderTypeList
    ... (continue for all types)
END FOR

VALIDATE mandatory lists are populated
RETURN EquityDtoV2 aggregate
```

### Output Contract

Split phase produces:

**Success Path:**
- Populated `EquityDtoV2` object
- All 13 DTO lists initialized (may be empty)
- Validation success flag

**Error Path:**
- Validation exception with:
  - Line number of failure
  - Record type attempted
  - Specific validation failure message
  - Stack trace for debugging

---

## 6. Invoke Phase Design (V2)

### Transformation Rules

Invoke phase bridges Split → PDF:

**DTO to JSON Transformation:**
1. Accept `EquityDtoV2` from Split phase
2. Use Jackson `ObjectMapper` with pretty-print enabled
3. Apply `@JsonProperty` annotations from `EquityDtoV2`
4. Write to file: `output/{partyCode}_{contractNo}.json`

**Derived Calculations:**
Certain PDF fields require calculations not present in raw data:

1. **Net Totals:**
   - Sum buy values - sum sell values per security
   - Aggregate by exchange and segment

2. **Grand Totals:**
   - Total pay-in/pay-out obligation across exchanges
   - Total STT, GST, charges

3. **Formatting:**
   - Decimal precision: 2 places for currency, 4 for rates
   - Date format: dd.MM.yyyy for display
   - Negative amounts: parentheses or minus sign

4. **Lookups:**
   - ISIN → Security Name mapping from D/O records
   - Exchange code → Full exchange name

### JSON Structure Guarantees

Generated JSON must ensure:

1. **List Ordering:**
   - D records: chronological by trade time
   - M records: exchange order (NSE FO, NSE CDS, BSE FO, BSE CDS, MCX'SX, NSE CASH, BSE CASH)
   - P records: alphabetical by security description
   - F records: match summary table order in PDF

2. **Null Handling:**
   - Empty lists: `[]` (not null)
   - Missing optional fields: `null`
   - Zero values: `0` or `0.0` (not null)

3. **Type Consistency:**
   - Dates: ISO-8601 strings or dd.MM.yyyy format
   - Currency: Double with 2 decimal precision
   - Quantities: Integer for equities, Double for F&O

4. **Schema Alignment:**
   - Matches `EquityDtoV2` class structure exactly
   - V1-compatible for potential reuse

### Error Handling Strategy

**Recoverable Errors:**
- Missing optional fields → Use default values or null
- Invalid numeric format → Log warning, use 0.0
- Unknown record subtype → Skip with warning

**Non-Recoverable Errors:**
- Missing mandatory fields (H, A, F) → Abort with clear message
- JSON serialization failure → Abort with Jackson exception
- File write permission denied → Abort with I/O exception

**Logging:**
- INFO: Successful phase completion, record counts
- WARN: Skipped records, default value substitutions
- ERROR: Validation failures, processing aborts

---

## 7. PDF Phase Design (V2)

### PDF Decomposition from `GeoJit.PDF`

**Page 1-2: Contract Note Main Body**

Components:
1. **Header Section:**
   - Geojit logo (top-left)
   - Title: "CONTRACT NOTE CUM TAX INVOICE" (centered)
   - Subtitle: "(Tax Invoice under Section 31 of GST Act)"
   - "ORIGINAL FOR RECIPIENT" (top-right)

2. **Company Information Block:**
   - Company name: GEOJIT INVESTMENTS LTD
   - SEBI Registration, CIN
   - Registered address, contact details
   - Compliance officer details
   - Dealing office address (from A record)

3. **Client Information Table:**
   - Contract note number, trade date
   - Client name, address, phone (from H record)
   - Trade code/UCC, PAN
   - Place of supply, GST No, IRN

4. **Exchange Settlement Table:**
   - Exchange/Clearing Corporation
   - Segment, Settlement No, Settlement Date
   - UCC Code

5. **Equity Segment Table:**
   Data source: S-CAPITAL, O, D records
   Columns:
   - ISIN
   - Security Name
   - Buy: Quantity, WAP, Brokerage, WAP after brokerage, Total Value
   - Sell: Quantity, WAP, Brokerage, WAP after brokerage, Total Value
   - Net Quantity, Net Obligation

6. **Derivative Segment Table:**
   Data source: S-FUTURES, D records
   Columns:
   - Contract Description
   - Buy/Sell/BF/CF
   - Quantity
   - WAP per unit (Foreign Currency)
   - WAP per unit (Rs)
   - Brokerage per unit
   - WAP after brokerage
   - Closing rate per unit
   - Net Total (Before Levies)
   - Remarks

7. **Financial Summary Table (Page 2):**
   Data source: F (FooterModelV2) records
   Columns:
   - Exchange & Segment
   - Pay In/Out Obligation
   - Securities Transaction Tax
   - SGST (9%), CGST (9%), IGST (18%)
   - TDS
   - Exchange Transaction Charges
   - SEBI Turnover Fees
   - Additional Cess
   - Stamp Duty
   - Net Amount Receivable/Payable

8. **Footer Section (Page 2):**
   - GST calculation note
   - Brokerage explanation
   - Regulatory disclaimer text
   - Date, place, signature block
   - Company PAN, GSTIN
   - Service description: "STOCK BROKER"
   - SAC: 997152
   - Authorized signatory name
   - Rights entitlement disclaimer
   - "* End Of Contract *" marker

**Page 3: Annexure - Trade Details**

Components:
1. **Title:** "Annexure - Trade Details"

2. **Clearing Corporation Header:**
   - Name: NSE Clearing Limited

3. **Trade Details Table (per exchange):**
   Data source: D records, grouped by exchange
   Section header: "Name Of Exchange & Segment : {EXCHANGE} {SEGMENT} | {EXCHANGE} BrokerCode : {CODE}"

   Columns:
   - Order No
   - Order Time
   - Trade No
   - Trade Time
   - Security/Contract Description
   - Buy/Sell
   - Quantity
   - Gross Rate/Trade Price Per Unit (Foreign Currency)
   - Gross Rate/Trade Price Per Unit (Rs)
   - Brokerage (Rs)
   - Net Rate Per Unit (Rs)
   - Closing Rate Per Unit (Only for derivatives)
   - Net Total (Before Levies)
   - Remarks

   SubTotal row after each exchange section

4. **Net Obligation (Equity Market) Table:**
   Data source: O records

   Columns:
   - Sl.No
   - Security
   - Segment
   - Bought: Quantity, Rate
   - Sold: Quantity, Rate
   - Net Obligation: Quantity, Rate, Amount

   Footer rows:
   - Total (sum of amounts)
   - Securities Transaction Tax
   - Net Amount

5. **Scrip-Summary Table:**
   Data source: SS records

   Columns:
   - Security Description
   - B/S
   - Quantity
   - Gross Rate Per Security (Rs)
   - Gross Total (Rs)
   - Gross Brokerage Per Security (Rs)
   - Brokerage (Total) (Rs)
   - Net Rate (Rs)
   - Net Total Amount (Rs)

6. **Statement of Securities Transaction Tax Table:**
   Data source: STT records

   **For Cash Transactions:**
   - Transaction settled by Delivery Purchase: Quantity, Price, Value, STT
   - Transaction settled by Delivery Sale: Quantity, Price, Value, STT
   - Transaction settled other than by Delivery: Quantity, Price, Value, STT
   - Total STT (Rounded to Nearest Rupee)

   **For F&O Transactions:**
   Section header: "Name Of Exchange & Segment : {EXCHANGE} {SEGMENT}"
   - Sl.No, Security, Expiry Date
   - Value of Transactions Futures: Sale, STT
   - Value of Transactions Options: Sale, STT
   - Total STT
   - Total (Rounded to nearest Rupee)

**Page 4-5: Daily Margin Statement**

Components:
1. **Title:** "Daily Margin Statement" | "Exchange : NSE, BSE, MCX'SX" (right-aligned)

2. **Margin Statement Table:**
   Data source: M records

   Columns:
   - Seg (Segment)
   - Trade Day
   - Margins available till T day:
     - Funds
     - Value of Securities (after haircut)
     - Value of margin pledge Securities (after haircut)
     - Bank Guarantees / FDR
     - Any other approved form of Margins
     - Total Margins Available (E)
   - Margin / Consolidated Crystallized Obligation / MTM required by Exchange/CC end of T & T+1 day:
     - Total upfront Margin
     - Consolidated Crystallized Obligation / MTM
     - Delivery Margin
     - Total Requirement
   - Excess / Shortfall w.r.t. Requirement by Exchange / CC
   - Additional Margin required by member as per RMS
   - Margin Status (Balance with Member / Due from client)

   Rows per segment: NSEFO, NSECDS, BSEFO, BSECDS, MCX'SX, NSECASH, BSECASH

   Summary row: "Summary of all Exchanges" with totals

3. **Margin Footnotes:**
   - Approved forms disclaimer
   - Settlements note
   - Collateral accounting note
   - Fund balance calculation note
   - Provisional penalty note
   - MTM/Premium breakdown by exchange
   - Figure notation (-ve = debit, +ve = credit)
   - BSE/CDSL compliance note
   - Instructions URL
   - Commodity exchanges note

4. **Margin Pledge Securities Details Table:**
   Data source: P records

   Columns:
   - Security
   - Qty
   - Total Value
   - HairCut Value
   - Balance Amount

   Footer: Total (sum of balance amounts)

### Page-Wise Rendering Plan

**Rendering Order:**

1. Initialize PDF document (A4, portrait)
2. Register fonts (Calibri regular, Calibri bold)
3. Create page event handler for headers/footers

**For Pages 1-2:**
1. Render company header block (static content + logo)
2. Render client information table (H record data)
3. Render exchange settlement table (F records)
4. Render equity segment table (S-CAPITAL, O, D records)
   - Include only if equity trades exist
5. Render derivative segment table (S-FUTURES, D records)
   - Include only if F&O trades exist
6. Check page break → Continue to Page 2
7. Render financial summary table (F records)
8. Render GST calculation footnote
9. Render disclaimer text block
10. Render signature block
11. Render "End Of Contract" marker

**For Page 3:**
1. Render "Annexure - Trade Details" title
2. Render clearing corporation header
3. FOR each unique exchange in D records:
   - Render exchange section header
   - Render trade details table
   - Render subtotal row
4. Render "Net Obligation (Equity Market)" section
   - Use O records
5. Render "Scrip-Summary" section
   - Use SS records
6. Render "Statement of Securities Transaction Tax" section
   - Use STT records
   - Separate cash and F&O sections

**For Pages 4-5:**
1. Render "Daily Margin Statement" title
2. Render margin statement table
   - Use M records in specific order
   - Add summary row
3. Render margin footnotes (6-7 numbered points)
4. Render "Margin Pledge Securities Details" title
5. Render pledged securities table
   - Use P records
6. Render total row

### Section-to-DTO Mapping

| PDF Section | Primary DTO Source | Secondary Sources | Calculations |
|-------------|-------------------|-------------------|--------------|
| Company Header | Static constants | - | - |
| Client Info | CustomerModel (H) | - | - |
| Dealing Office | DealingOfficeAddress (A) | - | - |
| Exchange Settlement | FooterModelV2 (F) | - | Extract unique exchanges |
| Equity Segment Table | SCapitalHeaderTypeModel (S-CAPITAL) | OHeaderTypeModel (O) | Aggregate buy/sell |
| Derivative Segment Table | SFuturesHeaderTypeModel (S-FUTURES) | DHeaderTypeModel (D) | Group by contract |
| Financial Summary | FooterModelV2 (F) | - | Sum totals |
| Trade Details | DHeaderTypeModel (D) | - | Group by exchange |
| Net Obligation | OHeaderTypeModel (O) | - | Calculate net amounts |
| Scrip Summary | SSHeaderTypeModel (SS) | - | - |
| STT Statement | STTHeaderTypeModel (STT) | - | Separate cash/F&O |
| Margin Statement | MHeaderTypeModel (M) | - | Order by segment |
| Pledged Securities | PHeaderTypeModel (P) | - | Sum total value |

### Pagination Rules

1. **Fixed Pages:**
   - Contract Note: Always 2 pages (force page break after equity/derivative tables)
   - Annexure: Always 1 page (compact layout)
   - Margin Statement: 2 pages (table continues to Page 5)

2. **Dynamic Overflow:**
   - If trade details exceed Page 3 capacity: Continue to additional pages
   - If pledged securities exceed Page 5 capacity: Continue to additional pages
   - Maintain header/footer on all pages

3. **Page Numbering:**
   - Format: "Page No : {n}" (bottom-right)
   - Sequential from 1 to N

4. **Headers on Continuation Pages:**
   - Repeat Geojit logo and company name
   - Do not repeat full company information block
   - Include client code in header for reference

---

## 8. Directory & Package Structure

### Exact Packages

```
com.geojit.contractnotes.equity_combinemargin.v2
├── model                      (Reuse V1 models via import)
├── dto                        (Reuse EquityDtoV2 via import)
├── parser
│   ├── RecordParser.java     (Interface for record parsing)
│   ├── CustomerRecordParser.java
│   ├── AddressRecordParser.java
│   ├── CapitalSummaryRecordParser.java
│   ├── FuturesSummaryRecordParser.java
│   ├── FooterRecordParser.java
│   ├── TradeDetailRecordParser.java
│   ├── OrderSummaryRecordParser.java
│   ├── StockSummaryRecordParser.java
│   ├── STTRecordParser.java
│   ├── MarginRecordParser.java
│   ├── PledgeRecordParser.java
│   └── ChargesRecordParser.java
├── processor
│   ├── SplitProcessor.java    (Split phase orchestrator)
│   ├── InvokeProcessor.java   (JSON transformation)
│   └── PDFProcessor.java      (PDF generation)
├── renderer
│   ├── PDFRenderer.java       (Main PDF rendering coordinator)
│   ├── HeaderRenderer.java    (Company/client headers)
│   ├── EquityTableRenderer.java
│   ├── DerivativeTableRenderer.java
│   ├── FinancialSummaryRenderer.java
│   ├── TradeDetailsRenderer.java
│   ├── NetObligationRenderer.java
│   ├── ScripSummaryRenderer.java
│   ├── STTRenderer.java
│   ├── MarginStatementRenderer.java
│   └── PledgedSecuritiesRenderer.java
├── util
│   ├── FileUtil.java          (File I/O operations)
│   ├── ValidationUtil.java    (Validation rules)
│   ├── FormatUtil.java        (Date/number formatting)
│   └── CalculationUtil.java   (Derived calculations)
└── ContractNoteProcessorV2.java  (Main entry point)
```

### File Naming Conventions

**Java Classes:**
- Processors: `{Phase}Processor.java` (e.g., `SplitProcessor.java`)
- Parsers: `{RecordType}RecordParser.java` (e.g., `CustomerRecordParser.java`)
- Renderers: `{Section}Renderer.java` (e.g., `EquityTableRenderer.java`)
- Utils: `{Purpose}Util.java` (e.g., `ValidationUtil.java`)

**Data Files:**
- Input text: `CN_{PARTY_CODE}_{suffix}.txt`
- Intermediate JSON: `{PARTY_CODE}_{CONTRACT_NO}.json`
- Output PDF: `{PARTY_CODE}_{CONTRACT_NO}.pdf`

**Resources:**
- Logo: `GeoJit_IMG.png` (reuse from V1 resources)
- Fonts: `calibri-400.ttf`, `calibri-Bold.ttf` (reuse from V1 resources)
- Sample inputs: `src/main/resources/sample/`

---

## 9. Execution Flow (Local)

### How a Developer Runs V2 End-to-End Locally

**Prerequisites:**
1. Maven installed
2. Java 8+ runtime
3. Sample input file: `CN_PLV083 1.txt`

**Execution Command:**
```bash
# From project root
mvn clean package

# Run V2 processor
java -cp target/geojit-contract-notes-1.0-SNAPSHOT.jar \
  com.geojit.contractnotes.equity_combinemargin.v2.ContractNoteProcessorV2 \
  src/main/resources/sample/CN_PLV083\ 1.txt \
  output/
```

**Arguments:**
1. Input file path (absolute or relative)
2. Output directory path (created if not exists)

**Console Output:**
```
[INFO] V2 Contract Note Processor - Starting
[INFO] Reading input file: /path/to/CN_PLV083 1.txt
[INFO] Split Phase - Starting
[INFO] Split Phase - Found 30 records
[INFO] Split Phase - Parsed: H=1, A=1, S=3, F=2, D=3, O=1, SS=1, STT=2, M=7, P=6, C=2
[INFO] Split Phase - Validation: PASSED
[INFO] Invoke Phase - Starting
[INFO] Invoke Phase - Serializing to JSON
[INFO] Invoke Phase - Written: output/PLV083_1237171.json (15.2 KB)
[INFO] PDF Phase - Starting
[INFO] PDF Phase - Initializing fonts
[INFO] PDF Phase - Rendering Page 1: Contract Note Header
[INFO] PDF Phase - Rendering Page 1: Equity Segment Table (1 securities)
[INFO] PDF Phase - Rendering Page 1: Derivative Segment Table (2 contracts)
[INFO] PDF Phase - Rendering Page 2: Financial Summary (2 exchanges)
[INFO] PDF Phase - Rendering Page 3: Trade Details Annexure (3 trades)
[INFO] PDF Phase - Rendering Page 3: Net Obligation Table
[INFO] PDF Phase - Rendering Page 3: Scrip Summary
[INFO] PDF Phase - Rendering Page 3: STT Statement
[INFO] PDF Phase - Rendering Page 4: Margin Statement (7 segments)
[INFO] PDF Phase - Rendering Page 5: Pledged Securities (6 items)
[INFO] PDF Phase - Written: output/PLV083_1237171.pdf (194.5 KB)
[INFO] V2 Contract Note Processor - Completed successfully in 1.2s
```

### Expected Artifacts at Each Step

**After Split Phase:**
- In-memory `EquityDtoV2` object
- Console log showing record counts
- No file output (intermediate structure only)

**After Invoke Phase:**
- JSON file: `output/PLV083_1237171.json`
- Contents:
  ```json
  {
    "customerList": [{ ... }],
    "dealingOfficeAddressList": [{ ... }],
    "sCapitalHeaderTypeList": [{ ... }],
    "sFuturesHeaderTypeList": [{ ... }, { ... }],
    "footerList": [{ ... }, { ... }],
    "dHeaderTypeList": [{ ... }, { ... }, { ... }],
    ...
  }
  ```
- Human-readable formatting (pretty-print enabled)

**After PDF Phase:**
- PDF file: `output/PLV083_1237171.pdf`
- Size: ~190-200 KB
- Pages: 5 (matching GeoJit.PDF structure)
- Viewable in any PDF reader

**On Error:**
- Console error output with stack trace
- Exit code: Non-zero
- Partial artifacts may exist (e.g., JSON created but PDF failed)

---

## 10. Validation & Acceptance Criteria

### How Correctness Is Verified

**Unit Testing:**
1. Each `RecordParser` tested with valid/invalid inputs
2. `ValidationUtil` tested with edge cases
3. `CalculationUtil` tested with known value calculations
4. Mock DTO objects used for renderer testing

**Integration Testing:**
1. End-to-end run with `CN_PLV083 1.txt` sample
2. Verify JSON structure matches expected schema
3. Verify PDF generation completes without exceptions

**Manual Verification:**
1. Visual comparison of generated PDF vs. `GeoJit.PDF`
2. Data spot-checks: client name, trade values, totals
3. Layout alignment: tables, headers, footers

### Visual Checks

**Page 1-2 (Contract Note):**
- [ ] Geojit logo appears in top-left corner
- [ ] Client name matches: "VIPIN G"
- [ ] Contract note number matches: "1237171"
- [ ] Trade date matches: "03.07.2025"
- [ ] Equity table shows: SAMBHV STEEL TUBES LIMITED, 10 shares, -1033.7000 net obligation
- [ ] Derivative table shows: IO 08 JUL 2025 BSXOPT 83600, Buy 20, Sell 20
- [ ] Financial summary shows: BSEFO-OPT = 287.09, NSEEN = -1038.34, Total = -751.25
- [ ] GST calculation note appears: "(18% of Rs.64.873)"
- [ ] Signature block shows: "Johny Varghese"
- [ ] "End Of Contract" marker present

**Page 3 (Annexure):**
- [ ] Trade details show 3 trades (2 BSE FO, 1 NSE EN)
- [ ] Net obligation table shows: SAMBHV STEEL TUBES LIMITED, Net -1037.34
- [ ] Securities Transaction Tax: 1.00
- [ ] Scrip summary shows: 10 shares bought at 103.3700
- [ ] STT statement shows: Cash STT = 1.01, F&O STT = 7.60

**Page 4-5 (Margin Statement):**
- [ ] Margin table shows 7 segments (NSEFO, NSECDS, BSEFO, BSECDS, MCX'SX, NSECASH, BSECASH)
- [ ] Summary row shows: Total Margins Available = 39728.66, Total Requirement = 202.74
- [ ] Pledged securities table shows 6 items
- [ ] Total pledged value: 40188.536464

### Data Checks

**Arithmetic Validation:**
1. Sum of equity net obligations across all securities = -1037.34
2. Sum of F&O net totals = 308.00 (B: -7272.00, S: 7580.00)
3. Total STT (cash + F&O) = 1.00 + 7.60 = 8.60 (but rounded separately)
4. Total pledged securities value = 40188.536464
5. Net amount payable = -751.25 (client receives)

**Reference Data:**
1. ISIN INE12NJ01018 → Security name "SAMBHV STEEL TUBES LIMITED" (consistent across tables)
2. Exchange codes: BSE → "BSE", NSE → "NSE"
3. Segment codes: FO → "FO", EN → "EN"

**Date/Time Parsing:**
1. Trade date: 03.07.2025 (dd.MM.yyyy format)
2. Order times: HH:mm:ss format (e.g., 12:42:04, 03:21:18)
3. Settlement date: 04.07.2025

**Decimal Precision:**
1. Currency amounts: 2 decimal places
2. Rates: 4 decimal places (e.g., 101.3700)
3. Quantities: Integer for equities, decimal for F&O

### Pass Criteria

V2 implementation is considered complete when:

1. **Functional:**
   - Processes `CN_PLV083 1.txt` without errors
   - Generates JSON matching expected structure
   - Generates 5-page PDF matching GeoJit.PDF layout

2. **Data Accuracy:**
   - All client information fields match source data
   - All trade details match source data
   - All calculated totals verified correct

3. **Visual Fidelity:**
   - Logo, headers, footers render correctly
   - Tables align properly with appropriate column widths
   - Page breaks occur at expected locations
   - Fonts (Calibri) render consistently

4. **Code Quality:**
   - All classes follow naming conventions
   - Package structure isolated under v2
   - No dependencies on V1 processor classes
   - Unit tests achieve >80% coverage

5. **Execution:**
   - Runs locally via main method
   - Completes in <5 seconds for single client
   - Produces human-readable console output
   - Returns exit code 0 on success

---

## 11. Future Wiring (Non-Implementation)

### How V2 Could Later Plug into S3 / SQS

**Without Implementing AWS Now:**

The current V2 design uses clean interfaces that allow future AWS integration:

**Split Phase Future Wiring:**
- Current: Reads from local file path
- Future: Add `S3SplitProcessor implements SplitProcessor`
  - Downloads from `raw-s3-geojit` bucket
  - Calls existing `SplitProcessor.process(InputStream)`
  - No change to core parsing logic

**Invoke Phase Future Wiring:**
- Current: Writes JSON to local file system
- Future: Add `S3InvokeProcessor implements InvokeProcessor`
  - Uploads JSON to `json-s3-geojit` bucket
  - Publishes message to SQS queue
  - Calls existing `InvokeProcessor.toJson(EquityDtoV2)`
  - No change to serialization logic

**PDF Phase Future Wiring:**
- Current: Writes PDF to local file system
- Future: Add `S3PDFProcessor implements PDFProcessor`
  - Listens for S3 upload event from `json-s3-geojit`
  - Downloads JSON, calls existing `PDFProcessor.generatePDF(EquityDtoV2)`
  - Uploads PDF to `pdf-s3-geojit` bucket
  - No change to rendering logic

**Lambda Handler Wrapper Pattern:**
```
V2 Lambda Handler (future):
  ├── handleRequest(S3Event event)
  │   ├── Extract bucket/key from event
  │   ├── Download file to temp location
  │   ├── Call ContractNoteProcessorV2.process(tempFile, tempOutput)
  │   ├── Upload output to S3
  │   └── Return success/failure
  └── Reuses all V2 processor/renderer classes
```

**Migration Path:**
1. Deploy V2 as separate Lambda functions (create-pdf-geojit-v2, etc.)
2. Route 5% of traffic to V2 via S3 prefix routing
3. Compare V2 output to V1 output (A/B testing)
4. Gradually increase V2 traffic percentage
5. Deprecate V1 once V2 proven stable

**Configuration Abstraction:**
Future configuration via environment variables or Spring profiles:
- `PROCESSOR_MODE=local|s3`
- `INPUT_SOURCE=file://{path}|s3://{bucket}/{key}`
- `OUTPUT_TARGET=file://{path}|s3://{bucket}/{key}`

This allows V2 to run in both local and cloud modes without code changes.

---

**End of Plan**
