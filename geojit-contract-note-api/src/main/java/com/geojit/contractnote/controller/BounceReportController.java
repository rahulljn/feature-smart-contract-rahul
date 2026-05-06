package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.*;
import com.geojit.contractnote.service.S3BounceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/bounce-report")
@RequiredArgsConstructor
@Tag(name = "Bounce Report", description = "S3 bounce log retrieval and resend functionality")
public class BounceReportController {

    private final S3BounceService s3BounceService;

    @Operation(summary = "List bounce records by date range and segment")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BounceListResponse>> listBounceRecords(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false) String segment) {

        LocalDate today = LocalDate.now();
        LocalDate defaultFrom = today.minusDays(7);
        if (from == null) from = defaultFrom;
        if (to == null) to = today;

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("from must be before or equal to to"));
        }

        List<BounceRecord> records = s3BounceService.listBounceRecords(from, to, segment);
        BounceListResponse response = new BounceListResponse(records, records.size(), from, to, segment);
        return ResponseEntity.ok(ApiResponse.ok("Bounce records retrieved", response));
    }

    @Operation(summary = "Get bounce records for a specific client")
    @GetMapping("/client/{partyCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BounceRecord>>> getClientBounceRecords(
            @PathVariable String partyCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false) String segment) {

        LocalDate today = LocalDate.now();
        LocalDate defaultFrom = today.minusDays(7);
        if (from == null) from = defaultFrom;
        if (to == null) to = today;

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("from must be before or equal to to"));
        }

        List<BounceRecord> records = s3BounceService.getClientBounceRecords(partyCode, from, to, segment);
        return ResponseEntity.ok(ApiResponse.ok("Client bounce records retrieved", records));
    }

    @Operation(summary = "Get presigned download URL for a bounce CSV")
    @GetMapping("/client/{partyCode}/download-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDownloadUrl(
            @PathVariable String partyCode,
            @RequestParam String s3Key) {

        if (!s3Key.startsWith("GeojitCN-EmailReport/")) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Invalid S3 key for presigned URL: " + s3Key));
        }

        String url = s3BounceService.generatePresignedUrl(s3Key);
        Map<String, Object> response = new HashMap<>();
        response.put("url", url);
        response.put("expiresInSeconds", 900);
        return ResponseEntity.ok(ApiResponse.ok("Presigned URL generated", response));
    }

    @Operation(summary = "Download all bounce reports merged as CSV")
    @GetMapping(value = "/download-all", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadAllReports(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false) String segment) {

        LocalDate today = LocalDate.now();
        LocalDate defaultFrom = today.minusDays(7);
        if (from == null) from = defaultFrom;
        if (to == null) to = today;

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] csvBytes = s3BounceService.downloadAllReportsMerged(from, to, segment);
            String filename = "BounceReport_" + from + "_to_" + to + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvBytes);
        } catch (java.io.IOException e) {
            log.error("Failed to download bounce reports: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Resend a single bounce record")
    @PostMapping("/resend")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    public ResponseEntity<ApiResponse<ResendBounceResult>> resendBounce(
            @RequestBody ResendBounceRequest request) {

        log.info("Resend requested for partyCode [{}] fileName [{}]", request.partyCode, request.fileName);

        s3BounceService.resendBounce(request.partyCode, request.fileName, request.email);
        return ResponseEntity.ok(ApiResponse.ok(
                "Resend queued successfully",
                new ResendBounceResult(request.partyCode, null, "QUEUED", "Resend queued successfully")
        ));
    }

    @Operation(summary = "Bulk resend multiple bounce records")
    @PostMapping("/resend-bulk")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    public ResponseEntity<ApiResponse<BulkResendResponse>> resendBounceBulk(
            @RequestBody BulkResendBounceRequest request) {

        log.info("Bulk resend requested for [{}] records", request.getRecords().size());

        List<ResendBounceResult> results = new ArrayList<>();
        int queued = 0;
        int failed = 0;

        for (ResendBounceRequest item : request.getRecords()) {
            try {
                s3BounceService.resendBounce(item.getPartyCode(), item.getFileName(), item.getEmail());
                results.add(new ResendBounceResult(item.getPartyCode(), null, "QUEUED", "Resend queued successfully"));
                queued++;
            } catch (Exception e) {
                log.error("Resend failed for partyCode [{}]: {}", item.getPartyCode(), e.getMessage());
                results.add(new ResendBounceResult(item.getPartyCode(), null, "FAILED", e.getMessage()));
                failed++;
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(
                "Bulk resend completed",
                new BulkResendResponse(queued, failed, results)
        ));
    }

    @lombok.Data
    public static class ResendBounceRequest {
        private String partyCode;
        private String fileName;
        private String email;
    }

    @lombok.Data
    public static class BulkResendBounceRequest {
        private List<ResendBounceRequest> records;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BulkResendResponse {
        private int queued;
        private int failed;
        private List<ResendBounceResult> results;
    }
}
