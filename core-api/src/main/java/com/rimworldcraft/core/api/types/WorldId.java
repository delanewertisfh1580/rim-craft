package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies a world and prevents accidental cross-world operations. */
public record WorldId(UUID value) {
    /** Creates a world identifier. */
    public WorldId { Objects.requireNonNull(value, "value"); }
}
