package com.geojit.contractnote.service;

import com.geojit.contractnote.dto.response.DashboardMetricsResponse;
import com.geojit.contractnote.dto.response.DashboardMetricsResponse.*;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JobRepository jobRepository;
    private final PipelineEventRepository pipelineEventRepository;

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics(LocalDate from, LocalDate to) {
        // Default to today if no dates provided
        LocalDate startDate = from != null ? from : LocalDate.now();
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Job> jobsInRange = jobRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        long totalCustomers = jobsInRange.stream().mapToLong(Job::getTotalCustomers).sum();
        long activeJobs = jobsInRange.stream()
                .filter(j -> j.getStatus() == Job.JobStatus.PROCESSING
                          || j.getStatus() == Job.JobStatus.EMAILING
                          || j.getStatus() == Job.JobStatus.SPLITTING)
                .count();
        long failedJobs = jobsInRange.stream()
                .filter(j -> j.getStatus() == Job.JobStatus.FAILED)
                .count();

        long totalPdfsGenerated = jobsInRange.stream().mapToLong(Job::getPdfGeneratedCount).sum();
        long totalEmailsSent    = jobsInRange.stream().mapToLong(Job::getEmailSentCount).sum();
        long totalDelivered     = jobsInRange.stream().mapToLong(Job::getEmailDeliveredCount).sum(); // SNS-backed delivery receipts
        long totalBounced       = jobsInRange.stream().mapToLong(Job::getEmailBouncedCount).sum();
        long totalFailed        = jobsInRange.stream().mapToLong(Job::getFailedCount).sum();

        long emailTotal = totalEmailsSent + totalBounced;
        double bounceRate   = emailTotal > 0 ? Math.round((double) totalBounced  / emailTotal    * 10000.0) / 100.0 : 0;
        double deliveryRate = totalEmailsSent > 0 ? Math.round((double) totalDelivered / totalEmailsSent * 10000.0) / 100.0 : 0;

        // Hourly activity — events grouped by hour for the date range
        List<HourlyActivity> hourlyActivity = buildHourlyActivity(startDateTime, endDateTime);

        // Recent activity — last 10 pipeline events in the range
        List<RecentActivity> recentActivity = buildRecentActivity(startDateTime);

        return DashboardMetricsResponse.builder()
                .totalCustomersInPipeline(totalCustomers)
                .totalPdfsGenerated(totalPdfsGenerated)
                .totalEmailsSent(totalEmailsSent)
                .totalDelivered(totalDelivered)
                .totalBounced(totalBounced)
                .bounceRate(bounceRate)
                .deliveryRate(deliveryRate)
                .activeJobs(activeJobs)
                .failedJobs(totalFailed > 0 ? totalFailed : failedJobs)
                .hourlyActivity(hourlyActivity)
                .recentActivity(recentActivity)
                .build();
    }

    private List<HourlyActivity> buildHourlyActivity(LocalDateTime start, LocalDateTime end) {
        // Build a map of hour → count from DB query
        Map<Integer, Long> hourMap = new LinkedHashMap<>();
        try {
            List<Object[]> rows = pipelineEventRepository.findHourlyEventCountsBetween(start, end);
            for (Object[] row : rows) {
                int hour = ((Number) row[0]).intValue();
                long cnt = ((Number) row[1]).longValue();
                hourMap.put(hour, cnt);
            }
        } catch (Exception e) {
            log.warn("Failed to query hourly activity: {}", e.getMessage());
        }

        // Fill 0-23 hours
        List<HourlyActivity> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(HourlyActivity.builder()
                    .hour(h)
                    .eventCount(hourMap.getOrDefault(h, 0L))
                    .build());
        }
        return result;
    }

    private List<RecentActivity> buildRecentActivity(LocalDateTime since) {
        List<RecentActivity> result = new ArrayList<>();
        try {
            List<PipelineEvent> events = pipelineEventRepository
                    .findTop10ByEventTimestampAfterOrderByEventTimestampDesc(since);

            for (PipelineEvent e : events) {
                String desc = describeEvent(e);
                result.add(RecentActivity.builder()
                        .eventType(e.getEventType().name())
                        .jobId(e.getJobId() != null ? e.getJobId().toString() : null)
                        .partyCode(normaliseCode(e.getPartyCode()))
                        .description(desc)
                        .eventTimestamp(e.getEventTimestamp())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to query recent activity: {}", e.getMessage());
        }
        return result;
    }

    private String describeEvent(PipelineEvent e) {
        String code = normaliseCode(e.getPartyCode());
        String jobShort = e.getJobId() != null ? e.getJobId().toString().substring(0, 8) : "unknown";
        return switch (e.getEventType()) {
            case JOB_REGISTERED       -> "Job registered — " + jobShort;
            case CUSTOMER_REGISTERED  -> "Customer " + code + " registered";
            case PDF_GENERATED        -> "PDF generated for " + code;
            case PDF_FAILED           -> "PDF failed for " + code;
            case EMAIL_SENT           -> "Email sent for " + code;
            case EMAIL_FAILED         -> "Email failed for " + code;
            case EMAIL_SKIPPED        -> "Email delivery failed — no address for " + code;
            case DELIVERY             -> "Email delivered to " + code;
            case BOUNCE               -> "Email bounced for " + code;
            case COMPLAINT            -> "Complaint from " + code;
            case RESEND_TRIGGERED     -> "Resend triggered for " + code;
            case PDF_TRIGGERED        -> "PDF triggered for " + code;
            case SPLIT_PROGRESS, SPLIT_COMPLETE -> "Splitting in progress for job " + jobShort;
        };
    }

    private String normaliseCode(String code) {
        if (code == null) return "unknown";
        return code.contains("/") ? code.split("/")[0] : code;
    }
}
