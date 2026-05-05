package com.geojit.contractnote.controller;

import com.geojit.contractnote.dto.response.ApiResponse;
import com.geojit.contractnote.entity.CmsPage;
import com.geojit.contractnote.repository.UserRepository;
import com.geojit.contractnote.service.BrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Configuration - Branding & CMS")
@RequiredArgsConstructor
@Slf4j
public class BrandingController {

    private final BrandingService brandingService;
    private final UserRepository userRepository;

    // ─── Branding ───────────────────────────────────────────────

    @GetMapping("/branding")
    @Operation(summary = "Get branding configuration")
    public ResponseEntity<ApiResponse<Map<String, String>>> getBranding() {
        Map<String, String> config = brandingService.getBrandingConfig();
        return ResponseEntity.ok(ApiResponse.ok("Branding config", config));
    }

    @PutMapping("/branding")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update branding configuration")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateBranding(
            @RequestBody Map<String, String> body) {
        brandingService.updateBrandingConfig(body);
        Map<String, String> updated = brandingService.getBrandingConfig();
        return ResponseEntity.ok(ApiResponse.ok("Branding updated", updated));
    }

    @PostMapping(value = "/branding/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload logo image to S3")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Logo file is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".png") &&
                !filename.toLowerCase().endsWith(".svg") && !filename.toLowerCase().endsWith(".jpg"))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Logo must be PNG, SVG, or JPG"));
        }

        // In a real implementation, this would upload to S3 and return the URL
        String logoUrl = "/api/v1/config/branding/logo/" + filename;
        System.setProperty("BRANDING_LOGO_URL", logoUrl);

        return ResponseEntity.ok(ApiResponse.ok("Logo uploaded", Map.of("url", logoUrl)));
    }

    // ─── CMS Pages ───────────────────────────────────────────────

    @GetMapping("/cms")
    @Operation(summary = "List all CMS pages")
    public ResponseEntity<ApiResponse<List<CmsPage>>> getCmsPages() {
        List<CmsPage> pages = brandingService.getAllCmsPages();
        return ResponseEntity.ok(ApiResponse.ok("CMS pages", pages));
    }

    @PutMapping("/cms/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update CMS page content by slug")
    public ResponseEntity<ApiResponse<CmsPage>> updateCmsPage(
            @PathVariable String slug,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String title = body.get("title");
        String content = body.get("content");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Content cannot be empty"));
        }

        String updatedBy = userDetails.getUsername();
        CmsPage updated = brandingService.updateCmsPage(slug, title, content, updatedBy);

        return ResponseEntity.ok(ApiResponse.ok("CMS page updated", updated));
    }
}
