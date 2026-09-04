# Persistence compliance report

**Status: PARTIAL.** Active persistence is platform-neutral JSON; NBT/Forge is blocked future work.

## Implemented

- `SaveDocument`, `SaveKey`, `AggregateVersion`, `SnapshotMapper`, `SaveMigration`, `MigrationRegistry`.
- `JsonSaveLoadPort` and `JsonFileSaveAdapter`.
- Atomic replacement, last-known-good fallback and corruption quarantine.
- World-scoped logical keys and future-version rejection.

## Pending

Full context snapshot mappers/repositories, production persistence bootstrap, NBT adapter and runtime integration.

## Rules

Aggregates do not serialize themselves. Future versions are rejected; supported old versions require deterministic migrations. Corrupt data is quarantined, never silently rewritten.

## Verification

```bash
./gradlew :infrastructure-common:test --no-daemon
```
