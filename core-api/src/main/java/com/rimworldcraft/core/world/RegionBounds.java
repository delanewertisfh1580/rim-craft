package com.rimworldcraft.core.world;

import com.rimworldcraft.core.shared.GridPosition;
import java.util.Objects;

/** Inclusive, platform-neutral bounds for one world region. */
public record RegionBounds(GridPosition minimum, GridPosition maximum) {
    public RegionBounds {
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(maximum, "maximum");
        if (minimum.x() > maximum.x() || minimum.y() > maximum.y() || minimum.z() > maximum.z()) {
            throw new IllegalArgumentException("minimum must not exceed maximum");
        }
    }

    public boolean contains(GridPosition position) {
        Objects.requireNonNull(position, "position");
        return position.x() >= minimum.x() && position.x() <= maximum.x()
                && position.y() >= minimum.y() && position.y() <= maximum.y()
                && position.z() >= minimum.z() && position.z() <= maximum.z();
    }
}
