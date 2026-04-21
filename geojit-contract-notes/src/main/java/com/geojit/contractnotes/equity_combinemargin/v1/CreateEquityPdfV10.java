//package com.geojit.contractnotes.geojit_ContractNote;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.security.Security;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.geojit.contractnotes.Model.*;
//import com.geojit.contractnotes.utils.HeaderFooterPageEventV2;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
//import org.json.JSONObject;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import com.geojit.contractnotes.DTO.EquityDtoV2;
//
////import com.geojit.contractnotes.Model.AHeaderTypeModel;
//
//import com.amazonaws.ClientConfiguration;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.model.ObjectMetadata;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//
//import com.itextpdf.text.BaseColor;
//import com.itextpdf.text.Chunk;
//import com.itextpdf.text.Document;
//import com.itextpdf.text.DocumentException;
//import com.itextpdf.text.Element;
//import com.itextpdf.text.Font;
//import com.itextpdf.text.FontFactory;
//import com.itextpdf.text.PageSize;
//import com.itextpdf.text.Paragraph;
//import com.itextpdf.text.Phrase;
//import com.itextpdf.text.Rectangle;
//import com.itextpdf.text.pdf.BaseFont;
//import com.itextpdf.text.pdf.PdfContentByte;
//import com.itextpdf.text.pdf.PdfPCell;
//import com.itextpdf.text.pdf.PdfPTable;
//import com.itextpdf.text.pdf.PdfPageEvent;
//import com.itextpdf.text.pdf.PdfReader;
//import com.itextpdf.text.pdf.PdfSignatureAppearance;
//import com.itextpdf.text.pdf.PdfStamper;
//import com.itextpdf.text.pdf.PdfWriter;
//import com.itextpdf.text.pdf.security.BouncyCastleDigest;
//import com.itextpdf.text.pdf.security.ExternalDigest;
//import com.itextpdf.text.pdf.security.ExternalSignature;
//import com.itextpdf.text.pdf.security.MakeSignature;
//import com.itextpdf.text.pdf.security.PrivateKeySignature;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//import java.security.KeyStore;
//import java.security.PrivateKey;
//import java.security.Provider;
//import java.security.cert.Certificate;
//import java.text.DecimalFormat;
//
//
//public class CreateEquityPdfV10 implements RequestHandler<Object, Integer> {
//
//    // Digital Signature Password
////    public static final char[] PASSWORD = "GeojitPassword".toCharArray();
//
//    private static List<CustomerModel> customerList = new ArrayList<>();
//    private static List<FooterModelV2> footerList = new ArrayList<>();
//    private static List<FOHeaderTypeModel> foHeaderTypeList = new ArrayList<>();
//    private static List<OHeaderTypeModel> oHeaderTypeList = new ArrayList<>();
//    private static List<DHeaderTypeModel> dHeaderTypeList = new ArrayList<>();
//    private static List<DealingOfficeAddress> dealingOfficeAddressList = new ArrayList<>();
//    private static List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList = new ArrayList<>();
//    private static List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList = new ArrayList<>();
//    private static List<AHeaderTypeModel> aHeaderTypeList = new ArrayList<>();
//    private static List<CAHeaderTypeModel> caHeaderTypeList = new ArrayList<>();
//    private static List<SSHeaderTypeModel> ssHeaderTypeModels=new ArrayList<>();
//    private static List<STTHeaderTypeModel> sttHeaderTypeModels= new ArrayList<>();
//    private static List<PHeaderTypeModel> pHeaderTypeModels= new ArrayList<>();
//    private static List<MHeaderTypeModel> mHeaderTypeModels= new ArrayList<>();
//
//    // Font Resources
//    private static ClassLoader classLoader = CreateEquityPdfV10.class.getClassLoader();
//    private static String pathCalibriFont = classLoader.getResource("calibri-400.ttf").getPath();
//    private String pathCalibriFontBold = classLoader.getResource("calibri-Bold.ttf").getPath();
//
//    // Geojit Specific Variables
//    static String contractNoteNo = "";
//    static String tradeDate = "";
//    static String clientName = "";
//    static String clientAddress = "";
//    static String clientPhone = "";
//    static String tradeCode = "";
//    static String placeOfSupply = "";
//    static String invoiceReferenceNumber = "";
//    static String gstIdentificationNo = "";
//    static String clientPan = "";
//
//    // Exchange and Settlement Information
//    static String bseExchange = "";
//    static String bseSegment = "";
//    static String bseSettlementNo = "";
//    static String bseSettlementDate = "";
//    static String bseUCCode = "";
//
//    static String nseExchange = "";
//    static String nseSegment = "";
//    static String nseSettlementNo = "";
//    static String nseSettlementDate = "";
//    static String nseUCCode = "";
//
//    // Dealing Office Information
//    static String dealingOfficeAddress = "";
//    static String dealingOfficeTelephone = "";
//
////     Company Information
//    static String companyName = "GEOJIT INVESTMENTS LTD";
//    static String sebiRegNo = "INZ000318938";
//    static String cinNo = "U66110KL2023PLC080586";
//    static String companyAddress = "7TH FLOOR, 34/659-P, CIVIL LINE ROAD,PADIVATTOM,KOCHI- 682024";
//    static String companyTel = "0484-2901000";
//    static String companyFax = "0484 2979695";
//    static String companyWebsite = "www.geojit.com/gil";
//    static String complianceOfficer = "Ancy C Sunny";
//    static String complianceEmail = "compliance@geojit.com";
//    static String complianceTel = "0484-2901000";
//    static String grievanceEmail = "grievances@geojit.com";
//
//    // Footer Information
//    static String companyPan = "AAKCG3453A";
//    static String companyGSTIN = "32AAKCG3453A1Z4";
//    static String serviceDescription = "STOCK BROKER";
//    static String serviceAccountCode = "997152";
//    static String authorizedSignatory = "Johny Varghese";
//
//    // Calculation Variables
//    static Double totalBrokerage = 0.0;
//    static Double totalSTT = 0.0;
//    static Double totalSGST = 0.0;
//    static Double totalCGST = 0.0;
//    static Double totalIGST = 0.0;
//    static Double totalTDS = 0.0;
//    static Double totalExchangeCharges = 0.0;
//    static Double totalSEBIFees = 0.0;
//    static Double totalStampDuty = 0.0;
//    static Double netAmountReceivable = 0.0;
//    static Double totalPayInObligation = 0.0;
//
//    // Equity Segment Variables
//    static int equityRecordCount = 0;
//    static boolean hasEquitySegment = false;
//
//    // Derivative Segment Variables
//    static int derivativeRecordCount = 0;
//    static boolean hasDerivativeSegment = false;
//
//    // Trade Details Variables
//    static int tradeDetailsCount = 0;
//
//    // Margin Statement Variables
//    static boolean hasMarginStatement = false;
//    static int marginRecordCount = 0;
//
//    // Page Numbers
//    static int currentPageNumber = 1;
//
//    // Decimal Formatters
//    private static DecimalFormat df2 = new DecimalFormat("#,##0.00");
//    private static DecimalFormat df4 = new DecimalFormat("#,##0.0000");
//    private static DecimalFormat dfInteger = new DecimalFormat("#,##0");
//
//    @Override
//    public Integer handleRequest(Object input, Context context) {
//        System.out.println("========== Geojit PDF Generation Started ==========");
//
//        try {
//            // Parse input JSON
//            ObjectMapper objectMapper = new ObjectMapper();
//            EquityDtoV2 equityDto = objectMapper.convertValue(input, EquityDtoV2.class);
//
//            System.out.println("Input JSON received: " + equityDto.toString());
//
//            // Extract data from DTO
//            extractDataFromDTO(equityDto);
//
//            // Generate PDF
//            String tempPdfPath = "/tmp/geojit_contract_note_temp.pdf";
//            String signedPdfPath = "/tmp/geojit_contract_note_signed.pdf";
//
//            // Create unsigned PDF
//            createPDF(tempPdfPath);
//
//            // Sign PDF
//            signPDF(tempPdfPath, signedPdfPath);
//
//            // Upload to S3
//            uploadToS3(signedPdfPath, equityDto);
//
//            System.out.println("========== Geojit PDF Generation Completed ==========");
//            return 200;
//
//        } catch (Exception e) {
//            System.err.println("Error in Geojit PDF generation: " + e.getMessage());
//            e.printStackTrace();
//            return 500;
//        }
//    }
//
//    /**
//     * Extract data from EquityDtoV2 and populate model lists
//     */
//    private void extractDataFromDTO(EquityDtoV2 equityDto) {
//        System.out.println("Extracting data from DTO...");
//
//        // Clear existing data
//        customerList.clear();
//        footerList.clear();
//        foHeaderTypeList.clear();
//        oHeaderTypeList.clear();
//        dHeaderTypeList.clear();
//        dealingOfficeAddressList.clear();
//        sCapitalHeaderTypeList.clear();
//        sFuturesHeaderTypeList.clear();
//        aHeaderTypeList.clear();
//        caHeaderTypeList.clear();
//
//        // Reset calculation variables
//        totalBrokerage = 0.0;
//        totalSTT = 0.0;
//        totalSGST = 0.0;
//        totalCGST = 0.0;
//        totalIGST = 0.0;
//        totalTDS = 0.0;
//        totalExchangeCharges = 0.0;
//        totalSEBIFees = 0.0;
//        totalStampDuty = 0.0;
//        netAmountReceivable = 0.0;
//        totalPayInObligation = 0.0;
//        equityRecordCount = 0;
//        derivativeRecordCount = 0;
//        tradeDetailsCount = 0;
//        hasEquitySegment = false;
//        hasDerivativeSegment = false;
//        hasMarginStatement = false;
//        marginRecordCount = 0;
//
//        // Populate lists from DTO
//        customerList = equityDto.getCustomerList();
//        footerList = equityDto.getFooterList();
//        foHeaderTypeList = equityDto.getFoHeaderTypeList();
//        oHeaderTypeList = equityDto.getoHeaderTypeList();
//        dHeaderTypeList = equityDto.getdHeaderTypeList();
//        dealingOfficeAddressList = equityDto.getDealingOfficeAddressList();
//        sCapitalHeaderTypeList = equityDto.getsCapitalHeaderTypeList();
//        sFuturesHeaderTypeList = equityDto.getsFuturesHeaderTypeList();
//        aHeaderTypeList = equityDto.getaHeaderTypeList();
//        caHeaderTypeList = equityDto.getCaHeaderTypeList();
//
//        // Extract customer information
//        if (customerList != null && !customerList.isEmpty()) {
//            CustomerModel customer = customerList.get(0);
//            contractNoteNo = customer.getContractnote() != null ? customer.getContractnote() : "";
//            tradeDate = customer.getTradedate() != null ? customer.getTradedate() : "";
//            clientName = customer.getName() != null ? customer.getName() : "";
//            clientAddress = formatClientAddress(customer);
//            clientPhone = customer.getPhone() != null ? customer.getPhone() : "";
//            tradeCode = customer.getPartycode() != null ? customer.getPartycode() : "";
//            clientPan = customer.getPanno() != null ? customer.getPanno() : "";
//            placeOfSupply = customer.getStatename() != null ? customer.getStatename() + "[" + customer.getStatecode() + "]" : "";
//            gstIdentificationNo = customer.getGstin() != null ? customer.getGstin() : "";
//        }
//
//        // Extract dealing office information
//        if (dealingOfficeAddressList != null && !dealingOfficeAddressList.isEmpty()) {
//            DealingOfficeAddress office = dealingOfficeAddressList.get(0);
//            dealingOfficeAddress = formatDealingOfficeAddress(office);
//            dealingOfficeTelephone = office.getTelephone() != null ? office.getTelephone() : "";
//        }
//
//        // Determine segments present
//        hasEquitySegment = (dHeaderTypeList != null && !dHeaderTypeList.isEmpty()) ||
//                (sCapitalHeaderTypeList != null && !sCapitalHeaderTypeList.isEmpty());
//
//        hasDerivativeSegment = (foHeaderTypeList != null && !foHeaderTypeList.isEmpty()) ||
//                (oHeaderTypeList != null && !oHeaderTypeList.isEmpty()) ||
//                (sFuturesHeaderTypeList != null && !sFuturesHeaderTypeList.isEmpty());
//
//        System.out.println("Data extraction completed.");
//        System.out.println("Contract Note: " + contractNoteNo);
//        System.out.println("Client: " + clientName);
//        System.out.println("Has Equity Segment: " + hasEquitySegment);
//        System.out.println("Has Derivative Segment: " + hasDerivativeSegment);
//    }
//
//    /**
//     * Format client address from CustomerModel
//     */
//    private String formatClientAddress(CustomerModel customer) {
//        StringBuilder address = new StringBuilder();
//
//        if (customer.getAddress1() != null && !customer.getAddress1().isEmpty()) {
//            address.append(customer.getAddress1());
//        }
//        if (customer.getAddress2() != null && !customer.getAddress2().isEmpty()) {
//            if (address.length() > 0) address.append(",\n");
//            address.append(customer.getAddress2());
//        }
//        if (customer.getAddress3() != null && !customer.getAddress3().isEmpty()) {
//            if (address.length() > 0) address.append(",\n");
//            address.append(customer.getAddress3());
//        }
//        if (customer.getCity() != null && !customer.getCity().isEmpty()) {
//            if (address.length() > 0) address.append(",\n");
//            address.append(customer.getCity());
//        }
//        if (customer.getPincode() != null && !customer.getPincode().isEmpty()) {
//            address.append(" PINCODE : ").append(customer.getPincode());
//        }
//
//        return address.toString();
//    }
//
//    /**
//     * Format dealing office address from DealingOfficeAddress model
//     */
//    private String formatDealingOfficeAddress(DealingOfficeAddress office) {
//        StringBuilder address = new StringBuilder();
//
//        if (office.getAddress1() != null && !office.getAddress1().isEmpty()) {
//            address.append(office.getAddress1());
//        }
//        if (office.getAddress2() != null && !office.getAddress2().isEmpty()) {
//            if (address.length() > 0) address.append(" ,,");
//            address.append(office.getAddress2());
//        }
//        if (office.getAddress3() != null && !office.getAddress3().isEmpty()) {
//            if (address.length() > 0) address.append(",,");
//            address.append(office.getAddress3());
//        }
//        if (office.getAddress4() != null && !office.getAddress4().isEmpty()) {
//            if (address.length() > 0) address.append(",,");
//            address.append(office.getAddress4());
//        }
//
//        return address.toString();
//    }
//
//    /**
//     * Create the main PDF document
//     */
//    private void createPDF(String outputPath) throws Exception {
//        System.out.println("Creating PDF at: " + outputPath);
//        // Initialize document
//        Document document = new Document(PageSize.A4);
//        document.setMargins(20, 20, 40, 20);
//
//        FileOutputStream fos = new FileOutputStream(outputPath);
//        PdfWriter writer = PdfWriter.getInstance(document, fos);
//
//        // Set page event for header/footer
//        HeaderFooterPageEventV2 event = new HeaderFooterPageEventV2();
//        writer.setPageEvent(event);
//
//        document.open();
//
//        try {
//            // PAGE 1: Main Contract Note
//            addPage1Header(document);
//            addPage1ClientInformation(document);
//            addPage1EquitySegment(document);
//            addPage1DerivativeSegment(document);
//            addPage1ExchangeSummary(document);
//            addPage1FooterNotes(document);
//            addPage1Signature(document);
//            addPage1Disclaimer(document);
//
//            // PAGE 2: Annexure - Trade Details
//            document.newPage();
//            addPage2TradeDetailsAnnexure(document);
//
//            // PAGE 3: Daily Margin Statement
//            if (hasMarginStatement) {
//                document.newPage();
//                addPage3MarginStatement(document);
//            }
//
//        } finally {
//            document.close();
//            fos.close();
//        }
//
//        System.out.println("PDF created successfully.");
//    }
//
//    /**
//     * PAGE 1 - HEADER SECTION
//     * Creates the top header with company information
//     */
//    private void addPage1Header(Document document) throws Exception {
//        System.out.println("Adding Page 1 Header...");
//
//        BaseFont calibriFont = BaseFont.createFont(pathCalibriFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//        BaseFont calibriFontBold = BaseFont.createFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//
//        // Row 1: Title and "ORIGINAL FOR RECIPIENT"
//        PdfPTable row1Table = new PdfPTable(2);
//        row1Table.setWidthPercentage(100);
//        row1Table.setWidths(new float[]{3.5f, 1.5f});
//
//        // Title Cell
//        PdfPCell titleCell = new PdfPCell();
//        titleCell.setBorder(Rectangle.BOX);
//        titleCell.setBorderWidth(1f);
//        titleCell.setPadding(8f);
//        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        Paragraph titlePara = new Paragraph();
//        Chunk title1 = new Chunk("CONTRACT NOTE CUM TAX INVOICE\n", new Font(calibriFontBold, 16, Font.BOLD));
//        Chunk title2 = new Chunk("(Tax Invoice under Section 31 of GST Act)", new Font(calibriFont, 9, Font.NORMAL));
//        titlePara.add(title1);
//        titlePara.add(title2);
//        titlePara.setAlignment(Element.ALIGN_CENTER);
//        titleCell.addElement(titlePara);
//
//        // Original Cell
//        PdfPCell originalCell = new PdfPCell();
//        originalCell.setBorder(Rectangle.BOX);
//        originalCell.setBorderWidth(1f);
//        originalCell.setPadding(8f);
//        originalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        Paragraph originalPara = new Paragraph("ORIGINAL FOR RECIPIENT", new Font(calibriFontBold, 10, Font.BOLD));
//        originalPara.setAlignment(Element.ALIGN_CENTER);
//        originalCell.addElement(originalPara);
//
//        row1Table.addCell(titleCell);
//        row1Table.addCell(originalCell);
//        document.add(row1Table);
//
//        // Row 2: Company Information
//        PdfPTable companyTable = new PdfPTable(1);
//        companyTable.setWidthPercentage(100);
//        companyTable.setSpacingBefore(0f);
//
//        PdfPCell companyCell = new PdfPCell();
//        companyCell.setBorder(Rectangle.BOX);
//        companyCell.setBorderWidth(1f);
//        companyCell.setPadding(6f);
//
//        Paragraph companyPara = new Paragraph();
//        companyPara.setAlignment(Element.ALIGN_CENTER);
//
//        Chunk companyNameChunk = new Chunk(companyName + "\n", new Font(calibriFontBold, 12, Font.BOLD));
//        Chunk sebiChunk = new Chunk("SEBI REGISTRATION NO : " + sebiRegNo + " | CIN No : " + cinNo + "\n",
//                new Font(calibriFont, 7.5f, Font.NORMAL));
//        Chunk addressChunk = new Chunk(companyAddress + " | TEL: " + companyTel + " | FAX:" + companyFax +
//                " | Website: " + companyWebsite, new Font(calibriFont, 6.5f, Font.NORMAL));
//
//        companyPara.add(companyNameChunk);
//        companyPara.add(sebiChunk);
//        companyPara.add(addressChunk);
//
//        companyCell.addElement(companyPara);
//        companyTable.addCell(companyCell);
//        document.add(companyTable);
//
//        // Row 3: Compliance Officer
//        PdfPTable complianceTable = new PdfPTable(1);
//        complianceTable.setWidthPercentage(100);
//        complianceTable.setSpacingBefore(0f);
//
//        PdfPCell complianceCell = new PdfPCell();
//        complianceCell.setBorder(Rectangle.BOX);
//        complianceCell.setBorderWidth(1f);
//        complianceCell.setPadding(4f);
//
//        Paragraph compliancePara = new Paragraph(
//                "NAME OF THE COMPLIANCE OFFICER : " + complianceOfficer + "| EMAIL:" + complianceEmail +
//                        " | TEL: " + complianceTel + " | EMAIL ID FOR INVESTOR COMPLAINT:" + grievanceEmail,
//                new Font(calibriFont, 6.5f, Font.NORMAL)
//        );
//        compliancePara.setAlignment(Element.ALIGN_CENTER);
//        complianceCell.addElement(compliancePara);
//
//        complianceTable.addCell(complianceCell);
//        document.add(complianceTable);
//
//        // Row 4: Dealing Office Address
//        PdfPTable dealingTable = new PdfPTable(1);
//        dealingTable.setWidthPercentage(100);
//        dealingTable.setSpacingBefore(0f);
//
//        PdfPCell dealingCell = new PdfPCell();
//        dealingCell.setBorder(Rectangle.BOX);
//        dealingCell.setBorderWidth(1f);
//        dealingCell.setPadding(4f);
//
//        Paragraph dealingPara = new Paragraph(
//                "DEALING OFFICES ADDRESS : " + dealingOfficeAddress + "| TELEPHONE NO: " + dealingOfficeTelephone,
//                new Font(calibriFont, 6.5f, Font.NORMAL)
//        );
//        dealingPara.setAlignment(Element.ALIGN_CENTER);
//        dealingCell.addElement(dealingPara);
//
//        dealingTable.addCell(dealingCell);
//        document.add(dealingTable);
//
//        System.out.println("Page 1 Header completed.");
//    }
//
//    /**
//     * PAGE 1 - CLIENT INFORMATION SECTION
//     * Left side: Client details, Right side: Exchange information
//     */
//    private void addPage1ClientInformation(Document document) throws Exception {
//        System.out.println("Adding Page 1 Client Information...");
//
//        BaseFont calibriFont = BaseFont.createFont(pathCalibriFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//        BaseFont calibriFontBold = BaseFont.createFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//
//        document.add(Chunk.NEWLINE);
//
//        // Main table with 2 columns (Left: Client Info, Right: Exchange Info)
//        PdfPTable mainTable = new PdfPTable(2);
//        mainTable.setWidthPercentage(100);
//        mainTable.setWidths(new float[]{2f, 3f});
//        mainTable.setSpacingBefore(3f);
//
//        // ===== LEFT SECTION: Client Information =====
//        PdfPTable leftTable = new PdfPTable(2);
//        leftTable.setWidthPercentage(100);
//        leftTable.setWidths(new float[]{1.3f, 1.7f});
//
//        // Contract Note No
//        leftTable.addCell(createLabelCell("CONTRACT NOTE NO :", calibriFont));
//        leftTable.addCell(createValueCell(contractNoteNo, calibriFontBold, true));
//
//        // Client Name
//        leftTable.addCell(createLabelCell("Name Of the Client :", calibriFont));
//        leftTable.addCell(createValueCell(clientName, calibriFontBold, true));
//
//        // Client Address
//        leftTable.addCell(createLabelCell("Address of the Client :", calibriFont));
//        leftTable.addCell(createValueCell(clientAddress, calibriFont, false));
//
//        // Phone No
//        leftTable.addCell(createLabelCell("Phone No :", calibriFont));
//        leftTable.addCell(createValueCell(clientPhone, calibriFont, false));
//
//        // Trade Code/UCC
//        leftTable.addCell(createLabelCell("TradeCode/UCC of Client :", calibriFont));
//        leftTable.addCell(createValueCell(tradeCode, calibriFont, false));
//
//        // Place of Supply
//        leftTable.addCell(createLabelCell("Place Of Supply [State Code] :", calibriFont));
//        leftTable.addCell(createValueCell(placeOfSupply, calibriFont, false));
//
//        // Invoice Reference Number (IRN)
//        leftTable.addCell(createLabelCell("Invoice Reference Number(IRN) :", calibriFont));
//        leftTable.addCell(createValueCell(invoiceReferenceNumber, calibriFont, false));
//
//        // GST Identification No
//        leftTable.addCell(createLabelCell("GST Identification No. :", calibriFont));
//        leftTable.addCell(createValueCell(gstIdentificationNo, calibriFont, false));
//
//        // ===== RIGHT SECTION: Exchange Information =====
//        PdfPTable rightTable = new PdfPTable(5);
//        rightTable.setWidthPercentage(100);
//        rightTable.setWidths(new float[]{2.5f, 1f, 1f, 1.2f, 1f});
//
//        // Trade Date Header (spanning all columns)
//        PdfPCell tradeDateHeader = new PdfPCell();
//        tradeDateHeader.setColspan(5);
//        tradeDateHeader.setBorder(Rectangle.BOX);
//        tradeDateHeader.setBorderWidth(0.5f);
//        tradeDateHeader.setPadding(4f);
//        tradeDateHeader.setBackgroundColor(new BaseColor(240, 240, 240));
//
//        Paragraph tradeDatePara = new Paragraph("TRADE DATE : " + tradeDate, new Font(calibriFontBold, 8, Font.BOLD));
//        tradeDatePara.setAlignment(Element.ALIGN_LEFT);
//        tradeDateHeader.addElement(tradeDatePara);
//        rightTable.addCell(tradeDateHeader);
//
//        // Column Headers
//        rightTable.addCell(createExchangeHeaderCell("EXCHANGE /\nCLEARING\nCORPORATION", calibriFontBold));
//        rightTable.addCell(createExchangeHeaderCell("SEGMENT", calibriFontBold));
//        rightTable.addCell(createExchangeHeaderCell("STTLNO", calibriFontBold));
//        rightTable.addCell(createExchangeHeaderCell("STTLDATE", calibriFontBold));
//        rightTable.addCell(createExchangeHeaderCell("UCCODE", calibriFontBold));
//
//        // Extract exchange information from footer list
//        extractExchangeInformation();
//
//        // BSE Row
//        rightTable.addCell(createExchangeDataCell(bseExchange, calibriFont, Element.ALIGN_LEFT));
//        rightTable.addCell(createExchangeDataCell(bseSegment, calibriFont, Element.ALIGN_CENTER));
//        rightTable.addCell(createExchangeDataCell(bseSettlementNo, calibriFont, Element.ALIGN_CENTER));
//        rightTable.addCell(createExchangeDataCell(bseSettlementDate, calibriFont, Element.ALIGN_CENTER));
//        rightTable.addCell(createExchangeDataCell(bseUCCode, calibriFont, Element.ALIGN_CENTER));
//
//        // NSE Row
//        rightTable.addCell(createExchangeDataCell(nseExchange, calibriFont, Element.ALIGN_LEFT));
//        rightTable.addCell(createExchangeDataCell(nseSegment, calibriFont, Element.ALIGN_CENTER));
//        rightTable.addCell(createExchangeDataCell(nseSettlementNo, calibriFont, Element.ALIGN_CENTER));
//        rightTable.addCell(createExchangeDataCell(nseSettlementDate, calibriFont, Element.ALIGN_CENTER));
//        rightTable.addCell(createExchangeDataCell(nseUCCode, calibriFont, Element.ALIGN_CENTER));
//
//        // Add both sections to main table
//        PdfPCell leftCell = new PdfPCell(leftTable);
//        leftCell.setBorder(Rectangle.NO_BORDER);
//        leftCell.setPadding(0);
//
//        PdfPCell rightCell = new PdfPCell(rightTable);
//        rightCell.setBorder(Rectangle.NO_BORDER);
//        rightCell.setPadding(0);
//
//        mainTable.addCell(leftCell);
//        mainTable.addCell(rightCell);
//
//        document.add(mainTable);
//
//        // Transaction confirmation text with PAN
//        Paragraph confirmPara = new Paragraph();
//        confirmPara.setSpacingBefore(6f);
//        confirmPara.setSpacingAfter(6f);
//
//        Chunk confirmText1 = new Chunk("Sir/Madam, I / We have this day done by your order and on your account the following transactions: ",
//                new Font(calibriFont, 7.5f, Font.NORMAL));
//        Chunk confirmText2 = new Chunk("PAN of the Client : " + clientPan,
//                new Font(calibriFontBold, 7.5f, Font.BOLD));
//
//        confirmPara.add(confirmText1);
//        confirmPara.add(confirmText2);
//
//        document.add(confirmPara);
//
//        System.out.println("Page 1 Client Information completed.");
//    }
//
//    /**
//     * Extract exchange information from footer list
//     */
//    private void extractExchangeInformation() {
//        // Initialize with empty values
//        bseExchange = "";
//        bseSegment = "";
//        bseSettlementNo = "";
//        bseSettlementDate = "";
//        bseUCCode = "";
//
//        nseExchange = "";
//        nseSegment = "";
//        nseSettlementNo = "";
//        nseSettlementDate = "";
//        nseUCCode = "";
//
//        if (footerList != null && !footerList.isEmpty()) {
//            for (FooterModelV2 footer : footerList) {
//                String exchangeName = footer.getExchange() != null ? footer.getExchange().trim() : "";
//
//                if (exchangeName.toUpperCase().contains("BSE")) {
//                    bseExchange = "BSE / NCL";
//                    bseSegment = footer.getSegment() != null ? footer.getSegment() : "";
////                    bseSettlementNo = footer.getn() != null ? footer.getSettlementno() : "";
//                    bseSettlementDate = footer.getSettlementdate() != null ? footer.getSettlementdate() : "";
//                    bseUCCode = tradeCode; // Use customer's trade code
//                } else if (exchangeName.toUpperCase().contains("NSE")) {
//                    nseExchange = "NSE / NCL";
//                    nseSegment = footer.getSegment() != null ? footer.getSegment() : "";
//                    nseSettlementNo = footer.getSettlementno() != null ? footer.getSettlementno() : "";
//                    nseSettlementDate = footer.getSettlementdate() != null ? footer.getSettlementdate() : "";
//                    nseUCCode = tradeCode; // Use customer's trade code
//                }
//            }
//        }
//    }
//
//    /**
//     * Create label cell for client information section
//     */
//    private PdfPCell createLabelCell(String text, BaseFont font) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(4f);
//        cell.setBackgroundColor(new BaseColor(248, 248, 248));
//
//        Paragraph para = new Paragraph(text, new Font(font, 7.5f, Font.NORMAL));
//        para.setAlignment(Element.ALIGN_LEFT);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * Create value cell for client information section
//     */
//    private PdfPCell createValueCell(String text, BaseFont font, boolean bold) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(4f);
//
//        int fontStyle = bold ? Font.BOLD : Font.NORMAL;
//        Paragraph para = new Paragraph(text != null ? text : "", new Font(font, 7.5f, fontStyle));
//        para.setAlignment(Element.ALIGN_LEFT);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * Create exchange header cell
//     */
//    private PdfPCell createExchangeHeaderCell(String text, BaseFont font) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setBackgroundColor(new BaseColor(230, 230, 230));
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        Paragraph para = new Paragraph(text, new Font(font, 7f, Font.BOLD));
//        para.setAlignment(Element.ALIGN_CENTER);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * Create exchange data cell
//     */
//    private PdfPCell createExchangeDataCell(String text, BaseFont font, int alignment) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        Paragraph para = new Paragraph(text != null ? text : "", new Font(font, 7.5f, Font.NORMAL));
//        para.setAlignment(alignment);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * PAGE 1 - EQUITY SEGMENT
//     * Displays equity transactions
//     */
//    private void addPage1EquitySegment(Document document) throws Exception {
//        System.out.println("Adding Page 1 Equity Segment...");
//
//        if (!hasEquitySegment) {
//            System.out.println("No equity transactions found. Skipping equity segment.");
//            return;
//        }
//
//        BaseFont calibriFont = BaseFont.createFont(pathCalibriFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//        BaseFont calibriFontBold = BaseFont.createFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//
//        // Equity Segment Title
//        Paragraph equityTitle = new Paragraph("Equity Segment", new Font(calibriFontBold, 10, Font.BOLD));
//        equityTitle.setSpacingBefore(8f);
//        equityTitle.setSpacingAfter(4f);
//        document.add(equityTitle);
//
//        // Create equity table with 11 columns
//        PdfPTable equityTable = new PdfPTable(11);
//        equityTable.setWidthPercentage(100);
//        equityTable.setWidths(new float[]{1.8f, 2.5f, 0.8f, 0.9f, 0.9f, 1f, 1.2f, 0.8f, 0.9f, 0.9f, 1f});
//
//        // Header Row 1: Main Headers
//        equityTable.addCell(createEquityHeaderCell("ISIN", calibriFontBold, 2, 1));
//        equityTable.addCell(createEquityHeaderCell("Security\nName /\nSymbol", calibriFontBold, 2, 1));
//        equityTable.addCell(createEquityHeaderCell("Buy", calibriFontBold, 1, 4));
//        equityTable.addCell(createEquityHeaderCell("Sell", calibriFontBold, 1, 4));
//        equityTable.addCell(createEquityHeaderCell("Net\nQuantity", calibriFontBold, 2, 1));
//        equityTable.addCell(createEquityHeaderCell("Net Obligation\nfor ISIN", calibriFontBold, 2, 1));
//
//        // Header Row 2: Sub Headers
//        equityTable.addCell(createEquityHeaderCell("Quantity", calibriFontBold, 1, 1));
//        equityTable.addCell(createEquityHeaderCell("WAP\n(across\nexchanges)", calibriFontBold, 1, 1));
//        equityTable.addCell(createEquityHeaderCell("Brokerage\nPer Share\n(Rs)", calibriFontBold, 1, 1));
//        equityTable.addCell(createEquityHeaderCell("WAP\nafter\nbrokerage", calibriFontBold, 1, 1));
//
//        equityTable.addCell(createEquityHeaderCell("Quantity", calibriFontBold, 1, 1));
//        equityTable.addCell(createEquityHeaderCell("WAP\n(across\nexchanges)", calibriFontBold, 1, 1));
//        equityTable.addCell(createEquityHeaderCell("Brokerage\nPer Share\n(Rs)", calibriFontBold, 1, 1));
//        equityTable.addCell(createEquityHeaderCell("WAP\nafter\nbrokerage", calibriFontBold, 1, 1));
//
//        // Process equity data from dHeaderTypeList
//        if (dHeaderTypeList != null && !dHeaderTypeList.isEmpty()) {
//            for (DHeaderTypeModel equity : dHeaderTypeList) {
//                addEquityDataRow(equityTable, equity, calibriFont);
//                equityRecordCount++;
//            }
//        }
//
//        // Process equity data from sCapitalHeaderTypeList
//        if (sCapitalHeaderTypeList != null && !sCapitalHeaderTypeList.isEmpty()) {
//            for (SCapitalHeaderTypeModel equity : sCapitalHeaderTypeList) {
//                addEquityDataRowFromSCapital(equityTable, equity, calibriFont);
//                equityRecordCount++;
//            }
//        }
//
//        document.add(equityTable);
//
//        // Add note below equity table
//        Paragraph equityNote = new Paragraph(
//                "* Exchange-wise details of orders and trades are provided in separate annexure.",
//                new Font(calibriFont, 6.5f, Font.NORMAL)
//        );
//        equityNote.setSpacingBefore(2f);
//        equityNote.setSpacingAfter(6f);
//        document.add(equityNote);
//
//        System.out.println("Page 1 Equity Segment completed. Records: " + equityRecordCount);
//    }
//
//    /**
//     * Create equity header cell with rowspan and colspan
//     */
//    private PdfPCell createEquityHeaderCell(String text, BaseFont font, int rowspan, int colspan) {
//        PdfPCell cell = new PdfPCell();
//        cell.setRowspan(rowspan);
//        cell.setColspan(colspan);
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setBackgroundColor(new BaseColor(220, 220, 220));
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//
//        Paragraph para = new Paragraph(text, new Font(font, 6.5f, Font.BOLD));
//        para.setAlignment(Element.ALIGN_CENTER);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * Add equity data row from DHeaderTypeModel
//     */
//    private void addEquityDataRow(PdfPTable table, DHeaderTypeModel equity, BaseFont font) {
//        // ISIN
//        table.addCell(createEquityDataCell(
//                equity.getBroker_code() != null ? equity.getBroker_code() : "",
//                font, Element.ALIGN_LEFT
//        ));
//
//        // Security Name
//        table.addCell(createEquityDataCell(
//                equity.getis() != null ? equity.getScripname() : "",
//                font, Element.ALIGN_LEFT
//        ));
//
//        // Buy Quantity
//        table.addCell(createEquityDataCell(
//                equity.getBuyqty() != null ? equity.getBuyqty() : "0",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Buy WAP
//        table.addCell(createEquityDataCell(
//                equity.getBuymarketrate() != null ? formatDecimal4(equity.getBuymarketrate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Buy Brokerage
//        table.addCell(createEquityDataCell(
//                equity.getBuybrokerage() != null ? formatDecimal4(equity.getBuybrokerage()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Buy WAP After Brokerage
//        Double buyWAP = parseDouble(equity.getBuymarketrate());
//        Double buyBrok = parseDouble(equity.getBuybrokerage());
//        Double buyWAPAfter = buyWAP + buyBrok;
//        table.addCell(createEquityDataCell(
//                formatDecimal4(buyWAPAfter.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Sell Quantity
//        table.addCell(createEquityDataCell(
//                equity.getSellqty() != null ? equity.getSellqty() : "0",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Sell WAP
//        table.addCell(createEquityDataCell(
//                equity.getSellmarketrate() != null ? formatDecimal4(equity.getSellmarketrate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Sell Brokerage
//        table.addCell(createEquityDataCell(
//                equity.getSellbrokerage() != null ? formatDecimal4(equity.getSellbrokerage()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Sell WAP After Brokerage
//        Double sellWAP = parseDouble(equity.getSellmarketrate());
//        Double sellBrok = parseDouble(equity.getSellbrokerage());
//        Double sellWAPAfter = sellWAP - sellBrok;
//        table.addCell(createEquityDataCell(
//                formatDecimal4(sellWAPAfter.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Net Quantity
//        int buyQty = parseInt(equity.getBuyqty());
//        int sellQty = parseInt(equity.getSellqty());
//        int netQty = buyQty - sellQty;
//        table.addCell(createEquityDataCell(
//                String.valueOf(netQty),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Net Obligation
//        Double buyTotal = buyQty * buyWAPAfter;
//        Double sellTotal = sellQty * sellWAPAfter;
//        Double netObligation = -(buyTotal - sellTotal);
//        table.addCell(createEquityDataCell(
//                formatDecimal2(netObligation.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Update totals
//        totalPayInObligation += netObligation;
//    }
//
//    /**
//     * Add equity data row from SCapitalHeaderTypeModel
//     */
//    private void addEquityDataRowFromSCapital(PdfPTable table, SCapitalHeaderTypeModel equity, BaseFont font) {
//        // ISIN
//        table.addCell(createEquityDataCell(
//                equity.getIsincode() != null ? equity.getIsincode() : "",
//                font, Element.ALIGN_LEFT
//        ));
//
//        // Security Name
//        table.addCell(createEquityDataCell(
//                equity.getScripname() != null ? equity.getScripname() : "",
//                font, Element.ALIGN_LEFT
//        ));
//
//        // Buy Quantity
//        table.addCell(createEquityDataCell(
//                equity.getBuyqty() != null ? equity.getBuyqty() : "0",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Buy WAP
//        table.addCell(createEquityDataCell(
//                equity.getBuymarketrate() != null ? formatDecimal4(equity.getBuymarketrate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Buy Brokerage
//        table.addCell(createEquityDataCell(
//                equity.getBuybrokerage() != null ? formatDecimal4(equity.getBuybrokerage()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Buy WAP After Brokerage
//        Double buyWAP = parseDouble(equity.getBuymarketrate());
//        Double buyBrok = parseDouble(equity.getBuybrokerage());
//        Double buyWAPAfter = buyWAP + buyBrok;
//        table.addCell(createEquityDataCell(
//                formatDecimal4(buyWAPAfter.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Sell Quantity
//        table.addCell(createEquityDataCell(
//                equity.getSellqty() != null ? equity.getSellqty() : "0",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Sell WAP
//        table.addCell(createEquityDataCell(
//                equity.getSellmarketrate() != null ? formatDecimal4(equity.getSellmarketrate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Sell Brokerage
//        table.addCell(createEquityDataCell(
//                equity.getSellbrokerage() != null ? formatDecimal4(equity.getSellbrokerage()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Sell WAP After Brokerage
//        Double sellWAP = parseDouble(equity.getSellmarketrate());
//        Double sellBrok = parseDouble(equity.getSellbrokerage());
//        Double sellWAPAfter = sellWAP - sellBrok;
//        table.addCell(createEquityDataCell(
//                formatDecimal4(sellWAPAfter.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Net Quantity
//        int buyQty = parseInt(equity.getBuyqty());
//        int sellQty = parseInt(equity.getSellqty());
//        int netQty = buyQty - sellQty;
//        table.addCell(createEquityDataCell(
//                String.valueOf(netQty),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Net Obligation
//        Double buyTotal = buyQty * buyWAPAfter;
//        Double sellTotal = sellQty * sellWAPAfter;
//        Double netObligation = -(buyTotal - sellTotal);
//        table.addCell(createEquityDataCell(
//                formatDecimal2(netObligation.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Update totals
//        totalPayInObligation += netObligation;
//    }
//
//    /**
//     * Create equity data cell
//     */
//    private PdfPCell createEquityDataCell(String text, BaseFont font, int alignment) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        Paragraph para = new Paragraph(text != null ? text : "", new Font(font, 7f, Font.NORMAL));
//        para.setAlignment(alignment);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * PAGE 1 - DERIVATIVE SEGMENT
//     * Displays derivative transactions (Futures & Options)
//     */
//    private void addPage1DerivativeSegment(Document document) throws Exception {
//        System.out.println("Adding Page 1 Derivative Segment...");
//
//        if (!hasDerivativeSegment) {
//            System.out.println("No derivative transactions found. Skipping derivative segment.");
//            return;
//        }
//
//        BaseFont calibriFont = BaseFont.createFont(pathCalibriFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//        BaseFont calibriFontBold = BaseFont.createFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//
//        // Derivative Segment Title
//        Paragraph derivTitle = new Paragraph("Derivative Segment", new Font(calibriFontBold, 10, Font.BOLD));
//        derivTitle.setSpacingBefore(8f);
//        derivTitle.setSpacingAfter(4f);
//        document.add(derivTitle);
//
//        // Create derivative table with 9 columns
//        PdfPTable derivTable = new PdfPTable(9);
//        derivTable.setWidthPercentage(100);
//        derivTable.setWidths(new float[]{3f, 0.8f, 0.9f, 1f, 1f, 1f, 1f, 1.3f, 1.5f});
//
//        // Header Row
//        derivTable.addCell(createDerivHeaderCell("Contract Description", calibriFontBold));
//        derivTable.addCell(createDerivHeaderCell("Buy[B]/\nSell[S]/\nBF/CF", calibriFontBold));
//        derivTable.addCell(createDerivHeaderCell("Quantity\n(In Foriegn\nCurrency)", calibriFontBold));
//        derivTable.addCell(createDerivHeaderCell("WAP per unit\n(Rs)", calibriFontBold));
//        derivTable.addCell(createDerivHeaderCell("WAP per unit\nAfter\nBrokerage(Rs)", calibriFontBold));
//        derivTable.addCell(createDerivHeaderCell("Brokerage\nPer Unit\n(Rs)", calibriFontBold));
//        derivTable.addCell(createDerivHeaderCell("Closing Rate\nper Unit\n(Rs)", calibriFontBold));
//        derivTable.addCell(createDerivHeaderCell("Net Total\n(Before Levies)", calibriFontBold));
//        derivTable.addCell(createDerivHeaderCell("Remarks", calibriFontBold));
//
//        // Process Options data from oHeaderTypeList
//        if (oHeaderTypeList != null && !oHeaderTypeList.isEmpty()) {
//            for (OHeaderTypeModel option : oHeaderTypeList) {
//                addDerivativeDataRow(derivTable, option, calibriFont);
//                derivativeRecordCount++;
//            }
//        }
//
//        // Process Futures data from foHeaderTypeList
//        if (foHeaderTypeList != null && !foHeaderTypeList.isEmpty()) {
//            for (FOHeaderTypeModel future : foHeaderTypeList) {
//                addFutureDataRow(derivTable, future, calibriFont);
//                derivativeRecordCount++;
//            }
//        }
//
//        // Process Futures data from sFuturesHeaderTypeList
//        if (sFuturesHeaderTypeList != null && !sFuturesHeaderTypeList.isEmpty()) {
//            for (SFuturesHeaderTypeModel future : sFuturesHeaderTypeList) {
//                addFutureDataRowFromSFutures(derivTable, future, calibriFont);
//                derivativeRecordCount++;
//            }
//        }
//
//        document.add(derivTable);
//
//        // Add note below derivative table
//        Paragraph derivNote = new Paragraph(
//                "* Exchange-wise details of orders and trades are provided in separate annexure.",
//                new Font(calibriFont, 6.5f, Font.NORMAL)
//        );
//        derivNote.setSpacingBefore(2f);
//        derivNote.setSpacingAfter(6f);
//        document.add(derivNote);
//
//        System.out.println("Page 1 Derivative Segment completed. Records: " + derivativeRecordCount);
//    }
//
//    /**
//     * Create derivative header cell
//     */
//    private PdfPCell createDerivHeaderCell(String text, BaseFont font) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setBackgroundColor(new BaseColor(220, 220, 220));
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//
//        Paragraph para = new Paragraph(text, new Font(font, 6.5f, Font.BOLD));
//        para.setAlignment(Element.ALIGN_CENTER);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * Add derivative data row from OHeaderTypeModel (Options)
//     */
//    private void addDerivativeDataRow(PdfPTable table, OHeaderTypeModel option, BaseFont font) {
//        // Contract Description
//        String contractDesc = buildOptionContractDescription(option);
//        table.addCell(createDerivDataCell(contractDesc, font, Element.ALIGN_LEFT));
//
//        // Buy/Sell
//        String buySell = option.getBuysell() != null ? option.getBuysell() : "";
//        table.addCell(createDerivDataCell(buySell, font, Element.ALIGN_CENTER));
//
//        // Quantity
//        table.addCell(createDerivDataCell(
//                option.getQuantity() != null ? option.getQuantity() : "0",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // WAP per unit
//        table.addCell(createDerivDataCell(
//                option.getRate() != null ? formatDecimal4(option.getRate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Brokerage Per Unit
//        table.addCell(createDerivDataCell(
//                option.getBrokerage() != null ? formatDecimal4(option.getBrokerage()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // WAP After Brokerage
//        Double rate = parseDouble(option.getRate());
//        Double brokerage = parseDouble(option.getBrokerage());
//        Double wapAfter = buySell.equals("B") ? rate + brokerage : rate - brokerage;
//        table.addCell(createDerivDataCell(
//                formatDecimal4(wapAfter.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Closing Rate
//        table.addCell(createDerivDataCell(
//                option.getClosingrate() != null ? formatDecimal4(option.getClosingrate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Net Total
//        int qty = parseInt(option.getQuantity());
//        Double netTotal = buySell.equals("B") ? -(qty * wapAfter) : (qty * wapAfter);
//        table.addCell(createDerivDataCell(
//                formatDecimal2(netTotal.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Remarks
//        table.addCell(createDerivDataCell("", font, Element.ALIGN_LEFT));
//
//        // Update totals
//        totalPayInObligation += netTotal;
//    }
//
//    /**
//     * Add future data row from FOHeaderTypeModel
//     */
//    private void addFutureDataRow(PdfPTable table, FOHeaderTypeModel future, BaseFont font) {
//        // Contract Description
//        String contractDesc = buildFutureContractDescription(future);
//        table.addCell(createDerivDataCell(contractDesc, font, Element.ALIGN_LEFT));
//
//        // Buy/Sell
//        String buySell = future.getBuysell() != null ? future.getBuysell() : "";
//        table.addCell(createDerivDataCell(buySell, font, Element.ALIGN_CENTER));
//
//        // Quantity
//        table.addCell(createDerivDataCell(
//                future.getQuantity() != null ? future.getQuantity() : "0",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // WAP per unit
//        table.addCell(createDerivDataCell(
//                future.getRate() != null ? formatDecimal4(future.getRate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Brokerage Per Unit
//        table.addCell(createDerivDataCell(
//                future.getBrokerage() != null ? formatDecimal4(future.getBrokerage()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // WAP After Brokerage
//        Double rate = parseDouble(future.getRate());
//        Double brokerage = parseDouble(future.getBrokerage());
//        Double wapAfter = buySell.equals("B") ? rate + brokerage : rate - brokerage;
//        table.addCell(createDerivDataCell(
//                formatDecimal4(wapAfter.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Closing Rate
//        table.addCell(createDerivDataCell(
//                future.getClosingrate() != null ? formatDecimal4(future.getClosingrate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Net Total
//        int qty = parseInt(future.getQuantity());
//        Double netTotal = buySell.equals("B") ? -(qty * wapAfter) : (qty * wapAfter);
//        table.addCell(createDerivDataCell(
//                formatDecimal2(netTotal.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Remarks
//        table.addCell(createDerivDataCell("", font, Element.ALIGN_LEFT));
//
//        // Update totals
//        totalPayInObligation += netTotal;
//    }
//
//    /**
//     * Add future data row from SFuturesHeaderTypeModel
//     */
//    private void addFutureDataRowFromSFutures(PdfPTable table, SFuturesHeaderTypeModel future, BaseFont font) {
//        // Contract Description
//        String contractDesc = buildFutureContractDescriptionFromSFutures(future);
//        table.addCell(createDerivDataCell(contractDesc, font, Element.ALIGN_LEFT));
//
//        // Buy/Sell
//        String buySell = future.getBuysell() != null ? future.getBuysell() : "";
//        table.addCell(createDerivDataCell(buySell, font, Element.ALIGN_CENTER));
//
//        // Quantity
//        table.addCell(createDerivDataCell(
//                future.getQuantity() != null ? future.getQuantity() : "0",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // WAP per unit
//        table.addCell(createDerivDataCell(
//                future.getRate() != null ? formatDecimal4(future.getRate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Brokerage Per Unit
//        table.addCell(createDerivDataCell(
//                future.getBrokerage() != null ? formatDecimal4(future.getBrokerage()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // WAP After Brokerage
//        Double rate = parseDouble(future.getRate());
//        Double brokerage = parseDouble(future.getBrokerage());
//        Double wapAfter = buySell.equals("B") ? rate + brokerage : rate - brokerage;
//        table.addCell(createDerivDataCell(
//                formatDecimal4(wapAfter.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Closing Rate
//        table.addCell(createDerivDataCell(
//                future.getClosingrate() != null ? formatDecimal4(future.getClosingrate()) : "0.0000",
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Net Total
//        int qty = parseInt(future.getQuantity());
//        Double netTotal = buySell.equals("B") ? -(qty * wapAfter) : (qty * wapAfter);
//        table.addCell(createDerivDataCell(
//                formatDecimal2(netTotal.toString()),
//                font, Element.ALIGN_RIGHT
//        ));
//
//        // Remarks
//        table.addCell(createDerivDataCell("", font, Element.ALIGN_LEFT));
//
//        // Update totals
//        totalPayInObligation += netTotal;
//    }
//
//    /**
//     * Build option contract description
//     * Format: IO 08 JUL 2025 BSXOPT 83600 PE
//     */
//    private String buildOptionContractDescription(OHeaderTypeModel option) {
//        StringBuilder desc = new StringBuilder();
//
//        if (option.getScripname() != null) {
//            desc.append(option.getScripname());
//        }
//        if (option.getInstrumentname() != null) {
//            desc.append(" ").append(option.getInstrumentname());
//        }
//        if (option.getOptiontype() != null) {
//            desc.append(" ").append(option.getOptiontype());
//        }
//        if (option.getStrikeprice() != null) {
//            desc.append(" ").append(option.getStrikeprice());
//        }
//
//        return desc.toString().trim();
//    }
//
//    /**
//     * Build future contract description from FOHeaderTypeModel
//     */
//    private String buildFutureContractDescription(FOHeaderTypeModel future) {
//        StringBuilder desc = new StringBuilder();
//
//        if (future.getScripname() != null) {
//            desc.append(future.getScripname());
//        }
//        if (future.getInstrumentname() != null) {
//            desc.append(" ").append(future.getInstrumentname());
//        }
//        if (future.getExpirydate() != null) {
//            desc.append(" ").append(future.getExpirydate());
//        }
//
//        return desc.toString().trim();
//    }
//
//    /**
//     * Build future contract description from SFuturesHeaderTypeModel
//     */
//    private String buildFutureContractDescriptionFromSFutures(SFuturesHeaderTypeModel future) {
//        StringBuilder desc = new StringBuilder();
//
//        if (future.getScripname() != null) {
//            desc.append(future.getScripname());
//        }
//        if (future.getInstrumentname() != null) {
//            desc.append(" ").append(future.getInstrumentname());
//        }
//        if (future.getExpirydate() != null) {
//            desc.append(" ").append(future.getExpirydate());
//        }
//
//        return desc.toString().trim();
//    }
//
//    /**
//     * Create derivative data cell
//     */
//    private PdfPCell createDerivDataCell(String text, BaseFont font, int alignment) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        Paragraph para = new Paragraph(text != null ? text : "", new Font(font, 7f, Font.NORMAL));
//        para.setAlignment(alignment);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * PAGE 1 - EXCHANGE SUMMARY
//     * Summary of charges by exchange and segment
//     */
//    private void addPage1ExchangeSummary(Document document) throws Exception {
//        System.out.println("Adding Page 1 Exchange Summary...");
//
//        BaseFont calibriFont = BaseFont.createFont(pathCalibriFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//        BaseFont calibriFontBold = BaseFont.createFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//
//        // Create summary table with 12 columns
//        PdfPTable summaryTable = new PdfPTable(12);
//        summaryTable.setWidthPercentage(100);
//        summaryTable.setWidths(new float[]{1.5f, 1f, 1f, 0.7f, 0.7f, 0.7f, 0.7f, 1f, 0.8f, 0.7f, 0.7f, 1.2f});
//        summaryTable.setSpacingBefore(8f);
//
//        // Header Row
//        summaryTable.addCell(createSummaryHeaderCell("Name Of Exchange\n& Segment", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("PAY IN/PAY OUT\nOBLIGATION", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("SECURITIES\nTRANSACTION\nTAX", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("SGST (**)\n9%", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("CGST (**)\n9%", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("IGST (**)\n18%", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("TDS", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("Exchange\nTransactn\nCharges", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("SEBI\nTurnover\nFees", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("Additional\nCess ***", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("Stamp Duty", calibriFontBold));
//        summaryTable.addCell(createSummaryHeaderCell("Net Amount Receivable By\nClient/(Payable by Client)", calibriFontBold));
//
//        // Reset totals
//        totalBrokerage = 0.0;
//        totalSTT = 0.0;
//        totalSGST = 0.0;
//        totalCGST = 0.0;
//        totalIGST = 0.0;
//        totalTDS = 0.0;
//        totalExchangeCharges = 0.0;
//        totalSEBIFees = 0.0;
//        totalStampDuty = 0.0;
//        netAmountReceivable = 0.0;
//
//        // Process footer data
//        if (footerList != null && !footerList.isEmpty()) {
//            for (FooterModelV2 footer : footerList) {
//                addExchangeSummaryRow(summaryTable, footer, calibriFont);
//            }
//        }
//
//        // Total Row
//        addExchangeSummaryTotalRow(summaryTable, calibriFontBold);
//
//        document.add(summaryTable);
//
//        // Add GST calculation note
//        Double gstBase = totalBrokerage + totalExchangeCharges + totalSEBIFees;
//        Paragraph gstNote = new Paragraph(
//                "** SGST: - State GST; CGST:-Central GST; IGST:-Integrated GST. GST is calculated on Brokerage, Exchange Transaction Charges and SEBI Fee. " +
//                        "(18% of Rs." + df2.format(gstBase) + ")",
//                new Font(calibriFont, 6.5f, Font.NORMAL)
//        );
//        gstNote.setSpacingBefore(2f);
//        document.add(gstNote);
//
//        // Add brokerage note
//        Paragraph brokNote = new Paragraph(
//                "# Brokerage shown is per unit in the case of Equities and total brokerage for the particular transaction in the case of F&O and Currency trades.",
//                new Font(calibriFont, 6.5f, Font.NORMAL)
//        );
//        brokNote.setSpacingBefore(1f);
//        document.add(brokNote);
//
//        // Add other charges note
//        Paragraph otherNote = new Paragraph(
//                "* Any other charges (DIS charge/AMC/Overdue charges etc.) which are due to us will be debited from the net amount.",
//                new Font(calibriFont, 6.5f, Font.NORMAL)
//        );
//        otherNote.setSpacingBefore(1f);
//        otherNote.setSpacingAfter(6f);
//        document.add(otherNote);
//
//        System.out.println("Page 1 Exchange Summary completed.");
//    }
//
//    /**
//     * Create summary header cell
//     */
//    private PdfPCell createSummaryHeaderCell(String text, BaseFont font) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setBackgroundColor(new BaseColor(220, 220, 220));
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//
//        Paragraph para = new Paragraph(text, new Font(font, 6f, Font.BOLD));
//        para.setAlignment(Element.ALIGN_CENTER);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * Add exchange summary data row
//     */
//    private void addExchangeSummaryRow(PdfPTable table, FooterModelV2 footer, BaseFont font) {
//        // Exchange & Segment
//        String exchangeSegment = (footer.getExchangename() != null ? footer.getExchangename() : "") +
//                (footer.getSegment() != null ? "-" + footer.getSegment() : "");
//        table.addCell(createSummaryDataCell(exchangeSegment, font, Element.ALIGN_LEFT));
//
//        // Pay In/Out Obligation
//        Double obligation = parseDouble(footer.getPayinpayout());
//        table.addCell(createSummaryDataCell(formatDecimal2(obligation.toString()), font, Element.ALIGN_RIGHT));
//
//        // STT
//        Double stt = parseDouble(footer.getStt());
//        totalSTT += stt;
//        table.addCell(createSummaryDataCell(formatDecimal2(stt.toString()), font, Element.ALIGN_RIGHT));
//
//        // SGST
//        Double sgst = parseDouble(footer.getSgst());
//        totalSGST += sgst;
//        table.addCell(createSummaryDataCell(formatDecimal2(sgst.toString()), font, Element.ALIGN_RIGHT));
//
//        // CGST
//        Double cgst = parseDouble(footer.getCgst());
//        totalCGST += cgst;
//        table.addCell(createSummaryDataCell(formatDecimal2(cgst.toString()), font, Element.ALIGN_RIGHT));
//
//        // IGST
//        Double igst = parseDouble(footer.getIgst());
//        totalIGST += igst;
//        table.addCell(createSummaryDataCell(formatDecimal2(igst.toString()), font, Element.ALIGN_RIGHT));
//
//        // TDS
//        Double tds = parseDouble(footer.getTds());
//        totalTDS += tds;
//        table.addCell(createSummaryDataCell(formatDecimal2(tds.toString()), font, Element.ALIGN_RIGHT));
//
//        // Exchange Charges
//        Double exchCharges = parseDouble(footer.getExchangetransactioncharge());
//        totalExchangeCharges += exchCharges;
//        table.addCell(createSummaryDataCell(formatDecimal4(exchCharges.toString()), font, Element.ALIGN_RIGHT));
//
//        // SEBI Fees
//        Double sebiFees = parseDouble(footer.getSebiturnover());
//        totalSEBIFees += sebiFees;
//        table.addCell(createSummaryDataCell(formatDecimal4(sebiFees.toString()), font, Element.ALIGN_RIGHT));
//
//        // Additional Cess
//        table.addCell(createSummaryDataCell("0", font, Element.ALIGN_RIGHT));
//
//        // Stamp Duty
//        Double stampDuty = parseDouble(footer.getStampduty());
//        totalStampDuty += stampDuty;
//        table.addCell(createSummaryDataCell(formatDecimal2(stampDuty.toString()), font, Element.ALIGN_RIGHT));
//
//        // Net Amount
//        Double netAmount = parseDouble(footer.getNetamount());
//        netAmountReceivable += netAmount;
//        table.addCell(createSummaryDataCell(formatDecimal2(netAmount.toString()), font, Element.ALIGN_RIGHT));
//
//        // Accumulate brokerage
//        Double brokerage = parseDouble(footer.getBrokerage());
//        totalBrokerage += brokerage;
//    }
//
//    /**
//     * Add exchange summary total row
//     */
//    private void addExchangeSummaryTotalRow(PdfPTable table, BaseFont font) {
//        // Exchange & Segment
//        table.addCell(createSummaryTotalCell("TOTAL(NET)", font, Element.ALIGN_LEFT));
//
//        // Pay In/Out Obligation
//        table.addCell(createSummaryTotalCell(formatDecimal2(totalPayInObligation.toString()), font, Element.ALIGN_RIGHT));
//
//        // STT
//        table.addCell(createSummaryTotalCell(formatDecimal2(totalSTT.toString()), font, Element.ALIGN_RIGHT));
//
//        // SGST
//        table.addCell(createSummaryTotalCell(formatDecimal2(totalSGST.toString()), font, Element.ALIGN_RIGHT));
//
//        // CGST
//        table.addCell(createSummaryTotalCell(formatDecimal2(totalCGST.toString()), font, Element.ALIGN_RIGHT));
//
//        // IGST
//        table.addCell(createSummaryTotalCell(formatDecimal2(totalIGST.toString()), font, Element.ALIGN_RIGHT));
//
//        // TDS
//        table.addCell(createSummaryTotalCell(formatDecimal2(totalTDS.toString()), font, Element.ALIGN_RIGHT));
//
//        // Exchange Charges
//        table.addCell(createSummaryTotalCell(formatDecimal4(totalExchangeCharges.toString()), font, Element.ALIGN_RIGHT));
//
//        // SEBI Fees
//        table.addCell(createSummaryTotalCell(formatDecimal4(totalSEBIFees.toString()), font, Element.ALIGN_RIGHT));
//
//        // Additional Cess
//        table.addCell(createSummaryTotalCell("0", font, Element.ALIGN_RIGHT));
//
//        // Stamp Duty
//        table.addCell(createSummaryTotalCell(formatDecimal2(totalStampDuty.toString()), font, Element.ALIGN_RIGHT));
//
//        // Net Amount with asterisk
//        table.addCell(createSummaryTotalCell(formatDecimal2(netAmountReceivable.toString()) + " *", font, Element.ALIGN_RIGHT));
//    }
//
//    /**
//     * Create summary data cell
//     */
//    private PdfPCell createSummaryDataCell(String text, BaseFont font, int alignment) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        Paragraph para = new Paragraph(text != null ? text : "0", new Font(font, 6.5f, Font.NORMAL));
//        para.setAlignment(alignment);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * Create summary total cell
//     */
//    private PdfPCell createSummaryTotalCell(String text, BaseFont font, int alignment) {
//        PdfPCell cell = new PdfPCell();
//        cell.setBorder(Rectangle.BOX);
//        cell.setBorderWidth(0.5f);
//        cell.setPadding(3f);
//        cell.setBackgroundColor(new BaseColor(240, 240, 240));
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        Paragraph para = new Paragraph(text != null ? text : "0", new Font(font, 6.5f, Font.BOLD));
//        para.setAlignment(alignment);
//        cell.addElement(para);
//
//        return cell;
//    }
//
//    /**
//     * Helper method to parse double values safely
//     */
//    private Double parseDouble(String value) {
//        if (value == null || value.trim().isEmpty()) {
//            return 0.0;
//        }
//        try {
//            return Double.parseDouble(value.replace(",", ""));
//        } catch (NumberFormatException e) {
//            return 0.0;
//        }
//    }
//
//    /**
//     * Helper method to parse integer values safely
//     */
//    private int parseInt(String value) {
//        if (value == null || value.trim().isEmpty()) {
//            return 0;
//        }
//        try {
//            return Integer.parseInt(value.replace(",", ""));
//        } catch (NumberFormatException e) {
//            return 0;
//        }
//    }
//
//    /**
//     * Format decimal with 2 places
//     */
//    private String formatDecimal2(String value) {
//        Double val = parseDouble(value);
//        return df2.format(val);
//    }
//
//    /**
//     * Format decimal with 4 places
//     */
//    private String formatDecimal4(String value) {
//        Double val = parseDouble(value);
//        return df4.format(val);
//    }
//
//    /**
//     * PAGE 1 - FOOTER NOTES
//     * Regulatory text and transaction information
//     */
//    private void addPage1FooterNotes(Document document) throws Exception {
//        System.out.println("Adding Page 1 Footer Notes...");
//
//        BaseFont calibriFont = BaseFont.createFont(pathCalibriFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//
//        Paragraph footerPara = new Paragraph();
//        footerPara.setSpacingBefore(8f);
//        footerPara.setSpacingAfter(6f);
//
//        String footerText = "Transactions mentioned in this contract note cum bill shall be governed and subject to the Rules, Bye-laws, Regulations and Circulars of the respective Exchanges on which trades have been executed and Securities " +
//                "and Exchange Board of India issued from time to time. It shall also be subject to the relevant Acts, Rules, Regulations, Directives, Notifications, Guidelines (including GST Laws) & Circulars issued by SEBI / " +
//                "Government of India / State Governments and Union Territory Governments issued from time to time. The Exchanges provide Complaint Resolution, Arbitration and Appellate arbitration facilities at the Regional " +
//                "Arbitration Centres (RAC). The client may approach its nearest centre, details of which are available on respective Exchange's website. Please visit www.nseindia.com for NSE, www.bseindia.com for BSE and " +
//                "www.msei.in for MSEI.";
//
//        Chunk footerChunk = new Chunk(footerText, new Font(calibriFont, 6f, Font.NORMAL));
//        footerPara.add(footerChunk);
//        footerPara.setAlignment(Element.ALIGN_JUSTIFIED);
//
//        document.add(footerPara);
//
//        System.out.println("Page 1 Footer Notes completed.");
//    }
//
//    /**
//     * PAGE 1 - SIGNATURE SECTION
//     * Date, place and authorized signatory
//     */
//    private void addPage1Signature(Document document) throws Exception {
//        System.out.println("Adding Page 1 Signature...");
//
//        BaseFont calibriFont = BaseFont.createFont(pathCalibriFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//        BaseFont calibriFontBold = BaseFont.createFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//
//        // Date and Place
//        PdfPTable datePlaceTable = new PdfPTable(2);
//        datePlaceTable.setWidthPercentage(100);
//        datePlaceTable.setWidths(new float[]{1f, 1f});
//        datePlaceTable.setSpacingBefore(8f);
//
//        // Left: Date and Place
//        PdfPCell leftCell = new PdfPCell();
//        leftCell.setBorder(Rectangle.NO_BORDER);
//        leftCell.setPadding(4f);
//
//        Paragraph leftPara = new Paragraph();
//        leftPara.add(new Chunk("Date: " + tradeDate + "\n", new Font(calibriFontBold, 8, Font.BOLD)));
//        leftPara.add(new Chunk("Place: PALARIVATTOM", new Font(calibriFontBold, 8, Font.BOLD)));
//        leftCell.addElement(leftPara);
//
//        // Right: Company and Signature
//        PdfPCell rightCell = new PdfPCell();
//        rightCell.setBorder(Rectangle.NO_BORDER);
//        rightCell.setPadding(4f);
//
//        Paragraph rightPara = new Paragraph();
//        rightPara.setAlignment(Element.ALIGN_RIGHT);
//        rightPara.add(new Chunk("Yours Faithfully,\n", new Font(calibriFont, 8, Font.NORMAL)));
//        rightPara.add(new Chunk(companyName + "\n", new Font(calibriFontBold, 8, Font.BOLD)));
//        rightPara.add(new Chunk("(PAN : " + companyPan + ", GSTIN : " + companyGSTIN + ")\n", new Font(calibriFont, 7, Font.NORMAL)));
//        rightCell.addElement(rightPara);
//
//        datePlaceTable.addCell(leftCell);
//        datePlaceTable.addCell(rightCell);
//
//        document.add(datePlaceTable);
//
//        // Service description
//        PdfPTable serviceTable = new PdfPTable(2);
//        serviceTable.setWidthPercentage(100);
//        serviceTable.setWidths(new float[]{1f, 1f});
//        serviceTable.setSpacingBefore(4f);
//
//        // Left: Service description
//        PdfPCell serviceLeftCell = new PdfPCell();
//        serviceLeftCell.setBorder(Rectangle.NO_BORDER);
//        serviceLeftCell.setPadding(4f);
//
//        Paragraph servicePara = new Paragraph();
//        servicePara.add(new Chunk("Description of Service : ", new Font(calibriFontBold, 7, Font.BOLD)));
//        servicePara.add(new Chunk(serviceDescription + "\n", new Font(calibriFont, 7, Font.NORMAL)));
//        servicePara.add(new Chunk("Service Account Code(SAC) : ", new Font(calibriFontBold, 7, Font.BOLD)));
//        servicePara.add(new Chunk(serviceAccountCode, new Font(calibriFont, 7, Font.NORMAL)));
//        serviceLeftCell.addElement(servicePara);
//
//        // Right: Authorized signatory
//        PdfPCell serviceRightCell = new PdfPCell();
//        serviceRightCell.setBorder(Rectangle.NO_BORDER);
//        serviceRightCell.setPadding(4f);
//
//        Paragraph signatoryPara = new Paragraph();
//        signatoryPara.setAlignment(Element.ALIGN_RIGHT);
//        signatoryPara.setSpacingBefore(10f);
//        signatoryPara.add(new Chunk("\n\n" + authorizedSignatory + "\n", new Font(calibriFontBold, 8, Font.BOLD)));
//        signatoryPara.add(new Chunk("(Name & Signature of Authorised Signatory)", new Font(calibriFont, 7, Font.NORMAL)));
//        serviceRightCell.addElement(signatoryPara);
//
//        serviceTable.addCell(serviceLeftCell);
//        serviceTable.addCell(serviceRightCell);
//
//        document.add(serviceTable);
//
//        System.out.println("Page 1 Signature completed.");
//    }
//
//    /**
//     * PAGE 1 - DISCLAIMER
//     * Disclaimer text at bottom
//     */
//    private void addPage1Disclaimer(Document document) throws Exception {
//        System.out.println("Adding Page 1 Disclaimer...");
//
//        BaseFont calibriFont = BaseFont.createFont(pathCalibriFont, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//        BaseFont calibriFontBold = BaseFont.createFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//
//        Paragraph disclaimerPara = new Paragraph();
//        disclaimerPara.setSpacingBefore(6f);
//        disclaimerPara.setSpacingAfter(4f);
//
//        Chunk disclaimerBold = new Chunk("Disclaimer: ", new Font(calibriFontBold, 6.5f, Font.BOLD));
//        Chunk disclaimerText = new Chunk("Purchase of REs (Rights Entitlements)only gives right to participate in the ongoing Rights Issue of the concerned company. REs which are neither subscribed by making an application with requisite application money " +
//                "nor renounced on or before the Issue Closing Date shall lapse and shall be extinguished after the Issue Closing Date.",
//                new Font(calibriFont, 6.5f, Font.NORMAL));
//
//        disclaimerPara.add(disclaimerBold);
//        disclaimerPara.add(disclaimerText);
//        disclaimerPara.setAlignment(Element.ALIGN_JUSTIFIED);
//
//        document.add(disclaimerPara);
//
//        // End of contract marker
//        Paragraph endMarker = new Paragraph(" * End Of Contract *", new Font(calibriFontBold, 8, Font.BOLD));
//        endMarker.setAlignment(Element.ALIGN_CENTER);
//        endMarker.setSpacingBefore(4f);
//        document.add(endMarker);
//
//        System.out.println("Page 1 Disclaimer completed.");
//    }
//
//
//
//}
