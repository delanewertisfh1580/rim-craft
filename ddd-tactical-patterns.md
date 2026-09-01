# RimWorldCraft — DDD Tactical Patterns

## 1. Введение

Этот документ описывает практическое применение тактических паттернов Domain-Driven Design в RimWorldCraft: Entity, Value Object, Aggregate, Repository, Factory, Domain Service и Domain Event.

Он является связующим руководством для [`bounded-contexts.md`](bounded-contexts.md), [`hexagonal-architecture.md`](hexagonal-architecture.md), module-документации, [`data-interfaces.md`](data-interfaces.md) и [`codestyle-and-solidd.md`](codestyle-and-solidd.md). Его задача — помочь разработчику превратить предметное правило игры в модель, защищающую инварианты, а не в набор таблиц и procedural code.

RimWorldCraft использует DDD внутри Minecraft-мода, но Core остаётся независимым от Minecraft API. Minecraft world, NBT, entities, Forge/Fabric и Baritone подключаются через ports/adapters.

## 2. Общие принципы моделирования

### 2.1 Моделировать предметную область, а не структуру данных

Класс должен выражать игровой смысл:

```java
colony.reserve(resource, reservation);
citizen.changeMood(calculation);
buildOrder.complete(result);
```

а не предоставлять набор полей для произвольной мутации:

```java
colony.setResources(map);
citizen.setMood(40);
buildOrder.setStatus(COMPLETED);
```

### 2.2 Инварианты защищаются владельцем

- `Citizen` защищает range mood/skills/needs и lifecycle.
- `Colony` защищает membership и resources.
- `BuildOrder` защищает progress/status/reservations.
- `Storyteller` защищает cooldown/incident state.

Сервис может координировать агрегаты, но не должен обходить их методы.

### 2.3 Value Objects immutable

Значения передаются между потоками, сравниваются по содержимому и не требуют identity. Mutable state должен быть локализован в aggregate или application flow.

### 2.4 ID — часть модели

ID не является случайным техническим полем. Он определяет непрерывность сущности и ссылку между агрегатами:

```text
ColonyId, CitizenId, BuildOrderId, IncidentId, TaskId, WorldId
```

Между агрегатами передаются IDs/summaries, а не object references.

### 2.5 Границы согласованности определяются транзакциями

Если два объекта всегда должны изменяться атомарно для одного инварианта, они вероятно принадлежат одному aggregate. Если изменение может быть eventual consistent, используйте разные aggregates и domain events.

## 3. Entity

### 3.1 Определение

Entity имеет непрерывную identity, изменяется со временем и остаётся той же сущностью, даже если поля меняются. В RimWorldCraft примеры:

- `Citizen`;
- `Colony`;
- `BuildOrder`;
- `Incident`;
- `Storyteller`.

### 3.2 Entity vs Value Object

| Вопрос | Entity | Value Object |
|---|---|---|
| Есть стабильный ID? | да | нет |
| Изменяется со временем? | да | обычно создаётся новая версия |
| Equality | по ID | по всем значениям |
| Пример | `Citizen`, `Colony` | `Position`, `Mood`, `ResourceAmount` |
| Может быть aggregate root? | да | нет |

`ResourceType` — value object; конкретная пачка ресурса может быть частью inventory entity, если ей нужен lifecycle/lot ID.

### 3.3 `Citizen`

```java
public final class Citizen {
    private final CitizenId id;
    private CitizenName name;
    private Mood mood;
    private final Map<SkillType, Skill> skills;
    private final Map<NeedType, Need> needs;
    private CitizenStatus status;
    private ColonyId colonyId;
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public void changeMood(MoodCalculation calculation) {
        requireAlive();
        Mood old = mood;
        mood = mood.recalculate(calculation);
        if (!old.equals(mood)) {
            pendingEvents.add(NPCMoodChangedEvent.of(
                    id, old.value(), mood.value(), calculation.reason()));
        }
    }

    public SkillExperienceResult addSkillExperience(
            SkillType type, int amount, SkillExperienceReason reason) {
        requireAlive();
        Skill skill = requireSkill(type);
        SkillExperienceResult result = skill.addExperience(amount);
        if (result.levelIncreased()) {
            pendingEvents.add(NPCSkillIncreasedEvent.of(
                    id, type, result.oldLevel(), result.newLevel(), reason));
        }
        return result;
    }

    private void requireAlive() {
        if (status == CitizenStatus.DEAD) {
            throw new CitizenAlreadyDeadException(id);
        }
    }
}
```

