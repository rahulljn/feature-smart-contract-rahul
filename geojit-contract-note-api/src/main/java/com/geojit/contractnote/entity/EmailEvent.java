package com.geojit.contractnote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ses_message_id", length = 500)
    private String sesMessageId;

    @Column(name = "party_code", length = 100)
    private String partyCode;

    @Column(name = "job_id")
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private EventType eventType;

    @Column(name = "bounce_type", length = 100)
    private String bounceType;

    @Column(name = "bounce_sub_type", length = 100)
    private String bounceSubType;

    @Column(name = "recipient_email", length = 500)
    private String recipientEmail;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @PrePersist
    public void prePersist() {
        if (this.eventTimestamp == null) this.eventTimestamp = LocalDateTime.now();
    }

    public enum EventType { SEND, DELIVERY, BOUNCE, COMPLAINT }
}
