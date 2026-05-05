package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.AlertNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {

    @Query("SELECT COUNT(n) FROM AlertNotification n WHERE n.isRead = false")
    long countUnread();

    Page<AlertNotification> findByIsReadFalseOrderBySentAtDesc(Pageable pageable);

    @Query("SELECT n FROM AlertNotification n WHERE " +
           "(:fromDate IS NULL OR n.sentAt >= :fromDate) AND " +
           "(:toDate IS NULL OR n.sentAt <= :toDate) AND " +
           "(:severity IS NULL OR n.ruleName IN :severity) AND " +
           "(:channel IS NULL OR n.channel IN :channel)")
    Page<AlertNotification> findByFilters(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("severity") List<String> severity,
            @Param("channel") List<String> channel,
            Pageable pageable);

    @Query("UPDATE AlertNotification n SET n.isRead = true WHERE n.isRead = false")
    int markAllAsRead();

    @Query("DELETE FROM AlertNotification n WHERE n.id IN :ids")
    void deleteByIds(@Param("ids") List<Long> ids);
}
