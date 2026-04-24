package com.geojit.contractnote.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geojit.contractnote.config.AppProperties;
import com.geojit.contractnote.dto.response.LambdaExceptionResponse;
import com.geojit.contractnote.entity.LambdaException;
import com.geojit.contractnote.repository.LambdaExceptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LambdaExceptionService {

    private static final int MAX_BATCHES          = 5;
    private static final int MESSAGES_PER_RECEIVE = 10;

    private final AmazonSQS                  sqs;
    private final AppProperties              appProperties;
    private final LambdaExceptionRepository  repository;
    private final ObjectMapper               objectMapper;

    /** Drain the exceptions SQS queue and persist new messages, then return all for the job. */
    @Transactional
    public List<LambdaExceptionResponse> fetchAndPersist(String jobId, String lambdaName) {
        drainQueue();
        return query(jobId, lambdaName);
    }

    private void drainQueue() {
        String queueUrl = appProperties.getAws().getSqs().getExceptionsQueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            log.debug("Exceptions queue URL not configured — skipping drain");
            return;
        }

        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            ReceiveMessageResult result;
            try {
                result = sqs.receiveMessage(new ReceiveMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMaxNumberOfMessages(MESSAGES_PER_RECEIVE)
                        .withWaitTimeSeconds(1));
            } catch (Exception e) {
                log.warn("Failed to receive from exceptions queue: {}", e.getMessage());
                break;
            }

            List<Message> messages = result.getMessages();
            if (messages == null || messages.isEmpty()) break;

            for (Message msg : messages) {
                try {
                    LambdaException entity = parseMessage(msg.getBody());
                    repository.save(entity);
                    sqs.deleteMessage(new DeleteMessageRequest(queueUrl, msg.getReceiptHandle()));
                } catch (Exception e) {
                    log.warn("Skipping unparseable exception message: {}", e.getMessage());
                }
            }
        }
    }

    private LambdaException parseMessage(String body) throws Exception {
        JsonNode node = objectMapper.readTree(body);
        Instant occurredAt;
        try {
            occurredAt = Instant.parse(node.path("timestamp").asText());
        } catch (Exception e) {
            occurredAt = Instant.now();
        }
        return LambdaException.builder()
                .jobId(node.path("jobId").asText(""))
                .lambdaName(node.path("lambdaName").asText("Unknown"))
                .recordId(node.has("recordId") && !node.get("recordId").isNull()
                        ? node.get("recordId").asText() : null)
                .errorType(node.path("errorType").asText(null))
                .errorMessage(node.path("errorMessage").asText(null))
                .stackTrace(node.path("stackTrace").asText(null))
                .environment(node.path("environment").asText(null))
                .occurredAt(occurredAt)
                .build();
    }

    private List<LambdaExceptionResponse> query(String jobId, String lambdaName) {
        List<LambdaException> entities = (lambdaName != null && !lambdaName.isBlank())
                ? repository.findByJobIdAndLambdaNameOrderByOccurredAtDesc(jobId, lambdaName)
                : repository.findByJobIdOrderByOccurredAtDesc(jobId);
        return entities.stream().map(LambdaExceptionResponse::from).toList();
    }
}
