package com.geojit.contractnote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExceptionCountsResponse {
    private long pdfFailed;
    private long emailFailed;
    private long bounced;
    private long skipped;
    private long invalidRecords;
    private long hardBounced;
    private long softBounced;
}
