# RimWorldCraft — Code Style, SOLID и Design Practices

## 1. Введение

Этот документ — обязательное руководство для разработчиков RimWorldCraft на Java 17+. Он объединяет правила оформления, применения SOLID, DDD и гексагональной архитектуры, а также требования к тестам, логированию и автоматическим проверкам.

Он дополняет:

- [`system-overview.md`](system-overview.md) — общую архитектуру;
- [`bounded-contexts.md`](bounded-contexts.md) — границы контекстов;
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — ports and adapters;
- [`module-npc-core.md`](module-npc-core.md), [`module-goal-ai.md`](module-goal-ai.md), [`module-building-system.md`](module-building-system.md) — доменные модули;
- [`event-system-api.md`](event-system-api.md) — события и event bus;
- [`data-interfaces.md`](data-interfaces.md) — repositories и factories.

Документ предназначен для Core, Infrastructure, Client, Configurator, тестов и code review. Если правило невозможно применить, исключение объясняется в PR или ADR, а не скрывается отключением проверки.

## 2. Философия кода

### 2.1 Базовые ценности

- Читаемость важнее краткости.
- Явное имя лучше комментария, объясняющего плохое имя.
- Один метод решает одну понятную задачу.
- Код должен быть тестируемым без Minecraft там, где это возможно.
- Слабая связанность между модулями, высокая связность внутри bounded context.
- Инварианты находятся рядом с моделью, которой они принадлежат.
- Неизменяемость предпочтительнее разделяемого mutable state.
- Ошибка должна быть видимой: нельзя скрывать exception или fallback.
- `Core` не знает Minecraft API.
- Бойся глобального состояния: static допустима для constants/stateless metadata, но не для mutable runtime services.

### 2.2 Правило трёх вопросов

Перед добавлением класса ответьте:

1. Какую одну ответственность он имеет?
2. Как его протестировать без Minecraft/server?
3. Какая зависимость должна быть портом, а не concrete class?

## 3. Именование

### 3.1 Пакеты

Пакеты всегда lowercase и отражают архитектурную границу:

```text
com.rimworldcraft.core.npc
com.rimworldcraft.core.npc.domain
com.rimworldcraft.core.ports.driven
com.rimworldcraft.infrastructure.adapters.driven
com.rimworldcraft.client.render
```

Не использовать аббревиатуры без общепринятого смысла и не помещать `net.minecraft` types в Core packages.

### 3.2 Классы и records

Классы — существительные в PascalCase:

```java
Citizen
ColonyManager
BuildOrderFactory
BaritonePathfinderAdapter
```

Суффиксы отражают роль:

| Суффикс | Роль |
|---|---|
| `Service` | application/domain service |
| `Repository` | data access contract/implementation |
| `Factory` | complex creation |
| `Adapter` | external integration |
| `Mapper` | model conversion |
| `Validator` | structural/semantic validation |
| `Policy`/`Strategy` | replaceable business algorithm |
| `Handler` | event/command handling |
| `Snapshot`/`View`/`Summary` | read-only projection |

### 3.3 Интерфейсы

Единое правило проекта:

- domain/application interfaces получают предметное имя без `I`: `CitizenRepository`, `CreateColonyUseCase`, `PriorityStrategy`;
- технические hexagonal ports сохраняют суффикс `Port`: `IPathfinderPort`, `IEventBusPort`, `ISaveLoadPort`;
- старые/внешне заданные `I...` contracts не переименовываются без migration.

Не создавать интерфейс только ради mock: интерфейс нужен для boundary, вариативности или устойчивого контракта.

### 3.4 Методы и поля

- методы — глаголы: `findById`, `calculateValue`, `assignTask`;
- boolean — `is`, `has`, `can`, `should`: `isAlive`, `canBuild`;
- поля — `private final`, где возможно: `private final CitizenRepository citizens`;
- constants — `UPPER_SNAKE_CASE`;
- generic type variables — `T`, `ID`, `E` только при ясном контексте;
- не использовать `data`, `thing`, `manager2`, `tmp` как production names.

### 3.5 Тесты

