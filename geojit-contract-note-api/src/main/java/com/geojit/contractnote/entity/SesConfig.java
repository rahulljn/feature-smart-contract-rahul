package com.geojit.contractnote.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ses_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SesConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "config_set_name", nullable = false, unique = true, length = 200)
    private String configSetName;

    @Column(name = "from_email", nullable = false, length = 500)
    private String fromEmail;

    @Column(name = "from_name", length = 200)
    private String fromName;

    @Column(length = 200)
    private String domain;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(name = "is_active", nullable = false)
    @Getter(onMethod_ = {@JsonProperty("isActive")})
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
