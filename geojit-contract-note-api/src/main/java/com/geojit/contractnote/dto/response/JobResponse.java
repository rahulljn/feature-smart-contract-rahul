package com.geojit.contractnote.dto.response;

import com.geojit.contractnote.entity.Job;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobResponse {
    private UUID         jobId;
    private String       fileName;
    private String       rawS3Key;
    private UploadedByResponse uploadedBy;
    private LocalDateTime uploadedAt;
    private Job.JobStatus status;
    private String       segmentType;
    private LocalDate    tradeDate;
    private int          totalRecords;
    private int          processedCount;
    private int          pdfGeneratedCount;
    private int          emailSentCount;
    private int          emailDeliveredCount;
    private int          emailFailedCount;
    private int          emailSkippedCount;
    private int          successCount;
    private int          bounceCount;
    private int          failureCount;
    private int          progressPercent;
    private LocalDateTime createdAt;

    public static JobResponse from(Job job) {
        int total = job.getTotalCustomers();
        // Progress: use emailSent+emailBounced+failed as "done" (delivery events may not arrive in sandbox)
        int done = job.getEmailSentCount() + job.getEmailBouncedCount() + job.getFailedCount();
        int progress = total > 0 ? Math.min(100, (int) Math.round((double) done / total * 100)) : 0;

        return JobResponse.builder()
                .jobId(job.getJobId())
                .fileName(job.getFileName())
                .rawS3Key(job.getRawS3Key())
                .uploadedBy(job.getUploadedBy() != null
                        ? UploadedByResponse.builder()
                            .userId(job.getUploadedBy().getUserId())
                            .name(job.getUploadedBy().getName())
                            .email(job.getUploadedBy().getEmail())
                            .build()
                        : null)
                .uploadedAt(job.getUploadedAt())
                .status(job.getStatus())
                .segmentType(job.getSegmentType())
                .tradeDate(job.getTradeDate())
                .totalRecords(total)
                .processedCount(job.getProcessedCount())
                .pdfGeneratedCount(job.getPdfGeneratedCount())
                .emailSentCount(job.getEmailSentCount())
                .emailDeliveredCount(job.getEmailDeliveredCount())
                .emailFailedCount(job.getEmailFailedCount())
                .emailSkippedCount(job.getEmailSkippedCount())
                .successCount(job.getEmailSentCount())
                .bounceCount(job.getEmailBouncedCount())
                .failureCount(job.getFailedCount())
                .progressPercent(progress)
                .createdAt(job.getCreatedAt())
                .build();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UploadedByResponse {
        private UUID userId;
        private String name;
        private String email;
    }
}
