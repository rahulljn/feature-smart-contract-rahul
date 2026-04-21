//package com.geojit.contractnotes.geojit_ContractNote;
//import com.geojit.contractnotes.Model.DHeaderTypeModel;
//import com.geojit.contractnotes.Model.SFuturesHeaderTypeModel;
//import com.geojit.contractnotes.Model.CAHeaderTypeModel;
//
//import com.geojit.contractnotes.Model.CHeaderTypeModel;
//import com.geojit.contractnotes.Model.CustomerModel;
//import com.geojit.contractnotes.Model.DealingOfficeAddress;
//import com.geojit.contractnotes.Model.DHeaderTypeModel;
//import com.geojit.contractnotes.Model.FOHeaderTypeModel;
//import com.geojit.contractnotes.Model.FooterModelV2;
//import com.geojit.contractnotes.Model.MHeaderTypeModel;
//import com.geojit.contractnotes.Model.OHeaderTypeModel;
//import com.geojit.contractnotes.Model.PHeaderTypeModel;
//import com.geojit.contractnotes.Model.SCapitalHeaderTypeModel;
//import com.geojit.contractnotes.Model.SFuturesHeaderTypeModel;
//import com.geojit.contractnotes.Model.SSHeaderTypeModel;
//import com.geojit.contractnotes.Model.STTHeaderTypeModel;
//
//
//
//
//
//
//import com.itextpdf.text.*;
//import com.itextpdf.text.pdf.*;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.util.List;
//
//import com.geojit.contractnotes.Model.CustomerModel;
//import com.geojit.contractnotes.Model.DealingOfficeAddress;
//
//public class CreateGeoJitEquityPdfV1 {
//    class GeoJitPageEvent extends PdfPageEventHelper {
//
//        private BaseFont bf;
//
//        public GeoJitPageEvent(BaseFont bf) {
//            this.bf = bf;
//        }
//
//        @Override
//        public void onEndPage(PdfWriter writer, Document document) {
//            PdfContentByte cb = writer.getDirectContent();
//            cb.beginText();
//            cb.setFontAndSize(bf, 6f);
//            cb.showTextAligned(
//                    Element.ALIGN_RIGHT,
//                    "Page No : " + writer.getPageNumber(),
//                    document.right(),
//                    document.bottom() - 10,
//                    0
//            );
//            cb.endText();
//        }
//    }
//    public class CreateGeoJitEquityPdfV1 {
//
//        private static final ClassLoader cl = CreateGeoJitEquityPdfV1.class.getClassLoader();
//        private static final String CALIBRI = cl.getResource("calibri-400.ttf").getPath();
//        private static final String CALIBRI_BOLD = cl.getResource("calibri-Bold.ttf").getPath();
//
//        public void generatePdfStep1(
//                List<CustomerModel> customerList,
//                List<DealingOfficeAddress> dealingOfficeList
//        ) throws Exception {
//
//            // ─────────────────────────────────────────────
//            // 1. CUSTOMER (GeoJit always has single customer)
//            // ─────────────────────────────────────────────
//            CustomerModel customer = customerList.get(0);
//
//            // ─────────────────────────────────────────────
//            // 2. DOCUMENT SETUP (GeoJit margins)
//            // ─────────────────────────────────────────────
//            Document document = new Document(
//                    PageSize.A4,
//                    20f,   // left
//                    20f,   // right
//                    110f,  // top
//                    40f    // bottom
//            );
//
//            FileOutputStream fos =
//                    new FileOutputStream(new File("/tmp/" + customer.getPartycode() + ".pdf"));
//
//            PdfWriter writer = PdfWriter.getInstance(document, fos);
//
//            // ─────────────────────────────────────────────
//            // 3. FONTS
//            // ─────────────────────────────────────────────
//            BaseFont bf = BaseFont.createFont(CALIBRI, BaseFont.CP1252, BaseFont.EMBEDDED);
//            BaseFont bfBold = BaseFont.createFont(CALIBRI_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
//
//            Font font6 = new Font(bf, 6);
//            Font font6Bold = new Font(bfBold, 6);
//            Font font7Bold = new Font(bfBold, 7);
//
//            // ─────────────────────────────────────────────
//            // 4. PAGE EVENT
//            // ─────────────────────────────────────────────
//            writer.setPageEvent(new GeoJitPageEvent(bf));
//
//            document.open();
//            PdfContentByte cb = writer.getDirectContent();
//
//            // ─────────────────────────────────────────────
//            // 5. GEOJIT HEADER (STATIC)
//            // ─────────────────────────────────────────────
//            cb.beginText();
//            cb.setFontAndSize(bfBold, 9);
//            cb.showTextAligned(
//                    Element.ALIGN_CENTER,
//                    "CONTRACT NOTE CUM TAX INVOICE",
//                    297.5f,
//                    820,
//                    0
//            );
//            cb.endText();
//
//            cb.beginText();
//            cb.setFontAndSize(bf, 6);
//            cb.showTextAligned(
//                    Element.ALIGN_CENTER,
//                    "(Tax Invoice under Section 31 of GST Act)",
//                    297.5f,
//                    808,
//                    0
//            );
//            cb.endText();
//
//            cb.beginText();
//            cb.setFontAndSize(bfBold, 8);
//            cb.showTextAligned(
//                    Element.ALIGN_CENTER,
//                    "GEOJIT INVESTMENTS LTD",
//                    297.5f,
//                    792,
//                    0
//            );
//            cb.endText();
//
//            cb.beginText();
//            cb.setFontAndSize(bf, 6);
//            cb.showTextAligned(
//                    Element.ALIGN_CENTER,
//                    "SEBI REGISTRATION NO : INZ000318938 | CIN No : U66110KL2023PLC080586",
//                    297.5f,
//                    780,
//                    0
//            );
//            cb.endText();
//
//            // ─────────────────────────────────────────────
//            // 6. DEALING OFFICE ADDRESS (Dynamic)
//            // ─────────────────────────────────────────────
//            DealingOfficeAddress office = dealingOfficeList.get(0);
//
//            cb.beginText();
//            cb.setFontAndSize(bf, 6);
//            cb.showTextAligned(
//                    Element.ALIGN_CENTER,
//                    "DEALING OFFICES ADDRESS : " + office.getDealingAddress(),
//                    297.5f,
//                    760,
//                    0
//            );
//            cb.endText();
//
//            // ─────────────────────────────────────────────
//            // 7. HORIZONTAL LINE
//            // ─────────────────────────────────────────────
//            cb.setLineWidth(1f);
//            cb.moveTo(20, 745);
//            cb.lineTo(575, 745);
//            cb.stroke();
//
//            // ─────────────────────────────────────────────
//            // 8. CLIENT DETAILS BLOCK (LEFT)
//            // ─────────────────────────────────────────────
//            float y = 730;
//
//            drawLabelValue(cb, bfBold, bf, "Name Of the Client :", customer.getName(), 20, y);
//            y -= 12;
//
//            drawLabelValue(cb, bfBold, bf, "Address of the Client :", customer.getAddress1(), 20, y);
//            y -= 10;
//            drawValue(cb, bf, customer.getAddress2(), 120, y);
//            y -= 10;
//            if (customer.getAddress3() != null && !customer.getAddress3().isEmpty()) {
//                drawValue(cb, bf, customer.getAddress3(), 120, y);
//                y -= 10;
//            }
//
//            drawLabelValue(cb, bfBold, bf, "Phone No :", customer.getMobileNo(), 20, y);
//            y -= 12;
//
//            drawLabelValue(cb, bfBold, bf, "TradeCode/UCC of Client :", customer.getClientCode(), 20, y);
//            y -= 12;
//
//            drawLabelValue(cb, bfBold, bf, "Place Of Supply [State Code] :", "KERALA[32]", 20, y);
//            y -= 12;
//
//            drawLabelValue(cb, bfBold, bf, "Invoice Reference Number (IRN) :", customer.getIrn(), 20, y);
//            y -= 12;
//
//            drawLabelValue(cb, bfBold, bf, "GST Identification No :", customer.getGstNo(), 20, y);
//
//            // ─────────────────────────────────────────────
//            // 9. CONTRACT META (RIGHT)
//            // ─────────────────────────────────────────────
//            float ry = 730;
//
//            drawLabelValue(cb, bfBold, bf, "CONTRACT NOTE NO :", customer.getContractNo(), 340, ry);
//            ry -= 12;
//
//            drawLabelValue(cb, bfBold, bf, "TRADE DATE :", customer.getTransactionDate(), 340, ry);
//
//            document.close();
//            writer.close();
//        }
//
//        // ─────────────────────────────────────────────
//        // UTIL METHODS (NO ASSUMPTIONS)
//        // ─────────────────────────────────────────────
//        private void drawLabelValue(PdfContentByte cb, BaseFont bfBold, BaseFont bf,
//                                    String label, String value,
//                                    float x, float y) {
//
//            cb.beginText();
//            cb.setFontAndSize(bfBold, 6);
//            cb.showTextAligned(Element.ALIGN_LEFT, label, x, y, 0);
//            cb.endText();
//
//            cb.beginText();
//            cb.setFontAndSize(bf, 6);
//            cb.showTextAligned(Element.ALIGN_LEFT, value == null ? "" : value, x + 100, y, 0);
//            cb.endText();
//        }
//
//        private void drawValue(PdfContentByte cb, BaseFont bf, String value, float x, float y) {
//            cb.beginText();
//            cb.setFontAndSize(bf, 6);
//            cb.showTextAligned(Element.ALIGN_LEFT, value == null ? "" : value, x, y, 0);
//            cb.endText();
//        }
//    }
//    /**
//     * STEP-1A
//     * GEOJIT STATIC COMPANY HEADER
//     * (Pixel-perfect, no DTO dependency)
//     */
//    private void drawGeoJitStaticHeader(
//            PdfContentByte cb,
//            BaseFont bf,
//            BaseFont bfBold
//    ) {
//
//        // ==============================
//        // HEADER Y START
//        // ==============================
//        float centerX = 297.5f;   // A4 center
//        float y = 830f;
//
//        // ------------------------------------------------
//        // CONTRACT NOTE TITLE
//        // ------------------------------------------------
//        cb.beginText();
//        cb.setFontAndSize(bfBold, 9);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "CONTRACT NOTE CUM TAX INVOICE",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        y -= 12;
//
//        cb.beginText();
//        cb.setFontAndSize(bf, 6);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "(Tax Invoice under Section 31 of GST Act)",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        // ------------------------------------------------
//        // ORIGINAL FOR RECIPIENT (RIGHT TOP)
//        // ------------------------------------------------
//        cb.beginText();
//        cb.setFontAndSize(bfBold, 6);
//        cb.showTextAligned(
//                Element.ALIGN_RIGHT,
//                "ORIGINAL FOR RECIPIENT",
//                575f,
//                830f,
//                0
//        );
//        cb.endText();
//
//        // ------------------------------------------------
//        // COMPANY NAME
//        // ------------------------------------------------
//        y -= 16;
//
//        cb.beginText();
//        cb.setFontAndSize(bfBold, 8);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "GEOJIT INVESTMENTS LTD",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        // ------------------------------------------------
//        // REGISTRATION DETAILS
//        // ------------------------------------------------
//        y -= 12;
//
//        cb.beginText();
//        cb.setFontAndSize(bf, 6);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "SEBI REGISTRATION NO : INZ000318938 | CIN No : U66110KL2023PLC080586",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        // ------------------------------------------------
//        // REGISTERED OFFICE ADDRESS
//        // ------------------------------------------------
//        y -= 12;
//
//        cb.beginText();
//        cb.setFontAndSize(bf, 6);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "7TH FLOOR, 34/659-P, CIVIL LINE ROAD, PADIVATTOM, KOCHI-682024",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        y -= 10;
//
//        cb.beginText();
//        cb.setFontAndSize(bf, 6);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "TEL : 0484-2901000 | FAX : 0484-2979695 | Website : www.geojit.com/gil",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        // ------------------------------------------------
//        // COMPLIANCE OFFICER
//        // ------------------------------------------------
//        y -= 14;
//
//        cb.beginText();
//        cb.setFontAndSize(bf, 6);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "NAME OF THE COMPLIANCE OFFICER : Ancy C Sunny | EMAIL : compliance@geojit.com | TEL : 0484-2901000",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        // ------------------------------------------------
//        // INVESTOR COMPLAINT EMAIL
//        // ------------------------------------------------
//        y -= 10;
//
//        cb.beginText();
//        cb.setFontAndSize(bf, 6);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "EMAIL ID FOR INVESTOR COMPLAINT : grievances@geojit.com",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        // ------------------------------------------------
//        // DEALING OFFICE HEADER LABEL
//        // (Actual address comes in Step-1B)
//        // ------------------------------------------------
//        y -= 14;
//
//        cb.beginText();
//        cb.setFontAndSize(bfBold, 6);
//        cb.showTextAligned(
//                Element.ALIGN_CENTER,
//                "DEALING OFFICES ADDRESS :",
//                centerX,
//                y,
//                0
//        );
//        cb.endText();
//
//        // ------------------------------------------------
//        // HEADER SEPARATOR LINE
//        // ------------------------------------------------
//        cb.setLineWidth(1.2f);
//        cb.moveTo(20f, y - 8);
//        cb.lineTo(575f, y - 8);
//        cb.stroke();
//    }
//    /**
//     * STEP-1B
//     * DEALING OFFICE ADDRESS (Dynamic, Centered, Wrapped)
//     */
//    private void drawDealingOfficeAddress(
//            PdfContentByte cb,
//            BaseFont bf,
//            BaseFont bfBold,
//            List<DealingOfficeAddress> officeList
//    ) {
//
//        if (officeList == null || officeList.isEmpty()) {
//            return; // GeoJit: no assumptions
//        }
//
//        // GeoJit always uses first dealing office
//        DealingOfficeAddress office = officeList.get(0);
//
//        String addressText = office.getDealingAddress();
//        String gstLocation = office.getGstLocation();
//
//        float pageCenterX = 297.5f;
//        float startY = 708f;   // Exactly below Step-1A line
//        float lineGap = 9f;
//        float maxWidth = 520f; // GeoJit usable width
//
//        // -----------------------------------------
//        // ADDRESS WRAPPING (Font-aware)
//        // -----------------------------------------
//        List<String> wrappedLines =
//                wrapText(addressText, bf, 6f, maxWidth);
//
//        float y = startY;
//
//        for (String line : wrappedLines) {
//            float textWidth = bf.getWidthPoint(line, 6f);
//            float x = pageCenterX - (textWidth / 2);
//
//            cb.beginText();
//            cb.setFontAndSize(bf, 6);
//            cb.showTextAligned(
//                    Element.ALIGN_LEFT,
//                    line,
//                    x,
//                    y,
//                    0
//            );
//            cb.endText();
//
//            y -= lineGap;
//        }
//
//        // -----------------------------------------
//        // GST LOCATION (CENTERED)
//        // -----------------------------------------
//        if (gstLocation != null && !gstLocation.trim().isEmpty()) {
//
//            String gstText = "GST Location : " + gstLocation;
//            float gstWidth = bf.getWidthPoint(gstText, 6f);
//            float gstX = pageCenterX - (gstWidth / 2);
//
//            cb.beginText();
//            cb.setFontAndSize(bf, 6);
//            cb.showTextAligned(
//                    Element.ALIGN_LEFT,
//                    gstText,
//                    gstX,
//                    y - 2,
//                    0
//            );
//            cb.endText();
//
//            y -= 12;
//        }
//
//        // -----------------------------------------
//        // SEPARATOR LINE BELOW ADDRESS BLOCK
//        // -----------------------------------------
//        cb.setLineWidth(1.0f);
//        cb.moveTo(20f, y);
//        cb.lineTo(575f, y);
//        cb.stroke();
//    }
//    /**
//     * Utility method to wrap text based on font width
//     * (Used heavily in GeoJit PDFs)
//     */
//    private List<String> wrapText(
//            String text,
//            BaseFont bf,
//            float fontSize,
//            float maxWidth
//    ) {
//
//        List<String> lines = new java.util.ArrayList<>();
//
//        if (text == null || text.trim().isEmpty()) {
//            return lines;
//        }
//
//        String[] words = text.split("\\s+");
//        StringBuilder currentLine = new StringBuilder();
//
//        for (String word : words) {
//
//            String testLine =
//                    currentLine.length() == 0
//                            ? word
//                            : currentLine + " " + word;
//
//            float testWidth = bf.getWidthPoint(testLine, fontSize);
//
//            if (testWidth <= maxWidth) {
//                currentLine.setLength(0);
//                currentLine.append(testLine);
//            } else {
//                lines.add(currentLine.toString());
//                currentLine.setLength(0);
//                currentLine.append(word);
//            }
//        }
//
//        if (currentLine.length() > 0) {
//            lines.add(currentLine.toString());
//        }
//
//        return lines;
//    }
//    /**
//     * STEP-1C
//     * Exchange / Clearing / Settlement Meta Table
//     * (GeoJit production-grade)
//     */
//    private void drawExchangeSettlementTable(
//            Document document,
//            BaseFont bf,
//            BaseFont bfBold,
//            List<DHeaderTypeModel> dHeaderList,
//            String uccCode
//    ) throws DocumentException {
//
//        // --------------------------------------------
//        // TABLE CONFIG
//        // --------------------------------------------
//        float[] colWidths = {
//                3.5f,  // Exchange / Clearing
//                1.8f,  // Segment
//                2.2f,  // Settlement No
//                2.2f,  // Settlement Date
//                1.8f   // UCC
//        };
//
//        PdfPTable table = new PdfPTable(colWidths);
//        table.setWidthPercentage(100);
//        table.setSpacingBefore(6f);
//
//        Font headerFont = new Font(bfBold, 6);
//        Font cellFont = new Font(bf, 6);
//
//        BaseColor headerBg = new BaseColor(221, 249, 236);
//
//        // --------------------------------------------
//        // HEADER ROW
//        // --------------------------------------------
//        addHeaderCell(table, "EXCHANGE / CLEARING\nCORPORATION", headerFont, headerBg);
//        addHeaderCell(table, "SEGMENT", headerFont, headerBg);
//        addHeaderCell(table, "STTL NO", headerFont, headerBg);
//        addHeaderCell(table, "STTL DATE", headerFont, headerBg);
//        addHeaderCell(table, "UCCODE", headerFont, headerBg);
//
//        // --------------------------------------------
//        // DATA ROWS (DHeaderTypeModel loop)
//        // --------------------------------------------
//        if (dHeaderList != null) {
//            for (DHeaderTypeModel d : dHeaderList) {
//
//                String exchange = safe(d.getExchange());
//                String segment = mapSegment(d.getSegment());
//                String sttlNo = safe(d.getSettlementNo());
//                String sttlDate = safe(d.getSettlementdate());
//
//                // GeoJit clearing corporation mapping
//                String exchangeClearing = exchange + " / NCL";
//
//                addCell(table, exchangeClearing, cellFont);
//                addCell(table, segment, cellFont);
//                addCell(table, sttlNo, cellFont);
//                addCell(table, sttlDate, cellFont);
//                addCell(table, uccCode, cellFont);
//            }
//        }
//
//        document.add(table);
//    }
//    private void addHeaderCell(
//            PdfPTable table,
//            String text,
//            Font font,
//            BaseColor bg
//    ) {
//        PdfPCell cell = new PdfPCell(new Phrase(text, font));
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setBackgroundColor(bg);
//        cell.setPadding(4f);
//        cell.setBorderWidth(0.5f);
//        table.addCell(cell);
//    }
//    private void addCell(
//            PdfPTable table,
//            String text,
//            Font font
//    ) {
//        PdfPCell cell = new PdfPCell(new Phrase(text, font));
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setPadding(4f);
//        cell.setBorderWidth(0.5f);
//        table.addCell(cell);
//    }
//    private String mapSegment(String segment) {
//        if (segment == null) return "";
//        if ("CAPITAL".equalsIgnoreCase(segment)) {
//            return "EN";
//        }
//        if ("FUTURES".equalsIgnoreCase(segment)) {
//            return "FO";
//        }
//        return segment;
//    }
//    private String safe(String val) {
//        return val == null ? "" : val;
//    }
//    /**
//     * STEP-1D
//     * Client Details - LEFT BLOCK
//     * (GeoJit production style)
//     */
//    private void drawClientLeftBlock(
//            PdfContentByte cb,
//            BaseFont bf,
//            BaseFont bfBold,
//            CustomerModel customer
//    ) {
//
//        if (customer == null) {
//            return; // no assumptions
//        }
//
//        float labelX = 20f;
//        float valueX = 140f;
//        float startY = 670f;      // EXACT GeoJit vertical start
//        float lineGap = 12f;
//
//        // --------------------------------------------------
//        // Name of Client
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "Name Of the Client :", labelX, startY);
//        drawValue(cb, bf, customer.getName(), valueX, startY);
//
//        startY -= lineGap;
//
//        // --------------------------------------------------
//        // Address (multi-line)
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "Address of the Client :", labelX, startY);
//
//        if (notEmpty(customer.getAddress1())) {
//            drawValue(cb, bf, customer.getAddress1(), valueX, startY);
//            startY -= lineGap;
//        }
//
//        if (notEmpty(customer.getAddress2())) {
//            drawValue(cb, bf, customer.getAddress2(), valueX, startY);
//            startY -= lineGap;
//        }
//
//        if (notEmpty(customer.getAddress3())) {
//            drawValue(cb, bf, customer.getAddress3(), valueX, startY);
//            startY -= lineGap;
//        }
//
//        // --------------------------------------------------
//        // Phone No
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "Phone No :", labelX, startY);
//        drawValue(cb, bf, customer.getMobileNo(), valueX, startY);
//
//        startY -= lineGap;
//
//        // --------------------------------------------------
//        // Trade Code / UCC
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "TradeCode/UCC of Client :", labelX, startY);
//        drawValue(cb, bf, customer.getClientCode(), valueX, startY);
//
//        startY -= lineGap;
//
//        // --------------------------------------------------
//        // Place of Supply
//        // NOTE: GeoJit prints STATE[CODE]
//        // State code mapping comes from business rules
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "Place Of Supply [State Code] :", labelX, startY);
//        drawValue(cb, bf, "KERALA[32]", valueX, startY);   // same as GeoJit PDF
//
//        startY -= lineGap;
//
//        // --------------------------------------------------
//        // IRN
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "Invoice Reference Number (IRN) :", labelX, startY);
//        drawValue(cb, bf, customer.getIrn(), valueX, startY);
//
//        startY -= lineGap;
//
//        // --------------------------------------------------
//        // GST Identification No
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "GST Identification No :", labelX, startY);
//        drawValue(cb, bf, customer.getGstNo(), valueX, startY);
//
//        startY -= lineGap;
//
//        // --------------------------------------------------
//        // PAN
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "PAN of the Client :", labelX, startY);
//        drawValue(cb, bf, customer.getPanNo(), valueX, startY);
//    }
//    private void drawLabel(
//            PdfContentByte cb,
//            BaseFont bfBold,
//            String text,
//            float x,
//            float y
//    ) {
//        cb.beginText();
//        cb.setFontAndSize(bfBold, 6);
//        cb.showTextAligned(Element.ALIGN_LEFT, text, x, y, 0);
//        cb.endText();
//    }
//
//    private void drawValue(
//            PdfContentByte cb,
//            BaseFont bf,
//            String text,
//            float x,
//            float y
//    ) {
//        cb.beginText();
//        cb.setFontAndSize(bf, 6);
//        cb.showTextAligned(
//                Element.ALIGN_LEFT,
//                text == null ? "" : text,
//                x,
//                y,
//                0
//        );
//        cb.endText();
//    }
//
//    private boolean notEmpty(String val) {
//        return val != null && !val.trim().isEmpty();
//    }
//    /**
//     * STEP-1E
//     * Client Details - RIGHT BLOCK
//     * (GeoJit production style)
//     */
//    private void drawClientRightBlock(
//            PdfContentByte cb,
//            BaseFont bf,
//            BaseFont bfBold,
//            CustomerModel customer
//    ) {
//
//        if (customer == null) {
//            return; // no assumptions
//        }
//
//        float labelX = 340f;
//        float valueX = 470f;
//        float startY = 670f;      // Same vertical start as LEFT block
//        float lineGap = 12f;
//
//        // --------------------------------------------------
//        // Contract Note No
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "CONTRACT NOTE NO :", labelX, startY);
//        drawValue(cb, bf, customer.getContractNo(), valueX, startY);
//
//        startY -= lineGap;
//
//        // --------------------------------------------------
//        // Trade Date
//        // --------------------------------------------------
//        drawLabel(cb, bfBold, "TRADE DATE :", labelX, startY);
//        drawValue(cb, bf, customer.getTransactionDate(), valueX, startY);
//
//        startY -= lineGap;
//
//        // --------------------------------------------------
//        // Business Type (if available)
//        // --------------------------------------------------
//        if (notEmpty(customer.getBuinessType())) {
//            drawLabel(cb, bfBold, "BUSINESS TYPE :", labelX, startY);
//            drawValue(cb, bf, customer.getBuinessType(), valueX, startY);
//            startY -= lineGap;
//        }
//
//        // --------------------------------------------------
//        // ECN FLAG (Optional)
//        // --------------------------------------------------
//        if (notEmpty(customer.getEcnFlag())) {
//            drawLabel(cb, bfBold, "ECN FLAG :", labelX, startY);
//            drawValue(cb, bf, customer.getEcnFlag(), valueX, startY);
//        }
//    }
//    /**
//     * STEP-2A
//     * Equity Segment - Section Title + Table Header
//     */
//    private PdfPTable drawEquityTableHeader(
//            BaseFont bf,
//            BaseFont bfBold
//    ) throws DocumentException {
//
//        Font headerFont = new Font(bfBold, 6);
//        Font normalFont = new Font(bf, 6);
//
//        // --------------------------------------------------
//        // COLUMN WIDTHS (GeoJit proportion)
//        // --------------------------------------------------
//        float[] colWidths = {
//                2.2f,  // ISIN
//                4.8f,  // Security Name
//                1.2f,  // Buy Qty
//                1.6f,  // Buy Rate
//                1.4f,  // Buy Brokerage
//                1.8f,  // Buy Value
//                1.2f,  // Sell Qty
//                1.6f,  // Sell Rate
//                1.4f,  // Sell Brokerage
//                1.8f   // Sell Value
//        };
//
//        PdfPTable table = new PdfPTable(colWidths);
//        table.setWidthPercentage(100);
//        table.setSpacingBefore(10f);
//
//        BaseColor headerBg = new BaseColor(221, 249, 236);
//
//        // --------------------------------------------------
//        // FIRST HEADER ROW
//        // --------------------------------------------------
//        addEquityHeaderCell(table, "ISIN", headerFont, headerBg, 1, 2);
//        addEquityHeaderCell(table, "Security Name", headerFont, headerBg, 1, 2);
//
//        addEquityHeaderCell(table, "BUY", headerFont, headerBg, 4, 1);
//        addEquityHeaderCell(table, "SELL", headerFont, headerBg, 4, 1);
//
//        // --------------------------------------------------
//        // SECOND HEADER ROW
//        // --------------------------------------------------
//        addEquityHeaderCell(table, "Qty", headerFont, headerBg, 1, 1);
//        addEquityHeaderCell(table, "Rate", headerFont, headerBg, 1, 1);
//        addEquityHeaderCell(table, "Brokerage", headerFont, headerBg, 1, 1);
//        addEquityHeaderCell(table, "Value", headerFont, headerBg, 1, 1);
//
//        addEquityHeaderCell(table, "Qty", headerFont, headerBg, 1, 1);
//        addEquityHeaderCell(table, "Rate", headerFont, headerBg, 1, 1);
//        addEquityHeaderCell(table, "Brokerage", headerFont, headerBg, 1, 1);
//        addEquityHeaderCell(table, "Value", headerFont, headerBg, 1, 1);
//
//        return table;
//    }
//    private void addEquityHeaderCell(
//            PdfPTable table,
//            String text,
//            Font font,
//            BaseColor bg,
//            int colspan,
//            int rowspan
//    ) {
//
//        PdfPCell cell = new PdfPCell(new Phrase(text, font));
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setBackgroundColor(bg);
//        cell.setPadding(4f);
//        cell.setColspan(colspan);
//        cell.setRowspan(rowspan);
//        cell.setBorderWidth(0.5f);
//        table.addCell(cell);
//    }
//    /**
//     * STEP-2B
//     * Equity Data Rows (GeoJit style)
//     */
//    private void populateEquityRows(
//            PdfPTable table,
//            BaseFont bf,
//            List<SCapitalHeaderTypeModel> equityList
//    ) {
//
//        Font cellFont = new Font(bf, 6);
//
//        if (equityList == null || equityList.isEmpty()) {
//            return;
//        }
//
//        // --------------------------------------------------
//        // GROUP BY ISIN (GeoJit requirement)
//        // --------------------------------------------------
//        Map<String, List<SCapitalHeaderTypeModel>> isinMap =
//                new LinkedHashMap<>();
//
//        for (SCapitalHeaderTypeModel row : equityList) {
//            isinMap
//                    .computeIfAbsent(row.getIsin(), k -> new ArrayList<>())
//                    .add(row);
//        }
//
//        // --------------------------------------------------
//        // ITERATE ISIN BLOCKS
//        // --------------------------------------------------
//        for (Map.Entry<String, List<SCapitalHeaderTypeModel>> entry : isinMap.entrySet()) {
//
//            String isin = entry.getKey();
//            List<SCapitalHeaderTypeModel> rows = entry.getValue();
//
//            double buyQty = 0, sellQty = 0;
//            double buyRate = 0, sellRate = 0;
//            double buyBrokerage = 0, sellBrokerage = 0;
//            double buyValue = 0, sellValue = 0;
//
//            String securityName = "";
//
//            // ------------------------------------------
//            // ACCUMULATE VALUES FOR THIS ISIN
//            // ------------------------------------------
//            for (SCapitalHeaderTypeModel r : rows) {
//
//                securityName = safe(r.getDesc());
//
//                buyQty += parseDouble(r.getBuyQty());
//                sellQty += parseDouble(r.getSellQty());
//
//                buyRate = parseDouble(r.getBuyMarketRate());
//                sellRate = parseDouble(r.getSellMarketRate());
//
//                double brokerage = parseDouble(r.getBrokerage());
//                buyBrokerage += brokerage;
//                sellBrokerage += brokerage;
//
//                buyValue += buyQty * buyRate;
//                sellValue += sellQty * sellRate;
//            }
//
//            // ------------------------------------------
//            // DATA ROW
//            // ------------------------------------------
//            addCell(table, isin, cellFont);
//            addCell(table, securityName, cellFont);
//
//            addCell(table, formatQty(buyQty), cellFont);
//            addCell(table, formatAmt(buyRate), cellFont);
//            addCell(table, formatAmt(buyBrokerage), cellFont);
//            addCell(table, formatAmt(buyValue), cellFont);
//
//            addCell(table, formatQty(sellQty), cellFont);
//            addCell(table, formatAmt(sellRate), cellFont);
//            addCell(table, formatAmt(sellBrokerage), cellFont);
//            addCell(table, formatAmt(sellValue), cellFont);
//
//            // ------------------------------------------
//            // NET OBLIGATION ROW (GeoJit style)
//            // ------------------------------------------
//            PdfPCell netCell = new PdfPCell(
//                    new Phrase(
//                            "Net Obligation for ISIN : " + isin +
//                                    "  =  " + formatAmt(sellValue - buyValue),
//                            cellFont
//                    )
//            );
//            netCell.setColspan(10);
//            netCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            netCell.setPadding(4f);
//            netCell.setBorderWidth(0.5f);
//            table.addCell(netCell);
//        }
//    }
//    private double parseDouble(String val) {
//        try {
//            return val == null || val.trim().isEmpty()
//                    ? 0.0
//                    : Double.parseDouble(val.trim());
//        } catch (Exception e) {
//            return 0.0;
//        }
//    }
//
//    private String formatAmt(double val) {
//        return String.format("%.2f", val);
//    }
//
//    private String formatQty(double val) {
//        return String.format("%.0f", val);
//    }
//
//    private String safe(String val) {
//        return val == null ? "" : val;
//    }
//
//    /**
//     * STEP-2C
//     * Equity Totals & Summary (GeoJit production)
//     */
//    private void addEquityTotalsRow(
//            PdfPTable table,
//            BaseFont bf,
//            List<SCapitalHeaderTypeModel> equityList
//    ) {
//
//        Font boldFont = new Font(bf, 6, Font.BOLD);
//
//        double totalBuyQty = 0;
//        double totalSellQty = 0;
//        double totalBrokerage = 0;
//        double totalBuyValue = 0;
//        double totalSellValue = 0;
//
//        if (equityList != null) {
//            for (SCapitalHeaderTypeModel row : equityList) {
//
//                double buyQty = parseDouble(row.getBuyQty());
//                double sellQty = parseDouble(row.getSellQty());
//
//                double buyRate = parseDouble(row.getBuyMarketRate());
//                double sellRate = parseDouble(row.getSellMarketRate());
//
//                double brokerage = parseDouble(row.getBrokerage());
//
//                totalBuyQty += buyQty;
//                totalSellQty += sellQty;
//                totalBrokerage += brokerage;
//
//                totalBuyValue += buyQty * buyRate;
//                totalSellValue += sellQty * sellRate;
//            }
//        }
//
//        double netAmount = totalSellValue - totalBuyValue;
//
//        // --------------------------------------------------
//        // TOTAL LABEL CELL
//        // --------------------------------------------------
//        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL (NET)", boldFont));
//        totalLabel.setColspan(2);
//        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
//        totalLabel.setPadding(4f);
//        totalLabel.setBorderWidth(0.8f);
//        table.addCell(totalLabel);
//
//        // --------------------------------------------------
//        // BUY TOTALS
//        // --------------------------------------------------
//        table.addCell(createTotalCell(formatQty(totalBuyQty), boldFont));
//        table.addCell(createTotalCell("-", boldFont));
//        table.addCell(createTotalCell(formatAmt(totalBrokerage), boldFont));
//        table.addCell(createTotalCell(formatAmt(totalBuyValue), boldFont));
//
//        // --------------------------------------------------
//        // SELL TOTALS
//        // --------------------------------------------------
//        table.addCell(createTotalCell(formatQty(totalSellQty), boldFont));
//        table.addCell(createTotalCell("-", boldFont));
//        table.addCell(createTotalCell(formatAmt(totalBrokerage), boldFont));
//        table.addCell(createTotalCell(formatAmt(totalSellValue), boldFont));
//
//        // --------------------------------------------------
//        // NET OBLIGATION SUMMARY ROW
//        // --------------------------------------------------
//        PdfPCell netCell = new PdfPCell(
//                new Phrase(
//                        "Net Amount Receivable / (Payable) for Equity Segment : "
//                                + formatAmt(netAmount),
//                        boldFont
//                )
//        );
//        netCell.setColspan(10);
//        netCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
//        netCell.setPadding(6f);
//        netCell.setBorderWidth(1.0f);
//        table.addCell(netCell);
//    }
//    private PdfPCell createTotalCell(String text, Font font) {
//        PdfPCell cell = new PdfPCell(new Phrase(text, font));
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setPadding(4f);
//        cell.setBorderWidth(0.8f);
//        return cell;
//    }
//    /**
//     * STEP-3A
//     * Derivative Segment - Section Title + Table Header
//     */
//    private PdfPTable drawDerivativeTableHeader(
//            BaseFont bf,
//            BaseFont bfBold
//    ) throws DocumentException {
//
//        Font headerFont = new Font(bfBold, 6);
//
//        // --------------------------------------------------
//        // COLUMN WIDTHS (GeoJit proportion)
//        // --------------------------------------------------
//        float[] colWidths = {
//                4.8f,  // Contract Description
//                1.2f,  // B / S / CF
//                1.4f,  // Quantity
//                1.6f,  // WAP per unit
//                1.4f,  // Brokerage
//                1.6f,  // Closing Rate
//                1.8f,  // Net Total
//                1.8f   // Remarks
//        };
//
//        PdfPTable table = new PdfPTable(colWidths);
//        table.setWidthPercentage(100);
//        table.setSpacingBefore(12f);
//
//        BaseColor headerBg = new BaseColor(221, 249, 236);
//
//        // --------------------------------------------------
//        // HEADER CELLS
//        // --------------------------------------------------
//        addDerivativeHeaderCell(table, "Contract Description", headerFont, headerBg);
//        addDerivativeHeaderCell(table, "B/S/\nCF", headerFont, headerBg);
//        addDerivativeHeaderCell(table, "Qty", headerFont, headerBg);
//        addDerivativeHeaderCell(table, "WAP\nPer Unit", headerFont, headerBg);
//        addDerivativeHeaderCell(table, "Brokerage", headerFont, headerBg);
//        addDerivativeHeaderCell(table, "Closing\nRate", headerFont, headerBg);
//        addDerivativeHeaderCell(table, "Net Total", headerFont, headerBg);
//        addDerivativeHeaderCell(table, "Remarks", headerFont, headerBg);
//
//        return table;
//    }
//    private void addDerivativeHeaderCell(
//            PdfPTable table,
//            String text,
//            Font font,
//            BaseColor bg
//    ) {
//        PdfPCell cell = new PdfPCell(new Phrase(text, font));
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setBackgroundColor(bg);
//        cell.setPadding(4f);
//        cell.setBorderWidth(0.5f);
//        table.addCell(cell);
//    }
//    /**
//     * STEP-3B
//     * Derivative Data Rows (GeoJit production style)
//     */
//    private void populateDerivativeRows(
//            PdfPTable table,
//            BaseFont bf,
//            List<SFuturesHeaderTypeModel> futuresList
//    ) {
//
//        Font cellFont = new Font(bf, 6);
//
//        if (futuresList == null || futuresList.isEmpty()) {
//            return;
//        }
//
//        for (SFuturesHeaderTypeModel row : futuresList) {
//
//            // --------------------------------------------------
//            // FETCH & SAFE VALUES
//            // --------------------------------------------------
//            String contractDesc = safe(row.getDesc());
//            String bsFlag = safe(row.getBuyOrSell());
//            String qty = safe(row.getQty());
//            String wap = safe(row.getWapRate());
//            String brokerage = safe(row.getBrokerage());
//            String closingRate = safe(row.getClosingRate());
//            String netTotal = safe(row.getNetTotal());
//            String remarks = safe(row.getRemark());
//
//            // --------------------------------------------------
//            // ADD CELLS (Exact column order)
//            // --------------------------------------------------
//            addCell(table, contractDesc, cellFont);
//            addCell(table, bsFlag, cellFont);
//            addCell(table, formatQty(qty), cellFont);
//            addCell(table, formatAmt(wap), cellFont);
//            addCell(table, formatAmt(brokerage), cellFont);
//            addCell(table, formatAmt(closingRate), cellFont);
//            addCell(table, formatAmt(netTotal), cellFont);
//            addCell(table, remarks, cellFont);
//        }
//    }
//    private String safe(String val) {
//        return val == null ? "" : val;
//    }
//
//    private String formatAmt(String val) {
//        try {
//            return String.format("%.2f", Double.parseDouble(val));
//        } catch (Exception e) {
//            return "0.00";
//        }
//    }
//
//    private String formatQty(String val) {
//        try {
//            return String.format("%.0f", Double.parseDouble(val));
//        } catch (Exception e) {
//            return "0";
//        }
//    }
//    /**
//     * STEP-3C
//     * Derivative Totals & Net Obligation
//     * (GeoJit production-grade)
//     */
//    private void addDerivativeTotalsRow(
//            PdfPTable table,
//            BaseFont bf,
//            List<SFuturesHeaderTypeModel> futuresList
//    ) {
//
//        Font boldFont = new Font(bf, 6, Font.BOLD);
//
//        double totalQty = 0.0;
//        double totalBrokerage = 0.0;
//        double totalNetAmount = 0.0;
//
//        if (futuresList != null) {
//            for (SFuturesHeaderTypeModel row : futuresList) {
//
//                double qty = parseDouble(row.getQty());
//                double brokerage = parseDouble(row.getBrokerage());
//                double netTotal = parseDouble(row.getNetTotal());
//
//                totalQty += qty;
//                totalBrokerage += brokerage;
//                totalNetAmount += netTotal;
//            }
//        }
//
//        // --------------------------------------------------
//        // TOTAL LABEL CELL
//        // --------------------------------------------------
//        PdfPCell totalLabel = new PdfPCell(
//                new Phrase("TOTAL (DERIVATIVE SEGMENT)", boldFont)
//        );
//        totalLabel.setColspan(2);
//        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
//        totalLabel.setPadding(5f);
//        totalLabel.setBorderWidth(0.8f);
//        table.addCell(totalLabel);
//
//        // --------------------------------------------------
//        // TOTAL QTY
//        // --------------------------------------------------
//        table.addCell(createTotalCell(formatQty(totalQty), boldFont));
//
//        // --------------------------------------------------
//        // WAP (NOT APPLICABLE)
//        // --------------------------------------------------
//        table.addCell(createTotalCell("-", boldFont));
//
//        // --------------------------------------------------
//        // TOTAL BROKERAGE
//        // --------------------------------------------------
//        table.addCell(createTotalCell(formatAmt(totalBrokerage), boldFont));
//
//        // --------------------------------------------------
//        // CLOSING RATE (NOT APPLICABLE)
//        // --------------------------------------------------
//        table.addCell(createTotalCell("-", boldFont));
//
//        // NET TOTAL
//        // --------------------------------------------------
//        table.addCell(createTotalCell(formatAmt(totalNetAmount), boldFont));
//
//        // --------------------------------------------------
//        // REMARKS (EMPTY)
//        // --------------------------------------------------
//        table.addCell(createTotalCell("", boldFont));
//
//        // --------------------------------------------------
//        // NET OBLIGATION SUMMARY ROW
//        // --------------------------------------------------
//        PdfPCell netCell = new PdfPCell(
//                new Phrase(
//                        "Net Amount Receivable / (Payable) for Derivative Segment : "
//                                + formatAmt(totalNetAmount),
//                        boldFont
//                )
//        );
//        netCell.setColspan(8);
//        netCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
//        netCell.setPadding(6f);
//        netCell.setBorderWidth(1.0f);
//        table.addCell(netCell);
//    }
//    /**
//     * STEP-4A
//     * Charges / GST / Net Amount - Table Header
//     */
//    private PdfPTable drawChargesTableHeader(
//            BaseFont bf,
//            BaseFont bfBold
//    ) throws DocumentException {
//
//        Font headerFont = new Font(bfBold, 6);
//
//        // --------------------------------------------------
//        // COLUMN WIDTHS (GeoJit proportion)
//        // --------------------------------------------------
//        float[] colWidths = {
//                3.6f,  // Exchange & Segment
//                1.4f,  // Pay In / Pay Out
//                1.0f,  // STT
//                1.0f,  // SGST
//                1.0f,  // CGST
//                1.0f,  // IGST
//                1.0f,  // TDS
//                1.4f,  // Exchange Txn Charges
//                1.2f,  // SEBI Fees
//                1.2f,  // Stamp Duty
//                1.8f   // Net Amount
//        };
//
//        PdfPTable table = new PdfPTable(colWidths);
//        table.setWidthPercentage(100);
//        table.setSpacingBefore(14f);
//
//        BaseColor headerBg = new BaseColor(221, 249, 236);
//
//        // --------------------------------------------------
//        // HEADER CELLS
//        // --------------------------------------------------
//        addChargesHeaderCell(table, "Name Of Exchange\n& Segment", headerFont, headerBg);
//        addChargesHeaderCell(table, "PAY IN /\nPAY OUT", headerFont, headerBg);
//        addChargesHeaderCell(table, "STT", headerFont, headerBg);
//        addChargesHeaderCell(table, "SGST\n9%", headerFont, headerBg);
//        addChargesHeaderCell(table, "CGST\n9%", headerFont, headerBg);
//        addChargesHeaderCell(table, "IGST\n18%", headerFont, headerBg);
//        addChargesHeaderCell(table, "TDS", headerFont, headerBg);
//        addChargesHeaderCell(table, "Exchange\nTxn Charges", headerFont, headerBg);
//        addChargesHeaderCell(table, "SEBI\nFees", headerFont, headerBg);
//        addChargesHeaderCell(table, "Stamp\nDuty", headerFont, headerBg);
//        addChargesHeaderCell(table, "Net Amount\nReceivable / Payable", headerFont, headerBg);
//
//        return table;
//    }
//    private void addChargesHeaderCell(
//            PdfPTable table,
//            String text,
//            Font font,
//            BaseColor bg
//    ) {
//        PdfPCell cell = new PdfPCell(new Phrase(text, font));
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setBackgroundColor(bg);
//        cell.setPadding(4f);
//        cell.setBorderWidth(0.5f);
//        table.addCell(cell);
//    }
//    /**
//     * STEP-4B
//     * Charges Data Rows (GeoJit production style)
//     */
//    private void populateChargesRows(
//            PdfPTable table,
//            BaseFont bf,
//            List<CHeaderTypeModel> chargeList
//    ) {
//
//        Font cellFont = new Font(bf, 6);
//
//        if (chargeList == null || chargeList.isEmpty()) {
//            return;
//        }
//
//        for (CHeaderTypeModel row : chargeList) {
//
//            String chargeType = safe(row.getChargeType());
//
//            // ------------------------------------------
//            // GeoJit FIELD MAPPING
//            // ------------------------------------------
//            String exchangeSegment = chargeType;
//            String payInOut = safe(row.getField1());
//            String stt = safe(row.getField2());
//            String sgst = safe(row.getField3());
//            String cgst = safe(row.getField4());
//            String igst = safe(row.getField5());
//            String tds = safe(row.getField6());
//            String exchTxn = safe(row.getField7());
//            String sebiFee = safe(row.getField8());
//            String stampDuty = safe(row.getField9());
//            String netAmount = safe(row.getField9()); // printed again in last column
//
//            // ------------------------------------------
//            // ADD ROW CELLS (Exact order)
//            // ------------------------------------------
//            addCell(table, exchangeSegment, cellFont);
//            addCell(table, payInOut, cellFont);
//            addCell(table, formatAmt(stt), cellFont);
//            addCell(table, formatAmt(sgst), cellFont);
//            addCell(table, formatAmt(cgst), cellFont);
//            addCell(table, formatAmt(igst), cellFont);
//            addCell(table, formatAmt(tds), cellFont);
//            addCell(table, formatAmt(exchTxn), cellFont);
//            addCell(table, formatAmt(sebiFee), cellFont);
//            addCell(table, formatAmt(stampDuty), cellFont);
//            addCell(table, formatAmt(netAmount), cellFont);
//        }
//    }
//
//    /**
//     * STEP-5A
//     * Annexure - Trade Details Header
//     */
//    private PdfPTable drawAnnexureHeader(
//            BaseFont bf,
//            BaseFont bfBold,
//            String clearingCorpName,
//            String exchangeSegment
//    ) throws DocumentException {
//
//        Font titleFont = new Font(bfBold, 7);
//        Font normalFont = new Font(bf, 6);
//
//        // --------------------------------------------------
//        // ANNEXURE TITLE
//        // --------------------------------------------------
//        PdfPTable wrapper = new PdfPTable(1);
//        wrapper.setWidthPercentage(100);
//        wrapper.setSpacingBefore(14f);
//
//        PdfPCell titleCell = new PdfPCell(
//                new Phrase("Annexure - Trade Details", titleFont)
//        );
//        titleCell.setBorder(Rectangle.NO_BORDER);
//        titleCell.setPaddingBottom(6f);
//        wrapper.addCell(titleCell);
//
//        // --------------------------------------------------
//        // CLEARING CORPORATION INFO
//        // --------------------------------------------------
//        PdfPCell ccCell = new PdfPCell(
//                new Phrase(
//                        "Name of the Clearing Corporation & Segment : "
//                                + clearingCorpName + " | " + exchangeSegment,
//                        normalFont
//                )
//        );
//        ccCell.setBorder(Rectangle.NO_BORDER);
//        ccCell.setPaddingBottom(6f);
//        wrapper.addCell(ccCell);
//
//        // --------------------------------------------------
//        // TRADE TABLE HEADER
//        // --------------------------------------------------
//        PdfPTable tradeHeader = new PdfPTable(new float[]{
//                2.2f,  // Order No
//                1.4f,  // Order Time
//                2.2f,  // Trade No
//                1.4f,  // Trade Time
//                4.2f,  // Security / Contract
//                0.8f,  // B/S
//                1.0f,  // Qty
//                1.4f,  // Rate
//                1.4f,  // Brokerage
//                1.6f,  // Net Total
//                1.6f   // Remarks
//        });
//        tradeHeader.setWidthPercentage(100);
//
//        BaseColor headerBg = new BaseColor(221, 249, 236);
//        Font headerFont = new Font(bfBold, 6);
//
//        addAnnexureHeaderCell(tradeHeader, "Order No", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Order Time", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Trade No", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Trade Time", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Security / Contract", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "B/S", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Qty", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Rate", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Brokerage", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Net Total", headerFont, headerBg);
//        addAnnexureHeaderCell(tradeHeader, "Remarks", headerFont, headerBg);
//
//        // --------------------------------------------------
//        // NEST TABLE
//        // --------------------------------------------------
//        PdfPCell tableCell = new PdfPCell(tradeHeader);
//        tableCell.setBorder(Rectangle.NO_BORDER);
//        wrapper.addCell(tableCell);
//
//        return wrapper;
//    }
//    private void addAnnexureHeaderCell(
//            PdfPTable table,
//            String text,
//            Font font,
//            BaseColor bg
//    ) {
//        PdfPCell cell = new PdfPCell(new Phrase(text, font));
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setBackgroundColor(bg);
//        cell.setPadding(4f);
//        cell.setBorderWidth(0.5f);
//        table.addCell(cell);
//    }
//    /**
//     * STEP-5B
//     * Annexure Trade Rows (GeoJit production style)
//     */
//    private void populateAnnexureTradeRows(
//            Document document,
//            PdfPTable tradeTable,
//            BaseFont bf,
//            List<TradeDetailsModel> tradeList
//    ) throws DocumentException {
//
//        Font cellFont = new Font(bf, 6);
//
//        if (tradeList == null || tradeList.isEmpty()) {
//            return;
//        }
//
//        for (TradeDetailsModel trade : tradeList) {
//
//            // ------------------------------------------
//            // PAGE BREAK SAFETY
//            // ------------------------------------------
//            if (tradeTable.getTotalHeight() > 700f) {
//                document.add(tradeTable);
//                document.newPage();
//                tradeTable.deleteBodyRows();
//            }
//
//            // ------------------------------------------
//            // SAFE VALUE EXTRACTION
//            // ------------------------------------------
//            String orderNo = safe(trade.getOrderNo());
//            String orderTime = safe(trade.getOrderTime());
//            String tradeNo = safe(trade.getTradeNo());
//            String tradeTime = safe(trade.getTradeTime());
//            String contract = safe(trade.getContractDesc());
//            String bsFlag = safe(trade.getBuySellFlag());
//            String qty = formatQty(trade.getQuantity());
//            String rate = formatAmt(trade.getRate());
//            String brokerage = formatAmt(trade.getBrokerage());
//            String netTotal = formatAmt(trade.getNetTotal());
//            String remarks = safe(trade.getRemarks());
//
//            // ------------------------------------------
//            // ADD ROW CELLS (EXACT ORDER)
//            // ------------------------------------------
//            addTradeCell(tradeTable, orderNo, cellFont);
//            addTradeCell(tradeTable, orderTime, cellFont);
//            addTradeCell(tradeTable, tradeNo, cellFont);
//            addTradeCell(tradeTable, tradeTime, cellFont);
//            addTradeCell(tradeTable, contract, cellFont);
//            addTradeCell(tradeTable, bsFlag, cellFont);
//            addTradeCell(tradeTable, qty, cellFont);
//            addTradeCell(tradeTable, rate, cellFont);
//            addTradeCell(tradeTable, brokerage, cellFont);
//            addTradeCell(tradeTable, netTotal, cellFont);
//            addTradeCell(tradeTable, remarks, cellFont);
//        }
//
//        // ADD REMAINING ROWS
//        document.add(tradeTable);
//    }
//    private void addTradeCell(
//            PdfPTable table,
//            String text,
//            Font font
//    ) {
//        PdfPCell cell = new PdfPCell(new Phrase(text, font));
//        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//        cell.setPadding(3f);
//        cell.setBorderWidth(0.5f);
//        table.addCell(cell);
//    }
//
//    /**
//     * STEP-5C
//     * Annexure Sub-Totals & Net Obligation
//     * (GeoJit production-grade)
//     */
//    private void addAnnexureSubTotals(
//            Document document,
//            BaseFont bf,
//            List<TradeDetailsModel> tradeList
//    ) throws DocumentException {
//
//        if (tradeList == null || tradeList.isEmpty()) {
//            return;
//        }
//
//        Font boldFont = new Font(bf, 6, Font.BOLD);
//
//        // --------------------------------------------------
//        // GROUP BY SECURITY / CONTRACT
//        // --------------------------------------------------
//        Map<String, List<TradeDetailsModel>> contractMap =
//                new LinkedHashMap<>();
//
//        for (TradeDetailsModel trade : tradeList) {
//            contractMap
//                    .computeIfAbsent(
//                            safe(trade.getContractDesc()),
//                            k -> new ArrayList<>()
//                    )
//                    .add(trade);
//        }
//
//        double grandNetAmount = 0.0;
//
//        // --------------------------------------------------
//        // ITERATE CONTRACT GROUPS
//        // --------------------------------------------------
//        for (Map.Entry<String, List<TradeDetailsModel>> entry : contractMap.entrySet()) {
//
//            String contract = entry.getKey();
//            List<TradeDetailsModel> trades = entry.getValue();
//
//            double totalQty = 0.0;
//            double totalNet = 0.0;
//
//            for (TradeDetailsModel t : trades) {
//                totalQty += parseDouble(t.getQuantity());
//                totalNet += parseDouble(t.getNetTotal());
//            }
//
//            grandNetAmount += totalNet;
//
//            // ----------------------------------------------
//            // SUBTOTAL ROW
//            // ----------------------------------------------
//            PdfPTable subtotalTable = new PdfPTable(1);
//            subtotalTable.setWidthPercentage(100);
//            subtotalTable.setSpacingBefore(4f);
//
//            PdfPCell subtotalCell = new PdfPCell(
//                    new Phrase(
//                            "SubTotal  " + contract +
//                                    "  | Qty : " + formatQty(totalQty) +
//                                    "  | Net Amount : " + formatAmt(totalNet),
//                            boldFont
//                    )
//            );
//
//            subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            subtotalCell.setPadding(5f);
//            subtotalCell.setBorderWidth(0.8f);
//            subtotalTable.addCell(subtotalCell);
//
//            document.add(subtotalTable);
//        }
//
//        // --------------------------------------------------
//        // NET OBLIGATION SUMMARY (ANNEXURE)
//        // --------------------------------------------------
//        Paragraph netPara = new Paragraph(
//                "Net Obligation (As per Annexure) : " + formatAmt(grandNetAmount),
//                boldFont
//        );
//        netPara.setAlignment(Element.ALIGN_RIGHT);
//        netPara.setSpacingBefore(8f);
//        document.add(netPara);
//    }
//
//
//}
