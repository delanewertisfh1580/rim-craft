package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies a citizen independently from a platform entity. */
public record CitizenId(UUID value) {
    /** Creates a citizen identifier. */
    public CitizenId {
        Objects.requireNonNull(value, "value");
    }

    /** Creates an identifier from an external UUID boundary value. */
    public static CitizenId fromUuid(UUID value) {
        return new CitizenId(value);
    }
}
