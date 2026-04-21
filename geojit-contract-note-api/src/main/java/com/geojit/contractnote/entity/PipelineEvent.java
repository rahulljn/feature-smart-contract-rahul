package com.geojit.contractnote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pipeline_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PipelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "party_code", length = 100)
    private String partyCode;

    @Column(name = "lambda_name", length = 200)
    private String lambdaName = "unknown";

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private EventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @PrePersist
    public void prePersist() {
        if (this.eventTimestamp == null) this.eventTimestamp = LocalDateTime.now();
        if (this.lambdaName == null) this.lambdaName = "unknown";
    }

    public enum EventType {
        JOB_REGISTERED, CUSTOMER_REGISTERED,
        SPLIT_PROGRESS, SPLIT_COMPLETE, PDF_TRIGGERED, PDF_GENERATED, PDF_FAILED,
        EMAIL_SENT, EMAIL_FAILED, EMAIL_SKIPPED,
        DELIVERY, BOUNCE, COMPLAINT,
        RESEND_TRIGGERED
    }
}
