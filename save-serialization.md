# RimWorldCraft — Save Serialization

## 1. Назначение и границы

Этот документ описывает persistence layer RimWorldCraft: `ISaveLoadPort`, `NbtSaveAdapter`, структуру world-scoped NBT, сериализацию агрегатов, версионирование, миграции, повреждённые данные, async save и тестирование.

Модуль является infrastructure-слоем. Он не содержит бизнес-правил Colony, NPC, Storyteller, Goal AI или Building System. Его задача — надёжно сохранить и восстановить platform-neutral snapshots, используя Minecraft `SavedData`/NBT или заменяемый backend.

Связи:

- [`hexagonal-architecture.md`](hexagonal-architecture.md) — порт `ISaveLoadPort`;
- [`data-interfaces.md`](data-interfaces.md) — repository contracts;
- [`module-colony-manager.md`](module-colony-manager.md) / [`module-colony.md`](module-colony.md) — Colony state;
- [`module-npc-core.md`](module-npc-core.md) — Citizen state;
- [`module-storyteller.md`](module-storyteller.md) — Storyteller, incidents и arcs;
- [`module-goal-ai.md`](module-goal-ai.md) — AI plans/tasks;
- [`module-building-system.md`](module-building-system.md) — build orders, blueprints, ghost blocks;
- [`entity-integration.md`](entity-integration.md) — runtime entity NBT, отдельный от Core aggregate persistence.

### 1.1 Требования

- сохранение не должно смешивать разные `WorldId`;
- каждый aggregate имеет schema version;
- неизвестная будущая версия не перезаписывается старым кодом;
- миграции детерминированы и идемпотентны;
- corrupted data изолируется, а не silently чинится;
- async IO не мутирует Core aggregate и Minecraft world из worker thread;
- write operations атомарны настолько, насколько позволяет backend;
- backup/rollback policy документирована.

## 2. Архитектура persistence

```text
Core aggregate
    -> Repository interface
    -> Snapshot mapper
    -> ISaveLoadPort
    -> NbtSaveAdapter
    -> Minecraft SavedData / NBT storage
```

`NbtSaveAdapter` не знает семантики `Colony` или `Citizen`: он сохраняет `SaveDocument`. Semantic mapper/serializer конкретного контекста выполняет преобразование aggregate ↔ document, но также остаётся вне domain model.

### 2.1 Уровни

| Уровень | Ответственность |
|---|---|
| Aggregate | инварианты и state transitions |
| Repository | выбрать key, hydrate/dehydrate aggregate, optimistic version |
| Snapshot Mapper | преобразовать domain snapshot в neutral document |
| `ISaveLoadPort` | абстрактное load/save/delete |
| `NbtSaveAdapter` | NBT encoding, SavedData lifecycle, atomic write и backup |
| Migration Registry | schema version transformations |
| Recovery Service | quarantine, fallback, diagnostics |

## 3. Порт `ISaveLoadPort`

```java
public interface ISaveLoadPort {
    Optional<SaveDocument> load(SaveKey key) throws SaveLoadException;
    void save(SaveKey key, SaveDocument document) throws SaveLoadException;
    void delete(SaveKey key) throws SaveLoadException;
    SaveMetadata metadata(SaveKey key) throws SaveLoadException;
}

public record SaveKey(
        WorldId worldId,
        String aggregateType,
        String aggregateId
) {
    public SaveKey {
        Objects.requireNonNull(worldId);
        if (!aggregateType.matches("[a-z][a-z0-9_-]{1,63}")) {
            throw new IllegalArgumentException("invalid aggregate type");
        }
        if (aggregateId.isBlank() || aggregateId.contains("..")) {
            throw new IllegalArgumentException("invalid aggregate id");
        }
    }
}

public record SaveDocument(
        String aggregateType,
        int schemaVersion,
        Map<String, Object> values,
        Map<String, String> metadata
) {
    public SaveDocument {
        if (schemaVersion < 1) throw new IllegalArgumentException("schema >= 1");
        values = Map.copyOf(values);
        metadata = Map.copyOf(metadata);
    }
}
```

