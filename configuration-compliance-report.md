# Configuration subsystem compliance report

## Status

**PARTIAL / JVM MVP.** NetworkNT `json-schema-validator` `1.5.6` is pinned in `infrastructure-common`. Configuration remains outside Core and uses immutable snapshots with atomic publication.

## Implemented

- Real JSON Schema validation using NetworkNT draft 2020-12 support.
- Canonical envelope: `$schema`, positive `schemaVersion`, and `data` object.
- Canonical runtime configuration locations under `config/rwc/{colony,npc,storyteller,building,goal,events,pathfinder}`.
- Added baseline configuration files for all requested contexts.
- `ConfigSnapshot` and `AtomicConfigPublisher`.
- `ConfigDiagnostic` containing logical key, physical path, JSON path, reason, fallback, and reload ID.
- `ConfigMutator` using JSON tree operations without modifying source fixtures.
- Semantic duplicate-ID and reference validation utilities.
- Gradle tasks: `configTest`, `configSchemaTest`, and `configMutationTest`.
- Smoke tests for missing fields, wrong types, negative/boundary values, unknown references, duplicates, mutation isolation, and corrupt JSON.

## Fallback policy

The documented hierarchy is retained: valid user config → last-known-good → packaged default → safe empty optional config → fatal startup for critical configuration. Atomic publication ensures an invalid candidate cannot replace the active snapshot.

## Known limitations

Full directory loader/orchestrator, persistent last-known-good storage, packaged-default materialization, per-context semantic schemas, complete cross-file registry, and fatal/optional classification are still pending. Existing legacy flat resource files remain for compatibility and should be migrated through a later controlled pass.

## Verification

```bash
./gradlew :infrastructure-common:test --no-daemon
./gradlew :infrastructure-common:configTest :infrastructure-common:configSchemaTest :infrastructure-common:configMutationTest --no-daemon
```

No `.env`, Minecraft/Forge dependency, or external credential was changed.
