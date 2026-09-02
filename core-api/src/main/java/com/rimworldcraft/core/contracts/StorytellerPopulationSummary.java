package com.rimworldcraft.core.contracts;

import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;

/** Minimal population facts; it is not an NPC aggregate or collection. */
public record StorytellerPopulationSummary(WorldId worldId, int livingCitizens, int incapacitatedCitizens) {
    public StorytellerPopulationSummary {
        Objects.requireNonNull(worldId, "worldId");
        if (livingCitizens < 0 || incapacitatedCitizens < 0 || incapacitatedCitizens > livingCitizens) {
            throw new IllegalArgumentException("invalid population summary");
        }
    }
}