Предпочтительный формат:

```text
[method]_Should[behavior]_When[condition]
```

Примеры:

```java
save_ShouldPersistEntity_WhenValid()
findById_ShouldReturnEmpty_WhenEntityDoesNotExist()
assignTask_ShouldReject_WhenCitizenIsNotMember()
```

Если проект использует современные descriptive test names, допускается sentence style при единообразии.

## 4. Структура классов и файлов

### 4.1 Файл

Один public class/interface/record на файл. Исключение — небольшие тесно связанные private nested types, не являющиеся самостоятельной доменной концепцией.

### 4.2 Порядок элементов

```text
package/imports
class declaration
static constants
static fields
instance fields
constructors
public methods
protected/package methods
private methods
nested classes
```

Для records fields/accessors задаются заголовком record; не добавлять бессмысленные setters.

### 4.3 Visibility

- минимальная необходимая visibility;
- domain invariants не обходятся public setters;
- коллекции наружу — immutable views/copies;
- package-private допустим для внутренних collaborators;
- `public` API документируется и тестируется.

### 4.4 Getters/setters

Value object должен быть immutable:

```java
public record ColonyId(UUID value) {
    public ColonyId {
        Objects.requireNonNull(value);
    }
}
```

Не использовать:

```java
public void setMood(int mood) { this.mood = mood; }
```

Вместо этого aggregate предоставляет намерение:

```java
public void changeMood(MoodCalculation calculation) { ... }
```

## 5. SOLID в RimWorldCraft

### 5.1 S — Single Responsibility

Класс имеет одну причину для изменения.

**Правильно:**

```java
final class Citizen {
    void changeMood(MoodCalculation calculation) { ... }
}

final class CitizenAI {
    void tick(WorldState state) { ... }
}

final class CitizenRenderer {
    void render(EntityCitizen entity) { ... }
}
```

`Citizen` меняется при изменении NPC domain rules, `CitizenAI` — при изменении decision policy, renderer — при изменении визуализации.

**Неправильно — God Object:**

```java
final class GodClass {
    void loadNbt() {}
    void calculateMood() {}
    void chooseGoal() {}
    void findPath() {}
    void spawnEntity() {}
    void renderHud() {}
    void publishEvents() {}
}
```

Такой класс знает persistence, domain, AI, Minecraft и UI одновременно. Декомпозировать на aggregate, services, ports, adapters и projections.

### 5.2 O — Open/Closed

Расширяем через strategy/registry/configuration, не переписывая стабильный код.

```java
public interface PriorityStrategy {
    boolean supports(GoalType type);
    int calculate(WorldState state);
}

public final class PriorityEvaluator {
    private final List<PriorityStrategy> strategies;
    // new strategy can be registered without changing evaluator algorithm
}
```

Новая trait обычно добавляется в `traits.json`; новый effect — отдельной `TraitEffectPolicy`, если он меняет поведение.

**Неправильно:**

```java
if (trait.equals("workaholic")) score += 15;
else if (trait.equals("brave")) score += 10;
else if (trait.equals("new_trait")) score += 20;
```

**Правильно:** validated config → composable modifier strategies.

### 5.3 L — Liskov Substitution

Реализация интерфейса должна сохранять его контракт.

Все `IPathfinderPort` implementations должны:

- возвращать valid Core `Path` или documented empty/failure;
- уважать timeout/cancel semantics;
- не требовать Minecraft type от caller;
- проверять world scope.

Все `DomainEvent` descendants должны быть immutable и безопасно приниматься generic event bus.

**Неправильно:** `FastPathfinderAdapter` молча игнорирует `canBreakBlocks=false`, нарушая contract.

### 5.4 I — Interface Segregation

Потребитель не должен зависеть от ненужных методов.

Вместо одного:

```java
interface WorldPort {
    inspect();
    setBlock();
    breakBlock();
    spawnEntity();
    findPath();
}
```

использовать capabilities:

```java
interface IBlockReader {
    BlockSnapshot inspect(GridPosition position);
}

interface IBlockWriter {
    BlockMutationResult apply(BlockOperation operation);
}

interface IPathfinderPort { ... }
```

