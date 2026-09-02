package com.rimworldcraft.core.contracts;

import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.WorldId;
import java.util.Objects;

/** Minimal colony projection shared across bounded-context boundaries. */
public record ColonySummary(WorldId worldId, ColonyId colonyId, int population, long value) {
    /** Validates a colony summary. */
    public ColonySummary {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(colonyId, "colonyId");
        if (population < 0) throw new IllegalArgumentException("population must not be negative");
        if (value < 0) throw new IllegalArgumentException("value must not be negative");
    }
}
