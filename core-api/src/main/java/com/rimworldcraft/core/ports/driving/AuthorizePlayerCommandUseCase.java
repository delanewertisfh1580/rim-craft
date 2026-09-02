package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.player.Permission;
import com.rimworldcraft.core.player.PlayerCommandResult;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;

public interface AuthorizePlayerCommandUseCase {
    PlayerCommandResult authorize(Command command);

    record Command(WorldId worldId, PlayerId playerId, CommandId commandId, String operation,
                   Permission permission, ColonyId colonyId, CitizenId citizenId) { }
}
