package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies a world region. */
public record RegionId(UUID value) {
    /** Creates a region identifier. */
    public RegionId { Objects.requireNonNull(value, "value"); }
}
