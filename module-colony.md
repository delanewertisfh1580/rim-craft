# RimWorldCraft — модуль Colony

## 1. Назначение документа

Этот документ описывает реализацию `ColonyContext` мода RimWorldCraft: агрегаты, правила владения данными, use cases, порты, события, конфигурацию, тестирование и критерии готовности.

Документ самодостаточен: он объясняет необходимые концепции DDD и гексагональной архитектуры, а ссылки в конце ведут к более подробным общесистемным соглашениям.

`ColonyContext` моделирует коллективное поселение игрока. Он является владельцем:

- идентичности и жизненного цикла колонии;
- членства жителей через `NpcId`;
- запасов и резерваций ресурсов;
- рабочих политик и зон;
- колониальных целей, wealth и aggregate-level morale;
- событий изменения состояния колонии.

Он **не** владеет внутренними needs/health NPC, Minecraft blocks/entities, player authentication или выбором Storyteller incidents.

## 2. Архитектурная модель

### 2.1 Bounded Context

Bounded Context — граница, внутри которой термин имеет однозначный смысл и действует один набор инвариантов. В Colony слово «resource» означает ресурс, принадлежащий колониальному ledger; в NPCContext тот же предмет может быть только целью задания. `Colony` не должен передаваться соседнему контексту как объект.

Соседние контексты используют:

- `ColonyId`, `NpcId`, `PlayerId`, `WorldId`;
- read-only summaries;
- версионируемые domain/integration events;
- публичные application ports.

### 2.2 Гексагональная граница

```text
Forge/Fabric packet, GUI, tick
              |
              v
  Colony driving adapters
              |
              v
  core.colony.port.in / use cases
              |
              v
  Colony aggregate + domain policies
              |
              v
  core.colony.port.out
              |
              v
 Minecraft world | save | event bus | config adapters
```

Core не импортирует `net.minecraft.*`, `net.minecraftforge.*` или `net.fabricmc.*`. Все внешние типы преобразуются в Core DTO на границе.

### 2.3 Пакетная структура

```text
com.rimworldcraft.core.colony/
  domain/
    Colony.java
    ColonyStatus.java
    ColonistMembership.java
    InventoryLedger.java
    WorkPolicy.java
    WorkZone.java
    ColonyObjective.java
    value/
    policy/
  application/
    CreateColonyService.java
    AssignWorkService.java
    ManageColonyService.java
    ColonyEventHandler.java
    query/
  port/
    in/
      CreateColonyUseCase.java
      AssignWorkUseCase.java
      ManageColonyZoneUseCase.java
      ApplyProductionResultUseCase.java
      GetColonyView.java
    out/
      ColonyRepository.java
      WorldSettlementPort.java
      ColonyEventPublisher.java
      ColonyConfigPort.java
      ColonyMembershipQueryPort.java
  event/
    ColonyFounded.java
    ColonistJoined.java
    WorkAssigned.java
    ColonyValueChangedEvent.java
  contract/
    ColonySummary.java
    ColonyView.java

com.rimworldcraft.infrastructure.adapters.driving.colony/
com.rimworldcraft.infrastructure.adapters.driven.colony/
```

## 3. Модель домена

### 3.1 Aggregate Root `Colony`

Aggregate Root — единственная точка изменения агрегата. Внешний код не получает mutable collections и не меняет поля напрямую.

```java
public final class Colony {
    private final ColonyId id;
    private final WorldId worldId;
    private ColonyName name;
    private SettlementSite site;
    private ColonyStatus status;
    private final Map<NpcId, ColonistMembership> members;
    private final InventoryLedger inventory;
    private WorkPolicy workPolicy;
    private final Set<WorkZone> zones;
    private final ColonyObjectives objectives;
    private MoraleSnapshot morale;
    private ColonyWealth wealth;
    private final List<DomainEvent> pendingEvents;

    public void addColonist(NpcId npcId, MembershipRole role, GameTick joinedAt) {
        requireActive();
        if (members.containsKey(npcId)) {
            throw new ColonyConflictException("NPC_ALREADY_MEMBER");
        }
        members.put(npcId, ColonistMembership.create(npcId, role, joinedAt));
        pendingEvents.add(new ColonistJoined(id, npcId, worldId, joinedAt));
    }

    public void assignWork(NpcId npcId, WorkTypeId workTypeId, WorkPriority priority) {
        requireActive();
        requireMember(npcId);
        workPolicy.assign(npcId, workTypeId, priority);
        pendingEvents.add(new WorkAssigned(id, npcId, workTypeId, priority));
    }

    public void applyResourceDelta(ResourceType type, int delta, ResourceChangeReason reason) {
        requireActive();
        inventory.apply(type, delta, reason);
        pendingEvents.add(new ColonyValueChangedEvent(
                id, worldId, "RESOURCE", type.value(), delta));
    }
}
```

