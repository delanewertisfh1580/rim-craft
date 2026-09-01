# RimWorldCraft — Data Interfaces

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

## 2. Основные концепции и термины

| Термин | Определение |
|---|---|
| **Repository** | Коллекция-подобный интерфейс для загрузки и сохранения aggregate roots. |
| **Factory** | Контракт создания валидного сложного объекта/агрегата из command, template или config. |
| **Specification** | Компонуемое бизнес-условие для проверки и поиска объектов. |
| **Aggregate** | Граница согласованного изменения с одним root. |
| **Value Object** | Immutable объект, определяемый значением, а не identity. |
| **ID** | Стабильная identity aggregate/entity, обычно value object вокруг UUID/string. |
| **Search Criteria** | Набор фильтров для запроса, paging, сортировки и scope. |
| **Pagination** | Ограниченный запрос страницей с `offset/limit` или cursor. |
| **Transactionality** | Гарантия атомарности операции относительно aggregate; cross-context transaction не подразумевается. |
| **Snapshot** | Сериализуемое состояние aggregate на момент времени. |
| **Optimistic Version** | Версия aggregate для обнаружения конкурентного обновления. |
| **Specification composition** | `and`, `or`, `not` без доступа к persistence implementation. |

## 3. Общая структура пакетов

```text
com.rimworldcraft.core.common.repository/
  IRepository.java
  ICrudRepository.java
  IPagingRepository.java
  PageRequest.java
  Page.java
  Sort.java

com.rimworldcraft.core.common.specification/
  ISpecification.java
  SpecificationException.java

com.rimworldcraft.core.colony.repository/
  IColonyRepository.java
  IZoneRepository.java

com.rimworldcraft.core.colony.factory/
  IColonyFactory.java

com.rimworldcraft.core.colony.specification/
  ColonyByPlayerSpecification.java
  ActiveColonySpecification.java

com.rimworldcraft.core.npc.repository/
  ICitizenRepository.java
  IRelationshipRepository.java

com.rimworldcraft.core.npc.factory/
  ICitizenFactory.java

com.rimworldcraft.core.npc.specification/
  CitizenBySkillSpecification.java
  CitizenByTraitSpecification.java

com.rimworldcraft.core.story.repository/
  IStorytellerRepository.java
  IIncidentRepository.java
  IStoryArcRepository.java

com.rimworldcraft.core.story.factory/
  IIncidentFactory.java

com.rimworldcraft.core.story.specification/
  IncidentByTypeSpecification.java
  ActiveIncidentSpecification.java

com.rimworldcraft.core.goal.repository/
  ICitizenAIRepository.java
  ITaskRepository.java

com.rimworldcraft.core.goal.specification/
  TaskByStatusSpecification.java
  TaskForCitizenSpecification.java

com.rimworldcraft.core.building.repository/
  IBuildOrderRepository.java
  IBlueprintRepository.java
  IGhostBlockRepository.java

com.rimworldcraft.core.building.factory/
  IBuildOrderFactory.java
  IBlueprintFactory.java

com.rimworldcraft.core.building.specification/
  BuildOrderByStatusSpecification.java
  BuildOrderByColonySpecification.java

com.rimworldcraft.infrastructure.repository/
  nbt/
  json/
  memory/
```

Имена из пользовательских требований `core.common`/`core.*` и ранее принятые namespaces `core.shared`/`core.ports` могут сосуществовать только при явном convention. Для нового кода рекомендуется `com.rimworldcraft.core.common` для generic contracts и `com.rimworldcraft.core.shared` для IDs/events.

## 4. Базовые типы

### 4.1 ID и version

```java
public record AggregateVersion(long value) {
    public AggregateVersion {
        if (value < 0) throw new IllegalArgumentException("version >= 0");
    }
}

public record PageRequest(int page, int size, Sort sort) {
    public PageRequest {
        if (page < 0 || size < 1 || size > 1000) {
            throw new IllegalArgumentException("Invalid page request");
        }
    }
    public int offset() { return page * size; }
}

public record Page<T>(List<T> content, PageRequest request, long totalElements) {
    public Page {
        content = List.copyOf(content);
        if (totalElements < 0) throw new IllegalArgumentException("total >= 0");
    }
}
```

### 4.2 Repository exceptions

```java
public class RepositoryException extends RuntimeException {
    public RepositoryException(String message, Throwable cause) { super(message, cause); }
}

public final class EntityNotFoundException extends RepositoryException {
    public EntityNotFoundException(String type, Object id) {
        super(type + " not found: " + id, null);
    }
}

public final class ConcurrentModificationException extends RepositoryException {
    public ConcurrentModificationException(String message) { super(message, null); }
}
```

