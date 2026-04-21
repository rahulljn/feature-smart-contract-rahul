package com.geojit.contractnotes.geojit_ContractNote;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * StatusConsumerLambda — bridges the Lambda pipeline to the Spring Boot API.
 *
 * Triggered by SQS Event Source Mapping on geojit-pipeline-status-queue.fifo.
 * Reads status events from the queue and POSTs them to the API's
 * POST /api/v1/pipeline/status-event endpoint.
 *
 * Event types handled:
 *   JOB_REGISTERED, CUSTOMER_REGISTERED, PDF_GENERATED, PDF_FAILED,
 *   EMAIL_SENT, EMAIL_FAILED, DELIVERY, BOUNCE, COMPLAINT, SPLIT_COMPLETE
 *
 * Auth: Uses X-Internal-Key header with a shared secret from environment variable.
 */
public class StatusConsumerLambda implements RequestHandler<Object, String> {

    private static final Logger logger = LoggerFactory.getLogger(StatusConsumerLambda.class);
    private static final String STATUS_QUEUE_NAME = "geojit-pipeline-status-queue.fifo";
    private static final Regions AWS_REGION = Regions.AP_SOUTH_1;
    private static final int MAX_MESSAGES_PER_BATCH = 10;
    private static final int API_TIMEOUT_MS = 15_000;

    // API connection config — set via Lambda environment variables
    private static final String API_BASE_URL = System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080/api/v1");
    private static final String INTERNAL_API_KEY = System.getenv().getOrDefault("INTERNAL_API_KEY", "dev-internal-key");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String handleRequest(Object input, Context context) {
        SimpleDateFormat istFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        istFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        int processedCount = 0;
        int failedCount = 0;

        logger.info("StatusConsumerLambda Started | startTime={}", istFormat.format(new Date()));

        AmazonSQS sqsClient = null;
        try {
            sqsClient = AmazonSQSClientBuilder.standard()
                    .withRegion(AWS_REGION)
                    .build();

            String queueUrl = sqsClient.getQueueUrl(STATUS_QUEUE_NAME).getQueueUrl();

            ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest(queueUrl)
                    .withMaxNumberOfMessages(MAX_MESSAGES_PER_BATCH)
                    .withVisibilityTimeout(30)
                    .withWaitTimeSeconds(0);

            // Loop until queue is empty or Lambda is about to timeout
            List<Message> messages;
            do {
                messages = sqsClient.receiveMessage(receiveRequest).getMessages();
                if (messages.isEmpty()) break;
                logger.info("Received {} messages from status queue", messages.size());

                for (Message message : messages) {
                    try {
                        String body = message.getBody();

                        // Validate it's proper JSON
                        JSONObject eventJson = new JSONObject(body);
                        String eventType = eventJson.optString("eventType", "UNKNOWN");
                        String jobId = eventJson.optString("jobId", "UNKNOWN");

                        // POST to API
                        boolean success = postStatusEvent(body);

                        if (success) {
                            sqsClient.deleteMessage(queueUrl, message.getReceiptHandle());
                            processedCount++;
                            logger.info("Status event forwarded | jobId={} | eventType={}", jobId, eventType);
                        } else {
                            failedCount++;
                            logger.warn("Status event forwarding FAILED | jobId={} | eventType={} | will retry", jobId, eventType);
                        }

                    } catch (Exception e) {
                        failedCount++;
                        logger.error("Error processing status message: {}", e.getMessage(), e);
                    }
                }
            } while (!messages.isEmpty());

        } catch (Exception e) {
            logger.error("StatusConsumerLambda fatal error: {}", e.getMessage(), e);
        } finally {
            if (sqsClient != null) {
                try { sqsClient.shutdown(); } catch (Exception ignored) {}
            }
        }

        logger.info("StatusConsumerLambda Completed | processed={} | failed={} | endTime={}",
                processedCount, failedCount, istFormat.format(new Date()));

        return String.format("processed=%d,failed=%d", processedCount, failedCount);
    }

    /**
     * POST the status event JSON to the Spring Boot API.
     * Uses HttpURLConnection to avoid extra dependencies.
     */
    private boolean postStatusEvent(String jsonBody) {
        HttpURLConnection connection = null;
        try {
            String endpoint = API_BASE_URL + "/pipeline/status-event";
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Internal-Key", INTERNAL_API_KEY);
            connection.setConnectTimeout(API_TIMEOUT_MS);
            connection.setReadTimeout(API_TIMEOUT_MS);
            connection.setDoOutput(true);

            // Write request body
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return true;
            } else {
                logger.error("API returned non-2xx status: {} for event", responseCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("Failed to POST status event to API: {}", e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try { connection.disconnect(); } catch (Exception ignored) {}
            }
        }
    }
}
