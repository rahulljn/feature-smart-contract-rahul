package com.geojit.contractnote.service;

import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineEventRepository pipelineEventRepository;
    private final JobCustomerRepository   jobCustomerRepository;
    private final JobRepository           jobRepository;
    private final EmailEventRepository    emailEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<PipelineEvent> getJobEvents(UUID jobId) {
        return pipelineEventRepository.findByJobIdOrderByEventTimestampAsc(jobId);
    }

    @Transactional(readOnly = true)
    public Page<PipelineEvent> getJobEventsPage(UUID jobId, Pageable pageable) {
        return pipelineEventRepository.findByJobId(jobId, pageable);
    }

    /** Called by the Status Consumer Lambda webhook — upserts DB from SQS events */
    @Transactional
    public void processStatusEvent(PipelineEvent event) {

        // ── SPLIT_PROGRESS: broadcast SSE only, no DB write (too many rows) ──
        if (event.getEventType() == PipelineEvent.EventType.SPLIT_PROGRESS) {
            if (event.getJobId() != null) {
                eventPublisher.publishEvent(new PipelineStatusAppEvent(event.getJobId(), event));
            }
            return;
        }

        pipelineEventRepository.save(event);

        if (event.getJobId() == null) return;

        // ── JOB_REGISTERED: Update job totals from Split Lambda ──
        if (event.getEventType() == PipelineEvent.EventType.JOB_REGISTERED) {
            jobRepository.findById(event.getJobId()).ifPresent(job -> {
                if (event.getPayload() != null) {
                    Object total = event.getPayload().get("totalCustomers");
                    if (total != null) {
                        try { job.setTotalCustomers(Integer.parseInt(total.toString())); } catch (NumberFormatException ignored) {}
                    }
                    // Store invalid customer count from Split Lambda validation
                    Object invalid = event.getPayload().get("invalidCustomers");
                    if (invalid != null) {
                        try {
                            int invalidCount = Integer.parseInt(invalid.toString());
                            if (invalidCount > 0) job.setInvalidRecordCount(invalidCount);
                        } catch (NumberFormatException ignored) {}
                    }
                    Object fileName = event.getPayload().get("fileName");
                    if (fileName != null && (job.getFileName() == null || job.getFileName().isBlank())) {
                        job.setFileName(fileName.toString());
                    }
                    // Set tradeDate from JOB_REGISTERED payload when Lambda includes it
                    if (job.getTradeDate() == null) {
                        Object td = event.getPayload().get("tradeDate");
                        if (td != null && !td.toString().isBlank()) {
                            LocalDate parsed = parseTradeDateSafely(td.toString());
                            if (parsed != null) job.setTradeDate(parsed);
                        }
                    }
                }
                job.setStatus(Job.JobStatus.PROCESSING);
                jobRepository.save(job);
            });
            eventPublisher.publishEvent(new PipelineStatusAppEvent(event.getJobId(), event));
            return;
        }

        // ── CUSTOMER_REGISTERED: Create JobCustomer row ──
        if (event.getEventType() == PipelineEvent.EventType.CUSTOMER_REGISTERED) {
            if (event.getPartyCode() != null) {
                String normCode = normaliseCode(event.getPartyCode());
                if (jobCustomerRepository.findByJob_JobIdAndPartyCode(event.getJobId(), normCode).isEmpty()) {
                    jobRepository.findById(event.getJobId()).ifPresent(job -> {
                        JobCustomer jc = JobCustomer.builder()
                                .job(job)
                                .partyCode(normCode)
                                .build();
                        if (event.getPayload() != null) {
                            jc.setEmail(event.getPayload().getOrDefault("email", "").toString());
                            jc.setTradeDate(event.getPayload().getOrDefault("tradeDate", "").toString());
                            jc.setContractNoteNo(event.getPayload().getOrDefault("contractNoteNo", "").toString());
                            jc.setSegment(event.getPayload().getOrDefault("segment", "").toString());
                            // Set job tradeDate from first CUSTOMER_REGISTERED if not yet populated
                            if (job.getTradeDate() == null) {
                                Object td = event.getPayload().get("tradeDate");
                                if (td != null && !td.toString().isBlank()) {
                                    LocalDate parsed = parseTradeDateSafely(td.toString());
                                    if (parsed != null) {
                                        job.setTradeDate(parsed);
                                        jobRepository.save(job);
                                    }
                                }
                            }
                        }
                        jobCustomerRepository.save(jc);
                        // Atomic increment
                        jobRepository.incrementProcessedCount(event.getJobId());
                    });
                }
            }
            eventPublisher.publishEvent(new PipelineStatusAppEvent(event.getJobId(), event));
            return;
        }

        // ── Existing events: PDF_GENERATED, PDF_FAILED, EMAIL_SENT, etc. ──
        if (event.getPartyCode() == null) return;

        jobCustomerRepository.findByJob_JobIdAndPartyCode(event.getJobId(), normaliseCode(event.getPartyCode()))
                .ifPresent(jc -> {
                    switch (event.getEventType()) {
                        case PDF_GENERATED -> {
                            if (jc.getPdfStatus() != JobCustomer.PdfStatus.GENERATED) {
                                jc.setPdfStatus(JobCustomer.PdfStatus.GENERATED);
                                jc.setPdfGeneratedAt(event.getEventTimestamp());
                                Object key = event.getPayload() != null ? event.getPayload().get("s3Key") : null;
                                if (key != null) jc.setPdfS3Key(key.toString());
                                jobRepository.incrementPdfGeneratedCount(event.getJobId());
                            }
                        }
                        case PDF_FAILED -> {
                            if (jc.getPdfStatus() != JobCustomer.PdfStatus.FAILED) {
                                jc.setPdfStatus(JobCustomer.PdfStatus.FAILED);
                                jobRepository.incrementFailedCount(event.getJobId());
                                jobRepository.incrementPdfFailedCount(event.getJobId());
                            }
                        }
                        case EMAIL_SENT -> {
                            // Always capture sesMessageId if not yet recorded
                            Object msgId = event.getPayload() != null ? event.getPayload().get("sesMessageId") : null;
                            if (msgId != null && jc.getSesMessageId() == null) {
                                jc.setSesMessageId(msgId.toString());
                            }
                            // Advance to SENT only if still PENDING — never regress from DELIVERED/BOUNCED
                            if (jc.getEmailStatus() == JobCustomer.EmailStatus.PENDING) {
                                jc.setEmailStatus(JobCustomer.EmailStatus.SENT);
                            }
                            // Count once, guarded by emailSentAt. When SES events (DELIVERY/BOUNCE)
                            // arrive before this Lambda event, emailSentAt is still null even though
                            // emailStatus is already advanced — so the counter still gets incremented.
                            if (jc.getEmailSentAt() == null) {
                                jc.setEmailSentAt(event.getEventTimestamp());
                                jobRepository.incrementEmailSentCount(event.getJobId());
                            }
                        }
                        case EMAIL_FAILED -> {
                            if (jc.getEmailStatus() != JobCustomer.EmailStatus.FAILED) {
                                jc.setEmailStatus(JobCustomer.EmailStatus.FAILED);
                                // Persist the Lambda error message so the Exceptions UI can display the root cause
                                Object err = event.getPayload() != null ? event.getPayload().get("error") : null;
                                if (err != null && !err.toString().isBlank()) jc.setBounceReason(err.toString());
                                jobRepository.incrementEmailFailedCount(event.getJobId());
                            }
                        }
                        case EMAIL_SKIPPED -> {
                            if (jc.getEmailStatus() != JobCustomer.EmailStatus.SKIPPED) {
                                jc.setEmailStatus(JobCustomer.EmailStatus.SKIPPED);
                                jobRepository.incrementEmailSkippedCount(event.getJobId());
                            }
                        }
                        case DELIVERY -> {
                            // Idempotency: only process if not already DELIVERED (SNS may retry)
                            if (jc.getEmailStatus() != JobCustomer.EmailStatus.DELIVERED) {
                                jc.setEmailStatus(JobCustomer.EmailStatus.DELIVERED);
                                jc.setDeliveredAt(event.getEventTimestamp());
                                jobRepository.incrementEmailDeliveredCount(event.getJobId());
                                emailEventRepository.save(EmailEvent.builder()
                                        .sesMessageId(jc.getSesMessageId())
                                        .partyCode(event.getPartyCode())
                                        .jobId(event.getJobId())
                                        .recipientEmail(jc.getEmail())
                                        .eventType(EmailEvent.EventType.DELIVERY)
                                        .eventTimestamp(event.getEventTimestamp() != null
                                                ? event.getEventTimestamp() : LocalDateTime.now())
                                        .build());
                            } else {
                                log.debug("Duplicate DELIVERY event ignored | partyCode={}", jc.getPartyCode());
                            }
                        }
                        case BOUNCE -> {
                            // Idempotency: skip if already BOUNCED; never regress from DELIVERED
                            if (jc.getEmailStatus() != JobCustomer.EmailStatus.BOUNCED
                                    && jc.getEmailStatus() != JobCustomer.EmailStatus.DELIVERED) {
                                jc.setEmailStatus(JobCustomer.EmailStatus.BOUNCED);
                                jc.setBouncedAt(event.getEventTimestamp());
                                Object bt = event.getPayload() != null ? event.getPayload().get("bounceType") : null;
                                Object br = event.getPayload() != null ? event.getPayload().get("bounceSubType") : null;
                                if (bt != null) jc.setBounceType(bt.toString());
                                if (br != null) jc.setBounceReason(br.toString());
                                jobRepository.incrementEmailBouncedCount(event.getJobId());
                                String bounceTypeStr = bt != null ? bt.toString().toLowerCase() : "";
                                if (bounceTypeStr.contains("permanent")) {
                                    jobRepository.incrementHardBounceCount(event.getJobId());
                                } else if (bounceTypeStr.contains("transient")) {
                                    jobRepository.incrementSoftBounceCount(event.getJobId());
                                }
                                emailEventRepository.save(EmailEvent.builder()
                                        .sesMessageId(jc.getSesMessageId())
                                        .partyCode(event.getPartyCode())
                                        .jobId(event.getJobId())
                                        .recipientEmail(jc.getEmail())
                                        .eventType(EmailEvent.EventType.BOUNCE)
                                        .bounceType(bt != null ? bt.toString() : null)
                                        .bounceSubType(br != null ? br.toString() : null)
                                        .eventTimestamp(event.getEventTimestamp() != null
                                                ? event.getEventTimestamp() : LocalDateTime.now())
                                        .build());
                            } else {
                                log.debug("BOUNCE event skipped — current status={} | partyCode={}", jc.getEmailStatus(), jc.getPartyCode());
                            }
                        }
                        case COMPLAINT -> {
                            // Same guard: never regress from DELIVERED; skip duplicate BOUNCED
                            if (jc.getEmailStatus() != JobCustomer.EmailStatus.BOUNCED
                                    && jc.getEmailStatus() != JobCustomer.EmailStatus.DELIVERED) {
                                jc.setEmailStatus(JobCustomer.EmailStatus.BOUNCED);
                                jc.setBouncedAt(event.getEventTimestamp());
                                jobRepository.incrementEmailBouncedCount(event.getJobId());
                                jobRepository.incrementHardBounceCount(event.getJobId()); // complaints = permanent hard bounces
                                emailEventRepository.save(EmailEvent.builder()
                                        .sesMessageId(jc.getSesMessageId())
                                        .partyCode(event.getPartyCode())
                                        .jobId(event.getJobId())
                                        .recipientEmail(jc.getEmail())
                                        .eventType(EmailEvent.EventType.COMPLAINT)
                                        .eventTimestamp(event.getEventTimestamp() != null
                                                ? event.getEventTimestamp() : LocalDateTime.now())
                                        .build());
                            } else {
                                log.debug("COMPLAINT event skipped — current status={} | partyCode={}", jc.getEmailStatus(), jc.getPartyCode());
                            }
                        }
                        default -> {}
                    }
                    jobCustomerRepository.save(jc);

                    // Check completion after every event to avoid stuck jobs
                    checkAndCompleteJob(event.getJobId());

                    // Publish for SSE broadcast
                    eventPublisher.publishEvent(new PipelineStatusAppEvent(event.getJobId(), event));
                });
    }

    private void checkAndCompleteJob(UUID jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            int total = job.getTotalCustomers();
            // JOB_REGISTERED payload may omit totalCustomers — fall back to CUSTOMER_REGISTERED count
            if (total <= 0) total = (int) job.getProcessedCount();
            if (total <= 0) return;

            long pdfFailed    = job.getFailedCount();          // PDF_FAILED events
            long emailSent    = job.getEmailSentCount();       // EMAIL_SENT events
            long emailFailed  = job.getEmailFailedCount();     // EMAIL_FAILED events
            long emailSkipped = job.getEmailSkippedCount();    // EMAIL_SKIPPED (suppression list) events
            long done         = emailSent + emailFailed + emailSkipped + pdfFailed;

            // PROCESSING → EMAILING
            if (job.getStatus() == Job.JobStatus.PROCESSING) {
                long pdfDone = job.getPdfGeneratedCount() + pdfFailed;
                if (pdfDone >= total) {
                    if (job.getTotalCustomers() <= 0) job.setTotalCustomers(total);
                    job.setStatus(Job.JobStatus.EMAILING);
                    jobRepository.save(job);
                    log.info("Job transitioned to EMAILING | jobId={} | pdfDone={}/{}", jobId, pdfDone, total);
                }
            }

            if (done >= total) {
                if (job.getTotalCustomers() <= 0) job.setTotalCustomers(total);
                if (pdfFailed > 0 || emailFailed > 0 || job.getEmailBouncedCount() > 0) {
                    job.setStatus(Job.JobStatus.PARTIAL);
                } else {
                    job.setStatus(Job.JobStatus.COMPLETED);
                }
                jobRepository.save(job);
                log.info("Job completed | jobId={} | status={} | done={}/{} | pdfFailed={} | emailFailed={} | bounced={}",
                        jobId, job.getStatus(), done, total, pdfFailed, emailFailed, job.getEmailBouncedCount());
            }
        });
    }

    /**
     * Tries common date formats used in Indian financial raw files.
     * Returns null and logs a warning if none match.
     */
    private LocalDate parseTradeDateSafely(String raw) {
        String[] patterns = { "yyyyMMdd", "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy" };
        for (String pat : patterns) {
            try {
                return LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern(pat));
            } catch (Exception ignored) {}
        }
        log.warn("Could not parse tradeDate value: '{}'", raw);
        return null;
    }

    /** Strip suffix from partyCode — Lambda sends "ZYR175/ZYR175", we store "ZYR175". */
    private String normaliseCode(String code) {
        if (code == null) return null;
        int slash = code.indexOf('/');
        return slash > 0 ? code.substring(0, slash) : code;
    }

    /** Spring event published after each pipeline status event is processed. */
    public record PipelineStatusAppEvent(UUID jobId, PipelineEvent event) {}
}
