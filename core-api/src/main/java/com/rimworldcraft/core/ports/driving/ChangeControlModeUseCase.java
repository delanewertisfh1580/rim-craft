package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.player.ControlMode;
import com.rimworldcraft.core.player.PlayerCommandResult;
import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;

public interface ChangeControlModeUseCase {
    PlayerCommandResult change(Command command);
    record Command(WorldId worldId, PlayerId playerId, CommandId commandId, ControlMode controlMode) { }
}
