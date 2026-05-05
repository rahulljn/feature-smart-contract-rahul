package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, Long> {

    List<Segment> findByIsActiveTrue();

    Optional<Segment> findByCode(String code);
}