`SaveDocument` должен содержать только NBT-compatible scalar/list/map values после normalization. Arbitrary Java objects, `Entity`, `Level`, callback и threads запрещены.

## 4. `NbtSaveAdapter`

### 4.1 Контракт и регистрация

```java
public final class NbtSaveAdapter implements ISaveLoadPort {
    private final SavedDataAccess savedData;
    private final NbtCodec codec;
    private final SaveAtomicity atomicity;

    @Override
    public Optional<SaveDocument> load(SaveKey key) throws SaveLoadException {
        CompoundTag root = savedData.read(key);
        if (root == null) return Optional.empty();
        return Optional.of(codec.decode(root));
    }

    @Override
    public void save(SaveKey key, SaveDocument document) throws SaveLoadException {
        CompoundTag encoded = codec.encode(document);
        savedData.writeAtomically(key, encoded);
    }

    @Override
    public void delete(SaveKey key) throws SaveLoadException {
        savedData.delete(key);
    }
}
```

Регистрация выполняется в composition root при world/server lifecycle:

```java
public Runtime createRuntime(ServerContext server) {
    ISaveLoadPort saveLoad = new NbtSaveAdapter(
            new MinecraftSavedDataAccess(server),
            new NbtCodec(),
            SaveAtomicity.BACKUP_THEN_REPLACE);
    return new Runtime(saveLoad);
}
```

Entity runtime NBT registration не заменяет регистрацию `ISaveLoadPort`: это два разных механизма.

### 4.2 Key layout

Рекомендуемый logical key:

```text
rwc/<worldId>/<aggregateType>/<aggregateId>
```

Примеры:

```text
rwc/frontier/colony/7f8c...
rwc/frontier/citizen/2d1...
rwc/frontier/storyteller/main
rwc/frontier/citizen-ai/2d1...
rwc/frontier/build-order/91ab...
```

Физический `SavedData` key может быть другим, но logical identity должна сохраняться в metadata.

## 5. Общая NBT-схема

### 5.1 Root envelope

| NBT key | Тип | Required | Назначение |
|---|---|---:|---|
| `format` | string | yes | `rwc-save` |
| `formatVersion` | int | yes | версия общего envelope |
| `aggregateType` | string | yes | `colony`, `citizen`, `storyteller` и т.д. |
| `aggregateId` | string | yes | стабильный ID |
| `worldId` | string | yes | world scope |
| `schemaVersion` | int | yes | версия конкретного aggregate |
| `aggregateVersion` | long | yes | optimistic concurrency version |
| `savedAtTick` | long | yes | server game tick |
| `savedAt` | string | optional | ISO-8601 UTC |
| `checksum` | string | optional | integrity check |
| `payload` | compound | yes | aggregate-specific data |
| `metadata` | compound | optional | diagnostics/migration info |

Пример в JSON-представлении NBT:

```json
{
  "format": "rwc-save",
  "formatVersion": 1,
  "aggregateType": "citizen",
  "aggregateId": "2d1c7f0e-...",
  "worldId": "frontier",
  "schemaVersion": 2,
  "aggregateVersion": 18,
  "savedAtTick": 24000,
  "savedAt": "2026-09-01T12:00:00Z",
  "payload": {},
  "metadata": { "lastMigration": "1-to-2" }
}
```

### 5.2 NBT type policy

- UUID хранится как canonical string или two-long format, но convention должен быть единым;
- `enum` хранится как stable string, не ordinal;
- decimal values — double или scaled integer согласно schema;
- map keys должны быть valid NBT strings;
- список имеет explicit item shape;
- `null` заменяется отсутствующим optional key или explicit state enum;
- unknown fields сохраняются только в `extensions`, если это разрешено schema.

## 6. Схемы агрегатов

### 6.1 Colony

```text
payload:
  name: string
  status: ACTIVE|PAUSED|DESTROYED
  site:
    worldId: string
    x: int
    y: int
    z: int
  members: list<compound>
    - npcId: string
      role: string
      joinedAtTick: long
  inventory: list<compound>
    - resourceId: string
      amount: int
  reservations: list<compound>
    - reservationId: string
      resourceId: string
      amount: int
      orderId: string
  workPolicy: list<compound>
  zones: list<compound>
  objectives: list<compound>
  morale: double
  wealth: double
```

