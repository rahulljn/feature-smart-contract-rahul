package com.geojit.contractnotes.equity_combinemargin.v1;

import com.geojit.contractnotes.equity_combinemargin.v1.DTO.EquityDtoV2;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.CHeaderTypeModel;
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

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicValuePDF {

    // ---------------------------------------------------------------
    //  STATIC RESOURCES (mirrors TestAGTS classLoader/font pattern)
    // ---------------------------------------------------------------

    private static ClassLoader classLoader = DynamicValuePDF.class.getClassLoader();
    private static String CALIBRI_FONT = "calibri-400.ttf";
    private static String CALIBRI_BOLD = "calibri-Bold.ttf";

    // ---------------------------------------------------------------
    //  CONSTANTS
    // ---------------------------------------------------------------

    // Font Sizes
    private static final float FONT_SIZE_TITLE = 18f;
    private static final float FONT_SIZE_SUBTITLE = 11f;
    private static final float FONT_SIZE_HEADER = 10f;
    private static final float FONT_SIZE_NORMAL = 8f;
    private static final float FONT_SIZE_DATA = 7.5f;
    private static final float FONT_SIZE_SMALL = 7f;
    private static final float FONT_SIZE_TINY = 6.5f;
    private static final float FONT_SIZE_MICRO = 6f;

    // Brand Colors
    private static final Color GEOJIT_GREEN = new DeviceRgb(0, 114, 114);
    private static final Color GEOJIT_DARK_GREEN = new DeviceRgb(0, 90, 90);
    private static final Color HEADER_BG_COLOR = new DeviceRgb(245, 245, 245);
    private static final Color TABLE_HEADER_GREY = new DeviceRgb(240, 240, 240);
    private static final Color TABLE_HEADER_GREEN = new DeviceRgb(198, 224, 180);
    private static final Color LIGHT_GREEN_BG = new DeviceRgb(200, 240, 240);
    private static final Color BORDER_COLOR = ColorConstants.BLACK;

    // Decimal Formatters
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private static final DecimalFormat decimalFormat4 = new DecimalFormat("0.0000");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    // Fonts (iText 7)
    private static PdfFont calibriNormal;
    private static PdfFont calibriBold;
    private static ImageData logoImageData;

    // ---------------------------------------------------------------
    //  DTO STORAGE
    // ---------------------------------------------------------------

    private static CustomerModel customer;
    private static DealingOfficeAddress dealingOffice;
    private static List<FooterModelV2> footerList = new ArrayList<>();
    private static List<FOHeaderTypeModel> foHeaderTypeList = new ArrayList<>();
    private static List<OHeaderTypeModel> oHeaderTypeList = new ArrayList<>();
    private static List<DHeaderTypeModel> dHeaderTypeList = new ArrayList<>();
    private static List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList = new ArrayList<>();
    private static List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList = new ArrayList<>();
    private static List<CHeaderTypeModel> caHeaderTypeList = new ArrayList<>();
    private static List<SSHeaderTypeModel> ssHeaderTypeModels = new ArrayList<>();
    private static List<STTHeaderTypeModel> sttHeaderTypeModels = new ArrayList<>();
    private static List<PHeaderTypeModel> pHeaderTypeModels = new ArrayList<>();
    private static List<MHeaderTypeModel> mHeaderTypeModels = new ArrayList<>();

    // ---------------------------------------------------------------
    //  MAIN METHOD (mirrors TestAGTS main: single outer try-catch)
    // ---------------------------------------------------------------

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        try {
            System.out.println("GEOJIT PDF GENERATOR V10.1 - LOCAL EXECUTION");

            // Step 1: Initialize fonts
            initializeFonts();

            // Step 2: Read and parse JSON from local file
            String jsonFilePath = (args.length > 0) ? args[0] : "src/main/resources/input.json";
            System.out.println("Reading JSON from: " + jsonFilePath);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            EquityDtoV2 dto = objectMapper.readValue(new File(jsonFilePath), EquityDtoV2.class);
            System.out.println("JSON parsed successfully");

            // Step 3: Load all models from DTO
            loadModelsFromDTO(dto);

            // Step 4: Validate data
            validateData();

            // Step 5: Generate PDF to local file
            String fileName = "Geojit_Contract_Note_" + customer.getContractNo() + ".pdf";
            File pdfFile = new File("src/main/resources/" + fileName);
            System.out.println("Generating PDF: " + pdfFile.getAbsolutePath());

            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());

            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PageNumberEventHandler());

            Document document = new Document(pdfDoc);
            document.setMargins(70, 20, 40, 20);

            // PAGE 1: Main Contract Note
            System.out.println("       Generating Page 1: Main Contract Note");
            addHeader(document);
            addClientInformation(document);
            addEquitySegment(document);
            addDerivativeSegment(document);
            addExchangeSummary(document);
            addFooterNotes(document);
            addSignature(document);
            addDisclaimer(document);

            // PAGE 2: Annexure - Trade Details
            System.out.println("       Generating Page 2: Annexure - Trade Details");
            document.add(new Paragraph("\n"));
            addTradeDetailsAnnexure(document);

            // PAGE 3: Margin Statement
            System.out.println("       Generating Page 3: Margin Statement");
            document.add(new Paragraph("\n"));
            addMarginStatement(document);
            addMarginPledgeSecurities(document);

            document.close();

            System.out.println("PDF Generated Successfully!");
            System.out.println("   Contract No: " + customer.getContractNo());
            System.out.println("   Client: " + customer.getName());
            System.out.println("   File: " + pdfFile.getAbsolutePath());
            System.out.println("file created");

        } catch (Exception e) {
            System.err.println("ERROR generating PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    //  INITIALIZE FONTS
    // ---------------------------------------------------------------

    private static void initializeFonts() throws Exception {
        System.out.println("    Initializing fonts...");

        try {
            try {
                InputStream logoStream = classLoader.getResourceAsStream("logo.png");

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
                calibriNormal = PdfFontFactory.createFont(calibriNormalBytes, PdfEncodings.IDENTITY_H);
                System.out.println("       Calibri Regular loaded");
            } else {
                calibriNormal = PdfFontFactory.createFont();
                System.out.println("       Calibri Regular not found, using default");
            }

            InputStream calibriBoldStream = classLoader.getResourceAsStream(CALIBRI_BOLD);
            if (calibriBoldStream != null) {
                byte[] calibriBoldBytes = readAllBytes(calibriBoldStream);
                calibriBold = PdfFontFactory.createFont(calibriBoldBytes, PdfEncodings.IDENTITY_H);
                System.out.println("       Calibri Bold loaded");
            } else {
                calibriBold = PdfFontFactory.createFont();
                System.out.println("       Calibri Bold not found, using default");
            }

            System.out.println("       Fonts initialized");

        } catch (Exception e) {
            System.err.println("       Error loading fonts: " + e.getMessage());
            calibriNormal = PdfFontFactory.createFont();
            calibriBold = PdfFontFactory.createFont();
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    // ---------------------------------------------------------------
    //  LOAD ALL MODELS FROM DTO
    // ---------------------------------------------------------------

    private static void loadModelsFromDTO(EquityDtoV2 dto) {
        System.out.println("   Loading models from DTO...");

        List<CustomerModel> customerList = dto.getCustomerList();
        customer = (customerList != null && !customerList.isEmpty())
                ? customerList.get(0)
                : new CustomerModel();

        System.out.println("       Customer: " + customer.getName());
        System.out.println("       Contract No: " + customer.getContractNo());
        System.out.println("       Trade Date: " + customer.getTransactionDate());

        List<DealingOfficeAddress> dealingOfficeList = dto.getDealingOfficeAddressList();
        dealingOffice = (dealingOfficeList != null && !dealingOfficeList.isEmpty())
                ? dealingOfficeList.get(0)
                : new DealingOfficeAddress();

        if (dealingOffice != null && dealingOffice.getDealingAddress() != null) {
            System.out.println("       Dealing Office: " + dealingOffice.getDealingAddress());
        }

        dHeaderTypeList = dto.getdHeaderTypeList() != null ?
                dto.getdHeaderTypeList() : new ArrayList<>();
        System.out.println("       Equity/Order Transactions: " + dHeaderTypeList.size());

        foHeaderTypeList = dto.getFoHeaderTypeList() != null ?
                dto.getFoHeaderTypeList() : new ArrayList<>();
        System.out.println("       Derivative Summaries: " + foHeaderTypeList.size());

        oHeaderTypeList = dto.getoHeaderTypeList() != null ?
                dto.getoHeaderTypeList() : new ArrayList<>();
        System.out.println("       Order Details: " + oHeaderTypeList.size());

        footerList = dto.getFooterList() != null ?
                dto.getFooterList() : new ArrayList<>();
        System.out.println("       Footer/Charges: " + footerList.size());
        // Debug: print first footer values to verify Jackson mapping
        if (!footerList.isEmpty()) {
            FooterModelV2 f0 = footerList.get(0);
            System.out.println("       Footer[0] exchange=" + f0.getExchange() + " segment=" + f0.getSegment());
            System.out.println("       Footer[0] pay_in_pay_out_obligation=" + f0.getPay_in_pay_out_obligation());
            System.out.println("       Footer[0] payinout=" + f0.getPayinout());
            System.out.println("       Footer[0] securities_transaction_tax=" + f0.getSecurities_transaction_tax());
            System.out.println("       Footer[0] stt=" + f0.getStt());
            System.out.println("       Footer[0] net_amount=" + f0.getNet_amount());
            System.out.println("       Footer[0] exchange_transaction_charges=" + f0.getExchange_transaction_charges());
            System.out.println("       Footer[0] sebi_fee=" + f0.getSebi_fee());
        }

        mHeaderTypeModels = dto.getmHeaderTypeList() != null ?
                dto.getmHeaderTypeList() : new ArrayList<>();
        System.out.println("       Margin Details: " + mHeaderTypeModels.size());

        pHeaderTypeModels = dto.getpHeaderTypeList() != null ?
                dto.getpHeaderTypeList() : new ArrayList<>();
        System.out.println("       Pledge Securities: " + pHeaderTypeModels.size());

        caHeaderTypeList = dto.getcHeaderTypeList() != null ?
                dto.getcHeaderTypeList() : new ArrayList<>();

        sCapitalHeaderTypeList = dto.getSCapitalHeaderTypeList() != null ?
                dto.getSCapitalHeaderTypeList() : new ArrayList<>();

        sFuturesHeaderTypeList = dto.getSFuturesHeaderTypeList() != null ?
                dto.getSFuturesHeaderTypeList() : new ArrayList<>();

        ssHeaderTypeModels = dto.getSsHeaderTypeList() != null ?
                dto.getSsHeaderTypeList() : new ArrayList<>();

        sttHeaderTypeModels = dto.getSttHeaderTypeList() != null ?
                dto.getSttHeaderTypeList() : new ArrayList<>();

        normalizeMarginData();

        System.out.println("       All models loaded successfully");
    }

    // ---------------------------------------------------------------
    //  NORMALIZE MARGIN DATA
    // ---------------------------------------------------------------

    private static void normalizeMarginData() {
        if (mHeaderTypeModels == null || mHeaderTypeModels.isEmpty()) {
            System.out.println("       No margin records to normalize");
            return;
        }

        System.out.println("       Normalizing margin data for " + mHeaderTypeModels.size() + " records...");

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

                System.out.println("       Normalized: " + m.getExchange() + " " + m.getSegment() +
                        " | Funds=" + m.getFunds());
            }
        }

        System.out.println("       Margin data normalization complete");
    }

    // ---------------------------------------------------------------
    //  VALIDATE DATA
    // ---------------------------------------------------------------

    private static void validateData() throws Exception {
        System.out.println("   Validating data...");

        if (customer == null) {
            throw new Exception("CRITICAL: Customer data is missing!");
        }

        if (customer.getContractNo() == null || customer.getContractNo().isEmpty()) {
            throw new Exception("CRITICAL: Contract Note Number is missing!");
        }

        if (customer.getName() == null || customer.getName().isEmpty()) {
            throw new Exception("CRITICAL: Client Name is missing!");
        }

        if (dHeaderTypeList.isEmpty() && sFuturesHeaderTypeList.isEmpty()) {
            System.out.println("       WARNING: No transactions found!");
        }

        if (footerList.isEmpty()) {
            System.out.println("       WARNING: No footer/charges data found!");
        }

        System.out.println("       Data validation passed");
    }

    // ---------------------------------------------------------------
    //  PAGE NUMBER EVENT HANDLER (inner class)
    // ---------------------------------------------------------------

    private static class PageNumberEventHandler implements IEventHandler {
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

            // Logo on every page
            if (logoImageData != null) {
                Image logo = new Image(logoImageData);
                logo.scaleToFit(120, 45);

                float x = pageSize.getLeft() + 20;
                float y = pageSize.getTop() - 50;

                canvas.add(logo.setFixedPosition(pageNum, x, y));
            }

            // Page number on left (matching reference)
            Paragraph p = new Paragraph("Page No : " + pageNum)
                    .setFontSize(FONT_SIZE_SMALL)
                    .setFontColor(ColorConstants.BLACK);

            canvas.showTextAligned(
                    p,
                    pageSize.getLeft() + 30,
                    pageSize.getBottom() + 20,
                    TextAlignment.LEFT
            );

            canvas.close();
        }
    }

    // ===============================================================
    //  PDF SECTION BUILDERS (ordered by appearance, like TestAGTS)
    // ===============================================================

    // ---------------------------------------------------------------
    //  HEADER SECTION
    // ---------------------------------------------------------------

    private static void addHeader(Document document) {
        try {

            // Main outer header container
            Table mainHeader = new Table(new float[]{1.2f, 3.8f, 1.3f});
            mainHeader.setWidth(UnitValue.createPercentValue(100));
            mainHeader.setBorder(Border.NO_BORDER);
            mainHeader.setMarginBottom(0);

            // LEFT: Logo placeholder
            Cell logoCell = new Cell()
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(8)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderBottom(Border.NO_BORDER);

            mainHeader.addCell(logoCell);

            // CENTER: Title (matches reference: single line "CONTRACT NOTE CUM TAX INVOICE")
            Cell titleCell = new Cell()
                    .add(new Paragraph("CONTRACT NOTE CUM TAX INVOICE")
                            .setFont(calibriBold)
                            .setFontSize(14)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(0))
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

            // RIGHT: Original for Recipient
            Cell originalCell = new Cell()
                    .add(new Paragraph("ORIGINAL FOR RECIPIENT")
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

            // Company Information Box
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
                    .add(new Paragraph("7TH FLOOR, 34/659-P, CIVIL LINE ROAD,PADIVATTOM,KOCHI- 682024 | TEL: 0484-2901000 | FAX:0484 2979695 | Website: www.geojit.com/gil")
                            .setFontSize(7)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(6)
                    .setBorder(Border.NO_BORDER);

            companyBox.addCell(companyCell);
            document.add(companyBox);

            // Compliance Officer Box
            Table complianceBox = new Table(1);
            complianceBox.setWidth(UnitValue.createPercentValue(100));
            complianceBox.setBorder(new SolidBorder(ColorConstants.BLACK, 1f));

            complianceBox.addCell(
                    new Cell()
                            .add(new Paragraph("NAME OF THE COMPLIANCE OFFICER : Ancy C Sunny| EMAIL:compliance@geojit.com | TEL: 0484-2901000 | EMAIL ID FOR INVESTOR COMPLAINT:grievances@geojit.com")
                                    .setFontSize(7)
                                    .setTextAlignment(TextAlignment.CENTER))
                            .setPadding(5)
                            .setBorder(Border.NO_BORDER)
            );

            document.add(complianceBox);

            // Dealing Office Box
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

    // ---------------------------------------------------------------
    //  CLIENT INFORMATION (matches reference layout)
    // ---------------------------------------------------------------

    private static void addClientInformation(Document document) {
        // Reference PDF layout: ONE outer bordered table containing:
        // Row 1: [CONTRACT NOTE NO: | 1237171 | TRADE DATE: 03.07.2025 | Exchange table header row]
        // Row 2: [Client info labels+values LEFT | Exchange data rows RIGHT]
        // All enclosed in a single bordered rectangle

        // Build as a single outer 2-column table (left=client info, right=exchange)
        // with a top row spanning for contract/trade date + exchange header

        float leftPct = 45f;
        float rightPct = 55f;

        // OUTER container with border
        Table outerTable = new Table(UnitValue.createPercentArray(new float[]{leftPct, rightPct}));
        outerTable.setWidth(UnitValue.createPercentValue(100));
        outerTable.setMarginTop(2);

        // === ROW 1: Contract Note + Trade Date on LEFT, Exchange Header on RIGHT ===

        // Left: Contract Note No + Trade Date
        Table cnRow = new Table(UnitValue.createPercentArray(new float[]{1.6f, 1.2f, 1.8f}));
        cnRow.setWidth(UnitValue.createPercentValue(100));
        cnRow.addCell(new Cell().add(new Paragraph("CONTRACT NOTE NO :").setFontSize(FONT_SIZE_DATA))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(3));
        cnRow.addCell(new Cell().add(new Paragraph(safe(customer.getContractNo())).setFontSize(FONT_SIZE_DATA).setFont(calibriBold))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(3));
        cnRow.addCell(new Cell().add(new Paragraph("TRADE DATE : " + safe(customer.getTransactionDate())).setFontSize(FONT_SIZE_DATA).setFont(calibriBold))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(3));

        outerTable.addCell(new Cell().add(cnRow)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(0));

        // Right: Exchange table header
        Table exchangeHeader = new Table(UnitValue.createPercentArray(new float[]{2.2f, 1f, 1.1f, 1.3f, 1f}));
        exchangeHeader.setWidth(UnitValue.createPercentValue(100));
        exchangeHeader.addCell(createExchangeHeaderCell("EXCHANGE /\nCLEARING\nCORPORATION"));
        exchangeHeader.addCell(createExchangeHeaderCell("SEGMENT"));
        exchangeHeader.addCell(createExchangeHeaderCell("STTLNO"));
        exchangeHeader.addCell(createExchangeHeaderCell("STTLDATE"));
        exchangeHeader.addCell(createExchangeHeaderCell("UCCODE"));

        outerTable.addCell(new Cell().add(exchangeHeader)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(0));

        // === ROW 2: Client Details LEFT, Exchange Data RIGHT ===

        // LEFT: Client detail rows
        Table leftTable = new Table(UnitValue.createPercentArray(new float[]{1.6f, 2f}));
        leftTable.setWidth(UnitValue.createPercentValue(100));

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
        leftTable.addCell(createInfoValueCell(safe(customer.getClientCode()), true));

        leftTable.addCell(createInfoLabelCell("Place Of Supply [State Code] :"));
        leftTable.addCell(createInfoValueCell("KERALA[32]", true));

        leftTable.addCell(createInfoLabelCell("Invoice Reference Number(IRN) :"));
        leftTable.addCell(createInfoValueCell(safe(customer.getIrn()), false));

        leftTable.addCell(createInfoLabelCell("GST Identification No. :"));
        leftTable.addCell(createInfoValueCell(safe(customer.getGstNo()), false));

        outerTable.addCell(new Cell().add(leftTable)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP));

        // RIGHT: Exchange data rows
        Map<String, DHeaderTypeModel> uniqueExchanges = new LinkedHashMap<>();
        for (DHeaderTypeModel d : dHeaderTypeList) {
            String actualSegment = safe(d.getSegment2());
            if (actualSegment.isEmpty()) {
                actualSegment = safe(d.getSegment());
            }
            String key = safe(d.getExchange()) + "|" + actualSegment;
            if (!uniqueExchanges.containsKey(key)) {
                uniqueExchanges.put(key, d);
            }
        }

        Table exchangeData = new Table(UnitValue.createPercentArray(new float[]{2.2f, 1f, 1.1f, 1.3f, 1f}));
        exchangeData.setWidth(UnitValue.createPercentValue(100));

        for (DHeaderTypeModel d : uniqueExchanges.values()) {
            String exchange = safe(d.getExchange());
            String segment = safe(d.getSegment2());
            if (segment.isEmpty()) {
                segment = safe(d.getSegment());
                if ("CAPITAL".equalsIgnoreCase(segment)) segment = "EN";
                else if ("FUTURES".equalsIgnoreCase(segment)) segment = "FO";
            }
            String sttlNo = safe(d.getSettlementNo());
            String sttlDate = safe(d.getSettlementdate());
            if (!isNumeric(sttlNo)) sttlNo = "";
            if (sttlDate.equalsIgnoreCase("STTLDATE") || sttlDate.isEmpty()) sttlDate = "";
            String ucCode = safe(customer.getPartycode());
            if (ucCode.isEmpty()) {
                String cc = safe(customer.getClientCode());
                ucCode = cc.contains("/") ? cc.split("/")[0] : cc;
            }

            String exchangeClearing = exchange + " / NCL";

            exchangeData.addCell(createExchangeDataCell(exchangeClearing, TextAlignment.LEFT));
            exchangeData.addCell(createExchangeDataCell(segment, TextAlignment.CENTER));
            exchangeData.addCell(createExchangeDataCell(sttlNo, TextAlignment.CENTER));
            exchangeData.addCell(createExchangeDataCell(sttlDate, TextAlignment.CENTER));
            exchangeData.addCell(createExchangeDataCell(ucCode, TextAlignment.CENTER));
        }

        outerTable.addCell(new Cell().add(exchangeData)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)).setPadding(0)
                .setVerticalAlignment(VerticalAlignment.TOP));

        document.add(outerTable);

        // PAN confirmation text
        Paragraph confirmText = new Paragraph()
                .add(new Text("Sir/Madam, I / We have this day done by your order and on your account the following transactions: ")
                        .setFontSize(FONT_SIZE_DATA))
                .add(new Text("PAN of the Client :   " + safe(customer.getPanNo()))
                        .setFontSize(FONT_SIZE_DATA)
                        .setFont(calibriBold))
                .setMarginTop(4)
                .setMarginBottom(4);
        document.add(confirmText);
    }

    // ---------------------------------------------------------------
    //  EQUITY SEGMENT (underlined title matching reference)
    // ---------------------------------------------------------------

    private static void addEquitySegment(Document document) {
        if (sCapitalHeaderTypeList.isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("Equity Segment")
                .setFont(calibriBold)
                .setFontSize(FONT_SIZE_HEADER)
                .setUnderline()
                .setMarginTop(8)
                .setMarginBottom(4);
        document.add(sectionTitle);

        float[] columnWidths = {0.8f, 2.0f, 0.6f, 0.8f, 0.75f, 0.8f, 0.9f, 0.6f, 0.8f, 0.75f, 0.8f, 0.9f, 0.6f, 0.95f};

        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        // Row 1 Headers
        Cell securityDescHeader = new Cell(2, 2)
                .add(new Paragraph("Security\nDescription")
                        .setFontSize(FONT_SIZE_SMALL).setBold().setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(securityDescHeader);

        Cell buyHeader = new Cell(1, 5)
                .add(new Paragraph("Buy")
                        .setFontSize(FONT_SIZE_DATA).setBold().setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(buyHeader);

        Cell sellHeader = new Cell(1, 5)
                .add(new Paragraph("Sell")
                        .setFontSize(FONT_SIZE_DATA).setBold().setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(sellHeader);

        Cell netObligationHeader = new Cell(2, 2)
                .add(new Paragraph("Net Obligation for ISIN\n[Before Levies] (Rs)*")
                        .setFontSize(FONT_SIZE_TINY).setBold().setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        table.addHeaderCell(netObligationHeader);

        // Row 2 Sub-headers
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

        // Row 3: ISIN / Security Name sub-headers
        Cell isinLabel = new Cell()
                .add(new Paragraph("ISIN").setFontSize(FONT_SIZE_TINY).setBold()
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(TABLE_HEADER_GREY)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setTextAlignment(TextAlignment.CENTER);
        table.addCell(isinLabel);

        Cell securityNameLabel = new Cell()
                .add(new Paragraph("Security\nName /\nSymbol").setFontSize(FONT_SIZE_TINY).setBold()
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(TABLE_HEADER_GREY)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setTextAlignment(TextAlignment.CENTER);
        table.addCell(securityNameLabel);

        for (int i = 0; i < 12; i++) {
            Cell emptyCell = new Cell()
                    .add(new Paragraph(""))
                    .setBackgroundColor(TABLE_HEADER_GREY)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2);
            table.addCell(emptyCell);
        }

        // Data rows from SCapitalHeaderTypeModel
        for (SCapitalHeaderTypeModel sc : sCapitalHeaderTypeList) {
            String isin = safe(sc.getIsin());
            String securityDesc = safe(sc.getSecurityDescription());

            int buyQty = parseInt(sc.getBuyQty());
            double buyWap = parseDouble(sc.getBuyWap());
            double buyBrokerage = parseDouble(sc.getBuyBrokerage());
            double buyWapAfterBrokerage = parseDouble(sc.getBuyWapAfterBrokerage());
            double buyValue = parseDouble(sc.getBuyValue());

            int sellQty = parseInt(sc.getSellQty());
            double sellWap = parseDouble(sc.getSellWap());
            double sellBrokerage = parseDouble(sc.getSellBrokerage());
            double sellWapAfterBrokerage = parseDouble(sc.getSellWapAfterBrokerage());
            double sellValue = parseDouble(sc.getSellValue());

            int netQuantity = parseInt(sc.getNetQty());
            double netObligation = parseDouble(sc.getNetObligationIsin());

            table.addCell(createEquityDataCell(isin, TextAlignment.LEFT));
            table.addCell(createEquityDataCell(securityDesc, TextAlignment.LEFT, true));
            table.addCell(createEquityDataCell(String.valueOf(buyQty), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(buyWap), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(buyBrokerage), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(buyWapAfterBrokerage), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(buyValue), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(String.valueOf(sellQty), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(sellWap), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(sellBrokerage), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(sellWapAfterBrokerage), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(sellValue), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(String.valueOf(netQuantity), TextAlignment.RIGHT));
            table.addCell(createEquityDataCell(formatDecimal4(netObligation), TextAlignment.RIGHT));
        }

        document.add(table);

        Paragraph footnote = new Paragraph("* Exchange-wise details of orders and trades are provided in separate annexure.")
                .setFontSize(FONT_SIZE_TINY).setItalic().setMarginTop(2);
        document.add(footnote);
    }

    // ---------------------------------------------------------------
    //  DERIVATIVE SEGMENT (10 columns matching reference)
    // ---------------------------------------------------------------

    private static void addDerivativeSegment(Document document) {
        if (sFuturesHeaderTypeList.isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("Derivative Segment")
                .setFont(calibriBold).setFontSize(FONT_SIZE_HEADER)
                .setUnderline()
                .setMarginTop(8).setMarginBottom(4);
        document.add(sectionTitle);

        // 10 columns matching reference: Contract, B/S, Qty, WAP Foreign, WAP Rs, Brokerage, WAP After Brok, Closing Rate, Net Total, Remarks
        float[] columnWidths = {2.2f, 0.5f, 0.7f, 0.8f, 0.8f, 0.8f, 0.9f, 0.8f, 0.9f, 0.8f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        table.addHeaderCell(createDerivativeHeaderCell("Contract Description"));
        table.addHeaderCell(createDerivativeHeaderCell("Buy[B]/\nSell[S]/\nBF/CF"));
        table.addHeaderCell(createDerivativeHeaderCell("Quantity"));
        table.addHeaderCell(createDerivativeHeaderCell("WAP per unit\n(In Foriegn\nCurrency)"));
        table.addHeaderCell(createDerivativeHeaderCell("WAP per unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("Brokerage\nPer Unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("WAP per unit\nAfter\nBrokerage(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("Closing Rate\nper Unit\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("Net Total\n(Before Levies)\n(Rs)"));
        table.addHeaderCell(createDerivativeHeaderCell("Remarks"));

        for (SFuturesHeaderTypeModel sf : sFuturesHeaderTypeList) {
            String contractDesc = safe(sf.getContractDesc());

            String buySell = safe(sf.getTradeType()).toUpperCase();
            if ("BUY".equals(buySell)) buySell = "B";
            else if ("SELL".equals(buySell)) buySell = "S";

            int quantity = parseInt(sf.getTradeQty());
            double wapForeign = parseDouble(sf.getTradeWapFc());
            double wapPerUnit = parseDouble(sf.getTradeWap());
            // tradeBrokerage from raw data is already per-unit value
            double brokeragePerUnit = parseDouble(sf.getTradeBrokerage());

            double wapAfterBrokerage = parseDouble(sf.getTradeWapAfterBrokerage());
            if (wapAfterBrokerage == 0.0) {
                if ("B".equals(buySell)) wapAfterBrokerage = wapPerUnit + brokeragePerUnit;
                else if ("S".equals(buySell)) wapAfterBrokerage = wapPerUnit - brokeragePerUnit;
            }

            double closingRate = parseDouble(sf.getTradeClosingRate());
            double netTotal = parseDouble(sf.getTradeNetTotal());

            String remarks = safe(sf.getRemarks());

            table.addCell(createDerivativeDataCell(contractDesc, TextAlignment.LEFT));
            table.addCell(createDerivativeDataCell(buySell, TextAlignment.CENTER));
            table.addCell(createDerivativeDataCell(String.valueOf(quantity), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal4(wapForeign), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal4(wapPerUnit), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal4(brokeragePerUnit), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal4(wapAfterBrokerage), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal4(closingRate), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(formatDecimal4(netTotal), TextAlignment.RIGHT));
            table.addCell(createDerivativeDataCell(remarks, TextAlignment.LEFT));
        }

        document.add(table);

        Paragraph footnote = new Paragraph("* Exchange-wise details of orders and trades are provided in separate annexure.")
                .setFontSize(FONT_SIZE_TINY).setItalic().setMarginTop(2);
        document.add(footnote);
    }

    // ---------------------------------------------------------------
    //  EXCHANGE SUMMARY (green header background matching reference)
    // ---------------------------------------------------------------

    private static void addExchangeSummary(Document document) {
        if (footerList.isEmpty()) {
            return;
        }

        float[] columnWidths = {1.2f, 0.8f, 0.6f, 0.5f, 0.5f, 0.5f, 0.4f, 0.7f, 0.5f, 0.6f, 0.5f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);
        table.setMarginTop(8);

        table.addHeaderCell(createHeaderCellGreen("Name Of Exchange\n& Segment"));
        table.addHeaderCell(createHeaderCellGreen("PAY IN/PAY OUT\nOBLIGATION"));
        table.addHeaderCell(createHeaderCellGreen("SECURITIES\nTRANSACTION TAX"));
        table.addHeaderCell(createHeaderCellGreen("SGST (**)\n9%"));
        table.addHeaderCell(createHeaderCellGreen("CGST (**)\n9%"));
        table.addHeaderCell(createHeaderCellGreen("IGST (**)\n18%"));
        table.addHeaderCell(createHeaderCellGreen("TDS"));
        table.addHeaderCell(createHeaderCellGreen("Exchange Transactn\nCharges"));
        table.addHeaderCell(createHeaderCellGreen("SEBI Turnover\nFees"));
        table.addHeaderCell(createHeaderCellGreen("Additional Cess ***"));
        table.addHeaderCell(createHeaderCellGreen("Stamp\nDuty"));
        table.addHeaderCell(createHeaderCellGreen("Net Amount Receivable By\nClient/(Payable by Client)"));

        // Accumulators for TOTAL row
        double totalPayInOut = 0, totalSTT = 0, totalSGST = 0, totalCGST = 0, totalIGST = 0;
        double totalTDS = 0, totalExchangeCharges = 0, totalSEBIFees = 0;
        double totalCess = 0, totalStampDuty = 0, totalNetAmount = 0;

        for (FooterModelV2 footer : footerList) {
            // Exchange segment display: BSE+FO+OPT = "BSEFO-OPT", NSE+EN+""= "NSEEN"
            String exchange = safe(footer.getExchange());
            String segment = safe(footer.getSegment());
            String instrument = safe(footer.getInstrument());
            String exchangeSegment = exchange + segment + (instrument.isEmpty() ? "" : "-" + instrument);

            // Use fallback: try long-name getter first, then short-name (Jackson may map either)
            String rawPayInOut = firstNonEmpty(footer.getPay_in_pay_out_obligation(), footer.getPayinout());
            String rawSTT = firstNonEmpty(footer.getSecurities_transaction_tax(), footer.getStt());
            String rawSGST = safe(footer.getSgst());
            String rawCGST = safe(footer.getCgst());
            String rawIGST = safe(footer.getIgst());
            String rawTDS = safe(footer.getTds());
            String rawExchangeCharges = firstNonEmpty(footer.getExchange_transaction_charges(), footer.getExchangecharge());
            String rawSEBIFees = firstNonEmpty(footer.getSebi_fee(), footer.getSebifee());
            String rawCess = firstNonEmpty(footer.getAdd_cess(), footer.getCess());
            String rawStampDuty = safe(footer.getStampduty());
            String rawNetAmount = safe(footer.getNet_amount());

            // Accumulate for totals
            totalPayInOut += parseDouble(rawPayInOut);
            totalSTT += parseDouble(rawSTT);
            totalSGST += parseDouble(rawSGST);
            totalCGST += parseDouble(rawCGST);
            totalIGST += parseDouble(rawIGST);
            totalTDS += parseDouble(rawTDS);
            totalExchangeCharges += parseDouble(rawExchangeCharges);
            totalSEBIFees += parseDouble(rawSEBIFees);
            totalCess += parseDouble(rawCess);
            totalStampDuty += parseDouble(rawStampDuty);
            totalNetAmount += parseDouble(rawNetAmount);

            // Display raw values as-is from pipe file
            table.addCell(createCell(exchangeSegment, TextAlignment.LEFT, true));
            table.addCell(createCell(rawPayInOut, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawSTT, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawSGST, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawCGST, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawIGST, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawTDS, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawExchangeCharges, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawSEBIFees, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawCess, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawStampDuty, TextAlignment.RIGHT, false));
            table.addCell(createCell(rawNetAmount, TextAlignment.RIGHT, false));
        }

        // TOTAL row
        table.addCell(createCell("TOTAL(NET)", TextAlignment.LEFT, true));
        table.addCell(createCell(formatDecimalSmart(totalPayInOut), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalSTT), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalSGST), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalCGST), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalIGST), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalTDS), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalExchangeCharges), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalSEBIFees), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalCess), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalStampDuty), TextAlignment.RIGHT, true));
        table.addCell(createCell(formatDecimalSmart(totalNetAmount) + " *", TextAlignment.RIGHT, true));

        document.add(table);

        // GST footnote — read from C|SEBI record if available
        String gstBaseText = "";
        if (caHeaderTypeList != null) {
            for (CHeaderTypeModel c : caHeaderTypeList) {
                String seg = safe(c.getSegment());
                if (seg.contains("18%") || seg.contains("Rs.")) {
                    gstBaseText = seg;
                    break;
                }
            }
        }
        if (gstBaseText.isEmpty()) {
            // Fallback: calculate GST base = Brokerage + Exchange Charges + SEBI Fee
            // Get total brokerage from D records (equity brokerage is per unit * qty, FO is total)
            double totalBrokerage = 0;
            for (DHeaderTypeModel d : dHeaderTypeList) {
                try {
                    double brokerage = parseDouble(d.getBrokerage());
                    String segment = safe(d.getSegment2());
                    if (segment.isEmpty()) segment = safe(d.getSegment());
                    // For equity (EN/CAPITAL), brokerage is per share → multiply by qty
                    if ("EN".equalsIgnoreCase(segment) || "CAPITAL".equalsIgnoreCase(segment)) {
                        double qty = parseDouble(d.getQty());
                        totalBrokerage += brokerage * qty;
                    } else {
                        // For FO, brokerage is total for the transaction
                        totalBrokerage += brokerage;
                    }
                } catch (Exception e) { /* skip */ }
            }
            double gstBase = totalBrokerage + totalExchangeCharges + totalSEBIFees;
            gstBaseText = "18% of Rs." + formatDecimal(gstBase);
        }

        document.add(new Paragraph("** SGST: - State GST; CGST:-Central GST; IGST:-Integrated GST. " +
                "GST is calculated on Brokerage, Exchange Transaction Charges and SEBI Fee. (" + gstBaseText + ")")
                .setFontSize(FONT_SIZE_TINY).setMarginTop(2));

        document.add(new Paragraph("# Brokerage shown is per unit in the case of Equities and total " +
                "brokerage for the particular transaction in the case of F&O and Currency trades.")
                .setFontSize(FONT_SIZE_TINY));

        document.add(new Paragraph("* Any other charges (DIS charge/AMC/Overdue charges etc.) which " +
                "are due to us will be debited from the net amount.")
                .setFontSize(FONT_SIZE_TINY));
    }

    // ---------------------------------------------------------------
    //  FOOTER NOTES
    // ---------------------------------------------------------------

    private static void addFooterNotes(Document document) {
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
                .setTextAlignment(TextAlignment.JUSTIFIED).setMarginTop(8);
        document.add(notes);
    }

    // ---------------------------------------------------------------
    //  SIGNATURE (with QR code and signature placeholders)
    // ---------------------------------------------------------------

    private static void addSignature(Document document) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginTop(8);
        table.setBorder(Border.NO_BORDER);

        String tradeDate = safe(customer.getTransactionDate());

        // Left: Date, Place, QR code placeholder
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Date:  " + tradeDate).setFontSize(FONT_SIZE_SMALL))
                .add(new Paragraph("Place: " + extractPlaceFromDealingOffice()).setFontSize(FONT_SIZE_SMALL))
                .add(new Paragraph("\n\n[QR Code / Digital Signature]")
                        .setFontSize(FONT_SIZE_MICRO).setFontColor(ColorConstants.GRAY));

        // Right: Signature block
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Yours Faithfully,").setFontSize(FONT_SIZE_SMALL).setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("GEOJIT INVESTMENTS LTD").setFontSize(FONT_SIZE_SMALL).setBold()
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("(PAN : AAKCG3453A,  GSTIN : " + extractGSTIN() + ")")
                        .setFontSize(FONT_SIZE_TINY).setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("Description of Service : STOCK BROKER")
                        .setFontSize(FONT_SIZE_TINY).setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("Service Account Code(SAC) : 997152")
                        .setFontSize(FONT_SIZE_TINY).setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("\n\nJohny Varghese").setFontSize(FONT_SIZE_SMALL).setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("(Name & Signature of Authorised Signatory)")
                        .setFontSize(FONT_SIZE_TINY).setTextAlignment(TextAlignment.RIGHT));

        table.addCell(leftCell);
        table.addCell(rightCell);
        document.add(table);
    }

    // ---------------------------------------------------------------
    //  DISCLAIMER
    // ---------------------------------------------------------------

    private static void addDisclaimer(Document document) {
        document.add(new Paragraph()
                .add(new Text("Disclaimer: ").setBold())
                .add(new Text("Purchase of REs (Rights Entitlements)only gives right to participate in the ongoing " +
                        "Rights Issue of the concerned company. REs which are neither subscribed by making an application " +
                        "with requisite application money nor renounced on or before the Issue Closing Date shall lapse and " +
                        "shall be extinguished after the Issue Closing Date."))
                .setFontSize(FONT_SIZE_TINY).setMarginTop(8).setTextAlignment(TextAlignment.JUSTIFIED));

        document.add(new Paragraph(" * End Of Contract *")
                .setFontSize(FONT_SIZE_SMALL).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(8));
    }

    // ---------------------------------------------------------------
    //  PAGE 2: ANNEXURE - TRADE DETAILS (full format matching reference)
    // ---------------------------------------------------------------

    private static void addTradeDetailsAnnexure(Document document) {
        if (dHeaderTypeList.isEmpty()) {
            return;
        }

        document.add(new Paragraph("\n").setMarginTop(15));

        document.add(new Paragraph("Annexure - Trade Details")
                .setFont(calibriBold).setFontSize(FONT_SIZE_SUBTITLE)
                .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(8));

        document.add(new Paragraph("Name of the Clearing Corporation & Segment  : NSE Clearing Limited")
                .setFontSize(FONT_SIZE_NORMAL).setBold().setMarginBottom(6));

        // 14-column Trade Details table matching reference
        float[] columnWidths = {0.9f, 0.6f, 0.7f, 0.6f, 2f, 0.4f, 0.5f, 0.7f, 0.7f, 0.6f, 0.6f, 0.7f, 0.8f, 0.6f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        table.addHeaderCell(createHeaderCellGreen("Order\nNo."));
        table.addHeaderCell(createHeaderCellGreen("Order\nTime"));
        table.addHeaderCell(createHeaderCellGreen("Trade\nNo."));
        table.addHeaderCell(createHeaderCellGreen("Trade\nTime"));
        table.addHeaderCell(createHeaderCellGreen("Security/Contract\nDescription"));
        table.addHeaderCell(createHeaderCellGreen("Buy/\nSell"));
        table.addHeaderCell(createHeaderCellGreen("Quantity"));
        table.addHeaderCell(createHeaderCellGreen("Gross Rate/Trade\nPrice Per Unit\n(in foreign currency)"));
        table.addHeaderCell(createHeaderCellGreen("Gross Rate/Trade\nPrice Per Unit\n(Rs)"));
        table.addHeaderCell(createHeaderCellGreen("#Brokerage\n(Rs)"));
        table.addHeaderCell(createHeaderCellGreen("Net Rate\nPer Unit\n(Rs)"));
        table.addHeaderCell(createHeaderCellGreen("Closing Rate\nPer Unit(Only\nFor derivatives)\n(Rs)"));
        table.addHeaderCell(createHeaderCellGreen("Net total\n(Before Levies)\n(Rs.)"));
        table.addHeaderCell(createHeaderCellGreen("Remarks"));

        // Group D records by exchange+segment2 (segment2 has actual FO/EN, segment is always CAPITAL)
        Map<String, List<DHeaderTypeModel>> byExchange = safeGroupBy(dHeaderTypeList, d -> {
            String exchange = safe(d.getExchange());
            String segment = safe(d.getSegment2());
            // Fallback to segment if segment2 is empty
            if (segment.isEmpty()) {
                segment = safe(d.getSegment());
                if ("CAPITAL".equalsIgnoreCase(segment)) segment = "EN";
                else if ("FUTURES".equalsIgnoreCase(segment)) segment = "FO";
            }
            return exchange + " " + segment;
        });

        double grandTotalAmount = 0.0;

        for (Map.Entry<String, List<DHeaderTypeModel>> entry : byExchange.entrySet()) {
            String exchangeSegment = entry.getKey().trim();
            List<DHeaderTypeModel> trades = entry.getValue();

            if (!exchangeSegment.isEmpty()) {
                String exchange = exchangeSegment.split(" ")[0];
                String segType = exchangeSegment.contains("FO") ? "FO" : "EN";
                // Get broker code from first trade in this group
                String brokerCode = safe(trades.get(0).getBroker_code());
                if (brokerCode.isEmpty()) {
                    brokerCode = exchange.contains("NSE") ? "13372" : "0328";
                }

                table.addCell(new Cell(1, 14)
                        .add(new Paragraph("Name Of Exchange & Segment :   " + exchange + " " + segType + " | " +
                                exchange + " BrokerCode : " + brokerCode)
                                .setFontSize(FONT_SIZE_TINY).setBold().setTextAlignment(TextAlignment.LEFT))
                        .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                        .setBackgroundColor(TABLE_HEADER_GREEN));
            }

            double subTotal = 0.0;
            String subTotalDesc = "";

            for (DHeaderTypeModel d : trades) {
                double netTotal = parseDouble(d.getNet_total());
                subTotal += netTotal;

                String buySell = safe(d.getBuy_sell()).toUpperCase();
                if ("BUY".equals(buySell)) buySell = "B";
                else if ("SELL".equals(buySell)) buySell = "S";

                // gross_rate_fc may contain placeholder text like "Gross Rate/Trade..."
                String grossRateFc = safe(d.getGross_rate_fc());
                if (!grossRateFc.isEmpty() && !isNumeric(grossRateFc)) grossRateFc = "";

                // market_rate for Rs column
                String marketRate = safe(d.getMarket_rate());
                if (marketRate.isEmpty()) marketRate = safe(d.getPrice());

                // closing_rate may contain placeholder text
                String closingRate = safe(d.getClosing_rate());
                if (!closingRate.isEmpty() && !isNumeric(closingRate)) closingRate = "";

                table.addCell(createCell(safe(d.getOrderno()), TextAlignment.LEFT, false));
                table.addCell(createCell(safe(d.getOrder_time()), TextAlignment.CENTER, false));
                table.addCell(createCell(safe(d.getTrade_no()), TextAlignment.LEFT, false));
                table.addCell(createCell(safe(d.getTrade_time()), TextAlignment.CENTER, false));
                table.addCell(createCell(safe(d.getSecurity_contract_description()), TextAlignment.LEFT, false));
                table.addCell(createCell(buySell, TextAlignment.CENTER, false));
                table.addCell(createCell(safe(d.getQty()), TextAlignment.RIGHT, false));
                table.addCell(createCell(grossRateFc.isEmpty() ? "" : formatDecimal4(parseDouble(grossRateFc)), TextAlignment.RIGHT, false));
                table.addCell(createCell(marketRate.isEmpty() ? "" : formatDecimal4(parseDouble(marketRate)), TextAlignment.RIGHT, false));
                table.addCell(createCell(formatDecimal4(parseDouble(d.getBrokerage())), TextAlignment.RIGHT, false));
                table.addCell(createCell(safe(d.getNet_rate()).isEmpty() ? "" : formatDecimal4(parseDouble(d.getNet_rate())), TextAlignment.RIGHT, false));
                table.addCell(createCell(closingRate.isEmpty() ? "-" : formatDecimal4(parseDouble(closingRate)), TextAlignment.RIGHT, false));
                table.addCell(createCell(formatDecimal4(netTotal), TextAlignment.RIGHT, false));
                table.addCell(createCell(safe(d.getRemark()), TextAlignment.LEFT, false));

                // Capture exchangeID for subtotal description
                if (safe(d.getExchangeID()) != null && !safe(d.getExchangeID()).isEmpty()) {
                    subTotalDesc = safe(d.getExchangeID());
                }
            }

            // SubTotal row
            table.addCell(new Cell(1, 4).add(new Paragraph("SubTotal").setFontSize(FONT_SIZE_TINY).setBold()
                    .setTextAlignment(TextAlignment.LEFT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));
            table.addCell(new Cell(1, 2).add(new Paragraph(subTotalDesc).setFontSize(FONT_SIZE_TINY)
                    .setTextAlignment(TextAlignment.LEFT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));
            table.addCell(new Cell(1, 7).add(new Paragraph("").setFontSize(FONT_SIZE_TINY))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));
            table.addCell(new Cell().add(new Paragraph(formatDecimal4(subTotal)).setFontSize(FONT_SIZE_TINY).setBold()
                    .setTextAlignment(TextAlignment.RIGHT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));

            grandTotalAmount += subTotal;
        }

        document.add(table);

        // After trade details, add Net Obligation, Scrip Summary, Cash STT, Derivatives STT
        addNetObligationSection(document);
        addScripSummary(document);
        addCashSTTSection(document);
        addSTTSection(document);
    }

    // ---------------------------------------------------------------
    //  NET OBLIGATION SECTION
    // ---------------------------------------------------------------

    private static void addNetObligationSection(Document document) {
        if (oHeaderTypeList.isEmpty()) {
            return;
        }

        document.add(new Paragraph("Net Obligation (Equity Market)")
                .setFont(calibriBold).setFontSize(FONT_SIZE_NORMAL)
                .setMarginTop(8).setMarginBottom(4));

        float[] columnWidths = {0.5f, 2.5f, 0.8f, 0.7f, 0.9f, 0.7f, 0.9f, 0.7f, 0.9f, 1f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        table.addHeaderCell(createSpanningHeader("Sl.No", 2, 1));
        table.addHeaderCell(createSpanningHeader("Security", 2, 1));
        table.addHeaderCell(createSpanningHeader("Segment", 2, 1));
        table.addHeaderCell(createSpanningHeader("Bought", 1, 2));
        table.addHeaderCell(createSpanningHeader("Sold", 1, 2));
        table.addHeaderCell(createSpanningHeader("Net Obligation", 1, 2));
        table.addHeaderCell(createSpanningHeader("Amount", 2, 1));

        table.addHeaderCell(createNetObligationHeaderCell("Quantity"));
        table.addHeaderCell(createNetObligationHeaderCell("Rate"));
        table.addHeaderCell(createNetObligationHeaderCell("Quantity"));
        table.addHeaderCell(createNetObligationHeaderCell("Rate"));
        table.addHeaderCell(createNetObligationHeaderCell("Quantity"));
        table.addHeaderCell(createNetObligationHeaderCell("Rate"));

        double totalAmount = 0.0;
        double totalStt = 0.0;
        int slNo = 1;

        for (OHeaderTypeModel o : oHeaderTypeList) {
            // Use segment2 (f[4]) which has actual EN/FO, not segment (f[2] = always CAPITAL)
            String segment = safe(o.getSegment2());
            if (segment.isEmpty()) {
                segment = safe(o.getSegment());
                if ("CAPITAL".equalsIgnoreCase(segment)) segment = "EN";
            }

            double amount = parseDouble(o.getAmount());
            totalAmount += amount;
            totalStt += parseDouble(o.getStt());

            table.addCell(createNetObligationDataCell(String.valueOf(slNo++), TextAlignment.CENTER));
            table.addCell(createNetObligationDataCell(safe(o.getSecurityDescription()), TextAlignment.LEFT));
            table.addCell(createNetObligationDataCell(segment, TextAlignment.CENTER));
            table.addCell(createNetObligationDataCell(String.valueOf(parseInt(o.getBuyQty())), TextAlignment.RIGHT));
            table.addCell(createNetObligationDataCell(formatDecimal(parseDouble(o.getBuyRate())), TextAlignment.RIGHT));
            table.addCell(createNetObligationDataCell(String.valueOf(parseInt(o.getSellQty())), TextAlignment.RIGHT));
            table.addCell(createNetObligationDataCell(formatDecimal(parseDouble(o.getSellRate())), TextAlignment.RIGHT));
            table.addCell(createNetObligationDataCell(String.valueOf(parseInt(o.getNetQty())), TextAlignment.RIGHT));
            table.addCell(createNetObligationDataCell(formatDecimal(parseDouble(o.getNetRate())), TextAlignment.RIGHT));
            table.addCell(createNetObligationDataCell(formatDecimal(amount), TextAlignment.RIGHT));
        }

        table.addCell(new Cell(1, 9).add(new Paragraph("Total").setFontSize(FONT_SIZE_TINY).setBold()
                .setTextAlignment(TextAlignment.RIGHT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3));
        table.addCell(new Cell().add(new Paragraph(formatDecimal(totalAmount)).setFontSize(FONT_SIZE_TINY).setBold()
                .setTextAlignment(TextAlignment.RIGHT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3));

        document.add(table);

        double netAmount = totalAmount - totalStt;

        Table summaryTable = new Table(2);
        summaryTable.setWidth(UnitValue.createPercentValue(30));
        summaryTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        summaryTable.setMarginTop(4);

        summaryTable.addCell(createSummaryCell("Securities Transaction Tax", TextAlignment.LEFT, false));
        summaryTable.addCell(createSummaryCell(formatDecimal(totalStt), TextAlignment.RIGHT, false));
        summaryTable.addCell(createSummaryCell("Net Amount", TextAlignment.LEFT, true));
        summaryTable.addCell(createSummaryCell(formatDecimal(netAmount), TextAlignment.RIGHT, true));

        document.add(summaryTable);
    }

    // ---------------------------------------------------------------
    //  SCRIP SUMMARY
    // ---------------------------------------------------------------

    private static void addScripSummary(Document document) {
        if (ssHeaderTypeModels.isEmpty()) return;

        document.add(new Paragraph("Scrip-Summary").setFontSize(FONT_SIZE_SMALL).setBold()
                .setUnderline().setMarginTop(8).setMarginBottom(4));

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
            String buySell = safe(ss.getTradeType()).toUpperCase();
            if ("BUY".equals(buySell)) buySell = "B";
            if ("SELL".equals(buySell)) buySell = "S";

            table.addCell(createCell(safe(ss.getSecurityDescription()), TextAlignment.LEFT, false));
            table.addCell(createCell(buySell, TextAlignment.CENTER, false));
            table.addCell(createCell(String.valueOf(parseInt(ss.getTradeQty())), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(parseDouble(ss.getGrossRate())), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(parseDouble(ss.getGrossTotal())), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(parseDouble(ss.getGrossBrokerage())), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(parseDouble(ss.getBrokerage())), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal4(parseDouble(ss.getNetRate())), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(parseDouble(ss.getNetAmount())), TextAlignment.RIGHT, false));
        }

        document.add(table);
    }

    // ---------------------------------------------------------------
    //  CASH STT SECTION (new: matching reference PDF)
    // ---------------------------------------------------------------

    private static void addCashSTTSection(Document document) {
        // Filter STT records for equity segment (EN)
        List<STTHeaderTypeModel> equitySTTRecords = new ArrayList<>();
        for (STTHeaderTypeModel stt : sttHeaderTypeModels) {
            String seg = safe(stt.getSegment()).toUpperCase();
            if ("EN".equals(seg) || "CAPITAL".equals(seg) || "CASH".equals(seg)) {
                equitySTTRecords.add(stt);
            }
        }
        if (equitySTTRecords.isEmpty()) return;

        document.add(new Paragraph()
                .add(new Text("Statement Of Securities Transaction Tax(Cash Transactions) ").setBold())
                .add(new Text("For equity share in a company or a unit of an equity oriented fund"))
                .setFontSize(FONT_SIZE_SMALL).setMarginTop(8).setMarginBottom(4));

        // 16-column Cash STT table
        float[] columnWidths = {0.4f, 1.8f, 0.5f, 0.5f, 0.6f, 0.7f, 0.5f, 0.5f, 0.6f, 0.7f, 0.5f, 0.5f, 0.6f, 0.7f, 0.5f, 0.8f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        // Row 1 spanning headers
        table.addHeaderCell(createSpanningHeader("Sl.No", 2, 1));
        table.addHeaderCell(createSpanningHeader("Security", 2, 1));
        table.addHeaderCell(createSpanningHeader("Segment", 2, 1));
        table.addHeaderCell(createSpanningHeader("Transaction settled by Delivery Purchase", 1, 4));
        table.addHeaderCell(createSpanningHeader("Transaction settled by Delivery Sale", 1, 4));
        table.addHeaderCell(createSpanningHeader("Transaction settled other than By Delivery", 1, 4));
        table.addHeaderCell(createSpanningHeader("Total STT (Rs.)", 2, 1));

        // Row 2 sub-headers (repeated for each group)
        for (int g = 0; g < 3; g++) {
            table.addHeaderCell(createHeaderCellGreen("Quantity"));
            table.addHeaderCell(createHeaderCellGreen("Price"));
            table.addHeaderCell(createHeaderCellGreen("Value"));
            table.addHeaderCell(createHeaderCellGreen("STT"));
        }

        double totalSTT = 0.0;
        int slNo = 1;

        for (STTHeaderTypeModel stt : equitySTTRecords) {
            // Equity STT 18-field record mapped into 11-field STTHeaderTypeModel by mapSTT:
            // expDate=purchaseQty, futureSale=purchasePrice, futureStt=purchaseValue, optionSale=purchaseSTT
            // optionStt=saleQty, totalStt=salePrice (remaining fields truncated by mapper)
            String purchaseQty = safe(stt.getExpDate());
            String purchasePrice = safe(stt.getFutureSale());
            String purchaseValue = safe(stt.getFutureStt());
            String purchaseSTT = safe(stt.getOptionSale());
            String saleQty = safe(stt.getOptionStt());
            String salePrice = safe(stt.getTotalStt());
            // Sale value, sale STT, other delivery fields, and actual totalStt are lost due to mapper truncation
            // Calculate total from what we have
            double pStt = parseDouble(purchaseSTT);
            double rowTotal = pStt; // sale STT not available from mapper
            totalSTT += rowTotal;

            String segDisplay = safe(stt.getExchange()) + " " + safe(stt.getSegment());

            table.addCell(createCell(String.valueOf(slNo++), TextAlignment.CENTER, false));
            table.addCell(createCell(safe(stt.getSecurityDescription()), TextAlignment.LEFT, false));
            table.addCell(createCell(segDisplay, TextAlignment.CENTER, false));

            // Delivery Purchase - from raw data
            table.addCell(createCell(purchaseQty, TextAlignment.RIGHT, false));
            table.addCell(createCell(purchasePrice, TextAlignment.RIGHT, false));
            table.addCell(createCell(purchaseValue, TextAlignment.RIGHT, false));
            table.addCell(createCell(purchaseSTT, TextAlignment.RIGHT, false));

            // Delivery Sale - from raw data
            table.addCell(createCell(saleQty, TextAlignment.RIGHT, false));
            table.addCell(createCell(salePrice, TextAlignment.RIGHT, false));
            table.addCell(createCell("0.00", TextAlignment.RIGHT, false));
            table.addCell(createCell("0.00", TextAlignment.RIGHT, false));

            // Other than delivery - zeros
            table.addCell(createCell("0", TextAlignment.RIGHT, false));
            table.addCell(createCell("0", TextAlignment.RIGHT, false));
            table.addCell(createCell("0.00", TextAlignment.RIGHT, false));
            table.addCell(createCell("0.00", TextAlignment.RIGHT, false));

            table.addCell(createCell(purchaseSTT, TextAlignment.RIGHT, false));
        }

        // Total row
        table.addCell(new Cell(1, 15).add(new Paragraph("Total(Rounded to Nearest Rupee)").setFontSize(FONT_SIZE_TINY)
                .setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));
        table.addCell(new Cell().add(new Paragraph(formatDecimal(Math.round(totalSTT))).setFontSize(FONT_SIZE_TINY)
                .setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));

        document.add(table);
    }

    // ---------------------------------------------------------------
    //  DERIVATIVES STT SECTION
    // ---------------------------------------------------------------

    private static void addSTTSection(Document document) {
        if (sttHeaderTypeModels.isEmpty()) return;

        // Filter only derivatives STT records (exclude equity EN/CAPITAL/CASH)
        List<STTHeaderTypeModel> derivativeSTT = new ArrayList<>();
        for (STTHeaderTypeModel stt : sttHeaderTypeModels) {
            String seg = safe(stt.getSegment()).toUpperCase();
            if (!"EN".equals(seg) && !"CAPITAL".equals(seg) && !"CASH".equals(seg)) {
                derivativeSTT.add(stt);
            }
        }
        if (derivativeSTT.isEmpty()) return;

        Map<String, List<STTHeaderTypeModel>> byExchange = safeGroupBy(derivativeSTT, stt -> {
            String ex = safe(stt.getExchange());
            String seg = safe(stt.getSegment());
            return ex + " " + seg;
        });
        if (byExchange.isEmpty()) return;

        for (Map.Entry<String, List<STTHeaderTypeModel>> entry : byExchange.entrySet()) {
            String exchangeSegment = entry.getKey().trim();
            List<STTHeaderTypeModel> sttList = entry.getValue();
            String exchange = exchangeSegment.split(" ")[0];
            String brokerCode = exchange.contains("NSE") ? "13372" : "0328";

            document.add(new Paragraph("Name Of Exchange & Segment :   " + exchangeSegment + " | " +
                    exchange + " BrokerCode : " + brokerCode)
                    .setFontSize(FONT_SIZE_SMALL).setBold().setMarginTop(8).setMarginBottom(4));

            float[] columnWidths = {0.5f, 2.5f, 0.5f, 1f, 1f, 1f, 1f, 1f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setFontSize(FONT_SIZE_TINY);

            table.addHeaderCell(createSpanningHeader("Sl.No", 2, 1));
            table.addHeaderCell(createSpanningHeader("Security", 2, 1));
            table.addHeaderCell(createSpanningHeader("Expiry Date", 2, 1));
            table.addHeaderCell(createSpanningHeader("Value of Transactions Futures", 1, 2));
            table.addHeaderCell(createSpanningHeader("Value of Transactions Options", 1, 2));
            table.addHeaderCell(createSpanningHeader("Total STT(Rs.)", 2, 1));

            table.addHeaderCell(createHeaderCellGreen("Sale"));
            table.addHeaderCell(createHeaderCellGreen("STT"));
            table.addHeaderCell(createHeaderCellGreen("Sale"));
            table.addHeaderCell(createHeaderCellGreen("STT"));

            double totalSTT = 0.0;
            int slNo = 1;

            for (STTHeaderTypeModel stt : sttList) {
                double rowTotal = parseDouble(stt.getTotalStt());
                totalSTT += rowTotal;

                table.addCell(createCell(String.valueOf(slNo++), TextAlignment.CENTER, false));
                table.addCell(createCell(safe(stt.getSecurityDescription()), TextAlignment.LEFT, false));
                table.addCell(createCell(safe(stt.getExpDate()), TextAlignment.CENTER, false));
                table.addCell(createCell(safe(stt.getFutureSale()), TextAlignment.RIGHT, false));
                table.addCell(createCell(safe(stt.getFutureStt()), TextAlignment.RIGHT, false));
                table.addCell(createCell(safe(stt.getOptionSale()), TextAlignment.RIGHT, false));
                table.addCell(createCell(safe(stt.getOptionStt()), TextAlignment.RIGHT, false));
                table.addCell(createCell(safe(stt.getTotalStt()), TextAlignment.RIGHT, false));
            }

            table.addCell(new Cell(1, 7).add(new Paragraph("Total(Rounded to nearest Rupee)").setFontSize(FONT_SIZE_TINY)
                    .setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));
            table.addCell(new Cell(1, 1).add(new Paragraph(formatDecimal(totalSTT)).setFontSize(FONT_SIZE_TINY)
                    .setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2));

            document.add(table);
        }
    }

    // ---------------------------------------------------------------
    //  PAGE 3: MARGIN STATEMENT
    // ---------------------------------------------------------------

    private static void addMarginStatement(Document document) {
        if (mHeaderTypeModels.isEmpty()) return;

        document.add(new Paragraph("\n").setMarginTop(15));
        document.add(new Paragraph("Daily Margin Statement").setFont(calibriBold).setFontSize(FONT_SIZE_SUBTITLE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));

        Set<String> exchanges = new LinkedHashSet<>();
        for (MHeaderTypeModel m : mHeaderTypeModels) {
            String ex = safe(m.getExchange());
            String seg = safe(m.getSegment());
            // MCX'SX: exchange="MCX", segment="MCX'SX" - use segment directly
            if (seg.contains("'")) {
                exchanges.add(seg); // MCX'SX
            } else if (!ex.isEmpty()) {
                exchanges.add(ex);
            }
        }
        String exchangeList = exchanges.isEmpty() ? "NSE, BSE, MCX'SX" : String.join(", ", exchanges);

        document.add(new Paragraph("Exchange : " + exchangeList).setFont(calibriBold).setFontSize(FONT_SIZE_SMALL)
                .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(8));

        float[] columnWidths = {0.6f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        // Row 1 spanning headers
        table.addHeaderCell(createSpanningHeader("Seg", 2, 1));
        table.addHeaderCell(createSpanningHeader("Trade Day", 2, 1));
        table.addHeaderCell(createSpanningHeader("Margins available till T day", 1, 6));
        table.addHeaderCell(createSpanningHeader("Margin/ Consolidated Crystallized Obligation / MTM required by Exchange/CC end of T\n& T+1 day respectively", 1, 4));
        table.addHeaderCell(createSpanningHeader("Excess / Shortfall\nw.r.t. Requirement\nby Exchange / CC", 2, 1));
        table.addHeaderCell(createSpanningHeader("Additional Margin\nrequired by member\nas per RMS", 2, 1));
        table.addHeaderCell(createSpanningHeader("MarginStatus\n(Balance with\nMember\n/Due\nfromclient)", 2, 1));

        // Row 2 sub-headers
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

        double tFunds=0,tSec=0,tMP=0,tBG=0,tOther=0,tMA=0,tUM=0,tCon=0,tDM=0,tReq=0,tES=0,tAM=0,tMS=0;

        for (MHeaderTypeModel m : mHeaderTypeModels) {
            double funds=parseDouble(m.getFunds()), sec=parseDouble(m.getSecurities()),
                    mp=parseDouble(m.getMarginpledgesecurities()), bg=parseDouble(m.getBankguarantees()),
                    oa=parseDouble(m.getOtherapproved()), ma=parseDouble(m.getTotalmarginsavailable()),
                    um=parseDouble(m.getTotalupfrontmargin()), con=parseDouble(m.getConsolidatedcrystallized()),
                    dm=parseDouble(m.getDeliverymargin()), req=parseDouble(m.getTotalrequirement()),
                    es=parseDouble(m.getExcessshortfall()), am=parseDouble(m.getAdditionalmargin()),
                    ms=parseDouble(m.getMarginstatus());

            tFunds+=funds; tSec+=sec; tMP+=mp; tBG+=bg; tOther+=oa; tMA+=ma;
            tUM+=um; tCon+=con; tDM+=dm; tReq+=req; tES+=es; tAM+=am; tMS+=ms;

            // Display Seg as exchange+segment (e.g., NSEFO, NSECASH, MCX'SX)
            String segDisplay = safe(m.getSegment());
            String exchange = safe(m.getExchange());
            if (!exchange.isEmpty() && !segDisplay.toUpperCase().startsWith(exchange.toUpperCase())) {
                segDisplay = exchange + segDisplay;
            }
            table.addCell(createCell(segDisplay, TextAlignment.LEFT, false));
            table.addCell(createCell(safe(m.getTradeday()), TextAlignment.CENTER, false));
            table.addCell(createCell(formatDecimal(funds), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(sec), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(mp), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(bg), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(oa), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(ma), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(um), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(con), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(dm), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(req), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(es), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(am), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(ms), TextAlignment.RIGHT, false));
        }

        table.addCell(new Cell(1,2).add(new Paragraph("Summary of all Exchanges").setFontSize(FONT_SIZE_TINY).setBold()
                .setTextAlignment(TextAlignment.LEFT)).setBorder(new SolidBorder(ColorConstants.BLACK,1f)).setPadding(2));

        table.addCell(createCellGreenBg(formatDecimal(tFunds),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tSec),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tMP),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tBG),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tOther),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tMA),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tUM),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tCon),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tDM),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tReq),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tES),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tAM),TextAlignment.RIGHT,true));
        table.addCell(createCellGreenBg(formatDecimal(tMS),TextAlignment.RIGHT,true));

        document.add(table);
        addMarginStatementNotes(document);
    }

    // ---------------------------------------------------------------
    //  MARGIN STATEMENT NOTES
    // ---------------------------------------------------------------

    private static void addMarginStatementNotes(Document document) {
        document.add(new Paragraph()
                .add(new Text("* approved form as may be specified by the Exchange/Clearing Corporation /NSCCL/MCX-SXCCL from time to time. ").setFontSize(FONT_SIZE_TINY))
                .add(new Text("# Balance margin available for the day, pending bills will be adjusted with the available balance.").setFontSize(FONT_SIZE_TINY))
                .setMarginTop(5));

        document.add(new Paragraph("1) Settlements not due : 2) For margin reporting, additional T&C signed holdings is considered.").setFontSize(FONT_SIZE_TINY));

        document.add(new Paragraph("For collateral accounting in derivative segments, only the pledge eligible shares as per NSE will be considered. " +
                "Buying Power in FLIP is updated based on the scrip margin specified by GEOJIT INVESTMENTS LTD.").setFontSize(FONT_SIZE_TINY));

        String tradeDate = safe(customer.getTransactionDate());

        document.add(new Paragraph("3) The fund balance is arrived at without considering the funds in Margin Trading (MTF) and pending settlements. " +
                "4) Provisional Penalty for the trade date (" + tradeDate + ") is 0 /-").setFontSize(FONT_SIZE_TINY));

        double nseFO=0,nseCDS=0,mcxCDS=0,bseFO=0,bseCDS=0,mcx=0,ncdex=0,icex=0;

        // Try to read MTM values from C|MTM record first
        boolean foundMTM = false;
        if (caHeaderTypeList != null) {
            for (CHeaderTypeModel c : caHeaderTypeList) {
                String seg = safe(c.getSegment());
                // MTM record: segment field will be a numeric MTM value (not "18% of Rs...")
                // SEBI record has text like "18% of Rs.64.873" in segment
                if (!seg.contains("18%") && !seg.contains("Rs.") && !seg.isEmpty()) {
                    try {
                        // C|MTM record fields (via broken mapC mapping):
                        // segment=NSEFO, scrip_name=NSECDS, buy_qty=MCXCDS,
                        // buy_market_rate=BSEFO, sell_qty=BSECDS, sell_market_rate=MCX,
                        // brokerage=NCDEX, stt=ICEX
                        nseFO = parseDouble(c.getSegment());
                        nseCDS = parseDouble(c.getScrip_name());
                        mcxCDS = parseDouble(c.getBuy_qty());
                        bseFO = parseDouble(c.getBuy_market_rate());
                        bseCDS = parseDouble(c.getSell_qty());
                        mcx = parseDouble(c.getSell_market_rate());
                        ncdex = parseDouble(c.getBrokerage());
                        icex = parseDouble(c.getStt());
                        foundMTM = true;
                        break;
                    } catch (Exception e) {
                        // Not an MTM record, skip
                    }
                }
            }
        }

        // Fallback: calculate from M records if C|MTM not found
        if (!foundMTM) {
            for (MHeaderTypeModel m : mHeaderTypeModels) {
                String seg = safe(m.getSegment()).toUpperCase();
                double mtm = parseDouble(m.getConsolidatedcrystallized());
                if (seg.contains("NSEFO")) nseFO+=mtm;
                else if (seg.contains("NSECDS")) nseCDS+=mtm;
                else if (seg.contains("BSEFO")) bseFO+=mtm;
                else if (seg.contains("BSECDS")) bseCDS+=mtm;
                else if (seg.contains("MCXCDS") || seg.contains("MCX'SX")) mcxCDS+=mtm;
                else if (seg.contains("MCX")) mcx+=mtm;
                else if (seg.contains("NCDEX")) ncdex+=mtm;
                else if (seg.contains("ICEX")) icex+=mtm;
            }
            // If all zeros, try reading from footer net_amount (F records) for FO segments
            if (nseFO == 0 && bseFO == 0) {
                for (FooterModelV2 f : footerList) {
                    String ex = safe(f.getExchange()).toUpperCase();
                    String seg = safe(f.getSegment()).toUpperCase();
                    String rawNet = firstNonEmpty(f.getNet_amount());
                    double netAmt = parseDouble(rawNet);
                    if ("BSE".equals(ex) && "FO".equals(seg)) bseFO = netAmt;
                    else if ("NSE".equals(ex) && "FO".equals(seg)) nseFO = netAmt;
                }
            }
        }

        document.add(new Paragraph("5) MTM/Premium for the trade date (" + tradeDate + ") - " +
                "NSE FO = " + formatDecimal(nseFO) + " /-, NSE CDS = " + formatDecimal(nseCDS) + " /-, " +
                "MCX CDS = " + formatDecimal(mcxCDS) + " /-, BSE FO = " + formatDecimal(bseFO) + " /-, " +
                "BSE CDS = " + formatDecimal(bseCDS) + " /-, MCX = " + formatDecimal(mcx) + " /-, " +
                "NCDEX = " + formatDecimal(ncdex) + " /-, ICEX = " + formatDecimal(icex) + " /- . " +
                "6) All the figures are in Rupee. \"-Ve indicates debit balance, +Ve indicates credit balance\"").setFontSize(FONT_SIZE_TINY));

        document.add(new Paragraph("6)For the purchase of Shares of BSE Ltd/CDSL., you are requested to comply with the prescribed SECC regulation 19 and 20 and related circulars.").setFontSize(FONT_SIZE_TINY));
        document.add(new Paragraph(" For more details,\nhttp://www.geojit.com/equity-products/instructions").setFontSize(FONT_SIZE_TINY));
        document.add(new Paragraph("7)In case of trades/positions in commodity Exchanges, new margin statement will be issued separately.").setFontSize(FONT_SIZE_TINY));
    }

    // ---------------------------------------------------------------
    //  MARGIN PLEDGE SECURITIES (centered title matching reference)
    // ---------------------------------------------------------------

    private static void addMarginPledgeSecurities(Document document) {
        if (pHeaderTypeModels.isEmpty()) return;

        document.add(new Paragraph("Margin Pledge Securities Details").setFont(calibriBold)
                .setFontSize(FONT_SIZE_SUBTITLE)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(12).setMarginBottom(4));

        Table table = new Table(UnitValue.createPercentArray(new float[]{3f, 1f, 1f, 1f, 1.2f}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(FONT_SIZE_TINY);

        table.addHeaderCell(createHeaderCellGreen("Security"));
        table.addHeaderCell(createHeaderCellGreen("Qty"));
        table.addHeaderCell(createHeaderCellGreen("Total Value"));
        table.addHeaderCell(createHeaderCellGreen("HairCut Value"));
        table.addHeaderCell(createHeaderCellGreen("Balance Amount"));

        double totalValue=0, totalHairCut=0, totalBalance=0;

        for (PHeaderTypeModel p : pHeaderTypeModels) {
            double value=parseDouble(p.getTotalValue()), hairCut=parseDouble(p.getHaircutValue()), balance=parseDouble(p.getBalanceAmount());
            totalValue+=value; totalHairCut+=hairCut; totalBalance+=balance;

            table.addCell(createCell(safe(p.getSecurityDescription()), TextAlignment.LEFT, false));
            table.addCell(createCell(formatDecimal(parseDouble(p.getQty())), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(value), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(hairCut), TextAlignment.RIGHT, false));
            table.addCell(createCell(formatDecimal(balance), TextAlignment.RIGHT, false));
        }

        table.addCell(new Cell(1,2).add(new Paragraph("Total").setFontSize(FONT_SIZE_TINY).setBold()
                .setTextAlignment(TextAlignment.RIGHT)).setBorder(new SolidBorder(ColorConstants.BLACK,1f)).setPadding(2));
        table.addCell(createCellGreenBg(formatDecimal(totalValue), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalHairCut), TextAlignment.RIGHT, true));
        table.addCell(createCellGreenBg(formatDecimal(totalBalance), TextAlignment.RIGHT, true));

        document.add(table);
    }

    // ===============================================================
    //  INNER DATA CLASS
    // ===============================================================

    private static class EquityAggregation {
        String securityName;
        String exchange;
        String segment;
        int buyQuantity=0; double buyWAP=0, buyBrokerage=0, buyWAPAfterBrokerage=0, buyTotalValue=0;
        int sellQuantity=0; double sellWAP=0, sellBrokerage=0, sellWAPAfterBrokerage=0, sellTotalValue=0;
    }

    // ===============================================================
    //  DATA AGGREGATION
    // ===============================================================

    private static Map<String, EquityAggregation> aggregateEquityData() {
        Map<String, EquityAggregation> aggregated = new LinkedHashMap<>();

        for (DHeaderTypeModel d : dHeaderTypeList) {
            // Use segment2 (f[21]) which has actual FO/EN; segment (f[3]) is always CAPITAL
            String actualSegment = safe(d.getSegment2()).toUpperCase();
            if (actualSegment.isEmpty()) actualSegment = safe(d.getSegment()).toUpperCase();

            // Skip derivative trades
            boolean isDerivativeSegment = actualSegment.equals("FO") || actualSegment.equals("DERIVATIVES")
                    || actualSegment.contains("FUTURE") || actualSegment.contains("OPTION");
            if (isDerivativeSegment) continue;

            // Extract security name from security_contract_description
            String secDesc = safe(d.getSecurity_contract_description());
            if (secDesc.isEmpty()) continue;

            // Extract ISIN from description if present (e.g. "SAMBHV STEEL TUBES LIMITED - INE12NJ01018")
            String groupKey = secDesc;
            String securityName = secDesc;
            if (secDesc.contains(" - ")) {
                String[] parts = secDesc.split(" - ");
                securityName = parts[0].trim();
                groupKey = parts.length > 1 ? parts[1].trim() : securityName;
            }

            EquityAggregation agg = aggregated.computeIfAbsent(groupKey, k -> new EquityAggregation());
            if (agg.securityName == null || agg.securityName.isEmpty()) agg.securityName = securityName;
            if (agg.exchange == null || agg.exchange.isEmpty()) agg.exchange = safe(d.getExchange());
            if (agg.segment == null || agg.segment.isEmpty()) agg.segment = actualSegment;

            // Use correct getters matching InvokeEquityCombineMarginFile mapD
            int quantity = parseInt(d.getQty());              // f[10]
            double price = parseDouble(d.getMarket_rate());   // f[12]
            double brokerage = parseDouble(d.getBrokerage()); // f[13]
            String buySell = safe(d.getBuy_sell()).toUpperCase(); // f[9]

            if ("B".equals(buySell) || "BUY".equals(buySell)) {
                agg.buyQuantity += quantity;
                double totalValue = quantity * price;
                agg.buyTotalValue += totalValue;
                if (quantity > 0) agg.buyBrokerage = brokerage / quantity;
                agg.buyWAP = (agg.buyQuantity > 0) ? (agg.buyTotalValue / agg.buyQuantity) : 0.0;
                agg.buyWAPAfterBrokerage = agg.buyWAP + agg.buyBrokerage;
            } else if ("S".equals(buySell) || "SELL".equals(buySell)) {
                agg.sellQuantity += quantity;
                double totalValue = quantity * price;
                agg.sellTotalValue += totalValue;
                if (quantity > 0) agg.sellBrokerage = brokerage / quantity;
                agg.sellWAP = (agg.sellQuantity > 0) ? (agg.sellTotalValue / agg.sellQuantity) : 0.0;
                agg.sellWAPAfterBrokerage = agg.sellWAP - agg.sellBrokerage;
            }
        }

        return aggregated;
    }

    // Extract place name from dealing office address (e.g. "...PALARIVATTOM| TELEPHONE...")
    private static String extractPlaceFromDealingOffice() {
        if (dealingOffice == null) return "PALARIVATTOM";
        String addr = safe(dealingOffice.getDealingAddress());
        if (addr.isEmpty()) return safe(dealingOffice.getGstLocation());
        // Extract last location before "| TELEPHONE" or "|"
        String location = addr;
        if (location.contains("|")) {
            location = location.substring(0, location.indexOf("|")).trim();
        }
        // Get last comma-separated part (the place name)
        String[] parts = location.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (!part.isEmpty()) return part;
        }
        return safe(dealingOffice.getGstLocation());
    }

    // Extract GSTIN from dealing office or customer data
    private static String extractGSTIN() {
        if (dealingOffice != null) {
            String gst = safe(dealingOffice.getGstNo());
            if (!gst.isEmpty()) return gst;
        }
        // Fallback: use customer GST
        if (customer != null) {
            String gst = safe(customer.getGstNo());
            if (!gst.isEmpty()) return gst;
        }
        return "32AAKCG3453A1Z4"; // hardcoded fallback
    }

    // ===============================================================
    //  CELL HELPER METHODS
    // ===============================================================

    private static Cell createInfoLabelCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(FONT_SIZE_SMALL))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    private static Cell createInfoValueCell(String text, boolean bold) {
        Paragraph p = new Paragraph(text).setFontSize(FONT_SIZE_SMALL);
        if (bold) p.setFont(calibriBold);
        return new Cell().add(p).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    private static Cell createExchangeHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(FONT_SIZE_TINY).setBold()
                        .setTextAlignment(TextAlignment.CENTER)).setBackgroundColor(HEADER_BG_COLOR)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createExchangeDataCell(String text, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text).setFontSize(FONT_SIZE_SMALL).setTextAlignment(alignment))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createEquityHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(FONT_SIZE_TINY).setBold()
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createEquityDataCell(String content, TextAlignment alignment) {
        return createEquityDataCell(content, alignment, false);
    }

    private static Cell createEquityDataCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content).setFontSize(FONT_SIZE_TINY).setTextAlignment(alignment);
        p.setFont(bold ? calibriBold : calibriNormal);
        return new Cell().add(p).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createDerivativeHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(FONT_SIZE_SMALL).setBold()
                        .setFontColor(ColorConstants.BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createDerivativeDataCell(String content, TextAlignment alignment) {
        return new Cell().add(new Paragraph(content).setFontSize(FONT_SIZE_TINY).setTextAlignment(alignment))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createSpanningHeader(String text, int rowSpan, int colSpan) {
        return new Cell(rowSpan, colSpan)
                .add(new Paragraph(text).setFontSize(FONT_SIZE_TINY).setBold()
                        .setFontColor(ColorConstants.BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createNetObligationHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(FONT_SIZE_TINY).setBold()
                        .setFontColor(ColorConstants.BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createNetObligationDataCell(String content, TextAlignment alignment) {
        return new Cell().add(new Paragraph(content).setFontSize(FONT_SIZE_TINY).setTextAlignment(alignment))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createSummaryCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content).setFontSize(FONT_SIZE_SMALL).setTextAlignment(alignment);
        if (bold) p.setBold();
        return new Cell().add(p).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3);
    }

    // Green header cell matching reference PDF green header style
    private static Cell createHeaderCellGreen(String content) {
        return new Cell().add(new Paragraph(content).setFontSize(FONT_SIZE_TINY).setBold()
                        .setFontColor(ColorConstants.BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(TABLE_HEADER_GREEN)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(3)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createCellGreenBg(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content).setFontSize(FONT_SIZE_TINY).setTextAlignment(alignment);
        if (bold) p.setBold();
        return new Cell().add(p).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private static Cell createCell(String content, TextAlignment alignment, boolean bold) {
        Paragraph p = new Paragraph(content).setFontSize(FONT_SIZE_TINY).setTextAlignment(alignment);
        if (bold) p.setBold();
        return new Cell().add(p).setBorder(new SolidBorder(ColorConstants.BLACK, 1f)).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ===============================================================
    //  UTILITY METHODS
    // ===============================================================

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static <T, K> Map<K, List<T>> safeGroupBy(List<T> list, Function<T, K> keyExtractor) {
        if (list == null || list.isEmpty()) return new LinkedHashMap<>();
        return list.stream().filter(Objects::nonNull).filter(item -> keyExtractor.apply(item) != null)
                .collect(Collectors.groupingBy(keyExtractor, LinkedHashMap::new, Collectors.toList()));
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // Fallback helper: returns the first non-null/non-empty value
    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return "";
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            Double.parseDouble(value.replaceAll(",", "").trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(value.replaceAll(",", "").trim()); }
        catch (NumberFormatException e) { System.err.println("Warning: Could not parse double: " + value); return 0.0; }
    }

    private static int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try { return Integer.parseInt(value.replaceAll(",", "").split("\\.")[0].trim()); }
        catch (NumberFormatException e) { System.err.println("Warning: Could not parse int: " + value); return 0; }
    }

    private static String formatDecimal(double value) { return decimalFormat.format(value); }
    private static String formatDecimal(String value) { return formatDecimal(parseDouble(value)); }
    private static String formatDecimal4(double value) { return decimalFormat4.format(value); }
    private static String formatDecimal4(String value) { return formatDecimal4(parseDouble(value)); }

    // Smart format: strips trailing zeros for natural display (matching reference)
    private static String formatDecimalSmart(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        String s = String.valueOf(value);
        // Remove trailing zeros after decimal point
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }

}