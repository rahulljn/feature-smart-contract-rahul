package com.geojit.contractnote.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.exception.ResourceNotFoundException;
import com.geojit.contractnote.exception.ValidationException;
import com.geojit.contractnote.repository.*;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Handles re-sending contract note emails to individual customers.
 *
 * Two resend paths:
 *  A. Email-only (pdfS3Key present): PDF already exists in S3 — download it and
 *     send directly via SES without going through the Lambda pipeline.
 *  B. Full re-process (no pdfS3Key): reset both statuses and push to
 *     geojit-map-split-processing-queue.fifo — Invoke Lambda re-creates PDF + email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResendService {

    private final JobRepository              jobRepository;
    private final JobCustomerRepository      jobCustomerRepository;
    private final SuppressionListRepository  suppressionListRepository;
    private final PipelineEventRepository    pipelineEventRepository;
    private final SesConfigRepository        sesConfigRepository;
    private final EmailTemplateRepository    emailTemplateRepository;
    private final SqsService                 sqsService;
    private final S3Service                  s3Service;
    private final AmazonSimpleEmailService   ses;
    private final AuditService               auditService;

    /**
     * Resend for a single customer within a job.
     *
     * @param jobId         the job UUID
     * @param partyCode     customer party code
     * @param overrideEmail optional alternate email to send to (if null uses customer's registered email)
     * @param templateId    optional template UUID override (if null uses active template)
     * @param requestedBy   authenticated user triggering the resend
     * @param httpRequest   servlet request for audit IP
     */
    @Transactional
    public void resendForCustomer(UUID jobId, String partyCode, String overrideEmail, UUID templateId,
                                  User requestedBy, jakarta.servlet.http.HttpServletRequest httpRequest) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "jobId", jobId));

        if (!isResendableStatus(job.getStatus())) {
            throw new ValidationException(
                    "Resend not allowed for job in status: " + job.getStatus() +
                    ". Job must be COMPLETED or PARTIAL.");
        }

        JobCustomer customer = jobCustomerRepository
                .findByJob_JobIdAndPartyCode(jobId, partyCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "JobCustomer", "jobId+partyCode", jobId + "/" + partyCode));

        String effectiveEmail = (overrideEmail != null && !overrideEmail.isBlank()) ? overrideEmail : customer.getEmail();

        if (suppressionListRepository.existsByEmailIgnoreCase(effectiveEmail)) {
            throw new ValidationException(
                    "Cannot resend — email is in the suppression list: " + effectiveEmail);
        }

        String messageId;

        if (customer.getPdfS3Key() != null && !customer.getPdfS3Key().isBlank()) {
            // ── Path A: PDF already generated — email-only resend via SES directly ──
            log.info("Resend | email-only path | jobId={} | partyCode={} | to={}", jobId, partyCode, effectiveEmail);
            customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
            customer.setSesMessageId(null);
            // pdfStatus stays GENERATED — we're reusing the existing PDF
            jobCustomerRepository.save(customer);

            messageId = sendEmailDirectly(job, customer, effectiveEmail, templateId);

            customer.setSesMessageId(messageId);
            customer.setEmailStatus(JobCustomer.EmailStatus.SENT);
            jobCustomerRepository.save(customer);
        } else {
            // ── Path B: No PDF — full re-process via Lambda pipeline ──
            log.info("Resend | full-reprocess path | jobId={} | partyCode={}", jobId, partyCode);
            customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
            customer.setPdfStatus(JobCustomer.PdfStatus.PENDING);
            customer.setSesMessageId(null);
            jobCustomerRepository.save(customer);

            messageId = sqsService.sendResendTrigger(
                    jobId.toString(), partyCode, requestedBy.getEmail());
        }

        pipelineEventRepository.save(PipelineEvent.builder()
                .jobId(jobId)
                .partyCode(partyCode)
                .eventType(PipelineEvent.EventType.RESEND_TRIGGERED)
                .payload(java.util.Map.of(
                        "messageId",     messageId,
                        "requestedBy",   requestedBy.getEmail(),
                        "path",          customer.getPdfS3Key() != null ? "EMAIL_ONLY" : "FULL_REPROCESS",
                        "overrideEmail", overrideEmail != null ? overrideEmail : ""
                ))
                .build());

        auditService.log(requestedBy, AuditLog.AuditAction.RESEND,
                "job_customers", java.util.Map.of(
                        "jobId",     jobId.toString(),
                        "partyCode", partyCode,
                        "messageId", messageId
                ), httpRequest);

        log.info("Resend complete | jobId={} | partyCode={} | by={} | messageId={}",
                jobId, partyCode, requestedBy.getEmail(), messageId);
    }

    /**
     * Bulk resend — re-queue all failed/bounced customers for a job.
     * Uses the same two-path logic as single resend.
     */
    @Transactional
    public int bulkResendFailed(UUID jobId, User requestedBy,
                                jakarta.servlet.http.HttpServletRequest httpRequest,
                                UUID templateId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "jobId", jobId));

        if (!isResendableStatus(job.getStatus())) {
            throw new ValidationException("Bulk resend not allowed for job in status: " + job.getStatus());
        }

        List<JobCustomer> failed = jobCustomerRepository.findAllFailedByJobId(jobId);
        if (failed.isEmpty()) {
            log.info("Bulk resend | jobId={} | no failed customers", jobId);
            return 0;
        }

        int queued = 0;
        for (JobCustomer customer : failed) {
            if (suppressionListRepository.existsByEmailIgnoreCase(customer.getEmail())) {
                log.warn("Bulk resend | skipping suppressed | email={}", customer.getEmail());
                continue;
            }

            try {
                if (customer.getPdfS3Key() != null && !customer.getPdfS3Key().isBlank()) {
                    // Path A: email-only
                    customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
                    customer.setSesMessageId(null);
                    jobCustomerRepository.save(customer);

                    String msgId = sendEmailDirectly(job, customer, customer.getEmail(), templateId);
                    customer.setSesMessageId(msgId);
                    customer.setEmailStatus(JobCustomer.EmailStatus.SENT);
                    jobCustomerRepository.save(customer);
                } else {
                    // Path B: full re-process
                    customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
                    customer.setPdfStatus(JobCustomer.PdfStatus.PENDING);
                    customer.setSesMessageId(null);
                    jobCustomerRepository.save(customer);

                    sqsService.sendResendTrigger(jobId.toString(), customer.getPartyCode(), requestedBy.getEmail());
                }
                queued++;
            } catch (Exception e) {
                log.error("Bulk resend failed for partyCode={} | error={}", customer.getPartyCode(), e.getMessage());
            }
        }

        auditService.log(requestedBy, AuditLog.AuditAction.BULK_RESEND,
                "jobs", java.util.Map.of(
                        "jobId",       jobId.toString(),
                        "queued",      queued,
                        "totalFailed", failed.size()
                ), httpRequest);

        log.info("Bulk resend complete | jobId={} | queued={}/{}", jobId, queued, failed.size());
        return queued;
    }

    /**
     * Bulk resend for a specific list of party codes within a job.
     */
    @Transactional
    public int bulkResendByPartyCodes(UUID jobId, List<String> partyCodes, User requestedBy,
                                      jakarta.servlet.http.HttpServletRequest httpRequest,
                                      UUID templateId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "jobId", jobId));

        List<JobCustomer> customers = jobCustomerRepository.findByJobIdAndPartyCodes(jobId, partyCodes);
        int queued = 0;
        for (JobCustomer customer : customers) {
            if (suppressionListRepository.existsByEmailIgnoreCase(customer.getEmail())) continue;
            try {
                if (customer.getPdfS3Key() != null && !customer.getPdfS3Key().isBlank()) {
                    customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
                    customer.setSesMessageId(null);
                    jobCustomerRepository.save(customer);
                    String msgId = sendEmailDirectly(job, customer, customer.getEmail(), templateId);
                    customer.setSesMessageId(msgId);
                    customer.setEmailStatus(JobCustomer.EmailStatus.SENT);
                    jobCustomerRepository.save(customer);
                } else {
                    customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
                    customer.setPdfStatus(JobCustomer.PdfStatus.PENDING);
                    customer.setSesMessageId(null);
                    jobCustomerRepository.save(customer);
                    sqsService.sendResendTrigger(jobId.toString(), customer.getPartyCode(), requestedBy.getEmail());
                }
                queued++;
            } catch (Exception e) {
                log.error("Bulk resend (by codes) failed for partyCode={} | {}", customer.getPartyCode(), e.getMessage());
            }
        }
        auditService.log(requestedBy, AuditLog.AuditAction.BULK_RESEND, "jobs",
                java.util.Map.of("jobId", jobId.toString(), "queued", queued, "scope", "BY_PARTY_CODES"), httpRequest);
        return queued;
    }

    /**
     * Resend all BOUNCED customers across all jobs (global retry after suppression fix).
     */
    @Transactional
    public int bulkResendAllBounced(User requestedBy, jakarta.servlet.http.HttpServletRequest httpRequest,
                                    UUID templateId) {
        List<JobCustomer> allBounced = jobCustomerRepository.findAllBounced();
        int queued = 0;
        for (JobCustomer customer : allBounced) {
            if (suppressionListRepository.existsByEmailIgnoreCase(customer.getEmail())) continue;
            try {
                Job job = jobRepository.findById(customer.getJob().getJobId()).orElse(null);
                if (job == null) continue;
                if (customer.getPdfS3Key() != null && !customer.getPdfS3Key().isBlank()) {
                    customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
                    customer.setSesMessageId(null);
                    jobCustomerRepository.save(customer);
                    String msgId = sendEmailDirectly(job, customer, customer.getEmail(), templateId);
                    customer.setSesMessageId(msgId);
                    customer.setEmailStatus(JobCustomer.EmailStatus.SENT);
                    jobCustomerRepository.save(customer);
                } else {
                    customer.setEmailStatus(JobCustomer.EmailStatus.PENDING);
                    customer.setPdfStatus(JobCustomer.PdfStatus.PENDING);
                    customer.setSesMessageId(null);
                    jobCustomerRepository.save(customer);
                    sqsService.sendResendTrigger(job.getJobId().toString(), customer.getPartyCode(), requestedBy.getEmail());
                }
                queued++;
            } catch (Exception e) {
                log.error("Bulk resend (all bounced) failed for partyCode={} | {}", customer.getPartyCode(), e.getMessage());
            }
        }
        auditService.log(requestedBy, AuditLog.AuditAction.BULK_RESEND, "global",
                java.util.Map.of("queued", queued, "scope", "ALL_BOUNCED"), httpRequest);
        return queued;
    }

    // ─── private helpers ─────────────────────────────────────────────────

    /**
     * Download the existing PDF from S3 and send it via SES directly.
     * Replicates the Lambda's AmazonSES.sendMailWithHeader() MIME pattern.
     *
     * @param toEmail    destination email (may be an override, not necessarily customer's registered email)
     * @param templateId optional template ID override; if null uses active template
     */
    private String sendEmailDirectly(Job job, JobCustomer customer, String toEmail, UUID templateId) {
        // Resolve active SES config
        List<SesConfig> activeConfigs = sesConfigRepository.findByIsActiveTrueOrderByConfigSetNameAsc();
        String configSetName = activeConfigs.isEmpty() ? "geojit-config-set"
                : activeConfigs.get(0).getConfigSetName();
        String fromEmail = activeConfigs.isEmpty() ? "noreply@geojit.co.in"
                : activeConfigs.get(0).getFromEmail();
        String fromName  = activeConfigs.isEmpty() ? "GEOJIT"
                : (activeConfigs.get(0).getFromName() != null ? activeConfigs.get(0).getFromName() : "GEOJIT");

        // Resolve email template (specific ID override or active template)
        Optional<EmailTemplate> tmplOpt = (templateId != null)
                ? emailTemplateRepository.findById(templateId)
                : emailTemplateRepository.findByIsActiveTrue();
        String subject  = tmplOpt.map(EmailTemplate::getSubject).orElse("Your Contract Note");
        String htmlBody = tmplOpt.map(EmailTemplate::getHtmlBody)
                .orElse("<html><body><p>Dear Customer,</p><p>Please find your contract note attached.</p>"
                      + "<p>Regards,<br/>GEOJIT</p></body></html>");

        // Download existing PDF from S3
        byte[] pdfBytes = s3Service.downloadPdfBytes(customer.getPdfS3Key());

        try {
            // Build metajson header (matches Lambda format for bounce/delivery tracking)
            String metajson = String.format(
                    "{\"partycode\":\"%s\",\"jobid\":\"%s\",\"tradedate\":\"%s\"}",
                    customer.getPartyCode(),
                    job.getJobId() != null ? job.getJobId().toString() : "UNKNOWN",
                    customer.getTradeDate() != null ? customer.getTradeDate() : "");

            // Build MIME message using Jakarta Mail (same structure as Lambda)
            Session session = Session.getInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
            mimeMessage.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(toEmail));
            mimeMessage.setSubject(subject, "UTF-8");
            mimeMessage.addHeader("metajson", metajson);

            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            MimeBodyPart pdfPart = new MimeBodyPart();
            pdfPart.setDataHandler(new DataHandler(
                    new ByteArrayDataSource(pdfBytes, "application/pdf")));
            pdfPart.setFileName(customer.getPartyCode() + "_ContractNote.pdf");
            multipart.addBodyPart(pdfPart);

            mimeMessage.setContent(multipart);
            mimeMessage.saveChanges();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mimeMessage.writeTo(out);

            SendRawEmailRequest rawReq = new SendRawEmailRequest()
                    .withRawMessage(new RawMessage().withData(ByteBuffer.wrap(out.toByteArray())))
                    .withConfigurationSetName(configSetName);

            String messageId = ses.sendRawEmail(rawReq).getMessageId();
            log.info("Direct SES resend sent | partyCode={} | jobId={} | sesMessageId={}",
                    customer.getPartyCode(), job.getJobId(), messageId);
            return messageId;

        } catch (Exception e) {
            throw new ValidationException("Failed to send resend email for "
                    + customer.getPartyCode() + ": " + e.getMessage());
        }
    }

    private boolean isResendableStatus(Job.JobStatus status) {
        return status == Job.JobStatus.COMPLETED
            || status == Job.JobStatus.PARTIAL
            || status == Job.JobStatus.FAILED;
    }
}
