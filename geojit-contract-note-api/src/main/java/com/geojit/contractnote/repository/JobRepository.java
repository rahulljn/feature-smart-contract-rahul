package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByStatusOrderByUploadedAtDesc(Job.JobStatus status, Pageable pageable);

    Page<Job> findAllByOrderByUploadedAtDesc(Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.uploadedAt >= CURRENT_DATE ORDER BY j.uploadedAt DESC")
    List<Job> findTodaysJobs();

    @Query("SELECT j FROM Job j WHERE j.createdAt >= :start AND j.createdAt < :end ORDER BY j.uploadedAt DESC")
    List<Job> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT j FROM Job j WHERE j.createdAt >= :start AND j.createdAt < :end ORDER BY j.uploadedAt DESC")
    Page<Job> findPageByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.createdAt >= :start AND j.createdAt < :end AND j.status = :status ORDER BY j.uploadedAt DESC")
    Page<Job> findPageByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, Job.JobStatus status, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.tradeDate BETWEEN :from AND :to ORDER BY j.uploadedAt DESC")
    Page<Job> findByTradeDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    // ─── Atomic counter increments (avoid read-modify-write race condition) ───

    @Modifying
    @Query("UPDATE Job j SET j.processedCount = j.processedCount + 1 WHERE j.jobId = :jobId")
    void incrementProcessedCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.pdfGeneratedCount = j.pdfGeneratedCount + 1 WHERE j.jobId = :jobId")
    void incrementPdfGeneratedCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.emailSentCount = j.emailSentCount + 1 WHERE j.jobId = :jobId")
    void incrementEmailSentCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.emailDeliveredCount = j.emailDeliveredCount + 1 WHERE j.jobId = :jobId")
    void incrementEmailDeliveredCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.emailBouncedCount = j.emailBouncedCount + 1 WHERE j.jobId = :jobId")
    void incrementEmailBouncedCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.emailFailedCount = j.emailFailedCount + 1 WHERE j.jobId = :jobId")
    void incrementEmailFailedCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.emailSkippedCount = j.emailSkippedCount + 1 WHERE j.jobId = :jobId")
    void incrementEmailSkippedCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.failedCount = j.failedCount + 1 WHERE j.jobId = :jobId")
    void incrementFailedCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.pdfFailedCount = j.pdfFailedCount + 1 WHERE j.jobId = :jobId")
    void incrementPdfFailedCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.invalidRecordCount = j.invalidRecordCount + 1 WHERE j.jobId = :jobId")
    void incrementInvalidRecordCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.hardBounceCount = j.hardBounceCount + 1 WHERE j.jobId = :jobId")
    void incrementHardBounceCount(UUID jobId);

    @Modifying
    @Query("UPDATE Job j SET j.softBounceCount = j.softBounceCount + 1 WHERE j.jobId = :jobId")
    void incrementSoftBounceCount(UUID jobId);

    @Query("SELECT j FROM Job j WHERE j.status IN ('PROCESSING', 'EMAILING') AND j.uploadedAt < :cutoff")
    List<Job> findStuckJobs(LocalDateTime cutoff);
    java.util.Optional<Job> findByFileName(String fileName);
}