Низкоуровневые `IOException`, NBT errors и SQL exceptions адаптер переводит в `RepositoryException`/`PersistenceException`.

## 5. Базовые интерфейсы репозиториев

### 5.1 `IRepository<T, ID>`

```java
public interface IRepository<T, ID> {
    Optional<T> findById(ID id);
    T save(T entity);
    void delete(T entity);
    void deleteById(ID id);
    boolean existsById(ID id);
    long count();
}
```

`T` — тип aggregate/entity, которым владеет repository; `ID` — его identity. `findById` возвращает empty при отсутствии. `save` возвращает canonical saved object, включая generated ID/version. `delete` должен быть идемпотентным либо явно документировать `EntityNotFoundException`; для общих репозиториев выбран idempotent delete.

### 5.2 `ICrudRepository<T, ID>`

```java
public interface ICrudRepository<T, ID> extends IRepository<T, ID> {
    List<T> findAll();
    List<T> findAllById(Iterable<ID> ids);
    List<T> saveAll(Iterable<T> entities);
    void deleteAll();
}
```

Порядок `findAllById` соответствует порядку входных IDs, если это обещано конкретным backend; иначе результат должен быть documented. `saveAll` атомарен на уровне batch только если конкретный contract это гарантирует; базовый contract гарантирует, что каждый элемент либо сохранён, либо ошибка явно возвращена.

### 5.3 `IPagingRepository<T, ID>`

```java
public interface IPagingRepository<T, ID> extends ICrudRepository<T, ID> {
    Page<T> findAll(PageRequest pageRequest);
    Page<T> findAll(ISpecification<T> specification, PageRequest pageRequest);
}
```

Paging должен быть deterministic: sort обязателен для больших/изменяемых наборов. `PageRequest.size` ограничен, чтобы запрос не загрузил весь world state.

### 5.4 Асинхронные варианты

Minecraft server tick обычно требует bounded synchronous operation. Async API допускается только для IO, с явным contract:

```java
public interface AsyncRepository<T, ID> {
    CompletableFuture<Optional<T>> findByIdAsync(ID id);
    CompletableFuture<T> saveAsync(T entity);
}
```

Async completion не должен мутировать Minecraft world из worker thread. Если async repository добавляется, нужен lifecycle/cancellation policy и integration test.

## 6. Colony repositories

### 6.1 `IColonyRepository`

```java
public interface IColonyRepository
        extends IPagingRepository<Colony, ColonyId> {
    Optional<Colony> findByColonyId(WorldId worldId, ColonyId colonyId);
    Optional<Colony> findByName(WorldId worldId, ColonyName name);
    List<Colony> findAllActive(WorldId worldId);
    List<Colony> findByPlayerId(WorldId worldId, PlayerId playerId);
    Colony updateValue(WorldId worldId, ColonyId colonyId, long newValue);
    Colony addResource(WorldId worldId, ColonyId colonyId,
                       ResourceType type, int amount);
    Colony removeResource(WorldId worldId, ColonyId colonyId,
                          ResourceType type, int amount);
}
```

#### Контракты методов

| Метод | Возвращает | Условия и ошибки |
|---|---|---|
| `findByColonyId` | `Optional<Colony>` | empty при отсутствии; cross-world ID не смешивать |
| `findByName` | `Optional<Colony>` | name normalized; duplicate names запрещены в world |
| `findAllActive` | `List<Colony>` | только `ACTIVE`; bounded/paged при большом мире |
| `findByPlayerId` | `List<Colony>` | возвращает доступные игроку colonies согласно projection |
| `updateValue` | saved `Colony` | value `>=0`; not-found/concurrency error |
| `addResource` | saved `Colony` | amount `>0`; resource type valid |
| `removeResource` | saved `Colony` | amount `>0`; insufficient resources domain error |

#### Пример использования

```java
Colony colony = colonies.findByColonyId(worldId, colonyId)
        .orElseThrow(() -> new ColonyNotFoundException(colonyId));

Colony updated = colonies.removeResource(
        worldId, colony.id(), ResourceType.of("minecraft:oak_log"), 4);
```

Методы `addResource/removeResource` допустимы как application convenience, но не должны обходить Colony aggregate invariants. Если backend вызывает их напрямую, adapter обязан hydrate → invoke domain method → save.

### 6.2 `IZoneRepository`

```java
public interface IZoneRepository
        extends ICrudRepository<Zone, ZoneId> {
    List<Zone> findByColonyId(WorldId worldId, ColonyId colonyId);
    Optional<Zone> findByPosition(GridPosition position);
    List<Zone> findOverlappingZones(Zone zone);
}
```

