# Drift detection and CI policy

## Current reality

The repository has Gradle configuration for Java compilation, JUnit tests, configuration checks and an ArchUnit task. It does not currently contain an active CI workflow or the proposed `docsCheck`, `integrationTest`, `acceptanceTest`, `migrationTest`, JaCoCo, Checkstyle or SpotBugs tasks.

## Current gates

```bash
./gradlew :core-api:test :infrastructure-common:test --no-daemon
./gradlew :core-impl:test --no-daemon
./gradlew check --no-daemon
```

`check` is currently BLOCKED by compilation errors in `ArchitectureTest.java`. This must be fixed before declaring the baseline green.

## Required future gates

When modules become active, add gates incrementally for:

- compile and test;
- Core dependency isolation;
- configuration/schema validation;
- persistence migrations/recovery;
- event contract/idempotency;
- runtime adapter contracts;
- network/server authority;
- coverage/performance/stress;
- documentation consistency.

Each gate must have a real Gradle task, source/config inputs, deterministic output and a failing test/fixture where practical. Do not document a proposed task as available until it runs successfully.

## Documentation drift policy

Update the owning document when changing public contracts, ports, events, schemas, persistence, security boundaries or module scope. Pure private refactors may use a documented no-doc-needed rationale.

## Agent checks

- Compare `settings.gradle` with module claims.
- Compare build tasks with command examples.
- Compare status reports with source and test paths.
- Verify every relative Markdown link.
- Keep design-only material labeled `PLANNED` or `BLOCKED`.

## References

- [`AGENTS.md`](AGENTS.md)
- [`architecture-test-report.md`](architecture-test-report.md)
- [`documentation-consistency-report.md`](documentation-consistency-report.md)
- [`testing-strategy.md`](testing-strategy.md)
- [`definition-of-done-do-d.md`](definition-of-done-do-d.md)
