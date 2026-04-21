//package com.geojit.contractnotes.geojit_ContractNote.V2;
//
//import com.amazonaws.regions.Regions;
//import com.amazonaws.services.lambda.AWSLambda;
//import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
//import com.amazonaws.services.lambda.model.InvokeRequest;
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.services.lambda.runtime.events.S3Event;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
//import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import com.amazonaws.services.s3.model.S3Object;
//import com.amazonaws.services.s3.model.S3ObjectInputStream;
//import com.amazonaws.services.sqs.AmazonSQS;
//import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
//import com.amazonaws.services.sqs.model.SendMessageRequest;
//import com.amazonaws.services.sqs.model.SendMessageResult;
//import org.json.JSONObject;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.security.SecureRandom;
//import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.*;
//
//public class Split2 implements RequestHandler<S3Event, String> {
//    private String s3FileName = null;
//    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
//    private static final String ERROR_LOG_BUCKET   = "geojit-error-bucket-prod";
//    private static final String CHUNKS_BUCKET      = "chunks-s3-geojit-prod";
//    private static final String SQS_QUEUE_NAME     = "geojit-map-split-processing-queue.fifo";
//    private static final String INVOKE_LAMBDA_NAME = "invoke-lambda-geojit";
//
//    // Delimiter used in the raw file
//    private static final String DELIMITER = "~";
//
//   private AmazonS3 s3Client;
//   private String bucketName  = "";
//   private String fileSuffix  = "";
//   private File remainingRecordsFile = null;
//
//    // ─── Field length constants (from actual data analysis) ───────────────────
//    private static final int H_LEN = 13;   // Header
//    private static final int E_LEN = 7;    // Exchange
//    private static final int P_LEN = 16;   // Position (single format only)
//    private static final int V_LEN = 12;   // V - Contract Description (was old D 12-field format)
//    private static final int O_LEN = 14;   // Obligation
//    private static final int F_LEN = 4;    // Footer
//    private static final int N_LEN = 14;   // Note
//    private static final int D_LEN = 16;   // Detail - Trade Detail (single format only)
//    private static final int M_LEN = 12;   // Margin
//    private static final int T_LEN = 5;    // Total
//    private static final int U_LEN = 11;   // U - Position Summary (was old P 11-field format)
//    private static final int C_LEN = 18;   // Contract
//    private static final int R_LEN = 3;    // Rounded Total
//    private static final int A_LEN = 5;    // Amount
//    private static final int L_LEN = 10;   // Lot
//    private static final int G_LEN = 17;   // General
//    private static final int J_LEN = 15;   // Journal
//    private static final int K_LEN = 7;    // Security
//    private static final int Q_LEN = 6;    // Quantity
//
//    @Override
//    public String handleRequest(S3Event s3event, Context context) {
//        S3Object fullObject = null;
//        try {
//            S3EventNotificationRecord record = s3event.getRecords().get(0);
//            bucketName  = record.getS3().getBucket().getName();
//            s3FileName  = record.getS3().getObject().getUrlDecodedKey();
//            int randNumber = SECURE_RANDOM.nextInt(1000);
//            fileSuffix = new SimpleDateFormat(randNumber + "_yyyyMMddHHmmssSSS'.txt'").format(new Date());
//            remainingRecordsFile = new File("/tmp/" + fileSuffix);
//
//            s3Client = AmazonS3ClientBuilder.standard()
//                    .withRegion(Regions.AP_SOUTH_1)
//                    .build();
//
//            fullObject = s3Client.getObject(new GetObjectRequest(bucketName, s3FileName));
//            processStatementFile(fullObject.getObjectContent());
//
//            System.out.println("Geojit Split Lambda Completed Successfully");
//
//
//        } catch (Exception e) {
//            System.err.println("Error: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            try {
//                if (remainingRecordsFile != null && remainingRecordsFile.exists() && remainingRecordsFile.length() > 0) {
//                    uploadChunkAndNotify(remainingRecordsFile, fileSuffix);
//                    boolean deleted = remainingRecordsFile.delete();
//                    if (!deleted) {
//                        System.err.println("Warning: Could not delete file: "
//                                + remainingRecordsFile.getName());
//                    }
//                }
//
//                File errorFile = new File("/tmp/Failurefile.txt");
//                if (errorFile.exists() && errorFile.length() > 0) {
//                    uploadErrorFile(errorFile);
//                    boolean errorDeleted = errorFile.delete();
//                    if (!errorDeleted) {
//                        System.err.println("Warning: Could not delete error file: "
//                                + errorFile.getName());
//                    }
//                }
//
//                triggerInvokeLambda();
//
//            } catch (Exception e) {
//                System.err.println("Error in finally block: " + e.getMessage());
//                e.printStackTrace();
//            }
//
//            if (s3Client != null) s3Client.shutdown();
//            if (fullObject != null) {
//                try { fullObject.close(); } catch (IOException e) { e.printStackTrace(); }
//            }
//        }
//        return "successful";
//    }
//
//    private void processStatementFile(S3ObjectInputStream input) {
//        BufferedReader reader = null;
//        try {
//            List<String> currentCustomerLines = new ArrayList<>();
//            String currentCustomerId = null;
//            boolean currentCustomerValid = true;
//            int customerCount = 0;
//            boolean firstLine = true;
//
//
//            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
//            String line;
//
//            System.out.println("Processing start time: " + new Date());
//
//            while ((line = reader.readLine()) != null) {
//                // Remove UTF-8 BOM from first line
//                if (firstLine) {
//                    if (line.startsWith("\uFEFF")) {
//                    }
//                    firstLine = false;
//                }
//
//                String[] strFields = line.split(DELIMITER, -1);
//
//                if (strFields.length >= 2 && strFields[1].equals("H")) {
//                    // Save previous customer BEFORE starting new one
//                    if (!currentCustomerLines.isEmpty()) {
//                        writeCustomerData(currentCustomerLines, currentCustomerValid, customerCount);
//                        customerCount++;
//                    }
//
//                    // Start new customer
//                    currentCustomerLines  = new ArrayList<>();
//                    currentCustomerValid = (strFields.length == H_LEN || strFields.length == H_LEN - 1);
//                    currentCustomerId = strFields[0];
//                    currentCustomerLines.add(line);
//                    continue;
//                }
//
//                // Validate non-header lines
//                boolean lineValid = validateLine(strFields, currentCustomerId);
//                if (!lineValid) {
//                    currentCustomerValid = false;
//                }
//                currentCustomerLines.add(line);
//            }
//
//            // Save last customer
//            if (!currentCustomerLines.isEmpty()) {
//                writeCustomerData(currentCustomerLines, currentCustomerValid, customerCount);
//                customerCount++;
//            } else {
//
//            }
//
//            System.out.println("Processing end time: " + new Date());
//
//        } catch (IOException e) {
//            System.err.println("Error in processStatementFile: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            try {
//                if (reader != null) reader.close();
//                if (input  != null) input.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private boolean validateLine(String[] strFields, String currentCustomerId) {
//        if (strFields.length < 2) {
//            return false;
//        }
//
//        String lineType = strFields[1];
//        String recordId = strFields[0];
//
//        if (!recordId.equals(currentCustomerId)) {
//
//            return false;
//        }
//
//        boolean lineValid = true;
//        switch (lineType) {
//            case "E":
//                lineValid = (strFields.length == E_LEN);
//                break;
//            case "P":
//                lineValid = (strFields.length == P_LEN);
//                break;
//            case "V":
//                lineValid = (strFields.length == V_LEN);
//                break;
//            case "D":
//                lineValid = (strFields.length == D_LEN);
//                break;
//            case "O":
//                lineValid = (strFields.length == O_LEN);
//                break;
//            case "F":
//                lineValid = (strFields.length == F_LEN);
//                break;
//            case "N":
//                lineValid = (strFields.length == N_LEN);
//                break;
//            case "M":
//                lineValid = (strFields.length == M_LEN);
//                break;
//            case "T":
//                lineValid = (strFields.length == T_LEN);
//                break;
//            case "U":
//                lineValid = (strFields.length == U_LEN);
//                break;
//            case "C":
//                lineValid = (strFields.length == C_LEN);
//                break;
//            case "R":
//                lineValid = (strFields.length == R_LEN);
//                break;
//            case "A":
//                lineValid = (strFields.length == A_LEN);
//                break;
//            case "L":
//                lineValid = (strFields.length == L_LEN);
//                break;
//            case "G":
//                lineValid = (strFields.length == G_LEN);
//                break;
//            case "J":
//                lineValid = (strFields.length == J_LEN);
//                break;
//            case "K":
//                lineValid = (strFields.length == K_LEN);
//                break;
//            case "Q":
//                lineValid = (strFields.length == Q_LEN);
//                break;
//            default:
//                lineValid = false;
//
//        }
//
//        return lineValid;
//    }
//
//    private void writeCustomerData(List<String> lines, boolean isValid, int customerNum) throws IOException {
//        if (isValid) {
//            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
//                    new FileOutputStream(remainingRecordsFile, true),
//                    StandardCharsets.UTF_8))) {
//                for (String line : lines) {
//                    writer.write(line);
//                    writer.newLine();
//                }
//            }
//
//            long fileSizeInMB = remainingRecordsFile.length() / (1024 * 1024);
//            if (fileSizeInMB > 250) {
//
//                String chunkName = new SimpleDateFormat("yyyyMMddHHmmssSSS'.txt'").format(new Date());
//                uploadChunkAndNotify(remainingRecordsFile, chunkName);
//
//                boolean deleted = remainingRecordsFile.delete();
//                if (!deleted) {
//                    System.err.println("Warning: Could not delete file: "
//                            + remainingRecordsFile.getName());
//                }
//                fileSuffix = new SimpleDateFormat("yyyyMMddHHmmssSSS'.txt'").format(new Date());
//                remainingRecordsFile = new File("/tmp/" + fileSuffix);
//            }
//        } else {
//
//            // ✅ FIX
//            try (BufferedWriter errorWriter = new BufferedWriter(new OutputStreamWriter(
//                    new FileOutputStream(new File("/tmp/Failurefile.txt"), true),
//                    StandardCharsets.UTF_8))) {
//                for (String line : lines) {
//                    errorWriter.write(line);
//                    errorWriter.newLine();
//                }
//            }
//        }
//    }
//
//    private void uploadChunkAndNotify(File chunkFile, String chunkName) {
//        try {
//            String subFile = "StatementSubfile_" + chunkName;
//
//            s3Client.putObject(new PutObjectRequest(CHUNKS_BUCKET, subFile, chunkFile));
//
//            JSONObject json = new JSONObject();
//            json.put("bucketName", CHUNKS_BUCKET);
//            json.put("s3Key", subFile);
//
//            AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
//            String queueUrl = sqsClient.getQueueUrl(SQS_QUEUE_NAME).getQueueUrl();
//            SendMessageRequest msgRequest = new SendMessageRequest(queueUrl, json.toString());
//            msgRequest.setMessageGroupId(subFile);
//            msgRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
//
//            sqsClient.sendMessage(msgRequest);
//            sqsClient.shutdown();
//
//        } catch (Exception e) {
//            System.err.println("Error uploading chunk: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void uploadErrorFile(File errorFile) {
//        try {
//
//            Date date = new Date();
//            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
//
//            String errorKey = String.format("FailurefileGeojit/%d/%d/%d/%s",
//                    localDate.getYear(),
//                    localDate.getMonthValue(),
//                    localDate.getDayOfMonth(),
//                    s3FileName);
//
//            s3Client.putObject(new PutObjectRequest(ERROR_LOG_BUCKET, errorKey, errorFile));
//
//        } catch (Exception e) {
//            System.err.println("Error uploading error file: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void triggerInvokeLambda() {
//        try {
//
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("key", "next call");
//
//            AWSLambda client = AWSLambdaAsyncClient.builder().withRegion(Regions.AP_SOUTH_1).build();
//            InvokeRequest request = new InvokeRequest()
//                    .withFunctionName(INVOKE_LAMBDA_NAME)
//                    .withInvocationType("Event")
//                    .withPayload(jsonObject.toString());
//
//            client.invoke(request);
//            client.shutdown();
//
//        } catch (Exception e) {
//            System.err.println("Error triggering Invoke Lambda: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}