package com.geojit.contractnotes.equity_combinemargin.v1;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.*;

import com.geojit.contractnotes.equity_combinemargin.v1.DTO.EquityDtoV2;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.CAHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.CustomerModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.DHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.DealingOfficeAddress;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.FOHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.FooterModelV2;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.MHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.OHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.PHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SCapitalHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SFuturesHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SSHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.STTHeaderTypeModel;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.HorizontalAlignment;

import java.util.List;
import java.util.Objects;
import com.itextpdf.io.font.PdfEncodings;

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
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import java.io.*;
import java.net.URL;
import java.security.Security;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GEOJIT CONTRACT NOTE PDF GENERATOR V10 - COMPLETE DYNAMIC IMPLEMENTATION
 * ═══════════════════════════════════════════════════════════════════════════
 * ✅ ALL BORDERS FIXED: 1pt solid black throughout
 * ✅ ALL SPACING CORRECTED: Margins and padding standardized
 * ✅ ALL ALIGNMENT FIXED: Proper text and cell alignment
 *
 * @author Geojit PDF Team
 * @version 10.1 - Complete Alignment & Border Fix
 */
public class DynamicValuePDF_Shivraj implements RequestHandler<S3Event, Integer> {

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTANTS & CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════

    // Digital Signature Password
    public static final char[] PASSWORD = "GeojitPassword".toCharArray();

    // S3 Buckets
    private static final String JSON_BUCKET = "json-s3-geojit";
    private static final String PDF_BUCKET = "pdf-s3-geojit";

    // Font Resources
    private static final ClassLoader classLoader = DynamicValuePDF_Shivraj.class.getClassLoader();
    private static final String CALIBRI_FONT = "calibri-400.ttf";
    private static final String CALIBRI_BOLD = "calibri-Bold.ttf";

    // Font Sizes (Exact Geojit Specifications)
    private static final float FONT_SIZE_TITLE = 18f;
    private static final float FONT_SIZE_SUBTITLE = 11f;
    private static final float FONT_SIZE_HEADER = 10f;
    private static final float FONT_SIZE_NORMAL = 8f;
    private static final float FONT_SIZE_DATA = 7.5f;
    private static final float FONT_SIZE_SMALL = 7f;
    private static final float FONT_SIZE_TINY = 6.5f;
    private static final float FONT_SIZE_MICRO = 6f;

    // Geojit Brand Colors (Exact RGB values from PDF)
    private static final Color GEOJIT_GREEN = new DeviceRgb(0, 114, 114);        // #007272
    private static final Color GEOJIT_DARK_GREEN = new DeviceRgb(0, 90, 90);     // Darker shade
    private static final Color HEADER_BG_COLOR = new DeviceRgb(245, 245, 245);   // #F5F5F5
    private static final Color TABLE_HEADER_GREY = new DeviceRgb(240, 240, 240); // #F0F0F0
    private static final Color LIGHT_GREEN_BG = new DeviceRgb(200, 240, 240);    // #C8F0F0
    private static final Color BORDER_COLOR = ColorConstants.BLACK;

    // Decimal Formatters
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat decimalFormat4 = new DecimalFormat("#,##0.0000");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    // AWS Clients
    private final AmazonS3 s3Client;
    private final ObjectMapper objectMapper;
    private String sourceBucket;

    // Fonts (iText 7)
    private PdfFont calibriNormal;
    private PdfFont calibriBold;
    private ImageData logoImageData;

    // ═══════════════════════════════════════════════════════════════════════
    //  DTO STORAGE - ALL MODELS FOR DYNAMIC DATA FETCHING
    // ═══════════════════════════════════════════════════════════════════════

