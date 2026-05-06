package com.geojit.contractnote.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.geojit.contractnote.dto.response.*;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.exception.ResourceNotFoundException;
import com.geojit.contractnote.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// TODO: add Spring Cache (@Cacheable) with 5-min TTL on
// listBounceRecords() and getClientBounceRecords() for production

@Slf4j
@Service
@RequiredArgsConstructor
public class S3BounceService {

    @Value("${app.aws.s3.bounce-report-bucket}")
    private String bounceBucket;

    private final AmazonS3 amazonS3;
    private final SegmentService segmentService;
    private final JobRepository jobRepository;
    private final JobCustomerRepository jobCustomerRepository;
    private final ResendService resendService;

    private static final String[] DATE_FORMATS = {
        "dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "dd.MM.yyyy"
    };

    /**
     * Parses a CSV line handling RFC 4180 compliant fields with quotes.
     * Handles: quoted fields, escaped quotes (""), embedded commas.
     * Returns array of field values with quotes stripped.
     */
    private String[] parseCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) return new String[0];

        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote ("") - treat as literal quote
                    current.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote mode
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());

        // Remove surrounding quotes and unescape inner double-quotes
        String[] result = new String[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i).trim();
            if (field.startsWith("\"") && field.endsWith("\"")) {
                field = field.substring(1, field.length() - 1);
                field = field.replace("\"\"", "\"");
            }
            result[i] = field;
        }

        return result;
    }

    /**
     * Parses date string trying multiple formats.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        dateStr = dateStr.trim();

        for (String fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {}
        }
        log.warn("Could not parse date: [{}] - tried formats: {}", dateStr, Arrays.toString(DATE_FORMATS));
        return null;
    }

    /**
     * Lists all S3 objects under a prefix, handling pagination.
     */
    private List<String> listAllObjects(String prefix) {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(bounceBucket)
                    .withPrefix(prefix)
                    .withMaxKeys(1000);

            if (continuationToken != null) {
                req.setContinuationToken(continuationToken);
            }

            try {
                ListObjectsV2Result result = amazonS3.listObjectsV2(req);
                result.getObjectSummaries().forEach(obj -> {
                    if (obj.getKey().endsWith(".csv")) {
                        keys.add(obj.getKey());
                    }
                });
                continuationToken = result.getNextContinuationToken();
            } catch (AmazonServiceException e) {
                log.error("Error listing S3 objects under prefix [{}]: {}", prefix, e.getMessage(), e);
                break;
            }
        } while (continuationToken != null);

        return keys;
    }

    /**
     * Parses CSV content from S3 and converts to BounceRecord list.
     */
    private List<BounceRecord> parseCsvFromS3(String s3Key, LocalDate recordDate, String segmentFolder) {
        List<BounceRecord> records = new ArrayList<>();

        try (S3Object s3Obj = amazonS3.getObject(new GetObjectRequest(bounceBucket, s3Key));
             S3ObjectInputStream content = s3Obj.getObjectContent();
             BufferedReader reader = new BufferedReader(new InputStreamReader(content))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    // Skip header if it starts with "CLIENT CODE"
                    if (line.startsWith("CLIENT CODE")) continue;
                }

                String[] fields = parseCsvLine(line);
                if (fields.length < 9) {
                    log.warn("Skipping malformed CSV line in [{}]: {}", s3Key, line);
                    continue;
                }

                BounceRecord record = BounceRecord.builder()
                        .partyCode(fields[0].trim())
                        .clientName(fields[1].trim())
                        .clientEmail(fields[2].trim())
                        .activityDate(fields[3].trim())
                        .contractNo(fields[4].trim())
                        .tradeDate(fields[5].trim())
                        .fileName(fields[6].trim())
                        .bounceType(fields[7].trim())
                        .bounceReason(fields[8].trim())
                        .s3Key(s3Key)
                        .segment(segmentFolder)
                        .recordDate(recordDate)
                        .build();
                records.add(record);
            }
        } catch (Exception e) {
            log.error("Error parsing CSV from S3 [{}]: {}", s3Key, e.getMessage(), e);
        }

        return records;
    }

    /**
     * METHOD 1: Bulk listing — uses BounceLogAllReport path.
     */
    public List<BounceRecord> listBounceRecords(LocalDate from, LocalDate to, String segmentCode) {
        List<BounceRecord> results = new ArrayList<>();
        List<String> segmentsToSearch = new ArrayList<>();

        // Resolve segment(s) to search
        if (segmentCode == null || segmentCode.trim().isEmpty()) {
            // All active segments
            segmentService.getActiveSegments().forEach(s -> segmentsToSearch.add(s.getS3Folder()));
        } else {
            segmentService.getByCode(segmentCode)
                    .map(Segment::getS3Folder)
                    .ifPresentOrElse(
                        segmentsToSearch::add,
                        () -> { throw new IllegalArgumentException("Unknown segment: " + segmentCode); }
                    );
        }

        // Iterate dates
        LocalDate current = from;
        while (!current.isAfter(to)) {
            for (String segmentFolder : segmentsToSearch) {
                String prefix = "GeojitCN-EmailReport/BounceLogAllReport/"
                        + segmentFolder + "/"
                        + String.format("%04d", current.getYear()) + "/"
                        + String.format("%02d", current.getMonthValue()) + "/"
                        + String.format("%02d", current.getDayOfMonth()) + "/allreport/";

                List<String> keys = listAllObjects(prefix);
                for (String key : keys) {
                    List<BounceRecord> parsed = parseCsvFromS3(key, current, segmentFolder);
                    results.addAll(parsed);
                }
            }
            current = current.plusDays(1);
        }

        // Sort by recordDate desc, partyCode asc
        results.sort((a, b) -> {
            int dateCompare = b.getRecordDate().compareTo(a.getRecordDate());
            if (dateCompare != 0) return a.getPartyCode().compareTo(b.getPartyCode());
            return dateCompare;
        });

        return results;
    }

    /**
     * METHOD 2: Single client lookup — uses per-client BounceLog path.
     */
    public List<BounceRecord> getClientBounceRecords(String partyCode, LocalDate from, LocalDate to, String segmentCode) {
        List<BounceRecord> results = new ArrayList<>();
        String safePartyCode = partyCode.replace("/", "_").replace("\\", "_").trim();
        List<String> segmentsToSearch = new ArrayList<>();

        // Resolve segment(s) to search
        if (segmentCode == null || segmentCode.trim().isEmpty()) {
            segmentService.getActiveSegments().forEach(s -> segmentsToSearch.add(s.getS3Folder()));
        } else {
            segmentService.getByCode(segmentCode)
                    .map(Segment::getS3Folder)
                    .ifPresentOrElse(
                        segmentsToSearch::add,
                        () -> { throw new IllegalArgumentException("Unknown segment: " + segmentCode); }
                    );
        }

        LocalDate current = from;
        while (!current.isAfter(to)) {
            for (String segmentFolder : segmentsToSearch) {
                String prefix = "GeojitCN-EmailReport/BounceLog/"
                        + segmentFolder + "/"
                        + String.format("%04d", current.getYear()) + "/"
                        + String.format("%02d", current.getMonthValue()) + "/"
                        + String.format("%02d", current.getDayOfMonth()) + "/"
                        + safePartyCode + "/";

                List<String> keys = listAllObjects(prefix);
                for (String key : keys) {
                    List<BounceRecord> parsed = parseCsvFromS3(key, current, segmentFolder);
                    results.addAll(parsed);
                }
            }
            current = current.plusDays(1);
        }

        // Sort by recordDate desc
        results.sort((a, b) -> b.getRecordDate().compareTo(a.getRecordDate()));

        return results;
    }

    /**
     * METHOD 3: Generate presigned URL for bounce CSV download.
     */
    public String generatePresignedUrl(String s3Key) {
        if (s3Key == null || !s3Key.startsWith("GeojitCN-EmailReport/")) {
            throw new SecurityException("Invalid S3 key for presigned URL: " + s3Key);
        }

        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                bounceBucket, s3Key, HttpMethod.GET)
                .withExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000));

        String url = amazonS3.generatePresignedUrl(req).toString();
        log.info("Generated presigned URL for [{}]", s3Key);
        return url;
    }

    /**
     * METHOD 4: Download all reports merged into single CSV.
     */
    public byte[] downloadAllReportsMerged(LocalDate from, LocalDate to, String segmentCode) throws IOException {
        StringBuilder merged = new StringBuilder();
        boolean headerWritten = false;
        String header = "CLIENT CODE,CLIENT NAME,CLIENT EMAIL,ACTIVITY DATE,CONTRACT NO,TRADE DATE,fileName,BO Type,BO REASON";

        List<String> segmentsToSearch = new ArrayList<>();

        // Resolve segment(s) to search
        if (segmentCode == null || segmentCode.trim().isEmpty()) {
            segmentService.getActiveSegments().forEach(s -> segmentsToSearch.add(s.getS3Folder()));
        } else {
            segmentService.getByCode(segmentCode)
                    .map(Segment::getS3Folder)
                    .ifPresentOrElse(
                        segmentsToSearch::add,
                        () -> { throw new IllegalArgumentException("Unknown segment: " + segmentCode); }
                    );
        }

        LocalDate current = from;
        while (!current.isAfter(to)) {
            for (String segmentFolder : segmentsToSearch) {
                String prefix = "GeojitCN-EmailReport/BounceLogAllReport/"
                        + segmentFolder + "/"
                        + String.format("%04d", current.getYear()) + "/"
                        + String.format("%02d", current.getMonthValue()) + "/"
                        + String.format("%02d", current.getDayOfMonth()) + "/allreport/";

                List<String> keys = listAllObjects(prefix);
                for (String key : keys) {
                    try (S3Object s3Obj = amazonS3.getObject(new GetObjectRequest(bounceBucket, key));
                         S3ObjectInputStream content = s3Obj.getObjectContent();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(content))) {

                        String line;
                        boolean firstLine = true;
                        while ((line = reader.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                                // Write header once
                                if (!headerWritten) {
                                    merged.append(header).append("\n");
                                    headerWritten = true;
                                }
                                continue;
                            }

                            String[] fields = parseCsvLine(line);
                            if (fields.length >= 7) {
                                String fileName = fields[6].trim();
                                // Apply segment filter on fileName if segmentCode specified
                                boolean include = segmentCode == null || segmentCode.trim().isEmpty();
                                if (!include) {
                                    // Map segment folder back to code for matching
                                    include = fileName.toLowerCase().contains(segmentFolder.toLowerCase());
                                }
                                if (include) {
                                    merged.append(line).append("\n");
                                }
                            }
                        }
                    }
                }
            }
            current = current.plusDays(1);
        }

        return merged.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * METHOD 5: Resend bounce record.
     * Finds JobCustomer by partyCode (handles "ZVL056" and "ZVL056/ZVL056"),
     * then sends email via SES if PDF exists, or re-queues via SQS if not.
     */
    public ResendBounceResult resendBounce(String partyCode, String fileName, String fallbackEmail) {
        // Normalize party code: "ZVL056/ZVL056" → "ZVL056"
        String normalized = partyCode.contains("/")
                ? partyCode.substring(0, partyCode.indexOf('/'))
                : partyCode;

        // Find matching JobCustomers (handles both "ZVL056" and "ZVL056/ZVL056" in DB)
        List<JobCustomer> matches = jobCustomerRepository.findAllByPartyCode(normalized);
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("JobCustomer", "partyCode", normalized);
        }

        // Pick the most recent one with a generated PDF
        JobCustomer jc = matches.stream()
                .filter(c -> c.getPdfS3Key() != null && !c.getPdfS3Key().isBlank())
                .findFirst()
                .orElse(matches.get(0));

        Job job = jc.getJob();

        try {
            // Use DB email, fall back to the email from bounce CSV
            String effectiveEmail = (jc.getEmail() != null && !jc.getEmail().isBlank())
                    ? jc.getEmail()
                    : fallbackEmail;

            if (jc.getPdfS3Key() != null && !jc.getPdfS3Key().isBlank()) {
                // Path A: PDF exists — send email directly via SES
                log.info("Bounce resend | email-only path | partyCode={} | jobId={}", normalized, job.getJobId());

                if (effectiveEmail == null || effectiveEmail.isBlank()) {
                    return new ResendBounceResult(normalized, job.getJobId().toString(), "FAILED",
                            "No email address on file for " + normalized);
                }

                jc.setEmailStatus(JobCustomer.EmailStatus.PENDING);
                jc.setSesMessageId(null);
                jobCustomerRepository.save(jc);

                String messageId = resendService.sendEmailDirectly(job, jc, effectiveEmail, null);

                jc.setSesMessageId(messageId);
                jc.setEmailStatus(JobCustomer.EmailStatus.SENT);
                jobCustomerRepository.save(jc);

                return new ResendBounceResult(normalized, job.getJobId().toString(), "QUEUED", "Resend queued successfully");
            } else {
                // Path B: No PDF — reset for full re-process via Lambda pipeline
                log.info("Bounce resend | full-reprocess path | partyCode={} | jobId={}", normalized, job.getJobId());

                jc.setEmailStatus(JobCustomer.EmailStatus.PENDING);
                jc.setPdfStatus(JobCustomer.PdfStatus.PENDING);
                jobCustomerRepository.save(jc);

                return new ResendBounceResult(normalized, job.getJobId().toString(), "QUEUED",
                        "Re-process queued — no PDF available");
            }
        } catch (Exception e) {
            log.error("Resend failed for partyCode [{}]: {}", normalized, e.getMessage(), e);
            return new ResendBounceResult(normalized, job.getJobId().toString(), "FAILED", e.getMessage());
        }
    }
}
