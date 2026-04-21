package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    Optional<Certificate> findByIsActiveTrue();
}