    private CustomerModel customer;
    private DealingOfficeAddress dealingOffice;
    private List<FooterModelV2> footerList = new ArrayList<>();
    private List<FOHeaderTypeModel> foHeaderTypeList = new ArrayList<>();
    private List<OHeaderTypeModel> oHeaderTypeList = new ArrayList<>();
    private List<DHeaderTypeModel> dHeaderTypeList = new ArrayList<>();
    private List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList = new ArrayList<>();
    private List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList = new ArrayList<>();
    private List<CAHeaderTypeModel> caHeaderTypeList = new ArrayList<>();
    private List<SSHeaderTypeModel> ssHeaderTypeModels = new ArrayList<>();
    private List<STTHeaderTypeModel> sttHeaderTypeModels = new ArrayList<>();
    private List<PHeaderTypeModel> pHeaderTypeModels = new ArrayList<>();
    private List<MHeaderTypeModel> mHeaderTypeModels = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public DynamicValuePDF_Shivraj() {
        this.s3Client = AmazonS3ClientBuilder.defaultClient();
        this.objectMapper = new ObjectMapper();


        // Register BouncyCastle for digital signatures
        Security.addProvider(new BouncyCastleProvider());

        try {
            initializeFonts();
            System.out.println("✓ Geojit PDF Generator V10.1 initialized with fonts loaded");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize fonts in constructor: " + e.getMessage());
            try {
                this.calibriNormal = PdfFontFactory.createFont();
                this.calibriBold = PdfFontFactory.createFont();
            } catch (Exception ex) {
                throw new RuntimeException("Cannot initialize PDF fonts", ex);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LAMBDA HANDLER ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public Integer handleRequest(S3Event s3Event, Context context) {
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("  GEOJIT PDF GENERATOR V10.1 - FIXED IMPLEMENTATION");
        System.out.println("════════════════════════════════════════════════════════");

        try {
            // Extract JSON key from S3 event
            String jsonKey = extractJsonKeyFromS3Event(s3Event);

            System.out.println("→ Processing JSON: " + jsonKey);

            // Step 1: Download JSON from S3
            EquityDtoV2 dto = downloadAndParseDTO(jsonKey);

            // Step 2: Load all models from DTO
            loadModelsFromDTO(dto);

            // Step 3: Validate data
            validateData();

            // Step 4: Generate PDF
            File pdfFile = generatePDF(customer, dealingOffice, footerList, foHeaderTypeList,
                    oHeaderTypeList, dHeaderTypeList, sCapitalHeaderTypeList,
                    sFuturesHeaderTypeList, caHeaderTypeList, ssHeaderTypeModels,
                    sttHeaderTypeModels, pHeaderTypeModels, mHeaderTypeModels);

            // Step 5: Upload to S3
            String s3Key = uploadPDFToS3(pdfFile);

            // Step 6: Generate presigned URL7
            String downloadUrl = generatePresignedUrl(s3Key);

            System.out.println("✓ PDF Generated Successfully!");
            System.out.println("   Contract No: " + customer.getContractNo());
            System.out.println("   Client: " + customer.getName());
            System.out.println("   S3 Key: " + s3Key);
            System.out.println("   Download URL: " + downloadUrl);
            System.out.println("════════════════════════════════════════════════════════");

            return 200; // Success

        } catch (Exception e) {
            System.err.println("✗ ERROR generating PDF: " + e.getMessage());
            e.printStackTrace();
            return 500; // Error
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 1: DOWNLOAD & PARSE DTO FROM S3
    // ═══════════════════════════════════════════════════════════════════════

    private EquityDtoV2 downloadAndParseDTO(String jsonKey) throws Exception {
        System.out.println("   [1/7] Downloading JSON from S3...");

        String bucketToUse = (this.sourceBucket != null) ? this.sourceBucket : JSON_BUCKET;

        System.out.println("       → Bucket: " + bucketToUse);
        System.out.println("       → Key: " + jsonKey);

        S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucketToUse, jsonKey));

        try (InputStream inputStream = s3Object.getObjectContent()) {
            EquityDtoV2 dto = objectMapper.readValue(inputStream, EquityDtoV2.class);
            System.out.println("       ✓ JSON parsed successfully");
            return dto;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EXTRACT JSON KEY FROM S3 EVENT
    // ═══════════════════════════════════════════════════════════════════════

    private String extractJsonKeyFromS3Event(S3Event s3Event) {
        if (s3Event == null || s3Event.getRecords() == null || s3Event.getRecords().isEmpty()) {
            throw new IllegalArgumentException("S3 event is null or has no records");
        }

        S3EventNotification.S3EventNotificationRecord record = s3Event.getRecords().get(0);

        this.sourceBucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();

        try {
            key = java.net.URLDecoder.decode(key, "UTF-8");
        } catch (Exception e) {
            System.err.println("Warning: Could not URL decode key: " + key);
        }

        System.out.println("   S3 Event Details:");
        System.out.println("   → Source Bucket: " + this.sourceBucket);
        System.out.println("   → File Key: " + key);

        return key;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 2: LOAD ALL MODELS FROM DTO (100% DYNAMIC)
    // ═══════════════════════════════════════════════════════════════════════

    private void loadModelsFromDTO(EquityDtoV2 dto) {
        System.out.println("   [2/7] Loading models from DTO...");

        List<CustomerModel> customerList = dto.getCustomerList();
        this.customer = (customerList != null && !customerList.isEmpty())
                ? customerList.get(0)
                : new CustomerModel();

        System.out.println("       → Customer: " + customer.getName());
        System.out.println("       → Contract No: " + customer.getContractNo());
        System.out.println("       → Trade Date: " + customer.getTransactionDate());

        List<DealingOfficeAddress> dealingOfficeList = dto.getDealingOfficeAddressList();
        this.dealingOffice = (dealingOfficeList != null && !dealingOfficeList.isEmpty())
                ? dealingOfficeList.get(0)
                : new DealingOfficeAddress();

        if (dealingOffice != null && dealingOffice.getDealingAddress() != null) {
            System.out.println("       → Dealing Office: " + dealingOffice.getDealingAddress());
        }

        this.dHeaderTypeList = dto.getdHeaderTypeList() != null ?
                dto.getdHeaderTypeList() : new ArrayList<>();
        System.out.println("       → Equity/Order Transactions: " + dHeaderTypeList.size());

        this.foHeaderTypeList = dto.getFoHeaderTypeList() != null ?
                dto.getFoHeaderTypeList() : new ArrayList<>();
        System.out.println("       → Derivative Summaries: " + foHeaderTypeList.size());

        this.oHeaderTypeList = dto.getoHeaderTypeList() != null ?
                dto.getoHeaderTypeList() : new ArrayList<>();
        System.out.println("       → Order Details: " + oHeaderTypeList.size());

        this.footerList = dto.getFooterList() != null ?
                dto.getFooterList() : new ArrayList<>();
        System.out.println("       → Footer/Charges: " + footerList.size());

        this.mHeaderTypeModels = dto.getmHeaderTypeList() != null ?
                dto.getmHeaderTypeList() : new ArrayList<>();
        System.out.println("       → Margin Details: " + mHeaderTypeModels.size());

        this.pHeaderTypeModels = dto.getpHeaderTypeList() != null ?
                dto.getpHeaderTypeList() : new ArrayList<>();
        System.out.println("       → Pledge Securities: " + pHeaderTypeModels.size());

        this.caHeaderTypeList = dto.getCaHeaderTypeList() != null ?
                dto.getCaHeaderTypeList() : new ArrayList<>();

        this.sCapitalHeaderTypeList = dto.getSCapitalHeaderTypeList() != null ?
                dto.getSCapitalHeaderTypeList() : new ArrayList<>();

        this.sFuturesHeaderTypeList = dto.getSFuturesHeaderTypeList() != null ?
                dto.getSFuturesHeaderTypeList() : new ArrayList<>();

        this.ssHeaderTypeModels = dto.getSsHeaderTypeList() != null ?
                dto.getSsHeaderTypeList() : new ArrayList<>();

        this.sttHeaderTypeModels = dto.getSttHeaderTypeList() != null ?
                dto.getSttHeaderTypeList() : new ArrayList<>();

        normalizeMarginData();
        convertFuturesDataToFOFormat();

        System.out.println("       ✓ All models loaded successfully");
    }
// ═══════════════════════════════════════════════════════════════════════
//  FIX: CONVERT FUTURES DATA TO FO FORMAT
// ═══════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════
//  FIX: CONVERT FUTURES DATA TO FO FORMAT - CORRECTED FIELD NAMES
// ═══════════════════════════════════════════════════════════════════════

    private void convertFuturesDataToFOFormat() {
        System.out.println("   [2.5/7] Converting Futures data to FO format...");

        // Convert S|FUTURES entries to FOHeaderTypeModel format
        for (SFuturesHeaderTypeModel futures : sFuturesHeaderTypeList) {
            FOHeaderTypeModel fo = new FOHeaderTypeModel();

            // Map fields from SFuturesHeaderTypeModel to FOHeaderTypeModel
            fo.setSecurityname(safe(futures.getContractDesc()));
            fo.setSymbol(safe(futures.getContractDesc()));
            fo.setBuySell(safe(futures.getTradeType()));  // B or S
            fo.setQuantity(safe(futures.getTradeQty()));
            fo.setPrice(safe(futures.getTradeWap()));
            fo.setBrokerage(safe(futures.getTradeBrokerage()));
            fo.setClosingRate(safe(futures.getTradeClosingRate()));
            fo.setNetTotal(safe(futures.getTradeNetTotal()));
            fo.setRemarks(safe(futures.getRemarks()));

            foHeaderTypeList.add(fo);
            System.out.println("       → Converted: " + fo.getSecurityname() + " " + fo.getBuySell());
        }

        System.out.println("       ✓ Converted " + sFuturesHeaderTypeList.size() + " futures entries to FO format");
    }    // ═══════════════════════════════════════════════════════════════════════
    //  NORMALIZE MARGIN DATA (PRODUCTION FIX)
    // ═══════════════════════════════════════════════════════════════════════

    private void normalizeMarginData() {
        if (mHeaderTypeModels == null || mHeaderTypeModels.isEmpty()) {
            System.out.println("       → No margin records to normalize");
            return;
        }

        System.out.println("       → Normalizing margin data for " + mHeaderTypeModels.size() + " records...");

        for (MHeaderTypeModel m : mHeaderTypeModels) {
            boolean needsNormalization = (m.getFunds() == null || m.getFunds().trim().isEmpty()) &&
                    (m.getPayInPayOut() != null && !m.getPayInPayOut().trim().isEmpty());

            if (needsNormalization) {
                if (m.getTradeday() == null || m.getTradeday().trim().isEmpty()) {
                    m.setTradeday(safe(m.getDate()));
                }

                m.setFunds(safe(m.getPayInPayOut()));
                m.setSecurities(safe(m.getMtmProfit()));
                m.setMarginpledgesecurities(safe(m.getPremium()));
                m.setBankguarantees(safe(m.getOptionValue()));
                m.setOtherapproved(safe(m.getAssignmentValue()));
                m.setTotalmarginsavailable(safe(m.getTotal()));
                m.setTotalupfrontmargin(safe(m.getDebits()));
                m.setConsolidatedcrystallized(safe(m.getTdsFund()));
                m.setDeliverymargin(safe(m.getOtherDebits()));
                m.setTotalrequirement(safe(m.getTotalDebits()));
                m.setExcessshortfall(safe(m.getBalance()));
                m.setAdditionalmargin(safe(m.getPreviousBalance()));
                m.setMarginstatus(safe(m.getNetBalance()));

                System.out.println("       → Normalized: " + m.getExchange() + " " + m.getSegment() +
                        " | Funds=" + m.getFunds());
            }
        }

        System.out.println("       ✓ Margin data normalization complete");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 3: VALIDATE DATA
    // ═══════════════════════════════════════════════════════════════════════

    private void validateData() throws Exception {
        System.out.println("   [3/6] Validating data...");

        if (customer == null) {
            throw new Exception("CRITICAL: Customer data is missing!");
        }

        if (customer.getContractNo() == null || customer.getContractNo().isEmpty()) {
            throw new Exception("CRITICAL: Contract Note Number is missing!");
        }

        if (customer.getName() == null || customer.getName().isEmpty()) {
            throw new Exception("CRITICAL: Client Name is missing!");
        }

        if (dHeaderTypeList.isEmpty() && foHeaderTypeList.isEmpty()) {
            System.out.println("       ⚠ WARNING: No transactions found!");
        }

        if (footerList.isEmpty()) {
            System.out.println("       ⚠ WARNING: No footer/charges data found!");
        }

        System.out.println("       ✓ Data validation passed");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 4: INITIALIZE FONTS
    // ═══════════════════════════════════════════════════════════════════════

    public void initializeFonts() throws Exception {
        System.out.println("    Initializing fonts...");

        try {
            try {
                InputStream logoStream = getClass().getClassLoader()
                        .getResourceAsStream("logo.png");

                if (logoStream != null) {
                    byte[] logoBytes = readAllBytes(logoStream);
                    logoImageData = ImageDataFactory.create(logoBytes);
                } else {
                    System.out.println("Logo not found in resources");
                }

            } catch (Exception e) {
                System.out.println("Error loading logo globally: " + e.getMessage());
            }
            InputStream calibriNormalStream = classLoader.getResourceAsStream(CALIBRI_FONT);
            if (calibriNormalStream != null) {
                byte[] calibriNormalBytes = readAllBytes(calibriNormalStream);
                this.calibriNormal = PdfFontFactory.createFont(calibriNormalBytes, PdfEncodings.IDENTITY_H);
                System.out.println("       → Calibri Regular loaded");
            } else {
                this.calibriNormal = PdfFontFactory.createFont();
                System.out.println("       ⚠ Calibri Regular not found, using default");
            }

            InputStream calibriBoldStream = classLoader.getResourceAsStream(CALIBRI_BOLD);
            if (calibriBoldStream != null) {
                byte[] calibriBoldBytes = readAllBytes(calibriBoldStream);
                this.calibriBold = PdfFontFactory.createFont(calibriBoldBytes, PdfEncodings.IDENTITY_H);
                System.out.println("       → Calibri Bold loaded");
            } else {
                this.calibriBold = PdfFontFactory.createFont();
                System.out.println("       ⚠ Calibri Bold not found, using default");
            }

            System.out.println("       ✓ Fonts initialized");

        } catch (Exception e) {
            System.err.println("       ✗ Error loading fonts: " + e.getMessage());
            this.calibriNormal = PdfFontFactory.createFont();
            this.calibriBold = PdfFontFactory.createFont();
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 5: GENERATE PDF (MAIN METHOD)
    // ═══════════════════════════════════════════════════════════════════════

    public File generatePDF(CustomerModel customer, DealingOfficeAddress dealingOffice,
                            List<FooterModelV2> footerList, List<FOHeaderTypeModel> foHeaderTypeList,
                            List<OHeaderTypeModel> oHeaderTypeList, List<DHeaderTypeModel> dHeaderTypeList,
                            List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList,
                            List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList,
                            List<CAHeaderTypeModel> caHeaderTypeList, List<SSHeaderTypeModel> ssHeaderTypeList,
                            List<STTHeaderTypeModel> sttHeaderTypeList, List<PHeaderTypeModel> pHeaderTypeList,
                            List<MHeaderTypeModel> mHeaderTypeList) throws Exception {

        this.customer = customer;
        this.dealingOffice = dealingOffice;
        this.footerList = footerList;
        this.foHeaderTypeList = foHeaderTypeList;
        this.oHeaderTypeList = oHeaderTypeList;
        this.dHeaderTypeList = dHeaderTypeList;
        this.sCapitalHeaderTypeList = sCapitalHeaderTypeList;
        this.sFuturesHeaderTypeList = sFuturesHeaderTypeList;
        this.caHeaderTypeList = caHeaderTypeList;
        this.ssHeaderTypeModels = ssHeaderTypeList;
        this.sttHeaderTypeModels = sttHeaderTypeList;
        this.pHeaderTypeModels = pHeaderTypeList;
        this.mHeaderTypeModels = mHeaderTypeList;

        System.out.println("✓ Geojit PDF Generator V10.1 initialized");
        System.out.println("[4/6] Generating PDF...");

        String fileName = "Geojit_Contract_Note_" + this.customer.getContractNo() + ".pdf";
        File pdfFile = new File("/tmp/" + fileName);
//
//        String outputDir = System.getProperty("user.dir"); // project root
//        File pdfFile = new File(outputDir + "/" + fileName);


        PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4);

        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PageNumberEventHandler());

        Document document = new Document(pdfDoc);
        document.setMargins(70, 20, 40, 20);

        try {
            // PAGE 1: Main Contract Note
            System.out.println("       → Generating Page 1: Main Contract Note");
            addHeader(document);
            addClientInformation(document);
            addEquitySegment(document);
            addDerivativeSegment(document);
            addExchangeSummary(document);
            addFooterNotes(document);
            addSignature(document);
            addDisclaimer(document);

            // PAGE 2: Trade Details Annexure
            System.out.println("       → Generating Page 2: Trade Details Annexure");
            document.add(new Paragraph("\n"));
            addTradeDetailsAnnexure(document);

            // PAGE 3: Margin Statement
            System.out.println("       → Generating Page 3: Margin Statement");
            document.add(new Paragraph("\n"));
            addMarginStatement(document);
            addMarginPledgeSecurities(document);

            System.out.println("       ✓ PDF content generated");

        } finally {
            document.close();
        }

        System.out.println("       ✓ PDF file created: " + pdfFile.getAbsolutePath());
        return pdfFile;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 6: UPLOAD PDF TO S3
    // ═══════════════════════════════════════════════════════════════════════

    private String uploadPDFToS3(File pdfFile) throws Exception {
        System.out.println("   [5/6] Uploading PDF to S3...");

        String s3Key = "pdfs/" + customer.getContractNo() + "/" + pdfFile.getName();

        PutObjectRequest putRequest = new PutObjectRequest(PDF_BUCKET, s3Key, pdfFile)
                .withMetadata(new ObjectMetadata());

        s3Client.putObject(putRequest);

        System.out.println("       ✓ PDF uploaded to S3: " + s3Key);
        return s3Key;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STEP 7: GENERATE PRESIGNED URL
    // ═══════════════════════════════════════════════════════════════════════

    private String generatePresignedUrl(String s3Key) {
        System.out.println("   [7/7] Generating presigned URL...");

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60 * 24 * 7; // 7 days
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(PDF_BUCKET, s3Key)
                        .withMethod(com.amazonaws.HttpMethod.GET)
                        .withExpiration(expiration);

        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

        System.out.println("       ✓ Presigned URL generated (valid for 7 days)");
        return url.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAGE NUMBER EVENT HANDLER
    // ═══════════════════════════════════════════════════════════════════════

    private class PageNumberEventHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {

            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNum = pdfDoc.getPageNumber(page);

            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(
                    page.newContentStreamBefore(),
                    page.getResources(),
                    pdfDoc
            );

            Canvas canvas = new Canvas(pdfCanvas, pageSize);

            // =============================
            // ADD LOGO ON EVERY PAGE
            // =============================
            if (logoImageData != null) {
                Image logo = new Image(logoImageData);
                logo.scaleToFit(60, 40);

                float x = pageSize.getLeft() + 20;  // LEFT MARGIN
                float y = pageSize.getTop() - 50;   // TOP POSITION

                canvas.add(logo.setFixedPosition(pageNum, x, y));
            }

            // =============================
            // PAGE NUMBER (RIGHT SIDE)
            // =============================
            Paragraph p = new Paragraph("Page No : " + pageNum)
                    .setFontSize(FONT_SIZE_MICRO)
                    .setFontColor(GEOJIT_DARK_GREEN);

            canvas.showTextAligned(
                    p,
                    pageSize.getRight() - 30,
                    pageSize.getBottom() + 20,
                    TextAlignment.RIGHT
            );

            canvas.close();
        }

    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HEADER SECTION - ✅ FIXED: 1pt borders, correct spacing
    // ═══════════════════════════════════════════════════════════════════════

    private void addHeader(Document document) {
        try {

            // ===============================
            // MAIN OUTER HEADER CONTAINER
            // ===============================

            Table mainHeader = new Table(new float[]{1.2f, 3.8f, 1.3f});
            mainHeader.setWidth(UnitValue.createPercentValue(100));
            mainHeader.setBorder(Border.NO_BORDER);
            mainHeader.setMarginBottom(0);

            // ===============================
            // LEFT : LOGO IMAGE
            // ===============================

//            Image logo;
//
//            try {
//                InputStream logoStream = getClass().getClassLoader()
//                        .getResourceAsStream("logo.png");
//
//                if (logoStream == null) {
//                    throw new RuntimeException("Logo not found in resources");
//                }
//
//                byte[] logoBytes = readAllBytes(logoStream);
//                ImageData imageData = ImageDataFactory.create(logoBytes);
//                logo = new Image(imageData);
//                logo.scaleToFit(80, 60);
//            } catch (Exception e) {
//                logo = new Image(ImageDataFactory.create(new byte[]{}));
//            }

            Cell logoCell = new Cell()
//                    .add(logo.setHorizontalAlignment(HorizontalAlignment.CENTER))
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(8)
                    .setBorderRight(Border.NO_BORDER)   // ✅ NO BORDER
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderBottom(Border.NO_BORDER);

            mainHeader.addCell(logoCell);

            // ===============================
            // CENTER : TITLE SECTION
            // ===============================

            Cell titleCell = new Cell()
                    .add(new Paragraph("CONTRACT NOTE CUM TAX")
                            .setFont(calibriBold)
                            .setFontSize(18)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(0))
                    .add(new Paragraph("INVOICE")
                            .setFont(calibriBold)
                            .setFontSize(18)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(0))
                    .add(new Paragraph("(Tax Invoice under Section 31 of GST Act)")
                            .setFont(calibriNormal)
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(2))
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setPadding(12)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBorder(Border.NO_BORDER);

            mainHeader.addCell(titleCell);

            // ===============================
            // RIGHT : ORIGINAL FOR RECIPIENT
            // ===============================

            Cell originalCell = new Cell()
                    .add(new Paragraph("ORIGINAL FOR\nRECIPIENT")
                            .setFont(calibriBold)
                            .setFontSize(10)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(10)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderBottom(Border.NO_BORDER);

            mainHeader.addCell(originalCell);

            document.add(mainHeader);

            // =====================================
            // COMPANY INFORMATION BOX
            // =====================================

            Table companyBox = new Table(1);
            companyBox.setWidth(UnitValue.createPercentValue(100));
            companyBox.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));
            companyBox.setMarginTop(0);

            Cell companyCell = new Cell()
                    .add(new Paragraph("GEOJIT INVESTMENTS LTD")
                            .setFont(calibriBold)
                            .setFontSize(11)
                            .setTextAlignment(TextAlignment.CENTER))
                    .add(new Paragraph("SEBI REGISTRATION NO : INZ000318938 | CIN No : U66110KL2023PLC080586")
                            .setFontSize(8)
                            .setTextAlignment(TextAlignment.CENTER))
                    .add(new Paragraph("7TH FLOOR, 34/659-P, CIVIL LINE ROAD, PADIVATTOM, KOCHI- 682024 | TEL: 0484-2901000 | FAX:0484 2979695 | Website: www.geojit.com/gil")
                            .setFontSize(7)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(6)
                    .setBorder(Border.NO_BORDER);

            companyBox.addCell(companyCell);

            document.add(companyBox);

            // =====================================
            // COMPLIANCE OFFICER BOX
            // =====================================

            Table complianceBox = new Table(1);
            complianceBox.setWidth(UnitValue.createPercentValue(100));
            complianceBox.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));

            complianceBox.addCell(
                    new Cell()
                            .add(new Paragraph("NAME OF THE COMPLIANCE OFFICER : Ancy C Sunny | EMAIL: compliance@geojit.com | TEL: 0484-2901000 | EMAIL ID FOR INVESTOR COMPLAINT: grievances@geojit.com")
                                    .setFontSize(7)
                                    .setTextAlignment(TextAlignment.CENTER))
                            .setPadding(5)
                            .setBorder(Border.NO_BORDER)
            );

            document.add(complianceBox);

            // =====================================
            // DEALING OFFICE BOX
            // =====================================

            Table dealingBox = new Table(1);
            dealingBox.setWidth(UnitValue.createPercentValue(100));
            dealingBox.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));

            String dealingAddress = (dealingOffice != null && dealingOffice.getDealingAddress() != null)
                    ? dealingOffice.getDealingAddress()
                    : "";

            dealingBox.addCell(
                    new Cell()
                            .add(new Paragraph("DEALING OFFICES ADDRESS : " + dealingAddress)
                                    .setFontSize(7)
                                    .setTextAlignment(TextAlignment.CENTER))
                            .setPadding(5)
                            .setBorder(Border.NO_BORDER)
            );

            document.add(dealingBox);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CLIENT INFORMATION - ✅ FIXED: 1pt borders, correct spacing
    // ═══════════════════════════════════════════════════════════════════════

    private void addClientInformation(Document document) {
        Table containerTable = new Table(new float[]{2.5f, 3.5f});  // ✅ Adjusted ratio
        containerTable.setWidth(UnitValue.createPercentValue(100));
        containerTable.setMarginTop(2);
        containerTable.setBorder(Border.NO_BORDER);

        // ═══════════════════════════════════════════════════════════════════
        // LEFT SECTION: Client Details
        // ═══════════════════════════════════════════════════════════════════
        Table leftTable = new Table(new float[]{1.6f, 2f});
        leftTable.setWidth(UnitValue.createPercentValue(100));

        leftTable.addCell(createInfoLabelCell("CONTRACT NOTE NO :"));
        leftTable.addCell(createInfoValueCell(safe(customer.getContractNo()), true));

        leftTable.addCell(createInfoLabelCell("Name Of the Client :"));
        leftTable.addCell(createInfoValueCell(safe(customer.getName()), true));

        leftTable.addCell(createInfoLabelCell("Address of the Client :"));
        StringBuilder address = new StringBuilder();
        if (notEmpty(customer.getAddress1())) address.append(customer.getAddress1());
        if (notEmpty(customer.getAddress2())) {
            if (address.length() > 0) address.append(",\n");
            address.append(customer.getAddress2());
        }
        if (notEmpty(customer.getAddress3())) {
            if (address.length() > 0) address.append(",\n");
            address.append(customer.getAddress3());
        }
        leftTable.addCell(createInfoValueCell(address.toString(), false));

        leftTable.addCell(createInfoLabelCell("Phone No :"));
        leftTable.addCell(createInfoValueCell(safe(customer.getMobileNo()), false));

        leftTable.addCell(createInfoLabelCell("TradeCode/UCC of Client :"));
        leftTable.addCell(createInfoValueCell(safe(customer.getClientCode()), false));

        leftTable.addCell(createInfoLabelCell("Place Of Supply [State Code] :"));
        leftTable.addCell(createInfoValueCell("KERALA[32]", false));

        leftTable.addCell(createInfoLabelCell("Invoice Reference Number(IRN) :"));
        leftTable.addCell(createInfoValueCell(safe(customer.getIrn()), false));

        leftTable.addCell(createInfoLabelCell("GST Identification No. :"));
        leftTable.addCell(createInfoValueCell(safe(customer.getGstNo()), false));

        // ═══════════════════════════════════════════════════════════════════
        // RIGHT SECTION: Trade Date + Exchange Table (VERTICALLY STACKED)
        // ═══════════════════════════════════════════════════════════════════
        Table rightSection = new Table(1);
        rightSection.setWidth(UnitValue.createPercentValue(100));
        rightSection.setBorder(Border.NO_BORDER);

        // Trade Date Cell
        Cell tradeDateCell = new Cell()
                .add(new Paragraph("TRADE DATE : " + safe(customer.getTransactionDate()))
                        .setFontSize(FONT_SIZE_DATA)
                        .setFont(calibriBold)
                        .setTextAlignment(TextAlignment.LEFT))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3);
        rightSection.addCell(tradeDateCell);

        // Exchange Table
        Table exchangeTable = new Table(new float[]{2.2f, 1f, 1.1f, 1.3f, 1f});
        exchangeTable.setWidth(UnitValue.createPercentValue(100));
        exchangeTable.setMarginTop(0);

        exchangeTable.addHeaderCell(createExchangeHeaderCell("EXCHANGE /\nCLEARING\nCORPORATION"));
        exchangeTable.addHeaderCell(createExchangeHeaderCell("SEGMENT"));
        exchangeTable.addHeaderCell(createExchangeHeaderCell("STTLNO"));
        exchangeTable.addHeaderCell(createExchangeHeaderCell("STTLDATE"));
        exchangeTable.addHeaderCell(createExchangeHeaderCell("UCCODE"));

        // Get unique exchanges from dHeaderTypeList
        Map<String, DHeaderTypeModel> uniqueExchanges = new LinkedHashMap<>();
        for (DHeaderTypeModel d : dHeaderTypeList) {
            String key = safe(d.getExchange()) + "|" + safe(d.getSegment());
            if (!uniqueExchanges.containsKey(key)) {
                uniqueExchanges.put(key, d);
            }
        }

        // Add exchange data rows
        for (DHeaderTypeModel d : uniqueExchanges.values()) {
            String exchange = safe(d.getExchange());
            String segment = safe(d.getSegment());
            String sttlNo = safe(d.getSettlementNo());
            String sttlDate = safe(d.getSettlementdate());
            String ucCode = safe(customer.getClientCode());

            String exchangeClearing = exchange + " / NCL";
            String displaySegment = segment;
            if ("CAPITAL".equalsIgnoreCase(segment)) {
                displaySegment = "EN";
            } else if ("FUTURES".equalsIgnoreCase(segment)) {
                displaySegment = "FO";
            }

            exchangeTable.addCell(createExchangeDataCell(exchangeClearing, TextAlignment.LEFT));
            exchangeTable.addCell(createExchangeDataCell(displaySegment, TextAlignment.CENTER));
            exchangeTable.addCell(createExchangeDataCell(sttlNo, TextAlignment.CENTER));
            exchangeTable.addCell(createExchangeDataCell(sttlDate, TextAlignment.CENTER));
            exchangeTable.addCell(createExchangeDataCell(ucCode, TextAlignment.CENTER));
        }

        // Add exchange table to right section
        rightSection.addCell(new Cell()
                .add(exchangeTable)
                .setBorder(Border.NO_BORDER)
                .setPadding(0));

        // ═══════════════════════════════════════════════════════════════════
        // ADD LEFT AND RIGHT TO CONTAINER (SIDE BY SIDE)
        // ═══════════════════════════════════════════════════════════════════
        containerTable.addCell(new Cell()
                .add(leftTable)
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP));  // ✅ Top align

        containerTable.addCell(new Cell()
                .add(rightSection)
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP));  // ✅ Top align

        document.add(containerTable);

        // PAN confirmation text
        Paragraph confirmText = new Paragraph()
                .add(new Text("Sir/Madam, I / We have this day done by your order and on your account the following transactions: ")
                        .setFontSize(FONT_SIZE_DATA))
                .add(new Text("PAN of the Client : " + safe(customer.getPanNo()))
                        .setFontSize(FONT_SIZE_DATA)
                        .setFont(calibriBold)
                        .setFontColor(GEOJIT_DARK_GREEN))
                .setMarginTop(4)
                .setMarginBottom(4);
        document.add(confirmText);
    }
    // ═══════════════════════════════════════════════════════════════════════
    //  HELPER METHODS - CLIENT INFORMATION - ✅ FIXED: 1pt borders
    // ═══════════════════════════════════════════════════════════════════════

    private Cell createInfoLabelCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(FONT_SIZE_SMALL))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    private Cell createInfoValueCell(String text, boolean bold) {
        Paragraph p = new Paragraph(text).setFontSize(FONT_SIZE_SMALL);
        if (bold) {
            p.setFont(calibriBold).setFontColor(GEOJIT_DARK_GREEN);
        }
        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    private Cell createExchangeHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(HEADER_BG_COLOR)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createExchangeDataCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(FONT_SIZE_SMALL)
                        .setTextAlignment(alignment))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  END OF PART 1
    //  Continue to Part 2 for remaining methods...
    // ═══════════════════════════════════════════════════════════════════════}
        /**
         * ═══════════════════════════════════════════════════════════════════════════
         *  EQUITY SEGMENT - 100% DYNAMIC from DHeaderTypeModel
         * ═══════════════════════════════════════════════════════════════════════════
         */
        // ═══════════════════════════════════════════════════════════════════════════
        //  PART 2 - CONTINUATION OF DynamicValuePDF.java
        //  Paste this after Part 1 (inside the DynamicValuePDF class)
        // ═══════════════════════════════════════════════════════════════════════════

        // ═══════════════════════════════════════════════════════════════════════
        //  EQUITY SEGMENT - ✅ FIXED: 1pt borders, correct spacing
        // ═══════════════════════════════════════════════════════════════════════

        private void addEquitySegment(Document document) {
            // ✅ ADD DEBUG LOGGING
            System.out.println("📊 addEquitySegment called");
            System.out.println("   → dHeaderTypeList size: " + dHeaderTypeList.size());

            if (dHeaderTypeList.isEmpty()) {
                System.out.println("       → No equity transactions found, skipping equity segment");
                return;
            }

            // ✅ DEBUG: Print what segments we have
            for (DHeaderTypeModel d : dHeaderTypeList) {
                System.out.println("   → Found segment: " + d.getSegment() + " | BuySell: " + d.getBuySell() + " | ISIN: " + d.getIsin());
            }

            Paragraph sectionTitle = new Paragraph("Equity Segment")
                    .setFont(calibriBold)
                    .setFontSize(FONT_SIZE_HEADER)
                    .setFontColor(GEOJIT_DARK_GREEN)
                    .setMarginTop(8)
                    .setMarginBottom(4);
            document.add(sectionTitle);

            float[] columnWidths = {0.8f, 2.0f, 0.6f, 0.8f, 0.75f, 0.8f, 0.9f, 0.6f, 0.8f, 0.75f, 0.8f, 0.9f, 0.6f, 0.95f};

            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setFontSize(FONT_SIZE_TINY);

            // Headers
            Cell securityDescHeader = new Cell(2, 2)
                    .add(new Paragraph("Security\nDescription")
                            .setFontSize(FONT_SIZE_SMALL)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(securityDescHeader);

            Cell buyHeader = new Cell(1, 5)
                    .add(new Paragraph("Buy")
                            .setFontSize(FONT_SIZE_DATA)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(buyHeader);

            Cell sellHeader = new Cell(1, 5)
                    .add(new Paragraph("Sell")
                            .setFontSize(FONT_SIZE_DATA)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(sellHeader);

            Cell netObligationHeader = new Cell(2, 2)
                    .add(new Paragraph("Net Obligation for ISIN\n[Before Levies] (Rs)*")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(netObligationHeader);

            // Sub-headers
            table.addHeaderCell(createEquityHeaderCell("Quantity"));
            table.addHeaderCell(createEquityHeaderCell("WAP\n(across\nexchanges)"));
            table.addHeaderCell(createEquityHeaderCell("Brokerage\nPer Share\n(Rs)"));
            table.addHeaderCell(createEquityHeaderCell("WAP (across\nexchanges)\nafter\nbrokerage\n(Rs)"));
            table.addHeaderCell(createEquityHeaderCell("Total Buy\nValue after\nbrokerage"));
            table.addHeaderCell(createEquityHeaderCell("Quantity"));
            table.addHeaderCell(createEquityHeaderCell("WAP\n(across\nexchanges)"));
            table.addHeaderCell(createEquityHeaderCell("Brokerage\nPer Share\n(Rs)"));
            table.addHeaderCell(createEquityHeaderCell("WAP (across\nexchanges)\nafter\nbrokerage\n(Rs)"));
            table.addHeaderCell(createEquityHeaderCell("Total Sell\nValue after\nbrokerage"));

            Cell isinLabel = new Cell()
                    .add(new Paragraph("ISIN")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(TABLE_HEADER_GREY)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(2)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addCell(isinLabel);

            Cell securityNameLabel = new Cell()
                    .add(new Paragraph("Security\nName /\nSymbol")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(TABLE_HEADER_GREY)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(2)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addCell(securityNameLabel);

            for (int i = 0; i < 12; i++) {
                Cell emptyCell = new Cell()
                        .add(new Paragraph(""))
                        .setBackgroundColor(TABLE_HEADER_GREY)
                        .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                        .setPadding(2);
                table.addCell(emptyCell);
            }

            // Data rows
            Map<String, EquityAggregation> aggregatedData = aggregateEquityData();

            // ✅ ADD DEBUG
            System.out.println("   → Aggregated equity data count: " + aggregatedData.size());

            for (Map.Entry<String, EquityAggregation> entry : aggregatedData.entrySet()) {
                String isin = entry.getKey();
                EquityAggregation agg = entry.getValue();

                table.addCell(createEquityDataCell(isin, TextAlignment.LEFT));
                table.addCell(createEquityDataCell(agg.securityName, TextAlignment.LEFT, true));
                table.addCell(createEquityDataCell(String.valueOf(agg.buyQuantity), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(agg.buyWAP), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(agg.buyBrokerage), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(agg.buyWAPAfterBrokerage), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(agg.buyTotalValue), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(String.valueOf(agg.sellQuantity), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(agg.sellWAP), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(agg.sellBrokerage), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(agg.sellWAPAfterBrokerage), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(agg.sellTotalValue), TextAlignment.RIGHT));

                int netQuantity = agg.buyQuantity - agg.sellQuantity;
                double netObligation = agg.buyTotalValue - agg.sellTotalValue;

                table.addCell(createEquityDataCell(String.valueOf(netQuantity), TextAlignment.RIGHT));
                table.addCell(createEquityDataCell(formatDecimal4(netObligation), TextAlignment.RIGHT));
            }

            document.add(table);

            Paragraph footnote = new Paragraph("* Exchange-wise details of orders and trades are provided in separate annexure.")
                    .setFontSize(FONT_SIZE_TINY)
                    .setItalic()
                    .setMarginTop(2);
            document.add(footnote);
        }
    private static class EquityAggregation {
        String securityName;
        int buyQuantity = 0;
        double buyWAP = 0.0;
        double buyBrokerage = 0.0;
        double buyWAPAfterBrokerage = 0.0;
        double buyTotalValue = 0.0;
        int sellQuantity = 0;
        double sellWAP = 0.0;
        double sellBrokerage = 0.0;
        double sellWAPAfterBrokerage = 0.0;
        double sellTotalValue = 0.0;
    }

    private Map<String, EquityAggregation> aggregateEquityData() {
        Map<String, EquityAggregation> aggregated = new LinkedHashMap<>();

        System.out.println("🔍 aggregateEquityData() started");

        for (DHeaderTypeModel d : dHeaderTypeList) {
            String segment = safe(d.getSegment()).toUpperCase();

            // ✅ FIXED: More flexible segment checking
            // Accept: CAPITAL, EN, CASH, EQ, or anything that's NOT a derivative segment
            boolean isEquitySegment = segment.equals("CAPITAL")
                    || segment.equals("EN")
                    || segment.equals("CASH")
                    || segment.equals("EQ")
                    || segment.contains("EQUITY");

            // ✅ Skip only if it's clearly a derivative segment
            boolean isDerivativeSegment = segment.equals("FO")
                    || segment.equals("DERIVATIVES")
                    || segment.contains("FUTURE")
                    || segment.contains("OPTION");

            if (isDerivativeSegment) {
                System.out.println("   ⏭️ Skipping derivative segment: " + segment);
                continue;
            }

            // ✅ If not clearly derivative, check if ISIN exists (equity indicator)
            String isin = safe(d.getIsin());
            if (isin.isEmpty()) {
                System.out.println("   ⏭️ Skipping (no ISIN): segment=" + segment);
                continue;
            }

            System.out.println("   ✅ Processing equity: " + isin + " | Segment: " + segment + " | Qty: " + d.getQuantity());

            EquityAggregation agg = aggregated.computeIfAbsent(isin, k -> new EquityAggregation());

            if (agg.securityName == null || agg.securityName.isEmpty()) {
                agg.securityName = safe(d.getSecurityname());
            }

            int quantity = parseInt(d.getQuantity());
            double price = parseDouble(d.getPrice());
            double brokerage = parseDouble(d.getBrokerage());

            String buySell = safe(d.getBuySell()).toUpperCase();

            if ("B".equals(buySell) || "BUY".equals(buySell)) {
                agg.buyQuantity += quantity;
                double totalValue = quantity * price;
                agg.buyTotalValue += totalValue;

                if (quantity > 0) {
                    agg.buyBrokerage = brokerage / quantity;
                }

                agg.buyWAP = (agg.buyQuantity > 0) ? (agg.buyTotalValue / agg.buyQuantity) : 0.0;
                agg.buyWAPAfterBrokerage = agg.buyWAP + agg.buyBrokerage;
                agg.buyTotalValue += brokerage;

            } else if ("S".equals(buySell) || "SELL".equals(buySell)) {
                agg.sellQuantity += quantity;
                double totalValue = quantity * price;
                agg.sellTotalValue += totalValue;

                if (quantity > 0) {
                    agg.sellBrokerage = brokerage / quantity;
                }

                agg.sellWAP = (agg.sellQuantity > 0) ? (agg.sellTotalValue / agg.sellQuantity) : 0.0;
                agg.sellWAPAfterBrokerage = agg.sellWAP - agg.sellBrokerage;
                agg.sellTotalValue -= brokerage;
            }

        }

        System.out.println("   📊 Total equity ISINs aggregated: " + aggregated.size());
        return aggregated;
    }    private Cell createEquityHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER))
//                .setBackgroundColor(TABLE_HEADER_GREY)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(2)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createEquityDataCell(String content, TextAlignment alignment) {
        return createEquityDataCell(content, alignment, false);
    }

    private Cell createEquityDataCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content)
                .setFontSize(FONT_SIZE_TINY)
                .setTextAlignment(alignment);
        if (bold) {
            p.setFont(calibriBold);
        } else {
            p.setFont(calibriNormal);
        }
        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DERIVATIVE SEGMENT - ✅ FIXED: 1pt borders, correct spacing
    // ═══════════════════════════════════════════════════════════════════════

    private void addDerivativeSegment(Document document) {
        if (foHeaderTypeList.isEmpty()) {
            System.out.println("       → No derivative transactions found, skipping derivative segment");
            return;
        }

        Paragraph sectionTitle = new Paragraph("Derivative Segment")
                .setFont(calibriBold)
                .setFontSize(FONT_SIZE_HEADER)
                .setFontColor(GEOJIT_DARK_GREEN)
                .setMarginTop(8)  // ✅ FIXED: Reduced from 10
                .setMarginBottom(4);  // ✅ FIXED: Reduced from 5
        document.add(sectionTitle);

        float[] columnWidths = {2.5f, 0.6f, 0.8f, 0.9f, 1f, 0.9f, 0.9f, 1f, 1f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        table.addHeaderCell(createDerivativeHeaderCell("Contract Description"));
        table.addHeaderCell(createDerivativeHeaderCell("Buy[B]/\nSell[S]/\nBF/CF"));
        table.addHeaderCell(createDerivativeHeaderCell("Quantity\n(In Foreign\nCurrency)"));
        table.addHeaderCell(createDerivativeHeaderCell("WAP per unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("WAP per unit\nAfter\nBrokerage(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("Brokerage\nPer Unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("Closing Rate\nper Unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("Net Total\n(Before Levies)"));
        table.addHeaderCell(createDerivativeHeaderCell("Remarks"));

        for (FOHeaderTypeModel fo : foHeaderTypeList) {
            String contractDesc = safe(fo.getSecurityname());
            if (contractDesc.isEmpty()) {
                contractDesc = safe(fo.getSymbol());
            }

            String buySell = safe(fo.getBuySell()).toUpperCase();
            if ("BUY".equals(buySell)) {
                buySell = "B";
            } else if ("SELL".equals(buySell)) {
                buySell = "S";
            }

            int quantity = parseInt(fo.getQuantity());
            double wapPerUnit = parseDouble(fo.getPrice());
            double brokeragePerUnit = parseDouble(fo.getBrokeragePerUnit());
            if (brokeragePerUnit == 0.0 && quantity > 0) {
                brokeragePerUnit = parseDouble(fo.getBrokerage()) / quantity;
            }

            double wapAfterBrokerage = wapPerUnit;
            if ("B".equals(buySell)) {
                wapAfterBrokerage = wapPerUnit + brokeragePerUnit;
            } else if ("S".equals(buySell)) {
                wapAfterBrokerage = wapPerUnit - brokeragePerUnit;
            }

            double closingRate = parseDouble(fo.getClosingRate());
            double netTotal = quantity * wapPerUnit;
            if ("B".equals(buySell)) {
                netTotal = -(netTotal + parseDouble(fo.getBrokerage()));
            } else if ("S".equals(buySell)) {
                netTotal = netTotal - parseDouble(fo.getBrokerage());
            }

            String remarks = safe(fo.getRemarks());

            table.addCell(createDerivativeDataCell(contractDesc, TextAlignment.LEFT));
            table.addCell(createDerivativeDataCell(buySell, TextAlignment.CENTER));
            table.addCell(createDerivativeDataCell(String.valueOf(quantity), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal(wapPerUnit), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal(wapAfterBrokerage), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal(brokeragePerUnit), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal(closingRate), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal(netTotal), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(remarks, TextAlignment.LEFT));
        }

        document.add(table);

        Paragraph footnote = new Paragraph("* Exchange-wise details of orders and trades are provided in separate annexure.")
                .setFontSize(FONT_SIZE_TINY)
                .setItalic()
                .setMarginTop(2);  // ✅ FIXED: Reduced from 3
        document.add(footnote);
    }

    private Cell createDerivativeHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(FONT_SIZE_SMALL)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createDerivativeDataCell(String content, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(content)
                        .setFontSize(FONT_SIZE_TINY)
                        .setTextAlignment(alignment))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EXCHANGE SUMMARY - ✅ FIXED: 1pt borders, correct spacing
    // ═══════════════════════════════════════════════════════════════════════

    private void addExchangeSummary(Document document) {
        if (footerList.isEmpty()) {
            System.out.println("       → No footer/charges data found, skipping exchange summary");
            return;
        }

        float[] columnWidths = {1f, 0.7f, 0.5f, 0.5f, 0.5f, 0.5f, 0.4f, 0.7f, 0.5f, 0.6f, 0.5f, 0.6f, 1f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);
        table.setMarginTop(8);  // ✅ FIXED: Reduced from 10

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
        table.addHeaderCell(createHeaderCellGreen("Net Amount"));
        table.addHeaderCell(createHeaderCellGreen("Receivable By\nClient/(Payable\nby Client)"));

        double totalPayInOut = 0.0;
        double totalSTT = 0.0;
        double totalSGST = 0.0;
        double totalCGST = 0.0;
        double totalIGST = 0.0;
        double totalTDS = 0.0;
        double totalExchangeCharges = 0.0;
        double totalSEBIFees = 0.0;
        double totalCess = 0.0;
        double totalStampDuty = 0.0;
        double totalNetAmount = 0.0;

        for (FooterModelV2 footer : footerList) {
            String exchangeSegment = safe(footer.getExchange()) + safe(footer.getSegment());

            double payInOut = parseDouble(footer.getPayinout());
            double stt = parseDouble(footer.getStt());
            double sgst = parseDouble(footer.getSgst());
            double cgst = parseDouble(footer.getCgst());
            double igst = parseDouble(footer.getIgst());
            double tds = parseDouble(footer.getTds());
            double exchangeCharges = parseDouble(footer.getExchangecharge());
            double sebiFees = parseDouble(footer.getSebifee());
            double cess = parseDouble(footer.getCess());
            double stampDuty = parseDouble(footer.getStampduty());

            double netAmount = payInOut - stt - sgst - cgst - igst - tds - exchangeCharges - sebiFees - cess - stampDuty;

            totalPayInOut += payInOut;
            totalSTT += stt;
            totalSGST += sgst;
            totalCGST += cgst;
            totalIGST += igst;
            totalTDS += tds;
            totalExchangeCharges += exchangeCharges;
            totalSEBIFees += sebiFees;
            totalCess += cess;
            totalStampDuty += stampDuty;
            totalNetAmount += netAmount;

            table.addCell(createCell(exchangeSegment, TextAlignment.LEFT, false));
            table.addCell(createCell(formatDecimal(payInOut), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(stt), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(sgst), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(cgst), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(igst), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(tds), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(exchangeCharges), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(sebiFees), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(cess), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(stampDuty), TextAlignment.RIGHT, false));
            table.addCell(createCell("", TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(netAmount), TextAlignment.RIGHT, false));
        }

        Cell totalCell = createCell("TOTAL(NET)", TextAlignment.LEFT, true);
        table.addCell(totalCell);

        table.addCell(createCellGreenBg(formatDecimal(totalPayInOut), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalSTT), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalSGST), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalCGST), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalIGST), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalTDS), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalExchangeCharges), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalSEBIFees), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalCess), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalStampDuty), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg("", TextAlignment.RIGHT, true));

        Cell finalAmountCell = createCell(formatDecimal(totalNetAmount) + " *", TextAlignment.RIGHT, true);
        table.addCell(finalAmountCell);

        document.add(table);

        double gstBase = totalExchangeCharges + totalSEBIFees;

        Paragraph footnote1 = new Paragraph("** SGST: - State GST; CGST:-Central GST; IGST:-Integrated GST. " +
                "GST is calculated on Brokerage, Exchange Transaction Charges and SEBI Fee. (18% of Rs." + formatDecimal(gstBase) + ")")
                .setFontSize(FONT_SIZE_TINY)
                .setMarginTop(2);
        document.add(footnote1);

        Paragraph footnote2 = new Paragraph("# Brokerage shown is per unit in the case of Equities and total " +
                "brokerage for the particular transaction in the case of F&O and Currency trades.")
                .setFontSize(FONT_SIZE_TINY);
        document.add(footnote2);

        Paragraph footnote3 = new Paragraph("* Any other charges (DIS charge/AMC/Overdue charges etc.) which " +
                "are due to us will be debited from the net amount.")
                .setFontSize(FONT_SIZE_TINY);
        document.add(footnote3);
    }

    private Cell createHeaderCellGreen(String content) {
        return new Cell()
                .add(new Paragraph(content)
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createCellGreenBg(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content)
                .setFontSize(FONT_SIZE_TINY)
                .setTextAlignment(alignment);
        if (bold) {
            p.setBold();
        }
        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content)
                .setFontSize(FONT_SIZE_TINY)
                .setTextAlignment(alignment);

        if (bold) {
            p.setBold();
        }

        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FOOTER NOTES, SIGNATURE, DISCLAIMER - ✅ FIXED: spacing
    // ═══════════════════════════════════════════════════════════════════════

    private void addFooterNotes(Document document) {
        Paragraph notes = new Paragraph()
                .add(new Text("Transactions mentioned in this contract note cum bill shall be governed and subject to " +
                        "the Rules, Bye-laws, Regulations and Circulars of the respective Exchanges on which trades have " +
                        "been executed and Securities and Exchange Board of India issued from time to time. It shall also " +
                        "be subject to the relevant Acts, Rules, Regulations, Directives, Notifications, Guidelines " +
                        "(including GST Laws) & Circulars issued by SEBI / Government of India / State Governments and " +
                        "Union Territory Governments issued from time to time. The Exchanges provide Complaint Resolution, " +
                        "Arbitration and Appellate arbitration facilities at the Regional Arbitration Centres (RAC). The " +
                        "client may approach its nearest centre, details of which are available on respective Exchange's website. " +
                        "Please visit www.nseindia.com for NSE, www.bseindia.com for BSE and www.msei.in for MSEI.")
                        .setFontSize(FONT_SIZE_TINY))
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(8);  // ✅ FIXED: Reduced from 10
        document.add(notes);
    }

    private void addSignature(Document document) {
        float[] columnWidths = {1, 1};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginTop(8);  // ✅ FIXED: Reduced from 10
        table.setBorder(Border.NO_BORDER);

        String tradeDate = safe(customer.getTransactionDate());
        String place = "PALARIVATTOM";

        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Date: " + tradeDate)
                        .setFontSize(FONT_SIZE_SMALL))
                .add(new Paragraph("Place: " + place)
                        .setFontSize(FONT_SIZE_SMALL));

        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Yours Faithfully,")
                        .setFontSize(FONT_SIZE_SMALL)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("GEOJIT INVESTMENTS LTD")
                        .setFontSize(FONT_SIZE_SMALL)
                        .setBold()
                        .setFontColor(GEOJIT_DARK_GREEN)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("(PAN : AAKCG3453A, GSTIN : 32AAKCG3453A1Z4)")
                        .setFontSize(FONT_SIZE_TINY)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("\nDescription of Service : STOCK BROKER")
                        .setFontSize(FONT_SIZE_TINY)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("Service Account Code(SAC) : 997152")
                        .setFontSize(FONT_SIZE_TINY)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("\n\nJohny Varghese")
                        .setFontSize(FONT_SIZE_SMALL)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("(Name & Signature of Authorised Signatory)")
                        .setFontSize(FONT_SIZE_TINY)
                        .setTextAlignment(TextAlignment.RIGHT));

        table.addCell(leftCell);
        table.addCell(rightCell);

        document.add(table);
    }

    private void addDisclaimer(Document document) {
        Paragraph disclaimer = new Paragraph()
                .add(new Text("Disclaimer: ").setBold())
                .add(new Text("Purchase of REs (Rights Entitlements)only gives right to participate in the ongoing " +
                        "Rights Issue of the concerned company. REs which are neither subscribed by making an application " +
                        "with requisite application money nor renounced on or before the Issue Closing Date shall lapse and " +
                        "shall be extinguished after the Issue Closing Date."))
                .setFontSize(FONT_SIZE_TINY)
                .setMarginTop(8)  // ✅ FIXED: Reduced from 10
                .setTextAlignment(TextAlignment.JUSTIFIED);
        document.add(disclaimer);

        Paragraph endOfContract = new Paragraph(" * End Of Contract *")
                .setFontSize(FONT_SIZE_SMALL)
                .setBold()
                .setFontColor(GEOJIT_DARK_GREEN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8);  // ✅ FIXED: Reduced from 10
        document.add(endOfContract);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAGE 2: TRADE DETAILS ANNEXURE - ✅ FIXED: All borders and spacing
    // ═══════════════════════════════════════════════════════════════════════

    private void addTradeDetailsAnnexure(Document document) {
        if (oHeaderTypeList.isEmpty()) {
            System.out.println("       → No order/trade details found, skipping annexure");
            return;
        }

        document.add(new Paragraph("\n").setMarginTop(15));

        Paragraph title = new Paragraph("Annexure - Net Obligation Details")
                .setFont(calibriBold)
                .setFontSize(FONT_SIZE_SUBTITLE)
                .setFontColor(GEOJIT_DARK_GREEN)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(8);
        document.add(title);

        Paragraph clearingCorp = new Paragraph("Name of the Clearing Corporation & Segment : NSE Clearing Limited")
                .setFontSize(FONT_SIZE_NORMAL)
                .setBold()
                .setMarginBottom(6);
        document.add(clearingCorp);

        float[] columnWidths = {0.5f, 2f, 1f, 0.8f, 1f, 0.8f, 1f, 0.8f, 1f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        table.addHeaderCell(createTradeDetailHeaderCell("Sl.No"));
        table.addHeaderCell(createTradeDetailHeaderCell("Security\nDescription"));
        table.addHeaderCell(createTradeDetailHeaderCell("Segment"));
        table.addHeaderCell(createTradeDetailHeaderCell("Buy\nQty"));
        table.addHeaderCell(createTradeDetailHeaderCell("Buy\nRate"));
        table.addHeaderCell(createTradeDetailHeaderCell("Sell\nQty"));
        table.addHeaderCell(createTradeDetailHeaderCell("Sell\nRate"));
        table.addHeaderCell(createTradeDetailHeaderCell("Net\nQty"));
        table.addHeaderCell(createTradeDetailHeaderCell("Net\nRate"));
        table.addHeaderCell(createTradeDetailHeaderCell("Amount"));

        int slNo = 1;
        double totalAmount = 0.0;

        for (OHeaderTypeModel o : oHeaderTypeList) {
            String securityDesc = safe(o.getSecurityDescription());
            String segment = safe(o.getSegment());
            String buyQty = safe(o.getBuyQty());
            String buyRate = safe(o.getBuyRate());
            String sellQty = safe(o.getSellQty());
            String sellRate = safe(o.getSellRate());
            String netQty = safe(o.getNetQty());
            String netRate = safe(o.getNetRate());
            String amount = safe(o.getAmount());

            totalAmount += parseDouble(amount);

            table.addCell(createTradeDetailDataCell(String.valueOf(slNo++), TextAlignment.CENTER));
            table.addCell(createTradeDetailDataCell(securityDesc, TextAlignment.LEFT));
            table.addCell(createTradeDetailDataCell(segment, TextAlignment.CENTER));
            table.addCell(createTradeDetailDataCell(buyQty, TextAlignment.RIGHT));
            table.addCell(createTradeDetailDataCell(formatDecimal(buyRate), TextAlignment.RIGHT));
            table.addCell(createTradeDetailDataCell(sellQty, TextAlignment.RIGHT));
            table.addCell(createTradeDetailDataCell(formatDecimal(sellRate), TextAlignment.RIGHT));
            table.addCell(createTradeDetailDataCell(netQty, TextAlignment.RIGHT));
            table.addCell(createTradeDetailDataCell(formatDecimal(netRate), TextAlignment.RIGHT));
            table.addCell(createTradeDetailDataCell(formatDecimal(amount), TextAlignment.RIGHT));
        }

        Cell totalLabelCell = new Cell(1, 9)
                .add(new Paragraph("Total")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3);
        table.addCell(totalLabelCell);

        Cell totalAmountCell = new Cell()
                .add(new Paragraph(formatDecimal(totalAmount))
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3);
        table.addCell(totalAmountCell);

        document.add(table);

        addNetObligationSection(document);
        addSTTSection(document);
    }

    private Cell createTradeDetailHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createTradeDetailDataCell(String content, TextAlignment alignment) {
        return createTradeDetailDataCell(content, alignment, false);
    }

    private Cell createTradeDetailDataCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content)
                .setFontSize(FONT_SIZE_TINY)
                .setTextAlignment(alignment);

        if (bold) {
            p.setBold();
        }

        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))  // ✅ FIXED: 1pt
                .setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Continue with remaining methods in next section...
    //  (Due to character limits, see continuation below)
    // ═══════════════════════════════════════════════════════════════════════
        /**
         * ═══════════════════════════════════════════════════════════════════════════
         *  NET OBLIGATION SECTION - DYNAMIC from SCapitalHeaderTypeModel
         * ═══════════════════════════════════════════════════════════════════════════
         */
        // ═══════════════════════════════════════════════════════════════════════════
        //  PART 3 - FINAL SECTION OF DynamicValuePDF.java
        //  Paste this after Part 2 (inside the DynamicValuePDF class, before closing brace)
        // ═══════════════════════════════════════════════════════════════════════════

        // ═══════════════════════════════════════════════════════════════════════
        //  NET OBLIGATION SECTION - ✅ FIXED: All borders
        // ═══════════════════════════════════════════════════════════════════════

        private void addNetObligationSection(Document document) {
            if (sCapitalHeaderTypeList.isEmpty()) {
                System.out.println("       → No capital market data found");
                return;
            }

            Paragraph title = new Paragraph("Net Obligation (Equity Market)")
                    .setFont(calibriBold)
                    .setFontSize(FONT_SIZE_NORMAL)
                    .setFontColor(GEOJIT_DARK_GREEN)
                    .setMarginTop(8)
                    .setMarginBottom(4);
            document.add(title);

            float[] columnWidths = {0.5f, 2.5f, 0.8f, 0.7f, 0.9f, 0.7f, 0.9f, 0.7f, 0.9f, 1f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setFontSize(FONT_SIZE_TINY);

            Cell slNoHeader = new Cell(2, 1)
                    .add(new Paragraph("Sl.No")
                            .setFontSize(FONT_SIZE_SMALL)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(slNoHeader);

            Cell securityHeader = new Cell(2, 1)
                    .add(new Paragraph("Security")
                            .setFontSize(FONT_SIZE_SMALL)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(securityHeader);

            Cell segmentHeader = new Cell(2, 1)
                    .add(new Paragraph("Segment")
                            .setFontSize(FONT_SIZE_SMALL)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(segmentHeader);

            Cell boughtHeader = new Cell(1, 2)
                    .add(new Paragraph("Bought")
                            .setFontSize(FONT_SIZE_SMALL)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(boughtHeader);

            Cell soldHeader = new Cell(1, 2)
                    .add(new Paragraph("Sold")
                            .setFontSize(FONT_SIZE_SMALL)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(soldHeader);

            Cell netObligationHeader = new Cell(1, 2)
                    .add(new Paragraph("Net Obligation")
                            .setFontSize(FONT_SIZE_SMALL)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(netObligationHeader);

            Cell amountHeader = new Cell(2, 1)
                    .add(new Paragraph("Amount")
                            .setFontSize(FONT_SIZE_SMALL)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(amountHeader);

            table.addHeaderCell(createNetObligationHeaderCell("Quantity"));
            table.addHeaderCell(createNetObligationHeaderCell("Rate"));
            table.addHeaderCell(createNetObligationHeaderCell("Quantity"));
            table.addHeaderCell(createNetObligationHeaderCell("Rate"));
            table.addHeaderCell(createNetObligationHeaderCell("Quantity"));
            table.addHeaderCell(createNetObligationHeaderCell("Rate"));

            double totalAmount = 0.0;
            int slNo = 1;

            for (SCapitalHeaderTypeModel sc : sCapitalHeaderTypeList) {
                String securityName = safe(sc.getSecurityDescription());
                String segment = safe(sc.getSegment());

                if ("CAPITAL".equalsIgnoreCase(segment)) {
                    segment = "EN";
                }

                int boughtQty = parseInt(sc.getBuyQty());
                double boughtRate = parseDouble(sc.getBuyWap());
                int soldQty = parseInt(sc.getSellQty());
                double soldRate = parseDouble(sc.getSellWap());
                int netQty = parseInt(sc.getNetQty());
                double netRate = parseDouble(sc.getNetObligationIsin());
                double buyValue = parseDouble(sc.getBuyValue());
                double sellValue = parseDouble(sc.getSellValue());
                double amount = buyValue - sellValue;

                totalAmount += amount;

                table.addCell(createNetObligationDataCell(String.valueOf(slNo++), TextAlignment.CENTER));
                table.addCell(createNetObligationDataCell(securityName, TextAlignment.LEFT));
                table.addCell(createNetObligationDataCell(segment, TextAlignment.CENTER));
                table.addCell(createNetObligationDataCell(String.valueOf(boughtQty), TextAlignment.RIGHT));
                table.addCell(createNetObligationDataCell(formatDecimal(boughtRate), TextAlignment.RIGHT));
                table.addCell(createNetObligationDataCell(String.valueOf(soldQty), TextAlignment.RIGHT));
                table.addCell(createNetObligationDataCell(formatDecimal(soldRate), TextAlignment.RIGHT));
                table.addCell(createNetObligationDataCell(String.valueOf(netQty), TextAlignment.RIGHT));
                table.addCell(createNetObligationDataCell(formatDecimal(netRate), TextAlignment.RIGHT));
                table.addCell(createNetObligationDataCell(formatDecimal(amount), TextAlignment.RIGHT));
            }

            Cell totalLabelCell = new Cell(1, 9)
                    .add(new Paragraph("Total")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3);
            table.addCell(totalLabelCell);

            Cell totalAmountCell = new Cell()
                    .add(new Paragraph(formatDecimal(totalAmount))
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3);
            table.addCell(totalAmountCell);

            document.add(table);

            double sttAmount = 0.0;
            for (OHeaderTypeModel o : oHeaderTypeList) {
                sttAmount += parseDouble(o.getStt());
            }

            double netAmount = totalAmount - sttAmount;

            Table summaryTable = new Table(2);
            summaryTable.setWidth(UnitValue.createPercentValue(30));
            summaryTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            summaryTable.setMarginTop(4);

            summaryTable.addCell(createSummaryCell("Securities Transaction Tax", TextAlignment.LEFT, false));
            summaryTable.addCell(createSummaryCell(formatDecimal(sttAmount), TextAlignment.RIGHT, false));
            summaryTable.addCell(createSummaryCell("Net Amount", TextAlignment.LEFT, true));
            summaryTable.addCell(createSummaryCell(formatDecimal(netAmount), TextAlignment.RIGHT, true));

            document.add(summaryTable);

            addScripSummary(document);
        }

    private Cell createNetObligationHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createNetObligationDataCell(String content, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(content)
                        .setFontSize(FONT_SIZE_TINY)
                        .setTextAlignment(alignment))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell createSummaryCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content)
                .setFontSize(FONT_SIZE_SMALL)
                .setTextAlignment(alignment);

        if (bold) {
            p.setBold();
        }

        return new Cell()
                .add(p)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCRIP SUMMARY - ✅ FIXED
    // ═══════════════════════════════════════════════════════════════════════

    private void addScripSummary(Document document) {
        if (ssHeaderTypeModels.isEmpty()) {
            System.out.println("       → No scrip summary data found");
            return;
        }

        Paragraph title = new Paragraph("Scrip-Summary")
                .setFontSize(FONT_SIZE_SMALL)
                .setBold()
                .setFontColor(GEOJIT_DARK_GREEN)
                .setMarginTop(8)
                .setMarginBottom(4);
        document.add(title);

        float[] columnWidths = {2f, 0.5f, 0.7f, 0.9f, 1f, 0.9f, 1f, 0.9f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        table.addHeaderCell(createHeaderCellGreen("Security Description"));
        table.addHeaderCell(createHeaderCellGreen("B/S"));
        table.addHeaderCell(createHeaderCellGreen("Quantity"));
        table.addHeaderCell(createHeaderCellGreen("Gross Rate Per\nSecurity(Rs)"));
        table.addHeaderCell(createHeaderCellGreen("Gross Total(Rs)"));
        table.addHeaderCell(createHeaderCellGreen("Gross BrokeragePer\nSecurity(Rs)"));
        table.addHeaderCell(createHeaderCellGreen("Brokerage(Total)(\nRs)"));
        table.addHeaderCell(createHeaderCellGreen("Net Rate(Rs)"));
        table.addHeaderCell(createHeaderCellGreen("Net Total Amount(Rs)"));

        for (SSHeaderTypeModel ss : ssHeaderTypeModels) {
            String securityDesc = safe(ss.getSecurityDescription());
            String buySell = safe(ss.getTradeType()).toUpperCase();
            if ("BUY".equals(buySell)) buySell = "B";
            if ("SELL".equals(buySell)) buySell = "S";

            int quantity = parseInt(ss.getTradeQty());
            double grossRate = parseDouble(ss.getGrossRate());
            double grossTotal = parseDouble(ss.getGrossTotal());
            double grossBrokeragePerSecurity = parseDouble(ss.getGrossBrokerage());
            double brokerageTotal = parseDouble(ss.getBrokerage());
            double netRate = parseDouble(ss.getNetRate());
            double netTotal = parseDouble(ss.getNetAmount());

            table.addCell(createCell(securityDesc, TextAlignment.LEFT, false));
            table.addCell(createCell(buySell, TextAlignment.CENTER, false));
            table.addCell(createCell(String.valueOf(quantity), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(grossRate), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(grossTotal), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(grossBrokeragePerSecurity), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(brokerageTotal), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(netRate), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(netTotal), TextAlignment.RIGHT, false));
        }

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STT SECTION - ✅ FIXED
    // ═══════════════════════════════════════════════════════════════════════

    private void addSTTSection(Document document) {
        if (sttHeaderTypeModels.isEmpty()) {
            System.out.println("       → No STT data found");
            return;
        }

        Map<String, List<STTHeaderTypeModel>> byExchange = safeGroupBy(
                sttHeaderTypeModels,
                stt -> safe(stt.getExchange())
        );

        if (byExchange.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<STTHeaderTypeModel>> entry : byExchange.entrySet()) {
            String exchange = entry.getKey();
            List<STTHeaderTypeModel> sttList = entry.getValue();

            String brokerCode = "0328";
            if (exchange.contains("NSE")) {
                brokerCode = "13372";
            }

            Paragraph title = new Paragraph("Statement Of Securities Transaction Tax (Derivatives) - " + exchange)
                    .setFontSize(FONT_SIZE_SMALL)
                    .setBold()
                    .setFontColor(GEOJIT_DARK_GREEN)
                    .setMarginTop(8)
                    .setMarginBottom(4);
            document.add(title);

            Paragraph exchangeHeader = new Paragraph("Name Of Exchange & Segment : " + exchange +
                    " | Broker Code : " + brokerCode)
                    .setFontSize(FONT_SIZE_SMALL)
                    .setBold()
                    .setFontColor(GEOJIT_DARK_GREEN)
                    .setMarginBottom(4);
            document.add(exchangeHeader);

            float[] columnWidths = {0.5f, 2.5f, 0.5f, 1f, 1f, 1f, 1f, 1f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setFontSize(FONT_SIZE_TINY);

            Cell slNoCell = new Cell(2, 1)
                    .add(new Paragraph("Sl.No")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(slNoCell);

            Cell securityCell = new Cell(2, 1)
                    .add(new Paragraph("Security")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(securityCell);

            Cell expiryCell = new Cell(2, 1)
                    .add(new Paragraph("Expiry Date")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(expiryCell);

            Cell futuresHeader = new Cell(1, 2)
                    .add(new Paragraph("Value of Transactions Futures")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(futuresHeader);

            Cell optionsHeader = new Cell(1, 2)
                    .add(new Paragraph("Value of Transactions Options")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(optionsHeader);

            Cell totalSTTCell = new Cell(2, 1)
                    .add(new Paragraph("Total STT(Rs.)")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.WHITE)

                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(totalSTTCell);

            table.addHeaderCell(createHeaderCellGreen("Sale"));
            table.addHeaderCell(createHeaderCellGreen("STT"));
            table.addHeaderCell(createHeaderCellGreen("Sale"));
            table.addHeaderCell(createHeaderCellGreen("STT"));

            double totalSTT = 0.0;
            int slNo = 1;

            for (STTHeaderTypeModel stt : sttList) {
                String securityName = safe(stt.getSecurityDescription());
                String expiryDate = safe(stt.getExpDate());
                double futureSale = parseDouble(stt.getFutureSale());
                double futureStt = parseDouble(stt.getFutureStt());
                double optionSale = parseDouble(stt.getOptionSale());
                double optionStt = parseDouble(stt.getOptionStt());
                double rowTotal = futureStt + optionStt;
                totalSTT += rowTotal;

                table.addCell(createCell(String.valueOf(slNo++), TextAlignment.CENTER, false));
                table.addCell(createCell(securityName, TextAlignment.LEFT, false));
                table.addCell(createCell(expiryDate, TextAlignment.CENTER, false));
                table.addCell(createCell(formatDecimal(futureSale), TextAlignment.RIGHT, false));
                table.addCell(createCell(formatDecimal(futureStt), TextAlignment.RIGHT, false));
                table.addCell(createCell(formatDecimal(optionSale), TextAlignment.RIGHT, false));
                table.addCell(createCell(formatDecimal(optionStt), TextAlignment.RIGHT, false));
                table.addCell(createCell(formatDecimal(rowTotal), TextAlignment.RIGHT, false));
            }

            Cell totalLabelCell = new Cell(1, 7)
                    .add(new Paragraph("Total(Rounded to nearest Rupee)")
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(2);
            table.addCell(totalLabelCell);

            Cell totalValueCell = new Cell(1, 1)
                    .add(new Paragraph(formatDecimal(totalSTT))
                            .setFontSize(FONT_SIZE_TINY)
                            .setBold()
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(2);
            table.addCell(totalValueCell);

            document.add(table);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAGE 3: MARGIN STATEMENT - ✅ FIXED
    // ═══════════════════════════════════════════════════════════════════════

    private void addMarginStatement(Document document) {
        if (mHeaderTypeModels.isEmpty()) {
            System.out.println("       → No margin data found");
            return;
        }

        document.add(new Paragraph("\n").setMarginTop(15));

        Paragraph title = new Paragraph("Daily Margin Statement")
                .setFont(calibriBold)
                .setFontSize(FONT_SIZE_SMALL)
                .setFontColor(GEOJIT_DARK_GREEN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4);
        document.add(title);

        Set<String> exchanges = mHeaderTypeModels.stream()
                .map(m -> safe(m.getExchange()))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String exchangeList = String.join(", ", exchanges);
        if (exchangeList.isEmpty()) {
            exchangeList = "NSE, BSE, MCX'SX";
        }

        Paragraph exchange = new Paragraph("Exchange : " + exchangeList)
                .setFont(calibriBold)
                .setFontSize(FONT_SIZE_SMALL)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(8);
        document.add(exchange);

        float[] columnWidths = {0.6f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        Cell segCell = new Cell(2, 1)
                .add(new Paragraph("Seg")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(segCell);

        Cell tradeDayCell = new Cell(2, 1)
                .add(new Paragraph("Trade Day")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(tradeDayCell);

        Cell marginsAvailableHeader = new Cell(1, 6)
                .add(new Paragraph("Margins available till T day")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(marginsAvailableHeader);

        Cell marginReqHeader = new Cell(1, 4)
                .add(new Paragraph("Margin/ Consolidated Crystallized Obligation / MTM required by Exchange/CC end of T\n& T+1 day respectively")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(marginReqHeader);

        Cell excessCell = new Cell(2, 1)
                .add(new Paragraph("Excess / Shortfall\nw.r.t. Requirement\nby Exchange / CC")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(excessCell);

        Cell additionalCell = new Cell(2, 1)
                .add(new Paragraph("Additional Margin\nrequired by member\nas per RMS")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(additionalCell);

        Cell marginStatusCell = new Cell(2, 1)
                .add(new Paragraph("MarginStatus\n(Balance with\nMember\n/Due\nfromclient)")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)

                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(marginStatusCell);

        table.addHeaderCell(createHeaderCellGreen("Funds"));
        table.addHeaderCell(createHeaderCellGreen("Value of Securities\n(after haircut)"));
        table.addHeaderCell(createHeaderCellGreen("Value of margin\npledge Securities\n(after haircut)"));
        table.addHeaderCell(createHeaderCellGreen("Bank Guarantees /\nFDR"));
        table.addHeaderCell(createHeaderCellGreen("Any other approved\nform of Margins*"));
        table.addHeaderCell(createHeaderCellGreen("Total Margins\nAvailable (E)"));
        table.addHeaderCell(createHeaderCellGreen("Total upfront Margin"));
        table.addHeaderCell(createHeaderCellGreen("Consolidated\nCrystallized\nObligation / MTM"));
        table.addHeaderCell(createHeaderCellGreen("Delivery Margin"));
        table.addHeaderCell(createHeaderCellGreen("Total Requirement"));

        double totalFunds = 0.0;
        double totalSecurities = 0.0;
        double totalMarginPledge = 0.0;
        double totalBankGuarantees = 0.0;
        double totalOther = 0.0;
        double totalMarginsAvailable = 0.0;
        double totalUpfrontMargin = 0.0;
        double totalConsolidated = 0.0;
        double totalDeliveryMargin = 0.0;
        double totalRequirement = 0.0;
        double totalExcessShortfall = 0.0;
        double totalAdditionalMargin = 0.0;
        double totalMarginStatus = 0.0;

        for (MHeaderTypeModel m : mHeaderTypeModels) {
            String segment = safe(m.getSegment());
            String tradeDay = safe(m.getTradeday());

            double funds = parseDouble(m.getFunds());
            double securities = parseDouble(m.getSecurities());
            double marginPledge = parseDouble(m.getMarginpledgesecurities());
            double bankGuarantees = parseDouble(m.getBankguarantees());
            double otherApproved = parseDouble(m.getOtherapproved());
            double marginsAvailable = parseDouble(m.getTotalmarginsavailable());
            double upfrontMargin = parseDouble(m.getTotalupfrontmargin());
            double consolidated = parseDouble(m.getConsolidatedcrystallized());
            double deliveryMargin = parseDouble(m.getDeliverymargin());
            double requirement = parseDouble(m.getTotalrequirement());
            double excessShortfall = parseDouble(m.getExcessshortfall());
            double additionalMargin = parseDouble(m.getAdditionalmargin());
            double marginStatus = parseDouble(m.getMarginstatus());

            totalFunds += funds;
            totalSecurities += securities;
            totalMarginPledge += marginPledge;
            totalBankGuarantees += bankGuarantees;
            totalOther += otherApproved;
            totalMarginsAvailable += marginsAvailable;
            totalUpfrontMargin += upfrontMargin;
            totalConsolidated += consolidated;
            totalDeliveryMargin += deliveryMargin;
            totalRequirement += requirement;
            totalExcessShortfall += excessShortfall;
            totalAdditionalMargin += additionalMargin;
            totalMarginStatus += marginStatus;

            table.addCell(createCell(segment, TextAlignment.LEFT, false));
            table.addCell(createCell(tradeDay, TextAlignment.CENTER, false));
            table.addCell(createCell(formatDecimal(funds), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(securities), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(marginPledge), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(bankGuarantees), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(otherApproved), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(marginsAvailable), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(upfrontMargin), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(consolidated), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(deliveryMargin), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(requirement), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(excessShortfall), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(additionalMargin), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(marginStatus), TextAlignment.RIGHT, false));
        }

        Cell summaryCell = new Cell(1, 2)
                .add(new Paragraph("Summary of all Exchanges")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setTextAlignment(TextAlignment.LEFT))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(2);
        table.addCell(summaryCell);

        table.addCell(createCellGreenBg(formatDecimal(totalFunds), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalSecurities), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalMarginPledge), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalBankGuarantees), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalOther), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalMarginsAvailable), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalUpfrontMargin), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalConsolidated), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalDeliveryMargin), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalRequirement), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalExcessShortfall), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalAdditionalMargin), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalMarginStatus), TextAlignment.RIGHT, true));

        document.add(table);

        addMarginStatementNotes(document);
    }

    private void addMarginStatementNotes(Document document) {
        Paragraph notes = new Paragraph()
                .add(new Text("* approved form as may be specified by the Exchange/Clearing Corporation /NSCCL/MCX-SXCCL from time to time. ")
                        .setFontSize(FONT_SIZE_TINY))
                .add(new Text("# Balance margin available for the day, pending bills will be adjusted with the available balance.")
                        .setFontSize(FONT_SIZE_TINY))
                .setMarginTop(5);
        document.add(notes);

        Paragraph note1 = new Paragraph("1) Settlements not due : 2) For margin reporting, additional T&C signed holdings is considered.")
                .setFontSize(FONT_SIZE_TINY);
        document.add(note1);

        Paragraph note2 = new Paragraph("For collateral accounting in derivative segments, only the pledge eligible shares as per NSE will be considered. " +
                "Buying Power in FLIP is updated based on the scrip margin specified by GEOJIT INVESTMENTS LTD.")
                .setFontSize(FONT_SIZE_TINY);
        document.add(note2);

        String tradeDate = safe(customer.getTransactionDate());

        Paragraph note3 = new Paragraph("3) The fund balance is arrived at without considering the funds in Margin Trading (MTF) and pending settlements. " +
                "4) Provisional Penalty for the trade date (" + tradeDate + ") is 0 /-")
                .setFontSize(FONT_SIZE_TINY);
        document.add(note3);

        double nseFO_MTM = 0.0;
        double nseCDS_MTM = 0.0;
        double mcxCDS_MTM = 0.0;
        double bseFO_MTM = 0.0;
        double bseCDS_MTM = 0.0;
        double mcx_MTM = 0.0;
        double ncdex_MTM = 0.0;
        double icex_MTM = 0.0;

        for (MHeaderTypeModel m : mHeaderTypeModels) {
            String segment = safe(m.getSegment()).toUpperCase();
            double mtm = parseDouble(m.getConsolidatedcrystallized());

            if (segment.contains("NSEFO")) {
                nseFO_MTM += mtm;
            } else if (segment.contains("NSECDS")) {
                nseCDS_MTM += mtm;
            } else if (segment.contains("BSEFO")) {
                bseFO_MTM += mtm;
            } else if (segment.contains("BSECDS")) {
                bseCDS_MTM += mtm;
            } else if (segment.contains("MCXCDS") || segment.contains("MCX'SX")) {
                mcxCDS_MTM += mtm;
            } else if (segment.contains("MCX")) {
                mcx_MTM += mtm;
            } else if (segment.contains("NCDEX")) {
                ncdex_MTM += mtm;
            } else if (segment.contains("ICEX")) {
                icex_MTM += mtm;
            }
        }

        Paragraph note4 = new Paragraph("5) MTM/Premium for the trade date (" + tradeDate + ") - " +
                "NSE FO = " + formatDecimal(nseFO_MTM) + " /-, " +
                "NSE CDS = " + formatDecimal(nseCDS_MTM) + " /-, " +
                "MCX CDS = " + formatDecimal(mcxCDS_MTM) + " /-, " +
                "BSE FO = " + formatDecimal(bseFO_MTM) + " /-, " +
                "BSE CDS = " + formatDecimal(bseCDS_MTM) + " /-, " +
                "MCX = " + formatDecimal(mcx_MTM) + " /-, " +
                "NCDEX = " + formatDecimal(ncdex_MTM) + " /-, " +
                "ICEX = " + formatDecimal(icex_MTM) + " /- . " +
                "6) All the figures are in Rupee. \"-Ve indicates debit balance, +Ve indicates credit balance\"")
                .setFontSize(FONT_SIZE_TINY);
        document.add(note4);

        Paragraph note5 = new Paragraph("6)For the purchase of Shares of BSE Ltd/CDSL., you are requested to comply with the prescribed SECC regulation 19 and 20 and related circulars.")
                .setFontSize(FONT_SIZE_TINY);
        document.add(note5);

        Paragraph note6 = new Paragraph(" For more details,\nhttp://www.geojit.com/equity-products/instructions")
                .setFontSize(FONT_SIZE_TINY);
        document.add(note6);

        Paragraph note7 = new Paragraph("7)In case of trades/positions in commodity Exchanges, new margin statement will be issued separately.")
                .setFontSize(FONT_SIZE_TINY);
        document.add(note7);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MARGIN PLEDGE SECURITIES - ✅ FIXED
    // ═══════════════════════════════════════════════════════════════════════

    private void addMarginPledgeSecurities(Document document) {
        if (pHeaderTypeModels.isEmpty()) {
            System.out.println("       → No pledge securities data found");
            return;
        }

        Paragraph title = new Paragraph("Margin Pledge Securities Details")
                .setFont(calibriBold)
                .setFontSize(FONT_SIZE_SMALL)
                .setFontColor(GEOJIT_DARK_GREEN)
                .setMarginTop(12)
                .setMarginBottom(4);
        document.add(title);

        float[] columnWidths = {3f, 1f, 1f, 1f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        table.addHeaderCell(createHeaderCellGreen("Security"));
        table.addHeaderCell(createHeaderCellGreen("Qty"));
        table.addHeaderCell(createHeaderCellGreen("Total Value"));
        table.addHeaderCell(createHeaderCellGreen("HairCut Value"));
        table.addHeaderCell(createHeaderCellGreen("Balance Amount"));

        double totalValue = 0.0;
        double totalHairCut = 0.0;
        double totalBalance = 0.0;

        for (PHeaderTypeModel p : pHeaderTypeModels) {
            String security = safe(p.getSecurityDescription());
            double quantity = parseDouble(p.getQty());
            double value = parseDouble(p.getTotalValue());
            double hairCut = parseDouble(p.getHaircutValue());
            double balance = parseDouble(p.getBalanceAmount());

            totalValue += value;
            totalHairCut += hairCut;
            totalBalance += balance;

            table.addCell(createCell(security, TextAlignment.LEFT, false));
            table.addCell(createCell(formatDecimal(quantity), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(value), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(hairCut), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(balance), TextAlignment.RIGHT, false));
        }

        Cell totalCell = new Cell(1, 2)
                .add(new Paragraph("Total")
                        .setFontSize(FONT_SIZE_TINY)
                        .setBold()
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(2);
        table.addCell(totalCell);

        table.addCell(createCellGreenBg(formatDecimal(totalValue), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalHairCut), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalBalance), TextAlignment.RIGHT, true));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UTILITY METHODS - ✅ ALL FIXED
    // ═══════════════════════════════════════════════════════════════════════

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private <T, K> Map<K, List<T>> safeGroupBy(List<T> list, Function<T, K> keyExtractor) {
        if (list == null || list.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return list.stream()
                .filter(Objects::nonNull)
                .filter(item -> keyExtractor.apply(item) != null)
                .collect(Collectors.groupingBy(
                        keyExtractor,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replaceAll(",", "").trim());
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse double value: " + value);
            return 0.0;
        }
    }

    private int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            String cleaned = value.replaceAll(",", "").split("\\.")[0].trim();
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse integer value: " + value);
            return 0;
        }
    }

    private String formatDecimal(double value) {
        return decimalFormat.format(value);
    }

    private String formatDecimal(String value) {
        return formatDecimal(parseDouble(value));
    }

    private String formatDecimal4(double value) {
        return decimalFormat4.format(value);
    }

    private String formatDecimal4(String value) {
        return formatDecimal4(parseDouble(value));
    }

}