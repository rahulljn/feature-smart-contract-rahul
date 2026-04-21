package com.geojit.contractnote.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id", updatable = false)
    private UUID templateId;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "TEXT")
    private String htmlBody;

    @Column(length = 100)
    private String segment;

    @Column(name = "is_active", nullable = false)
    @Getter(onMethod_ = {@JsonProperty("isActive")})
    private boolean isActive = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_edited_by")
    private User lastEditedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_edited_at")
    private LocalDateTime lastEditedAt;

    // ── Editable field columns ───────────────────────────────────────────
    /** Short greeting line, e.g. "Warm Greetings from Geojit Investments Ltd !" */
    @Column(name = "greeting_text", columnDefinition = "TEXT")
    private String greetingText;

    /** Opening body paragraph explaining the email purpose. */
    @Column(name = "body_intro", columnDefinition = "TEXT")
    private String bodyIntro;

    /** Optional logo image URL inserted at top of email body. */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** CSS color for the main body text, e.g. "#333333". */
    @Column(name = "body_color", length = 20)
    private String bodyColor;

    /** CSS color for the legal disclaimer footer text, e.g. "#666666". */
    @Column(name = "footer_color", length = 20)
    private String footerColor;
}
