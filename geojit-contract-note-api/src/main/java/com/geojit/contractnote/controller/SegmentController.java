package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.dto.response.SegmentDto;
import com.geojit.contractnote.entity.Segment;
import com.geojit.contractnote.service.SegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/segments")
@RequiredArgsConstructor
@Tag(name = "Segments", description = "Segment management for bounce report partitioning")
public class SegmentController {

    private final SegmentService segmentService;

    @Operation(summary = "List all active segments")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<SegmentDto>>> listSegments() {
        List<Segment> segments = segmentService.getActiveSegments();
        List<SegmentDto> dtos = segments.stream()
                .map(s -> SegmentDto.builder()
                        .id(s.getId())
                        .code(s.getCode())
                        .displayName(s.getDisplayName())
                        .s3Folder(s.getS3Folder())
                        .isActive(s.isActive())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Active segments retrieved", dtos));
    }
}
