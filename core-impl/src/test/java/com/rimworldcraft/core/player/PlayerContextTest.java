package com.rimworldcraft.core.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rimworldcraft.core.ports.driven.ClockPort;
import com.rimworldcraft.core.ports.driven.ColonyMembershipQueryPort;
import com.rimworldcraft.core.ports.driven.PlayerEventPort;
import com.rimworldcraft.core.ports.driving.AuthorizePlayerCommandUseCase;
import com.rimworldcraft.core.ports.driving.ChangeControlModeUseCase;
import com.rimworldcraft.core.ports.driving.ChangePlayerPermissionsUseCase;
import com.rimworldcraft.core.ports.driving.JoinColonyUseCase;
import com.rimworldcraft.core.ports.driving.LeaveColonyUseCase;
import com.rimworldcraft.core.ports.driving.RegisterPlayerUseCase;
import com.rimworldcraft.core.ports.driving.SelectPlayerTargetUseCase;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.CommandId;
import com.rimworldcraft.core.shared.GameTick;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit, contract, and application-boundary tests for Player Context. */
class PlayerContextTest {
    private static final WorldId WORLD = new WorldId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final WorldId OTHER_WORLD = new WorldId(UUID.fromString("00000000-0000-0000-0000-000000000102"));
    private static final PlayerId PLAYER = new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
    private static final PlayerId ADMIN = new PlayerId(UUID.fromString("00000000-0000-0000-0000-000000000202"));
    private static final ColonyId COLONY = new ColonyId(UUID.fromString("00000000-0000-0000-0000-000000000301"));
    private static final ColonyId OTHER_COLONY = new ColonyId(UUID.fromString("00000000-0000-0000-0000-000000000302"));
    private static final CitizenId CITIZEN = new CitizenId(UUID.fromString("00000000-0000-0000-0000-000000000401"));

    private InMemoryPlayerProfileRepository repository;
    private RecordingEvents events;
    private PlayerApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPlayerProfileRepository();
        events = new RecordingEvents();
        ClockPort clock = world -> new GameTick(42);
        ColonyMembershipQueryPort membership = (world, colony, player) -> WORLD.equals(world)
                && COLONY.equals(colony);
        service = new PlayerApplicationService(repository, membership, clock, events);
    }

    @Test
    void registrationUsesNormalizedIdentityAndRejectsInvalidIdentity() {
        PlayerCommandResult result = service.register(new RegisterPlayerUseCase.Command(WORLD, PLAYER, command(1)));
        assertTrue(result.accepted());
        assertThrows(NullPointerException.class,
                () -> service.register(new RegisterPlayerUseCase.Command(WORLD, null, command(2))));
        assertThrows(IllegalArgumentException.class,
                () -> service.get(OTHER_WORLD, PLAYER));
    }

    @Test
    void nonMemberCannotAuthorizeCommandForWrongColony() {
        service.register(new RegisterPlayerUseCase.Command(WORLD, PLAYER, command(3)));
        repository.save(repository.find(WORLD, PLAYER).orElseThrow()
                .withPermissions(Set.of(Permission.VIEW_COLONY, Permission.ASSIGN_WORK)));
        PlayerCommandResult result = service.authorize(new AuthorizePlayerCommandUseCase.Command(
                WORLD, PLAYER, command(4), "ASSIGN_WORK", Permission.ASSIGN_WORK, OTHER_COLONY, CITIZEN));
        assertEquals(PlayerCommandResult.Status.REJECTED, result.status());
        assertTrue(result.reason().contains("not a member"));
    }

    @Test
    void replayReturnsOriginalDecisionAndDoesNotPublishAgain() {
        service.register(new RegisterPlayerUseCase.Command(WORLD, PLAYER, command(5)));
        CommandId joinCommand = command(6);
        PlayerCommandResult first = service.join(new JoinColonyUseCase.Command(WORLD, PLAYER, COLONY, joinCommand));
        int eventCount = events.events.size();
        PlayerCommandResult replay = service.join(new JoinColonyUseCase.Command(WORLD, PLAYER, COLONY, joinCommand));
        assertEquals(PlayerCommandResult.Status.ACCEPTED, first.status());
        assertEquals(PlayerCommandResult.Status.REPLAYED_ACCEPTED, replay.status());
        assertEquals(eventCount, events.events.size());
    }

    @Test
    void leavingColonyCleansSelection() {
        service.register(new RegisterPlayerUseCase.Command(WORLD, PLAYER, command(7)));
        service.join(new JoinColonyUseCase.Command(WORLD, PLAYER, COLONY, command(8)));
        service.select(new SelectPlayerTargetUseCase.Command(WORLD, PLAYER, command(9), COLONY, CITIZEN));
        assertEquals(COLONY, service.get(WORLD, PLAYER).selectedColony().orElseThrow());
        PlayerCommandResult left = service.leave(new LeaveColonyUseCase.Command(WORLD, PLAYER, COLONY, command(10)));
        assertTrue(left.accepted());
        PlayerView view = service.get(WORLD, PLAYER);
        assertTrue(view.colonyIds().isEmpty());
        assertFalse(view.selectedColony().isPresent());
        assertFalse(view.selectedCitizen().isPresent());
    }

    @Test
    void permissionChangesAreServerSideAndAffectSubsequentCommands() {
        service.register(new RegisterPlayerUseCase.Command(WORLD, ADMIN, command(11)));
        service.register(new RegisterPlayerUseCase.Command(WORLD, PLAYER, command(12)));
        PlayerProfile admin = repository.find(WORLD, ADMIN).orElseThrow()
                .withPermissions(Set.of(Permission.VIEW_COLONY, Permission.MANAGE_COLONY));
        repository.save(admin);
        PlayerCommandResult changed = service.change(new ChangePlayerPermissionsUseCase.Command(
                WORLD, ADMIN, PLAYER, command(13), Set.of(Permission.VIEW_COLONY, Permission.ASSIGN_WORK)));
        assertTrue(changed.accepted());
        PlayerCommandResult authorized = service.authorize(new AuthorizePlayerCommandUseCase.Command(
                WORLD, PLAYER, command(14), "ASSIGN_WORK", Permission.ASSIGN_WORK, null, CITIZEN));
        assertTrue(authorized.accepted());
    }

    @Test
    void controlModeRequiresPermission() {
        service.register(new RegisterPlayerUseCase.Command(WORLD, PLAYER, command(15)));
        PlayerCommandResult denied = service.change(new ChangeControlModeUseCase.Command(
                WORLD, PLAYER, command(16), ControlMode.CITIZEN));
        assertEquals(PlayerCommandResult.Status.REJECTED, denied.status());
        PlayerProfile profile = repository.find(WORLD, PLAYER).orElseThrow()
                .withPermissions(Set.of(Permission.VIEW_COLONY, Permission.CHANGE_CONTROL_MODE));
        repository.save(profile);
        PlayerCommandResult accepted = service.change(new ChangeControlModeUseCase.Command(
                WORLD, PLAYER, command(17), ControlMode.CITIZEN));
        assertTrue(accepted.accepted());
        assertEquals(ControlMode.CITIZEN, service.get(WORLD, PLAYER).controlMode());
    }

    private static CommandId command(long value) {
        return new CommandId(new UUID(0, value));
    }

    private static final class RecordingEvents implements PlayerEventPort {
        private final List<PlayerEvent> events = new ArrayList<>();

        @Override
        public void publish(PlayerEvent event) {
            events.add(event);
        }
    }
}
