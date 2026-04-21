package com.geojit.contractnote.service;

import com.geojit.contractnote.entity.Job;
import com.geojit.contractnote.entity.JobCustomer;
import com.geojit.contractnote.repository.JobCustomerRepository;
import com.geojit.contractnote.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Auto-completes jobs that are stuck in PROCESSING or EMAILING state.
 * A job is considered stuck if it has not moved to a terminal state
 * within 15 minutes of being uploaded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobTimeoutService {

    private final JobRepository        jobRepository;
    private final JobCustomerRepository jobCustomerRepository;

    /** Run every 2 minutes, auto-complete any job stuck for more than 15 minutes. */
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void autoCompleteStuckJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Job> stuckJobs = jobRepository.findStuckJobs(cutoff);

        for (Job job : stuckJobs) {
            UUID jobId = job.getJobId();
            int total = job.getTotalCustomers();
            if (total <= 0) continue;

            long registered   = jobCustomerRepository.countByJob_JobId(jobId);
            long unregistered = Math.max(0, total - registered);
            long delivered    = jobCustomerRepository.countByJob_JobIdAndEmailStatus(jobId, JobCustomer.EmailStatus.DELIVERED);
            long bounced      = jobCustomerRepository.countByJob_JobIdAndEmailStatus(jobId, JobCustomer.EmailStatus.BOUNCED);
            long emailFailed  = jobCustomerRepository.countByJob_JobIdAndEmailStatus(jobId, JobCustomer.EmailStatus.FAILED);
            long pdfFailed    = jobCustomerRepository.countByJob_JobIdAndPdfStatus(jobId, JobCustomer.PdfStatus.FAILED);
            long invalidCount = pdfFailed + unregistered;

            job.setFailedCount((int) invalidCount);
            job.setEmailFailedCount((int) emailFailed);

            if (invalidCount > 0 || bounced > 0 || emailFailed > 0 || delivered < (registered - pdfFailed)) {
                job.setStatus(Job.JobStatus.PARTIAL);
            } else {
                job.setStatus(Job.JobStatus.COMPLETED);
            }

            jobRepository.save(job);
            log.info("Auto-completed stuck job | jobId={} | status={} | total={} | delivered={} | bounced={} | emailFailed={} | invalid={}",
                    jobId, job.getStatus(), total, delivered, bounced, emailFailed, invalidCount);
        }
    }
}
