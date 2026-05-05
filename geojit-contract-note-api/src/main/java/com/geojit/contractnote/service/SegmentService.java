package com.geojit.contractnote.service;

import com.geojit.contractnote.entity.Segment;
import com.geojit.contractnote.repository.SegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentService {

    private final SegmentRepository segmentRepository;

    public List<Segment> getActiveSegments() {
        return segmentRepository.findByIsActiveTrue();
    }

    public Optional<Segment> getByCode(String code) {
        return segmentRepository.findByCode(code);
    }
}
