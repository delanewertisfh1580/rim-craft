package com.rimworldcraft.core.player;

import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.GameTick;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/** Aggregate root for player-owned authority and preferences. */
public record PlayerProfile(PlayerId playerId, WorldId worldId, Set<ColonyMembership> memberships,
                            Set<Permission> permissions, ControlMode controlMode,
                            PlayerPreferences preferences, ProgressionState progression,
                            PlayerSelection selection, List<PlayerCommandRecord> commandAudit,
                            long aggregateVersion) {
    public PlayerProfile {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(memberships, "memberships");
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(controlMode, "controlMode");
        Objects.requireNonNull(preferences, "preferences");
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(commandAudit, "commandAudit");
        if (aggregateVersion < 0) throw new IllegalArgumentException("aggregateVersion must be >= 0");
        if (memberships.stream().anyMatch(membership -> !worldId.equals(membership.worldId()))) {
            throw new IllegalArgumentException("membership belongs to another world");
        }
        if (selection != null && memberships.stream().noneMatch(m -> m.colonyId().equals(selection.colonyId()))) {
            throw new IllegalArgumentException("selection must refer to a member colony");
        }
        memberships = Set.copyOf(memberships);
        permissions = Set.copyOf(permissions);
        commandAudit = List.copyOf(commandAudit);
    }

    public static PlayerProfile register(PlayerId playerId, WorldId worldId) {
        return new PlayerProfile(playerId, worldId, Set.of(), Set.of(Permission.VIEW_COLONY),
                ControlMode.OBSERVER, PlayerPreferences.defaults(), ProgressionState.empty(), null, List.of(), 0);
    }

    public boolean isMember(ColonyId colonyId) {
        Objects.requireNonNull(colonyId, "colonyId");
        return memberships.stream().anyMatch(membership -> membership.colonyId().equals(colonyId));
    }

    public boolean has(Permission permission) {
        return permissions.contains(Objects.requireNonNull(permission, "permission"));
    }

    public Optional<PlayerCommandRecord> receipt(CommandId commandId) {
        return commandAudit.stream().filter(record -> record.commandId().equals(commandId)).findFirst();
    }

    public PlayerProfile join(ColonyMembership membership) {
        Objects.requireNonNull(membership, "membership");
        if (!worldId.equals(membership.worldId())) throw new IllegalArgumentException("world mismatch");
        if (isMember(membership.colonyId())) return this;
        Set<ColonyMembership> updated = new HashSet<>(memberships);
        updated.add(membership);
        return copy(updated, permissions, controlMode, preferences, progression, selection, commandAudit);
    }

    public PlayerProfile leave(ColonyId colonyId) {
        Objects.requireNonNull(colonyId, "colonyId");
        Set<ColonyMembership> updated = new HashSet<>(memberships);
        updated.removeIf(membership -> membership.colonyId().equals(colonyId));
        PlayerSelection updatedSelection = selection != null && selection.colonyId().equals(colonyId) ? null : selection;
        return copy(updated, permissions, controlMode, preferences, progression, updatedSelection, commandAudit);
    }

    public PlayerProfile withPermissions(Set<Permission> updatedPermissions) {
        return copy(memberships, Objects.requireNonNull(updatedPermissions, "updatedPermissions"),
                controlMode, preferences, progression, selection, commandAudit);
    }

    public PlayerProfile withControlMode(ControlMode updatedMode) {
        return copy(memberships, permissions, Objects.requireNonNull(updatedMode, "updatedMode"),
                preferences, progression, selection, commandAudit);
    }

    public PlayerProfile select(PlayerSelection updatedSelection) {
        if (updatedSelection != null && !isMember(updatedSelection.colonyId())) {
            throw new IllegalArgumentException("cannot select a non-member colony");
        }
        return copy(memberships, permissions, controlMode, preferences, progression, updatedSelection, commandAudit);
    }

    public PlayerProfile record(PlayerCommandRecord command) {
        Objects.requireNonNull(command, "command");
        if (receipt(command.commandId()).isPresent()) throw new IllegalArgumentException("command already recorded");
        List<PlayerCommandRecord> updated = new java.util.ArrayList<>(commandAudit);
        updated.add(command);
        return copy(memberships, permissions, controlMode, preferences, progression, selection, updated);
    }

    private PlayerProfile copy(Set<ColonyMembership> newMemberships, Set<Permission> newPermissions,
                               ControlMode newMode, PlayerPreferences newPreferences,
                               ProgressionState newProgression, PlayerSelection newSelection,
                               List<PlayerCommandRecord> newAudit) {
        return new PlayerProfile(playerId, worldId, newMemberships, newPermissions, newMode,
                newPreferences, newProgression, newSelection, newAudit, aggregateVersion + 1);
    }
}
