package com.geojit.contractnotes.equity_combinemargin.v2;

import java.io.File;
import java.io.FileOutputStream;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.html.WebColors;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
//import com.mosl.contractnote.util.HeaderFooterPageEvent;

public class TestAGTS {

	private static ClassLoader classLoader = TestAGTS.class.getClassLoader();
	private static String pathCalibriFont = classLoader.getResource("calibri-400.ttf").getPath();
	private static String pathCalibriFontBold = classLoader.getResource("calibri-Bold.ttf").getPath();

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		String userPassword = " ";
		String ownerPassword = " ";
		try {
			Document document = new Document(PageSize.A3, 20, 20, 50, 34);
			BaseFont bf = BaseFont.createFont(pathCalibriFont, BaseFont.CP1252, BaseFont.EMBEDDED);
			Font font = new Font(bf, 8);
			Font font1 = new Font(bf, 7);
			FontFactory.register(pathCalibriFontBold);
			Font innercellFont = new Font(bf, 7);
			Font textFont6 = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 17);
			Font textFont7 = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);
			Font textFont8 = FontFactory.getFont(pathCalibriFontBold, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 7);
			@SuppressWarnings("deprecation")
			BaseColor myColor = WebColors.getRGBColor("#e5e8e8");
			FileOutputStream file = new FileOutputStream(
					new File("src/main/resources/shivraj"  + ".pdf"));
			System.out.println("file created");
			PdfWriter writer = PdfWriter.getInstance(document, file);
//			String panOfTheClient = "sameer";
			// Rectangle rectFooter = new Rectangle(30, 30, 550, 800);
//			writer.setBoxSize("art", rectFooter);
//			userPassword = panOfTheClient.toUpperCase();
//			ownerPassword = panOfTheClient.toUpperCase();

//			writer.setEncryption(userPassword.getBytes(), ownerPassword.getBytes(), PdfWriter.ALLOW_PRINTING,
//					PdfWriter.ENCRYPTION_AES_128);

			// set pagination
//			HeaderFooterPageEvent event = new HeaderFooterPageEvent();
//			writer.setPageEvent(event);
			document.open();

			Image logo = Image.getInstance(classLoader.getResource("mologo.PNG").getPath());
			// logo.setAbsolutePosition(8f, 1080f);
			logo.scaleAbsolute(98, 43);
			// document.add(logo);

			Image newSEBI = Image.getInstance(classLoader.getResource("NewSEBI.jpg").getPath());
			// newSEBI.setAbsolutePosition(8f, 1080f);
			newSEBI.scaleAbsolute(60, 80);
			// document.add(newSEBI);

			float[] pointColumnWidths = { 135f };
			PdfPTable nestedTable = new PdfPTable(pointColumnWidths);
			nestedTable.setSplitRows(true);
			nestedTable.setSplitLate(false);
			nestedTable.setWidthPercentage(100);

			float[] column1 = { 130 };

//			PdfPTable contractTopTable = new PdfPTable(column1);

			float[] columntop = { 12, 130, 10 };
			PdfPTable contractTopTable = new PdfPTable(column1);

