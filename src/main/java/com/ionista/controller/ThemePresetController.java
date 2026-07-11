package com.ionista.controller;

import com.ionista.dto.request.ThemePresetRequest;
import com.ionista.dto.response.ThemePresetResponse;
import com.ionista.service.ThemePresetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/themes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ThemePresetController {

    private final ThemePresetService themePresetService;

    @GetMapping
    public ResponseEntity<List<ThemePresetResponse>> listAll() {
        return ResponseEntity.ok(themePresetService.listAll());
    }

    @PostMapping
    public ResponseEntity<ThemePresetResponse> create(@Valid @RequestBody ThemePresetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(themePresetService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThemePresetResponse> update(@PathVariable Long id, @Valid @RequestBody ThemePresetRequest request) {
        return ResponseEntity.ok(themePresetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        themePresetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ThemePresetResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(themePresetService.activate(id));
    }
}