Публичный код не может напрямую изменить `mood`, `skills` или `status`. Адаптеры не должны получать mutable collections.

### 3.4 Правила Entity

- identity immutable;
- constructors/factories создают valid state;
- переходы состояния выражены намерениями (`die`, `complete`, `assign`);
- entity не знает repository и infrastructure;
- equality по identity только там, где entity semantics это требует;
- если объект не имеет meaningful lifecycle, не делайте его Entity.

## 4. Value Objects

### 4.1 Определение

Value Object определяется значениями, immutable и взаимозаменяем. Он должен валидировать собственные ограничения и не раскрывать setters.

### 4.2 `ResourceAmount`

```java
public record ResourceAmount(ResourceType type, int amount) {
    public ResourceAmount {
        Objects.requireNonNull(type);
        if (amount < 0) throw new IllegalArgumentException("amount >= 0");
    }

    public ResourceAmount add(int delta) {
        if (delta < 0) throw new IllegalArgumentException("delta >= 0");
        return new ResourceAmount(type, Math.addExact(amount, delta));
    }

    public ResourceAmount subtract(int delta) {
        if (delta < 0 || delta > amount) {
            throw new InsufficientResourcesException(type, delta, amount);
        }
        return new ResourceAmount(type, amount - delta);
    }
}
```

### 4.3 `Position`

```java
public record Position(WorldId worldId, int x, int y, int z) {
    public Position {
        Objects.requireNonNull(worldId);
    }

    public Position offset(int dx, int dy, int dz) {
        return new Position(worldId, x + dx, y + dy, z + dz);
    }
}
```

Это не Minecraft `BlockPos`; mapping выполняется в adapter.

### 4.4 `Skill`, `Mood`, `Relationship`, `WorldState`

- `Skill` содержит type/level/experience и возвращает новый progression result либо является маленькой entity внутри Citizen — решение фиксируется context model.
- `Mood` содержит value/modifiers/lastUpdated и создаёт новый value при recalculation.
- `Relationship` может быть immutable snapshot value object, а history хранится в Citizen aggregate или отдельном relation aggregate при масштабировании.
- `WorldState` — immutable planner snapshot для Goal AI.

```java
public record Mood(int value, List<MoodModifier> modifiers, GameTick lastUpdated) {
    public Mood {
        if (value < 0 || value > 100) throw new IllegalArgumentException("mood 0..100");
        modifiers = List.copyOf(modifiers);
    }

    public Mood withValue(int newValue, GameTick tick, String reason) {
        return new Mood(newValue, List.of(new MoodModifier(reason, newValue - value)), tick);
    }
}
```

### 4.5 Правила Value Object

- `final class` или `record`;
- final fields;
- defensive copies для collections/maps;
- `equals/hashCode` по значениям;
- no setters;
- фабричный метод для сложной валидации;
- не добавлять ID только для удобства persistence.

**Антипаттерн:** mutable `Position` с `setX`, используемый несколькими потоками. **Правильно:** создать новый `Position`.

## 5. Aggregate

### 5.1 Определение

Aggregate — кластер entities/value objects с одной consistency boundary и Aggregate Root. Внешний код обращается только к root.

### 5.2 Агрегаты проекта

| Aggregate Root | Владеет | Ссылки наружу |
|---|---|---|
| `Colony` | membership, inventory, zones, work policy, objectives | `NpcId`, `BuildOrderId` |
| `Citizen` | needs, mood, skills, traits, relationships, task state | `ColonyId`, target IDs |
| `Storyteller` | incidents, arcs, cooldown, timeline | `ColonyId`, `IncidentId` |
| `BuildOrder` | blueprint reference, ghost blocks/progress/resources | `ColonyId`, `CitizenId`, `BlueprintId` |
| `CitizenAI` | current goal/plan/action queue | `CitizenId`, `TaskId` |

### 5.3 `Colony` boundary

