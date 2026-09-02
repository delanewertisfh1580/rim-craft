package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.player.PlayerView;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;

public interface GetPlayerViewUseCase {
    PlayerView get(WorldId worldId, PlayerId playerId);
}
