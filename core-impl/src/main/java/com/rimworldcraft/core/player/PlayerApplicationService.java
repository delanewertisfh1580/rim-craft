package com.rimworldcraft.core.player;

import com.rimworldcraft.core.ports.driven.ClockPort;
import com.rimworldcraft.core.ports.driven.ColonyMembershipQueryPort;
import com.rimworldcraft.core.ports.driven.PlayerEventPort;
import com.rimworldcraft.core.ports.driven.PlayerProfileRepository;
import com.rimworldcraft.core.ports.driving.AuthorizePlayerCommandUseCase;
import com.rimworldcraft.core.ports.driving.ChangeControlModeUseCase;
import com.rimworldcraft.core.ports.driving.ChangePlayerPermissionsUseCase;
import com.rimworldcraft.core.ports.driving.GetPlayerViewUseCase;
import com.rimworldcraft.core.ports.driving.JoinColonyUseCase;
import com.rimworldcraft.core.ports.driving.LeaveColonyUseCase;
import com.rimworldcraft.core.ports.driving.PlayerApplicationUseCases;
import com.rimworldcraft.core.ports.driving.RegisterPlayerUseCase;
import com.rimworldcraft.core.ports.driving.SelectPlayerTargetUseCase;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.GameTick;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Application boundary for normalized, server-side Player commands.
 * Authentication and packet validation stay outside this service.
 */
public final class PlayerApplicationService implements PlayerApplicationUseCases {
    private final PlayerProfileRepository profiles;
    private final ColonyMembershipQueryPort colonyMembership;
    private final ClockPort clock;
    private final PlayerEventPort events;

