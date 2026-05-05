package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.entity.Certificate;
import com.geojit.contractnote.entity.Job;
import com.geojit.contractnote.entity.SesConfig;
import com.geojit.contractnote.repository.CertificateRepository;
import com.geojit.contractnote.repository.JobRepository;
import com.geojit.contractnote.repository.SesConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ops")
@Tag(name = "Ops Dashboard")
@RequiredArgsConstructor
@Slf4j
public class OpsController {

    private final JobRepository jobRepository;
    private final SesConfigRepository sesConfigRepository;
    private final CertificateRepository certificateRepository;

    @GetMapping("/today-summary")
    @Operation(summary = "Get today's pipeline summary metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySummary() {
        LocalDate today = LocalDate.now();
        List<Job> todayJobs = jobRepository.findByCreatedAtBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        int total = todayJobs.stream().mapToInt(Job::getTotalCustomers).sum();
        int completed = (int) todayJobs.stream()
                .filter(j -> j.getStatus() == Job.JobStatus.COMPLETED).count();
        int inProgress = (int) todayJobs.stream()
                .filter(j -> j.getStatus() == Job.JobStatus.PROCESSING
                        || j.getStatus() == Job.JobStatus.EMAILING
                        || j.getStatus() == Job.JobStatus.SPLITTING
                        || j.getStatus() == Job.JobStatus.VALIDATING).count();
        int failures = todayJobs.stream().mapToInt(j ->
                j.getEmailFailedCount() + j.getInvalidRecordCount() + j.getPdfFailedCount() + j.getEmailBouncedCount()
        ).sum();
        int delivered = todayJobs.stream()
                .mapToInt(Job::getEmailDeliveredCount)
                .sum();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("completed", completed);
        summary.put("inProgress", inProgress);
        summary.put("failures", failures);
        summary.put("delivered", delivered);
        return ResponseEntity.ok(ApiResponse.ok("Today's summary", summary));
    }

    @GetMapping("/today-jobs")
    @Operation(summary = "Get list of jobs uploaded today")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTodayJobs() {
        LocalDate today = LocalDate.now();
        List<Job> todayJobs = jobRepository.findByCreatedAtBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        List<Map<String, Object>> result = todayJobs.stream().map(job -> {
            Map<String, Object> jobData = new LinkedHashMap<>();
            jobData.put("jobId", job.getJobId());
            jobData.put("fileName", job.getFileName());
            jobData.put("segment", job.getSegmentType());
            jobData.put("records", job.getTotalCustomers());
            jobData.put("status", job.getStatus().name());
            jobData.put("duration", calculateDuration(job.getUploadedAt(), job.getUpdatedAt()));
            jobData.put("pdfFail", job.getPdfFailedCount());
            jobData.put("emailFail", job.getEmailFailedCount());
            jobData.put("delivered", job.getEmailDeliveredCount());
            return jobData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Today's jobs", result));
    }

    @GetMapping("/health")
    @Operation(summary = "Get system health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> health = new LinkedHashMap<>();

        Optional<SesConfig> activeSes = sesConfigRepository.findByIsActiveTrueOrderByConfigSetNameAsc().stream().findFirst();
        if (activeSes.isPresent()) {
            health.put("activeSesConfig", Map.of(
                    "name", activeSes.get().getConfigSetName(),
                    "fromEmail", activeSes.get().getFromEmail()
            ));
        } else {
            health.put("activeSesConfig", null);
        }

        Optional<Certificate> activeCert = certificateRepository.findByIsActiveTrue();
        if (activeCert.isPresent() && activeCert.get().getValidTo() != null) {
            long daysRemaining = activeCert.get().getValidTo().toLocalDate().toEpochDay() - java.time.LocalDate.now().toEpochDay();
            health.put("pfxDaysRemaining", Math.max(0, daysRemaining));
        } else {
            health.put("pfxDaysRemaining", 0);
        }

        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        long jobsStuckCount = jobRepository.findAll().stream()
                .filter(j -> (j.getStatus() == Job.JobStatus.PROCESSING
                        || j.getStatus() == Job.JobStatus.EMAILING)
                        && j.getUpdatedAt() != null
                        && j.getUpdatedAt().isBefore(twoHoursAgo))
                .count();
        health.put("jobsStuckCount", jobsStuckCount);

        List<Map<String, Object>> highBounceJobs = jobRepository.findAll().stream()
                .filter(j -> j.getEmailBouncedCount() > 0
                        
                        && j.getTotalCustomers() > 0)
                .filter(j -> (double) j.getEmailBouncedCount() / j.getTotalCustomers() * 100 > 5.0)
                .map(j -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("jobId", j.getJobId());
                    m.put("fileName", j.getFileName());
                    m.put("bounceRate", (double) j.getEmailBouncedCount() / j.getTotalCustomers() * 100);
                    return m;
                })
                .collect(Collectors.toList());
        health.put("highBounceJobs", highBounceJobs);
        health.put("failedCustomersToday", 0);
        health.put("invalidRecordsToday", 0);
        health.put("lambdaErrorsLastHour", 0);

        return ResponseEntity.ok(ApiResponse.ok("System health status", health));
    }

    @GetMapping("/hourly-failures")
    @Operation(summary = "Get failure distribution by hour (last 24 hours)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHourlyFailures() {
        List<Map<String, Object>> hourlyData = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> hourData = new LinkedHashMap<>();
            hourData.put("hour", String.format("%02d", hour));
            hourData.put("appFailures", 0);
            hourData.put("infraFailures", 0);
            hourlyData.add(hourData);
        }
        return ResponseEntity.ok(ApiResponse.ok("Hourly failures", hourlyData));
    }