Пример выше иллюстрирует модель; конкретные конструкторы, persistence mapper и тип `DomainEvent` должны соответствовать общим проектным conventions.

### 3.2 Состав агрегата

| Элемент | Тип | Назначение | Владелец |
|---|---|---|---|
| `ColonyId` | value object | стабильная идентичность | `Colony` |
| `WorldId` | value object | scope мира | Shared Kernel |
| `ColonyName` | value object | валидируемое имя | `Colony` |
| `SettlementSite` | value object | место основания | `Colony` |
| `ColonistMembership` | entity | роль `NpcId` в колонии | `Colony` |
| `InventoryLedger` | entity/value collection | запасы, reservations, transactions | `Colony` |
| `WorkPolicy` | entity | приоритеты и work assignments | `Colony` |
| `WorkZone` | entity | область и допустимые работы | `Colony` |
| `ColonyObjective` | entity | цель и progress | `Colony` |
| `MoraleSnapshot` | value object | aggregate-level summary | `Colony` |
| `ColonyWealth` | value object | оценка стоимости | `Colony` |

`Npc` не является вложенной entity. Состояние NPC принадлежит `NPCContext`, а Colony хранит `NpcId` и membership metadata.

### 3.3 Другие aggregate roots

В первой версии достаточно одного `Colony` aggregate root. При масштабировании допустимы отдельные roots:

- `ColonyInvitation` — если приглашения имеют самостоятельный lifecycle;
- `ConstructionProject` — если blueprint/build state требует независимой транзакционной границы;
- `ColonyTradeLedger` — если торговые операции становятся крупными.

Нельзя создавать отдельный aggregate только ради удобства DTO: граница определяется инвариантами и частотой совместного изменения.

## 4. Инварианты Colony

### 4.1 Идентичность и lifecycle

- `ColonyId` уникален в пределах `WorldId`.
- Активная колония имеет непустое имя, валидный site и хотя бы один статус.
- После `DESTROYED` нельзя добавлять жителей, ресурсы или задания.
- Переименование не меняет `ColonyId`.
- Повторная команда с тем же `CommandId` даёт тот же результат и не дублирует событие.

### 4.2 Членство

- Один `NpcId` не может быть добавлен дважды в одну колонию.
- Нельзя назначить работу NPC, который не является member.
- Удаление жителя должно быть идемпотентным.
- `NpcId` может принадлежать только одной активной colony membership, если multiplayer policy не разрешает иное.
- `NpcDied` удаляет membership через event handler, но не удаляет исторические записи ledger.

### 4.3 Ресурсы

- Количество ресурса не может стать отрицательным.
- Reservation не может превышать available amount.
- Commit выполняется ровно один раз.
- Release освобождает только свою reservation.
- Resource type должен существовать в `resources.json`/registry.
- Любое изменение inventory имеет `ResourceChangeReason`.

### 4.4 Work policy и zones

- Work type должен существовать в конфигурации.
- Priority находится в `0..100`.
- Зона должна принадлежать текущему `WorldId`.
- Нельзя назначить тип работы, запрещённый зоной или статусом колонии.
- Изменение policy публикует событие, которое NPCContext может обработать eventual-consistently.

## 5. Порты Colony

### 5.1 Входящие порты

```java
public interface CreateColonyUseCase {
    CreateColonyResult execute(CreateColonyCommand command);
}

public interface AssignWorkUseCase {
    AssignWorkResult execute(AssignWorkCommand command);
}

public interface ManageColonyZoneUseCase {
    ZoneResult addZone(AddZoneCommand command);
    ZoneResult removeZone(RemoveZoneCommand command);
}

public interface ApplyProductionResultUseCase {
    ProductionResult apply(ProductionResultCommand command);
}

public interface GetColonyView {
    ColonyView get(ColonyId colonyId, PlayerId viewerId);
}
```

### 5.2 Исходящие порты

