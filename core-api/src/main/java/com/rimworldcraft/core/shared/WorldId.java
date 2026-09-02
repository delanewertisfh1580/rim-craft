package com.rimworldcraft.core.shared;

import java.util.Objects;
import java.util.UUID;

/** Canonical shared-kernel world identifier. */
public record WorldId(UUID value) {
    /** Creates a world identifier. */
    public WorldId { Objects.requireNonNull(value, "value"); }
    /** Converts this identifier to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.WorldId toApiType() {
        return new com.rimworldcraft.core.api.types.WorldId(value);
    }
}
