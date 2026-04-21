package com.geojit.contractnotes.geojit_ContractNote;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Geojit PullDeliverSQS Lambda
 *
 * Processes email delivery success events delivered by SQS Event Source Mapping.
 * AWS automatically passes SQS messages via the input parameter as a Records array.
 * No manual SQS polling is needed — AWS handles message delivery and deletion.
 *
 * Trigger: SQS Event Source Mapping on geojit-deliver-email-queue
 *
 * Report bucket: geojit-report-files-s3-dev  (DEV) / geojit-report-files-s3-prod (PROD)
 *
 * S3 path patterns:
 *   GeojitCN-EmailReport/SentLog/{yyyy}/{MM}/{dd}/{partyCode}/
 *       {partyCode}_Sent_Log_GeojitCN_{timestamp}.csv             <- per-record
 *   GeojitCN-EmailReport/SentLogAllReport/{yyyy}/{MM}/{dd}/allreport/
 *       Sent_Log_GeojitCN_{timestamp}.csv                         <- batch (every 7500)
 *
 * CSV columns:
 *   CLIENT CODE, CLIENT NAME, CLIENT EMAIL, ACTIVITY DATE,
 *   CONTRACT NO, TRADE DATE, fileName, DELIVERY STATUS, Send Date Time
 */
public class PullDeliverSQS implements RequestHandler<Object, Object> {

