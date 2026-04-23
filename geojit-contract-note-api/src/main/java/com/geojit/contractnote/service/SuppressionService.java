package com.geojit.contractnote.service;

import com.geojit.contractnote.dto.request.SuppressionRequest;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.exception.*;
import com.geojit.contractnote.repository.SuppressionListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuppressionService {

    private final SuppressionListRepository suppressionListRepository;
    private final SesV2Client               sesV2Client;

    public Page<SuppressionList> getAll(Pageable pageable) {
        return suppressionListRepository.findAllByOrderByAddedAtDesc(pageable);
    }

    public boolean isSuppressed(String email) {
        return suppressionListRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Transactional
    public SuppressionList add(SuppressionRequest req, UUID addedBy) {
        String email = req.getEmail().toLowerCase().trim();
        if (suppressionListRepository.existsByEmail(email))
            throw new ValidationException("Email already suppressed: " + email);

        SuppressionList entry = SuppressionList.builder()
                .email(email)
                .reason(req.getReason())
                .addedBy(addedBy)
                .build();
        SuppressionList saved = suppressionListRepository.save(entry);

        try {
            SuppressionListReason awsReason = req.getReason() != null
                    && req.getReason().toLowerCase().contains("complaint")
                    ? SuppressionListReason.COMPLAINT
                    : SuppressionListReason.BOUNCE;
            sesV2Client.putSuppressedDestination(PutSuppressedDestinationRequest.builder()
                    .emailAddress(email)
                    .reason(awsReason)
                    .build());
            log.info("Added {} to AWS SES account suppression list (reason={})", email, awsReason);
        } catch (Exception e) {
            log.warn("Could not add {} to AWS SES suppression list — local DB entry still saved: {}", email, e.getMessage());
        }

        return saved;
    }

    @Transactional
    public void remove(String email) {
        SuppressionList entry = suppressionListRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Suppression entry", "email", email));
        suppressionListRepository.delete(entry);

        try {
            sesV2Client.deleteSuppressedDestination(DeleteSuppressedDestinationRequest.builder()
                    .emailAddress(email.toLowerCase().trim())
                    .build());
            log.info("Removed {} from AWS SES account suppression list", email);
        } catch (NotFoundException e) {
            log.debug("Email {} was not in AWS SES suppression list (already absent)", email);
        } catch (Exception e) {
            log.warn("Could not remove {} from AWS SES suppression list — local DB entry still deleted: {}", email, e.getMessage());
        }
    }

    public int pushToAws() {
        List<SuppressionList> all = suppressionListRepository.findAll();
        int pushed = 0;
        for (SuppressionList entry : all) {
            try {
                SuppressionListReason awsReason = "complaint".equalsIgnoreCase(entry.getReason())
                        ? SuppressionListReason.COMPLAINT : SuppressionListReason.BOUNCE;
                sesV2Client.putSuppressedDestination(PutSuppressedDestinationRequest.builder()
                        .emailAddress(entry.getEmail())
                        .reason(awsReason)
                        .build());
                pushed++;
            } catch (Exception e) {
                log.warn("Could not push {} to AWS SES suppression list: {}", entry.getEmail(), e.getMessage());
            }
        }
        log.info("Pushed {} local suppression entries to AWS SES", pushed);
        return pushed;
    }

    public int syncFromAws() {
        int added = 0;
        String nextToken = null;
        try {
            do {
                ListSuppressedDestinationsRequest.Builder reqBuilder =
                        ListSuppressedDestinationsRequest.builder().pageSize(100);
                if (nextToken != null) reqBuilder.nextToken(nextToken);
                ListSuppressedDestinationsResponse resp =
                        sesV2Client.listSuppressedDestinations(reqBuilder.build());

                for (SuppressedDestinationSummary dest : resp.suppressedDestinationSummaries()) {
                    String email = dest.emailAddress().toLowerCase().trim();
                    if (!suppressionListRepository.existsByEmail(email)) {
                        suppressionListRepository.save(SuppressionList.builder()
                                .email(email)
                                .reason(dest.reason().name().toLowerCase())
                                .build());
                        added++;
                    }
                }
                nextToken = resp.nextToken();
            } while (nextToken != null);
        } catch (Exception e) {
            log.error("Failed to sync suppression list from AWS SES: {}", e.getMessage());
            throw new RuntimeException("AWS SES sync failed: " + e.getMessage(), e);
        }
        log.info("Synced {} new entries from AWS SES account suppression list", added);
        return added;
    }
}
