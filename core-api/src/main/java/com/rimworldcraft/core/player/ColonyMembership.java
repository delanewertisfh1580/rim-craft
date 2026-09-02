package com.rimworldcraft.core.player;

import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.GameTick;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;

/** A player's membership in one colony within one world. */
public record ColonyMembership(WorldId worldId, ColonyId colonyId, GameTick joinedAt) {
    public ColonyMembership {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(joinedAt, "joinedAt");
    }
}
