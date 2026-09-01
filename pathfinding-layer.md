# RimWorldCraft — Pathfinding Layer

## 1. Введение

`Pathfinding Layer` — инфраструктурный слой, предоставляющий NPC маршруты по сложному воксельному миру Minecraft. Он реализует Core-порт `IPathfinderPort` и адаптирует Baritone либо совместимый форк Automatone к platform-neutral моделям `Position`, `Path`, `Waypoint` и `EntityTraversalContext`.

Модуль находится вне `core`: решение о том, **куда и зачем** идти, принимает [`module-goal-ai.md`](module-goal-ai.md), а navigation layer отвечает только за достижимость и маршрут. `module-building-system.md` использует его для подхода к ghost blocks; [`entity-integration.md`](entity-integration.md) — для движения runtime entities.

### 1.1 Ответственность

- построить путь между двумя Core positions;
- построить путь к entity target;
- проверить reachability;
- преобразовать traversal policy в Baritone settings;
- сгладить и нормализовать результат;
- выполнять поиск асинхронно с bounded concurrency;
- отменять stale запросы;
- возвращать диагностируемый результат без утечки Minecraft/Baritone types.

### 1.2 Не входит в модуль

- выбор `EAT`, `BUILD`, `FLEE` или иной Goal;
- управление needs/mood/skills;
- task assignment;
- окончательная физика движения entity;
- изменение блоков, кроме опциональной capability, явно разрешённой policy;
- хранение domain aggregates.

## 2. Термины

| Термин | Определение |
|---|---|
| **Path** | Нормализованная последовательность `Waypoint` от start к target. |
| **Waypoint** | Узел маршрута с позицией, cost/time estimate и traversal flags. |
| **Node** | Кандидат состояния в поисковом графе. |
| **Graph** | Связанные состояния пространства, по которым выполняется поиск. |
| **Voxel world** | Мир из дискретных блоковых ячеек с высотой, collision и traversal properties. |
| **A\\*** | Поиск минимальной/достаточно дешёвой цепочки с `f(n)=g(n)+h(n)`. |
| **Heuristic** | Оценка остаточной стоимости до цели. |
| **Cost** | Цена движения/действия: distance, danger, breaking, swimming, jump. |
| **Trajectory** | Runtime motion, получаемое из path и исполняемое entity navigation. |
| **Barrier** | Непроходимая граница: unloaded chunk, world border, protected area. |
| **Obstacle** | Блок/entity/условие, мешающее проходу. |
| **Smooth path** | Путь с удалёнными лишними collinear waypoints при сохранении безопасности. |
| **Traversal context** | Ограничения NPC: jump, swim, break/place, doors, fall height, speed. |
| **Stale request** | Поиск, чей результат больше не нужен из-за replan/cancel/revision. |

## 3. Обзор Baritone/Automatone

Baritone — pathfinding/navigation library для Minecraft, использующая собственный поиск по состояниям блокового мира, goal abstractions и configurable movement settings. Она умеет искать маршруты по terrain, учитывать прыжки, parkour, воду, двери, падения и другие traversal costs. Automatone рассматривается как совместимый fork/variant; конкретные API names зависят от версии.

### 3.1 Почему Baritone

- зрелый алгоритмический стек и практичный поиск по Minecraft world;
- поддержка разных типов целей и movement policies;
- возможность ограничивать timeout и опасные действия;
- open-source ecosystem и готовые integration points;
- можно заменить реализацию, сохранив `IPathfinderPort`.

### 3.2 Альтернативы

| Альтернатива | Ограничения |
|---|---|
| Vanilla `PathFinder` | хорошо подходит для стандартных mob goals, но ограничен для сложных colony routes и custom cost policies |
| Собственный A* | полный контроль, но дорого поддерживать voxel collision, chunk loading и edge cases |
| Другой mod pathfinder | может иметь нестабильный API, license/loader constraints или недостаточную поддержку traversal policies |

Выбор Baritone не делает его частью Core. Он остаётся заменяемым infrastructure dependency.

### 3.3 Подключение зависимости

Версия, coordinates и mappings должны соответствовать фактическому Forge/Fabric build setup. Пример-паттерн:

```groovy
repositories {
    maven { url = uri("https://example.invalid/baritone-maven") }
}

dependencies {
    implementation("baritone:baritone-api:<pinned-version>")
    // or the project-approved Automatone artifact
}
```

