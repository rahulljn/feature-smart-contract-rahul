package com.geojit.contractnote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CloudWatchLambdaResponse {
    private String lambdaName;
    private String logGroup;
    private int    errorCount;
    private List<CloudWatchExceptionResponse> events;
}
