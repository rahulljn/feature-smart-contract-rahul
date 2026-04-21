package com.geojit.contractnote.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for updating only the restricted editable fields of an email template.
 * The non-editable HTML skeleton is managed by EmailTemplateService.buildHtml().
 */
@Data
public class TemplateFieldsRequest {

    @NotBlank(message = "subject is required")
    private String subject;

    private String greetingText;
    private String bodyIntro;
    private String logoUrl;
    private String bodyColor;
    private String footerColor;
}
