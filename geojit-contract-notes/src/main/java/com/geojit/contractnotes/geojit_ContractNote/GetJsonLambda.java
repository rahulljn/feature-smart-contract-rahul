package com.geojit.contractnotes.geojit_ContractNote;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * GET JSON LAMBDA
 *
 * FLOW:
 *   Invoke.java (large payload >256KB)
 *       → saves JSON to S3 (cn-big-files/)
 *       → triggers GetJsonLambda with { bucketName, s3Key }
 *               ↓
 *   GetJsonLambda
 *       → passes { bucketName, s3Key } to CREATE PDF LAMBDA
 *       → JSON content NAHI padhta — sirf S3 key forward karta hai
 *               ↓
 *   DynamicValuePdf
 *       → S3 se JSON khud padhta hai bucketName + s3Key se
 *       → PDF generate karta hai
 */
public class GetJsonLambda implements RequestHandler<Object, Integer> {

    private static final Regions AWS_REGION      = Regions.AP_SOUTH_1;
    private static final String  PDF_LAMBDA_NAME = "create-pdf-geojit"; // DynamicValuePdf Lambda name
    private static final Logger logger = LoggerFactory.getLogger(GetJsonLambda.class);

    @Override
    public Integer handleRequest(Object input, Context context) {
        SimpleDateFormat istFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        istFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        logger.info("GetJsonLambda Started | startTime={}", istFormat.format(new Date()));


        AWSLambda lambdaClient = null;

        try {
            // ── Step 1: Input se bucketName aur s3Key lo ──
            ObjectMapper mapper      = new ObjectMapper();
            JSONObject   inputObject = new JSONObject(mapper.writeValueAsString(input));

            String bucketName = inputObject.getString("bucketName");
            String s3Key      = inputObject.getString("s3Key");

//            System.out.println("Received S3 reference: " + bucketName + "/" + s3Key);

            // ── Step 2: Sirf S3 key PDF Lambda ko pass karo ──
            // ✅ JSON content NAHI padhna yahan
            // ✅ DynamicValuePdf khud S3 se JSON padhega
            // ✅ Payload chhota rahega — 256KB limit kabhi exceed nahi hogi
            JSONObject pdfPayload = new JSONObject();
            pdfPayload.put("bucketName", bucketName);
            pdfPayload.put("s3Key", s3Key);

            lambdaClient = AWSLambdaAsyncClient.builder()
                    .withRegion(AWS_REGION)
                    .build();

            lambdaClient.invoke(new InvokeRequest()
                    .withFunctionName(PDF_LAMBDA_NAME)
                    .withInvocationType("Event")           // Async — fire and forget
                    .withPayload(pdfPayload.toString()));  // ✅ Sirf ~100 bytes ka payload

//            System.out.println("✓ CREATE PDF LAMBDA triggered with S3 key: " + s3Key);
            logger.info("GetJsonLambda Completed Successfully | endTime={}", istFormat.format(new Date()));
            return 200;

        } catch (Exception e) {
            logger.error("GetJsonLambda ERROR | error={} | endTime={}", e.getMessage(), istFormat.format(new Date()), e);
            return 500;
        } finally {
            if (lambdaClient != null) {
                try { lambdaClient.shutdown(); } catch (Exception ignored) {}
            }
        }
    }
}