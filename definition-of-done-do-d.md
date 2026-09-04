# Definition of Done

A change is Done only when behavior, boundaries, tests and documentation agree. “It compiles locally” or “runtime later” is not evidence for an unimplemented feature.

## Universal checklist

- [ ] Scope and owner are identified.
- [ ] Existing contracts and compatibility constraints were inspected.
- [ ] Core remains platform-independent.
- [ ] Invariants, validation, errors and idempotency are explicit.
- [ ] New/changed behavior has focused tests, including failure paths.
- [ ] Changed ports/adapters have contract or integration coverage where active.
- [ ] Config/schema/save/event contracts and migrations are updated when applicable.
- [ ] No public mutable state, swallowed exceptions, unbounded work or hidden time/random.
- [ ] Relevant documentation and links are synchronized.
- [ ] Verification commands and results are recorded.

## Task-specific

- Feature: acceptance behavior and compatibility are defined.
- Bug fix: regression test reproduces the root cause.
- Refactor: behavior is preserved and migration risk is explicit.
- Documentation: examples match actual code or are labeled DESIGN-ONLY.
- Runtime integration: requires active module, dependency coordinates and adapter tests; design text alone does not satisfy Done.

## Current project gates

```bash
./gradlew :core-api:test :infrastructure-common:test --no-daemon
./gradlew :core-impl:test --no-daemon
./gradlew check --no-daemon
```

The focused API/configuration tests pass. Full `check` is currently BLOCKED by `ArchitectureTest.java` API mismatch; do not mark architecture work Done until it is fixed.

## References

- [`AGENTS.md`](AGENTS.md)
- [`testing-strategy.md`](testing-strategy.md)
- [`architecture-test-report.md`](architecture-test-report.md)
- [`documentation-consistency-report.md`](documentation-consistency-report.md)
