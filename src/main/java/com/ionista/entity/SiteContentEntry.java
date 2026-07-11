package com.ionista.entity;

import com.ionista.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "site_content_entries")
public class SiteContentEntry extends BaseEntity {

    @Column(name = "content_key", nullable = false, unique = true, length = 100)
    private String contentKey;

    @Lob
    @Column(name = "content_value")
    private String contentValue;
}