    // ── Constants ──────────────────────────────────────────────────────────────
    // private static final String   REPORT_BUCKET    = "geojit-report-files-s3-prod";  // PROD
    private static final String   REPORT_BUCKET    = "geojit-report-files-s3-dev";
    private static final String   STATUS_QUEUE_NAME = "geojit-pipeline-status-queue.fifo";
    private static final int      BATCH_FLUSH_SIZE = 7500;
    private static final String[] DATE_FORMATS     = {"dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "dd.MM.yyyy"};


    // ── CSV header ─────────────────────────────────────────────────────────────
    private static final String CSV_HEADER =
            "CLIENT CODE,CLIENT NAME,CLIENT EMAIL,ACTIVITY DATE,CONTRACT NO,TRADE DATE,fileName,DELIVERY STATUS,Send Date Time";

    // ── Handler ────────────────────────────────────────────────────────────────
    @Override
    public Object handleRequest(Object input, Context context) {

        String    geojitTradedate = "";
        String    geojitExchange  = null;
        int       geojitCount     = 0;

        ClientConfiguration config = new ClientConfiguration();
        config.setConnectionTimeout(900_000);
        config.setSocketTimeout(900_000);
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.AP_SOUTH_1)
                .withClientConfiguration(config)
                .build();

        File      allReportFile   = new File("/tmp/geojit_sent_all.csv");
        File      singleFile      = new File("/tmp/geojit_sent_single.csv");

        // rows accumulator for batch all-report
        StringBuilder allReportRows = new StringBuilder();

        try {
            System.out.println("PullDeliverSQS started — reading from SQS event input.");

            ObjectMapper mapper    = new ObjectMapper();
            JSONObject   inputJson = new JSONObject(mapper.writeValueAsString(input));

            JSONArray records = inputJson.optJSONArray("Records");
            if (records == null || records.length() == 0) {
                System.out.println("No Records in input — exiting.");
                return null;
            }

            System.out.println("Received " + records.length() + " record(s) from SQS trigger.");

            SimpleDateFormat tsFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            tsFormatter.setTimeZone(TimeZone.getTimeZone("IST"));

            for (int r = 0; r < records.length(); r++) {
                try {
                    JSONObject sqsRecord   = records.getJSONObject(r);
                    String     messageBody = sqsRecord.getString("body");

                    // Unwrap SNS envelope
                    JSONObject json = new JSONObject(messageBody);
                    if (json.has("Message")) {
                        json = new JSONObject(json.getString("Message"));
                    }

                    System.out.println("SES eventType: " + json.optString("eventType"));

                    String eventType = json.optString("eventType");
                    if (!"Delivery".equals(eventType)) {
                        System.out.println("Skipping non-delivery event: " + eventType);
                        continue;
                    }

                    JSONObject mailJson     = json.optJSONObject("mail");
                    JSONObject deliveryJson = json.optJSONObject("delivery");

                    if (mailJson == null || mailJson.length() == 0) {
                        System.out.println("No mail object — skipping record.");
                        continue;
                    }

                    JSONArray headerArray = mailJson.optJSONArray("headers");
                    if (headerArray == null) {
                        System.out.println("No headers array — skipping record.");
                        continue;
                    }

                    for (int i = 0; i < headerArray.length(); i++) {
                        JSONObject header = headerArray.getJSONObject(i);
                        if (!"metajson".equals(header.optString("name"))) continue;

                        JSONObject meta      = new JSONObject(header.getString("value"));
                        String     partyCode = meta.optString("partycode");

                        // Sanitize partyCode — remove slash to avoid S3 folder nesting
                        String safePartyCode = partyCode.replace("/", "_").replace("\\", "_").trim();

                        if (!"geojit".equals(meta.optString("type"))) {
                            System.out.println("Skipping non-geojit type: " + meta.optString("type"));
                            break;
                        }

                        String tradedate = meta.optString("tradedate", "").trim();
                        if (tradedate.isEmpty()) {
                            System.out.println("Missing tradedate for partyCode: " + partyCode + " — skipping.");
                            break;
                        }

                        // Parse tradedate
                        Date tdDate = null;
                        for (String fmt : DATE_FORMATS) {
                            try {
                                tdDate = new SimpleDateFormat(fmt).parse(tradedate);
                                System.out.println("Parsed tradedate [" + tradedate + "] using format: " + fmt);
                                break;
                            } catch (Exception ignored) {}
                        }
                        if (tdDate == null) {
                            System.out.println("Could not parse tradedate: [" + tradedate + "] — skipping.");
                            break;
                        }

                        String tdDay   = new SimpleDateFormat("dd").format(tdDate);
                        String tdMonth = new SimpleDateFormat("MM").format(tdDate);
                        String tdYear  = new SimpleDateFormat("yyyy").format(tdDate);

                        // ── Build CSV fields ───────────────────────────────────
                        String clientCode    = escapeCsv(meta.optString("partycode"));
                        String clientName    = escapeCsv(meta.optString("name"));
                        String clientEmail   = escapeCsv(meta.optString("email"));
                        String activityDate  = escapeCsv(meta.optString("activitydate"));
                        String contractNo    = escapeCsv(meta.optString("contract-no"));
                        String fileName      = escapeCsv(meta.optString("filename"));
                        String sendDateTime  = escapeCsv(deliveryJson != null ? deliveryJson.optString("timestamp") : "");

                        String csvRow = clientCode + "," + clientName + "," + clientEmail + ","
                                + activityDate + "," + contractNo + "," + tradedate + ","
                                + fileName + "," + eventType + "," + sendDateTime;

                        geojitCount++;
                        geojitExchange  = "geojit";
                        geojitTradedate = tradedate;

                        allReportRows.append(csvRow).append("\n");

                        String timestamp = tsFormatter.format(new Date());

                        // ── Write per-record CSV to S3 ─────────────────────────
                        FileWriter singleWriter = new FileWriter(singleFile, false);
                        singleWriter.write(CSV_HEADER + "\n");
                        singleWriter.write(csvRow + "\n");
                        singleWriter.close();

                        String perRecordKey = "GeojitCN-EmailReport/SentLog/"
                                + tdYear + "/" + tdMonth + "/" + tdDay + "/"
                                + safePartyCode + "/" + safePartyCode
                                + "_Sent_Log_GeojitCN_" + timestamp + ".csv";

                        s3Client.putObject(new PutObjectRequest(REPORT_BUCKET, perRecordKey, singleFile));
                        singleFile.delete();
                        System.out.println("Wrote delivery record: " + perRecordKey);

                        // ── Send DELIVERY status event to pipeline status queue ──
                        String jobid = meta.optString("jobid", null);
                        if (jobid != null && !jobid.isEmpty()) {
                            sendDeliveryStatusEvent(jobid, partyCode, meta.optString("email"));
                        }

                        // ── Mid-batch flush ────────────────────────────────────
                        if (geojitCount >= BATCH_FLUSH_SIZE) {
                            flushAllReport(s3Client, allReportFile, allReportRows.toString(),
                                    tdYear, tdMonth, tdDay, timestamp,
                                    "SentLogAllReport", "Sent_Log_GeojitCN");
                            allReportRows = new StringBuilder();
                            geojitCount   = 0;
                        }

                        break;
                    }

                } catch (Exception e) {
                    System.err.println("Error processing record " + r + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // ── Final flush ────────────────────────────────────────────────────
            if (geojitExchange != null && allReportRows.length() > 0) {
                String timestamp = tsFormatter.format(new Date());

                Date tdDate = null;
                for (String fmt : DATE_FORMATS) {
                    try { tdDate = new SimpleDateFormat(fmt).parse(geojitTradedate); break; }
                    catch (Exception ignored) {}
                }
                if (tdDate != null) {
                    String tdDay   = new SimpleDateFormat("dd").format(tdDate);
                    String tdMonth = new SimpleDateFormat("MM").format(tdDate);
                    String tdYear  = new SimpleDateFormat("yyyy").format(tdDate);
                    flushAllReport(s3Client, allReportFile, allReportRows.toString(),
                            tdYear, tdMonth, tdDay, timestamp,
                            "SentLogAllReport", "Sent_Log_GeojitCN");
                }
            }

            System.out.println("PullDeliverSQS completed successfully.");

        } catch (Exception e) {
            System.err.println("Fatal error in PullDeliverSQS: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (allReportFile.exists()) allReportFile.delete();
            if (singleFile.exists())    singleFile.delete();
            s3Client.shutdown();
        }

        return null;
    }

    // ── Helper: flush batch CSV to S3 ─────────────────────────────────────────
    private void flushAllReport(AmazonS3 s3Client,
                                File     tmpFile,
                                String   csvRows,
                                String   tdYear,
                                String   tdMonth,
                                String   tdDay,
                                String   timestamp,
                                String   reportFolder,
                                String   filePrefix) {
        try {
            FileWriter writer = new FileWriter(tmpFile, false);
            writer.write(CSV_HEADER + "\n");
            writer.write(csvRows);
            writer.close();

            String s3Key = "GeojitCN-EmailReport/" + reportFolder + "/"
                    + tdYear + "/" + tdMonth + "/" + tdDay + "/allreport/"
                    + filePrefix + "_" + timestamp + ".csv";

            s3Client.putObject(new PutObjectRequest(REPORT_BUCKET, s3Key, tmpFile));
            System.out.println("Flushed all-report -> " + s3Key);
            tmpFile.delete();

        } catch (Exception e) {
            System.err.println("flushAllReport error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helper: escape CSV field (wrap in quotes if contains comma/quote/newline)
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void sendDeliveryStatusEvent(String jobid, String partyCode, String recipientEmail) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("recipientEmail", recipientEmail);

            JSONObject event = new JSONObject();
            event.put("jobId", jobid);
            event.put("eventType", "DELIVERY");
            event.put("lambdaName", "pull-delivery-geojit");
            event.put("partyCode", partyCode);
            event.put("payload", payload);
            event.put("eventTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

            AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
            String queueUrl = sqsClient.getQueueUrl(STATUS_QUEUE_NAME).getQueueUrl();
            SendMessageRequest msgRequest = new SendMessageRequest(queueUrl, event.toString());
            msgRequest.setMessageGroupId("status-" + jobid);
            msgRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
            sqsClient.sendMessage(msgRequest);
            sqsClient.shutdown();

            System.out.println("DELIVERY event sent | jobId=" + jobid + " | partyCode=" + partyCode);
        } catch (Exception e) {
            System.err.println("Error sending DELIVERY status event: " + e.getMessage());
        }
    }
}