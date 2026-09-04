# Data interfaces

## Contract rules

- Public Core contracts use typed IDs, `GridPosition`, immutable DTOs and `Optional`/typed results.
- Every world-scoped operation carries `WorldId`.
- Repository interfaces belong to Core; implementations belong to Infrastructure.
- Aggregates do not expose persistence methods.
- Cross-context calls return summaries/records, not foreign aggregates.
- Storage exceptions and JSON/NBT types do not cross into Core.

## Active contracts

- Target driven ports: `core.ports.driven.*Repository`, `*Port` and `JsonSaveLoadPort`.
- Target driving ports: `core.ports.driving.*UseCase`.
- Persistence primitives: `SaveDocument`, `SaveKey`, `AggregateVersion`, `SnapshotMapper`, `SaveMigration`.
- Active storage: `JsonFileSaveAdapter`; compatibility storage: `JsonSaveAdapter`.
- In-memory repositories are test/MVP implementations, not production durability.

## Adapter contract checklist

1. Validate identity, world scope and input shape.
2. Map external representation to Core values.
3. Perform only the requested capability.
4. Translate external failures into a stable port result/error.
5. Preserve atomicity, idempotency and version rules.
6. Add contract tests for save/find/delete, missing data, corruption and replay.

## References

- [`hexagonal-architecture.md`](hexagonal-architecture.md)
- [`save-serialization.md`](save-serialization.md)
- [`core-migration-notes.md`](core-migration-notes.md)
- [`testing-strategy.md`](testing-strategy.md)
