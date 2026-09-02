# RimWorldCraft — Data Dictionaries

## Canonical Shared Kernel

The Core uses the following immutable Java 17 value objects as canonical identifiers and scalar contracts:

| Concept | Canonical type | Boundary representation |
|---|---|---|
| World scope | `WorldId` | external UUID mapped at adapter boundary |
| Colony identity | `ColonyId` | external UUID mapped at adapter boundary |
| Citizen identity | `CitizenId` | external UUID mapped at adapter boundary |
| Player identity | `PlayerId` | external UUID mapped at adapter boundary |
| Region identity | `RegionId` | external UUID mapped at adapter boundary |
| Incident identity | `IncidentId` | external UUID mapped at adapter boundary |
| Command identity | `CommandId` | external UUID mapped at adapter boundary |
| Content identity | `ContentId` | namespaced string, `namespace:path` |
| Simulation time | `GameTick` | non-negative integer |
| Schema version | `SchemaVersion` | positive integer |
| Position | `GridPosition` | `{x,y,z}`; no Minecraft types |
| Gender | `core.api.types.Gender` | enum value |

`Citizen` and `CitizenId` are canonical; `Npc`/`NpcId` are legacy documentation terms. `GridPosition` is canonical; `Position` remains only as a compatibility value during migration. External UUIDs must be converted with the Core boundary mapper (`ExternalIdMapper`) before entering typed Core APIs. Typed identifiers are immutable records and reject null or invalid values in their constructors.

`WorldId` is always part of a world-scoped repository/query contract. Equal UUID values represent equal identifiers; different `WorldId` values never share scope implicitly.

## 1. Введение

Этот документ является справочником конфигурационных данных RimWorldCraft. Он дополняет [`system-overview.md`](system-overview.md), [`bounded-contexts.md`](bounded-contexts.md) и [`hexagonal-architecture.md`](hexagonal-architecture.md), фиксируя контракт между Core, инфраструктурой и авторами контента.

Все пользовательские настройки и контент хранятся в JSON в каталоге:

```text
config/rwc/
```

Конфиги разделены по bounded contexts. Core получает данные только через `IConfigPort`; он не читает файловую систему и не знает деталей JSON-библиотеки. В инфраструктуре размещаются discovery, parsing, JSON Schema validation, migration и публикация immutable snapshots.

### 1.1 Что такое схема

В RimWorldCraft под схемой понимаются два взаимодополняющих контракта:

1. **JSON Schema** — machine-readable контракт для IDE, CI и CLI-валидатора.
2. **Этот справочник** — human-readable описание семантики, связей, defaults и поведения.

Каждый файл имеет `$schema` и `version`. Поля с `_comment` допускаются как неисполняемые пояснения; стандартные JSON-комментарии (`//`, `/* */`) запрещены.

### 1.2 Загрузка и жизненный цикл

1. При старте мода `ConfigBootstrap` обнаруживает встроенные и пользовательские files.
2. `JsonConfigAdapter` парсит JSON в raw DTO.
3. `JsonSchemaValidator` проверяет структуру и типы.
4. `ConfigSemanticValidator` проверяет cross-references, ranges, duplicate IDs и domain invariants.
5. `ConfigMigrationService` переводит поддерживаемые старые версии в текущую canonical model.
6. Валидированные данные собираются в immutable `ConfigSnapshot`.
7. Snapshot атомарно публикуется через `IConfigPort`.

Перезагрузка выполняется безопасно на lifecycle boundary, обычно серверной командой `/rwc reload` или событием resource reload:

```text
read all -> parse all -> schema validate -> semantic validate -> migrate -> build snapshot -> atomic swap
```

Если новый snapshot невалиден, активный старый snapshot сохраняется. Частичная публикация отдельных файлов запрещена.

### 1.3 Рекомендуемый механизм валидации

В production рекомендуется использовать JSON Schema draft 2020-12 совместно с существующей JSON-библиотекой проекта (например, Jackson или Gson, согласно фактическому build setup). Название библиотеки-валидатора фиксируется в build configuration и не является частью Core API.

Проверки делятся на:

- **структурные:** типы, required fields, enum, regex, min/max;
- **семантические:** уникальность IDs, ссылки между файлами, отсутствие конфликтующих traits;
- **операционные:** доступность Minecraft registry ids, совместимость версии мода, ограничения производительности prefab.

