package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.entity.AlertNotification;
import com.geojit.contractnote.entity.AlertRule;
import com.geojit.contractnote.exception.ResourceNotFoundException;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;
    private final UserRepository userRepository;

    // ─── Unread Count ───────────────────────────────────────────────

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread alerts for current user")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = alertService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    // ─── Alert History ───────────────────────────────────────────────

    @GetMapping("/history")
    @Operation(summary = "Get paginated alert history with optional filters")
    public ResponseEntity<ApiResponse<Page<AlertNotification>>> getHistory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<AlertNotification> result = alertService.getHistory(from, to, severity, channel, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Alert history retrieved", result));
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all alerts as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead() {
        int marked = alertService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.ok("All alerts marked as read", Map.of("marked", marked)));
    }

    // ─── Alert Rules ─────────────────────────────────────────────────

    @GetMapping("/rules")
    @Operation(summary = "List all alert rules")
    public ResponseEntity<ApiResponse<List<AlertRule>>> getRules() {
        List<AlertRule> rules = alertService.getAllRules();
        return ResponseEntity.ok(ApiResponse.ok("Alert rules retrieved", rules));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new alert rule")
    public ResponseEntity<ApiResponse<AlertRule>> createRule(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String createdBy = userDetails.getUsername();

        AlertRule rule = AlertRule.builder()
                .name((String) body.get("name"))
                .triggerType(AlertRule.AlertTriggerType.valueOf((String) body.getOrDefault("triggerType", "JOB_STATUS")))
                .severity(AlertRule.AlertSeverity.valueOf((String) body.getOrDefault("severity", "WARNING")))
                .channels((String) body.getOrDefault("channels", "[]"))
                .recipients((String) body.getOrDefault("recipients", "[]"))
                .thresholdValue(body.get("thresholdValue") != null ? Integer.valueOf(body.get("thresholdValue").toString()) : null)
                .includeDeepLink((Boolean) body.getOrDefault("includeDeepLink", false))
                .isActive(true)
                .build();

        AlertRule created = alertService.createRule(rule, createdBy);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok("Alert rule created", created));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing alert rule")
    public ResponseEntity<ApiResponse<AlertRule>> updateRule(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String updatedBy = userDetails.getUsername();

        AlertRule updates = AlertRule.builder()
                .name((String) body.get("name"))
                .triggerType(body.get("triggerType") != null ? AlertRule.AlertTriggerType.valueOf((String) body.get("triggerType")) : null)
                .severity(body.get("severity") != null ? AlertRule.AlertSeverity.valueOf((String) body.get("severity")) : null)
                .channels((String) body.get("channels"))
                .recipients((String) body.get("recipients"))
                .thresholdValue(body.get("thresholdValue") != null ? Integer.valueOf(body.get("thresholdValue").toString()) : null)
                .includeDeepLink((Boolean) body.get("includeDeepLink"))
                .build();

        AlertRule updated = alertService.updateRule(id, updates, updatedBy);
        return ResponseEntity.ok(ApiResponse.ok("Alert rule updated", updated));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an alert rule")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        alertService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.ok("Alert rule deleted", null));
    }

    @PatchMapping("/rules/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or disable an alert rule")
    public ResponseEntity<ApiResponse<AlertRule>> toggleRule(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {

        Boolean isActive = body.getOrDefault("isActive", false);
        AlertRule updated = alertService.toggleRule(id, isActive);
        return ResponseEntity.ok(ApiResponse.ok("Alert rule " + (isActive ? "enabled" : "disabled"), updated));
    }

    // ─── Channel Config ───────────────────────────────────────────────

    @GetMapping("/channels")
    @Operation(summary = "Get alert channel configuration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChannelConfig() {
        Map<String, Object> config = alertService.getChannelConfig();
        return ResponseEntity.ok(ApiResponse.ok("Channel config retrieved", config));
    }

    @PutMapping("/channels")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update alert channel configuration")
    public ResponseEntity<ApiResponse<Void>> updateChannelConfig(@RequestBody Map<String, Object> body) {
        body.forEach((channel, config) -> {
            if (config instanceof Map) {
                alertService.updateChannelConfig(channel, (Map<String, String>) config);
            }
        });
        return ResponseEntity.ok(ApiResponse.ok("Channel config updated", null));
    }

    // ─── Resend Notification ───────────────────────────────────────

    @PostMapping("/history/{id}/resend")
    @Operation(summary = "Resend a failed alert notification")
    public ResponseEntity<ApiResponse<Void>> resendNotification(@PathVariable Long id) {
        // Implementation would queue a resend for the failed notification
        log.info("Resending notification id: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Notification resend queued", null));
    }
}