```java
public interface ColonyRepository {
    Optional<Colony> findById(WorldId worldId, ColonyId colonyId);
    Optional<Colony> findByName(WorldId worldId, ColonyName name);
    void save(Colony colony);
    void delete(WorldId worldId, ColonyId colonyId);
}

public interface WorldSettlementPort {
    SettlementValidation validate(SettlementSite site);
}

public interface ColonyEventPublisher {
    void publishAll(List<DomainEvent> events);
}

public interface ColonyConfigPort {
    ColonySettingsSnapshot current();
    WorkTypeDefinition workType(WorkTypeId id);
}

public interface ColonyMembershipQueryPort {
    MembershipSummary membership(WorldId worldId, ColonyId colonyId);
}
```

Порт `ColonyRepository` определяется Core, а `NbtColonyRepositoryAdapter` реализуется infrastructure. `WorldSettlementPort` возвращает `SettlementValidation`, а не `Level`, `BlockPos` или `Biome`.

## 6. Ключевые use cases

### 6.1 Создание колонии

**Команда:** `CreateColonyCommand(playerId, worldId, commandId, name, site)`.

```text
Player -> ForgeCommandAdapter: /rwc colony found
ForgeCommandAdapter -> CreateColonyUseCase: execute(command)
CreateColonyService -> ColonyRepository: проверка duplicate name
CreateColonyService -> WorldSettlementPort: validate(site)
WorldSettlementPort --> CreateColonyService: valid
CreateColonyService -> Colony: create starter state
CreateColonyService -> ColonyRepository: save
CreateColonyService -> ColonyEventPublisher: publish ColonyFounded
CreateColonyService --> Adapter: success(ColonyId)
Adapter --> Player: сообщение и HUD update
```

**Ошибки:** `NAME_ALREADY_EXISTS`, `INVALID_SITE`, `PLAYER_NOT_AUTHORIZED`, `WORLD_UNAVAILABLE`.

### 6.2 Добавление жителя

```text
NPCContext -> EventBus: NpcCreated/NpcReady
EventBus -> ColonyEventHandler: handle
ColonyEventHandler -> ColonyRepository: load colony
ColonyEventHandler -> Colony: addColonist(NpcId)
ColonyEventHandler -> ColonyRepository: save
ColonyEventHandler -> EventBus: ColonistJoined
```

Если колония уничтожена или отсутствует, обработчик не создаёт membership молча: событие получает retry/dead-letter policy.

### 6.3 Назначение работы

```text
Player -> Client: выбирает work type и NPC
Client -> Server: AssignWorkCommand
DrivingAdapter -> PlayerAuthorizationUseCase: authorize
DrivingAdapter -> AssignWorkUseCase: execute
AssignWorkService -> ColonyRepository: load
AssignWorkService -> Colony: requireMember + assignWork
AssignWorkService -> ColonyConfigPort: validate WorkTypeId
AssignWorkService -> ColonyRepository: save
AssignWorkService -> EventPublisher: WorkAssigned
NPCContext <- EventBus: принимает назначение
```

`AssignWorkService` не вызывает `Npc.assignJob()`. NPCContext сам решает, может ли NPC принять назначение.

### 6.4 Производство и потребление

```text
NPCContext -> EventBus: JobCompleted(outputIntent)
EventHandler -> ColonyRepository: load Colony
EventHandler -> Colony: validate reservation
Colony -> InventoryLedger: commit/produce
Colony -> EventBus: ResourceConsumed/ResourceProduced
EventHandler -> ColonyRepository: save
StorytellerContext <- EventBus: ColonyValueChangedEvent
```

Все результаты должны содержать `operationId`, чтобы повторная доставка не удвоила ресурсы.

## 7. События

### 7.1 Каталог событий

| Событие | Момент публикации | Основные подписчики |
|---|---|---|
| `ColonyFounded` | после сохранения новой колонии | Player, NPC bootstrap, projections |
| `ColonyRenamed` | имя успешно изменено | Player/UI, audit |
| `ColonistJoined` | membership добавлено | NPC, Player/UI |
| `ColonistLeft` | membership удалено | NPC, Player/UI |
| `WorkAssigned` | policy приняла assignment | NPC |
| `WorkPolicyChanged` | изменена политика работ | NPC, UI |
| `ResourceReserved` | reservation создана | job/projection |
| `ResourceConsumed` | commit потребления | Storyteller, UI |
| `ResourceProduced` | добавлена продукция | Storyteller, UI |
| `ColonyValueChangedEvent` | изменилась wealth/morale/resource value | Storyteller |
| `ColonyThreatMarked` | incident отмечен колонией | Player, UI |
| `ColonyObjectiveCompleted` | достигнута цель | Player, Storyteller |
| `ColonyDestroyed` | lifecycle завершён | Player, NPC, World projection |

