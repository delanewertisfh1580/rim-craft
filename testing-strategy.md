# RimWorldCraft — Testing Strategy

## 1. Введение

Этот документ определяет единую стратегию тестирования RimWorldCraft — Minecraft-мода на Java 17+ с Forge/Fabric, DDD, шестиугольной архитектурой, JSON-конфигурациями, event-driven взаимодействием и несколькими игровыми bounded contexts.

Цель стратегии — обеспечить не только отсутствие очевидных ошибок, но и устойчивость доменных инвариантов, совместимость сохранений, корректность multiplayer-синхронизации, предсказуемость процедурной генерации и приемлемое влияние на server tick.

Документ применяется к:

- Core: Colony, NPC, Storyteller, Goal AI, Building System;
- Infrastructure: NBT, JSON, entity, network, Baritone/Fabric/Forge adapters;
- Client: renderer, HUD и GUI projections;
- конфигурациям и миграциям;
- Event System и межконтекстным цепочкам;
- CI/CD, release candidates и nightly stability runs.

Он связан с [`definition-of-done-do-d.md`](definition-of-done-do-d.md), [`codestyle-and-solidd.md`](codestyle-and-solidd.md), [`ddd-tactical-patterns.md`](ddd-tactical-patterns.md), [`event-system-api.md`](event-system-api.md), [`data-interfaces.md`](data-interfaces.md), [`save-serialization.md`](save-serialization.md) и архитектурными module-документами.

### 1.1 Основные цели качества

- Core тестируется без Minecraft runtime.
- Каждый aggregate защищает свои инварианты.
- Каждый bugfix получает regression test.
- Каждая внешняя граница имеет integration или contract test.
- Конфиги и save schemas проверяются автоматически.
- Async, multiplayer и event delivery проверяются отдельно от happy path.
- Performance измеряется на realistic fixtures, а не только на маленьком mock.
- Flaky tests обнаруживаются, классифицируются и устраняются, а не скрываются бесконечными retries.

## 2. Принципы тестирования

1. **Тестируем поведение, а не реализацию.** Тест должен защищать observable contract.
2. **Пирамида важнее количества.** Больше быстрых unit-тестов, меньше дорогих world tests.
3. **Core-first.** Доменная логика должна быть проверяема на чистой JVM.
4. **Детерминированность.** Время, random, world facts и event delivery заменяются контролируемыми портами.
5. **Один тест — одна причина падения.** Не объединять несвязанные сценарии.
6. **Failure paths обязательны.** Проверяются timeout, missing data, conflicts, cancellation и corrupted input.
7. **Тесты должны быть повторяемыми.** Seed, fixture version и environment фиксируются.
8. **Coverage — индикатор, не доказательство качества.** Критические инварианты покрываются behavior tests и mutation testing.
9. **Integration boundaries тестируются контрактами.** Forge и Fabric implementations проходят один общий набор semantic checks.
10. **Test code имеет production-quality.** Плохой fixture может маскировать defect.

## 3. Пирамида тестов

```text
                 Acceptance / E2E
              ┌────────────────────┐
              │ few, slow, realistic│
              └────────────────────┘
           Integration / Contract / World
        ┌──────────────────────────────────┐
        │ adapters, NBT, JSON, network, MC │
        └──────────────────────────────────┘
                 Unit / Property
     ┌────────────────────────────────────────┐
     │ fast Core aggregates, policies, services│
     └────────────────────────────────────────┘
```

| Уровень | Целевая доля | Время | Среда | Примеры |
|---|---:|---:|---|---|
| Unit | 60–75% | миллисекунды | чистая JVM, fakes | `Colony`, `Citizen`, GOAP, value objects |
| Contract | 5–10% | секунды | adapter-independent suite | repositories, ports, codecs |
| Integration | 15–25% | секунды/минуты | test world/loader | NBT, entity, network, Baritone |
| Acceptance | 5–10% | минуты | realistic server/client | Gherkin user flows |
| Performance/stress | отдельный pipeline | минуты/часы | controlled benchmark | JMH, 1000 NPC, event flood |

### 3.1 Минимальные quality gates