Инварианты: no duplicate member IDs, inventory amounts non-negative, site world matches root, destroyed colony cannot have active work assignment.

### 6.2 Citizen/NPC

```text
payload:
  name: string
  gender: string
  race: string
  birthDate: string
  role: string
  colonyId: string (optional)
  status: ACTIVE|INCAPACITATED|DEAD
  traits: list<string>
  skills: list<compound>
    - type: string
      level: int
      experience: int
  needs: list<compound>
    - type: string
      currentValue: double
  mood:
    value: int
    modifiers: list<compound>
  relationships: list<compound>
    - targetId: string
      value: int
      history: list<compound>
  currentTask: compound (optional)
  position:
    worldId: string
    x: int
    y: int
    z: int
```

`CitizenId` находится в root `aggregateId`; допускается дублировать его в payload только для integrity check. Entity runtime NBT хранит только binding/projection metadata.

### 6.3 Storyteller

```text
payload:
  colonyId: string
  lastEventTick: long
  cooldownUntilTick: long
  activeIncidents: list<compound>
  storyArcs: list<compound>
  timeline: list<compound>
  pressure: double
  configVersion: string
```

Timeline имеет retention limit; служебные tick events не сохраняются без debug policy.

### 6.4 Goal AI

```text
payload:
  citizenId: string
  status: ACTIVE|SUSPENDED
  currentGoal: compound (optional)
  currentPlan: compound (optional)
    - planId: string
      goalType: string
      status: string
      actions: list<compound>
        - actionId: string
          type: string
          target: compound (optional)
  actionQueue: list<string>
  lastReplanTick: long
  planRevision: long
  pendingTaskIds: list<string>
```

Runtime path handles, futures и Minecraft entity UUID не являются достаточными для восстановления и не сохраняются как authoritative plan state.

### 6.5 Building

```text
payload:
  colonyId: string
  blueprintId: string
  targetPosition: compound
  priority: int
  status: PENDING|IN_PROGRESS|COMPLETED|CANCELLED|FAILED
  assignedCitizenIds: list<string>
  resourcesRequired: list<compound>
  resourcesConsumed: list<compound>
  reservations: list<string>
  progress: int
  createdAtTick: long
  startedAtTick: long (optional)
  completedAtTick: long (optional)
  ghostBlockIds: list<string>
```

`Blueprint` catalog может храниться отдельно от `BuildOrder`; order snapshot хранит `blueprintId` и version, а не обязательно копию всего prefab.

## 7. Serialization и repositories

### 7.1 Общий mapper

```java
public interface SnapshotMapper<T> {
    SaveDocument toDocument(T aggregate);
    T fromDocument(SaveDocument document);
}
```

Пример repository:

```java
public final class NbtCitizenRepository implements ICitizenRepository {
    private final ISaveLoadPort storage;
    private final SnapshotMapper<Citizen> mapper;

    @Override
    public Optional<Citizen> findById(WorldId worldId, CitizenId id) {
        SaveKey key = new SaveKey(worldId, "citizen", id.value().toString());
        return storage.load(key).map(mapper::fromDocument);
    }

    @Override
    public Citizen save(Citizen citizen) {
        SaveKey key = new SaveKey(citizen.worldId(), "citizen",
                citizen.id().value().toString());
        storage.save(key, mapper.toDocument(citizen));
        return citizen;
    }
}
```

Repository не должен загружать через один ключ весь другой bounded context. Для summaries используются projections/query ports.

### 7.2 Save ordering

При server save:

1. остановить приём новых mutation или открыть save barrier;
2. flush pending domain/outbox events;
3. snapshot aggregates в consistent tick;
4. записать documents atomically;
5. отметить `SavedData#setDirty`/backend commit;
6. снять barrier и отправить diagnostics.

## 8. Версионирование и миграции

### 8.1 Версии

- `formatVersion` — общий envelope;
- `schemaVersion` — конкретная модель aggregate;
- `aggregateVersion` — optimistic concurrency;
- `modVersion` — версия кода/контента, не заменяет schema version.

