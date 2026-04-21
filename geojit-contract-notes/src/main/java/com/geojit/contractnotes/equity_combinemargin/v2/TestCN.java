package com.geojit.contractnotes.equity_combinemargin.v2;

import java.io.File;
import java.io.FileOutputStream;

import com.geojit.contractnotes.equity_combinemargin.v2.MODEL.CNHeaderDTO;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.html.WebColors;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class TestCN {

    private static ClassLoader classLoader = TestCN.class.getClassLoader();
    private static String pathCalibriFont = classLoader.getResource("times_new_roman.ttf").getPath();
    private static String pathCalibriFontBold = classLoader.getResource("times_new_roman_bold.ttf").getPath();

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        String userPassword = " ";
        String ownerPassword = " ";
        try {
            // Custom page size: 11.69 x 8.27 inches (1 inch = 72 points)
            // Width: 11.69 * 72 = 841.68 points, Height: 8.27 * 72 = 595.44 points
            Rectangle pageSize = new Rectangle(841.68f, 595.44f);
            Document document = new Document(pageSize, 20, 20, 5, 34);
            BaseFont bf = BaseFont.createFont(pathCalibriFont, BaseFont.CP1252, BaseFont.EMBEDDED);
            Font font8 = new Font(bf, 8);
            Font font7 = new Font(bf, 7);
            Font font5 = new Font(bf, 5);
            Font font6 = new Font(bf, 6);
            Font font12= new Font(bf, 12);
            Font font10= new Font(bf, 10);
            FontFactory.register(pathCalibriFontBold);
            Font boldFont7 = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 7);
            Font boldFont8 = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8);
            Font boldFont10 = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);
            Font boldFont12 = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 12);

            FileOutputStream file = new FileOutputStream(
                    new File("src/main/resources/testCN"  + ".pdf"));
            System.out.println("file created");
            PdfWriter writer = PdfWriter.getInstance(document, file);

            document.open();

            // Create fonts for header
            Font headerFont = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14);
            Font subHeaderFont = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 12);
            Font rightFont = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);




            Image logo = Image.getInstance(classLoader.getResource("logo.PNG").getPath());
            logo.scaleAbsolute(120, 35);




            //this is parent table



            float[] pointColumnWidths = { 135f };
            PdfPTable nestedTable = new PdfPTable(pointColumnWidths);
            nestedTable.setSplitRows(true);
            nestedTable.setSplitLate(false);
            nestedTable.setWidthPercentage(100);


            PdfPCell space = new PdfPCell(new Phrase(" ", boldFont12));
            space.setVerticalAlignment(Element.ALIGN_TOP);
            space.setHorizontalAlignment(Element.ALIGN_CENTER);
            space.setBorderWidth(0);


            //below  is title

            float[] column = { 1,1,1 };
            PdfPTable contractTopTable1 = new PdfPTable(column);
            PdfPCell nested = new PdfPCell(logo,false);
            nested.setVerticalAlignment(Element.ALIGN_TOP);
            nested.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested.setBorderWidth(0);
            contractTopTable1.addCell(nested);

            PdfPCell nested1 = new PdfPCell(new Phrase("CONTRACT NOTE CUM TAX INVOICE \n (Tax Invoice under Section 31 of GST Act)", boldFont12));
            nested1.setVerticalAlignment(Element.ALIGN_BOTTOM);
            nested1.setHorizontalAlignment(Element.ALIGN_CENTER);
            nested1.setBorderWidth(0);
            contractTopTable1.addCell(nested1);

            PdfPCell nested2 = new PdfPCell(new Phrase("ORIGINAL FOR RECIPIENT", boldFont10));
            nested2.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            nested2.setBorderWidth(0);
            contractTopTable1.addCell(nested2);

            PdfPCell  nested3 = new PdfPCell(contractTopTable1);
            nested3.setBorderWidth(0);

            nestedTable.addCell(nested3);
            nestedTable.addCell(space);


            //below is address

            float[] column2 = { 1};
            PdfPTable contractTopTable2= new PdfPTable(column2);

            PdfPTable contractTopTable3= new PdfPTable(column2);



            contractTopTable2.addCell(space);
            contractTopTable2.addCell(space);

            PdfPCell nested4 = new PdfPCell(new Phrase("GEOJIT INVESTMENTS LTD", boldFont12));
            nested4.setVerticalAlignment(Element.ALIGN_TOP);
            nested4.setHorizontalAlignment(Element.ALIGN_CENTER);
            nested4.setBorderWidth(0);
            contractTopTable2.addCell(nested4);


            PdfPCell nested5 = new PdfPCell(new Phrase("SEBI REGISTRATION NO : INZ000318938 | CIN No : U66110KL2023PLC080586", font10));
            nested5.setVerticalAlignment(Element.ALIGN_TOP);
            nested5.setHorizontalAlignment(Element.ALIGN_CENTER);
            nested5.setBorderWidth(0);
            contractTopTable2.addCell(nested5);


            PdfPCell nested6 = new PdfPCell(new Phrase("7TH FLOOR, 34/659-P, CIVIL LINE ROAD,PADIVATTOM,KOCHI- 682024 | TEL: 0484-2901000 | FAX:0484 2979695 | Website: www.geojit.com/gil", font10));
            nested6.setVerticalAlignment(Element.ALIGN_TOP);
            nested6.setHorizontalAlignment(Element.ALIGN_CENTER);
            nested6.setBorderWidth(0);
            contractTopTable2.addCell(nested6);


            PdfPCell  nested7= new PdfPCell(contractTopTable2);
            nested7.setBorderWidth(1);


            contractTopTable3.addCell(nested7);


            PdfPCell nested8 = new PdfPCell(new Phrase("NAME OF THE COMPLIANCE OFFICER : Ancy C Sunny| EMAIL:compliance@geojit.com | TEL: 0484-2901000 | EMAIL ID FOR INVESTOR COMPLAINT:grievances@geojit.com", font8));
            nested8.setVerticalAlignment(Element.ALIGN_TOP);
            nested8.setHorizontalAlignment(Element.ALIGN_CENTER);
            nested8.setBorderWidth(1);
            contractTopTable3.addCell(nested8);

            PdfPCell nested9 = new PdfPCell(new Phrase("DEALING OFFICES ADDRESS : 1ST FLOOR, MASTERS TOWER ,,BYPASS-PIPELINE JUNCTION,,CIVIL LINE ROAD,,PALARIVATTOM| TELEPHONE NO: 9995800066", font6));
            nested9.setVerticalAlignment(Element.ALIGN_TOP);
            nested9.setHorizontalAlignment(Element.ALIGN_CENTER);
            nested9.setBorderWidth(1);
            contractTopTable3.addCell(nested9);


            PdfPCell nested10 = new PdfPCell(contractTopTable3);
            nested10.setBorderWidth(0);
            nestedTable.addCell(nested10);




            //below is PII and exchange segment table


            CNHeaderDTO cnHeaderDTO   = mockHeader();

            float[] column3 = { 1,1};
            float[] column3_1 = { 1,1,1};
            PdfPTable contractTopTable4= new PdfPTable(column3_1);


            float[] column4 = { 1,1};
            PdfPTable contractTopTable5= new PdfPTable(column4);




            PdfPCell left1 = new PdfPCell(new Phrase("CONTRACT NOTE NO :", font10));
            left1.setVerticalAlignment(Element.ALIGN_MIDDLE);
            left1.setHorizontalAlignment(Element.ALIGN_CENTER);
            contractTopTable4.addCell(left1);

            PdfPCell left2 = new PdfPCell(new Phrase(cnHeaderDTO.getContractNoteNo(), font10));
            left2.setVerticalAlignment(Element.ALIGN_MIDDLE);
            left2.setHorizontalAlignment(Element.ALIGN_CENTER);
            contractTopTable4.addCell(left2);

            PdfPCell left3 = new PdfPCell(new Phrase("TRADE DATE : " + cnHeaderDTO.getTradeDate(), font10));
            left3.setVerticalAlignment(Element.ALIGN_MIDDLE);
            left3.setHorizontalAlignment(Element.ALIGN_CENTER);
            contractTopTable4.addCell(left3);



            float[] column6= { 33.33f,67};
            PdfPTable contractTopTable6= new PdfPTable(column6);
            PdfPCell nested11 = new PdfPCell(new Phrase("Name Of the Client :", font8));
            nested11.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested11.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested11.setBorderWidthTop(0);
            nested11.setBorderWidthBottom(0);
            nested11.setBorderWidthLeft(1);
            nested11.setBorderWidthRight(0);
            contractTopTable6.addCell(nested11);



            PdfPCell nested12 = new PdfPCell(new Phrase(cnHeaderDTO.getClientName(), boldFont8));
            nested12.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested12.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested12.setBorderWidthTop(0);
            nested12.setBorderWidthBottom(0);
            nested12.setBorderWidthLeft(1);
            nested12.setBorderWidthRight(1);
            contractTopTable6.addCell(nested12);



            PdfPCell nested13 = new PdfPCell(new Phrase("Address of the Client :", font8));
            nested13.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested13.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested13.setBorderWidthTop(0);
            nested13.setBorderWidthBottom(0);
            nested13.setBorderWidthLeft(1);
            nested13.setBorderWidthRight(0);
            contractTopTable6.addCell(nested13);





            PdfPCell nested14 = new PdfPCell(new Phrase(cnHeaderDTO.getClientAddress1() + "\n" +cnHeaderDTO.getClientAddress2() + "\n"+cnHeaderDTO.getClientAddress3(), boldFont8));
            nested14.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested14.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested14.setBorderWidthTop(0);
            nested14.setBorderWidthBottom(0);
            nested14.setBorderWidthLeft(1);
            nested14.setBorderWidthRight(1);
            contractTopTable6.addCell(nested14);




            PdfPCell nested15 = new PdfPCell(new Phrase("Phone No :", font8));
            nested15.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested15.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested15.setBorderWidthTop(0);
            nested15.setBorderWidthBottom(0);
            nested15.setBorderWidthLeft(1);
            nested15.setBorderWidthRight(0);
            contractTopTable6.addCell(nested15);




            PdfPCell nested16 = new PdfPCell(new Phrase(cnHeaderDTO.getClientMobile(), boldFont8));
            nested16.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested16.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested16.setBorderWidthTop(0);
            nested16.setBorderWidthBottom(0);
            nested16.setBorderWidthLeft(1);
            nested16.setBorderWidthRight(1);
            contractTopTable6.addCell(nested16);




            PdfPCell nested17 = new PdfPCell(new Phrase("TradeCode/UCC of Client :", font8));
            nested17.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested17.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested17.setBorderWidthTop(0);
            nested17.setBorderWidthBottom(0);
            nested17.setBorderWidthLeft(1);
            nested17.setBorderWidthRight(0);
            contractTopTable6.addCell(nested17);




            PdfPCell nested18 = new PdfPCell(new Phrase(cnHeaderDTO.getUccCode(), boldFont8));
            nested18.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested18.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested18.setBorderWidthTop(0);
            nested18.setBorderWidthBottom(0);
            nested18.setBorderWidthLeft(1);
            nested18.setBorderWidthRight(1);
            contractTopTable6.addCell(nested18);




            PdfPCell nested19 = new PdfPCell(new Phrase("Place Of Supply [State Code] :", font8));
            nested19.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested19.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested19.setBorderWidthTop(0);
            nested19.setBorderWidthBottom(0);
            nested19.setBorderWidthLeft(1);
            nested19.setBorderWidthRight(0);
            contractTopTable6.addCell(nested19);




            PdfPCell nested20 = new PdfPCell(new Phrase(cnHeaderDTO.getBusinessType(), boldFont8));
            nested20.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested20.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested20.setBorderWidthTop(0);
            nested20.setBorderWidthBottom(0);
            nested20.setBorderWidthLeft(1);
            nested20.setBorderWidthRight(1);
            contractTopTable6.addCell(nested20);




            PdfPCell nested21 = new PdfPCell(new Phrase("Invoice Reference Number(IRN) :", font8));
            nested21.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested21.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested21.setBorderWidthTop(0);
            nested21.setBorderWidthBottom(0);
            nested21.setBorderWidthLeft(1);
            nested21.setBorderWidthRight(0);
            contractTopTable6.addCell(nested21);




            PdfPCell nested22 = new PdfPCell(new Phrase(cnHeaderDTO.getIrn(), boldFont8));
            nested22.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested22.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested22.setBorderWidthTop(0);
            nested22.setBorderWidthBottom(0);
            nested22.setBorderWidthLeft(1);
            nested22.setBorderWidthRight(1);
            contractTopTable6.addCell(nested22);




            PdfPCell nested23 = new PdfPCell(new Phrase("GST Identification No. :", font8));
            nested23.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested23.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested23.setBorderWidthTop(0);
            nested23.setBorderWidthBottom(1);
            nested23.setBorderWidthLeft(1);
            nested23.setBorderWidthRight(0);
            contractTopTable6.addCell(nested23);




            PdfPCell nested24 = new PdfPCell(new Phrase(cnHeaderDTO.getGstNo(), boldFont8));
            nested24.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nested24.setHorizontalAlignment(Element.ALIGN_LEFT);
            nested24.setBorderWidthTop(0);
            nested24.setBorderWidthBottom(1);
            nested24.setBorderWidthLeft(1);
            nested24.setBorderWidthRight(1);
            contractTopTable6.addCell(nested24);



            // left combine
            PdfPCell combine1 = new PdfPCell(contractTopTable4);
            combine1.setBorderWidth(0);

            PdfPCell combine2 = new PdfPCell(contractTopTable6);
            combine2.setBorderWidth(0);


            float[] column5 = { 1};

            PdfPTable leftFinal= new PdfPTable(column5);

            leftFinal.addCell(combine1);
            leftFinal.addCell(combine2);


