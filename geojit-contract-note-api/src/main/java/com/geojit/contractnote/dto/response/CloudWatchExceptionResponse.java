package com.geojit.contractnote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CloudWatchExceptionResponse {
    private String timestamp;
    private String logGroup;
    private String logStream;
    private String message;
}
