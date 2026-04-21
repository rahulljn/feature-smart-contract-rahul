package com.geojit.contractnotes.utils;

import java.io.IOException;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

public class HeaderFooterPageEventV2 extends PdfPageEventHelper {

    private String partyCode = "";
    private Image logo = null;

    private final ClassLoader classLoader = HeaderFooterPageEventV2.class.getClassLoader();

    public boolean isOrderTableStarted = false;
    public boolean isAnnexurePrinted = false;

    // Font paths - not final to allow initialization in method
    private String pathCalibriFontBold;
    private String pathCalibriFont;
    private String logoPath;

    // Fonts - initialized in constructor
    private BaseFont headerFont;
    private BaseFont bf;
    private BaseFont bfBold;

    public HeaderFooterPageEventV2(String partyCode) throws DocumentException, IOException {
        this.partyCode = partyCode;
        initializeFontsAndLogo();
    }

    public HeaderFooterPageEventV2() throws DocumentException, IOException {
        this.partyCode = "";
        initializeFontsAndLogo();
    }

    private void initializeFontsAndLogo() throws DocumentException, IOException {
        // Get resource paths
        pathCalibriFontBold = classLoader.getResource("calibri-Bold.ttf").getPath();
        pathCalibriFont = classLoader.getResource("calibri-400.ttf").getPath();
        logoPath = classLoader.getResource("GeojitLogo.jpg").getPath();

        // Register fonts
        FontFactory.register(pathCalibriFontBold);
        FontFactory.register(pathCalibriFont);

        // Create BaseFont instances
        headerFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        bf = BaseFont.createFont(pathCalibriFont, BaseFont.CP1252, BaseFont.EMBEDDED);
        bfBold = BaseFont.createFont(pathCalibriFontBold, BaseFont.CP1252, BaseFont.EMBEDDED);

        // Load logo
        logo = Image.getInstance(logoPath);
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        try {
            logo.setAbsolutePosition(25f, 784f);
            logo.scaleAbsolute(130, 27);
            document.add(logo);

            PdfContentByte cb = writer.getDirectContent();

            if (document.getPageNumber() == 1) {

                cb.beginText();
                cb.moveText(244, 817);
                cb.setColorFill(new BaseColor(0, 102, 204));
                cb.setFontAndSize(headerFont, 5);
                cb.showText("CONTRACT NOTE CUM TAX INVOICE");
                cb.endText();

                cb.beginText();
                cb.moveText(240, 810);
                cb.setFontAndSize(headerFont, 5);
                cb.showText("(Tax Invoice under Section 31 of GST Act)");
                cb.endText();

                cb.beginText();
                cb.moveText(215, 798);
                cb.setFontAndSize(headerFont, 14);
                cb.showText("Geojit Financial Services Limited");
                cb.endText();
            }

            if (isOrderTableStarted && !isAnnexurePrinted) {
                cb.beginText();
                cb.moveText(250, 798);
                cb.setFontAndSize(headerFont, 14);
                cb.showText("Annexure");
                cb.endText();
                isAnnexurePrinted = true;
            }

            PdfContentByte lineTop = writer.getDirectContent();
            lineTop.setColorStroke(BaseColor.LIGHT_GRAY);
            lineTop.setLineWidth(0.1f);
            lineTop.moveTo(20, 780);
            lineTop.lineTo(580, 780);
            lineTop.stroke();

            cb.beginText();
            cb.moveText(120, 772);
            cb.setFontAndSize(bf, 5);
            cb.showText(
                    "Registered Office: Geojit House, 34/2, Nagindas Master Road, Fort, Mumbai – 400001");
            cb.endText();

            cb.beginText();
            cb.moveText(250, 764);
            cb.setFontAndSize(bf, 5);
            cb.showText("CIN No: L67120KL1994PLC008403");
            cb.endText();

            cb.beginText();
            cb.moveText(160, 756);
            cb.setFontAndSize(bf, 5);
            cb.showText(
                    "SEBI Regn. No.: INZ000104737 | BSE: CM, F&O | NSE: CM, F&O, CD | MCX: F&O");
            cb.endText();

            cb.beginText();
            cb.moveText(140, 748);
            cb.setFontAndSize(bf, 5);
            cb.showText(
                    "Visit us at: www.geojit.com | Email: helpdesk@geojit.com | Toll Free: 1800-425-5501");
            cb.endText();

            PdfContentByte lineBottom = writer.getDirectContent();
            lineBottom.setColorStroke(BaseColor.LIGHT_GRAY);
            lineBottom.setLineWidth(0.1f);
            lineBottom.moveTo(20, 730);
            lineBottom.lineTo(580, 730);
            lineBottom.stroke();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {

        PdfContentByte cb = writer.getDirectContent();

        cb.beginText();
        cb.moveText(28, 18);
        cb.setFontAndSize(bfBold, 6);
        cb.showText("PAGE NO : " + document.getPageNumber());
        cb.endText();

        cb.beginText();
        cb.moveText(480, 18);
        cb.setFontAndSize(bfBold, 6);
        cb.showText("Client Code : " + partyCode);
        cb.endText();

        PdfContentByte line = writer.getDirectContent();
        line.setLineWidth(1f);
        line.setColorStroke(BaseColor.BLACK);
        line.moveTo(10, 30);
        line.lineTo(580, 30);
        line.stroke();
    }
}