Не копировать этот placeholder в production без выбора конкретного artifact/repository. Версия фиксируется lock/verification metadata, а optional dependency защищается runtime capability check.

## 4. Core-порт `IPathfinderPort`

### 4.1 Core-модели

```java
public record Position(WorldId worldId, int x, int y, int z) {}

public record Waypoint(
        Position position,
        double cumulativeCost,
        long expectedArrivalTicks,
        Set<PathFlag> flags
) {}

public record Path(
        Position start,
        Position target,
        List<Waypoint> waypoints,
        double totalCost,
        long estimatedTicks,
        PathStatus status
) {
    public Path {
        waypoints = List.copyOf(waypoints);
    }
}

public record EntityTraversalContext(
        int maxJumpHeight,
        int maxFallHeight,
        boolean canJump,
        boolean canSwim,
        boolean canFly,
        boolean canBreakBlocks,
        boolean canPlaceBlocks,
        boolean canOpenDoors,
        boolean allowParkour,
        Duration timeout
) {}
```

`Position` не содержит `BlockPos`; `Path` не содержит Baritone path object. `PathFlag` например: `JUMP`, `FALL`, `SWIM`, `OPEN_DOOR`, `BREAK_BLOCK`, `DANGER`, `PORTAL`.

### 4.2 Интерфейс

```java
public interface IPathfinderPort {
    Optional<Path> findPath(
            Position start,
            Position target,
            EntityTraversalContext context);

    Optional<Path> findPathToEntity(
            Position start,
            UUID entityId,
            EntityTraversalContext context);

    boolean isReachable(Position start, Position target);

    void cancelPath(UUID entityId);
}
```

Для асинхронного Core flow рекомендуется дополнительный интерфейс:

```java
public interface AsyncPathfinderPort {
    CompletableFuture<Optional<Path>> findPathAsync(
            Position start,
            Position target,
            EntityTraversalContext context,
            PathRequestId requestId);

    void cancel(PathRequestId requestId);
}
```

Синхронный метод допустим только для короткого bounded query. Он не должен блокировать server tick на полный Baritone search.

### 4.3 Контракты

- start и target имеют один `WorldId`;
- `timeout` ограничен и неотрицателен;
- path начинается с start либо нормализованной ближайшей точки;
- последний waypoint удовлетворяет target tolerance;
- результат не содержит unloaded/invalid nodes;
- cancellation не обязан вернуть path;
- `isReachable` не мутирует мир;
- entity lookup проверяет world/authority.

## 5. `BaritonePathfinderAdapter`

### 5.1 Роль

`BaritonePathfinderAdapter` реализует `IPathfinderPort`/`AsyncPathfinderPort` в `infrastructure.pathfinding.baritone`. Он является anti-corruption layer между Core и внешним API.

```text
Core Position/Context
        |
        v
BaritonePathfinderAdapter
  PositionMapper
  TraversalSettingsMapper
  RequestRegistry
  PathResultMapper
        |
        v
Baritone API + Minecraft world
```

### 5.2 Lifecycle

1. На common/server setup создаётся adapter factory.
2. Проверяется наличие нужной Baritone/Automatone capability и совместимость версии.
3. Для каждого server world/entity создаётся managed path context.
4. Settings baseline загружается из validated config.
5. На world unload отменяются requests и освобождаются handles.
6. На server shutdown executor/Baritone handles закрываются.

Baritone global process/shared settings нельзя изменять без synchronization: per-request overrides должны применяться локально либо через scoped settings API.

### 5.3 Интерфейсный sketch

Названия методов Baritone намеренно приведены как псевдокод: они меняются между версиями API.

```java
public final class BaritonePathfinderAdapter implements IPathfinderPort {
    private final BaritoneProvider provider;
    private final ExecutorService executor;
    private final RequestRegistry requests;
    private final PathResultMapper mapper;
    private final PathfinderSettings settings;

    @Override
    public Optional<Path> findPath(Position start, Position target,
                                   EntityTraversalContext context) {
        validate(start, target, context);
        return findPathAsync(start, target, context,
                PathRequestId.random()).join();
    }

    public CompletableFuture<Optional<Path>> findPathAsync(
            Position start, Position target,
            EntityTraversalContext context, PathRequestId requestId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BaritoneController baritone = provider.forWorld(start.worldId());
                ScopedSettings scoped = settingsMapper.toScopedSettings(context);
                BaritoneGoal goal = new GoalBlock(
                        new BlockPos(target.x(), target.y(), target.z()));
                requests.register(requestId, baritone);
                BaritonePath result = baritone.compute(goal, scoped,
                        context.timeout());
                return mapper.toCore(result, start, target);
            } catch (BaritoneTimeoutException ex) {
                return Optional.empty();
            } finally {
                requests.remove(requestId);
            }
        }, executor);
    }

    @Override
    public void cancelPath(UUID entityId) {
        requests.cancelForEntity(entityId);
    }
}
```