- `findByColonyId` возвращает только зоны scope colony/world.
- `findByPosition` использует deterministic boundary semantics.
- `findOverlappingZones` не мутирует zone и учитывает только тот же `WorldId`.
- invalid position/zone → `ValidationException`.

Пример:

```java
List<Zone> conflicts = zones.findOverlappingZones(candidate);
if (!conflicts.isEmpty()) throw new ZoneOverlapException(candidate.id());
```

## 7. NPC repositories

### 7.1 `ICitizenRepository`

```java
public interface ICitizenRepository
        extends IPagingRepository<Citizen, CitizenId> {
    Optional<Citizen> findById(WorldId worldId, CitizenId citizenId);
    List<Citizen> findAllByColonyId(WorldId worldId, ColonyId colonyId);
    List<Citizen> findAllByColonyIdWithRelations(
            WorldId worldId, ColonyId colonyId);
    List<Citizen> findBySkillLevel(
            WorldId worldId, SkillType skill, int minLevel);
    List<Citizen> findByTrait(WorldId worldId, String traitId);
    Citizen updateMood(WorldId worldId, CitizenId citizenId, int newMood);
    Citizen updateSkills(WorldId worldId, CitizenId citizenId,
                         Map<SkillType, Skill> skills);
}
```

| Метод | Контракт |
|---|---|
| `findById` | empty при отсутствии; `WorldId` обязателен |
| `findAllByColonyId` | не загружает relations graph по умолчанию |
| `findAllByColonyIdWithRelations` | explicit eager read model; защищать от unbounded graph |
| `findBySkillLevel` | `minLevel 0..100`; invalid range → validation error |
| `findByTrait` | `traitId` должен быть normalized; unknown ID даёт empty |
| `updateMood` | `0..100`; должен пройти Citizen invariant |
| `updateSkills` | skill map immutable/canonical; levels validated |

Пример:

```java
List<Citizen> builders = citizens.findBySkillLevel(
        worldId, SkillType.BUILDING, 50);
```

`updateMood/updateSkills` — не raw SQL update: реализация должна сохранить aggregate semantics и publish events через application layer или outbox.

### 7.2 `IRelationshipRepository`

```java
public interface IRelationshipRepository
        extends IRepository<Relationship, RelationshipKey> {
    List<Relationship> findByCitizenId(WorldId worldId, CitizenId citizenId);
    Optional<Relationship> findRelationship(
            WorldId worldId, CitizenId citizenId1, CitizenId citizenId2);
    Relationship save(Relationship relationship);
    void deleteByCitizenId(WorldId worldId, CitizenId citizenId);
}

public record RelationshipKey(CitizenId source, CitizenId target) {}
```

Self-links запрещены. `deleteByCitizenId` удаляет source links и, согласно policy, target links; policy должна быть едина для всех adapters. Если relationships хранятся внутри `Citizen`, этот repository может быть facade над `ICitizenRepository`.

## 8. Storyteller repositories

### 8.1 `IStorytellerRepository`

```java
public interface IStorytellerRepository
        extends IRepository<Storyteller, StorytellerId> {
    Optional<Storyteller> findByColonyId(WorldId worldId, ColonyId colonyId);
    Storyteller save(Storyteller storyteller);
    void deleteByColonyId(WorldId worldId, ColonyId colonyId);
}
```

`findByColonyId` обычно возвращает один root; duplicate storyteller per colony — corruption/conflict. `deleteByColonyId` идемпотентен.

### 8.2 `IIncidentRepository`

```java
public interface IIncidentRepository
        extends IPagingRepository<Incident, IncidentId> {
    List<Incident> findActiveByColonyId(WorldId worldId, ColonyId colonyId);
    List<Incident> findResolvedByColonyId(WorldId worldId, ColonyId colonyId);
    List<Incident> findByType(WorldId worldId, String type);
    List<Incident> findByStatus(WorldId worldId, IncidentStatus status);
}
```

`type` нормализуется в `IncidentType`, а не передаётся произвольной SQL string. `findByStatus` ограничивается world scope. Unknown status/type → validation error, missing records → empty list.

### 8.3 `IStoryArcRepository`

```java
public interface IStoryArcRepository
        extends IPagingRepository<StoryArc, StoryArcId> {
    List<StoryArc> findActiveByColonyId(WorldId worldId, ColonyId colonyId);
    List<StoryArc> findCompletedByColonyId(WorldId worldId, ColonyId colonyId);
    List<StoryArc> findByStatus(WorldId worldId, ArcStatus status);
}
```

Один colony может иметь несколько arcs только при explicit config policy. Repository не продвигает arc — это ответственность Storyteller service.

## 9. Goal AI repositories

### 9.1 `ICitizenAIRepository`

