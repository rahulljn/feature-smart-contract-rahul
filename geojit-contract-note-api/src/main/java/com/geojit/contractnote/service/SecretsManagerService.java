package com.geojit.contractnote.service;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.ListSecretsRequest;
import com.amazonaws.services.secretsmanager.model.ListSecretsResult;
import com.amazonaws.services.secretsmanager.model.SecretListEntry;
import com.geojit.contractnote.dto.response.SecretsManagerCertResponse;
import com.geojit.contractnote.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecretsManagerService {

    private final AWSSecretsManager     secretsManager;
    private final CertificateRepository certificateRepository;

    public List<SecretsManagerCertResponse> listCertificates() {
        Set<String> activeNames = certificateRepository.findAll().stream()
                .filter(c -> c.isActive())
                .map(c -> c.getSecretName())
                .collect(Collectors.toSet());

        List<SecretsManagerCertResponse> results = new ArrayList<>();
        try {
            ListSecretsRequest req = new ListSecretsRequest();
            ListSecretsResult  res = secretsManager.listSecrets(req);
            for (SecretListEntry entry : res.getSecretList()) {
                results.add(SecretsManagerCertResponse.builder()
                        .secretName(entry.getName())
                        .description(entry.getDescription())
                        .createdDate(entry.getCreatedDate() != null
                                ? entry.getCreatedDate().toInstant().atOffset(ZoneOffset.UTC).toString()
                                : null)
                        .lastChangedDate(entry.getLastChangedDate() != null
                                ? entry.getLastChangedDate().toInstant().atOffset(ZoneOffset.UTC).toString()
                                : null)
                        .isActive(activeNames.contains(entry.getName()))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Secrets Manager listSecrets failed: {}", e.getMessage());
        }
        return results;
    }
}