NPC perception получает reader, Building получает writer, planner — pathfinder. Не расширять port «на всякий случай».

### 5.5 D — Dependency Inversion

Core зависит от abstractions, Infrastructure — от Core contracts.

```java
public final class ValidateSettlementService {
    private final WorldSettlementPort world;

    public ValidateSettlementService(WorldSettlementPort world) {
        this.world = world;
    }
}
```

**Неправильно:**

```java
public final class ColonyService {
    private final ForgeBlockAdapter adapter = new ForgeBlockAdapter();
}
```

`ForgeBlockAdapter` подключается в composition root и внедряется через constructor.

## 6. Паттерны проекта

### 6.1 Repository

Repository представляет aggregate collection:

```java
public interface CitizenRepository {
    Optional<Citizen> findById(WorldId worldId, CitizenId id);
    Citizen save(Citizen citizen);
}
```

Repository не содержит UI, доменную бизнес-логику другого контекста или Minecraft mapping в Core.

### 6.2 Factory

Factory создаёт сложный валидный объект:

```java
public interface CitizenFactory {
    Citizen createColonist(WorldId worldId, ColonyId colonyId,
                           GenerationSeed seed);
}
```

`CitizenFactory` использует `TraitGenerator`, `NameGenerator`, config и random port, но сохраняет объект application service.

### 6.3 Strategy

Используется для взаимозаменяемых algorithms:

```java
public interface PathfindingStrategy {
    Optional<Path> find(Position start, Position target,
                        TraversalContext context);
}
```

Реализации: Baritone, Vanilla, test fake. Аналогично `PriorityStrategy`, `DifficultyScalingStrategy`, `ColonyValueRule`.

### 6.4 Observer / Event Bus

```java
public interface IEventBusPort {
    void publish(DomainEvent event);
    <E extends DomainEvent> Subscription subscribe(
            Class<E> type, EventHandler<E> handler);
}
```

События — факты, не команды. Handler не должен напрямую менять чужой aggregate; он вызывает owner use case.

### 6.5 Builder

Builder допустим для сложной конфигурации или command с большим числом optional fields:

```java
BuildOrder order = BuildOrderBuilder.forColony(colonyId)
        .blueprint(blueprintId)
        .position(position)
        .priority(8)
        .resources(required)
        .build();
```

Builder валидирует обязательные поля на `build()`. Не использовать builder для простого `record` с двумя полями.

### 6.6 Adapter

Adapter переводит external API в Core port:

```java
public final class ForgeEntityAdapter implements IEntitySpawnPort {
    // EntitySpawnIntent -> Minecraft Entity
}
```

Mapper boundary не должна протекать в Core.

### 6.7 Value Object

Value object immutable и валидирует собственные значения:

```java
public record ResourceAmount(ResourceType type, int amount) {
    public ResourceAmount {
        if (amount < 0) throw new IllegalArgumentException("amount >= 0");
    }
}
```

Использовать для `Position`, `ResourceType`, `SkillLevel`, `ColonyId`, `CommandId`, `GameTick`.

### 6.8 Aggregate

Aggregate объединяет сущности вокруг root и защищает invariants:

```java
colony.addColonist(npcId);
colony.reserveResource(resource);
colony.pullDomainEvents();
```

Не открывать mutable collections и не позволять service менять поля aggregate напрямую.

## 7. Исключения и ошибки

### 7.1 Категории

- Domain exceptions — unchecked, наследуются от `DomainException`;
- checked exceptions — только для реально восстанавливаемых внешних операций, если выбранный project convention это поддерживает;
- adapter exceptions переводятся в Core-facing `PortAccessException`/typed result.

```java
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
}

public final class ColonyNotFoundException extends DomainException {
    public ColonyNotFoundException(ColonyId id) {
        super("Colony with id " + id + " was not found");
    }
}
```

### 7.2 Правила

- exception message сообщает что произошло, с каким ID и почему;
- не логировать один и тот же exception на каждом слое без добавленной информации;
- не использовать exception для обычного `Optional.empty()` flow;
- не подавлять exception;
- catch должен либо обработать, либо обогатить и rethrow.