### 5.4 Search target

- точная block target → `GoalBlock`;
- target по X/Z → `GoalXZ`;
- entity target → `Goal`/dynamic goal, обновляющий position target;
- radius/tolerance → `GoalNear`/equivalent;
- moving target → requery/replan, а не фиксированный stale path.

Adapter не должен передавать `GoalBlock` в Core.

### 5.5 Синхронность и async

Baritone search выполняется в worker pool или собственном безопасном executor, но доступ к Minecraft world должен соответствовать loader thread rules. Если Baritone требует world reads на specific thread, используется platform scheduler/bridge; нельзя бездумно читать `Level` из arbitrary worker thread.

Рекомендуется:

- bounded pool `1..N` per server, а не thread per NPC;
- per-world/entity concurrency limit;
- request timeout и cancellation token;
- queue capacity с rejection/defer policy;
- result delivery через `CompletableFuture`;
- server-thread hop только для entity/world mutation.

Поиск не должен вызывать `Entity#setPos` или ставить блок в worker thread.

## 6. Маппинг данных

### 6.1 Таблица типов

| Core | Baritone/Minecraft | Правило |
|---|---|---|
| `Position` | `BlockPos` | преобразование координат и проверка world |
| `WorldId` | `ServerLevel`/dimension key | adapter resolves, Core type не просачивается |
| `EntityTraversalContext.canJump` | jump/parkour settings | false запрещает соответствующие движения |
| `canBreakBlocks` | `allowBreak`/`allowMine` | domain policy имеет приоритет |
| `canPlaceBlocks` | `allowPlace`/parkour place | включается только для build capability |
| `maxFallHeight` | `maxFallHeight` | clamp к server safety limit |
| `timeout` | primary/secondary timeout | min(request, global cap) |
| Core `Waypoint` | Baritone path positions | mapper добавляет flags/cost estimate |
| `PathStatus` | calculation result state | `FOUND`, `PARTIAL`, `TIMEOUT`, `CANCELLED`, `FAILED` |
| `EntityTraversalContext.canOpenDoors` | door interaction setting | адаптер не открывает protected door без permission |

### 6.2 Position mapper

```java
public final class PositionMapper {
    public BlockPos toMinecraft(Position position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    public Position toCore(ServerLevel level, BlockPos position) {
        return new Position(WorldIds.from(level), position.getX(),
                position.getY(), position.getZ());
    }
}
```

Mapper проверяет dimension consistency и не принимает client-provided level.

### 6.3 Traversal settings mapper

```java
public final class TraversalSettingsMapper {
    public ScopedSettings toScopedSettings(EntityTraversalContext context) {
        ScopedSettings settings = new ScopedSettings();
        settings.setAllowBreak(context.canBreakBlocks());
        settings.setAllowPlace(context.canPlaceBlocks());
        settings.setAllowParkour(context.allowParkour());
        settings.setAllowParkourPlace(context.canPlaceBlocks());
        settings.setAllowSwimming(context.canSwim());
        settings.setAllowJumping(context.canJump());
        settings.setMaxFallHeight(context.maxFallHeight());
        return settings;
    }
}
```

`allowBreak=true` не является разрешением на любую разрушительную операцию: resource/permission policy проверяется выше или отдельным action adapter.

### 6.4 Path mapper и сглаживание

1. получить Baritone path positions;
2. удалить consecutive duplicates;
3. вычислить flags по переходам;
4. проверить каждый сегмент через collision/traversal validator;
5. удалить collinear nodes только если line-of-sight и clearance безопасны;
6. посчитать cumulative cost/estimated ticks;
7. ограничить число waypoints;
8. вернуть immutable Core `Path`.

Нельзя сглаживать path так, чтобы waypoint пересекал corner, slab, door или опасный fall.

## 7. Конфигурация Baritone