//			contractTopTable.setPaddingTop(0);
			PdfPCell nestedTop = new PdfPCell(new Phrase("Motilal Oswal Financial Services Limited", textFont6));
			nestedTop.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop.setBorderWidth(0);
			contractTopTable.addCell(nestedTop);

			PdfPCell nestedTop1 = new PdfPCell(new Phrase(
					"Corr Add:Palm Spring Centre,2nd floor,Link road, malad (w),Mumbai-400064. Tel No:022-71881000 Fax No:022-71881333",
					font1));
			nestedTop1.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop1.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop1.setBorderWidth(0);
			nestedTop1.setPadding(1);
			contractTopTable.addCell(nestedTop1);

			PdfPCell nestedTop2 = new PdfPCell(new Phrase(
					"Registered Office: Motilal Oswal Tower, 6th floor,Rahimtullah Sayani Road, Opposite Parel ST Depot, Prabhadevi, Mumbai-400025 Tel No:+91 022-71934263 / 71934200",
					font1));
			nestedTop2.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop2.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop2.setBorderWidth(0);
			nestedTop2.setPadding(1);
			contractTopTable.addCell(nestedTop2);

			PdfPCell nestedTop3 = new PdfPCell(
					new Phrase("SEBI Regn.No.(BSE/NSE/MCX/NCDEX): INZ000158836, CIN: L67190MH2005PLC153397", font1));
			nestedTop3.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop3.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop3.setBorderWidth(0);
			nestedTop3.setPadding(1);
			contractTopTable.addCell(nestedTop3);

			PdfPCell nestedTop4 = new PdfPCell(new Phrase(
					"CDSL and NSDL: IN-DP-16-2015, AMFI: ARN 146822, SEBI Research Analyst: INH000000412, Investment Advisers: INA000007100, PMS:INP 000006712, IRDA CORPORATE AGENT:CA 0579",
					font1));
			nestedTop4.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop4.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop4.setBorderWidth(0);
			nestedTop4.setPadding(1);
			contractTopTable.addCell(nestedTop4);

			PdfPCell nestedTop5 = new PdfPCell(
					new Phrase("query@motilaloswal.com visit us at :www.motilaloswal.com", font1));
			nestedTop5.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop5.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop5.setBorderWidth(0);
			nestedTop5.setPadding(1);
			contractTopTable.addCell(nestedTop5);

			PdfPCell nestedTop6 = new PdfPCell(
					new Phrase("Email ID For Investor Complaints: grievances@motilaloswal.com", font1));
			nestedTop6.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop6.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop6.setBorderWidth(0);
			nestedTop6.setPadding(1);
			contractTopTable.addCell(nestedTop6);

			PdfPCell nestedTop10 = new PdfPCell(new Phrase(
					"Name of the Compliance Officer: Mr. Neeraj Agarwal (Email: na@motilaloswal.com, 022-71881085)",
					font1));
			nestedTop10.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop10.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop10.setBorderWidth(0);
			nestedTop10.setPadding(1);
			contractTopTable.addCell(nestedTop10);

			PdfPCell nestedTop7 = new PdfPCell(new Phrase(" ", font1));
			nestedTop7.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop7.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop7.setBorderWidth(0);
			nestedTop7.setPadding(1);
			contractTopTable.addCell(nestedTop7);

			PdfPCell nestedTop8 = new PdfPCell(new Phrase("ANNUAL GLOBAL TRANSACTION STATEMENT (AGTS)", textFont7));
			nestedTop8.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop8.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop8.setBorderWidth(0);
			contractTopTable.addCell(nestedTop8);

			PdfPCell nestedTop9 = new PdfPCell(new Phrase(" ", font1));
			nestedTop9.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedTop9.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTop9.setBorderWidth(0);
			contractTopTable.addCell(nestedTop9);

//			contractTopTable.setHorizontalAlignment(1);

			PdfPTable contractTable = new PdfPTable(columntop);
			PdfPCell nestedlogo = new PdfPCell(logo, false);
			nestedlogo.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedlogo.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedlogo.setBorderWidth(0);
			contractTable.addCell(nestedlogo);

			PdfPCell nestedAddcell = new PdfPCell(contractTopTable);
			nestedAddcell.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedAddcell.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedAddcell.setBorderWidth(0);
			contractTable.addCell(nestedAddcell);

			PdfPCell nestedA = new PdfPCell(newSEBI, false);
			nestedA.setVerticalAlignment(Element.ALIGN_RIGHT);
			nestedA.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nestedA.setBorderWidth(0);
			contractTable.addCell(nestedA);

			PdfPCell nested80Cell = new PdfPCell(contractTable);
			nested80Cell.setBorderWidth(0);

			nestedTable.addCell(nested80Cell);
