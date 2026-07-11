package com.ionista.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSettingsResponse {

    private String storeName;
    private String tagline;
    private String logoUrl;
    private String faviconUrl;
    private boolean announcementBarEnabled;
    private String announcementBarText;
    private String announcementBarLink;
    private ThemePresetResponse activeTheme;
}