Внутри: `InventoryLedger`, `WorkPolicy`, `WorkZone`, `ColonistMembership`, morale/wealth summaries. Вне: full `Citizen`, Minecraft blocks, renderer, Storyteller state.

```java
public final class Colony {
    private final ColonyId id;
    private final Map<NpcId, ColonistMembership> members = new HashMap<>();

    public void addColonist(NpcId npcId, MembershipRole role, GameTick tick) {
        requireActive();
        if (members.containsKey(npcId)) {
            throw new ColonyConflictException("NPC_ALREADY_MEMBER");
        }
        members.put(npcId, ColonistMembership.create(npcId, role, tick));
        pendingEvents.add(new CitizenJoinedEvent(id, npcId, role));
    }

    public Set<NpcId> memberIds() {
        return Set.copyOf(members.keySet());
    }
}
```

### 5.4 `BuildOrder` boundary

`BuildOrder` защищает status/progress/assignment/resource reservations. `Blueprint` может быть отдельным catalog object, потому что он обычно immutable и shared.

### 5.5 Aggregate rules

1. Root — единственная mutation entry point.
2. Repository загружает и сохраняет root целиком.
3. Aggregate не вызывает repository.
4. Другие aggregates связаны только IDs.
5. События публикуются после successful state transition.
6. Размер aggregate ограничен производительностью и транзакционной необходимостью.
7. Async handlers не мутируют aggregate без повторной загрузки/версии.

### 5.6 Слишком большой/маленький aggregate

**Слишком большой:** `Colony` включает все 200 NPC, все plans и весь block map — медленные save, lock contention, huge events.

**Слишком маленький:** resource reservation, order status и inventory изменяются разными независимыми объектами без transaction — появляются отрицательные ресурсы и ghost reservations.

## 6. Factory

### 6.1 Когда использовать

Factory нужна, если создание требует нескольких шагов, random/config, cross-field validation или выбора concrete policy. Простые records создаются constructor/factory method.

### 6.2 `CitizenFactory`

```java
public interface CitizenFactory {
    Citizen createRandomCitizen(WorldId worldId, GenerationSeed seed);
    Citizen createColonist(WorldId worldId, ColonyId colonyId,
                           GenerationSeed seed);
    Citizen createEnemy(WorldId worldId, FactionId faction,
                        GenerationSeed seed);
}
```

```java
public final class DefaultCitizenFactory implements CitizenFactory {
    private final NameGenerator names;
    private final TraitGenerator traits;
    private final SkillProfileGenerator skills;
    private final NpcConfigPort config;

    @Override
    public Citizen createColonist(WorldId worldId, ColonyId colonyId,
                                  GenerationSeed seed) {
        NpcArchetype archetype = config.archetype(NpcRole.COLONIST);
        return Citizen.create(
                CitizenId.newId(),
                names.generate(archetype, seed),
                traits.generate(archetype, seed),
                skills.generate(archetype, seed),
                config.initialNeeds(), colonyId,
                PositionFactory.initial(worldId, seed));
    }
}
```

Factory возвращает valid aggregate, но не сохраняет и не публикует события — lifecycle orchestration принадлежит application service.

### 6.3 Другие фабрики

- `IncidentFactory` создаёт raid/trade/disaster без spawn side effect;
- `BuildOrderFactory` рассчитывает required resources, но reservation выполняет `ResourceManager`;
- `BlueprintFactory` создаёт immutable blueprint из prefab/selection;
- `StoryArcFactory` создаёт arc из validated template.

## 7. Repository

### 7.1 Роль

Repository даёт иллюзию in-memory collection aggregate roots, скрывая NBT/JSON/database. Он возвращает полностью hydrated aggregate, а не lazy proxy.

```java
public interface CitizenRepository {
    Optional<Citizen> findById(WorldId worldId, CitizenId id);
    List<Citizen> findAllByColonyId(WorldId worldId, ColonyId colonyId);
    Citizen save(Citizen citizen);
    void delete(WorldId worldId, CitizenId id);
}
```

### 7.2 Правила

- repository interface в Core;
- implementation в Infrastructure;
- ID/value objects вместо raw storage IDs;
- `Optional` вместо null;
- aggregate invariants не обходятся direct update;
- optimistic version для concurrent save;
- queries не мутируют state;
- repository не знает UI и Minecraft entity.

