package com.geojit.contractnote.dto.response;

import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PipelineStatsResponse {
    private UUID   jobId;
    private String fileName;
    private String status;

    // Upload & split stage
    private long uploadCount;
    private long splitCount;

    // PDF stage
    private long pdfCount;
    private long pdfFailed;
    private long pdfPending;
    private long pdfRate;       // PDF_GENERATED events in last 60s

    // Email stage
    private long emailCount;
    private long emailBounced;
    private long emailFailed;
    private long emailPending;
    private long emailRate;     // EMAIL_SENT events in last 60s

    // Delivered stage
    private long deliveredCount;
    private long deliveredRate; // DELIVERY events in last 60s

    // Aggregate metrics
    private double medianSeconds;
    private double p95Seconds;
    private double errorRate;   // (pdfFailed + emailFailed) / uploadCount * 100
}
