package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.Job;
import com.geojit.contractnote.entity.JobCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobCustomerRepository extends JpaRepository<JobCustomer, Long> {

    Page<JobCustomer> findByJob_JobId(UUID jobId, Pageable pageable);

    Optional<JobCustomer> findByJob_JobIdAndPartyCode(UUID jobId, String partyCode);

    List<JobCustomer> findByPartyCodeOrderByCreatedAtDesc(String partyCode);

    List<JobCustomer> findByJob_JobIdAndEmailStatus(UUID jobId, JobCustomer.EmailStatus emailStatus);

    // Matches both "ZYR175" and "ZYR175/ZYR175" stored by Lambda
    @Query("SELECT jc FROM JobCustomer jc WHERE jc.partyCode = :partyCode OR jc.partyCode LIKE CONCAT(:partyCode, '/%') ORDER BY jc.createdAt DESC")
    List<JobCustomer> findAllByPartyCode(String partyCode);

    /**
     * Filtered history for a partyCode — date range and segment are optional.
     * fromDate/toDate filter on createdAt (when the job was processed).
     * segment uses case-insensitive contains match (pass null to skip).
     * Matches both "ZYR175" and "ZYR175/ZYR175" stored by Lambda.
     */
    @Query("""
            SELECT jc FROM JobCustomer jc
            WHERE (jc.partyCode = :partyCode OR jc.partyCode LIKE CONCAT(:partyCode, '/%'))
              AND (:fromDate IS NULL OR jc.createdAt >= :fromDate)
              AND (:toDate   IS NULL OR jc.createdAt <  :toDate)
              AND (:segment  IS NULL OR LOWER(jc.segment) LIKE :segmentPattern)
            ORDER BY jc.createdAt DESC
            """)
    List<JobCustomer> findAllByPartyCodeFiltered(
            @Param("partyCode")      String partyCode,
            @Param("fromDate")       LocalDateTime fromDate,
            @Param("toDate")         LocalDateTime toDate,
            @Param("segment")        String segment,
            @Param("segmentPattern") String segmentPattern
    );

    Optional<JobCustomer> findBySesMessageId(String sesMessageId);

    long countByJob_JobId(UUID jobId);

    long countByJob_JobIdAndEmailStatus(UUID jobId, JobCustomer.EmailStatus status);

    long countByJob_JobIdAndPdfStatus(UUID jobId, JobCustomer.PdfStatus status);

    /** For bulk resend: all customers with failed PDF or failed/bounced email */
    @Query("""
            SELECT jc FROM JobCustomer jc
            WHERE jc.job.jobId = :jobId
              AND (jc.pdfStatus   = :pdfFailed
               OR  jc.emailStatus = :emailFailed
               OR  jc.emailStatus = :emailBounced)
            """)
    List<JobCustomer> findAllFailedByJobIdRaw(
            @Param("jobId")        UUID jobId,
            @Param("pdfFailed")    JobCustomer.PdfStatus pdfFailed,
            @Param("emailFailed")  JobCustomer.EmailStatus emailFailed,
            @Param("emailBounced") JobCustomer.EmailStatus emailBounced);

    /** Convenience wrapper — keeps call sites clean */
    default List<JobCustomer> findAllFailedByJobId(UUID jobId) {
        return findAllFailedByJobIdRaw(jobId,
                JobCustomer.PdfStatus.FAILED,
                JobCustomer.EmailStatus.FAILED,
                JobCustomer.EmailStatus.BOUNCED);
    }

    /** All BOUNCED customers across all jobs (for global "retry all bounced" operation) */
    @Query("SELECT jc FROM JobCustomer jc WHERE jc.emailStatus = :bounced")
    List<JobCustomer> findAllBounced(@Param("bounced") JobCustomer.EmailStatus bounced);

    default List<JobCustomer> findAllBounced() {
        return findAllBounced(JobCustomer.EmailStatus.BOUNCED);
    }

    /** Count emails actually dispatched in the last 24 hours (from our own records) */
    @Query("SELECT COUNT(jc) FROM JobCustomer jc WHERE jc.emailSentAt >= :since AND jc.emailStatus IN ('SENT','DELIVERED','BOUNCED')")
    long countEmailsSentSince(@Param("since") LocalDateTime since);

    /** All customers for a specific job matching any of the given party codes */
    @Query("SELECT jc FROM JobCustomer jc WHERE jc.job.jobId = :jobId AND jc.partyCode IN :partyCodes")
    List<JobCustomer> findByJobIdAndPartyCodes(@Param("jobId") UUID jobId, @Param("partyCodes") List<String> partyCodes);

    /** Exceptions: PDF failed or email failed/bounced/skipped — server-side paginated */
    @Query("""
            SELECT jc FROM JobCustomer jc WHERE jc.job.jobId = :jobId
              AND (jc.pdfStatus = 'FAILED'
               OR  jc.emailStatus IN ('FAILED','BOUNCED','SKIPPED'))
            """)
    Page<JobCustomer> findAllExceptionsByJobId(@Param("jobId") UUID jobId, Pageable pageable);

    @Query("SELECT jc FROM JobCustomer jc WHERE jc.job.jobId = :jobId AND jc.pdfStatus = 'FAILED'")
    Page<JobCustomer> findPdfFailedByJobId(@Param("jobId") UUID jobId, Pageable pageable);

    @Query("SELECT jc FROM JobCustomer jc WHERE jc.job.jobId = :jobId AND jc.emailStatus = 'FAILED'")
    Page<JobCustomer> findEmailFailedByJobId(@Param("jobId") UUID jobId, Pageable pageable);

    @Query("SELECT jc FROM JobCustomer jc WHERE jc.job.jobId = :jobId AND jc.emailStatus = 'BOUNCED'")
    Page<JobCustomer> findBouncedByJobId(@Param("jobId") UUID jobId, Pageable pageable);

    @Query("SELECT jc FROM JobCustomer jc WHERE jc.job.jobId = :jobId AND jc.emailStatus = 'SKIPPED'")
    Page<JobCustomer> findSkippedByJobId(@Param("jobId") UUID jobId, Pageable pageable);
}