### 8.2 Migration contract

```java
public interface SaveMigration {
    String aggregateType();
    int fromVersion();
    int toVersion();
    SaveDocument migrate(SaveDocument source);
}

public final class MigrationRegistry {
    public SaveDocument migrateToCurrent(SaveDocument document) {
        SaveDocument current = document;
        while (current.schemaVersion() < currentSchema(current.aggregateType())) {
            SaveMigration migration = find(current.aggregateType(), current.schemaVersion());
            current = migration.migrate(current);
        }
        return current;
    }
}
```

### 8.3 Правила миграции

- миграция детерминирована;
- миграция не зависит от текущего Minecraft world, если это не documented external mapping;
- migration не меняет исходный document in-place;
- каждый шаг имеет `fromVersion`/`toVersion`;
- шаги тестируются отдельно;
- результат валидируется текущей schema и domain invariants;
- после успешной миграции snapshot записывается только после полной проверки;
- forward version не downgrade’ится.

### 8.4 Пример migration Citizen 1 → 2

Версия 1 хранила `mood` как integer; версия 2 добавляет `mood.modifiers` и `role`:

```java
public final class CitizenV1ToV2 implements SaveMigration {
    @Override
    public SaveDocument migrate(SaveDocument source) {
        Map<String, Object> values = new HashMap<>(source.values());
        values.putIfAbsent("role", "CITIZEN");
        Map<String, Object> mood = new HashMap<>();
        mood.put("value", values.remove("mood"));
        mood.put("modifiers", List.of());
        values.put("mood", mood);
        return new SaveDocument("citizen", 2, values,
                Map.of("migratedFrom", "1"));
    }
}
```

### 8.5 Добавление optional field

Новые поля добавляются optional с default:

```text
schemaVersion 2 -> 3:
  payload.schedule optional, default empty schedule
```

Required field добавляется только в новой schema version с migration, которая создаёт корректное значение для старых snapshots.

## 9. Повреждённые данные и recovery

### 9.1 Классификация

- `MISSING` — записи нет, допустимо для нового aggregate;
- `MALFORMED` — неверный NBT type/required key;
- `CORRUPTED` — checksum/duplicate/invariant violation;
- `UNSUPPORTED_FUTURE_VERSION` — версия новее текущего кода;
- `INCOMPATIBLE_REFERENCE` — отсутствует referenced ID;
- `IO_FAILURE` — storage unavailable.

### 9.2 Recovery policy

1. Не загружать corrupted aggregate в активную игру.
2. Скопировать raw document в quarantine/backup с timestamp.
3. Записать error code, key, schema version, correlation ID.
4. Для optional projection пересоздать projection из authoritative source.
5. Для critical aggregate остановить только загрузку этого aggregate или server startup согласно severity policy.
6. Не использовать «тихий» stale cache, если это может потерять состояние.
7. Предоставить operator command/report для диагностики.

```java
public enum SaveLoadErrorCode {
    MALFORMED,
    CORRUPTED,
    UNSUPPORTED_VERSION,
    INVALID_REFERENCE,
    IO_FAILURE
}
```

### 9.3 Fallback

- Missing optional AI plan → `CitizenAI` starts with `IDLE` и requests replan.
- Missing UI projection → rebuild from aggregate.
- Corrupt Colony aggregate → quarantine and controlled startup failure/restore backup.
- Future schema → read-only refusal, без overwrite.

Fallback не должен создавать фиктивных NPC, ресурсов или событий.

## 10. Асинхронное сохранение

### 10.1 Цель

NBT/SavedData может быть быстрым, но сериализация больших snapshots или external backend способен блокировать tick. Async save разрешён при snapshot isolation.

```java
public interface AsyncSaveLoadPort extends ISaveLoadPort {
    CompletableFuture<SaveResult> saveAsync(SaveKey key, SaveDocument document);
}
```

### 10.2 Безопасный алгоритм

```text
server thread: capture immutable snapshot at tick T
server thread: enqueue SaveDocument
worker: encode/compress/write temporary data
worker: fsync/commit if backend supports it
server lifecycle: publish save completion/failure
```

