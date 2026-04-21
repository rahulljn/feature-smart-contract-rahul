package com.geojit.contractnote.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String subject;
    @NotBlank
    private String htmlBody;
    private String segment;
    private boolean isActive;
}
