package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.*;
import com.geojit.contractnote.entity.Job;
import com.geojit.contractnote.repository.JobRepository;
import com.geojit.contractnote.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final JobRepository jobRepository;

    @GetMapping("/metrics")
    @Operation(summary = "Get KPI metrics for the dashboard")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getMetrics(from, to)));
    }

    @GetMapping("/monthly-volume")
    @Operation(summary = "Get monthly email volume for a given year")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthlyVolume(
            @RequestParam(defaultValue = "2026") int year) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            java.time.LocalDate start = java.time.LocalDate.of(year, month, 1);
            java.time.LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            List<Job> jobs = jobRepository.findByCreatedAtBetween(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
            int delivered = jobs.stream().mapToInt(Job::getEmailDeliveredCount).sum();
            int bounced = jobs.stream().mapToInt(Job::getEmailBouncedCount).sum();
            int failed = jobs.stream().mapToInt(Job::getEmailFailedCount).sum();
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("month", start.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH));
            m.put("delivered", delivered);
            m.put("bounced", bounced);
            m.put("failed", failed);
            result.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/bounce-breakdown")
    @Operation(summary = "Get bounce breakdown by type")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBounceBreakdown() {
        List<Job> allJobs = jobRepository.findAll();
        int hard = allJobs.stream().mapToInt(Job::getHardBounceCount).sum();
        int soft = allJobs.stream().mapToInt(Job::getSoftBounceCount).sum();
        int total = hard + soft;
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("hardBounce", hard);
        result.put("softBounce", soft);
        result.put("total", total);
        result.put("hardBouncePercent", total > 0 ? Math.round((double) hard / total * 100) : 0);
        result.put("softBouncePercent", total > 0 ? Math.round((double) soft / total * 100) : 0);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/daily-success-rate")
    @Operation(summary = "Get daily open rate and bounce rate for last N days")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailySuccessRate(
            @RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            java.time.LocalDate date = java.time.LocalDate.now().minusDays(i);
            List<Job> dayJobs = jobRepository.findByCreatedAtBetween(
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
            int sent = dayJobs.stream().mapToInt(Job::getEmailSentCount).sum();
            int delivered = dayJobs.stream().mapToInt(Job::getEmailDeliveredCount).sum();
            int bounced = dayJobs.stream().mapToInt(Job::getEmailBouncedCount).sum();
            double openRate = sent > 0 ? Math.round((double) delivered / sent * 1000.0) / 10.0 : 0;
            double bounceRate = sent > 0 ? Math.round((double) bounced / sent * 1000.0) / 10.0 : 0;
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("date", date.toString());
            m.put("month", date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH));
            m.put("openRate", openRate);
            m.put("bounceRate", bounceRate);
            result.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