    @GetMapping("/failure-distribution")
    @Operation(summary = "Get failure distribution by type for today")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFailureDistribution() {
        LocalDate today = LocalDate.now();
        List<Job> todayJobs = jobRepository.findByCreatedAtBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        long pdfFailed = todayJobs.stream()
                .mapToLong(j -> (long) j.getPdfFailedCount()).sum();
        long emailFailed = todayJobs.stream()
                .mapToLong(j -> (long) j.getEmailFailedCount()).sum();
        long emailBounced = todayJobs.stream()
                .mapToLong(j -> j.getEmailBouncedCount() > 0 ? j.getEmailBouncedCount() : 0).sum();
        long invalidRecords = todayJobs.stream().mapToLong(j -> (long) j.getInvalidRecordCount()).sum();

        List<Map<String, Object>> distribution = new ArrayList<>();
        distribution.add(Map.of("type", "PDF Failed", "count", pdfFailed, "color", "#8b5cf6"));
        distribution.add(Map.of("type", "Email Failed", "count", emailFailed, "color", "#ef4444"));
        distribution.add(Map.of("type", "Email Bounced", "count", emailBounced, "color", "#f59e0b"));
        distribution.add(Map.of("type", "Validation Failed", "count", invalidRecords, "color", "#64748b"));
        return ResponseEntity.ok(ApiResponse.ok("Failure distribution", distribution));
    }

    @GetMapping("/issues")
    @Operation(summary = "Get combined application and infrastructure issues")
    public ResponseEntity<ApiResponse<Map<String, List<Map<String, Object>>>>> getIssues() {
        Map<String, List<Map<String, Object>>> issues = new LinkedHashMap<>();
        List<Map<String, Object>> appIssues = new ArrayList<>();
        List<Map<String, Object>> infraIssues = new ArrayList<>();

        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        long stuckJobs = jobRepository.findAll().stream()
                .filter(j -> (j.getStatus() == Job.JobStatus.PROCESSING
                        || j.getStatus() == Job.JobStatus.EMAILING)
                        && j.getUpdatedAt() != null
                        && j.getUpdatedAt().isBefore(twoHoursAgo))
                .count();
        if (stuckJobs > 0) {
            appIssues.add(Map.of("id", "1", "name", "Stuck Jobs",
                    "severity", "HIGH", "count", stuckJobs,
                    "description", stuckJobs + " jobs stuck for > 2 hours"));
        }

        Optional<Certificate> activeCert = certificateRepository.findByIsActiveTrue();
        activeCert.ifPresent(cert -> {
            if (cert.getValidTo() != null) {
                long daysRemaining = cert.getValidTo().toLocalDate().toEpochDay() - java.time.LocalDate.now().toEpochDay();
                if (daysRemaining <= 30) {
                    appIssues.add(Map.of("id", "3", "name", "Certificate Expiring",
                            "severity", daysRemaining <= 7 ? "HIGH" : "MEDIUM",
                            "count", 1,
                            "description", "Certificate expires in " + daysRemaining + " days"));
                }
            }
        });

        Optional<SesConfig> activeSes = sesConfigRepository.findByIsActiveTrueOrderByConfigSetNameAsc().stream().findFirst();
        infraIssues.add(Map.of("id", "4", "name", "SES Config",
                "severity", "LOW", "count", 1,
                "description", activeSes.map(s -> "Active: " + s.getConfigSetName())
                        .orElse("No active SES config")));

        issues.put("appIssues", appIssues);
        issues.put("infraIssues", infraIssues);
        return ResponseEntity.ok(ApiResponse.ok("Issues", issues));
    }

    private String calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "In progress";
        long seconds = java.time.Duration.between(start, end).getSeconds();
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
}
