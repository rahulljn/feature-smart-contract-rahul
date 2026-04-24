package com.geojit.contractnote.dto.response;

import com.geojit.contractnote.entity.LambdaException;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LambdaExceptionResponse {

    private Long    id;
    private String  jobId;
    private String  lambdaName;
    private String  recordId;
    private String  errorType;
    private String  errorMessage;
    private String  stackTrace;
    private Instant occurredAt;

    public static LambdaExceptionResponse from(LambdaException e) {
        return LambdaExceptionResponse.builder()
                .id(e.getId())
                .jobId(e.getJobId())
                .lambdaName(e.getLambdaName())
                .recordId(e.getRecordId())
                .errorType(e.getErrorType())
                .errorMessage(e.getErrorMessage())
                .stackTrace(e.getStackTrace())
                .occurredAt(e.getOccurredAt())
                .build();
    }
}
