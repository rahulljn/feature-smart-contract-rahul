package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    Optional<EmailTemplate> findByIsActiveTrue();
    boolean existsByName(String name);
}
