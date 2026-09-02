package com.rimworldcraft.core.world;

import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;

/** Result of validating a settlement position against an immutable world snapshot. */
public record SettlementValidationResult(WorldId worldId, GridPosition position, boolean valid, String reason) {
    public SettlementValidationResult {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(position, "position");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason");
    }
}
