package com.ionista.service.impl;

import com.ionista.entity.SiteContentEntry;
import com.ionista.repository.SiteContentEntryRepository;
import com.ionista.service.SiteContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteContentServiceImpl implements SiteContentService {

    private final SiteContentEntryRepository siteContentEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getAll() {
        return siteContentEntryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        SiteContentEntry::getContentKey,
                        entry -> entry.getContentValue() == null ? "" : entry.getContentValue(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Transactional
    public Map<String, String> update(Map<String, String> updates) {
        updates.forEach((key, value) -> {
            SiteContentEntry entry = siteContentEntryRepository.findByContentKey(key)
                    .orElseGet(() -> SiteContentEntry.builder().contentKey(key).build());
            entry.setContentValue(value);
            siteContentEntryRepository.save(entry);
        });
        return getAll();
    }
}
