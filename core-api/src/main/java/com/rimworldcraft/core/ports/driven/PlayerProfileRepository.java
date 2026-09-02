package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.player.PlayerProfile;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Optional;

/** Repository contract for world-scoped Player profiles. */
public interface PlayerProfileRepository {
    Optional<PlayerProfile> find(WorldId worldId, PlayerId playerId);
    PlayerProfile save(PlayerProfile profile);
}
