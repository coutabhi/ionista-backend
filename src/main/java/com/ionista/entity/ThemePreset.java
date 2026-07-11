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
@Table(name = "theme_presets")
public class ThemePreset extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "primary_color", nullable = false, length = 20)
    private String primaryColor;

    @Column(name = "secondary_color", nullable = false, length = 20)
    private String secondaryColor;

    @Column(name = "accent_color", nullable = false, length = 20)
    private String accentColor;

    @Column(name = "background_color", nullable = false, length = 20)
    private String backgroundColor;

    @Column(name = "text_color", nullable = false, length = 20)
    private String textColor;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;
}
