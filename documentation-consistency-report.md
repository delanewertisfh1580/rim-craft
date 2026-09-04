# Documentation consistency report

**Scope:** all project Markdown files. **Checked:** 2026-09-03.

## Canonical decisions

- Project scope: Java 17 platform-neutral JVM MVP.
- Active modules: `core-api`, `core-impl`, `infrastructure-common`.
- Canonical aggregate term: `Citizen`; ID: `CitizenId`.
- Canonical coordinate: `GridPosition`.
- New ports: `core.ports.driving` and `core.ports.driven`.
- Shared values: `core.shared`; cross-context DTOs: `core.contracts`.
- Config version: positive `schemaVersion`; `$schema` is metadata.
- Repository fixture path: `config/rwc/`; intended runtime convention: `config/rimworldcraft/`.
- Active persistence: JSON `SaveDocument`/`JsonFileSaveAdapter`; NBT is blocked future work.
- Active event implementation: synchronous in-memory bus; durable outbox/production bootstrap are pending.

## Исправленные при синхронизации правила

1. Design-only examples are labeled as target/future behavior.
2. Compliance documents use the same four statuses: `IMPLEMENTED`, `PARTIAL`, `PLANNED`, `BLOCKED`.
3. Links target existing files only; missing planned modules are no longer presented as current references.
4. `AGENTS.md` is the agent-facing entrypoint and `implementation-status.md` is the status source of truth.
5. Runtime claims are separated from JVM evidence.

## Known repository facts

- `infrastructure-forge`, `test-common`, `test-core`, and `test-integration` are present but not included by `settings.gradle`.
- `ArchitectureTest.java` is checked in and referenced by `core-impl`, but currently does not compile against the configured ArchUnit API.
- The repository contains committed/generated `build/` and `.gradle/` output; do not use it as current source evidence.
- No active `.github/workflows/ci.yml`, `docsCheck`, `integrationTest`, `acceptanceTest`, `migrationTest`, `jacocoTestCoverageVerification`, Checkstyle or SpotBugs task was found in the active build.

## Link policy

Every relative Markdown link must resolve to an existing file. References to future work must be plain code names or explicitly marked `PLANNED`, not broken links.

## Agent reading order

1. `AGENTS.md`
2. `implementation-status.md`
3. relevant module/compliance document
4. architecture or persistence/event document if the boundary changes
5. `definition-of-done-do-d.md` before declaring work complete
