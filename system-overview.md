# RimWorldCraft — System Overview

## 1. Цель документа

Документ описывает целевую архитектуру мода **RimWorldCraft** — мода для Minecraft Java Edition, объединяющего симуляцию колонии в духе RimWorld с экшеном от первого лица.

Он предназначен для:

- новых разработчиков, которым нужно быстро понять устройство системы;
- архитекторов и технических лидов, принимающих решения о границах модулей;
- разработчиков Forge/Fabric-адаптеров;
- QA-инженеров, определяющих стратегию тестирования;
- авторов конфигураций и контента.

Документ фиксирует границы домена, правила зависимостей, основные сценарии и критерии готовности. Детали реализации должны соответствовать ему либо сопровождаться ADR.

## 2. Контекст системы

RimWorldCraft работает внутри Minecraft Java Edition. Minecraft предоставляет мир, тики, сущности, рендеринг, ввод, сохранения и сетевой транспорт. Мод не должен помещать бизнес-правила в Minecraft API: Minecraft выступает внешней платформой, подключённой через адаптеры.

### C4 Context Diagram

```plantuml
@startuml
left to right direction
actor "Player" as player
system "RimWorldCraft" as rim
system "Minecraft Java Edition" as mc
actor "Server Operator" as operator
actor "Content Author" as author

player --> rim : управление колонией\nи FPS-действия
rim --> mc : world/entity/render/input/network APIs
mc --> rim : ticks, события мира, packets
operator --> rim : server config, diagnostics
 author --> rim : JSON content packs
@enduml
```

### Границы ответственности

**Minecraft** отвечает за:

- загрузку модов и жизненный цикл клиента/сервера;
- физический и блочный мир;
- отображение, ввод и звук;
- базовые сущности и сетевую доставку;
- файловые ресурсы и стандартные сохранения.

**RimWorldCraft** отвечает за:

- правила колонии, NPC, истории и рейдов;
- доменную модель и инварианты;
- планирование и выполнение задач;
- интерпретацию JSON-конфигурации;
- синхронизацию доменного состояния между сервером и клиентом;
- проекцию доменного состояния в Minecraft.

## 3. Контейнеры

| Контейнер | Назначение | Основные технологии | Зависимости |
|---|---|---|---|
| **Client** | UI, HUD, камера, визуальные эффекты, локальный ввод и client-side prediction | Java 17, Minecraft client API, Forge/Fabric client hooks | Application ports, DTO/projections |
| **Core** | Независимое от Minecraft доменное ядро и application use cases | Java 17, DDD, SOLID, immutable value objects, domain events | Только JDK и собственные контракты |
| **Infrastructure** | Реализация портов: мир, сущности, сеть, persistence, время, random, логирование | Java 17, Forge/Fabric API, serializers, event bus | Core ports, Minecraft API |
| **Configurator** | Загрузка, валидация, версионирование и публикация JSON-конфигурации | Java 17, Jackson/Gson согласно build setup, JSON Schema | Core configuration ports |

### Направление зависимостей

```text
Minecraft API -> Infrastructure -> Core ports
Minecraft API -> Client -> Application ports
Configurator -> Core configuration ports
Core -> JDK only
```

`Core` не знает о `Client`, `Infrastructure`, `Configurator`, Forge, Fabric, `net.minecraft`, registries или Minecraft lifecycle.

## 4. Шестиугольная архитектура

Core является центром приложения. Входящие адаптеры вызывают application ports, исходящие порты реализуются инфраструктурой. Domain events публикуются через абстракцию событийной шины.

### Основные входящие порты

| Порт | Назначение | Реализация-адаптер |
|---|---|---|
| `CreateColonyUseCase` | создание колонии и старт сценария | `ColonyCommandAdapter` для network packet/GUI |
| `AssignWorkUseCase` | назначение работы NPC | `ColonyScreenHandlerAdapter`, packet handler |
| `AdvanceSimulationUseCase` | обработка серверного simulation tick | `MinecraftServerTickAdapter` |
| `HandlePlayerActionUseCase` | применение FPS-действия игрока | `MinecraftInputPacketAdapter` |
| `QueryColonyView` | получение read model для UI | `ColonyHudAdapter`, client query gateway |

### Основные исходящие порты

