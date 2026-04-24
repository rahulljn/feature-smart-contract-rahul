package com.geojit.contractnote.dto.response;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class S3TemplateResponse {
    private String  name;
    private String  status;
    private String  s3Key;
    private Instant lastModified;
    private long    size;

    public static S3TemplateResponse from(S3ObjectSummary s) {
        String key    = s.getKey();
        String status = key.startsWith("active/") ? "active" : "draft";
        String name   = key.substring(key.lastIndexOf('/') + 1).replaceAll("\\.html$", "");
        return S3TemplateResponse.builder()
                .name(name).status(status).s3Key(key)
                .lastModified(s.getLastModified().toInstant())
                .size(s.getSize())
                .build();
    }
}
