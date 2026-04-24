package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.dto.response.LambdaExceptionResponse;
import com.geojit.contractnote.service.LambdaExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exceptions")
@Tag(name = "Lambda Exceptions")
@RequiredArgsConstructor
public class LambdaExceptionController {

    private final LambdaExceptionService lambdaExceptionService;

    @GetMapping
    @Operation(summary = "Drain SQS exception queue and return persisted exceptions for a job")
    public ResponseEntity<ApiResponse<List<LambdaExceptionResponse>>> getExceptions(
            @RequestParam String jobId,
            @RequestParam(required = false) String lambdaName) {
        List<LambdaExceptionResponse> exceptions = lambdaExceptionService.fetchAndPersist(jobId, lambdaName);
        return ResponseEntity.ok(ApiResponse.ok(exceptions));
    }
}
