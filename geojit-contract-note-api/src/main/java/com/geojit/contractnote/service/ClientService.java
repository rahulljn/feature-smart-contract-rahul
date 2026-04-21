package com.geojit.contractnote.service;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.geojit.contractnote.config.AppProperties;
import com.geojit.contractnote.dto.response.JobCustomerResponse;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.exception.ValidationException;
import com.geojit.contractnote.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final JobCustomerRepository     jobCustomerRepository;
    private final EmailEventRepository      emailEventRepository;
    private final SuppressionListRepository suppressionListRepository;
    private final S3Service                 s3Service;
    private final AppProperties             appProperties;

    /**
     * Returns process history (as DTOs) + client details from S3 metadata.
     * Optional filters: fromDate, toDate (filter on createdAt), segment (case-insensitive contains).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getClientProfile(String partyCode,
                                                LocalDateTime fromDate,
                                                LocalDateTime toDate,
                                                String segment) {
        // Normalise "ZYR175/ZYR175" → "ZYR175" so callers can use either form
        String code = partyCode != null && partyCode.contains("/") ? partyCode.split("/")[0] : partyCode;

        List<JobCustomer> raw;
        if (fromDate != null || toDate != null || (segment != null && !segment.isBlank())) {
            String segmentVal     = (segment != null && !segment.isBlank()) ? segment : null;
            String segmentPattern = segmentVal != null ? "%" + segmentVal.toLowerCase() + "%" : "%";
            raw = jobCustomerRepository.findAllByPartyCodeFiltered(
                    code, fromDate, toDate, segmentVal, segmentPattern);
        } else {
            raw = jobCustomerRepository.findAllByPartyCode(code);
        }

        // Convert to DTOs so processedAt (computed) and jobId are properly serialized
        List<JobCustomerResponse> history = raw.stream()
                .map(JobCustomerResponse::from)
                .collect(Collectors.toList());

        // Fetch latest PDF's S3 metadata for client details (name, email, PAN)
        Map<String, String> clientDetails = new HashMap<>();
        if (!raw.isEmpty()) {
            JobCustomer latest = raw.get(0);
            if (latest.getPdfS3Key() != null) {
                try {
                    clientDetails = s3Service.getPdfMetadata(latest.getPdfS3Key());
                } catch (Exception e) {
                    log.warn("Could not fetch S3 metadata for partyCode={}: {}", partyCode, e.getMessage());
                }
            }
        }

        return Map.of(
                "partyCode",      partyCode,
                "clientDetails",  clientDetails,
                "processHistory", history,
                "totalContracts", history.size()
        );
    }

    /** List all PDFs for a partyCode from S3 */
    public List<Map<String, Object>> listPdfs(String partyCode) {
        List<S3ObjectSummary> summaries = s3Service.listPdfsForPartyCode(partyCode);
        List<Map<String, Object>> result = new ArrayList<>();
        String pdfBucket = appProperties.getAws().getS3().getPdfBucket();

        for (S3ObjectSummary s : summaries) {
            try {
                Map<String, String> meta = s3Service.getPdfMetadata(s.getKey());
                result.add(Map.of(
                        "s3Key",        s.getKey(),
                        "size",         s.getSize(),
                        "lastModified", s.getLastModified(),
                        "metadata",     meta
                ));
            } catch (Exception e) {
                log.warn("Could not get metadata for {}: {}", s.getKey(), e.getMessage());
            }
        }
        return result;
    }

    /** List delivery reports for partyCode from S3 */
    public List<Map<String, Object>> listReports(String partyCode, String reportType) {
        List<S3ObjectSummary> summaries = s3Service.listReportsForPartyCode(partyCode, reportType);
        List<Map<String, Object>> result = new ArrayList<>();
        String reportBucket = appProperties.getAws().getS3().getReportBucket();

        for (S3ObjectSummary s : summaries) {
            String presignedUrl = s3Service.generatePresignedUrl(reportBucket, s.getKey());
            result.add(Map.of(
                    "s3Key",        s.getKey(),
                    "size",         s.getSize(),
                    "lastModified", s.getLastModified(),
                    "downloadUrl",  presignedUrl
            ));
        }
        return result;
    }

    /** Email timeline for partyCode from DB */
    @Transactional(readOnly = true)
    public List<EmailEvent> getEmailTimeline(String partyCode) {
        String code = partyCode != null && partyCode.contains("/") ? partyCode.split("/")[0] : partyCode;
        return emailEventRepository.findByPartyCodeOrderByEventTimestampDesc(code);
    }

    /** Generate presigned URL for PDF download */
    public String getPdfDownloadUrl(String s3Key) {
        return s3Service.generatePdfPresignedUrl(s3Key);
    }

    /**
     * Update the registered email address for a client across all their job_customer records.
     * Validates that the new email is not in the suppression list.
     */
    @Transactional
    public int updateCustomerEmail(String partyCode, String newEmail) {
        if (newEmail == null || newEmail.isBlank())
            throw new ValidationException("New email cannot be blank");
        if (suppressionListRepository.existsByEmailIgnoreCase(newEmail))
            throw new ValidationException("New email is in the suppression list: " + newEmail);

        String code = partyCode != null && partyCode.contains("/") ? partyCode.split("/")[0] : partyCode;
        List<JobCustomer> customers = jobCustomerRepository.findAllByPartyCode(code);
        if (customers.isEmpty())
            throw new ValidationException("No records found for party code: " + partyCode);

        for (JobCustomer c : customers) {
            c.setEmail(newEmail);
        }
        jobCustomerRepository.saveAll(customers);
        log.info("Email updated | partyCode={} | newEmail={} | records={}", partyCode, newEmail, customers.size());
        return customers.size();
    }
}
