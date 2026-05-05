package com.geojit.contractnotes.equity_combinemargin.v1;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════
 *  GEOJIT EQUITY JSON FILE TRIGGER - S3 Event Triggered Lambda (FIXED)
 * ════════════════════════════════════════════════════════════════════════
 *
 * Triggered when JSON files are uploaded to json-s3-geojit bucket.
 * Downloads JSON, parses into DTO, generates PDF, uploads to pdf-s3-geojit.
 *
 * Flow:
 *   S3 Upload → This Lambda → Parse JSON → Generate PDF → Upload PDF ✅
 *
 * @author Geojit PDF Team
 * @version 6.1 - FIXED: Now uploads PDF to S3
 */
public class EquityCombineMarginJsonFileBaseTrigger implements RequestHandler<S3Event, String> {

    // ═══════════════════════════════════════════════════════════════════
    //  CONSTANTS
    // ═══════════════════════════════════════════════════════════════════

    private static final String PDF_BUCKET = "pdf-s3-geojit";

    // ═══════════════════════════════════════════════════════════════════
    //  DTO LISTS - Instance Level (cleared after each processing)
    // ═══════════════════════════════════════════════════════════════════

    private static List<CustomerModel> customerList = new ArrayList<>();
    private static List<DealingOfficeAddress> dealingOfficeAddressList = new ArrayList<>();
    private static List<FooterModelV2> footerList = new ArrayList<>();
    private static List<FOHeaderTypeModel> foHeaderTypeList = new ArrayList<>();
    private static List<OHeaderTypeModel> oHeaderTypeList = new ArrayList<>();
    private static List<DHeaderTypeModel> dHeaderTypeList = new ArrayList<>();
    private static List<SCapitalHeaderTypeModel> sCapitalHeaderTypeList = new ArrayList<>();
    private static List<SFuturesHeaderTypeModel> sFuturesHeaderTypeList = new ArrayList<>();
    private static List<CAHeaderTypeModel> caHeaderTypeList = new ArrayList<>();
    private static List<SSHeaderTypeModel> ssHeaderTypeList = new ArrayList<>();
    private static List<STTHeaderTypeModel> sttHeaderTypeList = new ArrayList<>();
    private static List<PHeaderTypeModel> pHeaderTypeList = new ArrayList<>();
    private static List<MHeaderTypeModel> mHeaderTypeList = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════════
    //  LAMBDA ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        EquityDtoV2 equityDto = null;
        File localJsonFile = null;
        File pdfFile = null;
        AmazonS3 s3Client = null;

        try {
            System.out.println("════════════════════════════════════════════════════════");
            System.out.println("  GEOJIT EQUITY PDF GENERATION - START");
            System.out.println("  Start Time: " + new Date());
            System.out.println("════════════════════════════════════════════════════════");

            // STEP 1: Extract S3 Event Details
            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);
            String srcBucket = record.getS3().getBucket().getName();
            String srcKey = record.getS3().getObject().getUrlDecodedKey();

            context.getLogger().log("[Trigger] Processing: " + srcBucket + "/" + srcKey);

            // STEP 2: Download JSON from S3
            s3Client = AmazonS3ClientBuilder.defaultClient();
            S3Object fullObject = s3Client.getObject(new GetObjectRequest(srcBucket, srcKey));
            S3ObjectInputStream s3is = fullObject.getObjectContent();

            Path path = Paths.get(srcKey);
            String fileName = path.getFileName().toString();
            System.out.println("[Trigger] File: " + fileName);

            String filePath = "/tmp/" + fileName;
            localJsonFile = new File(filePath);
            FileOutputStream fos = new FileOutputStream(localJsonFile);

