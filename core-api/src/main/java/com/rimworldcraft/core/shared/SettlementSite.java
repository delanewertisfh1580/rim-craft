package com.rimworldcraft.core.shared;

import java.util.Objects;

/** Immutable settlement location scoped to one world. */
public record SettlementSite(WorldId worldId, GridPosition position) {
    /** Validates the world scope and position. */
    public SettlementSite {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(position, "position");
    }
}
