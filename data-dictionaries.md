# RimWorldCraft — Data Dictionaries

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

## 2. Общие правила и соглашения

### 2.1 Общий envelope

Каждый конфигурационный файл использует корневой объект:

| Поле | Тип | Обязательность | Правило |
|---|---|---:|---|
| `$schema` | string | required | URI/путь JSON Schema |
| `version` | integer | required | положительная версия структуры |
| `modVersion` | string | optional | минимальная версия мода, например `0.4.0` |
| `_comment` | string | optional | пояснение, не читается доменной логикой |
| основной массив/объект | object/array | required | зависит от файла |

Пример:

```json
{
  "$schema": "rwc://schemas/npc/traits.schema.json",
  "version": 1,
  "modVersion": "0.4.0",
  "traits": []
}
```

### 2.2 Имена и идентификаторы

- Все `id` уникальны в пределах файла.
- Рекомендуемый формат: `^[a-z][a-z0-9_:-]{1,63}$`.
- Для собственного контента рекомендуется namespace: `rwc:optimist` или `my_pack:optimist`.
- ID является стабильным ключом save data и ссылок; переименование display text не меняет ID.
- Ссылки должны указывать на существующий ID, если поле не помечено как optional/nullable.
- Встроенные Minecraft IDs используют формат `minecraft:stone`; внешние mod IDs должны содержать namespace.

### 2.3 Типы и единицы измерения

| Обозначение | JSON-тип | Соглашение |
|---|---|---|
| `id` | string | стабильный namespaced identifier |
| `count` | integer | неотрицательное целое, если не указано иначе |
| `factor` | number | множитель, обычно `0.0..10.0` |
| `weight` | number | неотрицательный вес weighted selection |
| `ticks` | integer | Minecraft ticks, `20 ticks = 1 second` |
| `duration` | string | ISO-8601 duration, например `PT30S`, если используется вместо ticks |
| `dateTime` | string | ISO-8601 с timezone/UTC |
| `position` | object | relative `{x,y,z}` без Minecraft types |
| `key` | string | localization key, а не отображаемый текст |

### 2.4 Общие правила безопасности

- JSON не может содержать executable code, class names или reflection targets.
- Не разрешаются абсолютные пути и path traversal в content pack metadata.
- Неизвестные поля по умолчанию являются ошибкой для production, если файл не поддерживает `extensions`.
- Числа проверяются на overflow и разумные upper bounds.
- Значения конфигов копируются в immutable runtime model; mutable raw maps не передаются в Core.

## 3. Общий список файлов

| Файл | Контекст | Основное содержимое |
|---|---|---|
| `colony-settings.json` | Colony | стартовые ресурсы, зоны, block values, difficulty modifiers |
| `traits.json` | NPC | черты характера, mood/skill/social effects |
| `skills.json` | NPC | каталог навыков, ranges и skill effects |
| `story-events.json` | Storyteller | incidents, conditions, spawn и outcome effects |
| `prefabs.json` | Colony/World | шаблоны построек, блоки, ресурсы и build time |
| `npc-names.json` | NPC | имена по culture, race и gender |
| `world-biome-modifiers.json` | World | влияние биомов на fertility, resources и events |
| `player-achievements.json` | Player | условия достижений и награды |
| `needs.json` | NPC | потребности, decay и mood thresholds |
| `jobs.json` | Colony/NPC | виды работ, prerequisites и output intents |
| `resources.json` | Colony/World | типы ресурсов, stackability и category |
| `localization/*.json` | Cross-cutting | display strings по locale |

Ниже подробно описаны все восемь обязательных схем и дополнительные схемы, необходимые для cross-reference validation.

## 4. `colony-settings.json`

### 4.1 Назначение и владелец

- **Контекст:** `ColonyContext`
- **Парсер:** `ColonySettingsJsonParser`
- **Runtime model:** `ColonyConfigSnapshot`
- **Загрузчик:** `ColonyConfigurationRepository`
- **Назначение:** правила основания колонии, стартовый пакет, зоны, ценность блоков и модификаторы сложности.

### 4.2 Структура полей

| Путь | Тип | Обязательность | Допустимые значения/формат | Связи | Пример |
|---|---|---:|---|---|---|
| `$schema` | string | required | `rwc://schemas/colony/colony-settings.schema.json` | общий envelope | `rwc://...` |
| `version` | integer | required | `>=1` | migration version | `1` |
| `startingResources[]` | array | required | unique items | `resources[].id` | `[...]` |
| `startingResources[].resourceId` | string | required | ID из `resources.json` | cross-file ref | `minecraft:bread` |
| `startingResources[].count` | integer | required | `1..100000` | неотрицателен | `32` |
| `startingZones[]` | array | required | `0..32` items | `zone.id` unique | `[...]` |
| `startingZones[].id` | string | required | ID зоны | local unique | `home` |
| `startingZones[].shape` | string | required | `CIRCLE`, `RECTANGLE`, `RADIUS` | — | `CIRCLE` |
| `startingZones[].radius` | integer | conditional | `1..128`, required for circle/radius | — | `12` |
| `startingZones[].priority` | integer | optional | `0..100`, default `50` | work policy | `80` |
| `blockValues[]` | array | required | unique `blockId` | Minecraft/mod registry | `[...]` |
| `blockValues[].blockId` | string | required | namespaced block ID | runtime registry | `minecraft:stone` |
| `blockValues[].wealthValue` | number | required | `0..1000000` | Storyteller wealth | `1.0` |
| `blockValues[].buildValue` | number | optional | `0..1000000`, default `0` | — | `0.5` |
| `difficultyModifiers` | object | required | — | Storyteller reads summary | `{}` |
| `difficultyModifiers.wealthFactor` | number | required | `0..10` | raid scaling | `0.0002` |
| `difficultyModifiers.populationFactor` | number | required | `0..100` | raid scaling | `35` |
| `difficultyModifiers.moraleFactor` | number | optional | `-100..100`, default `0` | incident pressure | `0.5` |
| `foundingRules` | object | required | — | World port validates | `{}` |
| `foundingRules.minDistanceFromSpawn` | integer | optional | `0..100000`, default `0` | `WorldId` scope | `64` |
| `foundingRules.allowedClimates[]` | array | optional | IDs from biome modifiers/climates | cross-file | `[