| Порт | Назначение | Реализация-адаптер |
|---|---|---|
| `WorldPort` | чтение/изменение блоков, поиск позиций и опасностей | `MinecraftWorldAdapter` |
| `NpcRepository` | загрузка и сохранение NPC | `MinecraftNpcRepositoryAdapter` |
| `ColonyRepository` | persistence агрегатов колонии | `SavedDataColonyRepository` |
| `EventBusPort` | публикация доменных событий | `MinecraftEventBusAdapter` или `InMemoryEventBusAdapter` |
| `ClockPort` | детерминированное игровое время | `MinecraftTickClockAdapter` |
| `RandomPort` | контролируемая случайность | `SeededRandomAdapter` |
| `ConfigurationPort` | получение валидированного контента | `JsonConfigurationAdapter` |
| `NotificationPort` | сообщения игроку и системные уведомления | `MinecraftNotificationAdapter` |
| `NetworkPort` | доставка read models и команд | `ForgeNetworkAdapter` / `FabricNetworkAdapter` |

### Правила портов

1. Интерфейсы портов принадлежат Core или application boundary.
2. DTO портов не содержат Minecraft-типы.
3. Адаптер преобразует Minecraft objects в доменные value objects и обратно.
4. Доменная логика не вызывается напрямую из render thread, если операция изменяет состояние.
5. Все изменения состояния выполняются на authoritative server side.
6. Forge и Fabric имеют отдельные адаптеры; Core остаётся общим.

## 5. Ограниченные контексты

Пакеты приведены относительно `com.rimworldcraft`.

### Colony Context

- **Пакет:** `core.colony`
- **Агрегат:** `Colony`
- **Ответственность:** население, ресурсы, зоны, рабочие приоритеты, настроение колонии, основание и разрушение.
- **События:** `ColonyFounded`, `ColonistJoined`, `WorkAssigned`, `ResourceConsumed`, `ColonyMoraleChanged`, `ColonyDestroyed`.
- **Публичный контракт:** принимает команды и публикует факты; не управляет Minecraft entity напрямую.

### NPC Context

- **Пакет:** `core.npc`
- **Агрегат:** `Npc`
- **Ответственность:** потребности, навыки, traits, здоровье, job lifecycle и локальное принятие решений.
- **События:** `NpcSpawned`, `NeedThresholdReached`, `JobStarted`, `JobCompleted`, `NpcIncapacitated`, `NpcDied`.
- **Интеграция:** получает задания от Colony, запрашивает World facts через порт.

### Storyteller Context

- **Пакет:** `core.storyteller`
- **Агрегат:** `Storyteller`
- **Ответственность:** pacing, threat budget, адаптивная сложность, storyteller incidents, cooldowns и eligibility rules.
- **JVM boundary:** `StorytellerApplicationService`, immutable incident definitions, deterministic weighted selection, spawn-entry intent, postponement/retry result, outcome idempotency, and `StorytellerSnapshotMapper`.
- **События:** `IncidentScheduled`, `RaidGenerated`, `FireStarted`, `RewardGranted`.
- **Интеграция:** читает `StorytellerColonySummary`, `StorytellerPopulationSummary` и `WorldSnapshot` через query ports и не изменяет чужие агрегаты напрямую.

### World Context

- **Пакет:** `core.world`
- **Агрегат:** `WorldRegion` / immutable `WorldSnapshot`
- **Ответственность:** доступность клеток, terrain/climate/resource/hazard facts, spawn candidates и settlement validation.
- **JVM boundary:** `WorldSnapshotPort`, `WorldContextService`, immutable facts, world-scope checks, and typed settlement validation reasons.
- **События:** `RegionDiscovered`, `ResourceLocated`, `ThreatDetected`, `WeatherChanged`.
- **Интеграция:** Minecraft-адаптер в будущем поставляет facts; текущий Core хранит только необходимые абстрактные данные.

### Player Context

- **Пакет:** `core.player`
- **Агрегат:** `PlayerProfile`
- **Ответственность:** выбранная колония, разрешения, режим управления, прогресс и персональные настройки.
- **События:** `PlayerJoinedColony`, `PlayerLeftColony`, `CommandIssued`, `MilestoneReached`.
- **Интеграция:** команды игрока проходят application layer и проверяют permissions.

### Правила взаимодействия контекстов

- Context общаются через application contracts и domain events, а не через прямой доступ к агрегатам друг друга.
- У каждого контекста собственный язык и инварианты.
- `Colony` является владельцем колониальных ресурсов; `Npc` не изменяет их напрямую.
- `Storyteller` инициирует incidents, но их применение выполняется владельцами соответствующих агрегатов.
- События должны быть версионируемыми и содержать `eventId`, `occurredAt`, `worldId` и `schemaVersion`.

