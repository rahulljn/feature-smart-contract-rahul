package com.geojit.contractnote.service;

import com.geojit.contractnote.entity.AuditLog;
import com.geojit.contractnote.entity.User;
import com.geojit.contractnote.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(User user, AuditLog.AuditAction action, String targetEntity,
                    Map<String, Object> details, HttpServletRequest request) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(user != null ? user.getUserId() : null)
                    .userEmail(user != null ? user.getEmail() : "system")
                    .action(action)
                    .targetEntity(targetEntity)
                    .details(details)
                    .ipAddress(extractIp(request))
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLog> getAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByEventTimestampDesc(pageable);
    }

    public Page<AuditLog> getFiltered(Pageable pageable, String search, LocalDateTime from, LocalDateTime to, List<AuditLog.AuditAction> actions) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("userEmail")), pattern),
                    cb.like(cb.lower(root.get("targetEntity")), pattern)
                ));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventTimestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventTimestamp"), to));
            }
            if (actions != null && !actions.isEmpty()) {
                predicates.add(root.get("action").in(actions));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return auditLogRepository.findAll(spec, pageable);
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
