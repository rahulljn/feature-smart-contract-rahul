package com.geojit.contractnote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email", length = 500)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction action;

    @Column(name = "target_entity", length = 500)
    private String targetEntity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @PrePersist
    public void prePersist() {
        if (this.eventTimestamp == null) this.eventTimestamp = LocalDateTime.now();
    }

    public enum AuditAction {
        LOGIN, LOGOUT, UPLOAD, RESEND, BULK_RESEND,
        SUPPRESS, UNSUPPRESS, TEMPLATE_EDIT, CERT_UPLOAD,
        CONFIG_CHANGE, USER_CREATE, USER_UPDATE,
        USER_DEACTIVATE, VIEW_PDF, DOWNLOAD_REPORT
    }
}
