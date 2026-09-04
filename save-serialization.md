# Save serialization

**Status: PARTIAL.** Active persistence is platform-neutral JSON. NBT/Minecraft persistence is future BLOCKED work.

## Active contract

Core persistence primitives are in `core-api/.../core/persistence/`:

- `SaveDocument` — immutable normalized document;
- `SaveKey` — world-scoped logical key;
- `AggregateVersion` and `SchemaVersion`;
- `SnapshotMapper`;
- `SaveMigration` and `MigrationRegistry`;
- `JsonSaveLoadPort`.

The active adapter is `infrastructure-common/.../JsonFileSaveAdapter.java`.

## Guarantees

- Atomic temporary-file replacement where supported.
- Last-known-good fallback.
- Corruption quarantine.
- World-scoped logical paths.
- Future schema versions rejected.
- Old versions changed only by explicit deterministic migrations.
- Aggregates do not know JSON, NBT, filesystem or persistence APIs.

## Data rules

- Stable IDs and enum names, never enum ordinals.
- `schemaVersion` is the only schema version field.
- `WorldId` is required for world-scoped data.
- Optional values are absent or represented by explicit state, not arbitrary null payloads.
- Runtime handles, threads, callbacks, Minecraft entities and `Level` objects are never authoritative save data.

## Current limitations

Only the generic JSON boundary and limited adapter/tests are active. Full aggregate snapshot mappers/repositories for Colony, Citizen, Goal AI, Building and Storyteller are incomplete. No active `NbtSaveAdapter`, Minecraft `SavedData` integration or save migration matrix exists.

## Agent workflow for persistence changes

1. Define/modify the neutral `SaveDocument` contract.
2. Add mapper and migration behavior outside the aggregate.
3. Test round-trip, missing data, corruption, future versions and repeated load.
4. Keep writes atomic and recovery observable.
5. Update the owning context document and [`implementation-status.md`](implementation-status.md).

## Verification

```bash
./gradlew :infrastructure-common:test --no-daemon
```

## References

- [`data-interfaces.md`](data-interfaces.md)
- [`persistence-compliance-report.md`](persistence-compliance-report.md)
- [`module-colony.md`](module-colony.md)
- [`module-npc-core.md`](module-npc-core.md)
