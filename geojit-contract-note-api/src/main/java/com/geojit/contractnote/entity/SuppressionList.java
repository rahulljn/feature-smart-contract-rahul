package com.geojit.contractnote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "suppression_list")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuppressionList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "added_by")
    private UUID addedBy;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @PrePersist
    public void prePersist() {
        if (this.addedAt == null) this.addedAt = LocalDateTime.now();
    }
}
