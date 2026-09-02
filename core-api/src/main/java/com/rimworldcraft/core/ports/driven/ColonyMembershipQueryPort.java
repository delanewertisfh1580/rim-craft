package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;

/** Read-only boundary for validating a player's requested colony membership. */
public interface ColonyMembershipQueryPort {
    boolean mayJoin(WorldId worldId, ColonyId colonyId, PlayerId playerId);
}