```java
public interface ICitizenAIRepository
        extends IRepository<CitizenAI, CitizenId> {
    Optional<CitizenAI> findByCitizenId(WorldId worldId, CitizenId citizenId);
    CitizenAI save(CitizenAI ai);
    void deleteByCitizenId(WorldId worldId, CitizenId citizenId);
}
```

AI state scoped by world/citizen. При загрузке stale plan может быть сохранён, но application layer обязан revalidate against current WorldState.

### 9.2 `ITaskRepository`

```java
public interface ITaskRepository
        extends IPagingRepository<Task, TaskId> {
    List<Task> findByCitizenId(WorldId worldId, CitizenId citizenId);
    List<Task> findPendingTasks(WorldId worldId);
    List<Task> findByStatus(WorldId worldId, TaskStatus status);
}
```

`findPendingTasks` возвращает только unassigned/available tasks согласно status semantics; assignment выполняется через TaskManager, не прямым list mutation.

## 10. Building repositories

### 10.1 `IBuildOrderRepository`

```java
public interface IBuildOrderRepository
        extends IPagingRepository<BuildOrder, BuildOrderId> {
    Optional<BuildOrder> findById(WorldId worldId, BuildOrderId orderId);
    List<BuildOrder> findAllByColonyId(WorldId worldId, ColonyId colonyId);
    List<BuildOrder> findByStatus(WorldId worldId, BuildOrderStatus status);
    List<BuildOrder> findByCitizenId(WorldId worldId, CitizenId citizenId);
    BuildOrder updateProgress(WorldId worldId, BuildOrderId orderId, int progress);
    BuildOrder cancel(WorldId worldId, BuildOrderId orderId, String reason);
}
```

- progress `0..100` и не должен уменьшаться без repair policy;
- `updateProgress` не может переводить cancelled/completed order обратно в progress;
- `cancel` освобождает resources через application service/outbox, а не silently в repository;
- `findByCitizenId` учитывает assigned list.

### 10.2 `IBlueprintRepository`

```java
public interface IBlueprintRepository
        extends ICrudRepository<Blueprint, BlueprintId> {
    Optional<Blueprint> findById(String blueprintId);
    List<Blueprint> findAll();
    List<Blueprint> findByResource(ResourceType type);
    Blueprint save(Blueprint blueprint);
}
```

Blueprints могут быть immutable catalog records. `save` разрешён для player-created blueprint; built-in IDs не должны перезаписываться без content ownership policy.

### 10.3 `IGhostBlockRepository`

```java
public interface IGhostBlockRepository
        extends IPagingRepository<GhostBlock, GhostBlockId> {
    List<GhostBlock> findByPosition(GridPosition position);
    List<GhostBlock> findByBuildOrderId(WorldId worldId, BuildOrderId orderId);
    GhostBlock save(GhostBlock ghostBlock);
    void deleteByPosition(GridPosition position);
}
```

На одной позиции может быть максимум один active ghost block, если building policy не разрешает layers. `deleteByPosition` идемпотентен и проверяет `WorldId`.

## 11. Фабрики

### 11.1 Общие правила factory

Factory:

- возвращает полностью валидный объект;
- не сохраняет его автоматически, если это не указано contract;
- не спаунит Minecraft entity самостоятельно;
- использует `RandomPort`, `ITimePort` и config ports через constructor injection;
- переводит missing template/config в domain exception;
- обеспечивает deterministic generation при заданном seed.

### 11.2 `ICitizenFactory`

```java
public interface ICitizenFactory {
    Citizen createRandomCitizen(WorldId worldId, GenerationSeed seed);
    Citizen createColonist(WorldId worldId, ColonyId colonyId,
                           GenerationSeed seed);
    Citizen createEnemy(WorldId worldId, String faction,
                        GenerationSeed seed);
    Citizen createFromTemplate(WorldId worldId, String templateId,
                               GenerationSeed seed);
}
```

Предусловия: known role/template, valid world, required config pools. Ошибки: `InvalidNpcTemplateException`, `NamePoolEmptyException`, `ConfigurationException`.

### 11.3 `IIncidentFactory`

```java
public interface IIncidentFactory {
    Incident createRaid(ColonySummary colony, int difficulty);
    Incident createTradeCaravan(ColonySummary colony, TradeProfile profile);
    Incident createDisaster(ColonySummary colony, DisasterType type);
}
```

Factory не изменяет Colony и не спаунит entities. `difficulty` имеет configured range; missing spawn capability возвращается как intent validation failure, а не создаётся неполный incident.

### 11.4 `IBuildOrderFactory`

