package com.geojit.contractnote.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.geojit.contractnote.dto.response.SesStatisticsResponse;
import com.geojit.contractnote.dto.response.SesStatisticsResponse.SesDataPoint;
import com.geojit.contractnote.repository.JobCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesStatisticsService {

    private final AmazonSimpleEmailService sesClient;
    private final JobCustomerRepository jobCustomerRepository;

    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MMM d");

    public SesStatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate) {
        try {
            GetSendQuotaResult quota = sesClient.getSendQuota(new GetSendQuotaRequest());
            long max24h      = quota.getMax24HourSend().longValue();
            double maxPerSec = quota.getMaxSendRate();

            // Use our own DB records for accuracy — AWS quota counter may reflect
            // a different IAM identity than the Lambda that actually sends emails.
            long sent24h = jobCustomerRepository.countEmailsSentSince(
                    LocalDateTime.now(ZoneOffset.UTC).minusHours(24));

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

            // Aggregate full-range totals for rates (use all points for accuracy)
            long totalSent       = allPoints.stream().mapToLong(SendDataPoint::getDeliveryAttempts).sum();
            long totalBounced    = allPoints.stream().mapToLong(SendDataPoint::getBounces).sum();
            long totalComplaints = allPoints.stream().mapToLong(SendDataPoint::getComplaints).sum();

            double bounceRate    = totalSent > 0 ? Math.round((double) totalBounced    / totalSent * 10000.0) / 100.0 : 0;
            double complaintRate = totalSent > 0 ? Math.round((double) totalComplaints / totalSent * 10000.0) / 100.0 : 0;
            double reputationScore = Math.max(0, 100 - bounceRate * 5);

            long remainingSends    = Math.max(0, max24h - sent24h);
            double quotaUsedPercent = max24h > 0
                    ? Math.round((double) sent24h / max24h * 10000.0) / 100.0 : 0;

            // Aggregate 15-min AWS intervals into one total per calendar day
            LinkedHashMap<LocalDate, long[]> byDay = new LinkedHashMap<>();
            for (SendDataPoint dp : filtered) {
                LocalDate day = dp.getTimestamp().toInstant()
                        .atZone(ZoneOffset.UTC).toLocalDate();
                long[] totals = byDay.computeIfAbsent(day, k -> new long[4]);
                totals[0] += dp.getDeliveryAttempts();
                totals[1] += dp.getBounces();
                totals[2] += dp.getComplaints();
                totals[3] += dp.getRejects();
            }

            List<SesDataPoint> dataPoints = byDay.entrySet().stream()
                    .map(e -> new SesDataPoint(
                            e.getKey().format(LABEL_FMT),
                            e.getValue()[0],
                            e.getValue()[1],
                            e.getValue()[2],
                            e.getValue()[3]
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