**Неправильно:**

```java
try {
    repository.save(entity);
} catch (Exception ignored) {
}
```

**Правильно:**

```java
try {
    repository.save(entity);
} catch (PersistenceException ex) {
    LOG.error("Failed to save citizen {}", citizenId, ex);
    throw new NpcPersistenceException(citizenId, ex);
}
```

## 8. Логирование

### 8.1 Logger

Использовать logger, предоставляемый выбранным loader/build setup: SLF4J/Log4j2 facade. Не использовать `System.out`, `System.err` или `printStackTrace`.

```java
private static final Logger LOG = LoggerFactory.getLogger(CreateColonyService.class);

LOG.debug("Creating citizen with name: {}", name);
LOG.info("Colony {} created in world {}", colonyId, worldId);
LOG.warn("Pathfinding timed out for citizen {}", citizenId);
LOG.error("Failed to load colony {}", colonyId, exception);
```

### 8.2 Уровни

| Уровень | Использование |
|---|---|
| `ERROR` | потеря данных, невозможность продолжить операцию, unexpected exception |
| `WARN` | fallback, timeout, corrupt optional projection, deprecated config |
| `INFO` | startup/shutdown, reload, significant lifecycle events |
| `DEBUG` | входные IDs, decisions, mapping, plan diagnostics |
| `TRACE` | подробные node/path/event details только debug mode |

Не логировать passwords, keys, secrets, полные NBT, private social data или огромные collections.

## 9. Тестирование и покрытие

### 9.1 TDD flow

1. Сформулировать behavior.
2. Написать failing unit test.
3. Реализовать минимальное решение.
4. Refactor с сохранением tests.
5. Добавить integration/acceptance test, если задета boundary.

### 9.2 Цели покрытия

- Core: минимум `80%` line/branch coverage;
- Infrastructure: минимум `70%`;
- critical aggregates/services: higher local target по DoD модуля;
- coverage не заменяет tests на edge cases, mutation и contract behavior.

JaCoCo публикует report и блокирует CI при падении threshold. Все public Core methods должны быть покрыты behavior tests.

### 9.3 Уровни

| Уровень | Что тестировать |
|---|---|
| Unit | aggregates, policies, services, factories, specifications; fake ports |
| Integration | NBT/JSON, Forge/Fabric adapters, network, entity/world behavior |
| Contract | repository/port implementations имеют одинаковые semantics |
| Acceptance | пользовательские сценарии Gherkin |

## 10. Запрещённые практики

### 10.1 Console и swallowed errors

```java
System.out.println("Created");       // запрещено
exception.printStackTrace();         // запрещено
catch (Exception ignored) {}         // запрещено
```

### 10.2 Hardcode

**Неправильно:**

```java
if (hunger < 30) { ... }
```

**Правильно:**

```java
if (hunger < settings.survivalFloors().eat()) { ... }
```

Hardcoded physics constants допустимы только как named constants с объяснённой доменной причиной; content/difficulty values — в JSON.

### 10.3 Null

Для отсутствия результата использовать `Optional`, empty collection или typed result:

```java
public Optional<Citizen> findById(CitizenId id) { ... }
public List<Task> findPending() { return List.of(); }
```

`null` допустим только внутри строго локального framework boundary, если API этого требует, и должен быть немедленно преобразован.

### 10.4 Global mutable state

Запрещены mutable static singleton repositories/services. Stateless constants и immutable registries допустимы. Composition root владеет lifecycle instances.

### 10.5 Core/Minecraft coupling

```java
// запрещено в Core
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
```

Использовать `Position`, `GridPosition`, `IBlockWorldPort` и adapter mapper.

## 11. Автоматические проверки

### 11.1 Format и style

Рекомендуемый CI pipeline:

```text
format check
compile
unit tests
JaCoCo
Checkstyle/Google Java Format
SpotBugs
ArchUnit
integration tests
```

Конкретный formatter выбирается один раз и фиксируется в build configuration. Не форматировать разные файлы разными IDE profiles.