//            //for left
            contractTopTable5.addCell(leftFinal);
//
//            //for right
//            contractTopTable5.addCell("right");
// RIGHT SIDE - Exchange/Segment Table
            float[] rightColumns = { 25f, 15f, 15f, 15f, 15f };
            PdfPTable rightTable = new PdfPTable(rightColumns);
            rightTable.setWidthPercentage(100);

            // Header Row
            PdfPCell headerExchange = new PdfPCell(new Phrase("EXCHANGE /\nCLEARING\nCORPORATION", font8));
            headerExchange.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerExchange.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerExchange.setBorderWidth(1);
            rightTable.addCell(headerExchange);

            PdfPCell headerSegment = new PdfPCell(new Phrase("SEGMENT", font8));
            headerSegment.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerSegment.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerSegment.setBorderWidth(1);
            rightTable.addCell(headerSegment);

            PdfPCell headerSttlNo = new PdfPCell(new Phrase("STTLNO", font8));
            headerSttlNo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerSttlNo.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerSttlNo.setBorderWidth(1);
            rightTable.addCell(headerSttlNo);

            PdfPCell headerSttlDate = new PdfPCell(new Phrase("STTLDATE", font8));
            headerSttlDate.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerSttlDate.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerSttlDate.setBorderWidth(1);
            rightTable.addCell(headerSttlDate);

            PdfPCell headerUccode = new PdfPCell(new Phrase("UCCODE", font8));
            headerUccode.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerUccode.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerUccode.setBorderWidth(1);
            rightTable.addCell(headerUccode);

            // Data Row 1 - BSE / NCL
            PdfPCell bseExchange = new PdfPCell(new Phrase("BSE / NCL", boldFont8));
            bseExchange.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bseExchange.setHorizontalAlignment(Element.ALIGN_CENTER);
            bseExchange.setBorderWidth(1);
            rightTable.addCell(bseExchange);

            PdfPCell bseSegment = new PdfPCell(new Phrase("FO", boldFont8));
            bseSegment.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bseSegment.setHorizontalAlignment(Element.ALIGN_CENTER);
            bseSegment.setBorderWidth(1);
            rightTable.addCell(bseSegment);

            PdfPCell bseSttlNo = new PdfPCell(new Phrase("", boldFont8));
            bseSttlNo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bseSttlNo.setHorizontalAlignment(Element.ALIGN_CENTER);
            bseSttlNo.setBorderWidth(1);
            rightTable.addCell(bseSttlNo);

            PdfPCell bseSttlDate = new PdfPCell(new Phrase("", boldFont8));
            bseSttlDate.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bseSttlDate.setHorizontalAlignment(Element.ALIGN_CENTER);
            bseSttlDate.setBorderWidth(1);
            rightTable.addCell(bseSttlDate);

            PdfPCell bseUccode = new PdfPCell(new Phrase("PLV083", boldFont8));
            bseUccode.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bseUccode.setHorizontalAlignment(Element.ALIGN_CENTER);
            bseUccode.setBorderWidth(1);
            rightTable.addCell(bseUccode);

            // Data Row 2 - NSE / NCL
            PdfPCell nseExchange = new PdfPCell(new Phrase("NSE / NCL", boldFont8));
            nseExchange.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nseExchange.setHorizontalAlignment(Element.ALIGN_CENTER);
            nseExchange.setBorderWidth(1);
            rightTable.addCell(nseExchange);

            PdfPCell nseSegment = new PdfPCell(new Phrase("EN", boldFont8));
            nseSegment.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nseSegment.setHorizontalAlignment(Element.ALIGN_CENTER);
            nseSegment.setBorderWidth(1);
            rightTable.addCell(nseSegment);

            PdfPCell nseSttlNo = new PdfPCell(new Phrase("2025126", boldFont8));
            nseSttlNo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nseSttlNo.setHorizontalAlignment(Element.ALIGN_CENTER);
            nseSttlNo.setBorderWidth(1);
            rightTable.addCell(nseSttlNo);

            PdfPCell nseSttlDate = new PdfPCell(new Phrase("04.07.2025", boldFont8));
            nseSttlDate.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nseSttlDate.setHorizontalAlignment(Element.ALIGN_CENTER);
            nseSttlDate.setBorderWidth(1);
            rightTable.addCell(nseSttlDate);

            PdfPCell nseUccode = new PdfPCell(new Phrase("PLV083", boldFont8));
            nseUccode.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nseUccode.setHorizontalAlignment(Element.ALIGN_CENTER);
            nseUccode.setBorderWidth(1);
            rightTable.addCell(nseUccode);

            // Wrap in a cell and add to contractTopTable5
            PdfPCell rightCell = new PdfPCell(rightTable);
            rightCell.setBorderWidth(0);
            contractTopTable5.addCell(rightCell);
