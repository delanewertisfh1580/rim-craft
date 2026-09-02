package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.api.types.CitizenId;
import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.PlayerId;
import com.rimworldcraft.core.api.types.WorldId;

/** Adds a citizen to a colony through the Colony application boundary. */
public interface AddCitizenUseCase {
    /** Adds a citizen membership and returns the member identifier. */
    CitizenId add(AddCitizenCommand command);

    /** Immutable input for adding colony membership. */
    record AddCitizenCommand(WorldId worldId, ColonyId colonyId, CitizenId citizenId, PlayerId actorId) { }
}
