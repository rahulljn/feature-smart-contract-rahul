package com.geojit.contractnote.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geojit.contractnote.entity.JobCustomer;
import com.geojit.contractnote.entity.PipelineEvent;
import com.geojit.contractnote.repository.JobCustomerRepository;
import com.geojit.contractnote.service.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class SesWebhookController {

    private final PipelineService        pipelineService;
    private final JobCustomerRepository  jobCustomerRepository;
    private final ObjectMapper           objectMapper;

    @Value("${app.aws.sns.ses-topic-arn:}")
    private String expectedTopicArn;

    @PostMapping("/ses")
    public ResponseEntity<Void> handleSns(
            @RequestHeader(value = "X-Amz-Sns-Message-Type", required = false) String messageType,
            @RequestBody String rawBody) {

        try {
            JsonNode sns = objectMapper.readTree(rawBody);
            String type = messageType != null ? messageType : sns.path("Type").asText();

            // Reject requests from unexpected SNS topics
            if (!expectedTopicArn.isBlank()) {
                String topicArn = sns.path("TopicArn").asText("");
                if (!expectedTopicArn.equals(topicArn)) {
                    log.warn("SNS message rejected — unexpected TopicArn={}", topicArn);
                    return ResponseEntity.ok().build();
                }
            }

            if ("SubscriptionConfirmation".equals(type)) {
                String subscribeUrl = sns.path("SubscribeURL").asText();
                confirmSubscription(subscribeUrl);
                return ResponseEntity.ok().build();
            }

            if ("Notification".equals(type)) {
                String message = sns.path("Message").asText();
                JsonNode sesEvent = objectMapper.readTree(message);
                boolean processed = processNotification(sesEvent);
                // Customer not yet registered (SQS pipeline events still in-flight) — ask SNS to retry
                if (!processed) {
                    return ResponseEntity.status(503).build();
                }
            }

        } catch (Exception e) {
            log.error("SNS/SES webhook error: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    private void confirmSubscription(String subscribeUrl) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(subscribeUrl)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("SNS subscription confirmed | status={}", response.statusCode());
        } catch (Exception e) {
            log.error("Failed to confirm SNS subscription: {}", e.getMessage(), e);
        }
    }

    /** Returns true if the event was processed, false if the customer isn't registered yet (trigger SNS retry). */
    private boolean processNotification(JsonNode sesEvent) {
        // SES Configuration Set events use "eventType"; legacy SNS receipt rules use "notificationType"
        String notificationType = sesEvent.has("eventType")
                ? sesEvent.path("eventType").asText()
                : sesEvent.path("notificationType").asText();
        String sesMessageId = sesEvent.path("mail").path("messageId").asText();

        if (sesMessageId.isBlank()) {
            log.warn("SNS notification missing mail.messageId — skipping");
            return true;
        }

        // Primary lookup by sesMessageId
        Optional<JobCustomer> customer = jobCustomerRepository.findBySesMessageId(sesMessageId);

        // Fallback: SNS fires before EMAIL_SENT pipeline event arrives — resolve via metajson email header
        if (customer.isEmpty()) {
            customer = resolveCustomerViaMetaJson(sesEvent, sesMessageId);
        }

        if (customer.isEmpty()) {
            log.warn("Customer not yet registered for sesMessageId={} — requesting SNS retry", sesMessageId);
            return false;
        }

        JobCustomer jc = customer.get();
        PipelineEvent event = PipelineEvent.builder()
                .jobId(jc.getJob().getJobId())
                .partyCode(jc.getPartyCode())
                .lambdaName("ses-webhook")
                .eventTimestamp(LocalDateTime.now())
                .build();

        switch (notificationType) {
            case "Delivery" -> {
                event.setEventType(PipelineEvent.EventType.DELIVERY);
                String ts = sesEvent.path("delivery").path("timestamp").asText(null);
                if (ts != null) {
                    try { event.setEventTimestamp(LocalDateTime.parse(ts, DateTimeFormatter.ISO_DATE_TIME)); }
                    catch (Exception ignored) {}
                }
                log.info("DELIVERY event | sesMessageId={} | partyCode={}", sesMessageId, jc.getPartyCode());
            }
            case "Bounce" -> {
                JsonNode bounce = sesEvent.path("bounce");
                String bounceType    = bounce.path("bounceType").asText(null);
                String bounceSubType = bounce.path("bounceSubType").asText(null);
                event.setEventType(PipelineEvent.EventType.BOUNCE);
                event.setPayload(Map.of(
                        "bounceType",    bounceType    != null ? bounceType    : "",
                        "bounceSubType", bounceSubType != null ? bounceSubType : ""
                ));
                log.info("BOUNCE event | sesMessageId={} | type={} | partyCode={}", sesMessageId, bounceType, jc.getPartyCode());
            }
            case "Complaint" -> {
                event.setEventType(PipelineEvent.EventType.COMPLAINT);
                log.info("COMPLAINT event | sesMessageId={} | partyCode={}", sesMessageId, jc.getPartyCode());
            }
            default -> {
                log.debug("Unhandled SES notification type: {}", notificationType);
                return true;
            }
        }

        pipelineService.processStatusEvent(event);
        return true;
    }

    /**
     * SES embeds a "metajson" custom header in every outbound email containing jobid + partycode.
     * Used when the SNS delivery event arrives before EMAIL_SENT pipeline event stores sesMessageId.
     */
    private Optional<JobCustomer> resolveCustomerViaMetaJson(JsonNode sesEvent, String sesMessageId) {
        try {
            for (JsonNode header : sesEvent.path("mail").path("headers")) {
                if (!"metajson".equalsIgnoreCase(header.path("name").asText())) continue;
                JsonNode meta = objectMapper.readTree(header.path("value").asText());
                String jobIdStr  = meta.path("jobid").asText(null);
                String partyCode = meta.path("partycode").asText(null);
                if (jobIdStr == null || partyCode == null) break;

                Optional<JobCustomer> jc = jobCustomerRepository
                        .findByJob_JobIdAndPartyCode(UUID.fromString(jobIdStr), normaliseCode(partyCode));

                jc.ifPresent(c -> {
                    // Store sesMessageId so the primary lookup works on any future events
                    if (c.getSesMessageId() == null || c.getSesMessageId().isBlank()) {
                        c.setSesMessageId(sesMessageId);
                        jobCustomerRepository.save(c);
                    }
                    log.info("Resolved customer via metajson | sesMessageId={} | partyCode={}", sesMessageId, c.getPartyCode());
                });
                return jc;
            }
        } catch (Exception e) {
            log.warn("metajson fallback failed | sesMessageId={} | error={}", sesMessageId, e.getMessage());
        }
        return Optional.empty();
    }

    private String normaliseCode(String code) {
        if (code == null) return null;
        int slash = code.indexOf('/');
        return slash > 0 ? code.substring(0, slash) : code;
    }
}
