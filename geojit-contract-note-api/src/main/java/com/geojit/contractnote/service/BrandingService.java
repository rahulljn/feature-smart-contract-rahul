package com.geojit.contractnote.service;

import com.geojit.contractnote.entity.CmsPage;
import com.geojit.contractnote.repository.CmsPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrandingService {

    private final CmsPageRepository cmsPageRepository;

    // Default branding config - stored as environment variables or in a config table
    private static final String DEFAULT_APP_NAME = "Contract Note Platform";
    private static final String DEFAULT_PRIMARY_COLOR = "#00174b";
    private static final String DEFAULT_ACCENT_COLOR = "#497cff";

    public Map<String, String> getBrandingConfig() {
        return Map.of(
                "appName", System.getenv().getOrDefault("BRANDING_APP_NAME", DEFAULT_APP_NAME),
                "primaryColor", System.getenv().getOrDefault("BRANDING_PRIMARY_COLOR", DEFAULT_PRIMARY_COLOR),
                "accentColor", System.getenv().getOrDefault("BRANDING_ACCENT_COLOR", DEFAULT_ACCENT_COLOR),
                "logoUrl", System.getenv().getOrDefault("BRANDING_LOGO_URL", "")
        );
    }

    public void updateBrandingConfig(Map<String, String> config) {
        // In a real implementation, this would persist to an app_config table
        // For now, we log and update environment
        if (config.get("appName") != null) {
            System.setProperty("BRANDING_APP_NAME", config.get("appName"));
        }
        if (config.get("primaryColor") != null) {
            System.setProperty("BRANDING_PRIMARY_COLOR", config.get("primaryColor"));
        }
        if (config.get("accentColor") != null) {
            System.setProperty("BRANDING_ACCENT_COLOR", config.get("accentColor"));
        }
    }

    public List<CmsPage> getAllCmsPages() {
        return cmsPageRepository.findAll();
    }

    public Optional<CmsPage> getCmsPage(String slug) {
        return cmsPageRepository.findBySlug(slug);
    }

    @Transactional
    public CmsPage updateCmsPage(String slug, String title, String content, String updatedBy) {
        CmsPage page = cmsPageRepository.findBySlug(slug)
                .orElseGet(() -> CmsPage.builder()
                        .slug(slug)
                        .title(title)
                        .content(content)
                        .build());
        page.setTitle(title);
        page.setContent(content);
        page.setLastUpdatedBy(updatedBy);
        return cmsPageRepository.save(page);
    }
}