            byte[] readBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = s3is.read(readBuffer)) > 0) {
                fos.write(readBuffer, 0, bytesRead);
            }

            fos.close();
            s3is.close();

            System.out.println("[Trigger] Downloaded: " + localJsonFile.length() + " bytes");

            // STEP 3: Parse JSON into DTO
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(filePath));
            JSONObject jsonObject = (JSONObject) obj;

            ObjectMapper mapper = new ObjectMapper();
            equityDto = mapper.readValue(jsonObject.toString(), EquityDtoV2.class);

            System.out.println("[Trigger] JSON parsed successfully");

            // STEP 4: Extract All Lists from DTO
            customerList = safeList(equityDto.getCustomerList());
            dealingOfficeAddressList = safeList(equityDto.getDealingOfficeAddressList());
            dHeaderTypeList = safeList(equityDto.getdHeaderTypeList());
            oHeaderTypeList = safeList(equityDto.getoHeaderTypeList());
            footerList = safeList(equityDto.getFooterList());
            foHeaderTypeList = safeList(equityDto.getFoHeaderTypeList());
            caHeaderTypeList = safeList(equityDto.getCaHeaderTypeList());
            sCapitalHeaderTypeList = safeList(equityDto.getSCapitalHeaderTypeList());
            sFuturesHeaderTypeList = safeList(equityDto.getSFuturesHeaderTypeList());
            ssHeaderTypeList = safeList(equityDto.getSsHeaderTypeList());
            sttHeaderTypeList = safeList(equityDto.getSttHeaderTypeList());
            pHeaderTypeList = safeList(equityDto.getpHeaderTypeList());
            mHeaderTypeList = safeList(equityDto.getmHeaderTypeList());

            sCapitalHeaderTypeList.sort(
                    Comparator.comparing(m -> m.getIsin() != null ? m.getIsin() : "")
            );

            System.out.println("[Trigger] Extracted Lists:");
            System.out.println("  → Customers: " + customerList.size());
            System.out.println("  → D Headers: " + dHeaderTypeList.size());
            System.out.println("  → O Headers: " + oHeaderTypeList.size());

            // STEP 5: Extract Primary Customer
            CustomerModel customer = customerList.isEmpty() ? null : customerList.get(0);
            DealingOfficeAddress dealingOffice = dealingOfficeAddressList.isEmpty() ?
                    null : dealingOfficeAddressList.get(0);

            if (customer == null) {
                throw new IllegalArgumentException("No customer data found in JSON");
            }

            System.out.println("[Trigger] Customer: " + customer.getName());
            System.out.println("[Trigger] Contract No: " + customer.getContractNo());

            // ✅ STEP 6: INITIALIZE PDF GENERATOR WITH FONTS
            System.out.println("[Trigger] Initializing PDF Generator...");

            DynamicValuePDF_Shivraj pdfGenerator = new DynamicValuePDF_Shivraj();

            // ✅ CRITICAL FIX: Initialize fonts BEFORE generating PDF
            try {
                pdfGenerator.initializeFonts();
                System.out.println("[Trigger] ✓ Fonts initialized successfully");
            } catch (Exception fontError) {
                System.err.println("[Trigger] ✗ Font initialization failed: " + fontError.getMessage());
                fontError.printStackTrace();
                throw new RuntimeException("Font initialization failed", fontError);
            }

            // STEP 7: Generate PDF
            System.out.println("[Trigger] Generating PDF...");

            pdfFile = pdfGenerator.generatePDF(
                    customer,
                    dealingOffice,
                    footerList,
                    foHeaderTypeList,
                    oHeaderTypeList,
                    dHeaderTypeList,
                    sCapitalHeaderTypeList,
                    sFuturesHeaderTypeList,
                    caHeaderTypeList,
                    ssHeaderTypeList,
                    sttHeaderTypeList,
                    pHeaderTypeList,
                    mHeaderTypeList
            );

            System.out.println("[Trigger] PDF generated successfully: " + pdfFile.getName());

            // STEP 8: Upload PDF TO S3
            if (pdfFile != null && pdfFile.exists()) {
                String pdfKey = "pdfs/" + customer.getContractNo() + "/" + pdfFile.getName();

                System.out.println("[Trigger] Uploading PDF to S3...");
                System.out.println("  → Bucket: " + PDF_BUCKET);
                System.out.println("  → Key: " + pdfKey);
                System.out.println("  → Size: " + pdfFile.length() + " bytes");

                Map<String, String> userMeta = new HashMap<>();
                userMeta.put("partycode", safe(customer.getPartycode()));
                userMeta.put("email",     safe(customer.getEmail()));
                userMeta.put("tradedate", safe(customer.getTransactionDate()));
                userMeta.put("contractno", safe(customer.getContractNo()));
                ObjectMetadata pdfMeta = new ObjectMetadata();
                pdfMeta.setUserMetadata(userMeta);
                s3Client.putObject(new PutObjectRequest(PDF_BUCKET, pdfKey, pdfFile).withMetadata(pdfMeta));

                System.out.println("[Trigger] ✓ PDF uploaded successfully to S3");
            } else {
                System.err.println("[Trigger] ✗ PDF file is null or doesn't exist!");
                return "FAILED: PDF generation failed";
            }

            // STEP 9: Clear All Lists
            clearAllLists();

            System.out.println("════════════════════════════════════════════════════════");
            System.out.println("  GEOJIT EQUITY PDF GENERATION - COMPLETED");
            System.out.println("  Contract No: " + customer.getContractNo());
            System.out.println("  End Time: " + new Date());
            System.out.println("════════════════════════════════════════════════════════");

            return "SUCCESS";

        } catch (Exception e) {
            System.err.println("✗ Exception in EquityJsonFileBaseTrigger: " + e.getMessage());
            e.printStackTrace();
            context.getLogger().log("ERROR: " + e.getMessage());
            return "FAILED: " + e.getMessage();

        } finally {
            if (localJsonFile != null && localJsonFile.exists()) {
                localJsonFile.delete();
            }
            if (pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }
            if (s3Client != null) {
                s3Client.shutdown();
            }
        }
    }
    // ═══════════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════

    private static String safe(String s) {
        return s != null ? s.trim() : "";
    }

    /**
     * Returns the list itself or an empty mutable list when null
     */
    private static <T> List<T> safeList(List<T> src) {
        return src != null ? src : new ArrayList<>();
    }

    /**
     * Clears all DTO lists after PDF generation
     */
    private static void clearAllLists() {
        customerList.clear();
        dealingOfficeAddressList.clear();
        dHeaderTypeList.clear();
        oHeaderTypeList.clear();
        footerList.clear();
        foHeaderTypeList.clear();
        caHeaderTypeList.clear();
        sCapitalHeaderTypeList.clear();
        sFuturesHeaderTypeList.clear();
        ssHeaderTypeList.clear();
        sttHeaderTypeList.clear();
        pHeaderTypeList.clear();
        mHeaderTypeList.clear();
    }
}