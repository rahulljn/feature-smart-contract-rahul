package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "Client 360")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/codes")
    @Operation(summary = "All distinct party codes — used to populate the client code dropdown.")
    public ResponseEntity<ApiResponse<List<String>>> allCodes() {
        return ResponseEntity.ok(ApiResponse.ok(clientService.getAllPartyCodes()));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Autocomplete party codes — returns up to 8 codes matching the given prefix.")
    public ResponseEntity<ApiResponse<List<String>>> suggest(@RequestParam("q") String prefix) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.suggestPartyCodes(prefix)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search client by partyCode (queries DB + S3 metadata). Optional filters: fromDate, toDate, segment.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam("q") String partyCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String segment) {

        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to   = toDate   != null ? toDate.plusDays(1).atStartOfDay() : null;
        return ResponseEntity.ok(ApiResponse.ok(clientService.getClientProfile(partyCode, from, to, segment)));
    }

    @GetMapping("/{partyCode}/pdfs")
    @Operation(summary = "List all PDFs for a client from S3")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listPdfs(@PathVariable String partyCode) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.listPdfs(partyCode)));
    }

    @GetMapping("/{partyCode}/reports")
    @Operation(summary = "List delivery/bounce CSV reports for a client from S3")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listReports(
            @PathVariable String partyCode) {
        Map<String, Object> reports = new HashMap<>();
        reports.put("sentReports",   clientService.listReports(partyCode, "SentLog"));
        reports.put("bounceReports", clientService.listReports(partyCode, "BounceLog"));
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    @GetMapping("/{partyCode}/timeline")
    @Operation(summary = "Email event timeline for a client (from DB)")
    public ResponseEntity<ApiResponse<?>> getTimeline(@PathVariable String partyCode) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.getEmailTimeline(partyCode)));
    }

    @GetMapping("/{partyCode}/pdf-url")
    @Operation(summary = "Get presigned URL for PDF download (s3Key passed as query param to avoid path encoding issues)")
    public ResponseEntity<ApiResponse<String>> getPdfUrl(
            @PathVariable String partyCode,
            @RequestParam("key") String s3Key) {
        String url = clientService.getPdfDownloadUrl(s3Key);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @PutMapping("/{partyCode}/email")
    @Operation(summary = "Update the registered email for a client across all job_customer records")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEmail(
            @PathVariable String partyCode,
            @RequestBody Map<String, String> body) {
        String newEmail = body.get("newEmail");
        int updated = clientService.updateCustomerEmail(partyCode, newEmail);
        return ResponseEntity.ok(ApiResponse.ok("Email updated successfully",
                Map.of("partyCode", partyCode, "newEmail", newEmail, "recordsUpdated", updated)));
    }
}
