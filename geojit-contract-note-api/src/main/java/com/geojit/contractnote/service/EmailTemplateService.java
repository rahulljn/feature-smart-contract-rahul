package com.geojit.contractnote.service;

import com.geojit.contractnote.dto.request.TemplateFieldsRequest;
import com.geojit.contractnote.dto.request.TemplateRequest;
import com.geojit.contractnote.dto.response.S3TemplateResponse;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.exception.*;
import com.geojit.contractnote.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final S3Service               s3Service;

    @Transactional(readOnly = true)
    public List<EmailTemplate> getAll() {
        return emailTemplateRepository.findAll();
    }

    public List<S3TemplateResponse> listFromS3() {
        return s3Service.listTemplateObjects().stream()
                .map(S3TemplateResponse::from)
                .toList();
    }

    public String getContentFromS3(String name) {
        try {
            return s3Service.getTemplateContent("active/" + name + ".html");
        } catch (Exception e) {
            return s3Service.getTemplateContent("drafts/" + name + ".html");
        }
    }

    public EmailTemplate getById(UUID id) {
        return emailTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate", "id", id));
    }

    public EmailTemplate getActive() {
        return emailTemplateRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active email template found"));
    }

    @Transactional
    public EmailTemplate create(TemplateRequest req, User editedBy) {
        if (emailTemplateRepository.existsByName(req.getName()))
            throw new ValidationException("Template name already exists: " + req.getName());

        EmailTemplate t = EmailTemplate.builder()
                .name(req.getName())
                .subject(req.getSubject())
                .htmlBody(req.getHtmlBody())
                .segment(req.getSegment())
                .isActive(req.isActive())
                .lastEditedBy(editedBy)
                .lastEditedAt(LocalDateTime.now())
                .build();
        return emailTemplateRepository.save(t);
    }

    @Transactional
    public EmailTemplate update(UUID id, TemplateRequest req, User editedBy) {
        EmailTemplate t = getById(id);
        t.setSubject(req.getSubject());
        t.setHtmlBody(req.getHtmlBody());
        t.setSegment(req.getSegment());
        t.setLastEditedBy(editedBy);
        t.setLastEditedAt(LocalDateTime.now());
        return emailTemplateRepository.save(t);
    }

    /**
     * Update only the restricted editable fields and rebuild htmlBody from the fixed template skeleton.
     */
    @Transactional
    public EmailTemplate updateFields(UUID id, TemplateFieldsRequest req, User editedBy) {
        EmailTemplate t = getById(id);

        t.setSubject(req.getSubject());
        if (req.getGreetingText() != null) t.setGreetingText(req.getGreetingText());
        if (req.getBodyIntro()    != null) t.setBodyIntro(req.getBodyIntro());
        t.setLogoUrl(req.getLogoUrl()); // allow clearing logo
        if (req.getBodyColor()   != null && !req.getBodyColor().isBlank())   t.setBodyColor(req.getBodyColor());
        if (req.getFooterColor() != null && !req.getFooterColor().isBlank()) t.setFooterColor(req.getFooterColor());

        // Rebuild the full HTML from the fixed skeleton + updated editable fields
        String html = buildHtml(t);
        t.setHtmlBody(html);
        t.setLastEditedBy(editedBy);
        t.setLastEditedAt(LocalDateTime.now());

        EmailTemplate saved = emailTemplateRepository.save(t);

        // Sync the rebuilt HTML to S3 so the Email Lambda picks it up on next cache refresh
        s3Service.putTemplateHtml(html);

        return saved;
    }

    /**
     * Validate the template: checks for valid HTML structure and [NAME] placeholder.
     * Returns a map of { "valid": true/false, "errors": [...] }.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validate(UUID id) {
        EmailTemplate t = getById(id);
        String html = t.getHtmlBody();

        List<String> errors = new java.util.ArrayList<>();

        if (html == null || html.isBlank()) {
            errors.add("HTML body is empty");
        } else {
            String lower = html.toLowerCase();
            if (!lower.contains("<html"))   errors.add("Missing <html> tag");
            if (!lower.contains("<body"))   errors.add("Missing <body> tag");
            if (!lower.contains("</body>")) errors.add("Missing </body> closing tag");
            if (!lower.contains("</html>")) errors.add("Missing </html> closing tag");
            if (!html.contains("[NAME]"))   errors.add("Missing [NAME] placeholder — required for customer name substitution");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("templateId", id.toString());
        result.put("templateName", t.getName());
        return result;
    }

    @Transactional
    public EmailTemplate activate(UUID id) {
        // Deactivate all
        emailTemplateRepository.findAll().forEach(t -> { t.setActive(false); emailTemplateRepository.save(t); });
        // Activate target
        EmailTemplate t = getById(id);
        t.setActive(true);
        return emailTemplateRepository.save(t);
    }

    // ── HTML builder ────────────────────────────────────────────────────

    /**
     * Constructs the full HTML from the template's editable fields and the fixed body skeleton.
     * This is the single source of truth for what gets stored in html_body.
     */
    public String buildHtml(EmailTemplate t) {
        String bodyColor   = nonBlankOr(t.getBodyColor(),   "#333333");
        String footerColor = nonBlankOr(t.getFooterColor(), "#666666");
        String greeting    = nonBlankOr(t.getGreetingText(), "Warm Greetings from Geojit Investments Ltd !");
        String bodyIntro   = nonBlankOr(t.getBodyIntro(),
                "We hope your experience with Geojit Investments Ltd has been pleasant. " +
                "We are herewith sending you your digitally signed contract note (PDF Document).");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n");
        sb.append("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
        sb.append("</head>\n");
        sb.append("<body style=\"font-family: Arial, sans-serif; font-size: 10pt; color: ")
          .append(bodyColor).append(";\">\n");

        if (t.getLogoUrl() != null && !t.getLogoUrl().isBlank()) {
            sb.append("<div style=\"text-align:center;margin-bottom:16px;\">");
            sb.append("<img src=\"").append(t.getLogoUrl())
              .append("\" alt=\"Geojit Logo\" style=\"max-width:200px;\" /></div>\n");
        }

        sb.append("Dear [NAME],\n<br/><br/>\n");
        sb.append(greeting).append("\n<br/><br/>\n");
        sb.append(bodyIntro).append("\n<br/><br/>\n");
        sb.append("To open your attachment you require Adobe Acrobat Reader 6.0 or above or Foxit Reader.\n");
        sb.append("<br/><br/>\n<strong>Instructions for Opening the attachment:-</strong>\n<br/><br/>\n");
        sb.append("1. Click on the attachment provided with this mail. ")
          .append("If you are prompted for a password, please follow the below steps.\n");
        sb.append("<br/><br/>\n");
        sb.append("<strong>INDIVIDUAL</strong> clients may enter the first four characters of your PAN ")
          .append("(in CAPITAL letters) followed by first four characters of your DATE OF BIRTH (DOB) ")
          .append("[in DDMM format] as the password for the PDF attachment.\n");
        sb.append("<br/>\nFor Eg. PAN: BDPBV2015Z and DOB: 31.01.1979 then password will be ")
          .append("<strong>BDPB3101</strong>\n");
        sb.append("<br/><br/>\nFor <strong>NON-INDIVIDUAL</strong> clients you may enter your PAN ")
          .append("(in CAPITAL letters) as the password for the PDF attachment.\n");
        sb.append("<br/>\nFor Eg. PAN: BDPBV2015Z then password will be <strong>BDPBV2015Z</strong>\n");
        sb.append("<br/><br/>\n");
        sb.append("2. To view details regarding the digital signature, please click on the icon of a pen, ")
          .append("on the left hand side frame of Adobe acrobat.\n<br/><br/>\n");
        sb.append("<strong>Security Notice:</strong> We will never ask for your login ID, password, or OTP. ")
          .append("Please refrain from sharing this information with anyone. ")
          .append("Your security is our top priority.\n<br/><br/>\n");
        sb.append("To download Adobe Reader, please visit ")
          .append("<a href=\"http://get.adobe.com/reader/otherversions\">")
          .append("http://get.adobe.com/reader/otherversions</a>\n");
        sb.append("<br/>\nTo download Foxit Reader, please visit ")
          .append("<a href=\"http://www.foxitsoftware.com/downloads/\">")
          .append("http://www.foxitsoftware.com/downloads/</a>\n<br/><br/>\n");
        sb.append("For all queries, kindly contact ")
          .append("<a href=\"mailto:customercare@geojit.com\">customercare@geojit.com</a>.\n");
        sb.append("<br/>\nToll Free No: 1800-571-5501, 1800-103-5501. Paid Line: +91-484-3911777\n");
        sb.append("<br/><br/>\n");
        sb.append("<pre style=\"font-family: monospace; font-size: 9pt; color: ")
          .append(footerColor).append(";\">");
        sb.append("---------------------------------------------------------------------------\n");
        sb.append("The information contained in this electronic message and its attachments (the \"message\")\n");
        sb.append("is intended solely for the addressees and is confidential and privileged.\n");
        sb.append("If you are not the intended recipient, please notify the sender by reply e-mail\n");
        sb.append("and then destroy the message. Any dissemination, distribution, forwarding, copying,\n");
        sb.append("printing or disclosure, either whole or partial, is prohibited and may be unlawful.\n");
        sb.append("Equity/Mutual Fund investments are subject to market risks.\n");
        sb.append("Past performance does not guarantee future returns.\n");
        sb.append("We do not offer any product which gives guaranteed returns.\n");
        sb.append("WARNING: Computer viruses can be transmitted via email. The recipient should check\n");
        sb.append("this email and any attachments for the presence of viruses. The company accepts no\n");
        sb.append("liability for any damage caused by any virus transmitted by this email.\n");
        sb.append("-----------------------------------------------------------------------");
        sb.append("</pre>\n</body>\n</html>\n");

        return sb.toString();
    }

    private String nonBlankOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    // ── S3 seed on startup ───────────────────────────────────────────────

    /**
     * On application startup: if the template HTML file does not yet exist in S3,
     * seed it from the active DB template so the Email Lambda always has something to fetch.
     */
    @Bean
    public ApplicationRunner seedTemplateS3() {
        return args -> {
            try {
                if (!s3Service.templateHtmlExists()) {
                    EmailTemplate active = emailTemplateRepository.findByIsActiveTrue().orElse(null);
                    if (active != null) {
                        s3Service.putTemplateHtml(active.getHtmlBody());
                        log.info("Seeded email template HTML to S3 from DB template '{}'", active.getName());
                    }
                }
            } catch (Exception e) {
                log.warn("Template S3 seed skipped: {}", e.getMessage());
            }
        };
    }
}
