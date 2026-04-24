package com.geojit.contractnote.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "lambda_exceptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LambdaException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "lambda_name", nullable = false)
    private String lambdaName;

    @Column(name = "record_id")
    private String recordId;

    @Column(name = "error_type")
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "environment")
    private String environment;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
