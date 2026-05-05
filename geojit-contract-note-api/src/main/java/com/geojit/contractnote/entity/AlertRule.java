package com.geojit.contractnote.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "channels", columnDefinition = "jsonb")
    private String channels;

    @Column(name = "recipients", columnDefinition = "jsonb")
    private String recipients;

    @Column(name = "threshold_value")
    private Integer thresholdValue;

    @Column(name = "include_deep_link")
    private Boolean includeDeepLink = false;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AlertTriggerType {
        JOB_STATUS,
        BOUNCE_RATE,
        QUOTA,
        EXPIRY,
        ERROR_RATE,
        DELIVERY_RATE,
        LAMBDA_TIMEOUT
    }

    public enum AlertSeverity {
        CRITICAL,
        WARNING,
        INFO
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