### 7.3 Реализация

```java
public final class NbtCitizenRepository implements CitizenRepository {
    private final ISaveLoadPort storage;
    private final SnapshotMapper<Citizen> mapper;

    @Override
    public Optional<Citizen> findById(WorldId worldId, CitizenId id) {
        SaveKey key = new SaveKey(worldId, "citizen", id.value().toString());
        return storage.load(key).map(mapper::fromDocument);
    }

    @Override
    public Citizen save(Citizen citizen) {
        SaveKey key = SaveKeys.forCitizen(citizen);
        storage.save(key, mapper.toDocument(citizen));
        return citizen;
    }
}
```

### 7.4 Repository vs service

Repository отвечает за collection/persistence semantics. `NeedDecayService`, `TaskScheduler` и `IncidentSelectionService` содержат business policy и используют repositories, но не являются repositories.

## 8. Domain Service

### 8.1 Когда использовать

Domain Service подходит для stateless business operation, которая:

- не принадлежит естественно одному aggregate;
- использует несколько aggregates;
- зависит от внешнего domain port;
- выражает policy/algorithm.

Не помещайте в service простую операцию, которую должен защищать aggregate.

### 8.2 `ColonyValueCalculator`

```java
public final class ColonyValueCalculator {
    private final IBlockWorldPort blocks;
    private final ColonyValueRuleRegistry rules;

    public ColonyValue calculate(ColonySnapshot colony) {
        return rules.rules().stream()
                .map(rule -> rule.calculate(colony, blocks))
                .reduce(ColonyValue.zero(), ColonyValue::add);
    }
}
```

### 8.3 Примеры сервисов

| Service | Ответственность | Зависимости |
|---|---|---|
| `NeedDecayService` | обновить needs NPC batch-wise | `CitizenRepository`, `ITimePort`, environment ports |
| `IncidentSelectionService` | выбрать incident по weights/conditions | `Storyteller`, summaries, `RandomPort`, config |
| `TaskScheduler` | назначить tasks подходящим NPC | task/citizen summaries, priority policy |
| `RelationshipService` | вычислить relation delta | Citizen repository, time, config, events |
| `ColonyValueCalculator` | рассчитать wealth/value | block/resource ports, rules |

### 8.4 Stateless правило

```java
public final class NeedDecayService {
    private final CitizenRepository citizens;
    private final TimePort time;

    public NeedDecayService(CitizenRepository citizens, TimePort time) {
        this.citizens = citizens;
        this.time = time;
    }

    public void update(CitizenId id, NeedEnvironmentSnapshot environment) {
        Citizen citizen = citizens.findById(environment.worldId(), id)
                .orElseThrow(() -> new CitizenNotFoundException(id));
        citizen.updateNeeds(environment.delta());
        citizens.save(citizen);
    }
}
```

Service не хранит `Citizen` между вызовами и не содержит mutable global cache без явной expiration/concurrency policy.

## 9. Domain Events

### 9.1 Роль

Events сообщают о факте: `NPCDeathEvent`, `ColonyValueChangedEvent`, `IncidentStartedEvent`. Они снижают связанность контекстов, но не заменяют command и не делают distributed transaction.

Все events:

- immutable;
- имеют `eventId`, timestamp, source, version;
- содержат минимальный payload;
- публикуются после успешного aggregate transition;
- имеют idempotent handlers;
- versioned для эволюции.

### 9.2 Пример

```java
public final class NPCMoodChangedEvent extends DomainEvent {
    private final CitizenId citizenId;
    private final int oldMood;
    private final int newMood;
    private final String reason;

    public NPCMoodChangedEvent(EventMetadata metadata, CitizenId citizenId,
                               int oldMood, int newMood, String reason) {
        super(metadata);
        if (oldMood < 0 || oldMood > 100 || newMood < 0 || newMood > 100) {
            throw new IllegalArgumentException("mood must be 0..100");
        }
        this.citizenId = citizenId;
        this.oldMood = oldMood;
        this.newMood = newMood;
        this.reason = reason;
    }
}
```

### 9.3 Публикация

