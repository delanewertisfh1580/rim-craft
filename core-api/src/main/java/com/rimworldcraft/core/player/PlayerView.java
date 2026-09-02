package com.rimworldcraft.core.player;

import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable Player read model; it contains IDs and summaries, never aggregate copies. */
public record PlayerView(PlayerId playerId, WorldId worldId, Set<ColonyId> colonyIds,
                         Set<Permission> permissions, ControlMode controlMode,
                         PlayerPreferences preferences, ProgressionState progression,
                         ColonyId selectedColonyId, CitizenId selectedCitizenId,
                         List<PlayerCommandRecord> recentCommands, long aggregateVersion) {
    public PlayerView {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(worldId, "worldId");
        colonyIds = Set.copyOf(Objects.requireNonNull(colonyIds, "colonyIds"));
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        Objects.requireNonNull(controlMode, "controlMode");
        Objects.requireNonNull(preferences, "preferences");
        Objects.requireNonNull(progression, "progression");
        recentCommands = List.copyOf(Objects.requireNonNull(recentCommands, "recentCommands"));
        if (aggregateVersion < 0) throw new IllegalArgumentException("aggregateVersion must be >= 0");
    }

    public Optional<ColonyId> selectedColony() { return Optional.ofNullable(selectedColonyId); }
    public Optional<CitizenId> selectedCitizen() { return Optional.ofNullable(selectedCitizenId); }
}
