package com.rimworldcraft.core.shared;

import java.util.Objects;
import java.util.UUID;

/** Canonical shared-kernel colony identifier. */
public record ColonyId(UUID value) {
    /** Creates a colony identifier. */
    public ColonyId { Objects.requireNonNull(value, "value"); }
    /** Converts this identifier to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.ColonyId toApiType() {
        return new com.rimworldcraft.core.api.types.ColonyId(value);
    }
}
