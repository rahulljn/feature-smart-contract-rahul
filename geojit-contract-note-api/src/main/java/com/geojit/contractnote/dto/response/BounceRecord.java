package com.geojit.contractnote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BounceRecord {
    private String partyCode;       // CLIENT CODE
    private String clientName;      // CLIENT NAME
    private String clientEmail;     // CLIENT EMAIL
    private String activityDate;    // ACTIVITY DATE
    private String contractNo;      // CONTRACT NO
    private String tradeDate;       // TRADE DATE
    private String fileName;        // fileName (original job file)
    private String bounceType;      // BO Type (Permanent/Transient/Complaint)
    private String bounceReason;    // BO REASON
    private String s3Key;          // full S3 object key of the CSV
    private String segment;         // segment s3_folder value (e.g. "equity")
    private LocalDate recordDate;   // date parsed from S3 path yyyy/MM/dd
}