////			Adding table top
//			PdfPCell nestedAddcell = new PdfPCell(contractTopTable);
//			nestedAddcell.setBorderWidth(0);
//			nestedTable.addCell(nestedAddcell);

			float[] column = { 20, 80 };
			PdfPTable contractTopTable1 = new PdfPTable(column);
			PdfPCell nested = new PdfPCell(new Phrase("Name of the Client :", font));
			nested.setVerticalAlignment(Element.ALIGN_CENTER);
			nested.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nested.setBorderWidth(1);
			contractTopTable1.addCell(nested);

			PdfPCell nestedB = new PdfPCell(new Phrase("test", font));
			nestedB.setVerticalAlignment(Element.ALIGN_CENTER);
			nestedB.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedB.setBorderWidth(1);
			contractTopTable1.addCell(nestedB);

			PdfPCell nested1 = new PdfPCell(new Phrase("UCC(s) of the Client :", font));
			nested1.setVerticalAlignment(Element.ALIGN_CENTER);
			nested1.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nested1.setBorderWidth(1);
			contractTopTable1.addCell(nested1);

			PdfPCell nested2 = new PdfPCell(new Phrase(" ", font));
			nested2.setVerticalAlignment(Element.ALIGN_CENTER);
			nested2.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested2.setBorderWidth(1);
			contractTopTable1.addCell(nested2);

			PdfPCell nested3 = new PdfPCell(new Phrase("PAN of the Client :", font));
			nested3.setVerticalAlignment(Element.ALIGN_CENTER);
			nested3.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nested3.setBorderWidth(1);
			contractTopTable1.addCell(nested3);

			PdfPCell nested4 = new PdfPCell(new Phrase(" ", font));
			nested4.setVerticalAlignment(Element.ALIGN_CENTER);
			nested4.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested4.setBorderWidth(1);
			contractTopTable1.addCell(nested4);

			PdfPCell nested5 = new PdfPCell(new Phrase("Basis:", font));
			nested5.setVerticalAlignment(Element.ALIGN_CENTER);
			nested5.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nested5.setBorderWidth(1);
			contractTopTable1.addCell(nested5);

			PdfPCell nested6 = new PdfPCell(new Phrase(" ", font));
			nested6.setVerticalAlignment(Element.ALIGN_CENTER);
			nested6.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested6.setBorderWidth(1);
			contractTopTable1.addCell(nested6);

			PdfPCell nested7 = new PdfPCell(new Phrase("Date of Issue of AGTS:", font));
			nested7.setVerticalAlignment(Element.ALIGN_CENTER);
			nested7.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nested7.setBorderWidth(1);
			contractTopTable1.addCell(nested7);

			PdfPCell nested8 = new PdfPCell(new Phrase(" ", font));
			nested8.setVerticalAlignment(Element.ALIGN_CENTER);
			nested8.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested8.setBorderWidth(1);
			contractTopTable1.addCell(nested8);

			PdfPCell nested9 = new PdfPCell(new Phrase("FinancialYear :", font));
			nested9.setVerticalAlignment(Element.ALIGN_CENTER);
			nested9.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nested9.setBorderWidth(1);
			contractTopTable1.addCell(nested9);

			PdfPCell nested10 = new PdfPCell(new Phrase(" ", font));
			nested10.setVerticalAlignment(Element.ALIGN_CENTER);
			nested10.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested10.setBorderWidth(1);
			contractTopTable1.addCell(nested10);

			PdfPCell nested11 = new PdfPCell(new Phrase("Address of the Client :", font));
			nested11.setVerticalAlignment(Element.ALIGN_CENTER);
			nested11.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nested11.setBorderWidth(1);
			contractTopTable1.addCell(nested11);

			PdfPCell nested12 = new PdfPCell(new Phrase(" ", font));
			nested12.setVerticalAlignment(Element.ALIGN_CENTER);
			nested12.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested12.setBorderWidth(1);
			contractTopTable1.addCell(nested12);

			PdfPCell nested13 = new PdfPCell(new Phrase("", font));
			nested13.setVerticalAlignment(Element.ALIGN_CENTER);
			nested13.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested13.setBorderWidth(1);
			contractTopTable1.addCell(nested13);

			PdfPCell nested14 = new PdfPCell(new Phrase(" ", font));
			nested14.setVerticalAlignment(Element.ALIGN_CENTER);
			nested14.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested14.setBorderWidth(1);
			contractTopTable1.addCell(nested14);

			PdfPCell nested15 = new PdfPCell(new Phrase(" ", font));
			nested15.setVerticalAlignment(Element.ALIGN_CENTER);
			nested15.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested15.setBorderWidth(1);
			contractTopTable1.addCell(nested15);

			PdfPCell nested16 = new PdfPCell(new Phrase("Maharashtra-", font));
			nested16.setVerticalAlignment(Element.ALIGN_CENTER);
			nested16.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested16.setBorderWidth(1);
			contractTopTable1.addCell(nested16);

