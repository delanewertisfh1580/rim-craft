package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.api.types.PlayerId;
import com.rimworldcraft.core.api.types.WorldId;

/** Creates a colony through the Colony application boundary. */
public interface CreateColonyUseCase {
    /** Creates a colony after authorization and settlement validation. */
    ColonyId create(CreateColonyCommand command);

    /** Immutable input for colony creation. */
    record CreateColonyCommand(WorldId worldId, PlayerId playerId, String name, GridPosition position) { }
}