- Core coverage: `>=80%` line и branch coverage.
- Infrastructure coverage: `>=70%`.
- Критические модули могут иметь более строгий порог: NPC/Colony `>=85%`, Event System `>=85%` согласно их DoD.
- Все unit tests проходят без retries в PR pipeline.
- Нет новых critical/high static analysis findings.
- ArchUnit rules проходят.
- Integration tests запускаются для затронутых boundaries.

## 4. Организация тестового кода

```text
src/test/java/com/rimworldcraft/
  core/
    colony/
    npc/
    story/
    goal/
    building/
    events/
  contract/
    repository/
    ports/
    serialization/
  integration/
    nbt/
    network/
    entity/
    baritone/
  acceptance/
    steps/
    fixtures/

src/test/resources/
  config/valid/
  config/invalid/
  saves/v1/
  saves/v2/
  schemas/
  gherkin/
```

### 4.1 Naming

```text
<ClassName>Test
<ClassName>ContractTest
<ClassName>IntegrationTest
<ClassName>AcceptanceTest
<ClassName>PerformanceTest
```

Методы:

```java
save_ShouldPersistEntity_WhenValid()
findById_ShouldReturnEmpty_WhenMissing()
planner_ShouldRespectMaxDepth_WhenGraphContainsCycle()
```

Для Gherkin step definitions имена отражают бизнес-смысл, а не технический вызов.

## 5. Unit-тестирование Core

### 5.1 Что тестируется unit-тестом

- aggregate/entity behavior;
- value object validation/equality/immutability;
- domain policies и strategies;
- application services с fake/mock ports;
- factories и generators с fixed seed;
- specifications;
- event creation и pending events;
- GOAP graph/planner;
- error mapping на уровне Core.

Minecraft API, NBT, network buffers и реальный filesystem в unit tests не используются.

### 5.2 Test doubles

| Double | Назначение |
|---|---|
| Stub | вернуть фиксированный ответ |
| Fake | простая рабочая in-memory реализация контракта |
| Spy | записать вызовы/события для assertions |
| Mock | проверить collaboration, когда это часть contract |
| Fixture | создать валидное состояние для теста |

Использовать Mockito только для boundary collaboration; domain behavior предпочтительно тестировать с fakes.

### 5.3 Пример `Colony.addResource()`

```java
class ColonyTest {
    @Test
    void addResource_ShouldIncreaseAmount_WhenPositiveAmount() {
        Colony colony = ColonyFixtures.activeWithResource("minecraft:oak_log", 4);

        colony.addResource(ResourceType.of("minecraft:oak_log"), 3,
                ResourceChangeReason.PRODUCTION);

        assertThat(colony.resourceAmount(ResourceType.of("minecraft:oak_log")))
                .isEqualTo(7);
        assertThat(colony.pullDomainEvents())
                .anyMatch(event -> event instanceof ResourceUpdatedEvent);
    }

    @Test
    void addResource_ShouldReject_WhenAmountIsNotPositive() {
        Colony colony = ColonyFixtures.active();

        assertThatThrownBy(() -> colony.addResource(
                ResourceType.of("minecraft:stone"), 0,
                ResourceChangeReason.PRODUCTION))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void removeResource_ShouldReject_WhenAmountExceedsInventory() {
        Colony colony = ColonyFixtures.activeWithResource("minecraft:stone", 1);

        assertThatThrownBy(() -> colony.removeResource(
                ResourceType.of("minecraft:stone"), 2,
                ResourceChangeReason.BUILDING))
                .isInstanceOf(InsufficientResourcesException.class);
    }
}
```

Проверяются не private fields, а observable amount, event и exception.

### 5.4 Aggregate tests

Для каждого aggregate должны быть tests на:

- valid creation;
- invalid creation;
- каждый public behavior method;
- lifecycle transitions;
- boundary values;
- duplicate/replay operations;
- pending events;
- serialization-relevant state;
- concurrent version conflict, если применимо.

## 6. Тестирование bounded contexts

### 6.1 Colony

Проверить:

- membership add/remove;
- resource reservation/commit/release;
- zones и overlap;
- work policy;
- wealth/value calculation;
- `ColonyValueChangedEvent`;
- behavior после `ColonyDestroyed`.

### 6.2 NPC

Проверить:

- need decay и critical threshold;
- mood modifiers;
- skill XP/level caps;
- trait conflicts;
- directed relationships и self-link rejection;
- death terminal state;
- `NPCDeathEvent`, `NPCMoodChangedEvent`, `NPCSkillIncreasedEvent`.

