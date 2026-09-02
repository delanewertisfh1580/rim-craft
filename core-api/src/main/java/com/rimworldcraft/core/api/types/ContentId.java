package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.regex.Pattern;

/** Identifies namespaced content loaded from configuration. */
public record ContentId(String value) {
    private static final Pattern FORMAT = Pattern.compile("[a-z][a-z0-9_./-]{0,31}:[a-z][a-z0-9_./-]{0,63}");

    /** Creates a validated namespaced content identifier. */
    public ContentId {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("ContentId must match namespace:path format");
        }
    }
}