//			adding client details
			PdfPCell nestedAddcell1 = new PdfPCell(contractTopTable1);
			nestedAddcell1.setBorderWidth(1);
			nestedAddcell1.setHorizontalAlignment(Element.ALIGN_CENTER);
			nestedTable.addCell(nestedAddcell1);

			PdfPCell space = new PdfPCell(new Phrase(" ", font));
			space.setVerticalAlignment(Element.ALIGN_LEFT);
			space.setHorizontalAlignment(Element.ALIGN_LEFT);
			space.setBorderWidth(0);
			nestedTable.addCell(space);

			float[] column2 = { 30, 15, 8, 10, 10, 10, 10, 10, 5, 5, 10, 10, 10, 10, 10 };
			PdfPTable contractTopTable2 = new PdfPTable(column2);

			PdfPCell nested17 = new PdfPCell(new Phrase("Security/Commodity Description Trade", font1));
			nested17.setVerticalAlignment(Element.ALIGN_CENTER);
			nested17.setHorizontalAlignment(Element.ALIGN_LEFT);
			nested17.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested17);

			PdfPCell nested18 = new PdfPCell(new Phrase("Trade Date", font1));
			nested18.setVerticalAlignment(Element.ALIGN_CENTER);
			nested18.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested18.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested18);

			PdfPCell nested19 = new PdfPCell(new Phrase("Exch-\r\nange", font1));
			nested19.setVerticalAlignment(Element.ALIGN_CENTER);
			nested19.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested19.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested19);

			PdfPCell nested20 = new PdfPCell(new Phrase("Segment", font1));
			nested20.setVerticalAlignment(Element.ALIGN_CENTER);
			nested20.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested20.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested20);

			PdfPCell nested21 = new PdfPCell(new Phrase("Purchase\r\nQuantity", font1));
			nested21.setVerticalAlignment(Element.ALIGN_CENTER);
			nested21.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested21.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested21);

			PdfPCell nested22 = new PdfPCell(new Phrase("Net\r\nPurchase\r\nValue", font1));
			nested22.setVerticalAlignment(Element.ALIGN_CENTER);
			nested22.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested22.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested22);

			PdfPCell nested23 = new PdfPCell(new Phrase("Sell\r\nQuantity", font1));
			nested23.setVerticalAlignment(Element.ALIGN_CENTER);
			nested23.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested23.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested23);

			PdfPCell nested24 = new PdfPCell(new Phrase("Net\r\nSell\r\nValue", font1));
			nested24.setVerticalAlignment(Element.ALIGN_CENTER);
			nested24.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested24.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested24);

			PdfPCell nested25 = new PdfPCell(new Phrase("GST", font1));
			nested25.setVerticalAlignment(Element.ALIGN_CENTER);
			nested25.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested25.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested25);

			PdfPCell nested26 = new PdfPCell(new Phrase("STT/CTT", font1));
			nested26.setVerticalAlignment(Element.ALIGN_CENTER);
			nested26.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested26.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested26);

			PdfPCell nested27 = new PdfPCell(new Phrase("Transaction\r\ncharges", font1));
			nested27.setVerticalAlignment(Element.ALIGN_CENTER);
			nested27.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested27.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested27);

			PdfPCell nested28 = new PdfPCell(new Phrase("SEBI\r\nFEES", font1));
			nested28.setVerticalAlignment(Element.ALIGN_CENTER);
			nested28.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested28.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested28);

			PdfPCell nested29 = new PdfPCell(new Phrase("Stamp\r\nDuty", font1));
			nested29.setVerticalAlignment(Element.ALIGN_CENTER);
			nested29.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested29.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested29);

			PdfPCell nested30 = new PdfPCell(new Phrase("Other\r\nCharges", font1));
			nested30.setVerticalAlignment(Element.ALIGN_CENTER);
			nested30.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested30.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested30);

			PdfPCell nested31 = new PdfPCell(new Phrase("Net\r\nSettlement\r\nAmount", font1));
			nested31.setVerticalAlignment(Element.ALIGN_CENTER);
			nested31.setHorizontalAlignment(Element.ALIGN_CENTER);
			nested31.setBackgroundColor(myColor);
			contractTopTable2.addCell(nested31);

			for (int i = 0; i < 10; i++) {

				PdfPCell nested32 = new PdfPCell(new Phrase(" ", font1));
				nested32.setVerticalAlignment(Element.ALIGN_CENTER);
				nested32.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested32);

				PdfPCell nested33 = new PdfPCell(new Phrase(" ", font1));
				nested33.setVerticalAlignment(Element.ALIGN_CENTER);
				nested33.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested33);

				PdfPCell nested34 = new PdfPCell(new Phrase(" ", font1));
				nested34.setVerticalAlignment(Element.ALIGN_CENTER);
				nested34.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested34);

				PdfPCell nested46 = new PdfPCell(new Phrase(" ", font1));
				nested46.setVerticalAlignment(Element.ALIGN_CENTER);
				nested46.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested46);

				PdfPCell nested35 = new PdfPCell(new Phrase(" ", font1));
				nested35.setVerticalAlignment(Element.ALIGN_CENTER);
				nested35.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested35);

				PdfPCell nested36 = new PdfPCell(new Phrase(" ", font1));
				nested36.setVerticalAlignment(Element.ALIGN_CENTER);
				nested36.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested36);

				PdfPCell nested37 = new PdfPCell(new Phrase(" ", font1));
				nested37.setVerticalAlignment(Element.ALIGN_CENTER);
				nested37.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested37);

				PdfPCell nested38 = new PdfPCell(new Phrase(" ", font1));
				nested38.setVerticalAlignment(Element.ALIGN_CENTER);
				nested38.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested38);

				PdfPCell nested39 = new PdfPCell(new Phrase(" ", font1));
				nested39.setVerticalAlignment(Element.ALIGN_CENTER);
				nested39.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested39);

				PdfPCell nested40 = new PdfPCell(new Phrase(" ", font1));
				nested40.setVerticalAlignment(Element.ALIGN_CENTER);
				nested40.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested40);

				PdfPCell nested41 = new PdfPCell(new Phrase(" ", font1));
				nested41.setVerticalAlignment(Element.ALIGN_CENTER);
				nested41.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested41);

				PdfPCell nested42 = new PdfPCell(new Phrase(" ", font1));
				nested42.setVerticalAlignment(Element.ALIGN_CENTER);
				nested42.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested42);

				PdfPCell nested43 = new PdfPCell(new Phrase(" ", font1));
				nested43.setVerticalAlignment(Element.ALIGN_CENTER);
				nested43.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested43);

				PdfPCell nested44 = new PdfPCell(new Phrase(" ", font1));
				nested44.setVerticalAlignment(Element.ALIGN_CENTER);
				nested44.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested44);

				PdfPCell nested45 = new PdfPCell(new Phrase(" ", font1));
				nested45.setVerticalAlignment(Element.ALIGN_CENTER);
				nested45.setHorizontalAlignment(Element.ALIGN_CENTER);
				contractTopTable2.addCell(nested45);

			}

