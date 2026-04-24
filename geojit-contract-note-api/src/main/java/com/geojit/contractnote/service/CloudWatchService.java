package com.geojit.contractnote.service;

import com.geojit.contractnote.config.AppProperties;
import com.geojit.contractnote.dto.response.CloudWatchExceptionResponse;
import com.geojit.contractnote.dto.response.CloudWatchLambdaResponse;
import com.geojit.contractnote.entity.Job;
import com.geojit.contractnote.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilteredLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudWatchService {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final int MAX_PER_GROUP = 20;

    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            "split-lambda-geojit",       "Split",
            "invoke-lambda-geojit",      "Invoke",
            "create-pdf-geojit",         "PDF",
            "json-lambda-geojit",        "GetJson",
            "email-notification-geojit", "EmailNotification",
            "pull-bounce-geojit",        "PullBounce",
            "pull-delivery-geojit",      "PullDelivery",
            "status-consumer-geojit",    "StatusConsumer"
    );

    private final CloudWatchLogsClient cloudWatchLogsClient;
    private final AppProperties        appProperties;
    private final JobRepository        jobRepository;

    /**
     * Returns one {@link CloudWatchLambdaResponse} per Lambda that has errors for the given tab type.
     * Lambdas with zero errors are omitted — only exception logs are returned.
     *
     * Tab → Lambda log groups:
     *   ALL     → split, invoke, create-pdf, json, email-notification, pull-bounce, pull-delivery, status-consumer
     *   PDF     → invoke, create-pdf (DynamicValuePdf → PDF_FAILED/GENERATED), json (GetJsonLambda)
     *   EMAIL   → email-notification (EMAIL_SENT/FAILED), invoke, status-consumer
     *   BOUNCE  → pull-bounce (PullBounceSQS → BOUNCE/COMPLAINT), pull-delivery, status-consumer
     *   SKIPPED → email-notification (email-not-found path), invoke, status-consumer
     */
    public List<CloudWatchLambdaResponse> getExceptionLogs(UUID jobId, String type) {
        AppProperties.Aws.CloudWatch cw = appProperties.getAws().getCloudwatch();
        List<String> logGroups = resolveLogGroups(type.toUpperCase(), cw);

        Instant windowStart = resolveWindowStart(jobId);
        Instant windowEnd   = resolveWindowEnd(jobId);

        List<CloudWatchLambdaResponse> result = new ArrayList<>();
        for (String logGroup : logGroups) {
            List<CloudWatchExceptionResponse> events =
                    fetchFromLogGroup(logGroup, windowStart, windowEnd);
            if (!events.isEmpty()) {
                result.add(CloudWatchLambdaResponse.builder()
                        .lambdaName(lambdaName(logGroup))
                        .logGroup(logGroup)
                        .errorCount(events.size())
                        .events(events)
                        .build());
            }
        }
        return result;
    }

    // ─── Tab → log group mapping (based on actual Lambda source code) ─────────

    private List<String> resolveLogGroups(String type, AppProperties.Aws.CloudWatch cw) {
        return switch (type) {
            // Per-Lambda tabs — one log group each
            case "SPLIT"        -> List.of(cw.getSplitLambdaLogGroup());
            case "INVOKE"       -> List.of(cw.getInvokeLambdaLogGroup());
            case "GETJSON"      -> List.of(cw.getJsonLambdaLogGroup());
            case "PDF"          -> List.of(cw.getPdfLambdaLogGroup());
            case "EMAIL"        -> List.of(cw.getEmailLambdaLogGroup());
            case "PULLBOUNCE"   -> List.of(cw.getBounceLambdaLogGroup());
            case "PULLDELIVERY" -> List.of(cw.getDeliveryLambdaLogGroup());
            // ALL — full pipeline, all 8 Lambdas
            default -> List.of(
                    cw.getSplitLambdaLogGroup(),
                    cw.getInvokeLambdaLogGroup(),
                    cw.getPdfLambdaLogGroup(),
                    cw.getJsonLambdaLogGroup(),
                    cw.getEmailLambdaLogGroup(),
                    cw.getBounceLambdaLogGroup(),
                    cw.getDeliveryLambdaLogGroup(),
                    cw.getStatusConsumerLogGroup()
            );
        };
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Instant resolveWindowStart(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(Job::getUploadedAt)
                // DB stores LocalDateTime in the JVM's system timezone (IST on dev, UTC on Lambda).
                // Use systemDefault() so the Instant conversion is always correct, regardless of environment.
                .map(t -> t.atZone(ZoneId.systemDefault()).toInstant().minusSeconds(3600))
                .orElse(Instant.now().minusSeconds(3600));
    }

    private Instant resolveWindowEnd(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(Job::getUploadedAt)
                .map(t -> {
                    Instant end = t.atZone(ZoneId.systemDefault()).toInstant().plusSeconds(21600); // +6h
                    return end.isAfter(Instant.now()) ? Instant.now() : end;
                })
                .orElse(Instant.now());
    }

    private List<CloudWatchExceptionResponse> fetchFromLogGroup(
            String logGroup, Instant start, Instant end) {
        try {
            FilterLogEventsRequest request = FilterLogEventsRequest.builder()
                    .logGroupName(logGroup)
                    .startTime(start.toEpochMilli())
                    .endTime(end.toEpochMilli())
                    .filterPattern("?ERROR ?Exception ?error")
                    .limit(MAX_PER_GROUP)
                    .build();

            return cloudWatchLogsClient.filterLogEvents(request)
                    .events()
                    .stream()
                    .filter(e -> e.message() != null)
                    .map(e -> toResponse(e, logGroup))
                    .collect(Collectors.toList());

        } catch (ResourceNotFoundException ex) {
            log.warn("CloudWatch log group not found — verify Lambda function name config: {}", logGroup);
            return Collections.emptyList();
        } catch (Exception ex) {
            log.warn("CloudWatch fetch failed for {}: {}", logGroup, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private CloudWatchExceptionResponse toResponse(FilteredLogEvent event, String logGroup) {
        String ts = event.timestamp() != null
                ? TS_FMT.format(Instant.ofEpochMilli(event.timestamp()))
                : "";
        String msg = event.message() != null ? event.message().trim() : "";
        if (msg.length() > 500) msg = msg.substring(0, 500) + "…";
        return CloudWatchExceptionResponse.builder()
                .timestamp(ts)
                .logGroup(logGroup)
                .logStream(event.logStreamName() != null ? event.logStreamName() : "")
                .message(msg)
                .build();
    }

    private String lambdaName(String logGroup) {
        int idx = logGroup.lastIndexOf('/');
        String raw = idx >= 0 ? logGroup.substring(idx + 1) : logGroup;
        return DISPLAY_NAMES.getOrDefault(raw, raw);
    }
}