```java
public interface IBuildOrderFactory {
    BuildOrder createFromBlueprint(Blueprint blueprint,
                                   ColonySummary colony,
                                   GridPosition position);
    BuildOrder createFromGhostBlock(GhostBlock ghostBlock);
}
```

Проверки: blueprint valid, position same world, resources calculated, collision handled by validator/service, priority `1..10`. Фабрика может создать order с required resources, но reservation выполняет `ResourceManager`.

### 11.5 `IBlueprintFactory`

```java
public interface IBlueprintFactory {
    Blueprint createFromPrefab(String prefabId);
    Blueprint createFromPlayerSelection(Selection selection);
}
```

`Selection` — Core DTO bounded by max dimensions/block count. Prefab ID должен существовать в validated `prefabs.json`. Minecraft blocks нормализуются в namespaced string IDs.

## 12. Спецификации

### 12.1 `ISpecification<T>`

```java
public interface ISpecification<T> {
    boolean isSatisfiedBy(T entity);
    default ISpecification<T> and(ISpecification<T> other) {
        return entity -> isSatisfiedBy(entity) && other.isSatisfiedBy(entity);
    }
    default ISpecification<T> or(ISpecification<T> other) {
        return entity -> isSatisfiedBy(entity) || other.isSatisfiedBy(entity);
    }
    default ISpecification<T> not() {
        return entity -> !isSatisfiedBy(entity);
    }
}
```

Specification должна быть side-effect free, deterministic и не ходить в repository. Если backend умеет переводить specification в native query, это оптимизация adapter; truth semantics должны совпадать с `isSatisfiedBy`.

### 12.2 Примеры

```java
public final class CitizenBySkillSpecification
        implements ISpecification<Citizen> {
    private final SkillType skill;
    private final int minLevel;

    public CitizenBySkillSpecification(SkillType skill, int minLevel) {
        if (minLevel < 0 || minLevel > 100) throw new IllegalArgumentException();
        this.skill = skill;
        this.minLevel = minLevel;
    }

    @Override
    public boolean isSatisfiedBy(Citizen citizen) {
        return citizen.skill(skill).level() >= minLevel;
    }
}

public final class BuildOrderByStatusSpecification
        implements ISpecification<BuildOrder> {
    private final BuildOrderStatus status;

    public BuildOrderByStatusSpecification(BuildOrderStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    @Override
    public boolean isSatisfiedBy(BuildOrder order) {
        return order.status() == status;
    }
}

public final class IncidentByTypeSpecification
        implements ISpecification<Incident> {
    private final IncidentType type;

    @Override
    public boolean isSatisfiedBy(Incident incident) {
        return incident.type() == type;
    }
}
```

### 12.3 Использование в repository

```java
ISpecification<BuildOrder> ready = new BuildOrderByStatusSpecification(PENDING)
        .and(order -> order.priority().value() >= 7)
        .and(order -> order.resourcesRequired().equals(order.resourcesReserved()));

Page<BuildOrder> page = buildOrders.findAll(
        ready, new PageRequest(0, 50, Sort.by("priority", DESC)));
```

### 12.4 Спецификации контекстов

| Контекст | Specification |
|---|---|
| Colony | `ColonyByPlayerSpecification`, `ActiveColonySpecification`, `ColonyValueAtLeastSpecification` |
| NPC | `CitizenBySkillSpecification`, `CitizenByTraitSpecification`, `CitizenAvailableSpecification` |
| Storyteller | `IncidentByTypeSpecification`, `ActiveIncidentSpecification`, `IncidentEligibleSpecification` |
| Goal AI | `TaskByStatusSpecification`, `TaskForCitizenSpecification`, `GoalPrioritySpecification` |
| Building | `BuildOrderByStatusSpecification`, `BuildOrderByColonySpecification`, `BlueprintUsesResourceSpecification` |

## 13. Паттерны и соглашения

### 13.1 Return types

- поиск одного объекта → `Optional<T>`;
- коллекция → никогда `null`, при отсутствии `List.of()`;
- paging → `Page<T>`;
- mutation → сохранённый `T` либо typed result;
- batch → `List<T>` с documented atomicity;
- async → `CompletableFuture<T>` только если operation действительно IO-bound.

### 13.2 Именование

- `find...` — поиск без mutation;
- `get...` — допустим только если отсутствие является ошибкой и это documented;
- `save/saveAll` — persistence;
- `delete/deleteById` — удаление;
- `update...` — атомарная domain-aware mutation;
- `findAllBy...` — коллекционный фильтр;
- `findBy...` — predicate, обычно single или list по семантике.

### 13.3 Transactionality и concurrency

Repository сохраняет один aggregate атомарно. Рекомендуется optimistic version:

```java
public interface VersionedAggregate {
    AggregateVersion version();
}
```