//			adding column
			PdfPCell nestedAddcell2 = new PdfPCell(contractTopTable2);
			nestedAddcell2.setBorderWidth(1);
			nestedTable.addCell(nestedAddcell2);

			nestedTable.addCell(space);

			float[] column5 = { 4, 80 };
			PdfPTable contractTopTable5 = new PdfPTable(column5);

			PdfPCell nestedBottom1 = new PdfPCell(new Phrase("Disclaimer:", textFont7));
			nestedBottom1.setVerticalAlignment(Element.ALIGN_RIGHT);
			nestedBottom1.setHorizontalAlignment(Element.ALIGN_RIGHT);
			nestedBottom1.setBorderWidth(0);
			contractTopTable5.addCell(nestedBottom1);

			PdfPCell nestedBottom10 = new PdfPCell(new Phrase(
					" Figures are rounded-off to 2 decimal for your convenience, kindly refer the Contract Notes for accurate values.",
					font1));
			nestedBottom10.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom10.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom10.setBorderWidth(0);
			contractTopTable5.addCell(nestedBottom10);

			PdfPCell nestedAddcell5 = new PdfPCell(contractTopTable5);
			nestedAddcell5.setBorderWidth(0);
			nestedTable.addCell(nestedAddcell5);

			PdfPTable contractTopTable3 = new PdfPTable(column1);

			PdfPCell nestedBottom2 = new PdfPCell(
					new Phrase("* Net Purchase / Net Sale Value in inclusive of Brokerage.", font1));
			nestedBottom2.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom2.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom2.setBorderWidth(0);
			contractTopTable3.addCell(nestedBottom2);

			PdfPCell nestedBottom3 = new PdfPCell(new Phrase(
					"if you require any further information/ details the AGTS, you may write us at query@motilaloswal.com",
					font1));
			nestedBottom3.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom3.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom3.setBorderWidth(0);
			nestedBottom3.setPaddingLeft(100);
			contractTopTable3.addCell(nestedBottom3);

			PdfPCell nestedBottom4 = new PdfPCell(
					new Phrase("Customer Service Contact Numbers: 022-40548000/67490600", font1));
			nestedBottom4.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom4.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom4.setBorderWidth(0);
			contractTopTable3.addCell(nestedBottom4);

			PdfPCell nestedBottom5 = new PdfPCell(
					new Phrase("This is a computer-generated statement & hence no signature is required", font1));
			nestedBottom5.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom5.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom5.setBorderWidth(0);
			contractTopTable3.addCell(nestedBottom5);

			PdfPCell nestedBottom6Dev = new PdfPCell(new Phrase("developed by sameer", font1));
			nestedBottom6Dev.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom6Dev.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom6Dev.setBorderWidth(0);
			contractTopTable3.addCell(nestedBottom6Dev);

