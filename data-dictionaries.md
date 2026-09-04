# Data dictionary

## Canonical shared values

| Concept | Canonical type | Constraint |
|---|---|---|
| World/Colony/Citizen/Player/Region/Incident/Command identity | typed records in `core.shared` | non-null UUID |
| Content identity | `ContentId` | namespaced string |
| Simulation time | `GameTick` | `>= 0` |
| Schema version | `SchemaVersion` | `> 0` |
| Position | `GridPosition` | platform-neutral integer coordinates |

New Core contracts must use these values. `core.api.types`, `Position`, `Npc` and raw UUIDs are compatibility-only.

## Configuration envelope

Repository fixtures are under `infrastructure-common/src/main/resources/config/rwc/` and use:

```json
{"$schema":"rwc://schemas/example.schema.json","schemaVersion":1,"data":{}}
```

- `$schema` identifies the schema resource.
- `schemaVersion` is the only version field.
- Standard JSON comments are not supported.
- Context-specific data belongs in `data`.

## Loading policy

Target lifecycle:

```text
parse → schema validate → semantic validate → migrate → immutable snapshot → atomic publish
```

The active code provides NetworkNT schema validation, diagnostics, semantic helpers, immutable snapshots and atomic publication. Full directory discovery, per-context loaders and durable last-known-good orchestration are not complete.

## Validation rules

Validate required fields, types, ranges, enums, unique IDs, cross-file references, world scope and operational limits. Invalid candidates must not replace the active snapshot.

## Agent rules

- Reuse existing value objects and config keys.
- Update schema, fixture, parser and tests together.
- Document defaults, optionality, migration and failure policy.
- Never silently accept invalid IDs, negative values or cross-world references.

## References

- [`AGENTS.md`](AGENTS.md)
- [`configuration-compliance-report.md`](configuration-compliance-report.md)
- [`configuration-mutation-testing.md`](configuration-mutation-testing.md)
- [`implementation-status.md`](implementation-status.md)
