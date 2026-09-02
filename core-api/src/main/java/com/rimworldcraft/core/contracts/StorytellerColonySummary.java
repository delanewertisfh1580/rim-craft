package com.rimworldcraft.core.contracts;

import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;

/** Minimal colony facts needed for incident eligibility and scaling. */
public record StorytellerColonySummary(WorldId worldId, ColonyId colonyId, int population,
                                       long wealth, int morale, boolean active) {
    public StorytellerColonySummary {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(colonyId, "colonyId");
        if (population < 0 || wealth < 0 || morale < 0 || morale > 100) {
            throw new IllegalArgumentException("invalid colony summary");
        }
    }
}
