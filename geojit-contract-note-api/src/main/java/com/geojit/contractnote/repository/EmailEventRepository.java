package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.EmailEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailEventRepository extends JpaRepository<EmailEvent, Long> {

    List<EmailEvent> findByPartyCodeOrderByEventTimestampDesc(String partyCode);

    List<EmailEvent> findByJobIdOrderByEventTimestampDesc(UUID jobId);

    List<EmailEvent> findBySesMessageId(String sesMessageId);

    long countByEventType(EmailEvent.EventType eventType);
}
