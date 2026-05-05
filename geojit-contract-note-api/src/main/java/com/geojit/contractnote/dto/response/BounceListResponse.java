package com.geojit.contractnote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class BounceListResponse {
    private java.util.List<BounceRecord> records;
    private int totalCount;
    private LocalDate from;
    private LocalDate to;
    private String segment;
}
