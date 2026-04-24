package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.dto.response.SecretsManagerCertResponse;
import com.geojit.contractnote.dto.response.SesStatisticsResponse;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.exception.ResourceNotFoundException;
import com.geojit.contractnote.exception.ValidationException;
import com.geojit.contractnote.repository.*;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.service.S3Service;
import com.geojit.contractnote.service.SecretsManagerService;
import com.geojit.contractnote.service.SesStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Configuration")
@RequiredArgsConstructor
public class ConfigController {

    private final SesConfigRepository   sesConfigRepository;
    private final CertificateRepository certificateRepository;
    private final SesStatisticsService  sesStatisticsService;
    private final SecretsManagerService secretsManagerService;
    private final S3Service             s3Service;
    private final UserRepository        userRepository;

    // ─── SES Config ────────────────────────────────────────────────

    @GetMapping("/ses")
    @Operation(summary = "List all SES config sets")
    public ResponseEntity<ApiResponse<List<SesConfig>>> getSesConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(
                sesConfigRepository.findAllByOrderByConfigSetNameAsc()));
    }

    @PostMapping("/ses")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new SES config set")
    @Transactional
    public ResponseEntity<ApiResponse<SesConfig>> createSesConfig(@RequestBody Map<String, String> body) {
        SesConfig config = SesConfig.builder()
                .configSetName(body.get("configSetName"))
                .fromEmail(body.get("fromEmail"))
                .fromName(body.get("fromName"))
                .domain(body.get("domain"))
                .region(body.getOrDefault("region", "ap-south-1"))
                .isActive(false)
                .build();
        return ResponseEntity.ok(ApiResponse.ok(sesConfigRepository.save(config)));
    }

    @PutMapping("/ses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing SES config set")
    @Transactional
    public ResponseEntity<ApiResponse<SesConfig>> updateSesConfig(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        SesConfig config = sesConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SesConfig", "id", id));
        if (body.containsKey("configSetName")) config.setConfigSetName(body.get("configSetName"));
        if (body.containsKey("fromEmail"))     config.setFromEmail(body.get("fromEmail"));
        if (body.containsKey("fromName"))      config.setFromName(body.get("fromName"));
        if (body.containsKey("domain"))        config.setDomain(body.get("domain"));
        if (body.containsKey("region"))        config.setRegion(body.get("region"));
        return ResponseEntity.ok(ApiResponse.ok(sesConfigRepository.save(config)));
    }

    @PostMapping("/ses/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set a SES config as active (deactivates all others)")
    @Transactional
    public ResponseEntity<ApiResponse<SesConfig>> activateSesConfig(@PathVariable UUID id) {
        // Deactivate all first
        sesConfigRepository.findAll().forEach(c -> { c.setActive(false); sesConfigRepository.save(c); });
        // Activate the requested one
        SesConfig config = sesConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SesConfig", "id", id));
        config.setActive(true);
        return ResponseEntity.ok(ApiResponse.ok(sesConfigRepository.save(config)));
    }

    // ─── SES Statistics ────────────────────────────────────────────

    @GetMapping("/ses/statistics")
    @Operation(summary = "Get SES account statistics (reputation, quota, bounce rate, time-series)")
    public ResponseEntity<ApiResponse<SesStatisticsResponse>> getSesStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end   = endDate   != null ? endDate   : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(sesStatisticsService.getStatistics(start, end)));
    }

    // ─── Certificates ──────────────────────────────────────────────

    @GetMapping("/certificates")
    @Operation(summary = "List all certificates from AWS Secrets Manager")
    public ResponseEntity<ApiResponse<List<SecretsManagerCertResponse>>> getCertificates() {
        return ResponseEntity.ok(ApiResponse.ok(secretsManagerService.listCertificates()));
    }

    @GetMapping("/certificates/active")
    @Operation(summary = "Get active certificate")
    @Transactional
    public ResponseEntity<ApiResponse<Certificate>> getActiveCert() {
        // No role restriction — all authenticated users need to see active cert status
        return ResponseEntity.ok(ApiResponse.ok(
                certificateRepository.findByIsActiveTrue()
                        .orElseThrow(() -> new ResourceNotFoundException("Certificate", "isActive", true))));
    }

    @PostMapping("/certificates/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set a certificate as active by secretName (deactivates all others)")
    @Transactional
    public ResponseEntity<ApiResponse<SecretsManagerCertResponse>> activateCertificate(
            @RequestParam String secretName) {
        certificateRepository.findAll().forEach(c -> { c.setActive(false); certificateRepository.save(c); });
        Certificate cert = certificateRepository.findBySecretName(secretName)
                .orElseGet(() -> Certificate.builder()
                        .fileName(secretName)
                        .secretName(secretName)
                        .isActive(false)
                        .build());
        cert.setActive(true);
        certificateRepository.save(cert);
        return ResponseEntity.ok(ApiResponse.ok(secretsManagerService.listCertificates().stream()
                .filter(r -> r.getSecretName().equals(secretName))
                .findFirst().orElseThrow()));
    }

    @PostMapping(value = "/certificates/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a PFX certificate, extract metadata, store in S3")
    @Transactional
    public ResponseEntity<ApiResponse<Certificate>> uploadCertificate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password,
            @RequestParam(value = "label", required = false) String label,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (file == null || file.isEmpty())
            throw new ValidationException("Certificate file is empty");
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "certificate.pfx";
        String lower = filename.toLowerCase();
        if (!lower.endsWith(".pfx") && !lower.endsWith(".p12"))
            throw new ValidationException("Only .pfx and .p12 files are accepted");

        try {
            byte[] pfxBytes = file.getBytes();

            // Parse PFX to extract certificate metadata using standard Java KeyStore API
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new ByteArrayInputStream(pfxBytes), password.toCharArray());

            String alias = null;
            java.util.Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String a = aliases.nextElement();
                if (ks.isKeyEntry(a)) { alias = a; break; }
            }
            if (alias == null && ks.aliases().hasMoreElements()) alias = ks.aliases().nextElement();
            if (alias == null) throw new ValidationException("No certificate found in the PFX file");

            X509Certificate x509 = (X509Certificate) ks.getCertificate(alias);
            String subject    = x509.getSubjectX500Principal().getName();
            String issuer     = x509.getIssuerX500Principal().getName();
            java.time.LocalDateTime validFrom = x509.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            java.time.LocalDateTime validTo   = x509.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            byte[] digest     = MessageDigest.getInstance("SHA-1").digest(x509.getEncoded());
            String thumbprint = HexFormat.of().formatHex(digest).toUpperCase();

            // Store PFX in S3
            String s3Key = "certs/" + java.util.UUID.randomUUID() + "/" + filename;
            s3Service.uploadBytes(pfxBytes, s3Key, "application/x-pkcs12");

            // SecretName convention: certs/{label-or-filename}
            String secretName = "geojit/certs/" + (label != null && !label.isBlank() ? label : filename);

            User uploader = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

            Certificate cert = Certificate.builder()
                    .fileName(label != null && !label.isBlank() ? label : filename)
                    .subject(subject)
                    .issuer(issuer)
                    .validFrom(validFrom)
                    .validTo(validTo)
                    .thumbprint(thumbprint)
                    .secretName(secretName)
                    .s3Key(s3Key)
                    .isActive(false)
                    .uploadedBy(uploader)
                    .build();

            return ResponseEntity.ok(ApiResponse.ok("Certificate uploaded successfully", certificateRepository.save(cert)));

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to process PFX file: " + e.getMessage());
        }
    }
}
