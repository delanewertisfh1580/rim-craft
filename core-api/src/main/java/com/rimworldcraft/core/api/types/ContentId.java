package com.rimworldcraft.core.api.types;

import java.util.Objects;

/** Identifies namespaced content loaded from configuration. */
public record ContentId(String value) {
    /** Creates a content identifier. */
    public ContentId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || !value.contains(":")) {
            throw new IllegalArgumentException("ContentId must be namespaced");
        }
    }
}
