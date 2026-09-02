package com.rimworldcraft.core.shared;

import java.util.Objects;
import java.util.UUID;

/** Canonical shared-kernel region identifier. */
public record RegionId(UUID value) {
    /** Creates a region identifier. */
    public RegionId { Objects.requireNonNull(value, "value"); }
    /** Converts this identifier to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.RegionId toApiType() {
        return new com.rimworldcraft.core.api.types.RegionId(value);
    }
}