Если version не совпадает, adapter бросает `ConcurrentModificationException`. Application service загружает свежий aggregate, повторяет domain operation только при безопасной idempotency policy.

### 13.4 Межконтекстные данные

Repository одного контекста не возвращает aggregate другого. Например:

- NPC repository возвращает `Citizen`/`CitizenSummary`, но не `Colony`;
- Story repository получает `ColonySummaryPort`, а не `IColonyRepository` с обходом ownership;
- Building repository хранит `ColonyId` и `CitizenId`, но не копирует aggregates;
- Goal AI хранит `TaskId`/summaries и не импортирует `Citizen` aggregate.

## 14. Примеры инфраструктурных реализаций

### 14.1 `NbtColonyRepository`

```java
public final class NbtColonyRepository implements IColonyRepository {
    private final ISaveLoadPort storage;
    private final ColonySnapshotMapper mapper;

    public NbtColonyRepository(ISaveLoadPort storage,
                                ColonySnapshotMapper mapper) {
        this.storage = storage;
        this.mapper = mapper;
    }

    @Override
    public Optional<Colony> findByColonyId(WorldId worldId, ColonyId colonyId) {
        SaveKey key = new SaveKey(worldId, "colony", colonyId.value().toString());
        return storage.load(key).map(mapper::toDomain);
    }

    @Override
    public Colony save(Colony colony) {
        SaveKey key = new SaveKey(colony.worldId(), "colony",
                colony.id().value().toString());
        SaveDocument document = mapper.toDocument(colony);
        storage.save(key, document);
        return colony;
    }

    @Override
    public void deleteById(ColonyId id) {
        // world scope is supplied by the concrete repository/application boundary
    }

    // Remaining methods delegate to bounded index/query projections.
}
```

Адаптер использует `ISaveLoadPort`, но `NbtColonyRepository` может быть назван NBT implementation, потому что NBT knowledge находится только в infrastructure mapper/port implementation. Не помещать `CompoundTag` в method signatures Core repository.

### 14.2 Mapper

```java
public interface ColonySnapshotMapper {
    SaveDocument toDocument(Colony colony);
    Colony toDomain(SaveDocument document);
}
```

Mapper проверяет schemaVersion, мигрирует старые snapshots и переводит corruption в `PersistenceException`.

### 14.3 In-memory repository

```java
public final class InMemoryCitizenRepository implements ICitizenRepository {
    private final Map<CitizenId, Citizen> data = new HashMap<>();

    @Override
    public Optional<Citizen> findById(WorldId worldId, CitizenId id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public Citizen save(Citizen citizen) {
        data.put(citizen.id(), citizen);
        return citizen;
    }
}
```

Тестовая реализация должна соблюдать те же semantic contracts: scope, duplicate handling, optional behavior и validation.

### 14.4 `CitizenFactory` implementation

```java
public final class DefaultCitizenFactory implements ICitizenFactory {
    private final TraitGenerator traits;
    private final NameGenerator names;
    private final SkillProfileGenerator skills;
    private final NpcConfigRepository config;

    @Override
    public Citizen createColonist(WorldId worldId, ColonyId colonyId,
                                  GenerationSeed seed) {
        NpcArchetype archetype = config.archetype("colonist");
        return Citizen.create(
                CitizenId.newId(),
                names.generate(archetype.race(), archetype.culture(),
                        archetype.gender(), seed.random()),
                archetype.gender(), archetype.race(),
                traits.generate(archetype, seed.random()),
                skills.generate(archetype, seed.random()),
                config.initialNeeds(), colonyId,
                PositionFactory.initial(worldId, seed));
    }
}
```

Factory не вызывает `ICitizenRepository.save`; это делает use case, чтобы creation и persistence boundaries были видны.

## 15. Тестирование интерфейсов и контрактов

### 15.1 Repository contract tests

Каждая реализация обязана запускать общий abstract test suite.

```java
abstract class ColonyRepositoryContractTest {
    protected abstract IColonyRepository repository();
    protected abstract Colony newColony();

    @Test
    void saveThenFindByIdReturnsEntity() {
        Colony colony = newColony();
        repository().save(colony);

        Optional<Colony> loaded = repository().findByColonyId(
                colony.worldId(), colony.id());

        assertThat(loaded).contains(colony);
    }

    @Test
    void findMissingReturnsEmpty() {
        assertThat(repository().findByColonyId(
                WorldFixtures.id(), ColonyId.newId())).isEmpty();
    }

    @Test
    void deleteRemovesEntity() {
        Colony colony = newColony();
        repository().save(colony);
        repository().deleteById(colony.id());
        assertThat(repository().findByColonyId(
                colony.worldId(), colony.id())).isEmpty();
    }

    @Test
    void resourceRemovalRejectsInsufficientAmount() {
        Colony colony = ColonyFixtures.withResource("minecraft:stone", 1);
        repository().save(colony);
        assertThatThrownBy(() -> repository().removeResource(
                colony.worldId(), colony.id(), ResourceType.of("minecraft:stone"), 2))
                .isInstanceOf(InsufficientResourcesException.class);
    }
}
```

