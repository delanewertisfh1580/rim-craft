package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies a colony within a world. */
public record ColonyId(UUID value) {
    /** Creates a colony identifier. */
    public ColonyId {
        Objects.requireNonNull(value, "value");
    }

    /** Creates an identifier from an external UUID boundary value. */
    public static ColonyId fromUuid(UUID value) {
        return new ColonyId(value);
    }
}
