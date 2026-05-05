package com.geojit.contractnote.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "rule_name", length = 200)
    private String ruleName;

    @Column(name = "triggered_by", length = 500)
    private String triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertChannel channel;

    @Column(name = "recipient", length = 500)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
    }

    public enum AlertChannel {
        EMAIL,
        SMS,
        WHATSAPP
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