Источник: `config/rwc/pathfinding/pathfinder-settings.json`.

```json
{
  "$schema": "rwc://schemas/pathfinding/pathfinder-settings.schema.json",
  "version": 1,
  "backend": "BARITONE",
  "allowBreak": false,
  "allowPlace": false,
  "allowParkour": false,
  "allowParkourPlace": false,
  "allowMine": false,
  "allowSprint": true,
  "allowSwimming": true,
  "primaryTimeoutMS": 5000,
  "secondaryTimeoutMS": 10000,
  "maxFallHeight": 3,
  "maxJumpHeight": 1,
  "maxConcurrentSearches": 8,
  "queueCapacity": 128,
  "smoothPath": true,
  "maxWaypoints": 512,
  "fallbackBackend": "VANILLA"
}
```

### 7.1 Ограничения

- timeout `1..60000 ms`, server safety cap может быть ниже;
- `maxFallHeight >= 0` и ограничен policy;
- `maxJumpHeight >= 0`;
- concurrent searches и queue capacity bounded;
- `allowBreak/place/mine` false по умолчанию;
- fallback допускается только для read-only reachability/простых маршрутов и должен быть явно виден в diagnostics;
- unknown backend → startup validation error или configured safe fallback.

### 7.2 Per-request override

`EntityTraversalContext` может переопределять только разрешённые поля в пределах global safety bounds. Например, NPC builder может `canPlaceBlocks=true`, но NPC без capability не может включить `canBreakBlocks` через command packet.

### 7.3 Reload

Config reload выполняется atomic snapshot swap. Текущие requests завершаются на старом settings snapshot или отменяются по policy; нельзя менять global Baritone settings посреди вычисления без scoped synchronization.

## 8. Ошибки и fallback

### 8.1 Ошибки

```java
public class PathfindingException extends RuntimeException {
    public PathfindingException(String message, Throwable cause) {
        super(message, cause);
    }
}

public final class PathNotFoundException extends PathfindingException {
    public PathNotFoundException(String message) { super(message, null); }
}

public final class PathfinderTimeoutException extends PathfindingException {
    public PathfinderTimeoutException(String message) { super(message, null); }
}

public final class PathfinderCancelledException extends PathfindingException {
    public PathfinderCancelledException(String message) { super(message, null); }
}
```

Public port может возвращать `Optional.empty()` для not found/timeout/cancelled, если это выбранный контракт. Нельзя одновременно скрывать все причины: `PathResult`/diagnostics должны сохранять reason для logging и FailureHandler.

### 8.2 Стратегия

| Ситуация | Core result | Реакция |
|---|---|---|
| нет пути | `Optional.empty`/`NOT_FOUND` | Goal AI replan/fallback |
| timeout | `TIMEOUT` | defer, retry с budget или alternate goal |
| cancel | `CANCELLED` | stale result ignored |
| Baritone unavailable | `BACKEND_UNAVAILABLE` | safe fallback или disable navigation |
| unloaded chunk | `WORLD_UNAVAILABLE` | retry after load, не force-load без policy |
| malformed result | `INVALID_RESULT` | discard и diagnostic |
| adapter exception | `FAILED` | log + bounded retry |

Fallback vanilla `PathFinder` должен использовать тот же Core port, например `VanillaPathfinderAdapter`, и пройти contract tests. Нельзя fallback выполнять разрушительные действия, если Baritone policy была read-only.

## 9. Использование в Goal AI и Building

### 9.1 Goal AI

`Goal AI` вызывает путь для `MOVE_TO`, проверяет reachability, затем executor исполняет navigation intent. Path failure приводит к `ActionCompletedEvent(success=false)`/`PlanFailedEvent`, а не к бесконечной попытке.

### 9.2 Building System

Для ghost block:

1. Building создаёт `BuildTask` target position.
2. Goal AI создаёт `MOVE_TO` action.
3. Pathfinder строит path без break/place, если это policy строительства.
4. После достижения target Building вызывает `IBlockWorldPort` для реального блока.
5. При изменении ghost/obstacle path invalidates.

## 10. Жизненный цикл request и cancellation

```text
create PathRequestId
  -> validate world/context
  -> enqueue bounded request
  -> register entity/request mapping
  -> compute Baritone path
  -> map + validate result
  -> complete future
  -> remove registry entry
```

Cancellation:

1. mark request cancelled atomically;
2. call Baritone cancellation if supported;
3. interrupt future/stop delivery;
4. remove mapping;
5. ignore late result by request revision.

`cancelPath(entityId)` отменяет только requests этой entity. Новая replan request получает higher revision и автоматически делает старый result stale.

## 11. Тестирование

### 11.1 Unit-тесты mapper/settings

- `Position ↔ BlockPos` сохраняет координаты и world;
- traversal flags мапятся корректно;
- dangerous overrides clamp’ятся;
- smoothing не удаляет corner/obstacle waypoint;
- duplicate/invalid Baritone nodes отклоняются;
- result status корректно переводится;
- config validation ловит invalid timeout/queue/enum.

```java
@Test
void mapsReadOnlyContextWithoutDestructiveBaritoneSettings() {
    EntityTraversalContext context = TraversalFixtures.readOnly();
    ScopedSettings settings = mapper.toScopedSettings(context);

    assertThat(settings.allowBreak()).isFalse();
    assertThat(settings.allowPlace()).isFalse();
    assertThat(settings.allowParkour()).isFalse();
}
```

### 11.2 Adapter tests с mock Baritone API

Проверить:

- `GoalBlock` получает правильный target;
- timeout переводится в empty/diagnostic;
- null path не вызывает NullPointerException;
- cancel вызывает Baritone cancel;
- late result после cancel игнорируется;
- concurrent requests имеют независимые scoped settings;
- exceptions одного request не останавливают pool.

### 11.3 Test doubles

```java
final class FakePathfinderPort implements IPathfinderPort {
    private final Map<Position, Path> paths = new HashMap<>();

    @Override
    public Optional<Path> findPath(Position start, Position target,
                                   EntityTraversalContext context) {
        return Optional.ofNullable(paths.get(target));
    }

    @Override
    public Optional<Path> findPathToEntity(Position start, UUID entityId,
                                           EntityTraversalContext context) {
        return Optional.empty();
    }

    @Override public boolean isReachable(Position start, Position target) {
        return paths.containsKey(target);
    }
    @Override public void cancelPath(UUID entityId) {}
}
```

### 11.4 Integration с test Minecraft world

`TestModLoader`/loader-specific harness проверяет:

- ровная поверхность: короткий путь;
- горы/ступени: корректные jumps;
- вода: swimming policy;
- закрытые двери: `canOpenDoors` true/false;
- ямы/fall height;
- obstacle/house corridor;
- unloaded chunks;
- protected blocks;
- dynamic door/enemy invalidation;
- Forge и Fabric adapters с одинаковым contract suite;
- performance under multiple concurrent NPC searches.

### 11.5 Acceptance Gherkin

```gherkin
Feature: Pathfinding

  Scenario: NPC finds a path to a target
    Given a citizen at position (0, 64, 0)
    And a target at position (10, 64, 10)
    And block breaking is disabled
    When the citizen requests a path via IPathfinderPort
    Then a valid Path with waypoints is returned
    And the path length is approximately 14 blocks
    And no waypoint requires breaking a block

  Scenario: Search is cancelled after replanning
    Given a citizen is searching a path to a storage room
    When Goal AI requests a flee plan
    Then the storage path is cancelled
    And the old result is ignored
    And a path for the flee target is requested
```

### 11.6 Performance tests

Измеряются p50/p95/p99 latency, expanded nodes, queue wait, cancellation rate, memory per request и server tick impact. Не следует оценивать только среднее время: timeout spikes важнее для multiplayer.

## 12. DoD слоя навигации

- [ ] Baritone или Automatone подключён как pinned dependency и корректно загружается.
- [ ] `BaritonePathfinderAdapter` реализует `IPathfinderPort` и зарегистрирован в composition root.
- [ ] `findPath`, `findPathToEntity`, `isReachable` и `cancelPath` работают с Core models.
- [ ] Position, traversal context и path conversion покрыты tests.
- [ ] Асинхронный поиск не блокирует основной server tick.
- [ ] Cancellation и stale-result protection работают.
- [ ] Config загружается из JSON, валидируется и применяется per request безопасно.
- [ ] Написаны integration tests в test Minecraft world: plane, mountains, water, doors, pits.
- [ ] Fallback backend имеет явную policy и contract tests.
- [ ] ArchUnit rules не нарушены.
- [ ] Нет бесконечного queue/retry growth и утечки executor threads.
- [ ] Ошибки логируются с request/entity/world correlation metadata.

