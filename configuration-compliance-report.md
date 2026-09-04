# Configuration compliance report

**Status: PARTIAL.**

## Implemented

- NetworkNT JSON Schema validator `1.5.6` in `infrastructure-common`.
- Draft 2020-12 envelope/schema validation.
- Immutable `ConfigSnapshot` and atomic publication.
- Diagnostics and semantic duplicate/reference helpers.
- Non-destructive `ConfigMutator`.
- Baseline `config/rwc` fixtures and configuration smoke tests.

## Pending

Directory discovery/orchestration, full per-context parsers, persistent last-known-good storage, packaged-default materialization, complete cross-file registry and critical/optional classification.

## Rules

Use `schemaVersion`, not a second `version` field. Keep config parsing outside Core. Invalid candidates must not replace the active snapshot.

## Verification

```bash
./gradlew :infrastructure-common:test --no-daemon
./gradlew :infrastructure-common:configTest :infrastructure-common:configSchemaTest :infrastructure-common:configMutationTest --no-daemon
```
