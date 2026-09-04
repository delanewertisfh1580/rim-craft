# Shared Kernel report

**Status: PARTIAL.** Canonical typed values and compatibility mappers exist, but legacy API/value models remain during staged migration.

## Implemented

- `core.shared`: `WorldId`, `ColonyId`, `CitizenId`, `PlayerId`, `RegionId`, `IncidentId`, `CommandId`, `GameTick`, `SchemaVersion`, `GridPosition`, `ColonyName`, `SettlementSite` and `StorytellerId`.
- Immutable Java 17 records with constructor validation.
- Compatibility conversion from target values to `core.api.types` where required.
- Shared-value tests in `core-api`.

## Compatibility debt

`core.api.types`, raw UUIDs, legacy `Position`, duplicate/broad context packages and old ports remain. Do not remove them without coordinated consumer, test and persistence migration.

## Rules

Shared Kernel must not depend on context aggregates, Infrastructure or Minecraft. New APIs use target shared values and never introduce duplicate identifiers/enums.

## Verification

```bash
./gradlew :core-api:test --no-daemon
```

## References

- [`AGENTS.md`](AGENTS.md)
- [`core-migration-notes.md`](core-migration-notes.md)
- [`data-dictionaries.md`](data-dictionaries.md)