// END REPLACEMENT


nestedTable.addCell(contractTopTable5);



// Sir/Madam line with PAN
            // Sir/Madam line with PAN
            nestedTable.addCell(space);

            float[] panColumns = { 60f, 40f };
            PdfPTable panTable = new PdfPTable(panColumns);
            panTable.setWidthPercentage(100);

            PdfPCell sirMadamCell = new PdfPCell(new Phrase("Sir/Madam, I / We have this day done by your order and on your account the following transactions:", font10));
            sirMadamCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            sirMadamCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            sirMadamCell.setBorderWidth(0);
            sirMadamCell.setPadding(5);
            panTable.addCell(sirMadamCell);

            PdfPCell panCell = new PdfPCell(new Phrase("PAN of the Client :  AEUPV7717F", font10));
            panCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            panCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            panCell.setBorderWidth(0);
            panCell.setPadding(5);
            panTable.addCell(panCell);

            PdfPCell panWrapCell = new PdfPCell(panTable);
            panWrapCell.setBorderWidth(0);
            nestedTable.addCell(panWrapCell);

            nestedTable.addCell(space);

// Equity Segment Table
            float[] column7 = { 1 };
            PdfPTable equitySegmentTable = new PdfPTable(column7);

// Header: "Equity Segment"
            PdfPCell equityHeader = new PdfPCell(new Phrase("Equity Segment", boldFont12));
            equityHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            equityHeader.setHorizontalAlignment(Element.ALIGN_LEFT);
            equityHeader.setBorderWidth(0);
            equityHeader.setPadding(5);
            equitySegmentTable.addCell(equityHeader);

