package com.geojit.contractnote.dto.response;

import com.geojit.contractnote.entity.JobCustomer;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobCustomerResponse {
    private Long id;
    private UUID jobId;
    private String partyCode;
    private String contractNoteNo;
    private String tradeDate;
    private String email;
    private String segment;
    private String fileName;
    private JobCustomer.PdfStatus pdfStatus;
    private String pdfS3Key;
    private JobCustomer.EmailStatus emailStatus;
    private String sesMessageId;
    private String bounceType;
    private String bounceReason;
    private LocalDateTime pdfGeneratedAt;
    private LocalDateTime emailSentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime bouncedAt;
    private LocalDateTime processedAt;

    private static String normalise(String code) {
        if (code == null) return null;
        int slash = code.indexOf('/');
        return slash > 0 ? code.substring(0, slash) : code;
    }

    public static JobCustomerResponse from(JobCustomer jc) {
        return JobCustomerResponse.builder()
                .id(jc.getId())
                .jobId(jc.getJob() != null ? jc.getJob().getJobId() : null)
                .partyCode(normalise(jc.getPartyCode()))
                .contractNoteNo(jc.getContractNoteNo())
                .tradeDate(jc.getTradeDate())
                .email(jc.getEmail())
                .segment(jc.getSegment())
                .fileName(jc.getFileName())
                .pdfStatus(jc.getPdfStatus())
                .pdfS3Key(jc.getPdfS3Key())
                .emailStatus(jc.getEmailStatus())
                .sesMessageId(jc.getSesMessageId())
                .bounceType(jc.getBounceType())
                .bounceReason(jc.getBounceReason())
                .pdfGeneratedAt(jc.getPdfGeneratedAt())
                .emailSentAt(jc.getEmailSentAt())
                .deliveredAt(jc.getDeliveredAt())
                .bouncedAt(jc.getBouncedAt())
                .processedAt(latestOf(jc.getPdfGeneratedAt(), jc.getEmailSentAt(), jc.getDeliveredAt(), jc.getBouncedAt()))
                .build();
    }

    @SafeVarargs
    private static LocalDateTime latestOf(LocalDateTime... values) {
        LocalDateTime latest = null;
        for (LocalDateTime v : values) {
            if (v != null && (latest == null || v.isAfter(latest))) latest = v;
        }
        return latest;
    }
}
