package com.rimworldcraft.core.player;

import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.GameTick;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;

/** Immutable facts emitted by Player Context. */
public sealed interface PlayerEvent permits PlayerEvent.ProfileRegistered, PlayerEvent.JoinedColony,
        PlayerEvent.LeftColony, PlayerEvent.CommandAuthorized, PlayerEvent.CommandRejected,
        PlayerEvent.ControlModeChanged, PlayerEvent.SelectionChanged, PlayerEvent.PermissionsChanged {
    WorldId worldId();
    PlayerId playerId();
    GameTick occurredAt();

    record ProfileRegistered(WorldId worldId, PlayerId playerId, GameTick occurredAt) implements PlayerEvent { }
    record JoinedColony(WorldId worldId, PlayerId playerId, ColonyId colonyId, CommandId commandId,
                        GameTick occurredAt) implements PlayerEvent { }
    record LeftColony(WorldId worldId, PlayerId playerId, ColonyId colonyId, CommandId commandId,
                      GameTick occurredAt) implements PlayerEvent { }
    record CommandAuthorized(WorldId worldId, PlayerId playerId, CommandId commandId,
                             String operation, ColonyId colonyId, CitizenId citizenId,
                             GameTick occurredAt) implements PlayerEvent { }
    record CommandRejected(WorldId worldId, PlayerId playerId, CommandId commandId,
                           String operation, String reason, GameTick occurredAt) implements PlayerEvent { }
    record ControlModeChanged(WorldId worldId, PlayerId playerId, ControlMode controlMode,
                              CommandId commandId, GameTick occurredAt) implements PlayerEvent { }
    record SelectionChanged(WorldId worldId, PlayerId playerId, PlayerSelection selection,
                            CommandId commandId, GameTick occurredAt) implements PlayerEvent { }
    record PermissionsChanged(WorldId worldId, PlayerId playerId, java.util.Set<Permission> permissions,
                              CommandId commandId, GameTick occurredAt) implements PlayerEvent {
        public PermissionsChanged {
            permissions = java.util.Set.copyOf(permissions);
        }
    }
}