// Main table with columns
            float[] equityColumns = { 8f, 15f, 5f, 7f, 7f, 10f, 10f, 5f, 7f, 7f, 10f, 10f, 5f, 10f };
            PdfPTable equityMainTable = new PdfPTable(equityColumns);

// First header row - merged cells
            PdfPCell secDescHeader = new PdfPCell(new Phrase("Security\nDescription", font7));
            secDescHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            secDescHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            secDescHeader.setRowspan(2);
            secDescHeader.setColspan(2);
            secDescHeader.setBorderWidth(1);
            equityMainTable.addCell(secDescHeader);

            PdfPCell buyHeader = new PdfPCell(new Phrase("Buy", font8));
            buyHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            buyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            buyHeader.setColspan(5);
            buyHeader.setBorderWidth(1);
            equityMainTable.addCell(buyHeader);

            PdfPCell sellHeader = new PdfPCell(new Phrase("Sell", font8));
            sellHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            sellHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            sellHeader.setColspan(5);
            sellHeader.setBorderWidth(1);
            equityMainTable.addCell(sellHeader);

            PdfPCell netObligHeader = new PdfPCell(new Phrase("Net Obligation for ISIN\n[Before Levies] (Rs)*", font7));
            netObligHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            netObligHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            netObligHeader.setRowspan(2);
            netObligHeader.setColspan(2);
            netObligHeader.setBorderWidth(1);
            equityMainTable.addCell(netObligHeader);

