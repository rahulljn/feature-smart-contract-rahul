package com.geojit.contractnote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResendBounceResult {
    private String partyCode;
    private String jobId;
    private String status;     // "QUEUED" or "FAILED"
    private String message;
}
