package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.player.Permission;
import com.rimworldcraft.core.player.PlayerCommandResult;
import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Set;

public interface ChangePlayerPermissionsUseCase {
    PlayerCommandResult change(Command command);
    record Command(WorldId worldId, PlayerId actorPlayerId, PlayerId targetPlayerId,
                   CommandId commandId, Set<Permission> permissions) { }
}
