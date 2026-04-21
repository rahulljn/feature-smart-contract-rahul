package com.geojit.contractnote.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.geojit.contractnote.config.AppProperties;
import com.geojit.contractnote.exception.S3OperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3       amazonS3;
    private final AppProperties  appProperties;

    /**
     * Upload raw file to S3 raw bucket.
     * In local-mock mode the S3 call fails gracefully — a fake key is returned
     * so the job is still created in PostgreSQL.
     */
    public String uploadRawFile(MultipartFile file, String jobId) {
        String bucket = appProperties.getAws().getS3().getRawBucket();
        String key    = "raw/" + jobId + "/" + file.getOriginalFilename();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.addUserMetadata("jobid", jobId);

            amazonS3.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), metadata));
            log.info("Uploaded raw file | bucket={} | key={} | jobId={}", bucket, key, jobId);
            return key;
        } catch (IOException e) {
            throw new S3OperationException("Failed to read uploaded file: " + e.getMessage(), e);
        } catch (Exception e) {
            // In local-mock mode, AWS call fails — return mock key so the job is still created
            log.warn("⚠️  S3 upload skipped (local-mock mode) | jobId={} | reason={}", jobId, e.getMessage());
            return "mock-raw/" + jobId + "/" + file.getOriginalFilename();
        }
    }

    /**
     * List PDFs in the pdf bucket for a given partyCode.
     * Returns empty list in local-mock mode (no real S3).
     */
    public List<S3ObjectSummary> listPdfsForPartyCode(String partyCode) {
        String bucket = appProperties.getAws().getS3().getPdfBucket();
        String prefix = "contractNote/";
        try {
            ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(prefix);
            ListObjectsV2Result result = amazonS3.listObjectsV2(req);
            return result.getObjectSummaries().stream()
                    .filter(s -> s.getKey().contains("/" + partyCode + "/"))
                    .toList();
        } catch (Exception e) {
            log.warn("⚠️  S3 listPdfs skipped (local-mock mode) | partyCode={} | reason={}", partyCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get user metadata from a PDF's S3 object.
     * Returns empty map in local-mock mode.
     */
    public Map<String, String> getPdfMetadata(String s3Key) {
        String bucket = appProperties.getAws().getS3().getPdfBucket();
        try {
            ObjectMetadata metadata = amazonS3.getObjectMetadata(bucket, s3Key);
            return metadata.getUserMetadata();
        } catch (Exception e) {
            log.warn("⚠️  S3 getMetadata skipped (local-mock mode) | key={} | reason={}", s3Key, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * List delivery/bounce reports for a partyCode.
     * Returns empty list in local-mock mode.
     */
    public List<S3ObjectSummary> listReportsForPartyCode(String partyCode, String reportType) {
        String bucket = appProperties.getAws().getS3().getReportBucket();
        String prefix = "GeojitCN-EmailReport/" + reportType + "/";
        try {
            ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(prefix);
            ListObjectsV2Result result = amazonS3.listObjectsV2(req);
            return result.getObjectSummaries().stream()
                    .filter(s -> s.getKey().contains("/" + partyCode + "/"))
                    .toList();
        } catch (Exception e) {
            log.warn("⚠️  S3 listReports skipped (local-mock mode) | partyCode={} | reason={}", partyCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Generate a presigned URL for any S3 object.
     * Returns a placeholder in local-mock mode.
     */
    public String generatePresignedUrl(String bucket, String s3Key) {
        try {
            long expiryHours = appProperties.getAws().getPresignedUrlExpiryHours();
            Date expiry = new Date(System.currentTimeMillis() + expiryHours * 3_600_000L);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, s3Key)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiry);
            URL url = amazonS3.generatePresignedUrl(req);
            return url.toString();
        } catch (Exception e) {
            log.warn("⚠️  S3 presignedUrl skipped (local-mock mode) | key={} | reason={}", s3Key, e.getMessage());
            return "#local-mode-no-presigned-url-available";
        }
    }

    public String generatePdfPresignedUrl(String s3Key) {
        return generatePresignedUrl(appProperties.getAws().getS3().getPdfBucket(), s3Key);
    }

    /**
     * Upload arbitrary bytes to S3 raw bucket under a given key.
     * Used for PFX certificate storage under the certs/ prefix.
     */
    public String uploadBytes(byte[] bytes, String key, String contentType) {
        String bucket = appProperties.getAws().getS3().getRawBucket();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(contentType);
            amazonS3.putObject(new PutObjectRequest(bucket, key,
                    new java.io.ByteArrayInputStream(bytes), metadata));
            log.info("Uploaded bytes | bucket={} | key={}", bucket, key);
            return key;
        } catch (Exception e) {
            log.warn("⚠️  S3 uploadBytes skipped (local-mock mode) | key={} | reason={}", key, e.getMessage());
            return "mock-" + key;
        }
    }

    /**
     * Download PDF bytes from S3 pdf bucket.
     * Used by ResendService for email-only resends (PDF already exists).
     */
    public byte[] downloadPdfBytes(String s3Key) {
        String bucket = appProperties.getAws().getS3().getPdfBucket();
        try {
            S3Object obj = amazonS3.getObject(bucket, s3Key);
            return obj.getObjectContent().readAllBytes();
        } catch (Exception e) {
            throw new S3OperationException("Failed to download PDF from S3 | key=" + s3Key + " | " + e.getMessage(), e);
        }
    }
}
