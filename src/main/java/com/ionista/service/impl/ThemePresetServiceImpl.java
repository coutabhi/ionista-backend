package com.ionista.service.impl;

import com.ionista.dto.request.ThemePresetRequest;
import com.ionista.dto.response.ThemePresetResponse;
import com.ionista.entity.ThemePreset;
import com.ionista.exception.ConflictException;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.ThemePresetRepository;
import com.ionista.service.ThemePresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThemePresetServiceImpl implements ThemePresetService {

    private final ThemePresetRepository themePresetRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ThemePresetResponse> listAll() {
        return themePresetRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ThemePresetResponse create(ThemePresetRequest request) {
        if (themePresetRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("A theme named '" + request.getName() + "' already exists");
        }

        boolean makeActive = themePresetRepository.count() == 0;

        ThemePreset theme = ThemePreset.builder()
                .name(request.getName())
                .primaryColor(request.getPrimaryColor())
                .secondaryColor(request.getSecondaryColor())
                .accentColor(request.getAccentColor())
                .backgroundColor(request.getBackgroundColor())
                .textColor(request.getTextColor())
                .active(makeActive)
                .build();

        return toResponse(themePresetRepository.save(theme));
    }

    @Override
    @Transactional
    public ThemePresetResponse update(Long id, ThemePresetRequest request) {
        ThemePreset theme = findTheme(id);

        if (request.getName() != null && !request.getName().equalsIgnoreCase(theme.getName())
                && themePresetRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("A theme named '" + request.getName() + "' already exists");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            theme.setName(request.getName());
        }
        if (request.getPrimaryColor() != null) {
            theme.setPrimaryColor(request.getPrimaryColor());
        }
        if (request.getSecondaryColor() != null) {
            theme.setSecondaryColor(request.getSecondaryColor());
        }
        if (request.getAccentColor() != null) {
            theme.setAccentColor(request.getAccentColor());
        }
        if (request.getBackgroundColor() != null) {
            theme.setBackgroundColor(request.getBackgroundColor());
        }
        if (request.getTextColor() != null) {
            theme.setTextColor(request.getTextColor());
        }

        return toResponse(themePresetRepository.save(theme));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ThemePreset theme = findTheme(id);
        if (theme.isActive()) {
            throw new ConflictException("Cannot delete the currently active theme");
        }
        themePresetRepository.delete(theme);
    }

    @Override
    @Transactional
    public ThemePresetResponse activate(Long id) {
        ThemePreset theme = findTheme(id);
        themePresetRepository.findByActiveTrue().ifPresent(current -> {
            if (!current.getId().equals(id)) {
                current.setActive(false);
                themePresetRepository.save(current);
            }
        });
        theme.setActive(true);
        return toResponse(themePresetRepository.save(theme));
    }

    private ThemePreset findTheme(Long id) {
        return themePresetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found with id: " + id));
    }

    private ThemePresetResponse toResponse(ThemePreset theme) {
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