### 11.2 Инструменты

- **Checkstyle** — naming, imports, Javadoc, line/style constraints;
- **Google Java Format** — deterministic formatting, если принят проектом;
- **SpotBugs** — потенциальные defects, nullness и concurrency issues;
- **JaCoCo** — coverage;
- **ArchUnit** — package/dependency architecture;
- **SonarQube** — optional maintainability/security dashboard;
- **JUnit/Mockito** — unit and mock tests;
- **JSON Schema validator** — config contracts.

### 11.3 Pre-commit/CI

Локально рекомендуется запускать быстрый набор format/check/unit. CI всегда запускает полный набор независимо от локального результата. Generated files исключаются только явно, а не через широкие ignore patterns.

## 12. Правильный и неправильный код

### 12.1 God class

**Неправильно:**

```java
final class GodClass {
    void readNbt() {}
    void updateMood() {}
    void findBaritonePath() {}
    void spawnMinecraftEntity() {}
    void renderScreen() {}
}
```

**Правильно:**

```java
final class Citizen { /* NPC invariants */ }
final class MoodCalculationService { /* mood policy */ }
final class BaritonePathfinderAdapter { /* external integration */ }
final class ForgeEntityAdapter { /* entity projection */ }
final class CitizenRenderer { /* visualization only */ }
```

### 12.2 `instanceof` Minecraft в Core

**Неправильно:**

```java
if (entity instanceof ForgeEntity forgeEntity) {
    forgeEntity.moveTo(...);
}
```

**Правильно:**

```java
entityPort.applyIntent(new MoveIntent(citizenId, target));
```

### 12.3 Слишком много параметров

**Неправильно:**

```java
createCitizen(UUID id, String name, String race, String gender,
              int x, int y, int z, int mood, int hunger,
              int skill, String colonyId);
```

**Правильно:**

```java
createCitizen(CitizenCreationRequest request);
```

```java
public record CitizenCreationRequest(
        CitizenId id,
        CitizenName name,
        RaceId race,
        Gender gender,
        Position position,
        NeedState initialNeeds,
        ColonyId colonyId
) {}
```

### 12.4 Null вместо Optional

**Неправильно:**

```java
public Citizen find(String id) { return null; }
```

**Правильно:**

```java
public Optional<Citizen> findById(CitizenId id) { return Optional.empty(); }
```

### 12.5 Mutable configuration

**Неправильно:**

```java
Map<String, Object> config = loadJson();
service.setConfig(config);
```

**Правильно:**

```java
ConfigSnapshot<GoalSettings> snapshot = configPort.get(key, GoalSettings.class);
service = new PriorityEvaluator(snapshot.value());
```

## 13. Время и многопоточность

### 13.1 Игровое время

Использовать `ITimePort`/`ClockPort`:

```java
GameTick now = time.currentTick(worldId);
```

Не использовать `System.currentTimeMillis()` для game cooldown, NPC needs, Storyteller timing или plan duration.

### 13.2 Async operations

Для IO/path searches допускается `CompletableFuture` и bounded `ExecutorService`:

```java
CompletableFuture<Optional<Path>> future =
        pathfinder.findPathAsync(start, target, context, requestId);
```

Правила:

- executor имеет bounded queue;
- cancellation и timeout обязательны;
- immutable DTO передаются между потоками;
- Minecraft world mutation возвращается на server thread;
- shutdown закрывает executor;
- не создавать thread на каждого NPC.

### 13.3 Thread safety

- value objects immutable;
- mutable aggregate принадлежит одному application/server thread или защищён explicit lock;
- `ConcurrentHashMap` только для действительно shared registries;
- не публиковать mutable list в event;
- document happens-before и ownership для async adapter.

## 14. Документирование кода

### 14.1 Javadoc

Каждый public Core class, port, repository, factory и service method документируется:

```java
/**
 * Finds a citizen within the specified world scope.
 *
 * @param worldId world containing the citizen
 * @param citizenId stable citizen identifier
 * @return citizen, or empty when it does not exist
 * @throws RepositoryException when storage is unavailable
 */
Optional<Citizen> findById(WorldId worldId, CitizenId citizenId);
```

