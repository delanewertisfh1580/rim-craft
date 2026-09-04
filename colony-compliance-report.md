# Colony compliance report

**Status: PARTIAL.**

## Implemented evidence

| Capability | Evidence | Status |
|---|---|---|
| Typed Colony/World identity | `core-impl/.../core/colony/Colony.java` | IMPLEMENTED |
| Validated name/site | `core.shared.ColonyName`, `SettlementSite` | IMPLEMENTED |
| Membership invariants | `ColonyInvariantTest` | IMPLEMENTED |
| Resource atomicity | `ColonyInvariantTest` | IMPLEMENTED |
| Application boundary | `ColonyApplicationService`, `ColonyUseCases` | PARTIAL |
| Repository contract | `core.ports.driven.ColonyRepository` | IMPLEMENTED |
| Typed event publication | `ColonyEventEnvelope`; legacy events remain | PARTIAL |
| Full colony policy/persistence/outbox | not active | PLANNED |
| NBT/Minecraft runtime | not in active build | BLOCKED |

## Constraints

Colony owns resources and membership. It must not mutate Citizen internals or Minecraft objects directly. New code uses typed IDs, `GridPosition` and target ports.

## Verification

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```

The command is subject to the current `ArchitectureTest.java` compile blocker when the full `core-impl:test` task recompiles tests.

## Next work

Complete full policy model, command/event idempotency, snapshot persistence and runtime adapters.
