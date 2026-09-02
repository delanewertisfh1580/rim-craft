package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies a player at the application boundary. */
public record PlayerId(UUID value) {
    /** Creates a player identifier. */
    public PlayerId {
        Objects.requireNonNull(value, "value");
    }

    /** Creates an identifier from an external UUID boundary value. */
    public static PlayerId fromUuid(UUID value) {
        return new PlayerId(value);
    }
}
