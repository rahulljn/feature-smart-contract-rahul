package com.geojit.contractnote.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.geojit.contractnote.dto.response.SesStatisticsResponse;
import com.geojit.contractnote.dto.response.SesStatisticsResponse.SesDataPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesStatisticsService {

    private final AmazonSimpleEmailService sesClient;

    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MMM d HH:mm");

    public SesStatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate) {
        try {
            GetSendQuotaResult quota = sesClient.getSendQuota(new GetSendQuotaRequest());
            long max24h      = quota.getMax24HourSend().longValue();
            double maxPerSec = quota.getMaxSendRate();

            long sent24h = quota.getSentLast24Hours().longValue();

            GetSendStatisticsResult stats = sesClient.getSendStatistics(new GetSendStatisticsRequest());

            List<SendDataPoint> allPoints = stats.getSendDataPoints();

            // Filter to requested date range and sort chronologically
            List<SendDataPoint> filtered = allPoints.stream()
                    .filter(dp -> {
                        if (dp.getTimestamp() == null) return false;
                        LocalDate dpDate = dp.getTimestamp().toInstant()
                                .atZone(ZoneOffset.UTC).toLocalDate();
                        return !dpDate.isBefore(startDate) && !dpDate.isAfter(endDate);
                    })
                    .sorted(Comparator.comparing(SendDataPoint::getTimestamp))
                    .toList();

            // Rates use full account history (all available points) to match AWS account health calculation
            long totalSent       = allPoints.stream().mapToLong(SendDataPoint::getDeliveryAttempts).sum();
            long totalBounced    = allPoints.stream().mapToLong(SendDataPoint::getBounces).sum();
            long totalComplaints = allPoints.stream().mapToLong(SendDataPoint::getComplaints).sum();

            double bounceRate    = totalSent > 0 ? Math.round((double) totalBounced    / totalSent * 10000.0) / 100.0 : 0;
            double complaintRate = totalSent > 0 ? Math.round((double) totalComplaints / totalSent * 10000.0) / 100.0 : 0;
            double reputationScore = Math.max(0, 100 - bounceRate * 5);

            long remainingSends    = Math.max(0, max24h - sent24h);
            double quotaUsedPercent = max24h > 0
                    ? Math.round((double) sent24h / max24h * 10000.0) / 100.0 : 0;

            // Return raw 15-minute interval data points (same granularity as AWS console)
            List<SesDataPoint> dataPoints = filtered.stream()
                    .map(dp -> new SesDataPoint(
                            dp.getTimestamp().toInstant().atZone(ZoneOffset.UTC).format(LABEL_FMT),
                            dp.getDeliveryAttempts(),
                            dp.getBounces(),
                            dp.getComplaints(),
                            dp.getRejects()
                    ))
                    .toList();

            return SesStatisticsResponse.builder()
                    .reputationScore(reputationScore)
                    .sendRate(maxPerSec)
                    .bounceRate(bounceRate)
                    .complaintRate(complaintRate)
                    .dailySendQuota(max24h)
                    .sentLast24h(sent24h)
                    .remainingSends(remainingSends)
                    .quotaUsedPercent(quotaUsedPercent)
                    .dataPoints(dataPoints)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch SES statistics: {}", e.getMessage());
            return SesStatisticsResponse.builder()
                    .reputationScore(0).sendRate(0).bounceRate(0).complaintRate(0)
                    .dailySendQuota(0).sentLast24h(0).remainingSends(0).quotaUsedPercent(0)
                    .dataPoints(List.of())
                    .build();
        }
    }
}