    public PlayerApplicationService(PlayerProfileRepository profiles,
                                    ColonyMembershipQueryPort colonyMembership,
                                    ClockPort clock,
                                    PlayerEventPort events) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.colonyMembership = Objects.requireNonNull(colonyMembership, "colonyMembership");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.events = Objects.requireNonNull(events, "events");
    }

    @Override
    public PlayerCommandResult register(RegisterPlayerUseCase.Command command) {
        require(command);
        Optional<PlayerProfile> existing = profiles.find(command.worldId(), command.playerId());
        if (existing.isPresent()) {
            return replayOrReject(existing.get(), command.commandId(), "REGISTER", "player is already registered");
        }
        PlayerProfile profile = PlayerProfile.register(command.playerId(), command.worldId());
        profile = profile.record(acceptedRecord(command.commandId(), "REGISTER", tick(command.worldId())));
        profiles.save(profile);
        events.publish(new PlayerEvent.ProfileRegistered(command.worldId(), command.playerId(), tick(command.worldId())));
        return accepted(command.commandId(), "player registered");
    }

    @Override
    public PlayerCommandResult join(JoinColonyUseCase.Command command) {
        require(command);
        PlayerProfile profile = find(command.worldId(), command.playerId());
        PlayerCommandResult replay = replay(profile, command.commandId());
        if (replay != null) return replay;
        if (!colonyMembership.mayJoin(command.worldId(), command.colonyId(), command.playerId())) {
            return rejectAndRecord(profile, command.commandId(), "JOIN_COLONY", "membership is not authorized");
        }
        profile = profile.join(new ColonyMembership(command.worldId(), command.colonyId(), tick(command.worldId())));
        profile = profile.record(acceptedRecord(command.commandId(), "JOIN_COLONY", tick(command.worldId())));
        profiles.save(profile);
        events.publish(new PlayerEvent.JoinedColony(command.worldId(), command.playerId(), command.colonyId(),
                command.commandId(), tick(command.worldId())));
        return accepted(command.commandId(), "colony joined");
    }

    @Override
    public PlayerCommandResult leave(LeaveColonyUseCase.Command command) {
        require(command);
        PlayerProfile profile = find(command.worldId(), command.playerId());
        PlayerCommandResult replay = replay(profile, command.commandId());
        if (replay != null) return replay;
        if (!profile.isMember(command.colonyId())) {
            return rejectAndRecord(profile, command.commandId(), "LEAVE_COLONY", "player is not a colony member");
        }
        profile = profile.leave(command.colonyId()).record(acceptedRecord(command.commandId(), "LEAVE_COLONY", tick(command.worldId())));
        profiles.save(profile);
        events.publish(new PlayerEvent.LeftColony(command.worldId(), command.playerId(), command.colonyId(),
                command.commandId(), tick(command.worldId())));
        return accepted(command.commandId(), "colony left");
    }

    @Override
    public PlayerCommandResult authorize(AuthorizePlayerCommandUseCase.Command command) {
        require(command);
        PlayerProfile profile = findOrNull(command.worldId(), command.playerId());
        if (profile == null) return rejected(command.commandId(), "player is not registered");
        PlayerCommandResult replay = replay(profile, command.commandId());
        if (replay != null) return replay;
        String reason = authorizationFailure(profile, command.permission(), command.colonyId());
        if (reason != null) return rejectAndRecord(profile, command.commandId(), command.operation(), reason);
        profile = profile.record(acceptedRecord(command.commandId(), command.operation(), tick(command.worldId())));
        profiles.save(profile);
        events.publish(new PlayerEvent.CommandAuthorized(command.worldId(), command.playerId(), command.commandId(),
                command.operation(), command.colonyId(), command.citizenId(), tick(command.worldId())));
        return accepted(command.commandId(), "command authorized");
    }

    @Override
    public PlayerCommandResult change(ChangeControlModeUseCase.Command command) {
        require(command);
        PlayerProfile profile = find(command.worldId(), command.playerId());
        PlayerCommandResult replay = replay(profile, command.commandId());
        if (replay != null) return replay;
        if (!profile.has(Permission.CHANGE_CONTROL_MODE)) {
            return rejectAndRecord(profile, command.commandId(), "CHANGE_CONTROL_MODE", "permission denied");
        }
        profile = profile.withControlMode(command.controlMode()).record(acceptedRecord(command.commandId(), "CHANGE_CONTROL_MODE", tick(command.worldId())));
        profiles.save(profile);
        events.publish(new PlayerEvent.ControlModeChanged(command.worldId(), command.playerId(), command.controlMode(),
                command.commandId(), tick(command.worldId())));
        return accepted(command.commandId(), "control mode changed");
    }

    @Override
    public PlayerCommandResult select(SelectPlayerTargetUseCase.Command command) {
        require(command);
        PlayerProfile profile = find(command.worldId(), command.playerId());
        PlayerCommandResult replay = replay(profile, command.commandId());
        if (replay != null) return replay;
        if (!profile.isMember(command.colonyId())) {
            return rejectAndRecord(profile, command.commandId(), "SELECT_TARGET", "player is not a colony member");
        }
        PlayerSelection selection = command.citizenId() == null
                ? PlayerSelection.forColony(command.colonyId())
                : new PlayerSelection(command.colonyId(), command.citizenId());
        profile = profile.select(selection).record(acceptedRecord(command.commandId(), "SELECT_TARGET", tick(command.worldId())));
        profiles.save(profile);
        events.publish(new PlayerEvent.SelectionChanged(command.worldId(), command.playerId(), selection,
                command.commandId(), tick(command.worldId())));
        return accepted(command.commandId(), "selection changed");
    }

    @Override
    public PlayerCommandResult change(ChangePlayerPermissionsUseCase.Command command) {
        require(command);
        PlayerProfile actor = find(command.worldId(), command.actorPlayerId());
        PlayerProfile target = find(command.worldId(), command.targetPlayerId());
        PlayerCommandResult replay = replay(actor, command.commandId());
        if (replay != null) return replay;
        if (!actor.has(Permission.MANAGE_COLONY)) {
            return rejectAndRecord(actor, command.commandId(), "CHANGE_PERMISSIONS", "permission denied");
        }
        target = target.withPermissions(nonNullPermissions(command.permissions()));
        actor = actor.record(acceptedRecord(command.commandId(), "CHANGE_PERMISSIONS", tick(command.worldId())));
        profiles.save(target);
        profiles.save(actor);
        events.publish(new PlayerEvent.PermissionsChanged(command.worldId(), command.targetPlayerId(),
                target.permissions(), command.commandId(), tick(command.worldId())));
        return accepted(command.commandId(), "permissions changed");
    }

    @Override
    public PlayerView get(WorldId worldId, PlayerId playerId) {
        PlayerProfile profile = find(worldId, playerId);
        return new PlayerView(profile.playerId(), profile.worldId(),
                profile.memberships().stream().map(ColonyMembership::colonyId).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                profile.permissions(), profile.controlMode(), profile.preferences(), profile.progression(),
                profile.selection() == null ? null : profile.selection().colonyId(),
                profile.selection() == null ? null : profile.selection().citizenId(),
                profile.commandAudit(), profile.aggregateVersion());
    }

    private String authorizationFailure(PlayerProfile profile, Permission permission, ColonyId colonyId) {
        if (permission == null) return "permission is required";
        if (!profile.has(permission)) return "permission denied";
        if (colonyId != null && !profile.isMember(colonyId)) return "player is not a member of the requested colony";
        return null;
    }

    private PlayerProfile find(WorldId worldId, PlayerId playerId) {
        return profiles.find(Objects.requireNonNull(worldId, "worldId"), Objects.requireNonNull(playerId, "playerId"))
                .orElseThrow(() -> new IllegalArgumentException("player is not registered in this world"));
    }

    private PlayerProfile findOrNull(WorldId worldId, PlayerId playerId) {
        return profiles.find(Objects.requireNonNull(worldId, "worldId"), Objects.requireNonNull(playerId, "playerId")).orElse(null);
    }

    private static void require(Object command) { Objects.requireNonNull(command, "command"); }
    private GameTick tick(WorldId worldId) { return Objects.requireNonNull(clock.currentTick(worldId), "clock returned null"); }
    private static Set<Permission> nonNullPermissions(Set<Permission> permissions) {
        return Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
    }

    private PlayerCommandResult replay(PlayerProfile profile, CommandId commandId) {
        Objects.requireNonNull(commandId, "commandId");
        return profile.receipt(commandId).map(record -> record.status() == PlayerCommandRecord.CommandStatus.ACCEPTED
                ? acceptedReplay(commandId, record.reason())
                : rejectedReplay(commandId, record.reason())).orElse(null);
    }

    private PlayerCommandResult replayOrReject(PlayerProfile profile, CommandId commandId, String operation, String reason) {
        PlayerCommandResult prior = replay(profile, commandId);
        if (prior != null) return prior;
        return rejected(commandId, reason);
    }

    private PlayerCommandResult rejectAndRecord(PlayerProfile profile, CommandId commandId, String operation, String reason) {
        profile = profile.record(new PlayerCommandRecord(commandId, operation, PlayerCommandRecord.CommandStatus.REJECTED,
                reason, tick(profile.worldId())));
        profiles.save(profile);
        events.publish(new PlayerEvent.CommandRejected(profile.worldId(), profile.playerId(), commandId, operation,
                reason, tick(profile.worldId())));
        return rejected(commandId, reason);
    }

    private static PlayerCommandRecord acceptedRecord(CommandId commandId, String operation, GameTick processedAt) {
        return new PlayerCommandRecord(commandId, operation, PlayerCommandRecord.CommandStatus.ACCEPTED, "accepted", processedAt);
    }

    private static PlayerCommandResult accepted(CommandId commandId, String reason) {
        return new PlayerCommandResult(commandId, PlayerCommandResult.Status.ACCEPTED, reason);
    }
    private static PlayerCommandResult rejected(CommandId commandId, String reason) {
        return new PlayerCommandResult(commandId, PlayerCommandResult.Status.REJECTED, reason);
    }
    private static PlayerCommandResult acceptedReplay(CommandId commandId, String reason) {
        return new PlayerCommandResult(commandId, PlayerCommandResult.Status.REPLAYED_ACCEPTED, reason);
    }
    private static PlayerCommandResult rejectedReplay(CommandId commandId, String reason) {
        return new PlayerCommandResult(commandId, PlayerCommandResult.Status.REPLAYED_REJECTED, reason);
    }
}
