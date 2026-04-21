package com.geojit.contractnote.service;

import com.geojit.contractnote.dto.request.SuppressionRequest;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.exception.*;
import com.geojit.contractnote.repository.SuppressionListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuppressionService {

    private final SuppressionListRepository suppressionListRepository;

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
        return suppressionListRepository.save(entry);
    }

    @Transactional
    public void remove(String email) {
        SuppressionList entry = suppressionListRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Suppression entry", "email", email));
        suppressionListRepository.delete(entry);
    }
}