Подклассы запускают этот suite для `NbtColonyRepository`, `InMemoryColonyRepository` и будущих adapters.

### 15.2 Обязательные contract suites

| Contract | Проверки |
|---|---|
| `IColonyRepository` | save/find/delete, name uniqueness, resource invariants, value update |
| `ICitizenRepository` | scope, skill/trait queries, mood/skill validation |
| `IStorytellerRepository` | one-per-colony, lifecycle, delete |
| `IBuildOrderRepository` | progress monotonicity, status filters, cancel semantics |
| `IRepository` | optional, count, idempotent deletion |
| `IPagingRepository` | stable order, page bounds, total count |

### 15.3 Factory tests

- `createColonist` returns valid colony-bound citizen;
- same seed gives reproducible profile where policy requires;
- missing template fails with domain exception;
- traits conflicts are rejected;
- `createRaid` does not mutate colony;
- blueprint factory rejects oversized player selection.

### 15.4 Specification tests

Specification tests проверяют truth table и composition:

```java
@Test
void skillAndAvailabilitySpecificationMatchesBothRules() {
    ISpecification<Citizen> spec = new CitizenBySkillSpecification(
            SkillType.BUILDING, 50)
            .and(CitizenSpecifications.available());

    assertThat(spec.isSatisfiedBy(CitizenFixtures.builderAvailable())).isTrue();
    assertThat(spec.isSatisfiedBy(CitizenFixtures.builderIncapacitated())).isFalse();
}
```

### 15.5 Mock-тесты application services

Mockito используется для проверки collaboration, например:

```java
@Test
void buildingServiceSavesOrderAndReservesResources() {
    when(blueprints.findById("oak_wall")).thenReturn(Optional.of(BlueprintFixtures.oakWall()));
    when(colonies.findByColonyId(worldId, colonyId))
            .thenReturn(Optional.of(ColonyFixtures.standard(colonyId)));

    BuildOrder result = service.createOrder(command);

    verify(orders).save(any(BuildOrder.class));
    verify(inventory).reserve(any(ResourceReservation.class));
    assertThat(result.status()).isEqualTo(BuildOrderStatus.PENDING);
}
```

## 16. ArchUnit-правила интерфейсов данных

### 16.1 Repository packages содержат только interfaces

```java
classes()
    .that().resideInAnyPackage("com.rimworldcraft.core..repository..")
    .should().beInterfaces();
```

### 16.2 Core repositories не зависят от implementations

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core..repository..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.infrastructure..",
        "net.minecraft..",
        "net.minecraftforge..",
        "net.fabricmc.."
    );
```

### 16.3 Core repositories используют доменные типы

Полностью запретить primitives невозможно для scalar values, поэтому правило формулируется через forbidden infrastructure types и обязательные value objects для IDs:

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core..repository..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("java.sql..", "java.io..", "net.minecraft..");

noMethods()
    .that().areDeclaredInClassesThat()
    .resideInAnyPackage("com.rimworldcraft.core..repository..")
    .should().haveRawReturnType(UUID.class);
```

Методы используют `ColonyId`, `CitizenId`, `WorldId`, `ResourceType`, `GridPosition`, а не голые UUID/string там, где существует domain type.

### 16.4 Запрет framework annotations

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core..repository..")
    .should().beAnnotatedWith("org.springframework.stereotype.Repository");
