package com.rimworldcraft.core.colony;

import com.rimworldcraft.core.api.types.Position;
import com.rimworldcraft.core.api.types.ZoneType;
import java.util.Objects;
import java.util.UUID;

/** Immutable circular colony zone. */
public record Zone(UUID id, ZoneType type, Position center, int radius) {
    /** Validates a zone. */
    public Zone {
        Objects.requireNonNull(id); Objects.requireNonNull(type); Objects.requireNonNull(center);
        if (radius < 0) throw new IllegalArgumentException("radius must not be negative");
    }
    /** Returns whether the position belongs to this zone. */
    public boolean contains(Position position) { return center.distance(position) <= radius; }
}
