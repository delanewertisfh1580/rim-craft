# RimWorldCraft — Data Interfaces

## Canonical Shared Kernel conventions

All public Core data contracts use the canonical immutable value objects from `com.rimworldcraft.core.api.types`:

- `WorldId`, `ColonyId`, `CitizenId`, `PlayerId`, `RegionId`, `IncidentId`, `CommandId` for typed identity;
- `ContentId` for namespaced content keys;
- `GameTick` and `SchemaVersion` for validated scalar values;
- `GridPosition` for platform-neutral coordinates;
- `com.rimworldcraft.core.api.types.Gender` as the sole gender enum.

`Citizen`/`CitizenId` are canonical terminology. `Npc`/`NpcId` are legacy terms and must not be used in new APIs. `core.api.ports` is the current compatibility package; the target layout is `core.ports.driving`/`core.ports.driven`. External UUID and string values are converted at adapter/application boundaries through the typed mapper; they must not leak into new Core contracts.

Every world-scoped query includes `WorldId`. Cross-context APIs exchange IDs, summaries, and immutable DTOs rather than foreign aggregates. `Position` remains a compatibility type only; new contracts use `GridPosition`.

## Persistence contract

Platform-neutral persistence uses `com.rimworldcraft.core.persistence.SaveDocument`, `SaveKey`, `AggregateVersion`, `SnapshotMapper`, and `SaveMigration`. `JsonSaveLoadPort` is the current JSON boundary; `JsonFileSaveAdapter` performs atomic temp-file replacement, quarantine of malformed files, and last-known-good fallback. Domain aggregates do not expose persistence methods. NBT remains a future adapter only after the JSON contract is stable.

## 1. Введение

`data-interfaces.md` — единый источник правды для интерфейсов доступа к данным RimWorldCraft. Документ определяет контракты репозиториев, фабрик и спецификаций, но не конкретные способы хранения. Реализация может использовать NBT, JSON-файлы, память или другой backend без изменения Core.

Документ развивает [`hexagonal-architecture.md`](hexagonal-architecture.md), [`bounded-contexts.md`](bounded-contexts.md), `module-colony-manager.md`, `module-npc-core.md`, `module-storyteller.md`, `module-goal-ai.md`, `module-building-system.md` и [`event-system-api.md`](event-system-api.md).

### 1.1 Роль в гексагональной архитектуре

Репозитории — driven ports: application/domain code требует их capabilities, а Infrastructure реализует их. Фабрики и specifications — Core contracts/policies, не зависящие от NBT, Minecraft API, файловых путей или конкретной БД.

```text
Core use case
    -> core.*.repository / factory / specification interfaces
    -> Infrastructure adapter
    -> ISaveLoadPort / JSON / NBT / database
```

Правила:

- интерфейсы находятся в `core.*`;
- реализации находятся в `infrastructure.*`;
- Core не импортирует `net.minecraft`, NBT, SQL, IO implementation или framework annotations;
- aggregate repositories возвращают aggregate roots, а не raw records;
- межконтекстные ссылки передаются через IDs и summaries;
- `null` для отсутствующих результатов запрещён: используется `Optional` или typed result.