## 6. Ключевые сценарии использования

### 6.1 Создание колонии

**Предусловия:** игрок авторизован в мире, выбранная стартовая позиция доступна.

```text
Player -> Client: нажимает "Found colony"
Client -> Server: CreateColonyCommand(playerId, position, name)
Server adapter -> CreateColonyUseCase: execute(command)
UseCase -> WorldPort: validatePosition(position)
WorldPort --> UseCase: valid terrain and climate
UseCase -> ColonyRepository: save(new Colony(...))
UseCase -> EventBusPort: publish(ColonyFounded)
EventBusPort -> NpcRepository: create starter colonists
EventBusPort -> NetworkPort: publish ColonyView
NetworkPort --> Client: colony created
Client --> Player: отображает HUD и стартовые задачи
```

**Инварианты:** имя уникально в пределах мира, позиция валидна, колония создаётся атомарно, повторная команда идемпотентна по `commandId`.

### 6.2 Выполнение задачи NPC

```text
MinecraftServerTickAdapter -> AdvanceSimulationUseCase: tick(delta)
UseCase -> ColonyRepository: load active colonies
UseCase -> NpcRepository: load active NPCs
UseCase -> Npc: evaluateNeedsAndJob()
Npc -> WorldPort: findPath(origin, target)
WorldPort --> Npc: path or failure
Npc -> EventBusPort: publish(JobStarted)
Infrastructure adapter: moves Minecraft entity toward target
Npc -> EventBusPort: publish(JobCompleted)
EventBusPort -> Colony: apply resource/production result
Colony -> ColonyRepository: save()
```

NPC не телепортируется доменной логикой: домен выдаёт намерение, а адаптер отвечает за безопасное исполнение в Minecraft.

### 6.3 Генерация рейда

```text
ServerTick -> Storyteller: evaluate(tick, snapshot)
Storyteller -> ConfigurationPort: load incident definitions
Storyteller -> RandomPort: roll threat parameters
Storyteller -> WorldPort: find valid raid entry points
Storyteller -> EventBusPort: publish(RaidGenerated)
EventBusPort -> Colony: mark threat and update mood
EventBusPort -> NetworkPort: notify players
Infrastructure -> Minecraft: spawn hostile entities
```

**Правила:** incident проходит cooldown и eligibility checks; difficulty учитывает wealth, population, recent damage и storyteller curve; при отсутствии валидной точки рейд откладывается, а не создаётся в некорректной позиции.

### 6.4 Обработка FPS-действия игрока

```text
Player -> Client: attack/use/interact
Client -> NetworkPort: PlayerActionCommand
Network adapter -> HandlePlayerActionUseCase: execute(command)
UseCase -> PlayerProfile: check permissions and cooldown
UseCase -> WorldPort: validate target and line of sight
UseCase -> Npc/Colony: apply domain effect
UseCase -> EventBusPort: publish action event
NetworkPort --> Client: authoritative result
```

Клиент может показывать предварительную анимацию, но окончательный результат определяет сервер.

## 7. Конфигурация

Контент хранится в JSON и загружается через `ConfigurationPort`. Конфигурации разделяются по bounded context и имеют версию схемы.

### Организация файлов

```text
config/rimworldcraft/
  schema-version.json
  colony/
    starting-packages.json
    work-types.json
  npc/
    traits.json
    needs.json
    jobs.json
  storyteller/
    incidents.json
    difficulty-curves.json
  world/
    climates.json
    resources.json
  localization/
    ru_ru.json
```

Конфигурация должна быть immutable после публикации snapshot. Ошибки схемы фиксируются при загрузке с понятным путём (`storyteller.incidents[0].weight`), а некорректный content pack не должен частично менять работающую игру.

### Пример trait

```json
{
  "schemaVersion": 1,
  "traits": [
    {
      "id": "optimist",
      "displayKey": "trait.rimworldcraft.optimist",
      "effects": {
        "moodOffset": 6,
        "restRecoveryMultiplier": 1.05
      },
      "conflicts": ["pessimist"],
      "tags": ["personality", "social"]
    }
  ]
}
```

### Пример incident

