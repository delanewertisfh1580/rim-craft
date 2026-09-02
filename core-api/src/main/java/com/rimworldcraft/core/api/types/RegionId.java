package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies a world region. */
public record RegionId(UUID value) {
    /** Creates a region identifier. */
    public RegionId {
        Objects.requireNonNull(value, "value");
    }

    /** Creates an identifier from an external UUID boundary value. */
    public static RegionId fromUuid(UUID value) {
        return new RegionId(value);
    }
}