### 6.3 Storyteller

Проверить:

- weighted selection;
- conditions и cooldown;
- deterministic random seed;
- incident/arc lifecycle;
- history logging;
- difficulty scaling;
- event reaction на NPC/Colony facts.

### 6.4 Goal AI

Проверить:

- survival priority floor;
- workaholic/brave/social modifiers;
- GOAP plan construction;
- max depth/timeouts/cycles;
- action preconditions/effects;
- plan interruption/replanning;
- no infinite retry loop.

### 6.5 Building System

Проверить:

- blueprint validation;
- ghost block placement/removal;
- collision detection;
- resource reservation;
- progress monotonicity;
- task assignment/completion/cancellation;
- integration events.

## 7. Contract-тестирование портов и репозиториев

### 7.1 Цель

Contract test фиксирует semantics интерфейса. Каждая реализация (`InMemory`, `NBT`, JSON/database в будущем, Forge/Fabric) запускает один и тот же abstract suite.

### 7.2 Repository contract

```java
abstract class CitizenRepositoryContractTest {
    protected abstract ICitizenRepository repository();
    protected abstract Citizen newCitizen();

    @Test
    void save_ShouldPersistEntity_WhenValid() {
        Citizen citizen = newCitizen();
        repository().save(citizen);

        assertThat(repository().findById(citizen.worldId(), citizen.id()))
                .contains(citizen);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenMissing() {
        assertThat(repository().findById(
                WorldFixtures.id(), CitizenId.newId())).isEmpty();
    }

    @Test
    void delete_ShouldRemoveEntity_WhenPresent() {
        Citizen citizen = newCitizen();
        repository().save(citizen);
        repository().delete(WorldFixtures.id(), citizen.id());

        assertThat(repository().findById(
                citizen.worldId(), citizen.id())).isEmpty();
    }

    @Test
    void find_ShouldRespectWorldScope() {
        Citizen citizen = newCitizen();
        repository().save(citizen);

        assertThat(repository().findById(
                new WorldId("another-world"), citizen.id())).isEmpty();
    }
}
```

### 7.3 Port contracts

Для `IPathfinderPort` проверяются:

- одинаковые start/target semantics;
- unreachable result;
- timeout/cancellation;
- world scope;
- valid waypoint sequence;
- no destructive flags when disabled.

Для `IEventBusPort` проверяются:

- publish/subscription/unsubscription;
- handler isolation;
- duplicate registration;
- ordering;
- retries/dead letters;
- async shutdown.

## 8. Acceptance-тестирование и Cucumber-JVM

### 8.1 Назначение

Acceptance tests описывают behavior языком игрока/дизайнера. Они не должны повторять каждый unit test. Их задача — подтвердить пользовательский сценарий через несколько реальных слоёв.

Инструменты:

- Cucumber-JVM;
- JUnit 5 platform;
- loader-specific test harness для Forge/Fabric;
- test server/client fixtures.

### 8.2 Структура

```text
src/test/resources/features/
  colony.feature
  npc-mood.feature
  building.feature
  storyteller.feature
  multiplayer.feature

src/test/java/com/rimworldcraft/acceptance/
  CucumberTest.java
  steps/ColonySteps.java
  steps/NpcSteps.java
  steps/BuildingSteps.java
  support/TestWorldContext.java
```

Пример JUnit runner:

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "com.rimworldcraft.acceptance.steps")
public class CucumberTest {
}
```

Конкретные аннотации зависят от версии Cucumber-JVM; используемый вариант фиксируется в build configuration.

### 8.3 Gherkin: NPC строит стену

```gherkin
Feature: Building system

  Scenario: NPC builds a wall from a blueprint
    Given a colony with 10 wood
    And an available citizen with building skill 50
    When the player places a ghost block of a wooden wall
    Then a BuildOrder is created with status "PENDING"
    And the citizen is assigned to the task
    When the citizen places the block
    Then the ghost block disappears
    And the BuildOrder status becomes "COMPLETED"
    And the colony wood decreases by 1
```

### 8.4 Step definitions

```java
public final class BuildingSteps {
    private final AcceptanceWorldContext context;