// Second header row - detailed columns
            PdfPCell isinHeader = new PdfPCell(new Phrase("ISIN", font7));
            isinHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            isinHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            isinHeader.setBorderWidth(1);
            equityMainTable.addCell(isinHeader);

            PdfPCell secNameHeader = new PdfPCell(new Phrase("Security\nName /\nSymbol", font7));
            secNameHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            secNameHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            secNameHeader.setBorderWidth(1);
            equityMainTable.addCell(secNameHeader);

            PdfPCell buyQtyHeader = new PdfPCell(new Phrase("Quantity", font7));
            buyQtyHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            buyQtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            buyQtyHeader.setBorderWidth(1);
            equityMainTable.addCell(buyQtyHeader);

            PdfPCell buyWapHeader = new PdfPCell(new Phrase("WAP\n(across\nexchanges)", font6));
            buyWapHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            buyWapHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            buyWapHeader.setBorderWidth(1);
            equityMainTable.addCell(buyWapHeader);

            PdfPCell buyBrokHeader = new PdfPCell(new Phrase("Brokerage\nPer Share\n(Rs)", font6));
            buyBrokHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            buyBrokHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            buyBrokHeader.setBorderWidth(1);
            equityMainTable.addCell(buyBrokHeader);

            PdfPCell buyWapAfterHeader = new PdfPCell(new Phrase("WAP (across\nexchanges)\nafter\nbrokerage\n(Rs)", font6));
            buyWapAfterHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            buyWapAfterHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            buyWapAfterHeader.setBorderWidth(1);
            equityMainTable.addCell(buyWapAfterHeader);

            PdfPCell buyTotalHeader = new PdfPCell(new Phrase("Total Buy\nValue after\nbrokerage", font6));
            buyTotalHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            buyTotalHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            buyTotalHeader.setBorderWidth(1);
            equityMainTable.addCell(buyTotalHeader);

            PdfPCell sellQtyHeader = new PdfPCell(new Phrase("Quantity", font7));
            sellQtyHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            sellQtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            sellQtyHeader.setBorderWidth(1);
            equityMainTable.addCell(sellQtyHeader);

            PdfPCell sellWapHeader = new PdfPCell(new Phrase("WAP\n(across\nexchanges)", font6));
            sellWapHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            sellWapHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            sellWapHeader.setBorderWidth(1);
            equityMainTable.addCell(sellWapHeader);

            PdfPCell sellBrokHeader = new PdfPCell(new Phrase("Brokerage\nPer Share\n(Rs)", font6));
            sellBrokHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            sellBrokHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            sellBrokHeader.setBorderWidth(1);
            equityMainTable.addCell(sellBrokHeader);

            PdfPCell sellWapAfterHeader = new PdfPCell(new Phrase("WAP (across\nexchanges)\nafter\nbrokerage\n(Rs)", font6));
            sellWapAfterHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            sellWapAfterHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            sellWapAfterHeader.setBorderWidth(1);
            equityMainTable.addCell(sellWapAfterHeader);

            PdfPCell sellTotalHeader = new PdfPCell(new Phrase("Total Sell\nValue after\nbrokerage", font6));
            sellTotalHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            sellTotalHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            sellTotalHeader.setBorderWidth(1);
            equityMainTable.addCell(sellTotalHeader);

            PdfPCell netQtyHeader = new PdfPCell(new Phrase("Net\nQuantity", font7));
            netQtyHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            netQtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            netQtyHeader.setBorderWidth(1);
            equityMainTable.addCell(netQtyHeader);

            PdfPCell netObligValueHeader = new PdfPCell(new Phrase("Net Obligation\nfor ISIN", font7));
            netObligValueHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            netObligValueHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            netObligValueHeader.setBorderWidth(1);
            equityMainTable.addCell(netObligValueHeader);

