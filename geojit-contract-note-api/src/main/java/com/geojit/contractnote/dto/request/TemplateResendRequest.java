package com.geojit.contractnote.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for resending a contract note using a specific email template.
 */
@Data
public class TemplateResendRequest {

    @NotBlank(message = "partyCode is required")
    private String partyCode;

    /** Optional — if provided, resends from that specific job. If absent, uses the most recent job for the partyCode. */
    private UUID jobId;

    /** Optional override destination email (uses customer's registered email if absent). */
    private String overrideEmail;
}