```

Аналогично запрещаются CDI/Spring/Forge persistence annotations в Core.

### 16.5 Factories/specifications — Core only

```java
noClasses()
    .that().resideInAnyPackage(
        "com.rimworldcraft.core..factory..",
        "com.rimworldcraft.core..specification..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("com.rimworldcraft.infrastructure..", "net.minecraft..");
```

### 16.6 Implementations находятся в Infrastructure

```java
classes()
    .that().haveSimpleNameEndingWith("Repository")
    .and().resideOutsideOfPackage("com.rimworldcraft.core..")
    .should().resideInAnyPackage("com.rimworldcraft.infrastructure.repository..");
```

В проекте можно выбрать suffix `Adapter`; тогда правило должно требовать `RepositoryAdapter` и соответствующий package.

### 16.7 No cycles

```java
slices().matching("com.rimworldcraft.core.(*)..")
    .should().beFreeOfCycles();
```

## 17. DoD интерфейсов данных

- [ ] Все интерфейсы репозиториев определены и задокументированы Javadoc.
- [ ] Все интерфейсы фабрик определены и задокументированы.
- [ ] Все specifications определены хотя бы для базовых фильтров контекстов.
- [ ] Написаны contract tests для каждого repository implementation.
- [ ] Написаны mock-тесты для сервисов, использующих repositories/factories.
- [ ] ArchUnit rules интерфейсов данных проходят.
- [ ] Все интерфейсы находятся в `core.*`, а не в `infrastructure`.
- [ ] Методы используют `Optional`, immutable collections и domain IDs.
- [ ] Документированы concurrency/version, paging, deletion и transaction semantics.
- [ ] Низкоуровневые exceptions переводятся в Core-facing errors.
- [ ] Cross-context repositories не возвращают чужие aggregates.
- [ ] Для NBT/JSON/database реализаций есть единый contract suite.

## 18. Расширение и эволюция

### 18.1 Добавление метода

Новый метод нельзя добавлять в общий `IRepository` только ради одного контекста. Порядок:

1. проверить, является ли capability общей;
2. если нет — добавить в context-specific repository;
3. если метод breaking для adapters — создать `IColonySearchRepositoryV2` или отдельный interface;
4. обновить contract tests;
5. реализовать adapters;
6. обновить documentation и ArchUnit.

### 18.2 Default methods

`default`-метод допустим только для behavior, основанного на уже существующих primitive methods и не требующего backend semantics:

```java
public interface IRepository<T, ID> {
    Optional<T> findById(ID id);
    default T getRequired(ID id) {
        return findById(id).orElseThrow(() ->
                new EntityNotFoundException("Entity", id));
    }
}
```

Нельзя использовать default method для mutation, которая может нарушить atomicity конкретного backend.

### 18.3 Новые backend

Для database adapter:

- реализовать тот же repository interface;
- использовать mapper `domain ↔ persistence record`;
- перевести SQL/IO exceptions;
- пройти contract suite;
- обеспечить version/concurrency semantics;
- подключить через composition root.

Core services не меняются.

### 18.4 CQRS и read repositories

Если queries становятся тяжёлыми, добавить отдельные read ports:

```java
public interface ColonySummaryQuery {
    Page<ColonySummary> search(ColonySearchCriteria criteria,
                               PageRequest pageRequest);
}
```

Не расширять aggregate repository projection-specific methods бесконечно. Read model не может использоваться для mutation.

### 18.5 Event-driven persistence

После принятия `event-system-api.md` repository может публиковать outbox event, но repository не должен сам находить всех subscribers. Используются `IEventBusPort`/outbox adapter и idempotency marker.

## 19. Связи с другими документами

- [`hexagonal-architecture.md`](hexagonal-architecture.md) — repositories как driven ports, DI, `ISaveLoadPort` и adapter boundaries.
- [`bounded-contexts.md`](bounded-contexts.md) — aggregate ownership, IDs и запрет прямых межконтекстных зависимостей.
- `module-colony-manager.md` — `IColonyRepository`, `IZoneRepository`, resources и colony state.
- [`module-npc-core.md`](module-npc-core.md) — `ICitizenRepository`, `IRelationshipRepository`, Citizen factory и NPC state.
- `module-storyteller.md` — storyteller, incident и story arc repositories/factories.
- [`module-goal-ai.md`](module-goal-ai.md) — `ICitizenAIRepository`, `ITaskRepository` и task specifications.
- [`module-building-system.md`](module-building-system.md) — build order, blueprint и ghost block repositories/factories.
- [`event-system-api.md`](event-system-api.md) — event publication, outbox, idempotency и integration chains.
- [`data-dictionaries.md`](data-dictionaries.md) — config IDs, schema versions и JSON mappings.
- `save-serialization.md` — NBT snapshot mapping и persistence migrations.
- `testing-strategy.md` — repository contract tests, mocks и integration matrix.

## 20. Итоговый checklist выбора контракта

Перед созданием нового интерфейса разработчик отвечает:

1. Какой bounded context владеет данными?
2. Это aggregate repository, read query, factory или specification?
3. Должен ли результат быть `Optional`, `Page` или typed result?
4. Какие ID/value objects используются?
5. Каковы scope, transaction и concurrency guarantees?
6. Что происходит при missing/corrupt/duplicate data?
7. Может ли метод нарушить aggregate invariants?
8. Есть ли общий contract test?
9. Не импортирует ли Core инфраструктуру или framework?
10. Нужна ли новая версия interface вместо breaking change?

Если ответы не задокументированы, интерфейс не готов к добавлению в Core.
