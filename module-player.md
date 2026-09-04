# Player context

**Status: PARTIAL.** Player authority is implemented as a normalized JVM boundary. Authentication, packet decoding, durable storage and client projections are not active.

## Ownership

Player owns world-scoped memberships, permissions, control mode, preferences, progression, selection and command receipts. External adapters own authentication and connection identity. Colony owns colony state.

## Current code

- Models: `core-api/src/main/java/com/rimworldcraft/core/player/`.
- Service: `core-impl/src/main/java/com/rimworldcraft/core/player/PlayerApplicationService.java`.
- Ports: `core.ports.driving` and `core.ports.driven`.
- Tests: `core-impl/src/test/java/com/rimworldcraft/core/player/PlayerContextTest.java`.

## Implemented behavior

- Immutable `PlayerProfile` and nested values.
- World-scoped membership/selection checks.
- Server-side permission checks.
- Replay-safe accepted/rejected `CommandId` receipts.
- Selection cleanup when leaving a colony.
- Constructor-injected application service and deterministic clock.

## Not complete

Authentication adapter, packet normalization/bounds checks, distance/ownership/rate/stale-revision validation, durable profile mapper, event projection handlers, network/UI integration and audit retention.

## Security rules

Derive actor identity from the server connection, never from client payload. Validate world, membership, permission, target and command replay before mutation. The client is never authoritative.

## References

- [`bounded-contexts.md`](bounded-contexts.md)
- [`entity-integration.md`](entity-integration.md)
- [`player-compliance-report.md`](player-compliance-report.md)