Javadoc должен описывать purpose, parameters, return semantics, exceptions, thread/transaction guarantees, если они важны.

### 14.2 Комментарии

Комментировать:

- сложную эвристику;
- reason for non-obvious invariant;
- thread handoff;
- migration compatibility;
- workaround внешнего API.

Не комментировать очевидное:

```java
// Increment counter by one — плохой комментарий
counter++;
```

## 15. Code review checklist

- [ ] Изменение находится в правильном bounded context.
- [ ] Core не импортирует Minecraft/infrastructure.
- [ ] У класса одна ответственность.
- [ ] Dependencies внедряются через constructor/port.
- [ ] Нет mutable global state.
- [ ] IDs/value objects используются вместо неясных strings/primitives.
- [ ] Публичный API документирован.
- [ ] Ошибки не подавляются и имеют понятные codes/messages.
- [ ] Нет `System.out`, `printStackTrace`, необоснованного `null` или hardcode.
- [ ] Async code имеет timeout/cancel/shutdown policy.
- [ ] Добавлены unit/integration/contract tests по необходимости.
- [ ] Coverage gates проходят.
- [ ] ArchUnit/SpotBugs/formatter проходят.
- [ ] События versioned и handlers idempotent, если изменение event-driven.
- [ ] JSON/schema/documentation обновлены при изменении config contract.

## 16. DoD стиля кода для Pull Request

- [ ] Код отформатирован в соответствии с единым formatter.
- [ ] Все публичные Core ports, classes и methods имеют Javadoc.
- [ ] Нет предупреждений Checkstyle/SpotBugs или они обоснованы.
- [ ] ArchUnit rules не нарушены.
- [ ] Тесты добавлены и проходят.
- [ ] Покрытие не ниже `80%` для Core и `70%` для Infrastructure.
- [ ] Нет `System.out`, `System.err`, `e.printStackTrace()` и пустых catch-blocks.
- [ ] Нет необоснованного hardcode или mutable static state.
- [ ] Core не зависит от Minecraft API.
- [ ] Выполнен code review минимум двумя approvers для архитектурно значимых изменений; малые изменения следуют repository branch policy.
- [ ] Документация/ADR обновлены при изменении public contract.

## 17. Эволюция стандарта

Правила пересматриваются при:

- переходе на новую major версию Java;
- смене Forge/Fabric mappings или build toolchain;
- добавлении нового loader/backend;
- появлении нового recurring code smell;
- изменении архитектурных границ;
- обновлении formatter/static analysis tools.

Процедура:

1. предложить изменение с мотивацией и примерами;
2. проверить влияние на Core, adapters и CI;
3. обновить этот документ и связанные rules;
4. применить formatter/config migration;
5. объявить grace period для массового mechanical refactor;
6. включить новое правило в CI после исправления legacy violations;
7. создать ADR, если меняется архитектурный принцип.

## 18. Связи с другими документами

- `ddd-tactical-patterns.md` — подробности Aggregate, Entity, Value Object, Repository и Domain Service.
- `testing-strategy.md` — стратегия тестирования и coverage.
- `archunit-rules.md` — executable ArchUnit rules.
- `definition-of-done-do-d.md` — общий DoD проекта.
- [`system-overview.md`](system-overview.md) — контейнеры и dependency direction.
- [`bounded-contexts.md`](bounded-contexts.md) — границы и ownership контекстов.
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — ports, adapters и DI.
- [`event-system-api.md`](event-system-api.md) — event contracts и handler rules.
- [`data-interfaces.md`](data-interfaces.md) — repository/factory/specification contracts.

## 19. Итог

Хороший код RimWorldCraft должен быть понятен без запуска Minecraft, тестируем через порты, ограничен bounded context и устойчив к замене Forge/Fabric, Baritone, NBT или JSON backend. SOLID здесь не является формальной целью: он используется для сохранения этих свойств. Если класс одновременно принимает решения, хранит данные, пишет NBT и управляет entity, граница нарушена и её нужно разделить.