    public BuildingSteps(AcceptanceWorldContext context) {
        this.context = context;
    }

    @Given("a colony with {int} wood")
    public void colonyHasWood(int amount) {
        context.givenColonyWithResource("minecraft:oak_log", amount);
    }

    @When("the player places a ghost block of a wooden wall")
    public void playerPlacesGhostBlock() {
        context.placeGhostBlock("minecraft:oak_planks");
    }

    @Then("the BuildOrder status becomes {string}")
    public void buildOrderStatusIs(String expected) {
        assertThat(context.buildOrder().status().name()).isEqualTo(expected);
    }
}
```

Step definitions не должны содержать production business logic. Они вызывают test facade/application ports.

## 9. Интеграционные тесты

### 9.1 Когда обязательны

Integration test нужен, если затронуты:

- Minecraft API/entity/renderer;
- Forge/Fabric registration;
- network codec/packet handler;
- NBT/save adapter;
- JSON schema/parser;
- Baritone/pathfinding adapter;
- actual event chain;
- thread/lifecycle behavior.

### 9.2 `ForgeEntityAdapter.spawnCitizen()`

```java
class ForgeEntityAdapterIntegrationTest {
    private TestServer server;
    private ForgeEntityAdapter adapter;

    @BeforeEach
    void setUp() {
        server = TestServer.start();
        adapter = new ForgeEntityAdapter(server.context());
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void spawnCitizen_ShouldCreateEntityWithCitizenId() {
        CitizenId citizenId = CitizenId.newId();
        SpawnIntent intent = SpawnFixtures.citizen(citizenId, server.worldId());

        SpawnResult result = adapter.spawn(intent);

        assertThat(result.success()).isTrue();
        EntitySnapshot snapshot = adapter.find(result.entityId()).orElseThrow();
        assertThat(snapshot.citizenId()).isEqualTo(citizenId);
    }
}
```

Integration suite также проверяет despawn idempotency, wrong world, unload/reload и entity registration.

### 9.3 NBT save/load

```java
@Test
void saveLoad_ShouldRestoreCitizenState() {
    Citizen original = CitizenFixtures.complete();
    repository.save(original);

    Citizen restored = repository.findById(original.worldId(), original.id())
            .orElseThrow();

    assertThat(restored.name()).isEqualTo(original.name());
    assertThat(restored.skills()).isEqualTo(original.skills());
    assertThat(restored.needs()).isEqualTo(original.needs());
    assertThat(restored.relationships()).isEqualTo(original.relationships());
}
```

## 10. Тестирование Event System

### 10.1 Unit bus

```java
@Test
void failingHandler_ShouldNotPreventOtherHandlers() {
    List<DomainEvent> received = new ArrayList<>();
    bus.subscribe(NPCDeathEvent.class, event -> {
        throw new EventHandlerException("broken-handler", null);
    });
    bus.subscribe(NPCDeathEvent.class, received::add);

    bus.publish(NpcEventFixtures.death());

    assertThat(received).hasSize(1);
    assertThat(testMetrics.handlerFailures()).isEqualTo(1);
}
```

### 10.2 Event chain integration

Обязательные цепочки:

```text
NPCDeathEvent
  -> Colony membership removal
  -> CitizenLeftEvent
  -> Storyteller mourning trigger
  -> Goal AI task cancellation
```

```text
BuildOrderCompletedEvent
  -> Colony value update
  -> NPC skill XP
  -> Goal AI task completion
  -> Storyteller history fact
```

Проверять event IDs, causation/correlation IDs, ordering и duplicate delivery.

## 11. Performance и stress testing

### 11.1 Цель

Performance tests выявляют деградацию времени server tick, планирования, persistence, event dispatch и network payload. Stress tests проверяют поведение при максимальной realistic нагрузке и отсутствие unbounded memory/thread growth.

### 11.2 Инструменты

- **JMH** — микробенчмарки чистых алгоритмов и сериализации;
- **VisualVM** — базовый profiling heap/CPU/threads;
- **YourKit** — детальный CPU/allocations/lock profiling;
- loader test server — world/entity/tick measurements;
- JaCoCo и CI reports — quality trend.

### 11.3 Пример JMH benchmark

```java
@State(Scope.Benchmark)
public class StorytellerBenchmark {
    private Storyteller storyteller;
    private StorytellerContext context;

