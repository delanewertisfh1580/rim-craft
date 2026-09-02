# Player Context compliance report

## Status

**PARTIAL / JVM MVP.** Player Context is implemented as a platform-neutral, server-authoritative Core boundary. Authentication, packet decoding, client projection, and Minecraft runtime integration remain outside the active modules.

## Implemented

- Canonical shared `PlayerId` and `WorldId` identity boundary.
- Immutable `PlayerProfile` aggregate with:
  - world-scoped `ColonyMembership`;
  - `Permission` set;
  - `ControlMode`;
  - `PlayerPreferences`;
  - `ProgressionState`;
  - selected colony/citizen IDs;
  - immutable command audit receipts;
  - aggregate version.
- Explicit driving ports for registration, joining/leaving colonies, command authorization, control-mode changes, permission changes, target selection, and read views.
- Driven ports for profile persistence, public colony membership authorization, and Player event publication.
- Constructor-injected `PlayerApplicationService`.
- Server-side permission and membership checks.
- Command idempotency: replayed accepted/rejected `CommandId` values return the previous decision and do not publish duplicate events.
- Leaving a colony cleans a selection pointing to that colony.
- Permission changes require `MANAGE_COLONY` and replace the target permission set atomically.
- Player views contain IDs and projections only; no Colony or Citizen aggregate is stored.
- In-memory repository for deterministic JVM verification.

## Security boundary guarantees

- Authentication is not implemented in Core; the external adapter supplies a normalized `PlayerId`.
- Commands are rejected when the player is not registered in the requested world.
- Colony-scoped authorization requires both the requested permission and membership in the requested colony.
- The Player Context never calls a Colony aggregate or owns Colony inventory.
- The event port publishes immutable Player facts only.
- All simulation timestamps come from injected `ClockPort` rather than system time.

## Evidence matrix

| Requirement | Evidence | Status |
|---|---|---|
| `PlayerId` | `core-api/.../shared/PlayerId.java` | DONE |
| Player profile | `core-api/.../player/PlayerProfile.java` | DONE |
| Membership and selection | `ColonyMembership`, `PlayerSelection`, `PlayerProfile` | DONE |
| Permissions and control mode | `Permission`, `ControlMode`, `PlayerApplicationService` | DONE |
| Preferences and progression | `PlayerPreferences`, `ProgressionState` | DONE |
| Command audit/idempotency | `PlayerCommandRecord`, `PlayerCommandResult`, service replay path | DONE |
| Register/join/leave use cases | `core-api/.../ports/driving/*Player*UseCase.java` | DONE |
| Server-side authorization | `AuthorizePlayerCommandUseCase`, service authorization checks | DONE |
| Read model | `GetPlayerViewUseCase`, `PlayerView` | DONE |
| Unauthorized player | `PlayerContextTest` | DONE |
| Wrong colony | `PlayerContextTest.nonMemberCannotAuthorizeCommandForWrongColony` | DONE |
| Replay command | `PlayerContextTest.replayReturnsOriginalDecisionAndDoesNotPublishAgain` | DONE |
| Invalid identity/world scope | `PlayerContextTest.registrationUsesNormalizedIdentityAndRejectsInvalidIdentity` | DONE |
| Leaving and selection cleanup | `PlayerContextTest.leavingColonyCleansSelection` | DONE |
| Permission changes | `PlayerContextTest.permissionChangesAreServerSideAndAffectSubsequentCommands` | DONE |
| Authentication adapter | Not in active JVM modules | PENDING |
| Packet-to-command validation | Runtime network adapter not active | PENDING |
| Durable profile persistence | In-memory repository only | PENDING |
| Cross-context membership event handlers | Event bootstrap not active | PENDING |
| Client/network projection | Runtime module not active | PENDING |

## Verification

```bash
./gradlew :core-impl:test --tests com.rimworldcraft.core.player.PlayerContextTest --no-daemon
```

Result: **BUILD SUCCESSFUL** — 6 tests passed.

The full active build should be run after this documentation update. Active modules remain `core-api`, `core-impl`, and `infrastructure-common`; no Forge/Minecraft dependency was added.

## Remaining gaps

- Add a Player snapshot mapper and durable adapter using the existing JSON persistence contract.
- Add packet normalization and server-side distance, ownership, target-existence, rate-limit, replay, and stale-revision checks in the later multiplayer stage.
- Add event-envelope adaptation and membership projection handlers in the event bootstrap stage.
- Add integration/acceptance tests against the eventual server adapter; current tests are JVM unit/application-boundary tests.
