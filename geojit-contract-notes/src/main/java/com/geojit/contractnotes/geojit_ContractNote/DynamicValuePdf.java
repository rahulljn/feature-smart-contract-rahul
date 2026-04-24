package com.geojit.contractnotes.geojit_ContractNote;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.itextpdf.signatures.*;
import org.json.JSONObject;
import com.itextpdf.layout.properties.OverflowPropertyValue;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.WriterProperties;


import com.geojit.contractnotes.DTO.GeojitStatementDTO;
import com.geojit.contractnotes.Model.*;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.net.URL;
import java.security.Security;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GEOJIT CONTRACT NOTE PDF GENERATOR - REFACTORED FOR NEW DTO STRUCTURE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * DTO MIGRATION SUMMARY:
 * OLD ROOT DTO : EquityDtoV2               → NEW: GeojitStatementDTO
 * OLD HEADER   : CustomerModel             → NEW: HeaderDto           (tag H)
 * OLD EXCHANGE : DHeaderTypeModel          → NEW: ExchangeClearingDto (tag E)
 * OLD EQUITY   : DHeaderTypeModel          → NEW: EquitySegmentDto    (tag P)  [pre-computed]
 * OLD DERIV    : FOHeaderTypeModel         → NEW: DerivativeSegmentDto(tag V)  [pre-computed]
 * OLD SUMMARY  : FooterModelV2             → NEW: PayInPayOutDto      (tag O)  [pre-computed]
 * OLD DATE     : CustomerModel.date        → NEW: DatePlaceDto        (tag F)
 * OLD ANNEXURE : FOHeaderTypeModel+D...   → NEW: NameAndExchangeTotalDto(N)+NameClearingCorporationDto(D)
 * OLD NET OBL  : SCapitalHeaderTypeModel  → NEW: NetObligationDto    (tag M) + NetObligationTotalDto(tag T)
 * OLD SCRIP    : SSHeaderTypeModel        → NEW: ScripSummaryDto     (tag U)
 * OLD STT CASH : SCapitalHeaderTypeModel  → NEW: SecurityTransactionDto(C) + SecurityTransactionTotalDto(R)
 * OLD STT DERIV: FOHeaderTypeModel        → NEW: CashSegmentTotalDto (A) + CashSegmentDto (L)
 * OLD MARGIN   : MHeaderTypeModel         → NEW: DailyMarginDto      (tag G) + DailyMarginTotalDto(tag J)
 * OLD PLEDGE   : PHeaderTypeModel         → NEW: MarginPledgeDto     (tag K) + MarginPledgeTotalDto(tag Q)
 *
 * ⚠️ CRITICAL RULE ENFORCED:
 * - ZERO aggregation/calculation in this class
 * - All totals consumed directly from DTO
 * - This class is a DATA RENDERER only
 *
 * @version 12.0 - New DTO refactored
 */
public class DynamicValuePdf implements RequestHandler<Object, Integer> {

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTANTS & CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════

    // private static final String PDF_BUCKET = "pdf-s3-geojit-prod";  // PROD
    private static final String PDF_BUCKET = "pdf-s3-geojit-dev";
    private static final String STATUS_QUEUE_NAME = "geojit-pipeline-status-queue.fifo";
    private static final Logger logger = LoggerFactory.getLogger(DynamicValuePdf.class);

    String jobId = null;  // traceId from API — propagated from Invoke Lambda

    /**
     * Q1 ANSWER: Dealing office address is hardcoded
     */
    private static final String DEALING_OFFICE_ADDRESS =
            "1ST FLOOR, MASTERS TOWER, BYPASS-PIPELINE JUNCTION, " +
                    "CIVIL LINE ROAD, PALARIVATTOM | TELEPHONE NO: 9995800066";

    // Font Resources
    private static final ClassLoader classLoader = DynamicValuePdf.class.getClassLoader();

    // Font Sizes
    private static final float FONT_SIZE_TITLE = 12f;
    private static final float FONT_SIZE_SUBTITLE = 9f;
    private static final float FONT_SIZE_HEADER = 9f;
    private static final float FONT_SIZE_NORMAL = 8f;
    private static final float FONT_SIZE_DATA = 8f;
    private static final float FONT_SIZE_SMALL = 7f;
    private static final float FONT_SIZE_TINY = 6f;
    private static final float FONT_SIZE_MICRO = 6f;

    // Geojit Brand Colors
    private static final Color GEOJIT_GREEN = new DeviceRgb(0, 114, 114);
    private static final Color GEOJIT_DARK_GREEN = new DeviceRgb(0, 90, 90);
    private static final Color HEADER_BG_COLOR = new DeviceRgb(242, 242, 242);
    private static final Color TABLE_HEADER_GREY = new DeviceRgb(217, 217, 217);
    private static final Color LIGHT_GREEN_BG = new DeviceRgb(200, 240, 240);
    private static final Color BORDER_COLOR = ColorConstants.BLACK;

    // Formatters
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat decimalFormat4 = new DecimalFormat("#,##0.0000");

    // AWS
    private final AmazonS3 s3Client;
    private final ObjectMapper objectMapper;
    private String sourceBucket;

    // ═══════════════════════════════════════════════════════════════════════
    //  FIX: Fonts and logo are now LOCAL to each generatePDF() call.
    //  They are NO LONGER instance fields to avoid cross-document iText errors
    //  on warm Lambda container reuse.
    // ═══════════════════════════════════════════════════════════════════════
    // Fonts - declared as instance fields but reloaded fresh per generatePDF() call
    private PdfFont calibriNormal;
    private PdfFont calibriBold;
    // Logo - reloaded fresh per generatePDF() call (NOT loaded in constructor)
    private ImageData logoImageData;
    private ImageData signatureImageData;