//			adding bottom
			PdfPCell nestedAddcell3 = new PdfPCell(contractTopTable3);
			nestedAddcell3.setBorderWidth(0);
			nestedTable.addCell(nestedAddcell3);

			float[] column3 = { 5, 150 };

			PdfPTable contractTopTable4 = new PdfPTable(column3);

			PdfPCell nestedBottom6 = new PdfPCell(new Phrase("Date :", font1));
			nestedBottom6.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom6.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom6.setBorderWidth(0);
			contractTopTable4.addCell(nestedBottom6);

			PdfPCell nestedBottom7 = new PdfPCell(new Phrase("Test", font1));
			nestedBottom7.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom7.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom7.setBorderWidth(0);
			contractTopTable4.addCell(nestedBottom7);

			PdfPCell nestedBottom8 = new PdfPCell(new Phrase("Place :", font1));
			nestedBottom8.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom8.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom8.setBorderWidth(0);
			contractTopTable4.addCell(nestedBottom8);

			PdfPCell nestedBottom9 = new PdfPCell(new Phrase("Mumbai", font1));
			nestedBottom9.setVerticalAlignment(Element.ALIGN_LEFT);
			nestedBottom9.setHorizontalAlignment(Element.ALIGN_LEFT);
			nestedBottom9.setBorderWidth(0);
			contractTopTable4.addCell(nestedBottom9);

			PdfPCell nestedAddcell4 = new PdfPCell(contractTopTable4);
			nestedAddcell4.setBorderWidth(0);
			nestedTable.addCell(nestedAddcell4);

			document.add(nestedTable);
			document.close();

		} catch (Exception e) {

		}

	}

}
