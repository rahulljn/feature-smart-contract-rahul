package com.geojit.contractnote.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ResendRequest {

    @NotNull(message = "jobId is required")
    private UUID jobId;

    /** List of party codes to resend. If empty, triggers bulk resend of all failed in the job. */
    @NotEmpty(message = "At least one partyCode is required")
    private List<String> partyCodes;
}
