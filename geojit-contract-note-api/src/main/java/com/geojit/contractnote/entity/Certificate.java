package com.geojit.contractnote.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cert_id", updatable = false)
    private UUID certId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String issuer;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(length = 200)
    private String thumbprint;

    @Column(name = "secret_name", nullable = false, length = 500)
    private String secretName;

    @Column(name = "s3_key", length = 1000)
    private String s3Key;

    @Column(name = "is_active", nullable = false)
    @Getter(onMethod_ = {@JsonProperty("isActive")})
    private boolean isActive = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
