package com.ionista.repository;

import com.ionista.entity.ThemePreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThemePresetRepository extends JpaRepository<ThemePreset, Long> {

    Optional<ThemePreset> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}
