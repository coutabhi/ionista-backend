package com.ionista.service;

import com.ionista.dto.request.SiteSettingsRequest;
import com.ionista.dto.response.SiteSettingsResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SiteSettingsService {

    SiteSettingsResponse get();

    SiteSettingsResponse update(SiteSettingsRequest request);

    SiteSettingsResponse uploadLogo(MultipartFile file);

    SiteSettingsResponse uploadFavicon(MultipartFile file);
}
