package com.ionista.service;

import com.ionista.dto.request.ThemePresetRequest;
import com.ionista.dto.response.ThemePresetResponse;

import java.util.List;

public interface ThemePresetService {

    List<ThemePresetResponse> listAll();

    ThemePresetResponse create(ThemePresetRequest request);

    ThemePresetResponse update(Long id, ThemePresetRequest request);

    void delete(Long id);

    ThemePresetResponse activate(Long id);
}
