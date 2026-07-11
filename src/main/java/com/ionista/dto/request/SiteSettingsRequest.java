package com.ionista.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSettingsRequest {

    @Size(max = 100, message = "Store name cannot exceed 100 characters")
    private String storeName;

    @Size(max = 200, message = "Tagline cannot exceed 200 characters")
    private String tagline;

    private Boolean announcementBarEnabled;

    @Size(max = 300, message = "Announcement text cannot exceed 300 characters")
    private String announcementBarText;

    @Size(max = 300, message = "Announcement link cannot exceed 300 characters")
    private String announcementBarLink;
}
