package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.request.TemplateFieldsRequest;
import com.geojit.contractnote.dto.request.TemplateRequest;
import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.dto.response.S3TemplateResponse;
import com.geojit.contractnote.entity.*;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Email Templates")
@RequiredArgsConstructor
public class TemplateController {

    private final EmailTemplateService    emailTemplateService;
    private final UserRepository          userRepository;

    @GetMapping
    @Operation(summary = "List all email templates from S3")
    public ResponseEntity<ApiResponse<List<S3TemplateResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(emailTemplateService.listFromS3()));
    }

    @GetMapping("/{name}/content")
    @Operation(summary = "Get template HTML content from S3 by name")
    public ResponseEntity<ApiResponse<String>> getContent(@PathVariable String name) {
        return ResponseEntity.ok(ApiResponse.ok(emailTemplateService.getContentFromS3(name)));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active email template")
    public ResponseEntity<ApiResponse<EmailTemplate>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok(emailTemplateService.getActive()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a template by ID")
    public ResponseEntity<ApiResponse<EmailTemplate>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(emailTemplateService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Create a new email template")
    public ResponseEntity<ApiResponse<EmailTemplate>> create(
            @Valid @RequestBody TemplateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Template created", emailTemplateService.create(req, user)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Update an email template (full HTML)")
    public ResponseEntity<ApiResponse<EmailTemplate>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok("Template updated", emailTemplateService.update(id, req, user)));
    }

    @PutMapping("/{id}/fields")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @Operation(summary = "Update only the restricted editable fields of a template")
    public ResponseEntity<ApiResponse<EmailTemplate>> updateFields(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateFieldsRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok("Template fields updated",
                emailTemplateService.updateFields(id, req, user)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a template (deactivates all others)")
    public ResponseEntity<ApiResponse<EmailTemplate>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Template activated", emailTemplateService.activate(id)));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate HTML structure and required placeholders ([NAME])")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(emailTemplateService.validate(id)));
    }

}
