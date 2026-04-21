package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.*;
import com.geojit.contractnote.entity.AuditLog;
import com.geojit.contractnote.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit Log")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Get paginated audit log with optional filters. 'actions' accepts comma-separated values e.g. RESEND,BULK_RESEND")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAuditLog(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String actions) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventTimestamp").descending());
        LocalDateTime fromDt = (from != null) ? LocalDateTime.parse(from, DateTimeFormatter.ISO_DATE_TIME) : null;
        LocalDateTime toDt = (to != null) ? LocalDateTime.parse(to, DateTimeFormatter.ISO_DATE_TIME) : null;
        java.util.List<AuditLog.AuditAction> actionList = null;
        if (actions != null && !actions.isBlank()) {
            actionList = java.util.Arrays.stream(actions.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(AuditLog.AuditAction::valueOf)
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(auditService.getFiltered(pageable, search, fromDt, toDt, actionList))));
    }
}