    // ═══════════════════════════════════════════════════════════════════════
    //  DTO FIELDS  (NEW STRUCTURE — replaces all old CustomerModel etc.)
    // ═══════════════════════════════════════════════════════════════════════
    // [MODIFIED] All old model lists replaced with new GeojitStatementDTO fields
    private HeaderDto header;
    private List<ExchangeClearingDto> exchanges = new ArrayList<>();
    private List<EquitySegmentDto> positions = new ArrayList<>();
    private List<DerivativeSegmentDto> vRecords = new ArrayList<>();
    private List<NameClearingCorporationDto> details = new ArrayList<>();
    private List<PayInPayOutDto> obligations = new ArrayList<>();
    private DatePlaceDto footerDto;
    private List<NameAndExchangeTotalDto> notes = new ArrayList<>();
    private List<NetObligationDto> margins = new ArrayList<>();
    private NetObligationTotalDto obligationTotal;
    private List<ScripSummaryDto> uRecords = new ArrayList<>();
    private List<SecurityTransactionDto> contracts = new ArrayList<>();
    private SecurityTransactionTotalDto roundedTotal;
    private List<CashSegmentTotalDto> amounts = new ArrayList<>();
    private List<CashSegmentDto> lots = new ArrayList<>();
    private List<DailyMarginDto> generals = new ArrayList<>();
    private DailyMarginTotalDto journal;
    private List<MarginPledgeDto> securities = new ArrayList<>();
    private MarginPledgeTotalDto pledgeQuantity;

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    //  FIX: loadLogo() removed from constructor — logo is now loaded fresh
    //       inside generatePDF() to avoid iText cross-document object errors
    //       on warm Lambda container reuse.
    // ═══════════════════════════════════════════════════════════════════════
    public DynamicValuePdf() {
        this.s3Client = AmazonS3ClientBuilder.defaultClient();
        this.objectMapper = new ObjectMapper();
        //     this.s3Client = null;
        Security.addProvider(new BouncyCastleProvider());

        // FIX: fonts and logo are NOT initialized here anymore.
        // They are loaded fresh inside generatePDF() per invocation.
        // This prevents iText "indirect object belongs to other PDF document"
        // errors caused by warm Lambda container reuse.
        //        System.out.println("✓ Geojit PDF Generator V12.0 (New DTO) initialized");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FIX: loadFreshLogo() — loads logo bytes fresh every time.
    //  Called inside generatePDF() instead of constructor.
    // ═══════════════════════════════════════════════════════════════════════
    private ImageData loadFreshLogo() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("GeoJit_IMG.png");
        if (is != null) {
            return ImageDataFactory.create(is.readAllBytes());
        } else {
            throw new RuntimeException("Logo image not found!");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
//  LOAD SIGNATURE IMAGE — fresh per invocation (same pattern as logo)
// ═══════════════════════════════════════════════════════════════════════
    private ImageData loadFreshSignature() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("signature.png");
            if (is != null) {
                return ImageDataFactory.create(is.readAllBytes());
            } else {
                logger.error("Signature image not found in resources");
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to load signature: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LAMBDA HANDLER
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public Integer handleRequest(Object input, Context context) {
        //        System.out.println("  GEOJIT PDF GENERATOR V12.0 - NEW DTO STRUCTURE");
        SimpleDateFormat istFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        istFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        String contractNoteNo = "UNKNOWN";

        logger.info("DynamicValuePdf Started | startTime={}", istFormat.format(new Date()));

        try {
            ObjectMapper mapper = new ObjectMapper();
            JSONObject inputObject = new JSONObject(mapper.writeValueAsString(input));

            // Read jobId from payload (set by Invoke Lambda)
            if (inputObject.has("jobId")) {
                jobId = inputObject.getString("jobId");
                logger.info("JobId from payload: {}", jobId);
            }

            String jsonString;

            // ✅ CASE 1: GetJsonLambda se aaya — s3Key present hai
            if (inputObject.has("s3Key")) {
                String bucketName = inputObject.getString("bucketName");
                String s3Key = inputObject.getString("s3Key");

                S3Object s3Object = s3Client.getObject(
                        new GetObjectRequest(bucketName, s3Key));
                jsonString = new BufferedReader(
                        new InputStreamReader(s3Object.getObjectContent()))
                        .lines()
                        .collect(Collectors.joining("\n"));

            } else if (inputObject.has("statementData")) {
                // CASE 2: Invoke.java direct with jobId wrapper — unwrap statementData
                jsonString = inputObject.get("statementData").toString();
            } else {
                // CASE 3: Invoke.java direct without wrapper (backward compat)
                jsonString = inputObject.toString();
            }

            GeojitStatementDTO dto = mapper.readValue(jsonString, GeojitStatementDTO.class);
            loadModelsFromDTO(dto);
            validateData();
            contractNoteNo = safe(header != null ? header.getContractNoteNo() : "UNKNOWN");
            logger.info("DynamicValuePdf generating PDF | contractNoteNo={} | partyCode={}",
                    contractNoteNo, safe(header != null ? header.getTradeCode() : "UNKNOWN"));
            File pdfFile = generatePDF();
            String s3Key = uploadPDFToS3(pdfFile);

            // Send PDF_GENERATED status event
            sendPdfStatusEvent(s3Key, null);

            logger.info("PDF generated for customer | partyCode={} | contractNoteNo={}",
                    safe(header != null ? header.getTradeCode() : "UNKNOWN"),
                    contractNoteNo);
            logger.info("DynamicValuePdf Completed Successfully | contractNoteNo={} | endTime={}",
                    contractNoteNo, istFormat.format(new Date()));
            return 200;

        } catch (Exception e) {
            ExceptionPublisher.publish(jobId, "PDF", null, e);
            // Send PDF_FAILED status event
            sendPdfStatusEvent(null, e.getMessage());
            logger.error("DynamicValuePdf ERROR | contractNoteNo={} | error={} | endTime={}",
                    contractNoteNo, e.getMessage(), istFormat.format(new Date()), e);
            return 500;
        }
    }


    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 2 : LOAD ALL MODELS FROM GeojitStatementDTO
    //  [MODIFIED] Completely replaced — reads from new DTO structure
    //  [REMOVED] All old CustomerModel, DealingOfficeAddress, FOHeaderTypeModel,
    //            DHeaderTypeModel, FooterModelV2, SCapitalHeaderTypeModel,
    //            SFuturesHeaderTypeModel, CAHeaderTypeModel, SSHeaderTypeModel,
    //            STTHeaderTypeModel, PHeaderTypeModel, MHeaderTypeModel references
    //  [REMOVED] convertFuturesDataToFOFormat() — no longer needed
    //  [REMOVED] normalizeMarginData()           — no longer needed
    // ═══════════════════════════════════════════════════════════════════════
    public void loadModelsFromDTO(GeojitStatementDTO dto) {
        //        System.out.println("   [2/6] Loading models from GeojitStatementDTO...");

        // H-record: Header
        this.header = dto.getHeader() != null ? dto.getHeader() : new HeaderDto();
        //        System.out.println("       → Client      : " + safe(header.getNameOfClient()));
        //        System.out.println("       → Contract No : " + safe(header.getContractNoteNo()));
        //        System.out.println("       → Trade Date  : " + safe(header.getTradeDate()));

        // E-records: Exchange / Clearing Corporation table
        this.exchanges = dto.getExchanges() != null ? dto.getExchanges() : new ArrayList<>();
        //        System.out.println("       → Exchanges   : " + exchanges.size());

        // P-records: Equity Segment rows (pre-computed)
        this.positions = dto.getPositions() != null ? dto.getPositions() : new ArrayList<>();
        //        System.out.println("       → Equity rows : " + positions.size());

        // V-records: Derivative Segment rows (pre-computed)
        this.vRecords = dto.getVRecords() != null ? dto.getVRecords() : new ArrayList<>();
        //        System.out.println("       → Deriv rows  : " + vRecords.size());

        // D-records: Trade detail rows (Annexure)
        this.details = dto.getDetails() != null ? dto.getDetails() : new ArrayList<>();
        //        System.out.println("       → Trade details: " + details.size());

        // O-records: Pay In / Pay Out Exchange Summary
        this.obligations = dto.getObligations() != null ? dto.getObligations() : new ArrayList<>();
        //        System.out.println("       → ObligRows   : " + obligations.size());

        // F-record: Date & Place
        this.footerDto = dto.getFooter() != null ? dto.getFooter() : new DatePlaceDto();

        // N-records: Annexure section headers + SubTotals
        this.notes = dto.getNotes() != null ? dto.getNotes() : new ArrayList<>();
        //        System.out.println("       → Notes (N)   : " + notes.size());

        // M-records: Net Obligation rows
        this.margins = dto.getMargins() != null ? dto.getMargins() : new ArrayList<>();
        //        System.out.println("       → NetObl rows : " + margins.size());

        // T-record: Net Obligation Total
        this.obligationTotal = dto.getTotal();

        // U-records: Scrip Summary
        this.uRecords = dto.getURecords() != null ? dto.getURecords() : new ArrayList<>();
        //        System.out.println("       → Scrip rows  : " + uRecords.size());

        // C-records: STT Cash Transaction rows
        this.contracts = dto.getContracts() != null ? dto.getContracts() : new ArrayList<>();
        //        System.out.println("       → STT Cash    : " + contracts.size());

        // R-record: STT Cash Total
        this.roundedTotal = dto.getRoundedTotal();

        // A-records: STT Derivative section header
        this.amounts = dto.getAmounts() != null ? dto.getAmounts() : new ArrayList<>();
        //        System.out.println("       → STT A-recs  : " + amounts.size());

        // L-records: STT Derivative rows
        this.lots = dto.getLots() != null ? dto.getLots() : new ArrayList<>();
        //        System.out.println("       → STT L-recs  : " + lots.size());

        // G-records: Daily Margin rows
        this.generals = dto.getGenerals() != null ? dto.getGenerals() : new ArrayList<>();
        //        System.out.println("       → Margin rows : " + generals.size());

        // J-record: Daily Margin Total (Summary of all Exchanges)
        this.journal = dto.getJournal();

        // K-records: Margin Pledge Securities rows
        this.securities = dto.getSecurities() != null ? dto.getSecurities() : new ArrayList<>();
        //        System.out.println("       → Pledge rows : " + securities.size());

        // Q-record: Margin Pledge Total
        this.pledgeQuantity = dto.getQuantity();

        //        System.out.println("       ✓ All models loaded from GeojitStatementDTO");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 3 : VALIDATE DATA
    // ═══════════════════════════════════════════════════════════════════════
    public void validateData() throws Exception {
        //        System.out.println("   [3/6] Validating data...");
        if (header == null) logger.warn("CRITICAL: Header (H record) is missing!");
        if (safe(header.getContractNoteNo()).isEmpty())
            logger.warn("CRITICAL: Contract Note Number is missing!");
        if (safe(header.getNameOfClient()).isEmpty())
            logger.warn("CRITICAL: Client Name is missing!");
        if (positions.isEmpty() && vRecords.isEmpty())
            logger.warn("WARNING: No equity or derivative transactions found!");
        if (obligations.isEmpty())
            logger.warn("WARNING: No exchange summary (O records) found!");
        //        System.out.println("       ✓ Data validation passed");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 4 : INITIALIZE FONTS
    //  FIX: This method is now called inside generatePDF() on every invocation
    //       instead of once in the constructor, to prevent iText font objects
    //       from being shared across multiple PDF documents on warm Lambda reuse.
    // ═══════════════════════════════════════════════════════════════════════
    public void initializeFonts() throws Exception {
        try {
            InputStream normalStream = classLoader.getResourceAsStream("fonts/liberation-regular.ttf");
            if (normalStream != null) {
                this.calibriNormal = PdfFontFactory.createFont(readAllBytes(normalStream), PdfEncodings.IDENTITY_H);
                //                System.out.println("       → Liberation Regular loaded");
            } else {
                this.calibriNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                //                System.out.println("       ⚠ Using fallback Helvetica");
            }
            InputStream boldStream = classLoader.getResourceAsStream("fonts/liberation-bold.ttf");
            if (boldStream != null) {
                this.calibriBold = PdfFontFactory.createFont(readAllBytes(boldStream), PdfEncodings.IDENTITY_H);
                //                System.out.println("       → Liberation Bold loaded");
            } else {
                this.calibriBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                //                System.out.println("       ⚠ Using fallback Helvetica Bold");
            }
            //            System.out.println("       ✓ Fonts initialized");
        } catch (Exception e) {
            logger.error("Error loading fonts: {}" + e.getMessage());
            this.calibriNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            this.calibriBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1)
            buffer.write(data, 0, nRead);
        return buffer.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 5 : GENERATE PDF (MAIN ORCHESTRATION)
    //  [MODIFIED] Signature simplified — uses class-level DTO fields
    //  [REMOVED] All old model parameters from method signature
    //  FIX: initializeFonts() and loadFreshLogo() are now called HERE at the
    //       start of every generatePDF() invocation. This ensures fonts and
    //       logo are always fresh per PDF document and never shared across
    //       warm Lambda container reuses (which caused iText cross-document errors).
    // ═══════════════════════════════════════════════════════════════════════
    public File generatePDF() throws Exception {

        initializeFonts();
        this.logoImageData = loadFreshLogo();
        this.signatureImageData = loadFreshSignature();

        String safeContractNo = safe(header.getContractNoteNo())
                .replace("/", "_").replace("\\", "_").replace(":", "_");

        String fileName = "Geojit_Contract_Note_" + safeContractNo + ".pdf";
        File pdfFile = new File("/tmp/" + fileName);

        String password = generatePdfPassword();

        WriterProperties writerProps = new WriterProperties()
                .setStandardEncryption(
                        password.getBytes(),
                        "GeojitAdmin@2024".getBytes(),
                        EncryptionConstants.ALLOW_PRINTING,
                        EncryptionConstants.ENCRYPTION_AES_128
                );

        PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile), writerProps);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PageNumberEventHandler());

        Document document = new Document(pdfDoc);
        document.setMargins(5f, 10f, 25f, 10f);  // tight on page 1 — no blank space before logo

        try {
            // ── PAGE 1 ──
            addHeader(document);
            addClientInformation(document);
            addEquitySegment(document);
            document.setTopMargin(50f);  // ← move BEFORE derivative segment

            addDerivativeSegment(document);

// ── PAGE 2 ──
            addExchangeSummary(document);


            // 🔥 IMPORTANT FIX: CHECK SPACE BEFORE SIGNATURE + DISCLAIMER
            float remainingHeight = document.getRenderer()
                    .getCurrentArea()
                    .getBBox()
                    .getHeight();

            float requiredHeightForSignatureAndDisclaimer = 220f;
            if (remainingHeight < requiredHeightForSignatureAndDisclaimer) {
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }

            Div finalBlock = new Div().setKeepTogether(true);
            finalBlock.add(getFooterNotesBlock());

            finalBlock.add(getSignatureBlock());
            finalBlock.add(getDisclaimerBlock());

            document.add(finalBlock);
            // 🔥 ALWAYS MOVE ANNEXURE TO NEXT PAGE
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            addTradeDetailsAnnexure(document);

            // ── NEXT PAGES ──
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addMarginStatement(document);
            addMarginPledgeSecurities(document);

        } finally {
            document.close();
        }

        return signPdf(pdfFile);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  QR CODE GENERATOR
    // ═══════════════════════════════════════════════════════════════════════
    private ImageData generateQRCode(String data) throws Exception {
        QRCodeWriter qrWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = qrWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
        return ImageDataFactory.create(out.toByteArray());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 6 : UPLOAD PDF TO S3
    // ═══════════════════════════════════════════════════════════════════════
    public String uploadPDFToS3(File pdfFile) throws Exception {
        //        System.out.println("   [5/6] Uploading PDF to S3...");

        Date now = new Date();
        SimpleDateFormat yearFmt = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthFmt = new SimpleDateFormat("MM");
        SimpleDateFormat dayFmt = new SimpleDateFormat("dd");
        SimpleDateFormat logDateFmt = new SimpleDateFormat("yyyy-MM-dd");

        String year = yearFmt.format(now);
        String month = monthFmt.format(now);
        String day = dayFmt.format(now);
        String logDate = logDateFmt.format(now);
        String clientCode = safe(header.getTradeCode());
        String safeClientCode = clientCode.replace("/", "_").replace("\\", "_").trim();

        String s3Key = "contractNote/" + year + "/" + month + "/" + day + "/" + safeClientCode
                + "/" + pdfFile.getName();


        // MOSL jaisi rich metadata — setUserMetadata use karo
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("partycode", clientCode);
        userMetadata.put("name", safe(header.getNameOfClient()));
        userMetadata.put("email", safe(header.getEmail()));
        userMetadata.put("panno", safe(header.getPanOfClient()));
        userMetadata.put("contract-no", safe(header.getContractNoteNo()));
        userMetadata.put("tradedate", safe(header.getTradeDate()));
        userMetadata.put("senddate", logDate);
        userMetadata.put("filename", pdfFile.getName());
        if (jobId != null) userMetadata.put("jobid", jobId);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setUserMetadata(userMetadata);  // ✅ setUserMetadata use karo

        s3Client.putObject(new PutObjectRequest(PDF_BUCKET, s3Key, pdfFile)
                .withMetadata(metadata));

        //        System.out.println("       ✓ PDF uploaded: " + s3Key);
        //        System.out.println("       ✓ Email saved in metadata: " + safe(header.getEmail()));
        return s3Key;
    }

    private void sendPdfStatusEvent(String s3Key, String error) {
        if (jobId == null) return;
        try {
            String partyCode = safe(header != null ? header.getTradeCode() : "UNKNOWN");
            boolean isSuccess = (error == null);
            JSONObject payload = new JSONObject();
            if (isSuccess) {
                payload.put("s3Key", s3Key);
            } else {
                payload.put("error", error);
            }

            JSONObject event = new JSONObject();
            event.put("jobId", jobId);
            event.put("eventType", isSuccess ? "PDF_GENERATED" : "PDF_FAILED");
            event.put("lambdaName", "create-pdf-geojit");
            event.put("partyCode", partyCode);
            event.put("payload", payload);
            event.put("eventTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

            AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
            String queueUrl = sqsClient.getQueueUrl(STATUS_QUEUE_NAME).getQueueUrl();
            SendMessageRequest msgRequest = new SendMessageRequest(queueUrl, event.toString());
            msgRequest.setMessageGroupId("status-" + jobId);
            msgRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
            sqsClient.sendMessage(msgRequest);
            sqsClient.shutdown();

            logger.info("{} event sent | jobId={} | partyCode={}", isSuccess ? "PDF_GENERATED" : "PDF_FAILED", jobId, partyCode);
        } catch (Exception e) {
            logger.error("Error sending PDF status event: {}", e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 7 : PRESIGNED URL
    // ═══════════════════════════════════════════════════════════════════════
    private String generatePresignedUrl(String s3Key) {
        //        System.out.println("   [6/6] Generating presigned URL...");
        Date expiration = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7);
        GeneratePresignedUrlRequest req =
                new GeneratePresignedUrlRequest(PDF_BUCKET, s3Key)
                        .withMethod(com.amazonaws.HttpMethod.GET)
                        .withExpiration(expiration);
        URL url = s3Client.generatePresignedUrl(req);
        //        System.out.println("       ✓ Presigned URL generated (7 days)");
        return url.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAGE NUMBER + LOGO EVENT HANDLER (unchanged)
    // ═══════════════════════════════════════════════════════════════════════
    // FIND AND REPLACE THIS ENTIRE CLASS:
//    private class PageNumberEventHandler implements IEventHandler {
//        @Override
//        public void handleEvent(Event event) {
//            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
//            PdfDocument pdfDoc = docEvent.getDocument();
//            PdfPage page = docEvent.getPage();
//            int pageNumber = pdfDoc.getPageNumber(page);
//            Rectangle pageSize = page.getPageSize();
//
//            PdfCanvas pdfCanvas = new PdfCanvas(
//                    page.newContentStreamBefore(), page.getResources(), pdfDoc);
//
//            // ── FIX: Use pdfCanvas directly instead of layout Canvas for logo ──
//            // Old code used new Image(logoImageData) inside a layout Canvas
//            // which caused "indirect object belongs to other PDF document" error
//            if (pageNumber > 1 && logoImageData != null) {
//                try {
//                    float logoWidth = 180f;
//                    float logoHeight = 40f;
//                    float logoX = 15f;
//                    float logoY = pageSize.getTop() - logoHeight - 5f;
//
//                    pdfCanvas.addImageFittedIntoRectangle(
//                            logoImageData,
//                            new Rectangle(logoX, logoY, logoWidth, logoHeight),
//                            false
//                    );
//                } catch (Exception e) {
//                    System.err.println("Logo draw failed on page "
//                            + pageNumber + ": " + e.getMessage());
//                }
//            }
//
//            // ── Page number text (Canvas for text is fine) ──
//            Canvas canvas = new Canvas(pdfCanvas, pageSize);
//            canvas.add(new Paragraph("Page No : " + pageNumber)
//                    .setFont(calibriNormal)
//                    .setFontSize(FONT_SIZE_SMALL)
//                    .setTextAlignment(TextAlignment.LEFT)
//                    .setFixedPosition(pageNumber, 15, 10, pageSize.getWidth() - 30));
//            canvas.close();
//        }
//    }
    private class PageNumberEventHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdfDoc.getPageNumber(page);
            Rectangle pageSize = page.getPageSize();

            PdfCanvas pdfCanvas = new PdfCanvas(
                    page.newContentStreamAfter(), page.getResources(), pdfDoc);

            if (pageNumber > 1 && logoImageData != null) {
                try {
                    float logoWidth  = 180f;
                    float logoHeight = 40f;
                    float logoX      = 15f;
                    float logoY      = pageSize.getTop() - logoHeight - 5f;

                    pdfCanvas.addImageFittedIntoRectangle(
                            logoImageData,
                            new Rectangle(logoX, logoY, logoWidth, logoHeight),
                            false
                    );
                } catch (Exception e) {
                    logger.error("Logo draw failed on page "
                            + pageNumber + ": " + e.getMessage());
                }
            }

            // Draw white background strip at absolute bottom of page
            pdfCanvas.saveState();
            pdfCanvas.setFillColor(ColorConstants.WHITE);
            pdfCanvas.rectangle(0, 0, pageSize.getWidth(), 20f);
            pdfCanvas.fill();
            pdfCanvas.restoreState();

            // Draw page number using raw PdfCanvas — bypasses layout engine completely
            // This guarantees it always sits at the absolute bottom of the page
            try {
                pdfCanvas.beginText()
                        .setFontAndSize(calibriNormal, FONT_SIZE_SMALL)
                        .moveText(15, 7)
                        .showText("Page No : " + pageNumber)
                        .endText();
            } catch (Exception e) {
                logger.error("Page number draw failed: " + e.getMessage());
            }
        }
    }
    // ═══════════════════════════════════════════════════════════════════════
    //  HEADER SECTION
    //  [MODIFIED] dealingOffice object removed — address now hardcoded constant
    //  [REMOVED] DealingOfficeAddress parameter
    // ═══════════════════════════════════════════════════════════════════════
    private void addHeader(Document document) {
        try {
            // ── Logo + Title + ORIGINAL FOR RECIPIENT ──
            Table headerTable = new Table(new float[]{1.2f, 5.5f, 1.3f});
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setBorder(Border.NO_BORDER);
            headerTable.setMarginTop(0).setMarginBottom(0);

            Cell logoCell = new Cell().setBorder(Border.NO_BORDER)
                    .setPadding(0).setMarginTop(-5)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            if (logoImageData != null) {
                Image logo = new Image(logoImageData);
                logo.scaleToFit(180, 80);
                logoCell.add(logo);
            }
            headerTable.addCell(logoCell);

            // FIXED — matches original PDF
            headerTable.addCell(new Cell()
                    .add(new Paragraph("CONTRACT NOTE CUM TAX INVOICE")
                            .setFont(calibriBold).setFontSize(14f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMargin(0).setMultipliedLeading(0.9f))
                    .add(new Paragraph("(Tax Invoice under Section 31 of GST Act)")
                            .setFont(calibriBold).setFontSize(10f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(2f).setMarginBottom(0).setMultipliedLeading(0.9f))
                    .setPadding(0).setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER));

            headerTable.addCell(new Cell()
                    .add(new Paragraph("ORIGINAL FOR RECIPIENT")
                            .setFont(calibriBold).setFontSize(8f)
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setMargin(0).setMultipliedLeading(0.9f))
                    .setPadding(0).setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER));

            document.add(headerTable);

            // ── Company info box ──
            Table companyBox = new Table(1);
            companyBox.setWidth(UnitValue.createPercentValue(100));
            companyBox.setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setMarginTop(0);
            // FIXED — matches original PDF
            companyBox.addCell(new Cell()
                    .add(new Paragraph("GEOJIT INVESTMENTS LTD")
                            .setFont(calibriBold).setFontSize(11f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(4f).setMarginBottom(2f).setMultipliedLeading(0.9f))
                    .add(new Paragraph("SEBI REGISTRATION NO : INZ000318938 | CIN No : U66110KL2023PLC080586")
                            .setFont(calibriNormal).setFontSize(8f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(1f).setMarginBottom(1).setMultipliedLeading(0.9f))
                    .add(new Paragraph("7TH FLOOR, 34/659-P, CIVIL LINE ROAD, PADIVATTOM, KOCHI- 682024 | TEL: 0484-2901000 | FAX:0484 2979695 | Website: www.geojit.com/gil")
                            .setFont(calibriNormal).setFontSize(8f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(1f).setMarginBottom(1f).setMultipliedLeading(0.9f))
                    .setPadding(2f).setBorder(Border.NO_BORDER));
            document.add(companyBox);

            // ── Compliance officer box ──
            Table complianceBox = new Table(1);
            complianceBox.setWidth(UnitValue.createPercentValue(100));
            complianceBox.setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setMarginTop(0);
            complianceBox.addCell(new Cell()
                    .add(new Paragraph("NAME OF THE COMPLIANCE OFFICER : Ancy C Sunny | EMAIL: compliance@geojit.com | TEL: 0484-2901000 | EMAIL ID FOR INVESTOR COMPLAINT: grievances@geojit.com")
                            .setFont(calibriNormal).setFontSize(7f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(1f).setMarginBottom(1f).setMultipliedLeading(0.9f))
                    .setPadding(1.5f).setBorder(Border.NO_BORDER));
            document.add(complianceBox);

            // ── Dealing office box — Q1: hardcoded constant ──
            Table dealingBox = new Table(1);
            dealingBox.setWidth(UnitValue.createPercentValue(100));
            dealingBox.setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setMarginTop(0);
            dealingBox.addCell(new Cell()
                    .add(new Paragraph("DEALING OFFICES ADDRESS : " + safe(header.getDealingOfficeAddress()) + " | TELEPHONE NO: " + safe(header.getTelephoneNo()))
                            .setFont(calibriNormal).setFontSize(5f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(1f).setMarginBottom(1f).setMultipliedLeading(0.9f))
                    .setPadding(2f).setBorder(Border.NO_BORDER));
            document.add(dealingBox);

        } catch (Exception e) {
            logger.error("Error in addHeader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CLIENT INFORMATION
    //  [MODIFIED] Uses HeaderDto instead of CustomerModel
    //  [MODIFIED] Address is single field (no concatenation of address1/2/3)
    //  [MODIFIED] Exchange table uses ExchangeClearingDto list directly
    //  [REMOVED] Aggregation of dHeaderTypeList + foHeaderTypeList for exchange table
    //  [REMOVED] ExchangeInfo inner class computation
    // ═══════════════════════════════════════════════════════════════════════
    private void addClientInformation(Document document) {
        Table containerTable = new Table(new float[]{1f, 1f});
        containerTable.setWidth(UnitValue.createPercentValue(100));
        containerTable.setMarginTop(8).setBorder(Border.NO_BORDER);

        // ── LEFT: Client details ──
        Table leftTable = new Table(new float[]{0.8f, 1f, 1f});
        leftTable.setWidth(UnitValue.createPercentValue(100));

        // Row 1: CONTRACT NOTE NO + TRADE DATE
        leftTable.addCell(createFirstRowLabelCell("CONTRACT NOTE NO :"));
        leftTable.addCell(createFirstRowValueCell(safe(header.getContractNoteNo())));
        leftTable.addCell(createFirstRowValueCell("TRADE DATE : " + safe(header.getTradeDate())));

        // Rows 2-8 (no horizontal lines between)
        leftTable.addCell(labelCellNoHorizontal("Name Of the Client :", false, false));
        leftTable.addCell(valueCellSpanning(safe(header.getNameOfClient()), 2, false, true));

        // [MODIFIED] Address is single field from HeaderDto.getAddress()
        leftTable.addCell(labelCellNoHorizontal("Address of the Client :", false, false));
        leftTable.addCell(valueCellSpanning(safe(header.getAddress()), 2, false, true));

        leftTable.addCell(labelCellNoHorizontal("Phone No :", false, false));
        leftTable.addCell(valueCellSpanning(safe(header.getPhoneNo()), 2, false, true));

        leftTable.addCell(labelCellNoHorizontal("TradeCode/UCC of Client :", false, false));
        leftTable.addCell(valueCellSpanning(safe(header.getTradeCode()), 2, false, true));

        // [MODIFIED] Place of Supply now dynamic from HeaderDto.getPlaceOfSupply()
        leftTable.addCell(labelCellNoHorizontal("Place Of Supply [State Code] :", false, false));
        leftTable.addCell(valueCellSpanning(safe(header.getPlaceOfSupply()), 2, false, true));

        leftTable.addCell(labelCellNoHorizontal("Invoice Reference Number(IRN) :", false, false));
        leftTable.addCell(valueCellSpanning(safe(header.getInvoiceReferenceNumber()), 2, false, false));

        leftTable.addCell(labelCellNoHorizontal("GST Identification No. :", false, true));
        leftTable.addCell(valueCellSpanning(safe(header.getGstIdentificationNo()), 2, true, false));

        // ── RIGHT: Exchange table — [MODIFIED] direct from ExchangeClearingDto list ──
        Table exchangeTable = new Table(new float[]{2.2f, 1f, 1.1f, 1.3f, 1f});
        exchangeTable.setWidth(UnitValue.createPercentValue(100)).setMarginTop(0);

        exchangeTable.addHeaderCell(exchangeHeaderNoGrey("EXCHANGE /\nCLEARING\nCORPORATION"));
        exchangeTable.addHeaderCell(exchangeHeaderNoGrey("SEGMENT"));
        exchangeTable.addHeaderCell(exchangeHeaderNoGrey("STTLNO"));
        exchangeTable.addHeaderCell(exchangeHeaderNoGrey("STTLDATE"));
        exchangeTable.addHeaderCell(exchangeHeaderNoGrey("UCCODE"));

        // [MODIFIED] Iterate ExchangeClearingDto directly — no aggregation
        for (ExchangeClearingDto e : exchanges) {
            String exch = safe(e.getExchangeClearingCorporation());
            String seg = safe(e.getSegment());
            String sttlNo = safe(e.getSettlementNo());
            String sttlDt = safe(e.getSettlementDate());
            String ucCode = safe(e.getUcCode());

            // Display segment alias: EN for CAPITAL / FO for FUTURES
            String displaySeg = seg;
            if ("CAPITAL".equalsIgnoreCase(seg)) displaySeg = "EN";
            else if ("FUTURES".equalsIgnoreCase(seg)) displaySeg = "FO";

            // Exchange / NCL format
            String exchDisplay = exch.contains("/") ? exch : exch + " / NCL";

            exchangeTable.addCell(exchangeData(exchDisplay, TextAlignment.LEFT));
            exchangeTable.addCell(exchangeData(displaySeg, TextAlignment.CENTER));
            exchangeTable.addCell(exchangeData(sttlNo, TextAlignment.CENTER));
            exchangeTable.addCell(exchangeData(sttlDt, TextAlignment.CENTER));
            exchangeTable.addCell(exchangeData(ucCode, TextAlignment.CENTER));
        }

        containerTable.addCell(new Cell().add(leftTable)
                .setBorder(Border.NO_BORDER).setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP));
        containerTable.addCell(new Cell().add(exchangeTable)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP));
        document.add(containerTable);

        // Sir/Madam line — [MODIFIED] PAN from HeaderDto.getPanOfClient()
        document.add(new Paragraph()
                .add(new Text("Sir/Madam, I / We have this day done by your order and on your account the following transactions: ")
                        .setFont(calibriNormal).setFontSize(7f))
                .add(new Text("               ")
                        .setFont(calibriNormal).setFontSize(7f))
                .add(new Text("PAN of the Client : " + safe(header.getPanOfClient()))
                        .setFont(calibriNormal).setFontSize(7f))
                .setMarginTop(20).setMarginBottom(10).setMultipliedLeading(1.0f));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EQUITY SEGMENT
    //  [MODIFIED] Uses EquitySegmentDto (tag P) — ALL values pre-computed
    //  [REMOVED] aggregateEquityData() method entirely
    //  [REMOVED] EquityAggregation inner class
    //  [REMOVED] WAP, brokerage, totalValue calculations
    //  ⚠️ Zero calculation — direct DTO render
    // ═══════════════════════════════════════════════════════════════════════
    private void addEquitySegment(Document document) {
        //        System.out.println("📊 addEquitySegment — positions.size()=" + positions.size());
        if (positions.isEmpty()) {
            //            System.out.println("       → No equity positions, skipping");
            return;
        }

        document.add(new Paragraph("Equity Segment")
                .setFont(calibriBold).setFontSize(FONT_SIZE_HEADER)
                .setFontColor(ColorConstants.BLACK).setUnderline()
                .setMarginTop(6).setMarginBottom(8));

        float[] cols = {0.6f, 1.4f, 0.5f, 0.6f, 0.6f, 0.7f, 0.8f, 0.5f, 0.6f, 0.6f, 0.7f, 0.8f, 0.5f, 0.8f};
        Table table = new Table(UnitValue.createPercentArray(cols));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);
        table.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));

        // ROW 1: Group headers
        table.addHeaderCell(new Cell(1, 2)
                .add(new Paragraph("Security Description").setFont(calibriNormal).setFontSize(FONT_SIZE_SMALL)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addHeaderCell(new Cell(1, 5)
                .add(new Paragraph("Buy").setFont(calibriNormal).setFontSize(FONT_SIZE_DATA)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addHeaderCell(new Cell(1, 5)
                .add(new Paragraph("Sell").setFont(calibriNormal).setFontSize(FONT_SIZE_DATA)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addHeaderCell(new Cell(1, 2)
                .add(new Paragraph("Net Obligation for ISIN\n[Before Levies] (Rs)*")
                        .setFont(calibriNormal).setFontSize(FONT_SIZE_TINY)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));

        // ROW 2: Column headers
        table.addHeaderCell(createEquityHeaderCellWithPadding("ISIN"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Security\nName /\nSymbol"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Quantity"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("WAP\n(across\nexchanges)"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Brokerage\nPer Share\n(Rs)"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("WAP (across\nexchanges)\nafter\nbrokerage\n(Rs)"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Total Buy\nValue after\nbrokerage"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Quantity"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("WAP\n(across\nexchanges)"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Brokerage\nPer Share\n(Rs)"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("WAP (across\nexchanges)\nafter\nbrokerage\n(Rs)"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Total Sell\nValue after\nbrokerage"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Net\nQuantity"));
        table.addHeaderCell(createEquityHeaderCellWithPadding("Net Obligation\nfor ISIN"));

        // DATA ROWS — [MODIFIED] direct from EquitySegmentDto, NO calculation
        for (EquitySegmentDto p : positions) {
            table.addCell(createEquityDataCell(safe(p.getIsin()), TextAlignment.LEFT));
            table.addCell(createEquityDataCell(safe(p.getSecurityNameSymbol()), TextAlignment.LEFT, true));
            table.addCell(createEquityDataCell(safe(p.getBuyQuantity()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getBuyWAP()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getBuyBrokeragePerShare()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getBuyWAPAfterBrokerage()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getTotalBuy()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getSellQuantity()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getSellWAP()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getSellBrokeragePerShare()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getSellWAPAfterBrokerage()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getTotalSellValueAfterBrokerage()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getNetQuantity()), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(safe(p.getNetObligation()), TextAlignment.RIGHT));
        }

        document.add(table);
        document.add(new Paragraph("* Exchange-wise details of orders and trades are provided in separate annexure.")
                .setFontSize(FONT_SIZE_TINY).setItalic().setMarginTop(10));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DERIVATIVE SEGMENT
    //  [MODIFIED] Uses DerivativeSegmentDto (tag V) — ALL values pre-computed
    //  [REMOVED] FOHeaderTypeModel, SFuturesHeaderTypeModel processing
    //  [REMOVED] convertFuturesDataToFOFormat() call
    //  [REMOVED] WAP-after-brokerage, netTotal computation
    //  ⚠️ Zero calculation — direct DTO render
    // ═══════════════════════════════════════════════════════════════════════
    private void addDerivativeSegment(Document document) {
        if (vRecords.isEmpty()) {
            return;
        }

        // Only check page break when equity segment was also rendered
        // (no equity = derivative stays on page 1, no blank space)
        if (!positions.isEmpty()) {
            addPageBreakIfNeeded(document, 250);
        }

        document.add(new Paragraph("Derivative Segment")
                .setFont(calibriBold).setFontSize(FONT_SIZE_HEADER)
                .setFontColor(ColorConstants.BLACK).setUnderline()
                .setMarginTop(positions.isEmpty() ? 6 : 25)
                .setMarginBottom(8));

        float[] cols = {2.5f, 0.6f, 0.7f, 0.9f, 0.9f, 0.8f, 0.9f, 0.9f, 1f, 1f};

        // SINGLE TABLE — headers + data together
        // iText automatically repeats header rows when table spans multiple pages
        // This fixes: (1) invisible header table (2) data table with no column headers
        Table table = new Table(UnitValue.createPercentArray(cols));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        // Column header cells — these repeat on every page the table spans
        table.addHeaderCell(createDerivativeHeaderCellNormal("Contract Description"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("Buy[B]/\nSell[S]/\nBF/CF"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("Quantity"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("WAP per unit\n(In Foreign\nCurrency)"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("WAP per unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("Brokerage\nPer Unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("WAP per unit\nAfter\nBrokerage(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("Closing Rate\nper Unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("Net Total\n(Before Levies)"));
        table.addHeaderCell(createDerivativeHeaderCellNormal("Remarks"));

        // Data rows
        for (DerivativeSegmentDto v : vRecords) {
            table.addCell(createDerivativeDataCell(safe(v.getContractDescription()), TextAlignment.LEFT));
            table.addCell(createDerivativeDataCell(safe(v.getBuySell()), TextAlignment.CENTER));
            table.addCell(createDerivativeDataCell(safe(v.getQuantity()), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(safe(v.getWapPerUnitForeignCurrency()), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(safe(v.getWapPerUnitRs()), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(safe(v.getBrokeragePerUnitRs()), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(safe(v.getWapPerUnitAfterBrokerageRs()), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(safe(v.getClosingRatePerUnit()), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(safe(v.getNetTotal()), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(safe(v.getRemarks()), TextAlignment.LEFT));
        }

        document.add(table);
        document.add(new Paragraph(
                "* Exchange-wise details of orders and trades are provided in separate annexure.")
                .setFontSize(FONT_SIZE_TINY).setItalic().setMarginTop(10).setMarginBottom(10));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EXCHANGE SUMMARY
    //  [MODIFIED] Uses PayInPayOutDto (tag O) — direct render ALL rows
    //  [REMOVED] FooterModelV2 processing
    //  [REMOVED] All total accumulation loops (totalPayInOut, totalSTT, etc.)
    //  Q2 ANSWER: TOTAL(NET) row comes from DTO as an O-record
    //             (nameOfExchangeSegment = "TOTAL(NET)" or "Total")
    //  Q4 ANSWER: GST base derived from last O-record SGST+CGST values
    //  ⚠️ Zero calculation — direct DTO render
    // ═══════════════════════════════════════════════════════════════════════
    private void addExchangeSummary(Document document) {
        if (obligations.isEmpty()) {
            //            System.out.println("       → No obligations (O records) found");
            return;
        }

        float[] cols = {1f, 0.7f, 1f, 1f, 1f, 1f, 0.4f, 0.7f, 1f, 0.6f, 1f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(cols));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY).setMarginTop(8);

        table.addHeaderCell(createHeaderCellGreen("Name Of Exchange\n& Segment"));
        table.addHeaderCell(createHeaderCellGreen("PAY IN/PAY OUT\nOBLIGATION"));
        table.addHeaderCell(createHeaderCellGreen("SECURITIES\nTRANSACTION\nTAX"));
        table.addHeaderCell(createHeaderCellGreen("SGST (**)\n9%"));
        table.addHeaderCell(createHeaderCellGreen("CGST (**)\n9%"));
        table.addHeaderCell(createHeaderCellGreen("IGST (**)\n18%"));
        table.addHeaderCell(createHeaderCellGreen("TDS"));
        table.addHeaderCell(createHeaderCellGreen("Exchange\nTransactn\nCharges"));
        table.addHeaderCell(createHeaderCellGreen("SEBI\nTurnover\nFees"));
        table.addHeaderCell(createHeaderCellGreen("Additional\nCess ***"));
        table.addHeaderCell(createHeaderCellGreen("Stamp\nDuty"));
        table.addHeaderCell(createHeaderCellGreen("Net Amount Receivable By\nClient/(Payable by Client)"));

        // [MODIFIED] Render ALL O-records directly — last one is TOTAL(NET) from DTO
        // Determine the LAST O-record for GST footnote derivation
        PayInPayOutDto lastORecord = obligations.isEmpty() ? null : obligations.get(obligations.size() - 1);

        for (PayInPayOutDto o : obligations) {
            String exSeg = safe(o.getNameOfExchangeSegment());
            // Mark the total row if it matches standard total labels
            boolean isTotalRow = exSeg.toUpperCase().contains("TOTAL");

            table.addCell(createCell(exSeg, TextAlignment.LEFT, isTotalRow));
            table.addCell(createCell(safe(o.getPayInPayOut()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getSecuritiesTransactions()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getSgst()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getCgst()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getIgst()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getTds()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getExchangeTransactionCharges()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getSebiTurnover()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getAdditionalCess()), TextAlignment.RIGHT, false));
            table.addCell(createCell(safe(o.getStampDuty()), TextAlignment.RIGHT, false));
            String netCell = safe(o.getNetAmountReceivableByClient());
            if (isTotalRow) netCell = netCell + " *";
            table.addCell(createCell(netCell, TextAlignment.RIGHT, false));
        }

        document.add(table);

        // Q4: GST base derived from total O-record's (SGST+CGST) / 0.18
        // This is a display-only derivation from DTO values, not a business aggregation
        String gstBaseStr = "0.00";
        if (lastORecord != null) {
            double sgst = parseDouble(safe(lastORecord.getSgst()));
            double cgst = parseDouble(safe(lastORecord.getCgst()));
            double igst = parseDouble(safe(lastORecord.getIgst()));
            double totalGst = sgst + cgst + igst;
            double gstBase = (totalGst > 0) ? (totalGst / 0.18) : 0.0;
            gstBaseStr = decimalFormat.format(gstBase);
        }

        document.add(new Paragraph(
                "** SGST: - State GST; CGST:-Central GST; IGST:-Integrated GST. " +
                        "GST is calculated on Brokerage, Exchange Transaction Charges and SEBI Fee. " +
                        "(18% of Rs." + gstBaseStr + ")")
                .setFontSize(FONT_SIZE_TINY)
                .setMarginTop(2)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMultipliedLeading(1.0f));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FOOTER NOTES (unchanged layout)
    // ═══════════════════════════════════════════════════════════════════════

    private Div getFooterNotesBlock() {

        Div wrapper = new Div();

        Paragraph notes = new Paragraph()
                .setFont(calibriNormal)
                .setFontSize(7.5f)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(6);

        notes.add("# Brokerage shown is per unit in the case of Equities and total brokerage for the particular transaction in the case of F&O and Currency trades.\n");
        notes.add("* Any other charges (DIS charge/AMC/Overdue charges etc.) which are due to us will be debited from the net amount.\n");
        notes.add("Transactions mentioned in this contract note cum bill shall be governed and subject to the Rules, Bye-laws, Regulations and Circulars of the respective Exchanges on which trades have been executed and Securities and Exchange Board of India issued from time to time.\n");
        notes.add("It shall also be subject to the relevant Acts, Rules, Regulations, Directives, Notifications, Guidelines (including GST Laws) & Circulars issued by SEBI / Government of India / State Governments and Union Territory Governments issued from time to time.\n");
        notes.add("The Exchanges provide Complaint Resolution, Arbitration and Appellate arbitration facilities at the Regional Arbitration Centres (RAC). The client may approach its nearest centre, details of which are available on respective Exchange's website.\n");
        notes.add("Please visit www.nseindia.com for NSE, www.bseindia.com for BSE and www.msei.in for MSEI.\n");

        wrapper.add(notes);

        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SIGNATURE
    //  [MODIFIED] Date and Place from DatePlaceDto (tag F)
    //  [REMOVED] customer.getTransactionDate() usage
    //  [REMOVED] hardcoded "PALARIVATTOM" — now from footerDto.getPlace()
    //  QR code built from HeaderDto fields
    // ═══════════════════════════════════════════════════════════════════════


    // 🔥 NEW METHOD (ADD BELOW addSignature)
    private Div getSignatureBlock() {

        String tradeDate = (footerDto != null) ? safe(footerDto.getDate()) : "";
        String place = (footerDto != null) ? safe(footerDto.getPlace()) : "PALARIVATTOM";

        Div wrapper = new Div();

        Table table = new Table(new float[]{1, 1});
        table.setWidth(UnitValue.createPercentValue(100)).setMarginTop(8).setBorder(Border.NO_BORDER);

        // LEFT SIDE
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER);

        Table dateQRTable = new Table(new float[]{1f, 0.5f});
        dateQRTable.setBorder(Border.NO_BORDER).setWidth(UnitValue.createPercentValue(100));

        Cell dateCell = new Cell().setBorder(Border.NO_BORDER);
        dateCell.add(new Paragraph("Date: " + tradeDate).setFont(calibriNormal).setFontSize(FONT_SIZE_SMALL));
        dateCell.add(new Paragraph("Place: " + place).setFont(calibriNormal).setFontSize(FONT_SIZE_SMALL));

        dateQRTable.addCell(dateCell);

        Cell qrCell = new Cell().setBorder(Border.NO_BORDER);

        try {
            String qrData = "Contract No: " + safe(header.getContractNoteNo())
                    + "|PAN: " + safe(header.getPanOfClient())
                    + "|Date: " + tradeDate;

            Image qrImage = new Image(generateQRCode(qrData));
            qrImage.scaleToFit(80, 80);
            qrCell.add(qrImage);
        } catch (Exception e) {
            logger.error("QR error: " + e.getMessage());
        }

        dateQRTable.addCell(qrCell);
        leftCell.add(dateQRTable);

        // RIGHT SIDE
        // RIGHT SIDE
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER);
        Div rightContent = new Div().setMarginLeft(150);

        rightContent.add(new Paragraph("Yours Faithfully,")
                .setFont(calibriNormal).setFontSize(FONT_SIZE_SMALL)
                .setMarginTop(0).setMarginBottom(4f).setMultipliedLeading(1.0f));

        rightContent.add(new Paragraph("GEOJIT INVESTMENTS LTD")
                .setFont(calibriNormal).setFontSize(FONT_SIZE_SMALL)
                .setMarginTop(0).setMarginBottom(0).setMultipliedLeading(1.0f));

        rightContent.add(new Paragraph("(PAN : AAKCG3453A, GSTIN : 32AAKCG3453A1Z4)")
                .setFont(calibriNormal).setFontSize(FONT_SIZE_TINY)
                .setMarginTop(0).setMarginBottom(0).setMultipliedLeading(1.0f));

        rightContent.add(new Paragraph("Description of Service : STOCK BROKER")
                .setFont(calibriNormal).setFontSize(FONT_SIZE_TINY)
                .setMarginTop(0).setMarginBottom(0).setMultipliedLeading(1.0f));

        rightContent.add(new Paragraph("Service Account Code(SAC) : 997152")
                .setFont(calibriNormal).setFontSize(FONT_SIZE_TINY)
                .setMarginTop(0).setMarginBottom(0).setMultipliedLeading(1.0f));

        if (signatureImageData != null) {
            Image sign = new Image(signatureImageData);
            sign.scaleToFit(120, 40);
            sign.setMarginTop(4).setMarginBottom(2);
            rightContent.add(sign);
        }

        rightContent.add(new Paragraph("Johny Varghese")
                .setFont(calibriNormal).setFontSize(FONT_SIZE_SMALL)
                .setMarginTop(0).setMarginBottom(0).setMultipliedLeading(1.0f));

        rightContent.add(new Paragraph("(Name & Signature of Authorised Signatory)")
                .setFont(calibriNormal).setFontSize(FONT_SIZE_TINY)
                .setMarginTop(0).setMarginBottom(0).setMultipliedLeading(1.0f));

        rightCell.add(rightContent);

        table.addCell(leftCell);
        table.addCell(rightCell);

        wrapper.add(table);

        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DISCLAIMER (unchanged)
    // ═══════════════════════════════════════════════════════════════════════
    private Div getDisclaimerBlock() {

        Div wrapper = new Div();

        Paragraph disclaimer = new Paragraph()
                .add(new Text("Disclaimer: ").setFont(calibriBold))
                .add(new Text("Purchase of REs (Rights Entitlements) only gives right to participate in the ongoing " +
                        "Rights Issue of the concerned company. REs which are neither subscribed by making an application " +
                        "with requisite application money nor renounced on or before the Issue Closing Date shall lapse and " +
                        "shall be extinguished after the Issue Closing Date.").setFont(calibriBold))
                .setFontSize(FONT_SIZE_TINY)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(6);

        Paragraph end = new Paragraph(" * End Of Contract *")
                .setFont(calibriNormal)
                .setFontSize(FONT_SIZE_SMALL)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(6);

        wrapper.add(disclaimer);
        wrapper.add(end);

        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TRADE DETAILS ANNEXURE (Page 3)
    //  [MODIFIED] Uses NameAndExchangeTotalDto (N) + NameClearingCorporationDto (D)
    //  Q7 ANSWER: N-record defines section header/subtotal;
    //             D-records matched by securityContractDescription to that N-record
    // ═══════════════════════════════════════════════════════════════════════
    private void addTradeDetailsAnnexure(Document document) {
        //        System.out.println("📊 addTradeDetailsAnnexure — notes=" + notes.size() + " details=" + details.size());

        Paragraph title = new Paragraph("Annexure - Trade Details")
                .setFont(calibriBold)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(0)
                .setMarginBottom(4)
                .setKeepWithNext(true);

        document.add(title);

        // "Name of the Clearing Corporation" — static label per original PDF
        document.add(new Paragraph("Name of the Clearing Corporation & Segment : NSE Clearing Limited")
                .setFont(calibriNormal).setFontSize(10f).setMarginTop(1).setMarginBottom(1));

        // ── Global header table (column headers only — appears once) ──
//        float[] cols = {0.5f, 0.5f, 0.5f, 0.5f, 2.0f, 0.5f, 0.6f, 0.9f, 0.9f, 0.8f, 0.8f, 0.9f, 1.0f, 0.8f};
        float[] cols = {0.9f, 0.6f, 0.9f, 0.6f, 2.0f, 0.4f, 0.5f, 0.8f, 0.8f, 0.6f, 0.7f, 0.8f, 0.9f, 0.6f};

        Table globalHeader = new Table(UnitValue.createPercentArray(cols));
        globalHeader.setWidth(UnitValue.createPercentValue(100));
        globalHeader.setFontSize(FONT_SIZE_TINY);
        globalHeader.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));

        globalHeader.addHeaderCell(createSimpleHeaderCell("Order\nNo."));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Order\nTime"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Trade\nNo."));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Trade\nTime"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Security/Contract\nDescription"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Buy/\nSell"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Quantity"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Gross Rate/Trade\nPrice Per Unit\n(in foreign currency)"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Gross Rate/Trade\nPrice Per Unit\n(Rs)"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("#Brokerage\n(Rs)"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Net Rate\nPer Unit\n(Rs)"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Closing Rate\nPer Unit(Only\nFor derivatives)"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Net total\n(Before Levies)\n(Rs.)"));
        globalHeader.addHeaderCell(createSimpleHeaderCell("Remarks"));
        document.add(globalHeader);

        // ── Process each N-record as a section ──
        // Q7: N-record = section header + subtotal; D-records matched by security description
        for (NameAndExchangeTotalDto n : notes) {
            String nExchange = safe(n.getNameOfExchange());
            String nSegment = safe(n.getSegment());   // broker code line e.g. "BSE Broker Code:0328"
            String nSecurity = safe(n.getSecurityContractDescription());

            // Section exchange header line
            document.add(new Paragraph("Name Of Exchange & Segment : " + nExchange
            + " | " + nSegment)
                    .setFont(calibriBold).setFontSize(7f).setMarginTop(1).setMarginBottom(1));

            // Collect matching D-records for this N-record's security
            // Q7: match on securityContractDescription
            List<NameClearingCorporationDto> sectionDetails =
                    n.getDetails() != null ? n.getDetails() : new ArrayList<>();

            // Single table — data rows + SubTotal row together
            // This guarantees NIFTY in SubTotal aligns exactly under NIFTY data rows
            Table dataTable = new Table(UnitValue.createPercentArray(cols));
            dataTable.setWidth(UnitValue.createPercentValue(100));
            dataTable.setFontSize(FONT_SIZE_TINY);
            dataTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));

            for (NameClearingCorporationDto d : sectionDetails) {
                dataTable.addCell(createSimpleDataCell(safe(d.getOrderNo()),                         TextAlignment.CENTER));
                dataTable.addCell(createSimpleDataCell(safe(d.getOrderTime()),                       TextAlignment.CENTER));
                dataTable.addCell(createSimpleDataCell(safe(d.getTradeNo()),                         TextAlignment.CENTER));
                dataTable.addCell(createSimpleDataCell(safe(d.getTradeTime()),                       TextAlignment.CENTER));
                dataTable.addCell(createSimpleDataCell(safe(d.getSecurityContractDescription()),     TextAlignment.LEFT));
                dataTable.addCell(createSimpleDataCell(safe(d.getBuySell()),                         TextAlignment.CENTER));
                dataTable.addCell(createSimpleDataCell(safe(d.getQuantity()),                        TextAlignment.RIGHT));
                dataTable.addCell(createSimpleDataCell(safe(d.getGrossRatePricePerUnitForeignCcy()), TextAlignment.RIGHT));
                dataTable.addCell(createSimpleDataCell(safe(d.getGrossRatePricePerUnitRs()),         TextAlignment.RIGHT));
                dataTable.addCell(createSimpleDataCell(safe(d.getBrokerageRs()),                     TextAlignment.RIGHT));
                dataTable.addCell(createSimpleDataCell(safe(d.getNetRatePerUnitRs()),                TextAlignment.RIGHT));
                dataTable.addCell(createSimpleDataCell(safe(d.getClosingRatePerUnit()),              TextAlignment.RIGHT));
                dataTable.addCell(createSimpleDataCell(safe(d.getNetTotalBeforeLevies()),            TextAlignment.RIGHT));
                dataTable.addCell(createSimpleDataCell(safe(d.getRemarks()),                         TextAlignment.LEFT));
            }

            dataTable.addCell(new Cell(1, 4)
                    .add(new Paragraph("SubTotal").setFontSize(FONT_SIZE_TINY)
                            .setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER))
                    .setBorder(getStandardBorder())
                    .setPadding(1)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE));
            dataTable.addCell(new Cell()
                    .add(new Paragraph(nSecurity).setFontSize(FONT_SIZE_TINY)
                            .setFont(calibriNormal).setTextAlignment(TextAlignment.LEFT))
                    .setBorder(getStandardBorder())
                    .setPadding(1)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE));
            dataTable.addCell(createSimpleDataCell(safe(n.getBuySell()),                  TextAlignment.CENTER));
            dataTable.addCell(createSimpleDataCell(safe(n.getQuantity()),                 TextAlignment.RIGHT));
            dataTable.addCell(createSimpleDataCell(safe(n.getGrossRateForeignCurrency()), TextAlignment.RIGHT));
            dataTable.addCell(createSimpleDataCell(safe(n.getGrossRateRs()),              TextAlignment.RIGHT));
            dataTable.addCell(createSimpleDataCell(safe(n.getBrokerage()),                TextAlignment.RIGHT));
            dataTable.addCell(createSimpleDataCell(safe(n.getNetRate()),                  TextAlignment.RIGHT));
            dataTable.addCell(createSimpleDataCell(safe(n.getClosingRate()),              TextAlignment.RIGHT));
            dataTable.addCell(createSimpleDataCell(safe(n.getNetTotal()),                 TextAlignment.RIGHT));
            dataTable.addCell(createSimpleDataCell(safe(n.getRemarks()),                  TextAlignment.LEFT));

            document.add(dataTable);
            document.add(new Paragraph("").setMarginBottom(4));
        }

        // Net Obligation section
        addNetObligationSection(document);

        // Scrip Summary
        if (!uRecords.isEmpty()) {
            addScripSummaryOnPage3(document);
        }

        // STT Cash
        addSTTStatementCashTransactions(document);

        // STT Derivatives
        if (!lots.isEmpty()) {
            addSTTStatementDerivatives(document);
        }
    }


    //Multiple subtotal in annexure for multiple N records. Each N record has its own subtotal line which is the last line of that section. The subtotal line's security description cell shows the N record's security description, and the buy/sell, quantity, rates, net total etc. are the subtotal values for that N record's section.

//    private void addTradeDetailsAnnexure(Document document) {
//
//        Paragraph title = new Paragraph("Annexure - Trade Details")
//                .setFont(calibriBold)
//                .setFontSize(11)
//                .setTextAlignment(TextAlignment.RIGHT)
//                .setMarginTop(0)
//                .setMarginBottom(4)
//                .setKeepWithNext(true);
//        document.add(title);
//
//        document.add(new Paragraph("Name of the Clearing Corporation & Segment : NSE Clearing Limited")
//                .setFont(calibriNormal).setFontSize(10f).setMarginTop(1).setMarginBottom(1));
//
//        // Column widths — same as before
////        float[] cols = {0.5f, 0.5f, 0.5f, 0.5f, 2.0f, 0.5f, 0.6f, 0.9f, 0.9f, 0.8f, 0.8f, 0.9f, 1.0f, 0.8f};
//        float[] cols = {0.9f, 0.6f, 0.9f, 0.6f, 2.0f, 0.4f, 0.5f, 0.8f, 0.8f, 0.6f, 0.7f, 0.8f, 0.9f, 0.6f};
//
//        // ── Global column header table (printed ONCE at the top) ──
//        Table globalHeader = new Table(UnitValue.createPercentArray(cols));
//        globalHeader.setWidth(UnitValue.createPercentValue(100));
//        globalHeader.setFontSize(FONT_SIZE_TINY);
//        globalHeader.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));
//
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Order\nNo."));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Order\nTime"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Trade\nNo."));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Trade\nTime"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Security/Contract\nDescription"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Buy/\nSell"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Quantity"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Gross Rate/Trade\nPrice Per Unit\n(in foreign currency)"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Gross Rate/Trade\nPrice Per Unit\n(Rs)"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("#Brokerage\n(Rs)"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Net Rate\nPer Unit\n(Rs)"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Closing Rate\nPer Unit(Only\nFor derivatives)"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Net total\n(Before Levies)\n(Rs.)"));
//        globalHeader.addHeaderCell(createSimpleHeaderCell("Remarks"));
//        document.add(globalHeader);
//
//        // ══════════════════════════════════════════════════════════════════════
//        // STEP 1: Group N records by exchange.
//        //         Use LinkedHashMap to preserve the order exchanges appear in the file.
//        //         Key   = nameOfExchange (e.g. "BSEFO", "NSEFO")
//        //         Value = ordered list of all N tags for that exchange
//        // ══════════════════════════════════════════════════════════════════════
//        LinkedHashMap<String, List<NameAndExchangeTotalDto>> byExchange = new LinkedHashMap<>();
//        for (NameAndExchangeTotalDto n : notes) {
//            String exch = safe(n.getNameOfExchange());
//            byExchange.computeIfAbsent(exch, k -> new ArrayList<>()).add(n);
//        }
//
//        // ══════════════════════════════════════════════════════════════════════
//        // STEP 2: For each exchange group, print ONE exchange header,
//        //         then loop through its N tags printing D rows + SubTotal.
//        // ══════════════════════════════════════════════════════════════════════
//        for (Map.Entry<String, List<NameAndExchangeTotalDto>> entry : byExchange.entrySet()) {
//            String exchangeName  = entry.getKey();
//            List<NameAndExchangeTotalDto> exchangeNotes = entry.getValue();
//
//            // Get broker code from the first N tag of this exchange (they're all the same)
//            String brokerCodeLine = safe(exchangeNotes.get(0).getSegment());
//
//            // ── Exchange header: printed ONCE per exchange ──
//            document.add(new Paragraph("Name Of Exchange & Segment : " + exchangeName + " | " + brokerCodeLine)
//                    .setFont(calibriBold).setFontSize(7f).setMarginTop(1).setMarginBottom(1));
//
//            // ── Loop through each N tag of this exchange ──
//            for (NameAndExchangeTotalDto n : exchangeNotes) {
//                String nSecurity = safe(n.getSecurityContractDescription());
//                String nBuySell  = safe(n.getBuySell());
//
//                // ── Get D records for this N tag ──
//                // PRIMARY PATH: Parser pre-attached D records to this N tag via n.getDetails()
//                // This is the correct path when your parser reads the file sequentially
//                // and attaches each D tag to the N tag that immediately precedes it.
//                List<NameClearingCorporationDto> dRecords = n.getDetails() != null
//                        ? n.getDetails()
//                        : new ArrayList<>();
//
//                // FALLBACK PATH (uncomment if your parser puts ALL D records in a flat
//                // details list and does NOT pre-attach them to their N tag):
//                // ─────────────────────────────────────────────────────────────────────
//                // List<NameClearingCorporationDto> dRecords = details.stream()
//                //     .filter(d -> {
//                //         String dSec = safe(d.getSecurityContractDescription()).trim();
//                //         String dBS  = safe(d.getBuySell()).trim();
//                //         // Match on BOTH security name AND Buy/Sell
//                //         // Use startsWith because D security names sometimes have trailing space
//                //         return dSec.startsWith(nSecurity.trim())
//                //             && dBS.equalsIgnoreCase(nBuySell);
//                //     })
//                //     .collect(Collectors.toList());
//                // ─────────────────────────────────────────────────────────────────────
//
//                // ── Single table: D rows + SubTotal row together ──
//                // Keeping them in one table ensures SubTotal columns align with D columns.
//                Table dataTable = new Table(UnitValue.createPercentArray(cols));
//                dataTable.setWidth(UnitValue.createPercentValue(100));
//                dataTable.setFontSize(FONT_SIZE_TINY);
//                dataTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));
//
//                // D rows
//                for (NameClearingCorporationDto d : dRecords) {
//                    dataTable.addCell(createSimpleDataCell(safe(d.getOrderNo()),                         TextAlignment.CENTER));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getOrderTime()),                       TextAlignment.CENTER));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getTradeNo()),                         TextAlignment.CENTER));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getTradeTime()),                       TextAlignment.CENTER));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getSecurityContractDescription()),     TextAlignment.LEFT));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getBuySell()),                         TextAlignment.CENTER));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getQuantity()),                        TextAlignment.RIGHT));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getGrossRatePricePerUnitForeignCcy()), TextAlignment.RIGHT));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getGrossRatePricePerUnitRs()),         TextAlignment.RIGHT));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getBrokerageRs()),                     TextAlignment.RIGHT));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getNetRatePerUnitRs()),                TextAlignment.RIGHT));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getClosingRatePerUnit()),              TextAlignment.RIGHT));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getNetTotalBeforeLevies()),            TextAlignment.RIGHT));
//                    dataTable.addCell(createSimpleDataCell(safe(d.getRemarks()),                         TextAlignment.LEFT));
//                }
//
//                // SubTotal row (values come from this N tag)
//                dataTable.addCell(new Cell(1, 4)
//                        .add(new Paragraph("SubTotal").setFontSize(FONT_SIZE_TINY)
//                                .setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER))
//                        .setBorder(getStandardBorder())
//                        .setPadding(1)
//                        .setVerticalAlignment(VerticalAlignment.MIDDLE));
//                dataTable.addCell(new Cell()
//                        .add(new Paragraph(nSecurity).setFontSize(FONT_SIZE_TINY)
//                                .setFont(calibriNormal).setTextAlignment(TextAlignment.LEFT))
//                        .setBorder(getStandardBorder())
//                        .setPadding(1)
//                        .setVerticalAlignment(VerticalAlignment.MIDDLE));
//                dataTable.addCell(createSimpleDataCell(safe(n.getBuySell()),                  TextAlignment.CENTER));
//                dataTable.addCell(createSimpleDataCell(safe(n.getQuantity()),                 TextAlignment.RIGHT));
//                dataTable.addCell(createSimpleDataCell(safe(n.getGrossRateForeignCurrency()), TextAlignment.RIGHT));
//                dataTable.addCell(createSimpleDataCell(safe(n.getGrossRateRs()),              TextAlignment.RIGHT));
//                dataTable.addCell(createSimpleDataCell(safe(n.getBrokerage()),                TextAlignment.RIGHT));
//                dataTable.addCell(createSimpleDataCell(safe(n.getNetRate()),                  TextAlignment.RIGHT));
//                dataTable.addCell(createSimpleDataCell(safe(n.getClosingRate()),              TextAlignment.RIGHT));
//                dataTable.addCell(createSimpleDataCell(safe(n.getNetTotal()),                 TextAlignment.RIGHT));
//                dataTable.addCell(createSimpleDataCell(safe(n.getRemarks()),                  TextAlignment.LEFT));
//
//                document.add(dataTable);
//                document.add(new Paragraph("").setMarginBottom(2));
//            }
//
//            // Small gap between exchange sections
//            document.add(new Paragraph("").setMarginBottom(4));
//        }
//
//        // ── Rest of annexure (unchanged) ──
//        addNetObligationSection(document);
//
//        if (!uRecords.isEmpty()) {
//            addScripSummaryOnPage3(document);
//        }
//
//        addSTTStatementCashTransactions(document);
//
//        if (!lots.isEmpty()) {
//            addSTTStatementDerivatives(document);
//        }
//    }
    // ═══════════════════════════════════════════════════════════════════════
    //  NET OBLIGATION SECTION
    //  [MODIFIED] Uses NetObligationDto (tag M) — pre-computed rows
    //  [MODIFIED] Uses NetObligationTotalDto (tag T) — pre-computed totals
    //  [REMOVED] SCapitalHeaderTypeModel + OHeaderTypeModel computation
    //  [REMOVED] STT calculation from OHeaderTypeModel loop
    //  ⚠️ Zero calculation — all values from DTO
    // ═══════════════════════════════════════════════════════════════════════
    private void addNetObligationSection(Document document) {
        if (margins.isEmpty()) {
            //            System.out.println("       → No net obligation (M records) found");
            return;
        }

        document.add(new Paragraph("Net Obligation (Equity Market)")
                .setFont(calibriBold).setFontSize(12f)
                .setTextAlignment(TextAlignment.LEFT).setMarginTop(2).setMarginBottom(2));

        float[] cols = {0.5f, 2.5f, 0.8f, 0.8f, 0.9f, 0.8f, 0.9f, 0.8f, 0.9f, 1f};
        Table table = new Table(UnitValue.createPercentArray(cols));
        table.setWidth(UnitValue.createPercentValue(100)).setFontSize(7.2f);

        // Row 1: Group headers
        table.addHeaderCell(new Cell(1, 1).add(new Paragraph("").setMargin(0)).setPadding(2));
        table.addHeaderCell(new Cell(1, 1).add(new Paragraph("").setMargin(0)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));
        table.addHeaderCell(new Cell(1, 1).add(new Paragraph("").setMargin(0)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));
        table.addHeaderCell(new Cell(1, 2).add(new Paragraph("Bought").setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(TextAlignment.CENTER).setMargin(0).setMultipliedLeading(1.02f)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(1, 2).add(new Paragraph("Sold").setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(TextAlignment.CENTER).setMargin(0).setMultipliedLeading(1.02f)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(1, 2).add(new Paragraph("Net Obligation").setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(TextAlignment.CENTER).setMargin(0).setMultipliedLeading(1.02f)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(1, 1).add(new Paragraph("").setMargin(0)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));

        // Row 2: Column sub-headers
        table.addHeaderCell(createNetObligationSubHeader("Sl.No"));
        table.addHeaderCell(createNetObligationSubHeader("Security"));
        table.addHeaderCell(createNetObligationSubHeader("Segment"));
        table.addHeaderCell(createNetObligationSubHeader("Quantity"));
        table.addHeaderCell(createNetObligationSubHeader("Rate"));
        table.addHeaderCell(createNetObligationSubHeader("Quantity"));
        table.addHeaderCell(createNetObligationSubHeader("Rate"));
        table.addHeaderCell(createNetObligationSubHeader("Quantity"));
        table.addHeaderCell(createNetObligationSubHeader("Rate"));
        table.addHeaderCell(createNetObligationSubHeader("Amount"));

        // [MODIFIED] Data rows from NetObligationDto directly — NO computation
        for (NetObligationDto m : margins) {
            String seg = safe(m.getSegment());
            if ("CAPITAL".equalsIgnoreCase(seg)) seg = "EN";
            table.addCell(createNetObligationDataCellPro(safe(m.getSlNo()), TextAlignment.CENTER));
            table.addCell(createNetObligationDataCellPro(safe(m.getSecurity()), TextAlignment.LEFT));
            table.addCell(createNetObligationDataCellPro(seg, TextAlignment.CENTER));
            table.addCell(createNetObligationDataCellPro(safe(m.getQuantity1()), TextAlignment.CENTER));
            table.addCell(createNetObligationDataCellPro(safe(m.getRate1()), TextAlignment.CENTER));
            table.addCell(createNetObligationDataCellPro(safe(m.getQuantity2()), TextAlignment.CENTER));
            table.addCell(createNetObligationDataCellPro(safe(m.getRate2()), TextAlignment.CENTER));
            table.addCell(createNetObligationDataCellPro(safe(m.getQuantity3()), TextAlignment.CENTER));
            table.addCell(createNetObligationDataCellPro(safe(m.getRate3()), TextAlignment.CENTER));
            table.addCell(createNetObligationDataCellPro(safe(m.getAmount()), TextAlignment.CENTER));
        }

        // [MODIFIED] Total, STT, Net Amount from NetObligationTotalDto (T-record)
        // [REMOVED] Manual total/STT accumulation loops
        if (obligationTotal != null) {
            // Total row
            table.addCell(new Cell(1, 9).add(new Paragraph("Total").setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(TextAlignment.RIGHT).setMargin(0).setMultipliedLeading(1.02f)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));
            table.addCell(createNetObligationDataCellPro(safe(obligationTotal.getTotal()), TextAlignment.RIGHT));

            // STT row
            table.addCell(new Cell(1, 9).add(new Paragraph("Securities Transaction Tax").setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(TextAlignment.RIGHT).setMargin(0).setMultipliedLeading(1.02f)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));
            table.addCell(createNetObligationDataCellPro(safe(obligationTotal.getSecuritiesTransactionsTax()), TextAlignment.RIGHT));

            // Net Amount row
            table.addCell(new Cell(1, 9).add(new Paragraph("Net Amount").setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(TextAlignment.RIGHT).setMargin(0).setMultipliedLeading(1.02f)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));
            table.addCell(createNetObligationDataCellPro(safe(obligationTotal.getNetAmount()), TextAlignment.RIGHT));
        } else {
            // Q6: Show empty if total not present
            //            System.out.println("       ⚠ NetObligationTotalDto (T record) is null — showing empty");
            for (String label : new String[]{"Total", "Securities Transaction Tax", "Net Amount"}) {
                table.addCell(new Cell(1, 9).add(new Paragraph(label).setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(TextAlignment.RIGHT).setMargin(0)).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));
                table.addCell(createNetObligationDataCellPro("", TextAlignment.RIGHT));
            }
        }

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCRIP SUMMARY (Page 3)
    //  [MODIFIED] Uses ScripSummaryDto (tag U) — direct render
    //  [REMOVED] SSHeaderTypeModel processing
    //  ⚠️ Zero calculation — direct DTO render
    // ═══════════════════════════════════════════════════════════════════════
    private void addScripSummaryOnPage3(Document document) {
        Table titleTable = new Table(1);
        titleTable.setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER).setMarginTop(2).setMarginBottom(1);
        titleTable.addCell(new Cell().add(new Paragraph("Scrip-Summary").setFont(calibriBold).setFontSize(FONT_SIZE_NORMAL).setMargin(0)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));
        document.add(titleTable);

        float[] cols = {2.5f, 0.5f, 0.8f, 1f, 1f, 1f, 1f, 0.9f, 1.2f};
        Table headerTable = new Table(UnitValue.createPercentArray(cols));
        headerTable.setWidth(UnitValue.createPercentValue(100)).setFontSize(FONT_SIZE_TINY).setBorder(Border.NO_BORDER).setMarginTop(2).setMarginBottom(2);
        headerTable.addHeaderCell(createHeaderCellGreen("Security Description"));
        headerTable.addHeaderCell(createHeaderCellGreen("B/S"));
        headerTable.addHeaderCell(createHeaderCellGreen("Quantity"));
        headerTable.addHeaderCell(createHeaderCellGreen("Gross Rate Per\nSecurity(Rs)"));
        headerTable.addHeaderCell(createHeaderCellGreen("Gross Total(Rs)"));
        headerTable.addHeaderCell(createHeaderCellGreen("Gross BrokeragePer\nSecurity(Rs)"));
        headerTable.addHeaderCell(createHeaderCellGreen("Brokerage(Total)(\nRs)"));
        headerTable.addHeaderCell(createHeaderCellGreen("Net Rate(Rs)"));
        headerTable.addHeaderCell(createHeaderCellGreen("Net Total Amount(Rs)"));
        document.add(headerTable);

        // [MODIFIED] Direct render from ScripSummaryDto (U-record)
        for (ScripSummaryDto u : uRecords) {
            Table dataTable = new Table(UnitValue.createPercentArray(cols));
            dataTable.setWidth(UnitValue.createPercentValue(100)).setFontSize(FONT_SIZE_TINY).setBorder(Border.NO_BORDER).setMarginTop(0).setMarginBottom(2);

            String buySell = safe(u.getBuySell()).toUpperCase();
            if ("BUY".equals(buySell)) buySell = "B";
            if ("SELL".equals(buySell)) buySell = "S";

            dataTable.addCell(createCell(safe(u.getSecurityDescription()), TextAlignment.LEFT, false));
            dataTable.addCell(createCell(buySell, TextAlignment.CENTER, false));
            dataTable.addCell(createCell(safe(u.getQuantity()), TextAlignment.RIGHT, false));
            dataTable.addCell(createCell(safe(u.getGrossRatePerSecurity()), TextAlignment.RIGHT, false));
            dataTable.addCell(createCell(safe(u.getGrossTotalRs()), TextAlignment.RIGHT, false));
            dataTable.addCell(createCell(safe(u.getGrossBrokeragePerSecurity()), TextAlignment.RIGHT, false));
            dataTable.addCell(createCell(safe(u.getBrokerageTotal()), TextAlignment.RIGHT, false));
            dataTable.addCell(createCell(safe(u.getNetRateRs()), TextAlignment.RIGHT, false));
            dataTable.addCell(createCell(safe(u.getNetTotalAmountRs()), TextAlignment.RIGHT, false));
            document.add(dataTable);
        }

        document.add(new Paragraph("").setMarginBottom(4));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STT STATEMENT — CASH TRANSACTIONS
    //  [MODIFIED] Uses SecurityTransactionDto (tag C) — direct render
    //  [MODIFIED] Uses SecurityTransactionTotalDto (tag R) — direct total
    //  [REMOVED] SCapitalHeaderTypeModel + manual STT percentage calculations
    //  ⚠️ Zero calculation — all values from DTO
    // ═══════════════════════════════════════════════════════════════════════
    private void addSTTStatementCashTransactions(Document document) {

        // ✅ FIX: Skip entirely if no C records and no R record present
        if (contracts.isEmpty() && roundedTotal == null) {
            logger.warn("No STT Cash (C/R records) — skipping table");
            return;
        }

        // Title box
        Table titleTable = new Table(new float[]{5f, 3f});
        titleTable.setWidth(UnitValue.createPercentValue(100))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setMarginTop(2).setMarginBottom(2);
        titleTable.addCell(new Cell()
                .add(new Paragraph("Statement Of Securities Transaction Tax(Cash Transactions)")
                        .setFont(calibriBold).setFontSize(9f).setMargin(0))
                .setPadding(4).setBorderRight(Border.NO_BORDER));
        titleTable.addCell(new Cell()
                .add(new Paragraph("For equity share in a company or a unit of an equity oriented fund")
                        .setFont(calibriBold).setFontSize(8f)
                        .setTextAlignment(TextAlignment.RIGHT).setMargin(0))
                .setPadding(4).setBorderLeft(Border.NO_BORDER));
        document.add(titleTable);

        float[] cols = {0.5f, 2.5f, 0.8f, 0.7f, 0.8f, 1f, 0.7f, 0.7f, 0.8f, 1f,
                0.7f, 0.7f, 0.8f, 1f, 0.7f, 1f};
        Table table = new Table(UnitValue.createPercentArray(cols));
        table.setWidth(UnitValue.createPercentValue(100)).setFontSize(7f);

        // ROW 1: Group headers
        table.addHeaderCell(new Cell(2, 1).add(p("Sl.No"))
                .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell(2, 1).add(p("Security"))
                .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell(2, 1).add(p("Segment"))
                .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell(1, 4)
                .add(p("Transaction settled by Delivery Purchase"))
                .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell(1, 4)
                .add(p("Transaction settled by Delivery Sale"))
                .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell(1, 4)
                .add(p("Transaction settled other than By Delivery"))
                .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.CENTER));
        table.addHeaderCell(new Cell(2, 1).add(p("Total STT (Rs.)"))
                .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.CENTER));

        // ROW 2: Sub-headers
        for (String h : new String[]{"Quantity", "Price", "Value", "STT",
                "Quantity", "Price", "Value", "STT",
                "Quantity", "Price", "Value", "STT"})
            table.addHeaderCell(new Cell().add(p(h))
                    .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.CENTER));

        // Data rows from C-records (may be empty — that's fine now, guard is above)
        for (SecurityTransactionDto c : contracts) {
            table.addCell(dataCell(safe(c.getSlNo())));
            table.addCell(dataCell(safe(c.getSecurity())));
            table.addCell(dataCell(safe(c.getSegment())));
            table.addCell(dataCell(safe(c.getQuantity1())));
            table.addCell(dataCell(safe(c.getPrice1())));
            table.addCell(dataCell(safe(c.getValue1())));
            table.addCell(dataCell(safe(c.getStt1())));
            table.addCell(dataCell(safe(c.getQuantity2())));
            table.addCell(dataCell(safe(c.getPrice2())));
            table.addCell(dataCell(safe(c.getValue2())));
            table.addCell(dataCell(safe(c.getStt2())));
            table.addCell(dataCell(safe(c.getQuantity3())));
            table.addCell(dataCell(safe(c.getPrice3())));
            table.addCell(dataCell(safe(c.getValue3())));
            table.addCell(dataCell(safe(c.getStt3())));
            table.addCell(dataCell(safe(c.getTotalSttRs())));
        }

        // Total row from R-record
        String totalRounded = (roundedTotal != null)
                ? safe(roundedTotal.getTotalRoundedToNearestRupee()) : "";
        table.addCell(new Cell(1, 15).add(p("Total(Rounded to Nearest Rupee)"))
                .setBorder(getStandardBorder()).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(dataCell(totalRounded));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STT STATEMENT — DERIVATIVES
    //  [MODIFIED] Section header from CashSegmentTotalDto (tag A)
    //  [MODIFIED] Rows from CashSegmentDto (tag L) — direct render
    //  [REMOVED] FOHeaderTypeModel manual STT percentage calculations (0.0001%, 0.001%)
    //  ⚠️ Zero calculation — all values from DTO
    // ═══════════════════════════════════════════════════════════════════════

    private void addSTTStatementDerivatives(Document document) {
        if (lots.isEmpty()) {
            return;
        }

        float[] cols = {0.5f, 3f, 1f, 1f, 0.8f, 1f, 0.8f, 1f};

        for (int i = 0; i < amounts.size(); i++) {
            CashSegmentTotalDto a = amounts.get(i);

            // setKeepWithNext(true) keeps heading glued to the table below it
            document.add(new Paragraph("Name Of Exchange & Segment : "
                    + safe(a.getNameOfExchange()) + " | " + safe(a.getSegment()))
                    .setFont(calibriNormal).setFontSize(9f)
                    .setMarginTop(2).setMarginBottom(1)
                    .setKeepWithNext(true));

            Table table = new Table(UnitValue.createPercentArray(cols));
            table.setWidth(UnitValue.createPercentValue(100)).setFontSize(7f)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f));

            // ROW 1: Main headers
            table.addHeaderCell(new Cell(1, 3)
                    .add(new Paragraph("").setMargin(0))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));
            table.addHeaderCell(new Cell(1, 2)
                    .add(new Paragraph("Value of Transactions Futures")
                            .setFont(calibriNormal).setFontSize(7f)
                            .setTextAlignment(TextAlignment.CENTER).setMargin(0))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            table.addHeaderCell(new Cell(1, 2)
                    .add(new Paragraph("Value of Transactions Options")
                            .setFont(calibriNormal).setFontSize(7f)
                            .setTextAlignment(TextAlignment.CENTER).setMargin(0))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            table.addHeaderCell(new Cell(1, 1)
                    .add(new Paragraph("").setMargin(0))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2));

            // ROW 2: Sub-headers
            table.addHeaderCell(createSTTHeaderCell("Sl.No"));
            table.addHeaderCell(createSTTHeaderCell("Security"));
            table.addHeaderCell(createSTTHeaderCell("Expiry Date"));
            table.addHeaderCell(createSTTHeaderCell("Sale"));
            table.addHeaderCell(createSTTHeaderCell("STT"));
            table.addHeaderCell(createSTTHeaderCell("Sale"));
            table.addHeaderCell(createSTTHeaderCell("STT"));
            table.addHeaderCell(createSTTHeaderCell("Total STT(Rs.)"));

            final int exchangeIndex = i + 1;
            for (CashSegmentDto l : lots) {
                int slNo;
                try { slNo = Integer.parseInt(safe(l.getSlNo()).trim()); }
                catch (NumberFormatException e) { slNo = -1; }

                if (slNo == exchangeIndex) {
                    table.addCell(createSTTDataCell(safe(l.getSlNo()),       TextAlignment.CENTER));
                    table.addCell(createSTTDataCell(safe(l.getSecurity()),   TextAlignment.LEFT));
                    table.addCell(createSTTDataCell(safe(l.getExpiryDate()), TextAlignment.CENTER));
                    table.addCell(createSTTDataCell(safe(l.getSale1()),      TextAlignment.RIGHT));
                    table.addCell(createSTTDataCell(safe(l.getStt1()),       TextAlignment.RIGHT));
                    table.addCell(createSTTDataCell(safe(l.getSale2()),      TextAlignment.RIGHT));
                    table.addCell(createSTTDataCell(safe(l.getStt2()),       TextAlignment.RIGHT));
                    table.addCell(createSTTDataCell(safe(l.getTotalSttRs()), TextAlignment.RIGHT));
                }
            }

            table.addCell(new Cell(1, 7)
                    .add(new Paragraph("Total(Rounded to nearest Rupee)")
                            .setFont(calibriNormal).setFontSize(7f)
                            .setTextAlignment(TextAlignment.RIGHT).setMargin(0))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE));
            table.addCell(createSTTDataCell(safe(a.getTotalRoundedToNearestRupee()), TextAlignment.RIGHT));

            document.add(table);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DAILY MARGIN STATEMENT (Page 4)
    //  [MODIFIED] Uses DailyMarginDto (tag G) — direct render
    //  [MODIFIED] Uses DailyMarginTotalDto (tag J) — direct total row
    //  [REMOVED] MHeaderTypeModel processing
    //  [REMOVED] normalizeMarginData() call
    //  [REMOVED] summaryRow filter by segment="Summary of all Exchanges"
    //  Q5 ANSWER: DailyMarginDto.getSeq() = segment identifier (NSEFO, NSECDS etc.)
    //  ⚠️ Zero calculation — direct DTO render
    // ═══════════════════════════════════════════════════════════════════════
    private void addMarginStatement(Document document) {
        if (generals.isEmpty()) {
            //            System.out.println("       → No daily margin (G records) found");
            return;
        }

        document.add(new Paragraph("Daily Margin Statement")
                .setFont(calibriBold).setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));

        // Build exchange list from G-record seq fields for the exchange label
        Set<String> exchangeSet = generals.stream()
                .map(g -> safe(g.getSeq())).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String exchangeLabel = exchangeSet.isEmpty() ? "NSE,BSE,MCX'SX" : String.join(", ", exchangeSet);

        document.add(new Paragraph("Exchange : " + exchangeLabel)
                .setFont(calibriNormal).setFontSize(7f)
                .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(4));

        float[] cols = {0.5f, 0.7f, 0.6f, 0.7f, 0.9f, 0.7f, 0.7f, 0.9f, 0.8f, 0.9f, 0.7f, 0.8f, 0.8f, 0.7f, 0.9f};
        Table table = new Table(UnitValue.createPercentArray(cols));
        table.setWidth(UnitValue.createPercentValue(100)).setFontSize(6.5f);

        // Header rows (unchanged structure)
        table.addHeaderCell(new Cell(2, 1).add(new Paragraph("Seg").setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2f).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(2, 1).add(new Paragraph("Trade Day").setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2f).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(1, 6).add(new Paragraph("Margins available till T day").setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2f).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(1, 4).add(new Paragraph("Margin/ Consolidated Crystallized Obligation / MTM required by Exchange/CC end of T\n& T+1 day respectively").setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2f).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(2, 1).add(new Paragraph("Excess / Shortfall\nw.r.t. Requirement\nby Exchange / CC").setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2f).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(2, 1).add(new Paragraph("Additional Margin\nrequired by member\nas per RMS").setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2f).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        table.addHeaderCell(new Cell(2, 1).add(new Paragraph("MarginStatus\n(Balance with\nMember\n/Due\nfromclient)").setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2f).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));

        table.addHeaderCell(createMarginHeaderCell("Funds"));
        table.addHeaderCell(createMarginHeaderCell("Value of Securities\n(after haircut)"));
        table.addHeaderCell(createMarginHeaderCell("Value of margin\npledge Securities\n(after haircut)"));
        table.addHeaderCell(createMarginHeaderCell("Bank Guarantees /\nFDR"));
        table.addHeaderCell(createMarginHeaderCell("Any other approved\nform of Margins*"));
        table.addHeaderCell(createMarginHeaderCell("Total Margins\nAvailable (E)"));
        table.addHeaderCell(createMarginHeaderCell("Total upfront\nMargin"));
        table.addHeaderCell(createMarginHeaderCell("Consolidated\nCrystallized\nObligation / MTM"));
        table.addHeaderCell(createMarginHeaderCell("Delivery Margin"));
        table.addHeaderCell(createMarginHeaderCell("Total Requirement"));

        // [MODIFIED] Data rows from DailyMarginDto (G-records) — direct render
        // Q5 ANSWER: seq = segment identifier (NSEFO, NSECDS, BSEFO, etc.)
        for (DailyMarginDto g : generals) {
            table.addCell(createMarginDataCell(safe(g.getSeq()), TextAlignment.LEFT));
            table.addCell(createMarginDataCell(safe(g.getTradeDay()), TextAlignment.CENTER));
            table.addCell(createMarginDataCell(safe(g.getFunds()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getValueOfSecurities()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getValueOfMarginPledgeSecurities()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getBankGuaranteesFdr()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getAnyOtherApprovedFormOfMargins()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getTotalMarginsAvailable()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getTotalUpfrontMargin()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getConsolidatedCrystallised()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getDeliveryMargin()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getTotalRequirement()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getExcessShortfall()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getAdditionalMarginsRequiredByMemberAsPerRMS()), TextAlignment.RIGHT));
            table.addCell(createMarginDataCell(safe(g.getMarginStatusBalanceWithMember()), TextAlignment.RIGHT));
        }

        // [MODIFIED] Summary row from DailyMarginTotalDto (J-record)
        // [REMOVED] Filter by segment="Summary of all Exchanges" logic
        Cell summaryLabelCell = new Cell(1, 2).add(new Paragraph("Summary of all Exchanges").setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.LEFT).setMultipliedLeading(1.1f)).setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(4f).setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addCell(summaryLabelCell);

        if (journal != null) {
            // [MODIFIED] All 13 summary columns from DailyMarginTotalDto directly
            table.addCell(createMarginSummaryCell(safe(journal.getFunds())));
            table.addCell(createMarginSummaryCell(safe(journal.getValueOfSecurities())));
            table.addCell(createMarginSummaryCell(safe(journal.getValueOfMarginPledgeSecurities())));
            table.addCell(createMarginSummaryCell(safe(journal.getBankGuaranteesFdr())));
            table.addCell(createMarginSummaryCell(safe(journal.getAnyOtherApprovedFormOfMargins())));
            table.addCell(createMarginSummaryCell(safe(journal.getTotalMarginsAvailable())));
            table.addCell(createMarginSummaryCell(safe(journal.getTotalUpfrontMargin())));
            table.addCell(createMarginSummaryCell(safe(journal.getConsolidatedCrystallised())));
            table.addCell(createMarginSummaryCell(safe(journal.getDeliveryMargin())));
            table.addCell(createMarginSummaryCell(safe(journal.getTotalRequirement())));
            table.addCell(createMarginSummaryCell(safe(journal.getExcessShortfall())));
            table.addCell(createMarginSummaryCell(safe(journal.getAdditionalMarginsRequiredByMemberAsPerRMS())));
            table.addCell(createMarginSummaryCell(safe(journal.getMarginStatusBalanceWithMember())));
        } else {
            // Q6: show empty if J-record not present
            //            System.out.println("       ⚠ DailyMarginTotalDto (J) is null — showing empty");
            for (int i = 0; i < 13; i++) table.addCell(createMarginSummaryCell(""));
        }

        document.add(table);
        addMarginStatementNotes(document);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MARGIN STATEMENT NOTES
    //  [MODIFIED] MTM per segment values from DailyMarginDto.getSeq() (G-records)
    //  [REMOVED] old segment filter logic (NSEFO, BSEFO string matching)
    //  Q5 ANSWER: seq field identifies the segment (NSEFO, NSECDS, etc.)
    // ═══════════════════════════════════════════════════════════════════════
    private void addMarginStatementNotes(Document document) {
        document.add(new Paragraph()
                .add(new Text("* approved form as may be specified by the Exchange/Clearing Corporation /NSCCL/MCX-SXCCL from time to time. ").setFontSize(FONT_SIZE_TINY))
                .add(new Text("# Balance margin available for the day, pending bills will be adjusted with the available balance.").setFontSize(FONT_SIZE_TINY))
                .setMarginTop(25).setMarginBottom(2));

        document.add(new Paragraph("1) Settlements not due : 2) For margin reporting, additional T&C signed holdings is considered.")
                .setFontSize(FONT_SIZE_TINY).setMarginTop(2).setMarginBottom(2));
        document.add(new Paragraph("For collateral accounting in derivative segments, only the pledge eligible shares as per NSE will be considered. Buying Power in FLIP is updated based on the scrip margin specified by GEOJIT INVESTMENTS LTD.")
                .setFontSize(FONT_SIZE_TINY).setMarginTop(2).setMarginBottom(2));

        // [MODIFIED] Trade date from footerDto (F-record)
        String tradeDate = (footerDto != null) ? safe(footerDto.getDate()) : "";

        document.add(new Paragraph("3) The fund balance is arrived at without considering the funds in Margin Trading (MTF) and pending settlements. 4) Provisional Penalty for the trade date (" + tradeDate + ") is 0 /-")
                .setFontSize(FONT_SIZE_TINY).setMarginTop(2).setMarginBottom(2));

        // [MODIFIED] Build MTM note from G-records using seq as segment identifier
        // Q5: seq = segment name like NSEFO, NSECDS, BSEFO, etc.
        Map<String, String> mtmMap = new LinkedHashMap<>();
        for (DailyMarginDto g : generals) {
            String seg = safe(g.getSeq()).toUpperCase();
            if (!seg.isEmpty()) {
                mtmMap.put(seg, safe(g.getConsolidatedCrystallised()));
            }
        }

        String nseFO = mtmMap.getOrDefault("NSEFO", "0.00");
        String nseCDS = mtmMap.getOrDefault("NSECDS", "0.00");
        String mcxCDS = mtmMap.getOrDefault("MCXCDS", mtmMap.getOrDefault("MCX'SX", "0.00"));
        String bseFO = mtmMap.getOrDefault("BSEFO", "0.00");
        String bseCDS = mtmMap.getOrDefault("BSECDS", "0.00");
        String mcx = mtmMap.getOrDefault("MCX", "0.00");
        String ncdex = mtmMap.getOrDefault("NCDEX", "0.00");
        String icex = mtmMap.getOrDefault("ICEX", "0.00");

        document.add(new Paragraph("5) MTM/Premium for the trade date (" + tradeDate + ") - " +
                "NSE FO = " + nseFO + " /-, NSE CDS = " + nseCDS + " /-, MCX CDS = " + mcxCDS + " /-, " +
                "BSE FO = " + bseFO + " /-, BSE CDS = " + bseCDS + " /-, MCX = " + mcx + " /-, " +
                "NCDEX = " + ncdex + " /-, ICEX = " + icex + " /- . " +
                "6) All the figures are in Rupee. \"-Ve indicates debit balance, +Ve indicates credit balance\"")
                .setFontSize(FONT_SIZE_TINY).setMarginTop(2).setMarginBottom(2));

        document.add(new Paragraph("6)For the purchase of Shares of BSE Ltd/CDSL., you are requested to comply with the prescribed SECC regulation 19 and 20 and related circulars.")
                .setFontSize(FONT_SIZE_TINY).setMarginTop(2).setMarginBottom(2));
        document.add(new Paragraph(" For more details,\nhttp://www.geojit.com/equity-products/instructions")
                .setFontSize(FONT_SIZE_TINY).setMarginTop(2).setMarginBottom(2));
        document.add(new Paragraph("7)In case of trades/positions in commodity Exchanges, new margin statement will be issued separately.")
                .setFontSize(FONT_SIZE_TINY).setMarginTop(2).setMarginBottom(2));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MARGIN PLEDGE SECURITIES (Page 4–5)
    //  [MODIFIED] Uses MarginPledgeDto (tag K) — direct render
    //  [MODIFIED] Uses MarginPledgeTotalDto (tag Q) — direct total row
    //  [REMOVED] PHeaderTypeModel processing
    //  [REMOVED] "isTotal" string check on security name
    //  ⚠️ Zero calculation — all values from DTO
    // ═══════════════════════════════════════════════════════════════════════
    private void addMarginPledgeSecurities(Document document) {
        if (securities.isEmpty()) {
            //            System.out.println("       → No pledge securities (K records) found");
            return;
        }

        document.add(new Paragraph("Margin Pledge Securities Details")
                .setFont(calibriBold).setFontSize(12f).setFontColor(ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(4).setMarginBottom(8));

        float[] cols = {3.5f, 1f, 1.2f, 1.2f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(cols));
        table.setWidth(UnitValue.createPercentValue(49));
        table.setHorizontalAlignment(HorizontalAlignment.CENTER).setFontSize(7f);

        // Spacer header for logo clearance on continuation pages
        // Spacer header for logo clearance on continuation pages
        Cell logoSpacerHeader = new Cell(1, 5).add(new Paragraph("")).setMinHeight(40f).setBorder(Border.NO_BORDER);
        table.addHeaderCell(logoSpacerHeader);
        table.setSkipFirstHeader(true);

        // Column headers (as regular rows, not header — so they don't repeat)
        table.addCell(createPledgeHeaderCell("Security"));
        table.addCell(createPledgeHeaderCell("Qty"));
        table.addCell(createPledgeHeaderCell("Total Value"));
        table.addCell(createPledgeHeaderCell("HairCut Value"));
        table.addCell(createPledgeHeaderCell("Balance Amount"));

        // [MODIFIED] Data rows from MarginPledgeDto (K-records) — direct render
        for (MarginPledgeDto k : securities) {
            table.addCell(createPledgeDataCell(safe(k.getSecurity()), TextAlignment.LEFT, false));
            table.addCell(createPledgeDataCell(safe(k.getQty()), TextAlignment.RIGHT, false));
            table.addCell(createPledgeDataCell(safe(k.getTotalValue()), TextAlignment.RIGHT, false));
            table.addCell(createPledgeDataCell(safe(k.getHairCutValue()), TextAlignment.RIGHT, false));
            table.addCell(createPledgeDataCell(safe(k.getBalanceAmount()), TextAlignment.RIGHT, false));
        }

        // [MODIFIED] Total row from MarginPledgeTotalDto (Q-record) — direct
        // [REMOVED] PHeaderTypeModel "isTotal" filter + manual total accumulation
        if (pledgeQuantity != null) {
            table.addCell(new Cell()
                    .add(new Paragraph("Total").setFontSize(7f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                    .setPadding(6f).setVerticalAlignment(VerticalAlignment.MIDDLE));
            table.addCell(new Cell()
                    .add(new Paragraph("").setFontSize(7f))
                    .setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                    .setPadding(6f).setVerticalAlignment(VerticalAlignment.MIDDLE));
            table.addCell(createPledgeDataCell(safe(pledgeQuantity.getTotalValue()), TextAlignment.RIGHT, false));
            table.addCell(createPledgeDataCell(safe(pledgeQuantity.getHairCutValue()), TextAlignment.RIGHT, false));
            table.addCell(createPledgeDataCell(safe(pledgeQuantity.getBalanceAmount()), TextAlignment.RIGHT, false));
        } else {
            // Q6: show empty if Q-record not present
            //            System.out.println("       ⚠ MarginPledgeTotalDto (Q) is null — showing empty");
            table.addCell(new Cell()
                    .add(new Paragraph("Total").setFontSize(7f).setFont(calibriBold).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE).setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                    .setPadding(6f).setVerticalAlignment(VerticalAlignment.MIDDLE));
            for (int i = 0; i < 4; i++)
                table.addCell(createPledgeDataCell("", TextAlignment.RIGHT, false));
        }

        document.add(table);
    }
    // ═══════════════════════════════════════════════════════════════════════
    //  HELPER METHODS — CELL BUILDERS (layout unchanged from original)
    // ═══════════════════════════════════════════════════════════════════════

    private Cell createFirstRowLabelCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(calibriNormal).setFontSize(7f).setMargin(0))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createFirstRowValueCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(calibriNormal).setFontSize(7f).setMargin(0))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell labelCellNoHorizontal(String text, boolean isFirst, boolean isLast) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(calibriNormal).setFontSize(7f).setMargin(0))
                .setPadding(2).setVerticalAlignment(VerticalAlignment.TOP);
        cell.setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1f));
        cell.setBorderRight(Border.NO_BORDER);
        cell.setBorderTop(isFirst ? new SolidBorder(ColorConstants.BLACK, 1f) : Border.NO_BORDER);
        cell.setBorderBottom(isLast ? new SolidBorder(ColorConstants.BLACK, 1f) : Border.NO_BORDER);
        return cell;
    }

    private Cell valueCellSpanning(String text, int colspan, boolean isLast, boolean bold) {
        Paragraph p = new Paragraph(text).setFontSize(7f).setMargin(0);
        if (bold) p.setFont(calibriBold);
        else p.setFont(calibriNormal);
        Cell cell = new Cell(1, colspan).add(p).setPadding(2).setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1f));
        cell.setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f));
        cell.setBorderTop(Border.NO_BORDER);
        cell.setBorderBottom(isLast ? new SolidBorder(ColorConstants.BLACK, 1f) : Border.NO_BORDER);
        return cell;
    }

    private Cell exchangeHeaderNoGrey(String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(calibriNormal).setFontSize(6.5f).setTextAlignment(TextAlignment.CENTER).setMargin(0))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell exchangeData(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text).setFont(calibriNormal).setFontSize(7f).setTextAlignment(alignment).setMargin(0))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createEquityHeaderCellWithPadding(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(FONT_SIZE_TINY).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createEquityDataCell(String content, TextAlignment alignment) {
        return createEquityDataCell(content, alignment, false);
    }

    private Cell createEquityDataCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content).setFontSize(FONT_SIZE_TINY).setTextAlignment(alignment);
        if (bold) p.setFont(calibriBold);
        else p.setFont(calibriNormal);
        return new Cell().add(p).setBorder(getStandardBorder()).setPadding(0.75f).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createDerivativeHeaderCellNormal(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(FONT_SIZE_SMALL).setFont(calibriNormal).setFontColor(ColorConstants.BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(getStandardBorder()).setPadding(0.75f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createDerivativeDataCell(String content, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(content).setFontSize(FONT_SIZE_TINY).setFont(calibriNormal).setTextAlignment(alignment))
                .setBorder(getStandardBorder()).setPadding(0.75f).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createHeaderCellGreen(String content) {
        return new Cell()
                .add(new Paragraph(content).setFontSize(FONT_SIZE_TINY).setFont(calibriBold).setFontColor(ColorConstants.BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(getStandardBorder()).setPadding(0.75f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content).setFontSize(FONT_SIZE_TINY).setTextAlignment(alignment);
        if (bold) p.setFont(calibriBold);
        else p.setFont(calibriNormal);
        return new Cell().add(p).setBorder(getStandardBorder()).setPadding(0.75f).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

//    private Cell createSimpleHeaderCell(String text) {
//        return new Cell()
//                .add(new Paragraph(text).setFontSize(FONT_SIZE_TINY).setFont(calibriNormal).setFontColor(ColorConstants.BLACK).setTextAlignment(TextAlignment.CENTER))
//                .setBackgroundColor(ColorConstants.WHITE)
//                .setBorder(getStandardBorder()).setPadding(1)
//                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
//    }

//    private Cell createSimpleDataCell(String content, TextAlignment alignment) {
//        return new Cell()
//                .add(new Paragraph(content).setFontSize(FONT_SIZE_TINY).setFont(calibriNormal).setTextAlignment(alignment))
//                .setBorder(getStandardBorder()).setPadding(1).setVerticalAlignment(VerticalAlignment.MIDDLE);
//    }
private Cell createSimpleHeaderCell(String text) {
    Paragraph p = new Paragraph(text)
            .setFontSize(FONT_SIZE_TINY)
            .setFont(calibriNormal)
            .setFontColor(ColorConstants.BLACK)
            .setTextAlignment(TextAlignment.CENTER);
    // Force text to wrap inside column — works on all iText7 versions
    p.setProperty(Property.OVERFLOW_X, OverflowPropertyValue.FIT);

    return new Cell()
            .add(p)
            .setBackgroundColor(ColorConstants.WHITE)
            .setBorder(getStandardBorder())
            .setPadding(1)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
}

    private Cell createSimpleDataCell(String content, TextAlignment alignment) {
        Paragraph p = new Paragraph(content)
                .setFontSize(FONT_SIZE_TINY)
                .setFont(calibriNormal)
                .setTextAlignment(alignment);
        // Force text to wrap inside column — works on all iText7 versions
        p.setProperty(Property.OVERFLOW_X, OverflowPropertyValue.FIT);

        return new Cell()
                .add(p)
                .setBorder(getStandardBorder())
                .setPadding(1)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }


    private Cell createNetObligationSubHeader(String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(TextAlignment.CENTER).setMargin(0).setMultipliedLeading(1.02f))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createNetObligationDataCellPro(String content, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(content).setFont(calibriNormal).setFontSize(7.2f).setTextAlignment(alignment).setMargin(0).setMultipliedLeading(1.02f))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createMarginHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.CENTER).setMultipliedLeading(1.1f))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createMarginDataCell(String content, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(content).setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(alignment).setMultipliedLeading(1.1f))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(4f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createMarginSummaryCell(String content) {
        return new Cell()
                .add(new Paragraph(content).setFontSize(6.5f).setFont(calibriNormal).setTextAlignment(TextAlignment.RIGHT).setMultipliedLeading(1.1f))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(4f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createSTTHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(calibriNormal).setFontSize(7f).setTextAlignment(TextAlignment.CENTER).setMargin(0))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createSTTDataCell(String content, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(content).setFont(calibriNormal).setFontSize(7f).setTextAlignment(alignment).setMargin(0))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createPledgeHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(7f).setFont(calibriBold).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(3f)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createPledgeDataCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content).setFontSize(7f).setTextAlignment(alignment);
        if (bold) p.setFont(calibriBold);
        else p.setFont(calibriNormal);
        return new Cell().add(p).setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(6f)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UNIVERSAL TIGHT CELL/PARAGRAPH HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private Paragraph p(String text) {
        return new Paragraph(text).setFont(calibriNormal).setFontSize(FONT_SIZE_SMALL).setMultipliedLeading(1.1f);
    }

    private Paragraph pb(String text) {
        return new Paragraph(text).setFont(calibriBold).setFontSize(FONT_SIZE_SMALL).setMultipliedLeading(1.1f);
    }

    private Cell dataCell(String text) {
        return new Cell().add(p(text)).setPadding(2).setBorder(new SolidBorder(ColorConstants.BLACK, 1f));
    }

    private Cell headerCell(String text) {
        return new Cell().add(pb(text)).setTextAlignment(TextAlignment.CENTER).setPadding(2).setBorder(new SolidBorder(ColorConstants.BLACK, 1f));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════
    private SolidBorder getStandardBorder() {
        return new SolidBorder(ColorConstants.BLACK, 0.8f);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replaceAll(",", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String getBrokerCode(String exchange) {
        if (exchange != null && exchange.toUpperCase().contains("NSE")) return "13372";
        if (exchange != null && exchange.toUpperCase().contains("BSE")) return "0328";
        return "0328";
    }

    private void addPageBreakIfNeeded(Document document, float requiredSpace) {
        try {
            PdfDocument pdfDoc = document.getPdfDocument();
            PdfPage currentPage = pdfDoc.getPage(pdfDoc.getNumberOfPages());
            Rectangle pageSize = currentPage.getPageSize();
            float cursorY = document.getRenderer().getCurrentArea().getBBox().getY();
            float availableSpace = cursorY - document.getBottomMargin();
            if (availableSpace < requiredSpace) {
                //                System.out.println("       → Adding page break (required: " + requiredSpace + "pt)");
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }
        } catch (Exception e) {
            logger.error("Page break check failed: " + e.getMessage());
        }
    }

    private String generatePdfPassword() {
        String pan = safe(header.getPanOfClient()).toUpperCase().replaceAll("[^A-Z]", "");
        String date = safe(header.getTradeDate()).replaceAll("[^0-9]", ""); // 03.07.2026 → 03072026

        String panPart = pan.length() >= 4 ? pan.substring(0, 4) : pan;   // ABCD
        String datePart = date.length() >= 4 ? date.substring(0, 4) : date; // 0307

        String pwd = panPart + datePart;
        //        System.out.println("       → PDF Password : " + pwd
        //                + "  (PAN4=" + panPart + ", DDMM=" + datePart + ")");
        return pwd;
    }
    // ─────────────────────────────────────────────────
// SECRETS MANAGER SE VALUE LO
// ─────────────────────────────────────────────────
    private String getSecretValue(String secretName) {
        software.amazon.awssdk.services.secretsmanager.SecretsManagerClient client =
                software.amazon.awssdk.services.secretsmanager.SecretsManagerClient.builder()
                        .region(software.amazon.awssdk.regions.Region.AP_SOUTH_1)
                        .httpClient(
                                software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.builder()
                                        .build()
                        )
                        .build();
        try {
            return client.getSecretValue(r -> r.secretId(secretName)).secretString();
        } finally {
            client.close();
        }
    }

    // ─────────────────────────────────────────────────
// PDF DIGITAL SIGNING
// Secrets Manager se keys lekar iText se sign karta hai
    private File signPdf(File unsignedPdf) throws Exception {

        // Step 1: Secrets Manager se lo
//        String certificatePem = getSecretValue("prod/angelone-certificate");
//        String privateKeyPem  = getSecretValue("prod/angelone-private-key");
        // NAYI LINES — YE LAGAO
        String secretJson = getSecretValue("my-test-cert");
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, String> secretMap = mapper.readValue(
                secretJson,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}
        );
        String certificatePem = secretMap.get("certificate");
        String privateKeyPem  = secretMap.get("private_key");


        java.security.cert.X509Certificate certificate;
        try (java.io.InputStream certStream = new ByteArrayInputStream(
                certificatePem.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            certificate = (java.security.cert.X509Certificate)
                    java.security.cert.CertificateFactory.getInstance("X.509")
                            .generateCertificate(certStream);
        }
//        System.out.println("Certificate loaded: " + certificate.getSubjectDN());

        // Step 3: Private Key load karo
        // ✅ NAYA — LAGAO (BouncyCastle PEMParser use karta hai)
        java.security.PrivateKey privateKey;
        try (org.bouncycastle.openssl.PEMParser pemParser =
                     new org.bouncycastle.openssl.PEMParser(
                             new java.io.StringReader(privateKeyPem))) {

            Object parsed = pemParser.readObject();
            org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter converter =
                    new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                            .setProvider("BC");

            if (parsed instanceof org.bouncycastle.openssl.PEMKeyPair) {
                privateKey = converter.getKeyPair(
                        (org.bouncycastle.openssl.PEMKeyPair) parsed).getPrivate();
            } else if (parsed instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                privateKey = converter.getPrivateKey(
                        (org.bouncycastle.asn1.pkcs.PrivateKeyInfo) parsed);
            } else {
                throw new Exception("Unknown key format: "
                        + (parsed == null ? "null" : parsed.getClass().getName()));
            }
        }
//        System.out.println("Private key loaded ✅");

        // Step 4: Signed PDF output file
        File signedPdf = new File(
                unsignedPdf.getAbsolutePath().replace(".pdf", "_signed.pdf"));

        // Step 5: iText se sign karo
        PdfReader reader = new PdfReader(
                unsignedPdf.getAbsolutePath(),
                new com.itextpdf.kernel.pdf.ReaderProperties()
                        .setPassword("GeojitAdmin@2024".getBytes())
        );
        PdfSigner signer = new PdfSigner(
                reader,
                new FileOutputStream(signedPdf),
                new StampingProperties());

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance.setReason("Contract Note authenticated by Geojit Investments Ltd");
        appearance.setLocation("Kochi, India");
        appearance.setContact("compliance@geojit.com");


        String subjectDN = certificate.getSubjectDN().getName();
        String cn = subjectDN; // fallback
        for (String part : subjectDN.split(",")) {
            if (part.trim().startsWith("CN=")) {
                cn = part.trim().substring(3);
                break;
            }
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        String signDate = sdf.format(new java.util.Date());

// Visible signature box
        appearance.setRenderingMode(
                PdfSignatureAppearance.RenderingMode.DESCRIPTION
        );
        appearance.setLayer2Text(
                "Digitally Signed by: " + cn + "\n" +
                        "Reason: Contract Note - " + safe(header.getContractNoteNo()) + "\n" +
                        "Date: " + signDate + "\n" +
                        "Location: " + safe(footerDto != null ? footerDto.getPlace() : "Kochi, India")
        );

        int lastPage = signer.getDocument().getNumberOfPages();
        appearance.setPageNumber(lastPage);
        appearance.setPageRect(new Rectangle(36, 10, 250, 60));
        signer.setFieldName("GeojitDigitalSignature");

        IExternalSignature signature = new PrivateKeySignature(
                privateKey,
                DigestAlgorithms.SHA256,
                null);

        signer.signDetached(
                new BouncyCastleDigest(),
                signature,
                new java.security.cert.Certificate[]{certificate},
                null, null, null, 0,
                PdfSigner.CryptoStandard.CMS);

//        System.out.println("✅ PDF Successfully Signed: " + signedPdf.getName());
        unsignedPdf.delete();
        return signedPdf;
    }

} // END CLASS DynamicValuePdf