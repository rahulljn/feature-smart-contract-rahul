package com.geojit.contractnote.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geojit.contractnote.config.AppProperties;
import com.geojit.contractnote.entity.PipelineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("local")
@Slf4j
@RequiredArgsConstructor
public class LocalSqsPipelinePoller {

    private final AmazonSQS sqs;
    private final PipelineService pipelineService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        try {
            String queueName = appProperties.getAws().getSqs().getPipelineStatusQueue();
            String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();

            ReceiveMessageRequest req = new ReceiveMessageRequest(queueUrl)
                    .withMaxNumberOfMessages(10)
                    .withWaitTimeSeconds(0);

            List<Message> messages = sqs.receiveMessage(req).getMessages();
            for (Message msg : messages) {
                try {
                    PipelineEvent event = objectMapper.readValue(msg.getBody(), PipelineEvent.class);
                    pipelineService.processStatusEvent(event);
                    sqs.deleteMessage(queueUrl, msg.getReceiptHandle());
                    log.info("LocalSqsPoller processed | type={} | jobId={}", event.getEventType(), event.getJobId());
                } catch (Exception e) {
                    log.error("LocalSqsPoller failed to process message | body={} | error={}", msg.getBody(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("LocalSqsPoller poll error | {}", e.getMessage());
        }
    }
}
