package com.geojit.contractnote.service;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.geojit.contractnote.config.AppProperties;
import com.geojit.contractnote.dto.response.*;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.exception.ResourceNotFoundException;
import com.geojit.contractnote.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository             jobRepository;
    private final JobCustomerRepository     jobCustomerRepository;
    private final PipelineEventRepository   pipelineEventRepository;
    private final S3Service                 s3Service;
    private final AppProperties             appProperties;

    @Transactional
    public JobResponse uploadAndCreateJob(MultipartFile file, String segmentType,
                                          String tradeDate, User user) {
        // 1. Generate jobId
        UUID jobId = UUID.randomUUID();

        // 2. Upload to S3 raw bucket with jobId embedded in metadata
        String s3Key = s3Service.uploadRawFile(file, jobId.toString());

        // 3. Create job record in PostgreSQL
        Job job = Job.builder()
                .jobId(jobId)
                .fileName(file.getOriginalFilename())
                .rawS3Key(s3Key)
                .uploadedBy(user)
                .uploadedAt(LocalDateTime.now())
                .status(Job.JobStatus.SPLITTING)
                .segmentType(segmentType)
                .build();
        job = jobRepository.save(job);

        log.info("Job created | jobId={} | file={} | segment={}", jobId, file.getOriginalFilename(), segmentType);
        // S3 put event will automatically trigger split-lambda-geojit
        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(Pageable pageable) {
        return jobRepository.findAllByOrderByUploadedAtDesc(pageable)
                .map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByStatus(Job.JobStatus status, Pageable pageable) {
        return jobRepository.findByStatusOrderByUploadedAtDesc(status, pageable)
                .map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByDateRange(java.time.LocalDate from, java.time.LocalDate to, Job.JobStatus status, Pageable pageable) {
        java.time.LocalDateTime start = from.atStartOfDay();
        java.time.LocalDateTime end = to.plusDays(1).atStartOfDay();
        if (status != null) {
            return jobRepository.findPageByCreatedAtBetweenAndStatus(start, end, status, pageable).map(JobResponse::from);
        }
        return jobRepository.findPageByCreatedAtBetween(start, end, pageable).map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID jobId) {
        Job job = findJobById(jobId);
        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public Page<JobCustomerResponse> getJobCustomers(UUID jobId, Pageable pageable) {
        return jobCustomerRepository.findByJob_JobId(jobId, pageable)
                .map(JobCustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<JobCustomerResponse> getExceptions(UUID jobId, String type, Pageable pageable) {
        return switch (type.toUpperCase()) {
            case "PDF"     -> jobCustomerRepository.findPdfFailedByJobId(jobId, pageable).map(JobCustomerResponse::from);
            case "EMAIL"   -> jobCustomerRepository.findEmailFailedByJobId(jobId, pageable).map(JobCustomerResponse::from);
            case "BOUNCE"  -> jobCustomerRepository.findBouncedByJobId(jobId, pageable).map(JobCustomerResponse::from);
            case "SKIPPED" -> jobCustomerRepository.findSkippedByJobId(jobId, pageable).map(JobCustomerResponse::from);
            default        -> jobCustomerRepository.findAllExceptionsByJobId(jobId, pageable).map(JobCustomerResponse::from);
        };
    }

    public Page<JobCustomerResponse> getFailedCustomers(UUID jobId, Pageable pageable) {
        return jobCustomerRepository.findByJob_JobId(jobId, pageable)
                .map(JobCustomerResponse::from)
                .map(r -> r);
    }

    @Transactional
    public JobResponse updateJobStatus(UUID jobId, Job.JobStatus status) {
        Job job = findJobById(jobId);
        job.setStatus(status);
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public PipelineStatsResponse getPipelineStats(UUID jobId) {
        Job job = findJobById(jobId);

        // Throughput: events in the last 60 seconds
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(60);
        List<Object[]> recentEvents = pipelineEventRepository.countRecentEventsByType(jobId, since);
        Map<String, Long> rates = recentEvents.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // Latency percentiles (null-safe: returns zeros if no emails sent yet)
        Object[] latency = jobCustomerRepository.calculateLatencyPercentiles(jobId);
        double median = latency != null && latency[0] != null ? ((Number) latency[0]).doubleValue() : 0.0;
        double p95    = latency != null && latency[1] != null ? ((Number) latency[1]).doubleValue() : 0.0;

        long total      = Math.max(job.getTotalCustomers(), 1); // avoid divide-by-zero
        long pdfFailed  = job.getFailedCount();
        long emailFailed = job.getEmailFailedCount();

        return PipelineStatsResponse.builder()
                .jobId(job.getJobId())
                .fileName(job.getFileName())
                .status(job.getStatus().name())
                .uploadCount(job.getTotalCustomers())
                .splitCount(job.getProcessedCount())
                .pdfCount(job.getPdfGeneratedCount())
                .pdfFailed(pdfFailed)
                .pdfPending(Math.max(0, job.getTotalCustomers() - job.getPdfGeneratedCount() - pdfFailed))
                .pdfRate(rates.getOrDefault("PDF_GENERATED", 0L))
                .emailCount(job.getEmailSentCount())
                .emailBounced(job.getEmailBouncedCount())
                .emailFailed(emailFailed)
                .emailPending(Math.max(0, job.getPdfGeneratedCount() - job.getEmailSentCount() - emailFailed))
                .emailRate(rates.getOrDefault("EMAIL_SENT", 0L))
                .deliveredCount(job.getEmailDeliveredCount())
                .deliveredRate(rates.getOrDefault("DELIVERY", 0L))
                .medianSeconds(median)
                .p95Seconds(p95)
                .errorRate(Math.round((double)(pdfFailed + emailFailed) / total * 10000.0) / 100.0)
                .build();
    }

    @Transactional(readOnly = true)
    public ExceptionCountsResponse getExceptionCounts(UUID jobId) {
        findJobById(jobId); // validate existence
        return ExceptionCountsResponse.builder()
                .pdfFailed(jobCustomerRepository.countByJob_JobIdAndPdfStatus(jobId, JobCustomer.PdfStatus.FAILED))
                .emailFailed(jobCustomerRepository.countByJob_JobIdAndEmailStatus(jobId, JobCustomer.EmailStatus.FAILED))
                .bounced(jobCustomerRepository.countByJob_JobIdAndEmailStatus(jobId, JobCustomer.EmailStatus.BOUNCED))
                .skipped(jobCustomerRepository.countByJob_JobIdAndEmailStatus(jobId, JobCustomer.EmailStatus.SKIPPED))
                .build();
    }

    private Job findJobById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "jobId", jobId));
    }
}
