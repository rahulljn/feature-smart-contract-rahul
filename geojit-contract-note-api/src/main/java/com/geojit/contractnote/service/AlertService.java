package com.geojit.contractnote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.entity.AlertNotification;
import com.geojit.contractnote.entity.AlertRule;
import com.geojit.contractnote.repository.AlertNotificationRepository;
import com.geojit.contractnote.exception.ResourceNotFoundException;
import com.geojit.contractnote.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertNotificationRepository alertNotificationRepository;
    private final ObjectMapper objectMapper;

    // ─── Alert Rules ────────────────────────────────────────────────

    public List<AlertRule> getAllRules() {
        return alertRuleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public AlertRule createRule(AlertRule rule, String createdBy) {
        rule.setCreatedBy(createdBy);
        return alertRuleRepository.save(rule);
    }

    public AlertRule updateRule(UUID id, AlertRule updates, String updatedBy) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule", "id", id));
        if (updates.getName() != null) rule.setName(updates.getName());
        if (updates.getTriggerType() != null) rule.setTriggerType(updates.getTriggerType());
        if (updates.getSeverity() != null) rule.setSeverity(updates.getSeverity());
        if (updates.getChannels() != null) rule.setChannels(updates.getChannels());
        if (updates.getRecipients() != null) rule.setRecipients(updates.getRecipients());
        if (updates.getThresholdValue() != null) rule.setThresholdValue(updates.getThresholdValue());
        if (updates.getIncludeDeepLink() != null) rule.setIncludeDeepLink(updates.getIncludeDeepLink());
        rule.setCreatedBy(updatedBy);
        return alertRuleRepository.save(rule);
    }

    public void deleteRule(UUID id) {
        if (!alertRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("AlertRule", "id", id);
        }
        alertRuleRepository.deleteById(id);
    }

    public AlertRule toggleRule(UUID id, Boolean isActive) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule", "id", id));
        rule.setIsActive(isActive);
        return alertRuleRepository.save(rule);
    }

    // ─── Alert Notifications ───────────────────────────────────────

    public long getUnreadCount() {
        return alertNotificationRepository.countUnread();
    }

    public Page<AlertNotification> getHistory(LocalDateTime fromDate, LocalDateTime toDate,
                                               String severity, String channel, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));

        List<String> severityList = severity != null ? Arrays.asList(severity.split(",")) : null;
        List<String> channelList = channel != null ? Arrays.asList(channel.split(",")) : null;

        if (severityList == null && channelList == null) {
            return alertNotificationRepository.findByIsReadFalseOrderBySentAtDesc(pageable);
        }
        return alertNotificationRepository.findByFilters(fromDate, toDate, severityList, channelList, pageable);
    }

    @Transactional
    public int markAllAsRead() {
        return alertNotificationRepository.markAllAsRead();
    }

    @Transactional
    public void deleteHistory(LocalDateTime before) {
        if (before != null) {
            List<AlertNotification> toDelete = alertNotificationRepository.findAll()
                    .stream()
                    .filter(n -> n.getSentAt().isBefore(before))
                    .collect(java.util.stream.Collectors.toList());
            if (!toDelete.isEmpty()) {
                alertNotificationRepository.deleteAll(toDelete);
            }
        }
    }

    // ─── Alert Creation (triggered by events) ───────────────────────

    @Transactional
    public void createAlert(String ruleName, String triggeredBy, AlertNotification.AlertChannel channel,
                        String recipient, String details, UUID jobId, Boolean includeDeepLink) {
        AlertNotification notification = AlertNotification.builder()
                .ruleName(ruleName)
                .triggeredBy(triggeredBy)
                .channel(channel)
                .recipient(recipient)
                .details(details)
                .jobId(jobId)
                .status(AlertNotification.NotificationStatus.PENDING)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        alertNotificationRepository.save(notification);
    }

    private String triggeredByWithDeepLink(String triggeredBy, UUID jobId, Boolean includeDeepLink) {
        if (includeDeepLink != null && includeDeepLink && jobId != null) {
            return triggeredBy + " | DeepLink: /jobs/" + jobId;
        }
        return triggeredBy;
    }

    // ─── Channel Config ───────────────────────────────────────

    public Map<String, Object> getChannelConfig() {
        Map<String, Object> config = new HashMap<>();

        // Default config - could be stored in DB or env vars
        config.put("email", Map.of(
                "enabled", true,
                "smtpHost", System.getenv().getOrDefault("SMTP_HOST", ""),
                "fromAddress", System.getenv().getOrDefault("EMAIL_FROM", "noreply@geojit.com")
        ));
        config.put("sms", Map.of(
                "enabled", false,
                "provider", System.getenv().getOrDefault("SMS_PROVIDER", "Twilio"),
                "apiKey", "***"
        ));
        config.put("whatsapp", Map.of(
                "enabled", false,
                "businessId", "***"
        ));

        return config;
    }

    @Transactional
    public void updateChannelConfig(String channel, Map<String, String> config) {
        // In a real implementation, this would persist to app_config table
        // For now, we log and return success
        log.info("Updated channel config for {}: {}", channel, config);
    }

    // ─── Helper Methods ───────────────────────────────────────────────

    public List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON array: {}", json, e);
            return List.of();
        }
    }
}
