package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.SesConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SesConfigRepository extends JpaRepository<SesConfig, UUID> {
    List<SesConfig> findByIsActiveTrueOrderByConfigSetNameAsc();
    List<SesConfig> findAllByOrderByConfigSetNameAsc();
    Optional<SesConfig> findByConfigSetName(String configSetName);
}
