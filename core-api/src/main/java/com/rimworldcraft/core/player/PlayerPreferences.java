package com.rimworldcraft.core.player;

import java.util.Objects;

/** Immutable preferences owned by Player Context. */
public record PlayerPreferences(boolean notificationsEnabled, String locale) {
    public PlayerPreferences {
        if (locale == null || locale.isBlank()) {
            throw new IllegalArgumentException("locale must not be blank");
        }
        locale = locale.trim();
    }

    public static PlayerPreferences defaults() {
        return new PlayerPreferences(true, "en_us");
    }
}
