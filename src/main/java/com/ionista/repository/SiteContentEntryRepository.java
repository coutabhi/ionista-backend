package com.ionista.repository;

import com.ionista.entity.SiteContentEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteContentEntryRepository extends JpaRepository<SiteContentEntry, Long> {

    Optional<SiteContentEntry> findByContentKey(String contentKey);
}
