package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies a player at the application boundary. */
public record PlayerId(UUID value) {
    /** Creates a player identifier. */
    public PlayerId { Objects.requireNonNull(value, "value"); }
}
