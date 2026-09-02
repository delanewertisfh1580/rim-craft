package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.contracts.StorytellerPopulationSummary;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Optional;

/** Provides population facts without exposing NPC aggregates. */
public interface StorytellerPopulationSummaryPort {
    Optional<StorytellerPopulationSummary> find(WorldId worldId);
}
