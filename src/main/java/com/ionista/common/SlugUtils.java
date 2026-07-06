package com.ionista.common;

import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        String slug = normalized.replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "item" : slug;
    }
}
