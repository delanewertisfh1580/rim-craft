package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.contracts.StorytellerColonySummary;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Optional;

/** Provides minimal colony facts without exposing the Colony aggregate. */
public interface StorytellerColonySummaryPort {
    Optional<StorytellerColonySummary> find(WorldId worldId, ColonyId colonyId);
}
