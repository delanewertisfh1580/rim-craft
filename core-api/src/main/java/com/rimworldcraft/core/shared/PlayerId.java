package com.rimworldcraft.core.shared;

import java.util.Objects;
import java.util.UUID;

/** Canonical shared-kernel player identifier. */
public record PlayerId(UUID value) {
    /** Creates a player identifier. */
    public PlayerId { Objects.requireNonNull(value, "value"); }
    /** Converts this identifier to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.PlayerId toApiType() {
        return new com.rimworldcraft.core.api.types.PlayerId(value);
    }
}
