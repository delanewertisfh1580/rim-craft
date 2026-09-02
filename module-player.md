# RimWorldCraft — Player Context

## Scope

Player Context owns normalized player authority within a `WorldId`: colony membership, permissions, control mode, preferences, progression, selection, and command audit/idempotency. Authentication remains the responsibility of the external server/platform adapter.

## Canonical model

Package: `com.rimworldcraft.core.player`.

- `PlayerProfile` — immutable aggregate root;
- `ColonyMembership` — world-scoped membership projection;
- `Permission`, `ControlMode`, `PlayerPreferences`, `ProgressionState` — immutable policy/state values;
- `PlayerSelection` — selected colony and optional `CitizenId`, never an aggregate copy;
- `PlayerCommandRecord` — immutable accepted/rejected command receipt;
- `PlayerView` — read-only adapter projection;
- `PlayerEvent` — typed Player-owned facts.

The canonical identity is `com.rimworldcraft.core.shared.PlayerId`. Platform UUIDs are normalized before entering Core.

## Application boundary

`PlayerApplicationService` is constructor-injected with:

- `PlayerProfileRepository`;
- `ColonyMembershipQueryPort`;
- `ClockPort`;
- `PlayerEventPort`.

Driving contracts are in `com.rimworldcraft.core.ports.driving`:

- `RegisterPlayerUseCase`;
- `JoinColonyUseCase`;
- `LeaveColonyUseCase`;
- `AuthorizePlayerCommandUseCase`;
- `ChangeControlModeUseCase`;
- `ChangePlayerPermissionsUseCase`;
- `SelectPlayerTargetUseCase`;
- `GetPlayerViewUseCase`.

The combined `PlayerApplicationUseCases` interface is a composition-root convenience; individual use-case contracts remain the public boundary.

## Security and invariants

1. A command must carry normalized `PlayerId`, `WorldId`, and `CommandId`.
2. Every authorization decision is made server-side by Player Context.
3. An unregistered player cannot authorize commands.
4. A colony-scoped command requires both the requested permission and membership in the requested colony.
5. Join is accepted only when the public `ColonyMembershipQueryPort` authorizes it.
6. Replayed `CommandId` values return the prior accepted/rejected result and do not publish a second event.
7. Leaving a colony removes membership and clears a selection pointing to that colony.
8. A selection may reference only a member colony and stores IDs, not a `Colony` aggregate.
9. Control mode changes require `CHANGE_CONTROL_MODE`.
10. Permission changes require an actor with `MANAGE_COLONY`; target permissions replace the target set atomically.
11. Profiles and all nested collections are immutable at the publication boundary.
12. Domain code does not authenticate Minecraft accounts, inspect packets, or mutate platform entities.

## Events

`PlayerEvent` contains immutable typed facts for:

- profile registration;
- joining and leaving a colony;
- command authorization/rejection;
- control-mode changes;
- selection changes;
- permission changes.

The event port does not expose repositories or foreign aggregates. Runtime event-envelope adaptation is a later boundary.

## Evidence

- `core-api/src/main/java/com/rimworldcraft/core/player/`
- `core-api/src/main/java/com/rimworldcraft/core/ports/driving/`
- `core-api/src/main/java/com/rimworldcraft/core/ports/driven/PlayerProfileRepository.java`
- `core-api/src/main/java/com/rimworldcraft/core/ports/driven/ColonyMembershipQueryPort.java`
- `core-api/src/main/java/com/rimworldcraft/core/ports/driven/PlayerEventPort.java`
- `core-impl/src/main/java/com/rimworldcraft/core/player/PlayerApplicationService.java`
- `core-impl/src/main/java/com/rimworldcraft/core/player/InMemoryPlayerProfileRepository.java`
- `core-impl/src/test/java/com/rimworldcraft/core/player/PlayerContextTest.java`
- `player-compliance-report.md`

## Pending runtime work

The active build intentionally does not include Minecraft/Forge/Fabric. The following are Pending rather than claimed complete:

- authentication and packet-to-command adapter;
- durable profile snapshot mapper/adapter;
- event-envelope bootstrap and cross-context membership handlers;
- network distance, ownership, target-existence, rate, and stale-revision validation;
- client projections and UI integration;
- production repository and audit retention policy.
