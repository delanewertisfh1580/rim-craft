# Building System compliance report

## Status

**PARTIAL / JVM MVP.** The Building Context now has validated blueprint/build-order domain models and application boundaries without Minecraft dependencies.

## Implemented

- Immutable config-backed `Blueprint` with positive dimensions, non-negative costs, block definitions, and estimated duration.
- `BuildOrder` lifecycle: pending, reserved, in-progress, completed, cancelled, failed.
- Immutable `GhostBlock` projection.
- Placement, collision, bounds, and block-type validation port.
- Colony-owned resource reservation port.
- Citizen assignment and progress tracking.
- Completion, cancellation, and failure transitions.
- Idempotent resource-result application using result IDs.
- Platform-neutral world mutation intent port.
- `BuildingUseCases` and constructor-injected `BuildingApplicationService`.
- Build-order repository contract.
- Config-backed blueprint fixture.
- Unit and contract tests for documented invariants.

## Boundary guarantees

- Building Core does not import Minecraft or Forge classes.
- Resource reservations are delegated through a port; Building does not own Colony inventory.
- World changes are emitted as intents rather than performed directly.
- Completed orders cannot be cancelled.
- Failed/cancelled/completed orders cannot progress.
- Duplicate resource results are ignored.
- Oversized, colliding, or invalid-block placements are rejected before order creation.

## Known limitations

Full JSON blueprint loader, durable repository adapter, typed domain events, complete resource reservation transaction semantics, and production context handler integration remain pending. Existing legacy Building classes are retained for compatibility.

## Verification

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```

No Minecraft runtime or external credentials were added.