Не передавать mutable aggregate в worker. Не вызывать Minecraft `SavedData` mutation из worker, если API требует server thread; использовать thread-safe queue и commit callback.

### 10.3 Backpressure

- bounded save queue;
- coalesce newer snapshot по `(worldId, aggregateType, aggregateId)`;
- never drop latest critical snapshot silently;
- on queue saturation force synchronous barrier or fail loudly;
- shutdown drains queue with timeout.

### 10.4 Consistency

Snapshot содержит `capturedAtTick` и `aggregateVersion`. Если на момент commit version уже новее, старый snapshot нельзя публиковать поверх нового; adapter сравнивает versions либо repository использует last-write-wins только с monotonic guard.

## 11. `save-serialization` тестирование

### 11.1 Unit

- mapper round-trip для каждого aggregate;
- enum/string and UUID encoding;
- absent optional fields/defaults;
- invalid type/range/duplicate IDs;
- migration idempotency;
- future version rejection;
- checksum mismatch;
- deterministic serialization.

```java
@Test
void citizenSnapshotRoundTripsAllImportantFields() {
    Citizen original = CitizenFixtures.complete();
    SaveDocument document = mapper.toDocument(original);
    Citizen restored = mapper.fromDocument(document);

    assertThat(restored).usingRecursiveComparison().isEqualTo(original);
}
```

### 11.2 Contract tests `ISaveLoadPort`

Один abstract suite запускается для:

- `InMemorySaveLoadAdapter`;
- `NbtSaveAdapter`;
- future `JsonFileSaveAdapter`/database adapter.

Проверки: save/load/delete, missing key, overwrite/version, metadata, atomic failure, world scope.

### 11.3 Migration tests

```java
@ParameterizedTest
@ValueSource(ints = {1, 2})
void oldCitizenVersionsMigrateToCurrent(int version) {
    SaveDocument oldDocument = Fixtures.citizenDocument(version);
    SaveDocument migrated = migrations.migrateToCurrent(oldDocument);

    assertThat(migrated.schemaVersion()).isEqualTo(CURRENT_CITIZEN_SCHEMA);
    assertThat(validator.validate(migrated)).isEmpty();
    assertThat(migrations.migrateToCurrent(migrated)).isEqualTo(migrated);
}
```

### 11.4 Integration

- server/world save-load cycle;
- NBT physical write/read;
- crash simulation between temp write and replace;
- backup restore;
- concurrent save barrier;
- all aggregate types: Colony, Citizen, Storyteller, Goal AI, BuildOrder;
- entity binding `citizenId` separately via `entity-integration.md` contract.

## 12. DoD модуля сохранения

- [ ] `NbtSaveAdapter` реализует `ISaveLoadPort` и зарегистрирован в composition root.
- [ ] Все агрегаты имеют корректные методы сериализации/десериализации.
- [ ] Структура файлов и NBT-схемы утверждены и задокументированы.
- [ ] `formatVersion`, `schemaVersion` и `aggregateVersion` реализованы.
- [ ] Миграции реализованы минимум для первой backwards-compatible schema change.
- [ ] Асинхронное сохранение работает через immutable snapshots и не блокирует игру.
- [ ] Обработка malformed/corrupted/future-version files реализована.
- [ ] Написаны integration tests сохранения/загрузки всех критичных aggregates.
- [ ] Написаны migration tests, включая idempotency.
- [ ] Проверены atomic write, backup и recovery paths.
- [ ] ArchUnit rules не нарушены.
- [ ] Runtime entity NBT и Core aggregate persistence не смешаны.

## 13. ArchUnit-правила

### 13.1 Ограничение зависимостей save infrastructure

Строгое правило из требований формулируется с разрешённым whitelist:

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.save..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core..",
        "com.rimworldcraft.core.ports..",
        "com.rimworldcraft.core..repository.."
    );
```

Из-за того, что адаптер обязан реализовать Core port, практическое исключение разрешает только `ISaveLoadPort`, repository contracts, `SaveDocument` и shared IDs. Лучше поместить эти API в `core.ports.persistence`/`core.common.persistence`, а не разрешать весь `core..`.

### 13.2 `NbtSaveAdapter` реализует `ISaveLoadPort`

```java
classes()
    .that().haveSimpleName("NbtSaveAdapter")
    .should().implement(ISaveLoadPort.class);
