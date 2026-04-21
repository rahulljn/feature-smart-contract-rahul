package com.geojit.contractnote.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_customers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "party_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "party_code", nullable = false, length = 100)
    private String partyCode;

    @Column(name = "contract_note_no", length = 200)
    private String contractNoteNo;

    @Column(name = "trade_date", length = 50)
    private String tradeDate;

    @Column(name = "email", length = 500)
    private String email;

    @Column(name = "segment", length = 200)
    private String segment;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "pdf_status", nullable = false, length = 50)
    private PdfStatus pdfStatus = PdfStatus.PENDING;

    @Column(name = "pdf_s3_key", length = 1000)
    private String pdfS3Key;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 50)
    private EmailStatus emailStatus = EmailStatus.PENDING;

    @Column(name = "ses_message_id", length = 500)
    private String sesMessageId;

    @Column(name = "bounce_type", length = 100)
    private String bounceType;

    @Column(name = "bounce_reason", columnDefinition = "TEXT")
    private String bounceReason;

    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "bounced_at")
    private LocalDateTime bouncedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PdfStatus   { PENDING, GENERATED, FAILED }
    public enum EmailStatus { PENDING, SENT, BOUNCED, DELIVERED, FAILED, SKIPPED }
}
