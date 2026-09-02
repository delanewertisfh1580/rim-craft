package com.rimworldcraft.core.shared;

import java.util.Objects;

/** Immutable, validated colony name. */
public record ColonyName(String value) {
    /** Validates and creates a colony name. */
    public ColonyName {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("Colony name must contain 1-64 non-blank characters");
        }
    }
}
