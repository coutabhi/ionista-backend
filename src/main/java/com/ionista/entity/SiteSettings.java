package com.ionista.entity;

import com.ionista.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "site_settings")
public class SiteSettings extends BaseEntity {

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(length = 200)
    private String tagline;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "logo_public_id", length = 200)
    private String logoPublicId;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @Column(name = "favicon_public_id", length = 200)
    private String faviconPublicId;

    @Column(name = "announcement_bar_enabled", nullable = false)
    @Builder.Default
    private boolean announcementBarEnabled = false;

    @Column(name = "announcement_bar_text", length = 300)
    private String announcementBarText;

    @Column(name = "announcement_bar_link", length = 300)
    private String announcementBarLink;
}
