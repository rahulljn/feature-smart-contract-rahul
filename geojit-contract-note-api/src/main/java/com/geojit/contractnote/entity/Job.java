package com.geojit.contractnote.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Job {

    @Id
    @Column(name = "job_id", updatable = false, nullable = false)
    private UUID jobId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "raw_s3_key", length = 1000)
    private String rawS3Key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private JobStatus status = JobStatus.VALIDATING;

    @Column(name = "segment_type", length = 100)
    private String segmentType;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Column(name = "total_customers")
    private int totalCustomers = 0;

    @Column(name = "processed_count")
    private int processedCount = 0;

    @Column(name = "pdf_generated_count")
    private int pdfGeneratedCount = 0;

    @Column(name = "email_sent_count")
    private int emailSentCount = 0;

    @Column(name = "email_delivered_count")
    private int emailDeliveredCount = 0;

    @Column(name = "email_bounced_count")
    private int emailBouncedCount = 0;

    @Column(name = "email_failed_count")
    private int emailFailedCount = 0;

    @Column(name = "email_skipped_count")
    private int emailSkippedCount = 0;

    @Column(name = "failed_count")
    private int failedCount = 0;

    @Column(name = "invalid_record_count")
    private int invalidRecordCount = 0;

    @Column(name = "pdf_failed_count")
    private int pdfFailedCount = 0;

    @Column(name = "hard_bounce_count")
    private int hardBounceCount = 0;

    @Column(name = "soft_bounce_count")
    private int softBounceCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum JobStatus {
        VALIDATING, SPLITTING, PROCESSING, EMAILING, COMPLETED, FAILED, PARTIAL
    }
}
