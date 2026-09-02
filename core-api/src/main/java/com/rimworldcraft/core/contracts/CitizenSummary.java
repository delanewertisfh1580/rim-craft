package com.rimworldcraft.core.contracts;

import com.rimworldcraft.core.api.types.CitizenId;
import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.WorldId;
import java.util.Objects;

/** Minimal citizen projection shared across bounded-context boundaries. */
public record CitizenSummary(WorldId worldId, CitizenId citizenId, ColonyId colonyId, boolean alive) {
    /** Validates a citizen summary. */
    public CitizenSummary {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(citizenId, "citizenId");
        Objects.requireNonNull(colonyId, "colonyId");
    }
}
