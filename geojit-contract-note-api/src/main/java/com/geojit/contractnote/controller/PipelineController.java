package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.entity.PipelineEvent;
import com.geojit.contractnote.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipeline")
@Tag(name = "Pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @Value("${app.internal-api-key:dev-internal-key}")
    private String internalApiKey;

    @GetMapping("/events")
    @Operation(summary = "Get all pipeline events for a job")
    public ResponseEntity<ApiResponse<List<PipelineEvent>>> getEvents(@RequestParam UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(pipelineService.getJobEvents(jobId)));
    }

    @PostMapping("/status-event")
    @Operation(summary = "Receive a status event from Lambda (Status Consumer webhook)")
    public ResponseEntity<ApiResponse<Void>> receiveStatusEvent(
            @RequestHeader(value = "X-Internal-Key", required = false) String apiKey,
            @RequestBody PipelineEvent event) {
        if (internalApiKey != null && !internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        pipelineService.processStatusEvent(event);
        return ResponseEntity.ok(ApiResponse.ok("Event processed", null));
    }
}