```json
{
  "schemaVersion": 1,
  "incidents": [
    {
      "id": "raider_wave",
      "type": "RAID",
      "weight": 1.0,
      "cooldownTicks": 24000,
      "conditions": {
        "minColonists": 2,
        "minDaysSinceLastRaid": 3,
        "minThreatPoints": 80
      },
      "scaling": {
        "basePoints": 80,
        "pointsPerColonist": 35,
        "wealthFactor": 0.0002
      },
      "factionPool": ["outlander", "tribal"]
    }
  ]
}
```

Идентификаторы стабильны и используются в save data. Переименование отображаемого текста выполняется через localization keys, а не через изменение `id`.

## 8. Требования к тестированию

### Пирамида тестов

| Уровень | Доля | Что проверяет | Среда |
|---|---:|---|---|
| Unit | 70–80% | агрегаты, value objects, policies, use cases, invariants | чистая JVM, fake ports |
| Integration | 15–25% | repositories, JSON schema, event routing, network serialization, Minecraft adapter contracts | test fixtures, embedded/test Minecraft harness |
| Acceptance | 5–10% | бизнес-сценарии и пользовательские правила | Gherkin, dedicated test world |

### Unit-требования

- тестировать доменные правила без Minecraft runtime;
- использовать fake `ClockPort` и deterministic `RandomPort`;
- проверять позитивные и отрицательные инварианты;
- один тест должен проверять одну observable behavior;
- mutation testing рекомендуется для критических policies.

### Integration-требования

- проверять round-trip JSON: load → validate → domain snapshot;
- проверять сохранение и восстановление агрегата;
- проверять совместимость packet DTO;
- проверять, что адаптер корректно переводит Minecraft error в доменную ошибку;
- запускать Forge и Fabric adapter suites отдельно.

### Пример Gherkin

```gherkin
Feature: Founding a colony

  Scenario: Player founds a colony on valid terrain
    Given player "alex" has no colony in world "frontier"
    And position "100,64,200" is valid for settlement
    When player "alex" founds colony "New Dawn" at "100,64,200"
    Then colony "New Dawn" is created in world "frontier"
    And event "ColonyFounded" is published exactly once
    And starter resources are assigned to the colony
    And the player sees the colony HUD

  Scenario: Player cannot found a duplicate colony
    Given player "alex" already owns colony "New Dawn"
    When player "alex" founds colony "Second Dawn" at "100,64,200"
    Then the command is rejected with code "PLAYER_ALREADY_HAS_COLONY"
    And no new colony is persisted
```

## 9. Определение готовности (DoD)

Любой новый модуль считается готовым, если выполнены все пункты:

- [ ] ответственность и bounded context модуля документированы;
- [ ] доменная логика не зависит от Minecraft API;
- [ ] публичные контракты и порты определены;
- [ ] инварианты покрыты unit-тестами;
- [ ] интеграционные тесты покрывают адаптеры, persistence или JSON, если они затронуты;
- [ ] для пользовательского поведения добавлен acceptance/Gherkin-сценарий;
- [ ] JSON-схемы и пример конфигурации обновлены при изменении контента;
- [ ] ArchUnit-проверки проходят в CI;
- [ ] сервер является authoritative source of truth и сетевые ошибки обработаны;
- [ ] ADR или обновление существующей документации добавлены для архитектурных решений.

## 10. ArchUnit-правила

Правила выполняются в CI на каждом pull request. Ниже приведён концептуальный набор для Java/ArchUnit.

### Запрет Minecraft API в Core

```java
noClasses()
    .that().resideInAnyPackage("..core..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("net.minecraft..", "net.minecraftforge..", "net.fabricmc..");
```

### Core не зависит от Infrastructure и Client

```java
noClasses()
    .that().resideInAnyPackage("..core..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..infrastructure..", "..client..");
```

### Адаптеры зависят от портов, но не наоборот

```java
classes().that().resideInAnyPackage("..infrastructure..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("java..", "..core..", "net.minecraft..", "net.minecraftforge..", "net.fabricmc..");
```

### Domain не зависит от Application

```java
noClasses()
    .that().resideInAnyPackage("..core..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..core..application..");
```

### Bounded contexts не обращаются к внутренностям друг друга

```java
noClasses()
    .that().resideInAnyPackage("..core.colony..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..core.npc.internal..", "..core.storyteller.internal..", "..core.world.internal..");
```

### Application service не создаёт Minecraft objects

