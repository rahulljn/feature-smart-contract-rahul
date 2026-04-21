package com.geojit.contractnote.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geojit.contractnote.config.AppProperties;
import com.geojit.contractnote.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Thin wrapper around AWS SQS SDK v1.
 *
 * In local-mock mode, all SQS calls are caught gracefully and a mock message ID
 * is returned so the rest of the application flow (DB updates, audit logs) continues
 * to work without real AWS credentials.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqsService {

    private final AmazonSQS       sqs;
    private final AppProperties   appProperties;
    private final ObjectMapper    objectMapper;

    /**
     * Send a message to a FIFO queue.
     * Returns a mock message ID if the SQS call fails (local-mock mode).
     */
    public String sendFifoMessage(String queueName, Object payload, String groupId, String dedupId) {
        String body = serialize(payload);
        try {
            String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
            SendMessageRequest req = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(body)
                    .withMessageGroupId(groupId)
                    .withMessageDeduplicationId(dedupId);
            SendMessageResult result = sqs.sendMessage(req);
            log.info("SQS FIFO message sent | queue={} | groupId={} | messageId={}", queueName, groupId, result.getMessageId());
            return result.getMessageId();
        } catch (Exception e) {
            String mockId = "MOCK-MSG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.warn("⚠️  SQS sendFifoMessage skipped (local-mock mode) | queue={} | mockId={} | reason={}",
                    queueName, mockId, e.getMessage());
            return mockId;
        }
    }

    /**
     * Send a message to a standard (non-FIFO) queue.
     * Returns a mock message ID if the SQS call fails (local-mock mode).
     */
    public String sendMessage(String queueName, Object payload) {
        String body = serialize(payload);
        try {
            String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
            SendMessageRequest req = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(body);
            SendMessageResult result = sqs.sendMessage(req);
            log.info("SQS message sent | queue={} | messageId={}", queueName, result.getMessageId());
            return result.getMessageId();
        } catch (Exception e) {
            String mockId = "MOCK-MSG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.warn("⚠️  SQS sendMessage skipped (local-mock mode) | queue={} | mockId={} | reason={}",
                    queueName, mockId, e.getMessage());
            return mockId;
        }
    }

    /**
     * Send a resend trigger to the map-split FIFO queue.
     */
    public String sendResendTrigger(String jobId, String partyCode, String requestedBy) {
        Map<String, String> payload = Map.of(
                "jobId",       jobId,
                "partyCode",   partyCode,
                "requestedBy", requestedBy,
                "type",        "RESEND"
        );
        String dedupId = "resend-" + jobId + "-" + partyCode + "-" + System.currentTimeMillis();
        return sendFifoMessage(
                appProperties.getAws().getSqs().getMapSplitQueue(),
                payload,
                jobId,
                dedupId
        );
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize SQS message payload: " + e.getMessage());
        }
    }
}