### 7.2 Envelope

```java
public record ColonyValueChangedEvent(
        UUID eventId,
        WorldId worldId,
        ColonyId colonyId,
        String valueKind,
        String subjectId,
        BigDecimal delta,
        GameTick occurredAt,
        String correlationId,
        int schemaVersion
) implements DomainEvent {}
```

События immutable. `subjectId` — например `minecraft:iron_ingot` или `wealth`; это не ссылка на чужой aggregate.

### 7.3 Публикация и транзакция

События публикуются после успешного изменения aggregate и persistence. Если инфраструктура требует outbox, `ColonyRepository` сохраняет pending events вместе с snapshot; dispatcher публикует их повторно безопасно.

Нельзя публиковать `ColonyFounded`, а затем откатывать сохранение без compensation policy.

## 8. Конфигурация Colony

### 8.1 Файлы

Основной файл:

```text
config/rwc/colony/colony-settings.json
```

Дополнительные cross-context files:

```text
config/rwc/resources/resources.json
config/rwc/colony/work-types.json
config/rwc/colony/objectives.json
config/rwc/prefabs/prefabs.json
```

### 8.2 Реалистичный пример

```json
{
  "$schema": "rwc://schemas/colony/colony-settings.schema.json",
  "version": 1,
  "modVersion": "0.4.0",
  "startingResources": [
    { "resourceId": "minecraft:bread", "count": 32 },
    { "resourceId": "minecraft:oak_log", "count": 48 },
    { "resourceId": "minecraft:stone_pickaxe", "count": 2 }
  ],
  "startingZones": [
    {
      "id": "home",
      "shape": "CIRCLE",
      "radius": 12,
      "priority": 90,
      "allowedWorkTypes": ["build", "craft", "store"]
    },
    {
      "id": "food_garden",
      "shape": "RECTANGLE",
      "width": 16,
      "height": 12,
      "priority": 70,
      "allowedWorkTypes": ["grow", "harvest"]
    }
  ],
  "blockValues": [
    { "blockId": "minecraft:stone", "wealthValue": 1.0, "buildValue": 0.5 },
    { "blockId": "minecraft:iron_ore", "wealthValue": 12.0, "buildValue": 2.0 },
    { "blockId": "minecraft:diamond_block", "wealthValue": 500.0, "buildValue": 50.0 }
  ],
  "difficultyModifiers": {
    "wealthFactor": 0.0002,
    "populationFactor": 35.0,
    "moraleFactor": 0.5
  },
  "foundingRules": {
    "minDistanceFromSpawn": 64,
    "maxMembers": 32,
    "allowedClimates": ["temperate", "arid", "taiga"]
  }
}
```

### 8.3 Правила конфигурации

- `resourceId` должен существовать в `resources.json` или Minecraft registry.
- `startingZones[].id` уникален в файле.
- `CIRCLE` требует `radius`; `RECTANGLE` требует `width` и `height`.
- `priority` находится в `0..100`.
- `wealthValue` и `buildValue` неотрицательны.
- `allowedWorkTypes` ссылается на `work-types.json`.
- `allowedClimates` ссылается на `world-biome-modifiers.json`/climate catalog.
- Невалидный файл не заменяет активный snapshot.

### 8.4 Парсинг

```java
public final class ColonyConfigurationRepository {
    private final IConfigPort configPort;

    public ColonySettingsSnapshot current() {
        return configPort.get(
                new ConfigKey("colony", "colony-settings"),
                ColonySettingsSnapshot.class
        ).value();
    }
}
```

Parser и schema validator находятся в `infrastructure.config`; `ColonySettingsSnapshot` не должен быть mutable raw JSON tree.

## 9. Реализация сервисов и DI

### 9.1 `CreateColonyService`

