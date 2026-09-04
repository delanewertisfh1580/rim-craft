# Building compliance report

**Status: PARTIAL.**

## Implemented

- Validated immutable `Blueprint`.
- `BuildOrder` lifecycle with terminal-state guards.
- Immutable `GhostBlock` projection.
- Placement, collision, bounds and block-type validation ports.
- Resource reservation and world-mutation intent boundaries.
- Constructor-injected application service and repository contract.
- Focused invariant/config tests.

## Boundary guarantees

Building Core does not import Minecraft, own Colony inventory or perform block mutation. Duplicate resource results are ignored; completed/failed/cancelled orders cannot progress.

## Pending

Full blueprint JSON loader, durable repository, complete reservation transaction semantics, typed event integration and production handler/adapters.

## Verification

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```
