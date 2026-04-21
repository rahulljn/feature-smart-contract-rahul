package com.geojit.contractnote.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SesStatisticsResponse {
    private double reputationScore;
    private double sendRate;
    private double bounceRate;
    private double complaintRate;
    private int    dailySendQuota;
    private int    sentLast24h;
}
