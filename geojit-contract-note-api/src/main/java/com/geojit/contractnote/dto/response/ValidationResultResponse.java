package com.geojit.contractnote.dto.response;

import lombok.*;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ValidationResultResponse {
    private int totalCustomers;
    private int validCustomers;
    private int invalidCustomers;
    // Breakdown: how many customers failed due to each record type (e.g. "H" -> 120, "E" -> 45)
    private Map<String, Integer> invalidReasons;
}
