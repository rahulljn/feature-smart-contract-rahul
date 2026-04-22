package com.geojit.contractnote.dto.response;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SesStatisticsResponse {
    private double reputationScore;
    private double sendRate;
    private double bounceRate;
    private double complaintRate;
    private long   dailySendQuota;
    private long   sentLast24h;
    private long   remainingSends;
    private double quotaUsedPercent;
    private List<SesDataPoint> dataPoints;

    public record SesDataPoint(String timestamp, long sends, long bounces, long complaints, long rejects) {}
}
