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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository         jobRepository;
    private final JobCustomerRepository jobCustomerRepository;
    private final S3Service             s3Service;
    private final AppProperties         appProperties;

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

    private Job findJobById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "jobId", jobId));
    }
}
