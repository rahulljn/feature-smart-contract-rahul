package com.geojit.contractnote.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardMetricsResponse {
    private long totalCustomersInPipeline;
    private long totalPdfsGenerated;
    private long totalEmailsSent;
    private long totalDelivered;
    private long totalConfirmed;
    private long totalBounced;
    private double bounceRate;
    private double deliveryRate;
    private long activeJobs;
    private long failedJobs;
    private List<HourlyActivity> hourlyActivity;
    private List<RecentActivity> recentActivity;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HourlyActivity {
        private int hour;
        private long eventCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentActivity {
        private String eventType;
        private String jobId;
        private String partyCode;
        private String description;
        private LocalDateTime eventTimestamp;
    }
}
