package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findAllByOrderByEventTimestampDesc(Pageable pageable);

    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.eventTimestamp BETWEEN :from AND :to ORDER BY a.eventTimestamp DESC")
    Page<AuditLog> findByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
