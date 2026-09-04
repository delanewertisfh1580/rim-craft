# Testing strategy

## Current scope

The active build is a Java 17 JVM skeleton. Core tests must not require Minecraft. Forge/Fabric, network, Cucumber, performance and stress testing remain future layers until their modules/tasks are enabled.

## Test levels

| Level | Use now | Rule |
|---|---|---|
| Unit | active | domain invariants, policies, services, value objects; deterministic clock/random |
| Contract | partial | ports, repositories and adapters when an implementation is active |
| Integration | limited | JSON/config/persistence boundaries in active modules |
| Runtime/acceptance | planned | Minecraft, packets, entities, real paths and user flows |
| Performance/stress | planned | bounded tick/planner/event/save load |

## Required behavior coverage

Test both success and failure paths: null/invalid values, duplicate membership, insufficient resources, terminal Citizen state, world mismatch, replayed command/event, unknown schema, corruption, future versions, handler failure and bounded planner behavior.

## Test doubles

Prefer fakes/stubs for domain behavior. Use mocks/spies only when interaction with a boundary is the contract. Inject clock/random/world observations; never use system time or uncontrolled randomness in deterministic tests.

## Active test locations

- `core-api/src/test/java/...` — shared API/value tests.
- `core-impl/src/test/java/...` — context, service, event and architecture tests.
- `infrastructure-common/src/test/java/...` — configuration and JSON persistence tests.
- Inactive runtime tests: `test-core`, `test-integration`, `infrastructure-forge`.

## Verification commands

```bash
./gradlew :core-api:test :infrastructure-common:test --no-daemon
./gradlew :core-impl:test --no-daemon
./gradlew check --no-daemon
```

The first command passes. The `core-impl` test compilation and full `check` are currently blocked by the ArchUnit API mismatch in `ArchitectureTest.java`.

## When adding a feature

1. Add the smallest behavior test that fails before implementation.
2. Add negative/boundary/replay tests.
3. Add contract/integration tests only for active boundaries.
4. Record deterministic inputs and expected diagnostics.
5. Update the owning status/compliance document.

## References

- [`AGENTS.md`](AGENTS.md)
- [`definition-of-done-do-d.md`](definition-of-done-do-d.md)
- [`configuration-mutation-testing.md`](configuration-mutation-testing.md)
- [`event-system-api.md`](event-system-api.md)