    @Setup
    public void setUp() {
        storyteller = StorytellerFixtures.withActiveColonyValue(2000);
        context = StorytellerFixtures.standardContext();
    }

    @Benchmark
    public IncidentSelectionResult storytellerTick() {
        return storyteller.tick(context, new GameTick(24_000));
    }
}
```

JMH benchmark не должен включать server startup. Для end-to-end tick измеряется отдельным integration/stress harness.

### 11.4 Рекомендуемые сценарии и пороги

Пороги являются стартовыми baseline и уточняются после измерения целевой серверной конфигурации:

| Операция | Fixture | Целевой порог |
|---|---|---:|
| Save colony | 100 NPC, 500 inventory entries | `<=1 s` p95 |
| Load colony | тот же snapshot | `<=1 s` p95 |
| Storyteller tick | 100 active incidents candidates | `<=5 ms` p95 вне IO |
| GOAP plan | graph до 500 nodes | `<=5 ms` на request |
| Path request | medium terrain | `<=100 ms` p95 async queue excluded |
| Event dispatch | 1000 events, 10 handlers | `<=50 ms` total in benchmark harness |
| Network state packet | 100 tracked NPC | bounded payload, no tick spike |

Нельзя считать baseline универсальной гарантией для всех machines; изменения порогов фиксируются в performance ADR/report.

### 11.5 Stress scenarios

- 1000 NPC с decision interval;
- 1000 concurrent/queued event deliveries;
- 100 active build orders;
- 100 simultaneous path requests;
- repeated save/load cycles;
- 10+ players tracking same entities;
- event flood from `StorytellerTickedEvent` disabled/sampled;
- corrupted save batch;
- queue saturation and executor rejection.

Проверять:

- no OOM;
- no unbounded queue;
- no deadlocks;
- no starvation of critical events;
- tick remains under server budget;
- cancellation releases resources;
- memory returns after unload;
- failure rate and recovery are observable.

## 12. Тестирование JSON-конфигураций

### 12.1 Schema validation

Все config files проходят JSON Schema validator (например, `json-schema-validator`, если библиотека одобрена проектом) до semantic parsing.

Проверять:

- required fields;
- types;
- enum values;
- ranges;
- ID regex/uniqueness;
- additional properties policy;
- cross-file references;
- version compatibility.

### 12.2 Parser unit tests

```java
@Test
void traitsConfig_ShouldParseValidFixture() {
    TraitsDocument document = parser.parse(Resources.read("config/valid/traits.json"));

    assertThat(document.version()).isEqualTo(1);
    assertThat(document.traits()).extracting(TraitDefinition::id)
            .contains("rwc:optimist");
}

@Test
void config_ShouldRejectDuplicateIds() {
    String json = Resources.read("config/invalid/traits-duplicate-id.json");

    assertThatThrownBy(() -> parser.parse(json))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("duplicate id");
}
```

### 12.3 Mutation testing конфигурации

При изменении `story-events.json`:

1. проверить schema;
2. загрузить snapshot;
3. создать `Storyteller` с fixed state/seed;
4. выполнить selection;
5. проверить, что новый weight влияет ожидаемым образом;
6. проверить, что zero/negative weight не делает event некорректным.

См. также `configuration-mutation-testing.md`, если он будет выделен в отдельный документ.

## 13. Тестирование миграций данных

### 13.1 Правило fixtures

Старые NBT/save files хранятся в:

```text
src/test/resources/saves/v1/
src/test/resources/saves/v2/
```

Каждая schema change добавляет fixture старой версии и migration test.

### 13.2 Пример

```java
@Test
void version1Colony_ShouldMigrateToCurrentSchema() {
    SaveDocument oldDocument = SaveFixtures.load("saves/v1/colony-basic.nbt");

    SaveDocument migrated = migrationRegistry.migrateToCurrent(oldDocument);
    Colony restored = colonyMapper.fromDocument(migrated);

    assertThat(migrated.schemaVersion()).isEqualTo(CURRENT_SCHEMA);
    assertThat(restored.name()).isNotBlank();
    assertThat(restored.members()).isNotNull();
    assertThat(restored.inventory()).allSatisfy(
            resource -> assertThat(resource.amount()).isGreaterThanOrEqualTo(0));
}
```

Проверять:

- migration idempotency;
- missing optional defaults;
- old enum values;
- renamed fields;
- invalid references;
- future version rejection;
- backup/rollback behavior.

## 14. Ручное тестирование

### 14.1 Когда обязательно

- новая user-facing feature;
- UI/HUD/renderer change;
- multiplayer behavior;
- loader registration;
- visual/animation changes;
- performance-sensitive change;
- migration/recovery flow, который сложно полностью автоматизировать.

### 14.2 Чек-лист ручного тестирования

```markdown
## Manual Test Report
- Build/mod version:
- Loader/version:
- Java version:
- World type/seed:
- Server/client setup:

### Scenario
- [ ] Clean world
- [ ] Existing save
- [ ] Reload config
- [ ] Disconnect/reconnect
- [ ] Multiplayer with second player
- [ ] Expected UI/renderer state
- [ ] Error/fallback state
- [ ] No server log errors
- [ ] No visible desync

### Result
- Expected:
- Actual:
- Evidence:
```

### 14.3 Multiplayer checklist

- server state remains authoritative;
- packets serialize/deserialize correctly;
- unauthorized player commands rejected;
- stale revisions ignored;
- entity spawn/despawn synchronized;
- no duplicate event side effects after reconnect;
- two players see consistent colony/NPC state;
- save while clients connected does not corrupt state.

## 15. CI/CD automation

### 15.1 Каждый Pull Request

```text
compile
format/checkstyle
unit tests
JaCoCo coverage
SpotBugs
ArchUnit
JSON schema validation
fast repository/port contract tests
selected integration tests for changed modules
```

### 15.2 Release Candidate

```text
full unit suite
all contract tests
Forge integration suite
Fabric integration suite
NBT/save migration suite
Cucumber acceptance suite
network/multiplayer smoke tests
performance baseline
```

### 15.3 Nightly

```text
stress tests
1000 NPC scenario
long-running event bus stability
repeated save/load
all migration versions
Baritone/test-world matrix
memory/thread leak checks
full performance trend
```

### 15.4 Gradle task convention

Названия задач должны быть стабильными и документированными:

```text
./gradlew test
./gradlew unitTest
./gradlew integrationTest
./gradlew contractTest
./gradlew acceptanceTest
./gradlew performanceTest
./gradlew jacocoTestReport
./gradlew architectureTest
./gradlew configTest
./gradlew check
```

Не запускать длительные stress/performance suites на каждом PR, если они не затронуты, но release/nightly policy обязательна.

## 16. Тестирование multiplayer и network

### 16.1 Packet tests

Для каждого packet:

- encode → decode round-trip;
- invalid/truncated buffer;
- oversized strings/lists/maps;
- unknown enum/version;
- wrong actor/citizen ID;
- wrong world/dimension;
- stale revision;
- replay `commandId`;
- server thread handoff.

```java
@Test
void packetRoundTrip_ShouldPreserveRevisionAndIds() {
    CitizenStateS2CPacket original = PacketFixtures.state();
    FriendlyByteBuf buffer = TestBuffers.create();

    original.encode(buffer);
    CitizenStateS2CPacket restored = CitizenStateS2CPacket.decode(buffer);

    assertThat(restored).isEqualTo(original);
}
```

### 16.2 Bot/server tests

Использовать test server с несколькими scripted clients/bots:

- two players issue simultaneous build commands;
- one player sends unauthorized NPC action;
- reconnect during state update;
- delayed/out-of-order packet;
- entity tracking range change;
- server save with active clients.

Собирать server logs, packet metrics и final state assertions.

## 17. Обработка ошибок в тестах

### 17.1 Диагностика падения

При падении теста сохранять:

- seed;
- fixture/config version;
- world ID и tick;
- correlation ID;
- packet/event IDs;
- relevant logs;
- thread dump для timeout/deadlock;
- screenshot/replay для acceptance.

### 17.2 `@Tag`

```java
@Tag("slow")
class BaritoneWorldIntegrationTest { }

@Tag("stress")
class ThousandNpcStressTest { }

@Tag("multiplayer")
class MultiplayerSynchronizationTest { }
```

Рекомендуемые tags:

```text
unit, integration, contract, acceptance, slow, stress,
multiplayer, migration, performance, flaky-quarantine
```

### 17.3 Retry policy

Retry не должен маскировать flaky behavior.

- unit tests: retry запрещён;
- integration: максимум один diagnostic rerun локально, CI фиксирует first failure;
- external/test-server startup: bounded retry допустим;
- flaky test получает issue, owner и deadline;
- quarantined test не считается quality pass и не может оставаться без срока.

## 18. Метрики и отчёты

### 18.1 Обязательные метрики

- passed/failed/skipped tests;
- duration per suite/test class;
- Core/Infrastructure coverage;
- mutation score критических policies;
- flaky rate и retry count;
- defect count по уровню теста;
- migration compatibility pass rate;
- performance p50/p95/p99;
- event queue depth/failures;
- server tick impact;
- memory allocation/leak indicators.

### 18.2 Отчёты

CI публикует:

- JaCoCo HTML/XML;
- JUnit XML;
- Cucumber report;
- Checkstyle/SpotBugs/ArchUnit result;
- performance benchmark JSON/CSV;
- migration compatibility matrix;
- test server logs/artifacts при failure.

Trend review проводится регулярно. Рост времени тестов более чем на agreed threshold (например, 20%) требует investigation.

## 19. Связь с Definition of Done

| DoD-критерий | Как подтверждается |
|---|---|
| Core coverage >=80% | JaCoCo CI gate |
| Infrastructure >=70% | JaCoCo CI gate |
| Unit tests pass | JUnit task |
| Integration tests pass | selected/full integration task |
| Acceptance tests | Cucumber report + review |
| ArchUnit | architecture test task |
| JSON valid | schema/config test |
| Migration safe | migration fixture suite |
| Multiplayer correct | packet/server acceptance suite |
| Performance acceptable | JMH/server benchmark report |
| Error handling | negative tests + logs |
| Documentation updated | PR review/checklist |

См. [`definition-of-done-do-d.md`](definition-of-done-do-d.md) для полного PR checklist.

## 20. Практические шаблоны сценариев

### 20.1 Storyteller performance test

```java
@Tag("performance")
class StorytellerPerformanceTest {
    @Test
    void tick_ShouldCompleteWithinBudget_ForLargeColony() {
        Storyteller storyteller = StorytellerFixtures.largeScenario(
                1000, 100, 50);
        Stopwatch stopwatch = Stopwatch.createStarted();

        storyteller.tick(StorytellerFixtures.context(), new GameTick(24000));

        stopwatch.stop();
        assertThat(stopwatch.elapsed(TimeUnit.MILLISECONDS))
                .isLessThan(50);
    }
}
```

Для стабильных результатов предпочтителен JMH; обычный threshold test используется как smoke guard в integration pipeline.

### 20.2 Goal AI priority test

```java
@Test
void hungryCitizen_ShouldChooseEatOverBuild() {
    WorldState state = WorldStateFixtures.builder()
            .hunger(20)
            .assignedBuildTask(true)
            .build();

    Goal selected = evaluator.evaluate(state);

    assertThat(selected.type()).isEqualTo(GoalType.EAT);
}
```

### 20.3 Building acceptance

```gherkin
Feature: Building from a prefab