```java
public final class CreateColonyService implements CreateColonyUseCase {
    private final ColonyRepository colonies;
    private final WorldSettlementPort world;
    private final ColonyConfigPort config;
    private final ColonyEventPublisher events;
    private final ClockPort clock;

    public CreateColonyService(
            ColonyRepository colonies,
            WorldSettlementPort world,
            ColonyConfigPort config,
            ColonyEventPublisher events,
            ClockPort clock) {
        this.colonies = colonies;
        this.world = world;
        this.config = config;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public CreateColonyResult execute(CreateColonyCommand command) {
        if (colonies.findByName(command.worldId(), command.name()).isPresent()) {
            return CreateColonyResult.failure("NAME_ALREADY_EXISTS");
        }
        SettlementValidation validation = world.validate(command.site());
        if (!validation.valid()) {
            return CreateColonyResult.failure(validation.errorCode());
        }
        Colony colony = Colony.found(
                ColonyId.newId(), command.worldId(), command.name(),
                command.site(), config.current(), clock.currentTick(command.worldId()));
        colonies.save(colony);
        events.publishAll(colony.pullDomainEvents());
        return CreateColonyResult.success(colony.id());
    }
}
```

### 9.2 Composition root

```java
ColonyRepository repository = new NbtColonyRepositoryAdapter(savedData);
WorldSettlementPort world = new MinecraftSettlementAdapter(serverLevel);
ColonyConfigPort config = new JsonColonyConfigAdapter(resourceManager);
ColonyEventPublisher events = new EventBusColonyPublisher(coreEventBus);
ClockPort clock = new MinecraftTickClockAdapter(server);

CreateColonyUseCase createColony = new CreateColonyService(
        repository, world, config, events, clock);
```

Композиционный root находится в infrastructure и является единственным местом выбора Forge/Fabric adapter.

## 10. Сохранение и загрузка

### 10.1 Repository contract

Repository сохраняет aggregate snapshot, а не Minecraft entity. Рекомендуемый ключ:

```text
rwc:colony/<worldId>/<colonyId>
```

Пример platform-neutral snapshot:

```json
{
  "schemaVersion": 2,
  "aggregateType": "Colony",
  "worldId": "frontier",
  "colonyId": "7f8c3c2e-...",
  "name": "New Dawn",
  "status": "ACTIVE",
  "members": [
    { "npcId": "2d1...", "role": "COLONIST", "joinedAtTick": 1200 }
  ],
  "inventory": {
    "minecraft:bread": 28,
    "minecraft:oak_log": 40
  },
  "workPolicy": {
    "2d1...": { "grow": 80, "build": 50 }
  },
  "zones": [],
  "wealth": 146.5
}
```

NBT mapping, compression, `SavedData` lifecycle и migration находятся в adapter. Corrupt snapshot не должен приводить к частично созданной колонии.

### 10.2 Loading policy

- missing snapshot → `Optional.empty()`;
- supported old schema → migrate before hydrate;
- unknown future schema → safe failure и diagnostic;
- duplicate member IDs → corruption error;
- negative inventory → corruption error;
- invalid reference → migration/error policy, не silent fix.

## 11. Тестирование

### 11.1 Unit pyramid

Цель — не менее 85% line/branch coverage для aggregate/domain services, но coverage не заменяет проверку поведения. Приоритет — invariants и failure paths.

Тесты:

- `ColonyTest` — lifecycle, membership, inventory, zones, work policy;
- `InventoryLedgerTest` — reservation/commit/release;
- `WorkPolicyTest` — priority and work type rules;
- `CreateColonyServiceTest` — mocks/fakes ports;
- `AssignWorkServiceTest`;
- `ColonyEventHandlerTest` — idempotency.

### 11.2 Unit test создания колонии

```java
@ExtendWith(MockitoExtension.class)
class CreateColonyServiceTest {
    @Mock ColonyRepository colonies;
    @Mock WorldSettlementPort world;
    @Mock ColonyConfigPort config;
    @Mock ColonyEventPublisher events;
    @Mock ClockPort clock;

    @Test
    void createsColonyOnValidSite() {
        WorldId worldId = new WorldId("frontier");
        ColonyName name = ColonyName.of("New Dawn");
        SettlementSite site = SettlementSiteFixtures.valid(worldId);
        when(colonies.findByName(worldId, name)).thenReturn(Optional.empty());
        when(world.validate(site)).thenReturn(SettlementValidation.valid());
        when(config.current()).thenReturn(ColonyConfigFixtures.standard());
        when(clock.currentTick(worldId)).thenReturn(new GameTick(100));

        CreateColonyUseCase service = new CreateColonyService(
                colonies, world, config, events, clock);
        CreateColonyResult result = service.execute(
                new CreateColonyCommand(
                        new CommandId(UUID.randomUUID()),
                        new PlayerId(UUID.randomUUID()), worldId, name, site));

        assertThat(result.success()).isTrue();
        verify(colonies).save(any(Colony.class));
        verify(events).publishAll(argThat(events ->
                events.stream().anyMatch(e -> e instanceof ColonyFounded)));
    }
}
```

