# Persistence compliance report

## Status

**PARTIAL / JSON MVP.** Platform-neutral JSON persistence is implemented first. No NBT, Forge, or Minecraft runtime dependency was added.

## Implemented

- `SaveDocument`, `SaveKey`, `AggregateVersion`, and canonical `SchemaVersion` usage.
- `JsonSaveLoadPort` driven port.
- `SnapshotMapper` and deterministic `SaveMigration`/`MigrationRegistry` contracts.
- `JsonFileSaveAdapter` with:
  - atomic temporary-file replacement;
  - last-known-good copies;
  - corrupted-file quarantine;
  - repeated-load stability;
  - world-scoped logical keys.
- Removed legacy NBT persistence methods from `core.npc.Citizen`.
- Contract tests for JSON round-trip, recovery, quarantine, idempotent repeated loads, and future-version rejection.
- Updated `save-serialization.md` and `data-interfaces.md` references through the new JSON boundary.

## Version and recovery policy

- Future schema versions are rejected by the migration registry.
- Old versions can be migrated through explicit, deterministic, non-mutating steps.
- Corrupt active files are quarantined and the last-known-good document is attempted.
- Atomic replacement prevents partially-written active documents.

## Remaining work

Full snapshot mappers and repositories for Colony, Citizen, Storyteller, Goal AI, and Building are still pending because the current skeleton exposes incompatible legacy aggregate APIs and does not yet provide complete target models for all five contexts. NBT remains intentionally blocked until the JSON contract and mapper suite are complete.

## Verification

```bash
./gradlew :infrastructure-common:test --no-daemon
```

The persistence tests are included in the infrastructure-common test suite.
