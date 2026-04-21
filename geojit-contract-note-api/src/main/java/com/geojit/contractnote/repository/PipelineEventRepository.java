package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.PipelineEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PipelineEventRepository extends JpaRepository<PipelineEvent, Long> {

    List<PipelineEvent> findByJobIdOrderByEventTimestampAsc(UUID jobId);

    Page<PipelineEvent> findByJobId(UUID jobId, Pageable pageable);

    List<PipelineEvent> findByJobIdAndPartyCode(UUID jobId, String partyCode);

    @Query(value = "SELECT EXTRACT(HOUR FROM e.event_timestamp) AS hour, COUNT(*) AS cnt FROM pipeline_events e WHERE CAST(e.event_timestamp AS DATE) = CURRENT_DATE GROUP BY EXTRACT(HOUR FROM e.event_timestamp) ORDER BY hour", nativeQuery = true)
    List<Object[]> findHourlyEventCounts();

    @Query(value = "SELECT EXTRACT(HOUR FROM e.event_timestamp) AS hour, COUNT(*) AS cnt FROM pipeline_events e WHERE e.event_timestamp >= :start AND e.event_timestamp < :end GROUP BY EXTRACT(HOUR FROM e.event_timestamp) ORDER BY hour", nativeQuery = true)
    List<Object[]> findHourlyEventCountsBetween(@org.springframework.data.repository.query.Param("start") LocalDateTime start, @org.springframework.data.repository.query.Param("end") LocalDateTime end);

    List<PipelineEvent> findTop10ByEventTimestampAfterOrderByEventTimestampDesc(LocalDateTime since);
}
