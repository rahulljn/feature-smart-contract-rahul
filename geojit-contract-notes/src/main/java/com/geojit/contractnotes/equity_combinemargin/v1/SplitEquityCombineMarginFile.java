package com.geojit.contractnotes.equity_combinemargin.v1;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import com.amazonaws.services.s3.event.S3EventNotification;
import org.json.JSONObject;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.*;
import com.opencsv.CSVWriter;


public class SplitEquityCombineMarginFile implements RequestHandler<S3Event, String> {

    private static final String CHUNKS_BUCKET   = "chunks-s3-geojit";
    private static final String REPORT_BUCKET   = "geojit-report-files-s3";
    private static final String SQS_QUEUE_NAME  = "geojit-map-split-processing-queue.fifo";
    private static final long   CHUNK_LIMIT_KB  = 256;                     // split threshold in KB
    private static final long   CHUNK_LIMIT_B   = CHUNK_LIMIT_KB * 1024;

    private AmazonS3 s3Client;
    private String   errologBucket;          // read from application.properties
    private String   s3FileName;             // original key in raw bucket

    private File     chunkFile;              // current rolling chunk on /tmp
    private String   chunkSuffix;            // S3 key suffix for current chunk

    // Counter for chunk numbering (for unique deduplication IDs)
    private int      chunkCounter = 0;

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        S3Object fullObject = null;

        try {
            // ── 1. resolve source ──────────────────────────────────
            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);
            String bucketName = record.getS3().getBucket().getName();
            s3FileName        = record.getS3().getObject().getUrlDecodedKey();
            System.out.println("[Split] triggered for  " + bucketName + "/" + s3FileName);

            // ── 2. bootstrap chunk file ────────────────────────────
            chunkSuffix = generateFileSuffix(".txt");
            chunkFile   = new File("/tmp/" + chunkSuffix);
            chunkCounter = 0;  // Initialize counter

            // ── 3. load properties ─────────────────────────────────
            Properties prop = new Properties();
            prop.load(SplitEquityCombineMarginFile.class.getClassLoader()
                    .getResourceAsStream("application.properties"));
            errologBucket = prop.getProperty("errologBucket");

            // ── 4. S3 client with generous timeouts ────────────────
            ClientConfiguration config = new ClientConfiguration();
            config.setConnectionTimeout(900_000);
            config.setSocketTimeout(900_000);
            s3Client = AmazonS3ClientBuilder.standard()
                    .withClientConfiguration(config).build();

            // ── 5. stream & split ──────────────────────────────────
            fullObject = s3Client.getObject(new GetObjectRequest(bucketName, s3FileName));
            splitStream(fullObject.getObjectContent());

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            // ── 6a. flush leftover chunk to S3 + SQS ──────────────
            uploadAndEnqueueIfNonEmpty();

            // ── 6b. upload failure file (invalid records) ─────────
            uploadFailureFile();

            // ── 6c. write process-start time log ───────────────────
            writeProcessStartLog();

