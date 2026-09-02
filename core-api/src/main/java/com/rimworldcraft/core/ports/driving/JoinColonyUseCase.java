package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.player.PlayerCommandResult;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;

public interface JoinColonyUseCase {
    PlayerCommandResult join(Command command);
    record Command(WorldId worldId, PlayerId playerId, ColonyId colonyId, CommandId commandId) { }
}