// Data Row
            PdfPCell dataIsin = new PdfPCell(new Phrase("INE12NJ01018", font7));
            dataIsin.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataIsin.setHorizontalAlignment(Element.ALIGN_CENTER);
            dataIsin.setBorderWidth(1);
            equityMainTable.addCell(dataIsin);

            PdfPCell dataSecName = new PdfPCell(new Phrase("SAMBHV STEEL TUBES LIMITED", font7));
            dataSecName.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataSecName.setHorizontalAlignment(Element.ALIGN_LEFT);
            dataSecName.setBorderWidth(1);
            equityMainTable.addCell(dataSecName);

            PdfPCell dataBuyQty = new PdfPCell(new Phrase("10", font7));
            dataBuyQty.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataBuyQty.setHorizontalAlignment(Element.ALIGN_CENTER);
            dataBuyQty.setBorderWidth(1);
            equityMainTable.addCell(dataBuyQty);

            PdfPCell dataBuyWap = new PdfPCell(new Phrase("101.3700", font7));
            dataBuyWap.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataBuyWap.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataBuyWap.setBorderWidth(1);
            equityMainTable.addCell(dataBuyWap);

            PdfPCell dataBuyBrok = new PdfPCell(new Phrase("2.0000", font7));
            dataBuyBrok.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataBuyBrok.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataBuyBrok.setBorderWidth(1);
            equityMainTable.addCell(dataBuyBrok);

            PdfPCell dataBuyWapAfter = new PdfPCell(new Phrase("103.3700", font7));
            dataBuyWapAfter.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataBuyWapAfter.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataBuyWapAfter.setBorderWidth(1);
            equityMainTable.addCell(dataBuyWapAfter);

            PdfPCell dataBuyTotal = new PdfPCell(new Phrase("-1033.7000", font7));
            dataBuyTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataBuyTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataBuyTotal.setBorderWidth(1);
            equityMainTable.addCell(dataBuyTotal);

            PdfPCell dataSellQty = new PdfPCell(new Phrase("0", font7));
            dataSellQty.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataSellQty.setHorizontalAlignment(Element.ALIGN_CENTER);
            dataSellQty.setBorderWidth(1);
            equityMainTable.addCell(dataSellQty);

            PdfPCell dataSellWap = new PdfPCell(new Phrase("0.0000", font7));
            dataSellWap.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataSellWap.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataSellWap.setBorderWidth(1);
            equityMainTable.addCell(dataSellWap);

            PdfPCell dataSellBrok = new PdfPCell(new Phrase("0.0000", font7));
            dataSellBrok.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataSellBrok.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataSellBrok.setBorderWidth(1);
            equityMainTable.addCell(dataSellBrok);

            PdfPCell dataSellWapAfter = new PdfPCell(new Phrase("0.0000", font7));
            dataSellWapAfter.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataSellWapAfter.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataSellWapAfter.setBorderWidth(1);
            equityMainTable.addCell(dataSellWapAfter);

            PdfPCell dataSellTotal = new PdfPCell(new Phrase("0.0000", font7));
            dataSellTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataSellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataSellTotal.setBorderWidth(1);
            equityMainTable.addCell(dataSellTotal);

            PdfPCell dataNetQty = new PdfPCell(new Phrase("10", font7));
            dataNetQty.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataNetQty.setHorizontalAlignment(Element.ALIGN_CENTER);
            dataNetQty.setBorderWidth(1);
            equityMainTable.addCell(dataNetQty);

            PdfPCell dataNetOblig = new PdfPCell(new Phrase("-1033.7000", font7));
            dataNetOblig.setVerticalAlignment(Element.ALIGN_MIDDLE);
            dataNetOblig.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dataNetOblig.setBorderWidth(1);
            equityMainTable.addCell(dataNetOblig);