```text
aggregate method
  -> pending event
  -> repository save
  -> application publishes through IEventBusPort
  -> subscribers invoke own use cases
```

Не публиковать event до сохранения, если synchronous subscriber может увидеть state, которого ещё нет в persistence. Для надёжности использовать outbox.

## 10. Взаимодействие паттернов

### 10.1 Создание колонии

```text
Player command
  -> CreateColonyService
  -> ColonyFactory / Colony.found
  -> IColonyRepository.save
  -> ColonyCreatedEvent
  -> NPC handler -> CitizenFactory x3 -> ICitizenRepository.save
  -> Building handler -> initialize queue
  -> Storyteller handler -> create tracking state
```

Factory создаёт, aggregate валидирует, repository сохраняет, event bus уведомляет контексты.

### 10.2 Обновление потребностей NPC

```text
Server tick
  -> NeedDecayService
  -> CitizenRepository.findActive
  -> Citizen.updateNeeds
  -> NPCNeedCriticalEvent / NPCMoodChangedEvent
  -> repository.save
  -> EventBus
  -> Storyteller updates pressure
  -> Goal AI replans survival goal
```

### 10.3 Постройка

```text
Player selects blueprint
  -> BlueprintFactory
  -> BlueprintValidator
  -> BuildOrderFactory
  -> ResourceManager reserves Colony resources
  -> BuildOrderRepository.save
  -> BuildOrderCreatedEvent
  -> Goal AI creates BUILD task
  -> NPC executes actions
  -> BuildOrderCompletedEvent
  -> Colony value update + NPC XP + Storyteller fact
```

## 11. Антипаттерны и ошибки

### 11.1 Изменение aggregate извне

```java
colony.members().remove(npcId); // запрещено: обход инвариантов и events
```

Использовать `colony.removeColonist(npcId, reason)`.

### 11.2 Анемичная модель

Класс только с полями и public setters переносит правила в сервисы и допускает invalid states. Aggregate должен иметь behavior methods и защищать invariants.

### 11.3 Repository внутри aggregate

```java
class Citizen {
    private final CitizenRepository repository; // запрещено
}
```

Aggregate не знает persistence. Application service загружает root, вызывает behavior и сохраняет.

### 11.4 Глобальные object references

```java
citizen.setColony(colony); // создаёт coupling и огромный graph
```

Хранить `ColonyId`; для чтения использовать summary/query port.

### 11.5 Mutable Value Object

`Position` с setters, shared между AI/pathfinding/rendering, создаёт race conditions и скрытые изменения. Использовать immutable record.

### 11.6 Events до transaction

Если `ColonyCreatedEvent` опубликован до успешного save, NPC handler может создать жителей для несуществующей колонии. Использовать после commit/outbox.

### 11.7 Service как God Object

`GameManager` с colony, NPC, AI, NBT, rendering и events нарушает SRP. Разделить по context и capability.

## 12. Тестирование DDD-паттернов

### 12.1 Entity/Aggregate

Проверять behavior и invariants:

```java
@Test
void colonyRejectsDuplicateMember() {
    Colony colony = ColonyFixtures.active();
    NpcId npcId = NpcId.newId();
    colony.addColonist(npcId, MembershipRole.COLONIST, new GameTick(1));

    assertThatThrownBy(() -> colony.addColonist(
            npcId, MembershipRole.COLONIST, new GameTick(2)))
            .isInstanceOf(ColonyConflictException.class);
}
```

### 12.2 Value Object

```java
@Test
void resourceSubtractReturnsNewValueAndKeepsOriginal() {
    ResourceAmount original = ResourceAmount.of("minecraft:stone", 10);
    ResourceAmount result = original.subtract(3);

    assertThat(original.amount()).isEqualTo(10);
    assertThat(result.amount()).isEqualTo(7);
    assertThat(result).isNotEqualTo(original);
}
```

Проверять equals/hashCode, boundaries, immutability и overflow.

### 12.3 Factory

- valid profile;
- configured trait conflicts rejected;
- deterministic seed behavior;
- missing template/config error;
- factory не делает persistence side effect.

### 12.4 Repository

Использовать abstract contract tests для NBT, in-memory и будущей database реализации:

