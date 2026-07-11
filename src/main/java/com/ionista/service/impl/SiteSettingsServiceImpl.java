package com.ionista.service.impl;

import com.ionista.dto.request.SiteSettingsRequest;
import com.ionista.dto.response.ImageUploadResult;
import com.ionista.dto.response.SiteSettingsResponse;
import com.ionista.dto.response.ThemePresetResponse;
import com.ionista.entity.SiteSettings;
import com.ionista.entity.ThemePreset;
import com.ionista.repository.SiteSettingsRepository;
import com.ionista.repository.ThemePresetRepository;
import com.ionista.service.CloudinaryService;
import com.ionista.service.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SiteSettingsServiceImpl implements SiteSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final SiteSettingsRepository siteSettingsRepository;
    private final ThemePresetRepository themePresetRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public SiteSettingsResponse get() {
        return toResponse(loadOrCreate());
    }

    @Override
    @Transactional
    public SiteSettingsResponse update(SiteSettingsRequest request) {
        SiteSettings settings = loadOrCreate();

        if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
            settings.setStoreName(request.getStoreName());
        }
        if (request.getTagline() != null) {
            settings.setTagline(request.getTagline());
        }
        if (request.getAnnouncementBarEnabled() != null) {
            settings.setAnnouncementBarEnabled(request.getAnnouncementBarEnabled());
        }
        if (request.getAnnouncementBarText() != null) {
            settings.setAnnouncementBarText(request.getAnnouncementBarText());
        }
        if (request.getAnnouncementBarLink() != null) {
            settings.setAnnouncementBarLink(request.getAnnouncementBarLink());
        }

        return toResponse(siteSettingsRepository.save(settings));
    }

    @Override
    @Transactional
    public SiteSettingsResponse uploadLogo(MultipartFile file) {
        SiteSettings settings = loadOrCreate();
        if (settings.getLogoPublicId() != null) {
            cloudinaryService.delete(settings.getLogoPublicId());
        }
        ImageUploadResult result = cloudinaryService.upload(file, "ionista/branding/logo");
        settings.setLogoUrl(result.getUrl());
        settings.setLogoPublicId(result.getPublicId());
        return toResponse(siteSettingsRepository.save(settings));
    }

    @Override
    @Transactional
    public SiteSettingsResponse uploadFavicon(MultipartFile file) {
        SiteSettings settings = loadOrCreate();
        if (settings.getFaviconPublicId() != null) {
            cloudinaryService.delete(settings.getFaviconPublicId());
        }
        ImageUploadResult result = cloudinaryService.upload(file, "ionista/branding/favicon");
        settings.setFaviconUrl(result.getUrl());
        settings.setFaviconPublicId(result.getPublicId());
        return toResponse(siteSettingsRepository.save(settings));
    }

    private SiteSettings loadOrCreate() {
        return siteSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(() -> siteSettingsRepository.save(
                        SiteSettings.builder()
                                .storeName("Ionista")
                                .tagline("Contemporary kurtis, made slowly in Jaipur.")
                                .announcementBarEnabled(false)
                                .build()
                ));
    }

    private SiteSettingsResponse toResponse(SiteSettings settings) {
        ThemePresetResponse activeTheme = themePresetRepository.findByActiveTrue()
                .map(this::toThemeResponse)
                .orElse(null);

        return SiteSettingsResponse.builder()
                .storeName(settings.getStoreName())
                .tagline(settings.getTagline())
                .logoUrl(settings.getLogoUrl())
                .faviconUrl(settings.getFaviconUrl())
                .announcementBarEnabled(settings.isAnnouncementBarEnabled())
                .announcementBarText(settings.getAnnouncementBarText())
                .announcementBarLink(settings.getAnnouncementBarLink())
                .activeTheme(activeTheme)
                .build();
    }

    private ThemePresetResponse toThemeResponse(ThemePreset theme) {
        return ThemePresetResponse.builder()
                .id(theme.getId())
                .name(theme.getName())
                .primaryColor(theme.getPrimaryColor())
                .secondaryColor(theme.getSecondaryColor())
                .accentColor(theme.getAccentColor())
                .backgroundColor(theme.getBackgroundColor())
                .textColor(theme.getTextColor())
                .active(theme.isActive())
                .build();
    }
}