// Footer note
            PdfPCell footerNote = new PdfPCell(new Phrase("* Exchange-wise details of orders and trades are provided in separate annexure.", font7));
            footerNote.setVerticalAlignment(Element.ALIGN_MIDDLE);
            footerNote.setHorizontalAlignment(Element.ALIGN_LEFT);
            footerNote.setColspan(14);
            footerNote.setBorderWidth(0);
            footerNote.setPadding(5);
            equityMainTable.addCell(footerNote);

// Add to equity segment table
            PdfPCell equityMainCell = new PdfPCell(equityMainTable);
            equityMainCell.setBorderWidth(0);
            equitySegmentTable.addCell(equityMainCell);

// Add to main nested table
            PdfPCell equitySegmentWrap = new PdfPCell(equitySegmentTable);
            equitySegmentWrap.setBorderWidth(0);
            nestedTable.addCell(equitySegmentWrap);
//            ---------------------------------------------------------------------
            document.add(nestedTable);
            document.close();

            System.out.println("PDF created successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static CNHeaderDTO mockHeader() {
        CNHeaderDTO dto = new CNHeaderDTO();

        dto.setTradeCode("EQ");
        dto.setRecordType("H");
        dto.setUccCode("UCC12345");
        dto.setClientName("Sameer Shaikh");
        dto.setClientAddress1("Flat 101, Alpha Apartments");
        dto.setClientAddress2("MG Road");
        dto.setClientAddress3("Mumbai - 400001");
        dto.setContractNoteNo("CN20260215-001");
        dto.setClientPan("ABCDE1234F");
        dto.setTradeDate("15-02-2026");
        dto.setClientEmail("sameer.shaikh@example.com");
        dto.setClientMobile("9876543210");
        dto.setGstNo("27ABCDE1234F1Z5");
        dto.setIrn("IRN1234567890");
        dto.setBusinessType("EQUITY");

        return dto;
    }
}