## 13. ArchUnit-правила

### 13.1 Infrastructure может видеть Baritone/Minecraft, но Core — нет

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "net.minecraft..",
        "net.minecraftforge..",
        "net.fabricmc..",
        "baritone..",
        "automatone.."
    );
```

### 13.2 Adapter реализует только Core port contract

```java
classes()
    .that().haveSimpleName("BaritonePathfinderAdapter")
    .should().implement(IPathfinderPort.class);
```

### 13.3 Adapter находится в infrastructure pathfinding

```java
classes()
    .that().haveSimpleNameEndingWith("PathfinderAdapter")
    .should().resideInAnyPackage(
        "com.rimworldcraft.infrastructure.pathfinding..");
```

### 13.4 В адаптере нет bounded-context business logic

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.pathfinding..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core.npc.domain..",
        "com.rimworldcraft.core.colony.domain..",
        "com.rimworldcraft.core.goal.domain..",
        "com.rimworldcraft.core.story.domain.."
    );
```

Он может зависеть от ports, DTO и shared IDs, но не принимать решения о goals/resources.

### 13.5 Запрет `System.out`

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.pathfinding..")
    .should().accessClassesThat()
    .belongToAnyOf(System.class);
```

Используется logger/observability port.

### 13.6 Core port не зависит от Baritone

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.ports..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("baritone..", "net.minecraft..");
```

## 14. Расширение и замена backend

### 14.1 Другой алгоритм

Добавляется `AStarPathfinderAdapter`, `VanillaPathfinderAdapter` или новый library adapter, реализующий тот же `IPathfinderPort`. Core Goal AI не меняется. Все adapters проходят:

- position/context contract;
- reachability;
- cancellation;
- timeout;
- path validity;
- cross-world safety.

### 14.2 Летающие NPC

Ввести `FlyingTraversalContext` и отдельный adapter, который понимает 3D movement/clearance. Не добавлять flight branches в Goal AI planner; он видит capability через traversal context.

### 14.3 Динамические препятствия

- entity/door/block changes публикуют invalidation event;
- request registry помечает affected paths stale;
- Goal AI получает replan trigger;
- adapter не пытается бесконечно «чинить» старый path;
- door opening допускается только с соответствующей capability.

### 14.4 Streaming waypoint execution

Для длинных маршрутов можно возвращать path segments или async stream, но базовый `Path` contract сохраняется. Segment revision и target tolerance обязательны.

### 14.5 Baritone API migration

При обновлении Baritone:

1. оставить Core API без изменений;
2. обновить только mapper/adapter;
3. запустить adapter contract suite;
4. проверить license/version compatibility;
5. прогнать test worlds и performance benchmarks;
6. зафиксировать breaking mapping в ADR.

## 15. Связи с другими документами

- [`hexagonal-architecture.md`](hexagonal-architecture.md) — `IPathfinderPort`, driven adapters, Core DTO и DI.
- [`module-goal-ai.md`](module-goal-ai.md) — `MOVE_TO`, planning, action execution, replanning и failure handling.
- [`entity-integration.md`](entity-integration.md) — runtime NPC entities, server thread и entity movement projection.
- [`module-building-system.md`](module-building-system.md) — подход к ghost blocks, target validation и build tasks.
- [`module-npc-core.md`](module-npc-core.md) — NPC traversal capabilities, tasks и behavior state.
- [`system-overview.md`](system-overview.md) — контейнеры, server authority и общие ports.
- [`event-system-api.md`](event-system-api.md) — события invalidation, action failure и cross-context delivery.
- [`data-interfaces.md`](data-interfaces.md) — repositories и сохранение AI/path state.
- `save-serialization.md` — сохранение request/task state и migration policy.

## 16. Итоговая модель

```text
Goal AI: зачем идти и какая цель
       |
       v
IPathfinderPort: можно ли пройти и каким маршрутом
       |
       v
BaritonePathfinderAdapter
       |
       +--> Position/Context mapper
       +--> bounded async request registry
       +--> Baritone/Automatone
       +--> Core Path mapper/smoother
       |
       v
ActionExecutor -> Entity Integration -> Minecraft movement
```

Navigation Layer должен оставаться заменяемым техническим компонентом: Core знает только абстрактный путь и traversal policy, Goal AI — только action result, а Baritone/Minecraft details локализованы в adapter и его tests.
