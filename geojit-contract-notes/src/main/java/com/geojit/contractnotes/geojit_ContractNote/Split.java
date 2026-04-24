package com.geojit.contractnotes.geojit_ContractNote;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class Split implements RequestHandler<S3Event, String> {
    private static final Logger logger = LoggerFactory.getLogger(Split.class);
    private static String s3FileName = null;

    // Configuration - DEV environment (ap-south-1)
    // private static final String ERROR_LOG_BUCKET   = "geojit-error-bucket-prod";  // PROD
    private static final String ERROR_LOG_BUCKET   = "geojit-error-bucket-dev";
    // private static final String CHUNKS_BUCKET      = "chunks-s3-geojit-prod";     // PROD
    private static final String CHUNKS_BUCKET      = "chunks-s3-geojit-dev";
    private static final String SQS_QUEUE_NAME     = "geojit-map-split-processing-queue.fifo";
    private static final String INVOKE_LAMBDA_NAME = "invoke-lambda-geojit";
    private static final String STATUS_QUEUE_NAME  = "geojit-pipeline-status-queue.fifo";

    // Delimiter used in the raw file
    private static final String DELIMITER = "~";

    AmazonS3 s3Client;
    String bucketName  = "";
    String fileSuffix  = "";
    File remainingRecordsFile = null;
    String jobId = null;  // traceId from API — read from S3 object metadata
    int totalCustomerCount = 0;   // tracked for JOB_REGISTERED event
    String firstTradeDate  = null; // captured from first H record · included in JOB_REGISTERED + SPLIT_PROGRESS

    // ─── Field length constants (from actual data analysis) ───────────────────
    final int H_LEN = 15;   // Header
    final int E_LEN = 7;    // Exchange
    final int P_LEN = 16;   // Position (single format only)
    final int V_LEN = 12;   // V - Contract Description (was old D 12-field format)
    final int O_LEN = 14;   // Obligation
    final int F_LEN = 4;    // Footer
    final int N_LEN = 14;   // Note
    final int D_LEN = 16;   // Detail - Trade Detail (single format only)
    final int M_LEN = 12;   // Margin
    final int T_LEN = 5;    // Total
    final int U_LEN = 11;   // U - Position Summary (was old P 11-field format)
    final int C_LEN = 18;   // Contract
    final int R_LEN = 3;    // Rounded Total
    final int A_LEN = 5;    // Amount
    final int L_LEN = 10;   // Lot
    final int G_LEN = 17;   // General
    final int J_LEN = 15;   // Journal
    final int K_LEN = 7;    // Security
    final int Q_LEN = 6;    // Quantity

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        S3Object fullObject = null;
        SimpleDateFormat istFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        istFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        long startTimeMillis = System.currentTimeMillis();

        try {
            logger.info("Geojit Split Lambda Started | startTime={}", istFormat.format(new Date()));


            S3EventNotificationRecord record = s3event.getRecords().get(0);
            bucketName  = record.getS3().getBucket().getName();
            s3FileName  = record.getS3().getObject().getUrlDecodedKey();

            Random rand = new Random();
            int randNumber = rand.nextInt(1000);
            fileSuffix = new SimpleDateFormat(randNumber + "_yyyyMMddHHmmssSSS'.txt'").format(new Date());
            remainingRecordsFile = new File("/tmp/" + fileSuffix);

            s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.AP_SOUTH_1)
                    .build();

            // Read jobId (traceId) from S3 object metadata — set by API upload
            ObjectMetadata rawMetadata = s3Client.getObjectMetadata(bucketName, s3FileName);
            jobId = rawMetadata.getUserMetaDataOf("jobid");
            logger.info("JobId from S3 metadata: {}", jobId);

            fullObject = s3Client.getObject(new GetObjectRequest(bucketName, s3FileName));
            processStatementFile(fullObject.getObjectContent());

        } catch (Exception e) {
            ExceptionPublisher.publish(jobId, "Split", null, e);
            logger.error("Exception in Geojit Split Lambda: {}", e.getMessage(), e);
        } finally {
            try {
                if (remainingRecordsFile != null && remainingRecordsFile.exists() && remainingRecordsFile.length() > 0) {
                    uploadChunkAndNotify(remainingRecordsFile, fileSuffix);
                    remainingRecordsFile.delete();
                }

                File errorFile = new File("/tmp/Failurefile.txt");
                if (errorFile.exists() && errorFile.length() > 0) {
                    uploadErrorFile(errorFile);
                    errorFile.delete();
                }

                triggerInvokeLambda();

                // ── Send JOB_REGISTERED status event to status SQS ──
                sendJobRegisteredEvent();

            } catch (Exception e) {
                logger.error("Error in finally block: {}", e.getMessage(), e);
            }

            if (s3Client != null) s3Client.shutdown();
            if (fullObject != null) {
                try { fullObject.close(); } catch (IOException e) { logger.error("Error closing S3 object: {}", e.getMessage(), e); }
            }
            long durationSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
            logger.info("Geojit Split Lambda Completed | endTime={} | totalTimeTaken={}s",
                    istFormat.format(new Date()), durationSeconds);
        }
        return "successful";
    }

    private void processStatementFile(S3ObjectInputStream input) {
        BufferedReader reader = null;
        try {
            List<String> currentCustomerLines = new ArrayList<>();
            String currentCustomerId = null;
            boolean currentCustomerValid = true;
            int customerCount = 0;
            boolean firstLine = true;
            boolean currentCustomerHasHeader = false;

            reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            String line;

            while ((line = reader.readLine()) != null) {
                // Remove UTF-8 BOM from first line
                if (firstLine) {
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    firstLine = false;
                }
                if (line.trim().isEmpty()) continue;

                String[] strFields = line.split(DELIMITER, -1);

                // ═══════════════════════════════════════════════════════════
                // CRITICAL FIX: Detect customer boundary changes
                // Customer boundary detected by either:
                // 1. H record (proper customer start), OR
                // 2. Different customer ID (orphaned customer without H record)
                // ═══════════════════════════════════════════════════════════
                if (strFields.length >= 2) {
                    String recordId = strFields[0];
                    String recordType = strFields[1];

                    // Check if this is a new customer

                    boolean isNewCustomer = recordType.equals("H") ||
                            (currentCustomerId != null && !recordId.equals(currentCustomerId));

                    if (isNewCustomer && !currentCustomerLines.isEmpty()) {
                        // Save previous customer BEFORE starting new one

                        if (currentCustomerHasHeader) {
                            writeCustomerData(currentCustomerLines, currentCustomerValid, customerCount);
                            customerCount++;
                            totalCustomerCount++;
                            // Send live progress every 1000 customers
                            if (totalCustomerCount % 1000 == 0) {
                                sendSplitProgressEvent(totalCustomerCount);
                            }
                        }
                        // Reset for new customer
                        currentCustomerLines = new ArrayList<>();
                        currentCustomerValid = true;
                        currentCustomerHasHeader = false;// Reset validation flag
                    }

                    // Handle new customer start
                    if (recordType.equals("H")) {
                        // Proper customer with Header record
                        currentCustomerHasHeader = true;
                        currentCustomerValid = (strFields.length == H_LEN);
                        currentCustomerId = recordId;
                        currentCustomerLines.add(line);
                        // Capture tradeDate from field[3] of the very first H record
                        if (firstTradeDate == null && strFields.length >= 4 && !strFields[3].trim().isEmpty()) {
                            firstTradeDate = strFields[3].trim();
                        }

//                        System.out.println("Processing Header #" + customerCount +
//                                " - ID: '" + currentCustomerId + "', Fields: " + strFields.length +
//                                (currentCustomerValid ? " (VALID)" : " (INVALID)"));
                        continue;

                    } else if (currentCustomerId == null || !recordId.equals(currentCustomerId)) {
                        // Orphaned customer without Header record
//                        System.err.println("WARNING: Customer '" + recordId +
//                                "' starts without H record at line - MARKED AS INVALID");
                        currentCustomerId = recordId;
                        currentCustomerValid = false;  // No header = automatically invalid
                        currentCustomerLines.add(line);
                        continue;
                    }
                }

                // Validate non-header lines for current customer
                boolean lineValid = validateLine(strFields, currentCustomerId);
                if (!lineValid) {
                    currentCustomerValid = false;
                }
                currentCustomerLines.add(line);
            }

            // Save last customer
            if (!currentCustomerLines.isEmpty()  && currentCustomerHasHeader) {
//                System.out.println("--- Saving LAST customer #" + customerCount +
//                        " (ID: " + currentCustomerId +
//                        ", Valid: " + currentCustomerValid +
//                        ", Lines: " + currentCustomerLines.size() + ")");
                writeCustomerData(currentCustomerLines, currentCustomerValid, customerCount);
                customerCount++;
                totalCustomerCount++;
            } else {
                logger.info("WARNING: Last customer lines are EMPTY!");
            }

            logger.info("Customer Parsing completed | totalCustomers={}", customerCount);

        } catch (IOException e) {
            logger.error("Error in processStatementFile: {}", e.getMessage(), e);
        } finally {
            try {
                if (reader != null) reader.close();
                if (input  != null) input.close();
            } catch (IOException e) {
                logger.error("Error closing reader/input: {}", e.getMessage(), e);
            }
        }
    }

    private boolean validateLine(String[] strFields, String currentCustomerId) {
        if (strFields.length < 2) {
//            System.out.println("Validation FAILED: Insufficient fields (" + strFields.length + ")");
            return false;
        }

        String lineType = strFields[1];
        String recordId = strFields[0];

        if (!recordId.equals(currentCustomerId)) {
//            System.out.println(lineType + " validation FAILED: Expected ID: '" + currentCustomerId + "', Got: '" + recordId + "'");
            return false;
        }

        boolean lineValid = true;
        switch (lineType) {
            case "E":
                lineValid = (strFields.length == E_LEN);
                if (!lineValid) logger.warn("Exchange validation FAILED | fields={}/{}", strFields.length, E_LEN);
                break;
            case "P":
                lineValid = (strFields.length == P_LEN);
                if (!lineValid) logger.warn("Position validation FAILED | fields={}/{}", strFields.length, P_LEN);
                break;
            case "V":
                lineValid = (strFields.length == V_LEN);
                if (!lineValid) logger.warn("V validation FAILED | fields={}/{}", strFields.length, V_LEN);
                break;
            case "D":
                lineValid = (strFields.length == D_LEN);
                if (!lineValid) logger.warn("Detail validation FAILED | fields={}/{}", strFields.length, D_LEN);
                break;
            case "O":
                lineValid = (strFields.length == O_LEN);
                if (!lineValid) logger.warn("Obligation validation FAILED | fields={}/{}", strFields.length, O_LEN);
                break;
            case "F":
                lineValid = (strFields.length == F_LEN);
                if (!lineValid) logger.warn("Footer validation FAILED | fields={}/{}", strFields.length, F_LEN);
                break;
            case "N":
                lineValid = (strFields.length == N_LEN);
                if (!lineValid) logger.warn("Note validation FAILED | fields={}/{}", strFields.length, N_LEN);
                break;
            case "M":
                lineValid = (strFields.length == M_LEN);
                if (!lineValid) logger.warn("Margin validation FAILED | fields={}/{}", strFields.length, M_LEN);
                break;
            case "T":
                lineValid = (strFields.length == T_LEN);
                if (!lineValid) logger.warn("Total validation FAILED | fields={}/{}", strFields.length, T_LEN);
                break;
            case "U":
                lineValid = (strFields.length == U_LEN);
                if (!lineValid) logger.warn("U validation FAILED | fields={}/{}", strFields.length, U_LEN);
                break;
            case "C":
                lineValid = (strFields.length == C_LEN);
                if (!lineValid) logger.warn("Contract validation FAILED | fields={}/{}", strFields.length, C_LEN);
                break;
            case "R":
                lineValid = (strFields.length == R_LEN);
                if (!lineValid) logger.warn("Rounded Total validation FAILED | fields={}/{}", strFields.length, R_LEN);
                break;
            case "A":
                lineValid = (strFields.length == A_LEN);
                if (!lineValid) logger.warn("Amount validation FAILED | fields={}/{}", strFields.length, A_LEN);
                break;
            case "L":
                lineValid = (strFields.length == L_LEN);
                if (!lineValid) logger.warn("Lot validation FAILED | fields={}/{}", strFields.length, L_LEN);
                break;
            case "G":
                lineValid = (strFields.length == G_LEN);
                if (!lineValid) logger.warn("General validation FAILED | fields={}/{}", strFields.length, G_LEN);
                break;
            case "J":
                lineValid = (strFields.length == J_LEN);
                if (!lineValid) logger.warn("Journal validation FAILED | fields={}/{}", strFields.length, J_LEN);
                break;
            case "K":
                lineValid = (strFields.length == K_LEN);
                if (!lineValid) logger.warn("Security validation FAILED | fields={}/{}", strFields.length, K_LEN);
                break;
            case "Q":
                lineValid = (strFields.length == Q_LEN);
                if (!lineValid) logger.warn("Quantity validation FAILED | fields={}/{}", strFields.length, Q_LEN);
                break;
            default:
                lineValid = false;
                logger.warn("Unknown line type: {}", lineType);
        }

        return lineValid;
    }

    private void writeCustomerData(List<String> lines, boolean isValid, int customerNum) throws IOException {
        if (isValid) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(remainingRecordsFile, true))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            long fileSizeInMB = remainingRecordsFile.length() / (1024 * 1024);
            if (fileSizeInMB > 250) {
                String chunkName = new SimpleDateFormat("yyyyMMddHHmmssSSS'.txt'").format(new Date());
                uploadChunkAndNotify(remainingRecordsFile, chunkName);

                remainingRecordsFile.delete();
                fileSuffix = new SimpleDateFormat("yyyyMMddHHmmssSSS'.txt'").format(new Date());
                remainingRecordsFile = new File("/tmp/" + fileSuffix);
            }
        } else {
            logger.warn("Customer #{} has validation errors | lines={}", customerNum, lines.size());
            try (BufferedWriter errorWriter = new BufferedWriter(new FileWriter(new File("/tmp/Failurefile.txt"), true))) {
                for (String line : lines) {
                    errorWriter.write(line);
                    errorWriter.newLine();
                }
            }
        }
    }

    private void uploadChunkAndNotify(File chunkFile, String chunkName) {
        try {
            String subFile = "StatementSubfile_" + chunkName;

            s3Client.putObject(new PutObjectRequest(CHUNKS_BUCKET, subFile, chunkFile));

            JSONObject json = new JSONObject();
            json.put("bucketName", CHUNKS_BUCKET);
            json.put("s3Key", subFile);
            if (jobId != null) json.put("jobId", jobId);  // pass traceId to Invoke Lambda

            AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
            String queueUrl = sqsClient.getQueueUrl(SQS_QUEUE_NAME).getQueueUrl();
            SendMessageRequest msgRequest = new SendMessageRequest(queueUrl, json.toString());
            msgRequest.setMessageGroupId(subFile);
            msgRequest.setMessageDeduplicationId(UUID.randomUUID().toString());

            SendMessageResult result = sqsClient.sendMessage(msgRequest);
            sqsClient.shutdown();

        } catch (Exception e) {
            logger.error("Error uploading chunk: {}", e.getMessage(), e);
        }
    }

    private void uploadErrorFile(File errorFile) {
        try {
            Date date = new Date();
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            String errorKey = String.format("FailurefileGeojit/%d/%d/%d/%s",
                    localDate.getYear(),
                    localDate.getMonthValue(),
                    localDate.getDayOfMonth(),
                    s3FileName);

            s3Client.putObject(new PutObjectRequest(ERROR_LOG_BUCKET, errorKey, errorFile));

        } catch (Exception e) {
            logger.error("Error uploading error file: {}", e.getMessage(), e);
        }
    }

    private void sendJobRegisteredEvent() {
        if (jobId == null) {
            logger.warn("No jobId — skipping JOB_REGISTERED status event");
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("totalCustomers", totalCustomerCount);
            payload.put("fileName", s3FileName != null ? s3FileName : "unknown");
            payload.put("tradeDate", firstTradeDate != null ? firstTradeDate : "");

            JSONObject event = new JSONObject();
            event.put("jobId", jobId);
            event.put("eventType", "JOB_REGISTERED");
            event.put("lambdaName", "split-lambda-geojit");
            event.put("partyCode", (String) null);
            event.put("payload", payload);
            event.put("eventTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

            AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
            String queueUrl = sqsClient.getQueueUrl(STATUS_QUEUE_NAME).getQueueUrl();
            SendMessageRequest msgRequest = new SendMessageRequest(queueUrl, event.toString());
            msgRequest.setMessageGroupId("status-" + jobId);
            msgRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
            sqsClient.sendMessage(msgRequest);
            sqsClient.shutdown();

            logger.info("JOB_REGISTERED event sent | jobId={} | totalCustomers={}", jobId, totalCustomerCount);
        } catch (Exception e) {
            logger.error("Error sending JOB_REGISTERED event: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends a SPLIT_PROGRESS event to the status queue every 1000 customers.
     * Not saved to DB — SSE-only for live UI counter.
     * Non-critical: failures are logged and swallowed so split continues.
     */
    private void sendSplitProgressEvent(int customersFound) {
        if (jobId == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("customersFound", customersFound);
            if (firstTradeDate != null) payload.put("tradeDate", firstTradeDate);

            JSONObject event = new JSONObject();
            event.put("jobId", jobId);
            event.put("eventType", "SPLIT_PROGRESS");
            event.put("lambdaName", "split-lambda-geojit");
            event.put("payload", payload);
            event.put("eventTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

            AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
            String queueUrl = sqsClient.getQueueUrl(STATUS_QUEUE_NAME).getQueueUrl();
            SendMessageRequest msgRequest = new SendMessageRequest(queueUrl, event.toString());
            msgRequest.setMessageGroupId("status-" + jobId);
            msgRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
            sqsClient.sendMessage(msgRequest);
            sqsClient.shutdown();

            logger.info("SPLIT_PROGRESS event sent | jobId={} | customersFound={}", jobId, customersFound);
        } catch (Exception e) {
            logger.warn("Failed to send SPLIT_PROGRESS event (non-critical) | customersFound={} | error={}",
                    customersFound, e.getMessage());
        }
    }

    private void triggerInvokeLambda() {
        try {
            logger.info("Invoking {} Lambda...", INVOKE_LAMBDA_NAME);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key", "first call");
            if (jobId != null) jsonObject.put("jobId", jobId);

            AWSLambda client = AWSLambdaAsyncClient.builder().withRegion(Regions.AP_SOUTH_1).build();
            InvokeRequest request = new InvokeRequest()
                    .withFunctionName(INVOKE_LAMBDA_NAME)
                    .withInvocationType("Event")
                    .withPayload(jsonObject.toString());

            client.invoke(request);
            client.shutdown();

            logger.info("Invoke Lambda triggered successfully");

        } catch (Exception e) {
            logger.error("Error triggering Invoke Lambda: {}", e.getMessage(), e);
        }
    }
}