            // ── 6d. close & cleanup ────────────────────────────────
            closeQuietly(fullObject);
            if (s3Client != null) s3Client.shutdown();
        }

        return "SUCCESS";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CORE SPLIT LOGIC
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Reads the raw file line-by-line.  Groups by party code.  Uses the same
     * 7-flag validation pattern as Angel One so that every record type in a
     * party block is checked before the block is committed.
     */
    private void splitStream(S3ObjectInputStream input) throws IOException {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String  currentParty = null;
            boolean allOKFlag    = true;

            // per-party validity flags (reset on each new party)
            boolean flagH = false, flagD = false, flagO = false, flagF = false;
            boolean flagA = false, flagSCapital = false, flagSFutures = false;

            List<String> partyBuffer = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                String[] fields = line.split("\\|");
                if (fields.length < 2) {
                    allOKFlag = false;
                    partyBuffer.add(line);
                    continue;
                }

                String partyCode = fields[0].replaceAll("\\s+", "");

                // ── first line initialises party ───────────────────
                if (currentParty == null) {
                    currentParty = partyCode;
                }

                // ── party boundary detected ────────────────────────────
                if (!partyCode.equalsIgnoreCase(currentParty)) {
                    // CHECK FLAGS ONCE AT END OF PARTY
                    if (!(flagH && flagD && flagO && flagF && flagA && flagSCapital && flagSFutures)) {
                        allOKFlag = false;
                    }

                    // commit previous party's buffer
                    commitPartyBuffer(partyBuffer, allOKFlag);

                    // reset for new party
                    partyBuffer.clear();
                    currentParty   = partyCode;
                    allOKFlag      = true;
                    flagH          = false;  // CHANGE: start false, set true when seen
                    flagD          = false;
                    flagO          = false;
                    flagF          = false;
                    flagA          = false;
                    flagSCapital   = false;
                    flagSFutures   = false;
                }

                // ── per-record-type validation (NON-stateful, just count presence) ────
                String type = fields[1];
                switch (type) {
                    case "H":
                        if (fields.length == 15) flagH = true;
                        break;
                    case "A":
                        if (fields.length == 6) flagA = true;
                        break;
                    case "D":
                        if (fields.length == 23) flagD = true;
                        break;
                    case "O":
                        if (fields.length == 13) flagO = true;
                        break;
                    case "F":
                        if (fields.length == 16) flagF = true;
                        break;
                    case "S":
                        if (fields.length >= 3) {
                            if ("CAPITAL".equals(fields[2]) && fields.length == 17) {
                                flagSCapital = true;
                            } else if (("FUTURES".equals(fields[2]) || "OPTIONS".equals(fields[2])) && fields.length == 13) {
                                flagSFutures = true;
                            }
                        }
                        break;
                    // SS / STT / M / P / C are optional, don't gate validity
                    case "SS":
                    case "STT":
                    case "M":
                    case "P":
                    case "C":
                        // These are supplementary - presence validated but doesn't affect allOKFlag
                        break;
                    default:
                        // unknown record type
                        break;
                }

                partyBuffer.add(line);

                // gate: if any core flag failed, party is invalid
            }

            // ── flush last party ───────────────────────────────────
            if (!(flagH && flagD && flagO && flagF && flagA && flagSCapital && flagSFutures)) {
                allOKFlag = false;
            }
            commitPartyBuffer(partyBuffer, allOKFlag);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PARTY-BUFFER COMMIT  (valid → chunk file  |  invalid → failure file)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Writes the collected lines for one party either to the rolling chunk
     * file (valid) or to the failure file (invalid).  After writing to the
     * chunk, checks the 250-MB threshold and rotates if needed.
     */
    private void commitPartyBuffer(List<String> buffer, boolean valid) throws IOException {
        if (buffer.isEmpty()) return;

        File target = valid ? chunkFile : new File("/tmp/Failurefile.txt");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(target, true))) {
            for (String line : buffer) {
                bw.write(line);
                bw.newLine();
            }
        }

        // 250-MB rotation – only after writing to the VALID chunk
        if (valid && chunkFile.exists() && chunkFile.length() > CHUNK_LIMIT_B) {
            System.out.println("[Split] chunk size " + (chunkFile.length() / 1024)
                    + " KB – uploading and rotating.");
            uploadAndEnqueueIfNonEmpty();          // push current chunk
            chunkSuffix = generateFileSuffix(".txt");
            chunkFile   = new File("/tmp/" + chunkSuffix);   // fresh file
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  S3  +  SQS  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Uploads the chunk file to S3 and pushes its key onto the FIFO queue.
     * FIXED: Now includes MessageDeduplicationId for FIFO queue compliance.
     */
    private void uploadAndEnqueueIfNonEmpty() {
        if (chunkFile == null || !chunkFile.exists() || chunkFile.length() == 0) return;

        chunkCounter++;  // Increment for each chunk
        String s3Key = "ContractNoteSubfile_" + chunkSuffix;

        // ── S3 put ─────────────────────────────────────────────────
        s3Client.putObject(new PutObjectRequest(CHUNKS_BUCKET, s3Key, chunkFile));
        System.out.println("[Split] uploaded chunk #" + chunkCounter + "  " + CHUNKS_BUCKET + "/" + s3Key);

        // ── SQS send with FIFO-required parameters ─────────────────
        JSONObject json = new JSONObject();
        json.put("bucketName", CHUNKS_BUCKET);
        json.put("s3Key", s3Key);
        json.put("chunkNumber", chunkCounter);
        json.put("sourceFile", s3FileName);
        json.put("timestamp", System.currentTimeMillis());

        AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
        try {
            String queueUrl = sqs.getQueueUrl(SQS_QUEUE_NAME).getQueueUrl();

            // Generate unique MessageDeduplicationId (REQUIRED for FIFO)
            String deduplicationId = generateDeduplicationId(s3Key, chunkCounter);

            // MessageGroupId for FIFO ordering
            String messageGroupId = extractBaseFileName(s3FileName);

            System.out.println("[Split] Sending SQS message:");
            System.out.println("  Queue: " + queueUrl);
            System.out.println("  MessageGroupId: " + messageGroupId);
            System.out.println("  MessageDeduplicationId: " + deduplicationId);

            SendMessageRequest req = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(json.toString())
                    .withMessageGroupId(messageGroupId)           // FIFO: ordering per source file
                    .withMessageDeduplicationId(deduplicationId); // FIFO: prevent duplicates

            SendMessageResult res = sqs.sendMessage(req);

            System.out.println("[Split] ✓ SQS message sent successfully");
            System.out.println("  MessageId: " + res.getMessageId());
            System.out.println("  SequenceNumber: " + res.getSequenceNumber());

        } catch (Exception e) {
            System.err.println("[Split] ✗ ERROR sending SQS message: " + e.getMessage());
            e.printStackTrace();
            throw e;  // Re-throw to ensure visibility

        } finally {
            sqs.shutdown();
        }

        chunkFile.delete();
    }

    /** Pushes Failurefile.txt to the error-log bucket (date-partitioned). */
    private void uploadFailureFile() {
        File errorFile = new File("/tmp/Failurefile.txt");
        if (!errorFile.exists() || errorFile.length() == 0) return;

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String errorKey = String.format("Failurefile/%d/%d/%d/%s",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), s3FileName);

        s3Client.putObject(new PutObjectRequest(errologBucket, errorKey, errorFile));
        System.out.println("[Split] uploaded failure file to " + errologBucket + "/" + errorKey);
        errorFile.delete();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PROCESS-TIME LOG  (start CSV)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a tiny CSV with the process start-time and uploads it to the
     * report bucket.  Then invokes geojit-MapModelLambda (async) so that the
     * end-time row can be appended later by InvokeEquityFunction.
     */
    private void writeProcessStartLog() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("IST"));
            String activityDate = sdf.format(new Date());

            // ── build the S3 key (date-partitioned) ────────────────
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            String csvKey = String.format("Equity-EmailReport/Process-TimeLog/%d/%d/%d/equity_process_%s.csv",
                    today.getYear(), today.getMonthValue(), today.getDayOfMonth(), activityDate);

            // ── invoke MapModel lambda with the key (async) ────────
            JSONObject json = new JSONObject();
            json.put("bucketName", REPORT_BUCKET);
            json.put("s3Key", csvKey);

            AWSLambda lambda = AWSLambdaAsyncClient.builder().build();
            lambda.invoke(new InvokeRequest()
                    .withFunctionName("invoke-lambda-geojit")
                    .withInvocationType("Event")
                    .withPayload(json.toString()));
            lambda.shutdown();

            // ── write the CSV locally and upload ────────────────────
            File csvFile = new File("/tmp/time_report.csv");
            try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
                writer.writeNext(new String[]{"Segment", "Activity Date-Time"});
                writer.writeNext(new String[]{"equity-startTime", activityDate});
            }
            s3Client.putObject(new PutObjectRequest(REPORT_BUCKET, csvKey, csvFile));
            csvFile.delete();

        } catch (Exception e) {
            e.printStackTrace();   // non-fatal – do not block the main flow
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ═══════════════════════════════════════════════════════════════════

    /** Generates a unique suffix  →  e.g. "437_20260203143025123.txt" */
    private static String generateFileSuffix(String ext) {
        int rand = new Random().nextInt(1000);
        return new SimpleDateFormat(rand + "_yyyyMMddHHmmssSSS'" + ext + "'").format(new Date());
    }

    /**
     * Generate a unique MessageDeduplicationId for FIFO queue.
     * Uses SHA-256 hash of s3Key + chunkNumber + timestamp.
     * Returns a string of max 128 characters (FIFO requirement).
     */
    private String generateDeduplicationId(String s3Key, int chunkNumber) {
        try {
            // Create unique input string
            String input = s3Key + "_chunk_" + chunkNumber + "_" + System.currentTimeMillis();

            // Generate SHA-256 hash
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            // Return first 128 characters (FIFO limit)
            String deduplicationId = hexString.substring(0, Math.min(128, hexString.length()));

            return deduplicationId;

        } catch (Exception e) {
            // Fallback to UUID if hashing fails
            System.err.println("[Split] Warning: Hash generation failed, using UUID fallback");
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Extract base filename without path and extension
     */
    private String extractBaseFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "default-group";
        }

        // Remove path
        int lastSlash = filePath.lastIndexOf('/');
        String fileName = (lastSlash >= 0) ? filePath.substring(lastSlash + 1) : filePath;

        // Remove extension
        int lastDot = fileName.lastIndexOf('.');
        String baseName = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;

        // Replace any non-alphanumeric characters with underscore (FIFO requirement)
        baseName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");

        return baseName;
    }

    private static void closeQuietly(Closeable c) {
        try { if (c != null) c.close(); } catch (IOException ignored) {}
    }
}