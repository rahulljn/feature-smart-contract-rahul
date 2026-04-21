package com.geojit.contractnote.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.geojit.contractnote.dto.response.SesStatisticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesStatisticsService {

    private final AmazonSimpleEmailService sesClient;

    @Cacheable(value = "ses-stats", key = "'daily'")
    public SesStatisticsResponse getStatistics() {
        try {

            // Get send quota
            GetSendQuotaResult quota = sesClient.getSendQuota(new GetSendQuotaRequest());
            double max24h = quota.getMax24HourSend();
            double sent24h = quota.getSentLast24Hours();
            double maxPerSec = quota.getMaxSendRate();

            // Get send statistics (last 2 weeks of daily data points)
            GetSendStatisticsResult stats = sesClient.getSendStatistics(new GetSendStatisticsRequest());
            List<SendDataPoint> dataPoints = stats.getSendDataPoints();

            long totalSent = 0;
            long totalBounced = 0;
            long totalComplaints = 0;
            for (SendDataPoint dp : dataPoints) {
                totalSent += dp.getDeliveryAttempts();
                totalBounced += dp.getBounces();
                totalComplaints += dp.getComplaints();
            }

            double bounceRate = totalSent > 0 ? Math.round((double) totalBounced / totalSent * 10000.0) / 100.0 : 0;
            double complaintRate = totalSent > 0 ? Math.round((double) totalComplaints / totalSent * 10000.0) / 100.0 : 0;

            // Reputation score (simplified: 100 - bounce rate impact)
            double reputationScore = Math.max(0, 100 - bounceRate * 5);

            return SesStatisticsResponse.builder()
                    .reputationScore(reputationScore)
                    .sendRate(maxPerSec)
                    .bounceRate(bounceRate)
                    .complaintRate(complaintRate)
                    .dailySendQuota((int) max24h)
                    .sentLast24h((int) sent24h)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch SES statistics: {}", e.getMessage());
            return SesStatisticsResponse.builder()
                    .reputationScore(0)
                    .sendRate(0)
                    .bounceRate(0)
                    .complaintRate(0)
                    .dailySendQuota(0)
                    .sentLast24h(0)
                    .build();
        }
    }
}