### 11.3 Aggregate tests

Минимальные cases:

```text
founding with invalid name -> ValidationException
founding after destroyed -> ColonyConflictException
adding same NpcId twice -> NPC_ALREADY_MEMBER
assigning work to non-member -> NPC_NOT_MEMBER
reserve more than available -> INSUFFICIENT_RESOURCES
commit same operation twice -> idempotent no-op
remove missing member twice -> idempotent no-op
zone with foreign WorldId -> INVALID_WORLD
```

### 11.4 Интеграционные тесты persistence

Тестовый набор `NbtColonyRepositoryIntegrationTest` обязан проверять:

1. `Colony` сохраняется в NBT.
2. Загрузка возвращает эквивалентный aggregate.
3. Empty/missing save корректно обрабатывается.
4. `schemaVersion` migration выполняется до hydrate.
5. Повреждённые данные диагностируются и не публикуются как активная колония.
6. Сохранение не смешивает два `WorldId`.
7. Round-trip сохраняет `NpcId`, reservations, zones и pending metadata.

### 11.5 JSON-тесты

- все examples из `data-dictionaries.md` парсятся;
- schema validation отвергает duplicate IDs, invalid ranges и unknown references;
- reload сохраняет старый snapshot при ошибке;
- новый optional field получает documented default.

### 11.6 Межконтекстные contract tests

- `ColonistJoined` принимается NPC handler с правильным `NpcId`;
- `JobCompleted` обновляет inventory ровно один раз;
- `ColonyValueChangedEvent` принимается Storyteller;
- `ColonyDestroyed` очищает projections Player/NPC;
- event envelope остаётся совместимым между Forge и Fabric adapters.

## 12. Обработка ошибок

### 12.1 Core errors

```java
public enum ColonyErrorCode {
    INVALID_NAME,
    NAME_ALREADY_EXISTS,
    INVALID_SITE,
    COLONY_NOT_FOUND,
    COLONY_NOT_ACTIVE,
    NPC_NOT_MEMBER,
    NPC_ALREADY_MEMBER,
    INSUFFICIENT_RESOURCES,
    INVALID_WORK_TYPE,
    INVALID_ZONE,
    CONCURRENT_UPDATE,
    PERSISTENCE_FAILURE
}
```

Core возвращает `Result`/`CreateColonyResult` или собственные `CoreException` согласно единой стратегии проекта. Minecraft exception переводится adapter в `PortAccessException`; он не выходит в domain layer.

### 12.2 Retry и idempotency

- команда идентифицируется `CommandId`;
- изменение ресурса — `operationId`;
- событие — `eventId`;
- обработчик хранит processed markers;
- retryable save/event errors не повторяют domain mutation без проверки marker.

## 13. Definition of Done модуля Colony

### 13.1 Обязательный checklist

- [ ] Все агрегаты покрыты юнит-тестами с целевым покрытием **не менее 85%**; критические ветви инвариантов проверены отдельно.
- [ ] Все доменные сервисы покрыты юнит-тестами, включая успешные, ошибочные и повторные операции.
- [ ] Реализованы все доменные события модуля и их публикация после успешной фиксации состояния.
- [ ] Написаны интеграционные тесты для сохранения/загрузки `Colony`, включая migration и corruption paths.
- [ ] JSON-конфиги загружаются и парсятся корректно; semantic cross-references валидируются.
- [ ] ArchUnit-правила для модуля не нарушены (см. раздел 14).
- [ ] `data-dictionaries.md` обновлён и описывает все используемые поля `colony-settings.json`, включая defaults и references.
- [ ] Все публичные use cases, ошибки, события и ownership boundaries документированы.
- [ ] Проверена server authority, command idempotency и отсутствие Minecraft types в Core.
- [ ] Code review подтверждает отсутствие прямого доступа к внутренностям NPC/Storyteller/World контекстов.

### 13.2 Артефакты перед merge