```java
noClasses()
    .that().resideInAnyPackage("..core..application..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("net.minecraft.entity..", "net.minecraft.world..");
```

### Порты должны быть интерфейсами

```java
classes().that().resideInAnyPackage("..core..port..")
    .should().beInterfaces();
```

### Domain events не зависят от адаптеров

```java
noClasses()
    .that().resideInAnyPackage("..core..event..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..infrastructure..", "..client..");
```

Нарушение правила требует либо исправления зависимости, либо отдельного ADR с явным исключением. Исключения не должны скрывать циклические зависимости.

## 11. Связи с другими документами

| Документ | Детализируемые разделы |
|---|---|
| `bounded-contexts.md` | границы Colony, NPC, Storyteller, World, Player, ownership агрегатов |
| `hexagonal-architecture.md` | порты, адаптеры, dependency direction и application layer |
| `domain-model.md` | агрегаты, entities, value objects, инварианты |
| `use-cases.md` | команды, queries, sequence diagrams и ошибки |
| `events-and-messaging.md` | event envelopes, delivery guarantees, idempotency, versioning |
| `minecraft-adapters.md` | Forge/Fabric lifecycle, tick, entity, world и network adapters |
| `configuration-reference.md` | JSON schemas, content packs, migrations и validation errors |
| `persistence.md` | save format, snapshots, compatibility и recovery |
| `testing-strategy.md` | fixtures, test worlds, contract tests, CI matrix |
| `archunit-rules.md` | полный набор architectural tests и политика исключений |
| `adr/` | принятые и заменённые архитектурные решения |
| `security-and-multiplayer.md` | authoritative server, permissions, packet validation и trust boundaries |
| `observability.md` | logging, metrics, diagnostics и incident tracing |

## 12. Принятые архитектурные решения (ADR)

### ADR-001: Core полностью отделён от Minecraft API

**Статус:** Accepted

**Решение:** domain и application code компилируются против собственных интерфейсов и JDK, а интеграция с Forge/Fabric выполняется адаптерами.

**Обоснование:** это позволяет тестировать правила на обычной JVM, поддерживать Forge и Fabric без дублирования домена, уменьшить стоимость обновления Minecraft и контролировать архитектурный дрейф ArchUnit-проверками.

**Последствия:** потребуется явное преобразование DTO и объектов мира; адаптеры становятся существенной частью системы.

### ADR-002: JSON как формат конфигурации контента

**Статус:** Accepted

**Решение:** traits, jobs, incidents, scaling rules и стартовые наборы хранятся в версионируемых JSON-файлах и проходят schema validation до публикации snapshot.

**Обоснование:** JSON удобен для модпаков, diff в Git, внешнего редактирования и загрузки без перекомпиляции. Версия схемы позволяет выполнять миграции.

**Последствия:** нужны строгая валидация, диагностика ошибок и политика совместимости; произвольный код в конфигурации запрещён.

### ADR-003: Domain events через абстрактную событийную шину

**Статус:** Accepted

**Решение:** агрегаты публикуют domain events через `EventBusPort`; конкретная доставка выбирается адаптером. События являются фактами, а не командами.

**Обоснование:** контексты слабо связаны, проще добавлять реакции Storyteller, UI, persistence и analytics, а также тестировать сценарии через in-memory bus.

**Последствия:** требуется идемпотентность обработчиков, порядок событий в пределах агрегата и versioning envelope. Событийная шина не заменяет транзакционную границу агрегата.

## 13. Архитектурные инварианты проекта

- Minecraft server — источник истины для multiplayer-состояния.
- Core не знает о thread model Minecraft; адаптеры обеспечивают выполнение на правильном потоке.
- Агрегаты изменяются только через команды и application services.
- Случайность, время и окружение инъецируются через порты.
- Конфигурация не может обходить доменные инварианты.
- Сетевые входные данные считаются недоверенными.
- Любая архитектурно значимая зависимость должна быть отражена в документации и проверена CI.

## 14. Минимальный путь нового разработчика

1. Прочитать этот документ и `bounded-contexts.md`.
2. Найти нужный use case в `core/<context>/application`.
3. Изучить его входной порт и используемые исходящие порты.
4. Проверить доменные события и агрегатные инварианты.
5. Найти Forge/Fabric реализацию порта в `infrastructure`.
6. Добавить unit-тест без Minecraft, затем integration/acceptance-тест по необходимости.
7. Запустить ArchUnit и полный CI-набор перед созданием pull request.
