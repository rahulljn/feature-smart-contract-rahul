package com.geojit.contractnote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SegmentDto {
    private Integer id;
    private String code;           // e.g. 'EQUITY-COMBINEMARGIN'
    private String displayName;    // e.g. 'Equity Combine Margin'
    private String s3Folder;       // e.g. 'equity'
    private boolean isActive;
}