  Scenario: Construction consumes exactly the required resource
    Given a colony has 10 oak logs
    And prefab "oak_wall" requires 1 oak log
    And citizen "alex" has building skill 50
    When the player places the prefab at (10, 64, 10)
    And the construction task completes
    Then the colony has 9 oak logs
    And BuildOrderCompletedEvent is published once
    And citizen "alex" receives building experience
```

## 21. FAQ

### Нужно ли тестировать Minecraft API unit-тестами?

Нет. Minecraft/Forge/Fabric behavior проверяется integration/contract tests. Core unit tests используют ports и fakes.

### Можно ли поднять coverage искусственными тестами getters?

Это не цель. Coverage gate должен сопровождаться тестами инвариантов, failure paths и mutation tests для критических правил.

### Можно ли retry flaky test до зелёного результата?

Нет для unit. Для внешнего test server ограниченный retry допускается, но first failure сохраняется и нестабильность расследуется.

### Когда писать Gherkin?

Для ключевого пользовательского сценария, особенно если задействованы несколько контекстов, UI, сеть или реальный мир. Не нужно дублировать каждый внутренний метод.

### Как тестировать случайность?

Инъецировать `RandomPort`, фиксировать seed и проверять deterministic outcome. Дополнительно использовать property/statistical tests для weighted selection.

### Как тестировать async event bus?

Использовать deterministic executor/test clock, ожидание с bounded timeout, проверку ordering, cancellation, queue saturation и graceful shutdown.

### Что делать, если test world слишком медленный для PR?

Оставить быстрый smoke integration suite на PR, полный matrix — на release/nightly. Изменения boundary обязаны запускать соответствующий targeted integration test.

### Нужно ли сохранять старые NBT fixtures навсегда?

Все поддерживаемые schema versions — да. Удаление fixtures возможно только вместе с официальным прекращением поддержки версии и документированной migration policy.

## 22. План улучшения тестирования

### Краткосрочно

- стабилизировать unit/contract suite;
- добавить все missing migration fixtures;
- внедрить Cucumber-JVM smoke scenarios;
- включить packet fuzz/bounds tests;
- настроить JaCoCo, ArchUnit и JSON validation в CI.

### Среднесрочно

- добавить mutation testing для aggregates/planners;
- расширить Forge/Fabric contract matrix;
- автоматизировать multiplayer bot scenarios;
- ввести performance baseline dashboards;
- улучшить test data builders и deterministic replay.

### Долгосрочно

- property-based testing для planner/event ordering;
- длительные stability tests с save/load cycles;
- automated visual regression для renderer/UI;
- fault injection для event bus, persistence и network;
- профилирование реальных modpacks/world sizes;
- coverage ownership per bounded context;
- автоматическая проверка config mutations в pull request.

## 23. Связи с другими документами

- [`definition-of-done-do-d.md`](definition-of-done-do-d.md) — критерии готовности задач.
- [`archunit-rules.md`](archunit-rules.md) — архитектурные проверки.
- [`codestyle-and-solidd.md`](codestyle-and-solidd.md) — style/SOLID и test naming.
- `drift-detection-pipeline.md` — автоматизация CI и контроль архитектурного дрейфа.
- `configuration-mutation-testing.md` — mutation testing JSON-конфигураций.
- [`ddd-tactical-patterns.md`](ddd-tactical-patterns.md) — тестирование Entity, Value Object, Aggregate, Factory, Repository и Events.
- [`event-system-api.md`](event-system-api.md) — event bus, handlers, retries и event store.
- [`data-interfaces.md`](data-interfaces.md) — repository contract tests.
- [`save-serialization.md`](save-serialization.md) — NBT schemas и migration tests.
- [`module-npc-core.md`](module-npc-core.md) — NPC tests.
- [`module-goal-ai.md`](module-goal-ai.md) — planner/executor tests.
- [`module-building-system.md`](module-building-system.md) — construction tests.
- [`entity-integration.md`](entity-integration.md) — entity/network integration.
- [`pathfinding-layer.md`](pathfinding-layer.md) — Baritone/pathfinding tests.

## 24. Финальный checklist тестировщика

- [ ] Понимаю тип и acceptance criteria задачи.
- [ ] Есть unit tests для изменённой Core logic.
- [ ] Есть negative/error tests.
- [ ] Изменённые ports/adapters покрыты contract/integration tests.
- [ ] Config/schema changes проверены.
- [ ] Save schema changes покрыты migrations.
- [ ] Event chains и idempotency проверены.
- [ ] Multiplayer/network проверены, если применимо.
- [ ] Performance/stress scope определён.
- [ ] Manual test report приложен для user-facing changes.
- [ ] CI reports зелёные.
- [ ] Flaky test не скрыт retry.
- [ ] Evidence и логи доступны ревьюверу.

## 25. Итог

Тестовая стратегия RimWorldCraft строится вокруг изолируемого Core, явных портов и реалистичных integration boundaries. Unit-тесты защищают доменные инварианты, contract tests фиксируют semantics adapters, integration tests проверяют Minecraft/NBT/network, Cucumber подтверждает пользовательские сценарии, а JMH и stress suites защищают server performance.

Задача считается качественно проверенной только тогда, когда тесты покрывают не один happy path, а также ошибки, границы, миграции, повторную доставку, многопользовательские ограничения и эксплуатационные риски.