```text
core.colony domain + application code
unit test report >= 85% aggregate coverage
NbtColonyRepository integration suite
JSON schema + valid/invalid fixtures
ArchitectureTest passing
updated data-dictionaries.md
updated event contract documentation
```

## 14. ArchUnit-правила для модуля Colony

Ниже приведены правила в стиле ArchUnit. Package names предполагают namespace `com.rimworldcraft`; при иной структуре меняются только константы пакетов.

### 14.1 Запрет Minecraft API

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.colony..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "net.minecraft..",
        "net.minecraftforge..",
        "net.fabricmc..",
        "com.mojang.blaze3d.."
    );
```

Это правило распространяется на domain, application, ports, events и contracts Colony.

### 14.2 Разрешённые зависимости Colony

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.colony..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.infrastructure..",
        "com.rimworldcraft.client.."
    );
```

Colony может зависеть от `core.shared`, собственных пакетов, публичных `core.contracts` и `core.ports`, но не от infrastructure.

Для более строгого контроля forbidden sibling internals:

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.colony.domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core.npc.domain..",
        "com.rimworldcraft.core.storyteller.domain..",
        "com.rimworldcraft.core.world.domain..",
        "com.rimworldcraft.core.player.domain.."
    );
```

### 14.3 Driving adapters не используют driven ports напрямую

```java
noClasses()
    .that().resideInAnyPackage(
        "com.rimworldcraft.infrastructure.adapters.driving.colony..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core.ports.driven..",
        "com.rimworldcraft.infrastructure.adapters.driven.."
    );
```

Driving adapter вызывает `core.ports.driving` и выполняет mapping/transport concerns. Композиция driven dependencies остаётся в composition root.

### 14.4 Aggregate public methods должны иметь тесты

ArchUnit не может надёжно доказать semantic coverage каждого метода. Поэтому применяется комбинация правила naming/visibility и JaCoCo/coverage gate:

```java
classes()
    .that().haveSimpleName("Colony")
    .should().resideInAnyPackage("com.rimworldcraft.core.colony.domain..");
```

А обязательный CI-gate:

```text
JaCoCo: com.rimworldcraft.core.colony.domain.Colony >= 85% line and branch coverage
Mutation testing: critical invariant methods meet configured mutation threshold
```

Публичные методы aggregate перечисляются в `ColonyApiTest` и вызываются хотя бы одним behavior test. Это практичнее, чем пытаться реализовать brittle reflection rule в ArchUnit.

### 14.5 Запрет `System.out`

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft..")
    .should().accessClassesThat()
    .belongToAnyOf(System.class);
```

Если правило слишком широкое для test code, ограничить production package:

```java
noClasses()
    .that().resideOutsideOfPackages("..test..", "..tests..")
    .should().callMethod(System.class, "out");
```

В production использовать проектный logger через infrastructure/observability port. `System.err` также запрещён.

### 14.6 Порты — интерфейсы

```java
classes()
    .that().resideInAnyPackage("com.rimworldcraft.core.colony.port..")
    .should().beInterfaces();
```

### 14.7 Driven adapters реализуют Colony ports

```java
classes()
    .that().resideInAnyPackage(
        "com.rimworldcraft.infrastructure.adapters.driven.colony..")
    .and().haveSimpleNameEndingWith("Adapter")
    .should().implement(
        JavaClass.Predicates.resideInAnyPackage(
            "com.rimworldcraft.core.colony.port.out.."));
```

Конкретный matcher может быть адаптирован под версию ArchUnit; смысл правила — adapter не является произвольным utility class.

## 15. План расширения

### 15.1 Новые типы зон

Чтобы добавить `STOCKPILE`, `DEFENSE` или новую геометрию:

1. добавить `ZoneShape`/`ZoneType` как domain abstraction;
2. вынести специфические правила в `ZonePolicy`, а не в большой `Colony` switch;
3. добавить schema enum и semantic validator;
4. сохранить старые shapes и defaults;
5. добавить tests на overlap, bounds и foreign world;
6. добавить projection/adapter support отдельно.

```java
public interface ZoneRule {
    ValidationResult validate(ZoneDefinition zone, ColonySnapshot colony);
}
```

Новый тип не должен требовать изменения NPC aggregate. NPC получает `WorkZoneView` или event.

### 15.2 Новые ресурсы

Добавление ресурса — data-first operation:

1. создать namespaced `resourceId` в `resources.json`;
2. описать stackability, category, weight и optional tags;
3. добавить ссылки в starter package/prefabs/jobs;
4. зарегистрировать Minecraft mapping в adapter, если ресурс materialized;
5. добавить round-trip и negative reference tests.

Inventory работает с `ResourceType`, поэтому добавление типа не требует изменения `InventoryLedger`.

### 15.3 Новые вычисления ценности

`ColonyWealth` не должен превращаться в набор условных операторов. Ввести стратегии:

```java
public interface ColonyValueRule {
    BigDecimal contribution(ResourceStack stack, ColonyValueContext context);
}
```

Композиция:

```java
public final class CompositeColonyValueCalculator {
    private final List<ColonyValueRule> rules;

    public BigDecimal calculate(ColonySnapshot snapshot) {
        return rules.stream()
                .map(rule -> rule.contribution(snapshot.inventory(), snapshot.context()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

Новые правила подключаются через configuration/policy registry, но не через reflection из JSON. Для каждого правила нужны unit tests и описание влияния на `ColonyValueChangedEvent`.

### 15.4 Новые способы persistence

`ColonyRepository` остаётся неизменным. Добавляются:

- `DatabaseColonyRepositoryAdapter`;
- `JsonColonyRepositoryAdapter` для dev tools;
- `ReplicatedColonyRepositoryAdapter` при multiplayer backend.

Все реализации проходят единый repository contract suite.

### 15.5 Новые игроки и multiplayer policy

Если потребуется совместное управление, расширяется `MembershipRole`/`PermissionSet` в PlayerContext. Colony продолжает владеть ресурсами и state; PlayerContext только проверяет authority. Прямое добавление player permissions в `Colony` допустимо лишь через явно согласованный integration contract.

## 16. Контроль производительности

Colony simulation не должна сканировать все block positions на каждом tick. Требования:

- использовать cached summaries;
- пересчитывать wealth incrementally после `ResourceProduced/Consumed`;
- ограничивать размер event payload;
- выполнять тяжёлые queries по budget/interval;
- не сохранять aggregate на каждый render frame;
- batching persistence допустим, если сохранение не нарушает crash consistency.

Метрики:

```text
colony_tick_duration_ms
colony_save_duration_ms
colony_event_publish_failures
colony_active_members
colony_inventory_value
colony_pending_reservations
```

## 17. Практический checklist разработчика

Перед добавлением функции ответьте:

1. Принадлежит ли состояние `Colony`, а не NPC/Player/Storyteller?
2. Меняется ли aggregate только через его метод/use case?
3. Нужен ли новый value object вместо строки/числа?
4. Есть ли событие для заинтересованных контекстов?
5. Не попал ли Minecraft type в Core?
6. Идемпотентна ли команда/event handler?
7. Добавлены ли JSON schema и cross-reference tests?
8. Что происходит при missing/corrupt save?
9. Проходит ли ArchUnit и coverage gate?
10. Нужен ли ADR для изменения ownership или публичного contract?

## 18. Связи с другими документами

- [`bounded-contexts.md`](bounded-contexts.md) — границы `ColonyContext`, ownership `Colony`, membership через `NpcId` и межконтекстные события.
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — driving/driven ports, adapters, DI и изоляция Core от Minecraft API.
- [`data-dictionaries.md`](data-dictionaries.md) — схема `colony-settings.json`, типы полей, defaults, references и migration policy.
- `module-npc-core.md` — добавление/удаление жителей, `WorkAssigned`, `JobCompleted`, `NpcDied` и job execution.
- `module-storyteller.md` — чтение colony summaries и использование `ColonyValueChangedEvent` для threat/pacing.
- `module-world.md` — validation settlement site, biome facts и world boundary.
- `module-player.md` — authorization команд, membership и UI queries.
- `persistence.md` — NBT snapshots, migrations и repository contract.
- `testing-strategy.md` — общая test pyramid, fixtures, contract tests и CI.
- `archunit-rules.md` — полный набор architectural rules проекта.

## 19. Итог

`Colony` — единственный владелец коллективного состояния колонии. NPC представлены идентификаторами и событиями, Minecraft представлен портами и адаптерами, конфигурация загружается в immutable snapshot, а каждое изменение проходит через инварианты aggregate.

Такой дизайн позволяет добавлять зоны, ресурсы, ценностные правила, persistence backends и новые UI без переписывания доменного ядра и без размывания ответственности между Colony, NPC, Storyteller, World и Player контекстами.
