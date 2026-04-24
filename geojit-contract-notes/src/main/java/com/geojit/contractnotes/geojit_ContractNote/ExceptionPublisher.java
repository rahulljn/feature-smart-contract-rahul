package com.geojit.contractnotes.geojit_ContractNote;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.json.JSONObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

public final class ExceptionPublisher {

    private static final String QUEUE_URL_ENV = "EXCEPTIONS_QUEUE_URL";

    private ExceptionPublisher() {}

    public static void publish(String jobId, String lambdaName, String recordId, Throwable ex) {
        try {
            String queueUrl = System.getenv(QUEUE_URL_ENV);
            if (queueUrl == null || queueUrl.isBlank()) return;

            StringWriter sw = new StringWriter();
            if (ex != null) ex.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            if (trace.length() > 2000) trace = trace.substring(0, 2000);

            JSONObject msg = new JSONObject();
            msg.put("jobId",        jobId      != null ? jobId      : "");
            msg.put("lambdaName",   lambdaName != null ? lambdaName : "");
            msg.put("recordId",     recordId   != null ? recordId   : JSONObject.NULL);
            msg.put("errorType",    ex         != null ? ex.getClass().getSimpleName() : "Unknown");
            msg.put("errorMessage", ex         != null && ex.getMessage() != null ? ex.getMessage() : "");
            msg.put("stackTrace",   trace);
            msg.put("timestamp",    Instant.now().toString());
            msg.put("environment",  System.getenv().getOrDefault("ENVIRONMENT", "unknown"));

            AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                    .withRegion(Regions.AP_SOUTH_1).build();
            sqs.sendMessage(new SendMessageRequest(queueUrl, msg.toString()));
            sqs.shutdown();
        } catch (Exception ignored) {
            System.err.println("[ExceptionPublisher] publish failed: " + ignored.getMessage());
        }
    }
}