```

### 13.3 Adapter не содержит domain business logic

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.save..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core.colony.domain..",
        "com.rimworldcraft.core.npc.domain..",
        "com.rimworldcraft.core.story.domain..",
        "com.rimworldcraft.core.goal.domain..",
        "com.rimworldcraft.core.building.domain.."
    );
```

Business validation выполняется aggregate/mapper validator boundary; adapter занимается encoding, IO и lifecycle.

### 13.4 Запрет `System.out`

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.save..")
    .should().accessClassesThat()
    .belongToAnyOf(System.class);
```

Использовать logger/observability port.

### 13.5 Save serializers находятся вне Core domain

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.*.domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("com.rimworldcraft.infrastructure.save..", "net.minecraft.nbt..");
```

## 14. Будущее расширение

### 14.1 Новые поля

- добавлять optional field с default;
- поднять `schemaVersion`, если меняется структура/семантика;
- написать migration old → new;
- сохранить unknown extension fields, если это часть compatibility policy;
- обновить schema/docs/fixtures.

### 14.2 Новые файлы/агрегаты

Добавить aggregate type, key policy, mapper, repository adapter и contract tests. Не помещать данные нового контекста в существующий snapshot только ради удобства.

### 14.3 JSON вместо NBT

Создать `JsonFileSaveAdapter implements ISaveLoadPort`. Он использует те же `SaveDocument`, migration registry, atomic temp-file replace и contract suite. Core repositories не меняются.

```java
public final class JsonFileSaveAdapter implements ISaveLoadPort {
    private final Path root;
    private final SaveDocumentCodec codec;
    // same logical SaveKey and error semantics as NbtSaveAdapter
}
```

### 14.4 Database backend

`DatabaseSaveAdapter` может хранить envelope columns и payload JSON/blob. Требуются optimistic version, transaction, migration compatibility и backup policy. Нельзя менять domain aggregate из-за структуры таблиц.

### 14.5 Compression/encryption

Compression допустима за `ISaveLoadPort`. Encryption требует явного key management и не должна логировать ключи. Изменение формата фиксируется `formatVersion`, а не скрытой heuristic detection.

### 14.6 Event Store/outbox

Event outbox может сохраняться через тот же adapter, но имеет отдельный aggregate type `event-outbox`, retention и idempotency schema. Не смешивать audit events с authoritative aggregate snapshot без documented replay semantics.

## 15. Связи с другими документами

- [`hexagonal-architecture.md`](hexagonal-architecture.md) — `ISaveLoadPort`, driven adapters и composition root.
- [`data-interfaces.md`](data-interfaces.md) — repository contracts и mapper boundary.
- [`module-colony.md`](module-colony.md) — Colony aggregate и ownership ресурсов; также учитывается ожидаемое имя `module-colony-manager.md`.
- [`module-npc-core.md`](module-npc-core.md) — Citizen snapshot, traits, skills, needs и relationships.
- [`module-storyteller.md`](module-storyteller.md) — Storyteller, incidents, arcs и history retention.
- [`module-goal-ai.md`](module-goal-ai.md) — current plans, action queues и replan recovery.
- [`module-building-system.md`](module-building-system.md) — BuildOrder, Blueprint и GhostBlock snapshots.
- [`entity-integration.md`](entity-integration.md) — runtime entities, `citizenId` и отдельное entity NBT.
- [`event-system-api.md`](event-system-api.md) — outbox/event store и consistency policy.
- [`data-dictionaries.md`](data-dictionaries.md) — JSON config schema conventions и versioning.

## 16. Итог

Persistence layer сохраняет не Minecraft objects, а версионируемые snapshots доменных aggregates. `NbtSaveAdapter` реализует `ISaveLoadPort`, repositories отвечают за hydrate/dehydrate, migrations обновляют структуру, а recovery защищает мир от corrupt/future data. Immutable snapshots, atomic writes, optimistic versions и contract tests позволяют позднее заменить NBT JSON-файлами или базой данных без изменения Core.