```java
abstract class CitizenRepositoryContractTest {
    protected abstract CitizenRepository repository();

    @Test
    void saveThenFindReturnsHydratedAggregate() { /* ... */ }

    @Test
    void missingIdReturnsEmpty() { /* ... */ }

    @Test
    void deleteIsIdempotent() { /* ... */ }
}
```

### 12.5 Domain Service

Mockito/fakes заменяют repository, ports, clock и event bus. Проверяются результат и collaboration, но не implementation details без необходимости.

### 12.6 Domain Events

Проверять:

- event fields/version/source;
- aggregate publishes only on real state change;
- handler idempotency;
- subscriber failure isolation;
- event chain integration.

## 13. DoD для DDD-паттернов

- [ ] Класс соответствует одному явно выбранному tactical pattern.
- [ ] Ответственность и bounded context задокументированы.
- [ ] Инварианты защищены aggregate/value object methods.
- [ ] Aggregates не имеют public setters для domain state.
- [ ] Value Objects immutable и имеют корректные equals/hashCode.
- [ ] Repositories — Core interfaces без concrete storage/framework dependencies.
- [ ] Factories возвращают полностью валидные объекты и не имеют скрытых side effects.
- [ ] Domain services stateless либо их state policy явно документирована.
- [ ] Events immutable, versioned и публикуются через event bus/outbox.
- [ ] Межaggregate связи используют IDs/summaries.
- [ ] Есть unit/contract/integration tests для выбранного pattern.
- [ ] Javadoc описывает public contract, ошибки и consistency guarantees.

## 14. Связи с другими документами

- [`bounded-contexts.md`](bounded-contexts.md) — контексты и ownership aggregates.
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — ports/adapters и Core isolation.
- [`data-interfaces.md`](data-interfaces.md) — repositories, factories, specifications и contracts.
- [`module-colony.md`](module-colony.md) — Colony aggregate, resources и membership.
- [`module-npc-core.md`](module-npc-core.md) — Citizen, needs, mood, skills и relationships.
- [`module-storyteller.md`](module-storyteller.md) — incidents, arcs и storyteller events.
- [`module-goal-ai.md`](module-goal-ai.md) — CitizenAI, plans и tasks.
- [`module-building-system.md`](module-building-system.md) — BuildOrder, Blueprint и construction events.
- [`event-system-api.md`](event-system-api.md) — DomainEvent, event bus, outbox и idempotency.
- [`codestyle-and-solidd.md`](codestyle-and-solidd.md) — Java/SOLID/style rules.
- `testing-strategy.md` — общая стратегия тестирования.

## 15. Расширение и эволюция

### 15.1 Новое поле aggregate

Добавить поле через constructor/factory, установить migration default, обновить snapshot mapper и tests. Не добавлять public setter только ради persistence.

### 15.2 Новый event

Определить owner, минимальный payload, schema version, subscribers, idempotency и integration tests. Если меняется смысл существующего события, создать новую версию/type.

### 15.3 Новый bounded context

Сначала определить owner данных, aggregate boundary и contracts. Не расширять Shared Kernel domain behavior без ADR.

### 15.4 Расщепление aggregate

Если aggregate стал большим или часто вызывает contention:

1. описать invariant, который должен остаться атомарным;
2. выделить новый root;
3. заменить object reference на ID;
4. добавить event choreography;
5. обеспечить consistency/retry;
6. мигрировать save data;
7. сохранить repository contract tests.

### 15.5 Backward compatibility

- optional поля с defaults;
- versioned snapshots/events;
- migration adapters вне domain;
- additive API evolution;
- compatibility window перед удалением старого contract.

## 16. Итог

DDD-паттерны RimWorldCraft образуют последовательную модель:

```text
Value Objects защищают значения
        |
Entities сохраняют identity и lifecycle
        |
Aggregates защищают invariants
        |
Factories создают valid state
        |
Repositories управляют persistence
        |
Domain Services координируют policies
        |
Domain Events связывают контексты
```

Разработчик должен сначала определить смысл и владельца состояния, затем выбрать tactical pattern. Хорошая модель позволяет тестировать правила без Minecraft, сохранять агрегаты через заменяемые adapters, расширять систему без циклических зависимостей и делать межконтекстные изменения явными и наблюдаемыми.
