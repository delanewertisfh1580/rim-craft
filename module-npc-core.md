# RimWorldCraft — NPC Core

## 1. Введение

`NPC Core` — bounded context, отвечающий за индивидуальную жизнь, состояние и поведение неигровых персонажей RimWorldCraft. Он поддерживает колонистов, врагов, торговцев и другие типы NPC, но не является владельцем Minecraft-сущностей: Minecraft entity — техническая проекция доменного `Citizen` или другого NPC.

Модуль реализуется внутри `core.npc` и следует правилам из [`system-overview.md`](system-overview.md), [`bounded-contexts.md`](bounded-contexts.md), [`hexagonal-architecture.md`](hexagonal-architecture.md), [`data-dictionaries.md`](data-dictionaries.md) и [`module-colony-manager.md`](module-colony-manager.md), если этот документ будет добавлен под таким именем.

### 1.1 Ответственность

NPC Core отвечает за:

- генерацию идентичности NPC;
- жизненный цикл и terminal state;
- черты характера, навыки и опыт;
- потребности и настроение;
- социальные отношения;
- назначение и завершение задач;
- публикацию NPC domain events;
- сохранение/восстановление доменного состояния.

### 1.2 Что находится вне модуля

NPC Core **не отвечает** за:

- Minecraft `Entity`, `ServerLevel`, `BlockPos`, `ItemStack` или NBT API;
- владение запасами колонии;
- общую work policy и colony membership ownership;
- выбор storyteller incidents;
- окончательную физику движения и pathfinding implementation;
- authentication игрока;
- rendering, HUD и network packet format.

Внешнее окружение подключается через `IEntitySpawnPort`, `IPathfinderPort`, `ITimePort`, `ISaveLoadPort`, `IBlockWorldPort`, `IInventoryPort`, `IEventBusPort` и `IConfigPort`.

### 1.3 Главный принцип владения

`Citizen` владеет индивидуальными needs, skills, traits, mood, relationships и current task. `Colony` хранит только `NpcId` и membership metadata. NPC не получает ссылку на `Colony` aggregate и не изменяет его inventory напрямую.

## 2. Основные концепции и термины

| Термин | Определение в NPC Context |
|---|---|
| **NPC** | Любой управляемый доменной персонаж, не являющийся игроком: колонист, враг, торговец, животное с нужным поведением и т.д. |
| **Citizen** | Основной агрегат для индивидуального разумного NPC с identity, needs, skills и social state. |
| **Колонист** | NPC с `NpcRole.COLONIST` и `ColonyId`; его membership принадлежит ColonyContext. |
| **Враг** | NPC с hostile faction/role; обычно не имеет `ColonyId`, пока не captured/recruited. |
| **Торговец** | NPC с `NpcRole.TRADER`, торговым поведением и ограниченным combat/job profile. |
| **Черта характера (`Trait`)** | Immutable value object, ссылающийся на `traits.json` и содержащий эффекты personality. Может иметь duration. |
| **Навык (`Skill`)** | Значение proficiency по `SkillType`, level `0..100` и experience progression. |
| **Потребность (`Need`)** | Изменяющийся resource-like value: hunger, fatigue, social, comfort, safety. В этой модели большее значение означает большую удовлетворённость. |
| **Настроение (`Mood`)** | Итоговая оценка `0..100`, рассчитанная из needs, traits, relationships и временных modifiers. |
| **Отношения (`Relationship`)** | Направленное мнение одного NPC о другом: `targetId` и opinion `-100..100`. |
| **Семья** | Группа социальных связей и family metadata; в первой версии хранится как relation tags, а не отдельный aggregate. |
| **Роль в колонии** | Доменно разрешённая роль: `COLONIST`, `WARDEN`, `LEADER`, `TRADER`, `PRISONER` и т.п. |
| **Current task** | Намерение/назначение работы, которое NPC может принять и передать execution adapter. |
| **Need critical threshold** | Порог, ниже которого публикуется `NPCNeedCriticalEvent`; определяется `npc-settings.json`. |
| **Mood modifier** | Временная или постоянная причина изменения mood, например `HUNGER`, `TRAIT`, `RECENT_DEATH`. |

### 2.1 Семантика значений

Для `Need.currentValue` и `Mood.value` используется шкала удовлетворённости: `0` — критическое отсутствие, `100` — полная удовлетворённость. Поэтому hunger `80` означает «сыт», а не «сильно голоден». Если UI использует обратную шкалу, преобразование выполняется в presentation adapter.

## 3. Структура пакетов

```text
com.rimworldcraft.core.npc/
  aggregate/
    Citizen.java
    CitizenStatus.java
  entity/
    ActiveTrait.java
    Need.java
    Relationship.java
    CurrentTask.java
  valueobject/
    CitizenId.java
    CitizenName.java
    Gender.java
    RaceId.java
    BirthDate.java
    ColonyId.java
    NpcRole.java
    GridPosition.java
    Trait.java
    Skill.java
    Mood.java
    MoodModifier.java
    NeedType.java
    SkillType.java
  factory/
    CitizenFactory.java
    TraitGenerator.java
    NameGenerator.java
    SkillProfileGenerator.java
  service/
    NeedDecayService.java
    MoodCalculationService.java
    RelationshipService.java
    SkillExperienceService.java
    SocialInteractionService.java
  repository/
    ICitizenRepository.java
    IRelationshipRepository.java
  event/
    NPCCreatedEvent.java
    NPCMoodChangedEvent.java
    NPCSkillIncreasedEvent.java
    NPCRelationshipChangedEvent.java
    NPCNeedCriticalEvent.java
    NPCDeathEvent.java
    NPCJoinedColonyEvent.java
    NPCLeftColonyEvent.java
  port/
    in/
    out/
  contract/
    CitizenSummary.java
    NpcEnvironmentSnapshot.java
  exception/
    CitizenNotFoundException.java
    InvalidTraitException.java
    SkillLevelOutOfRangeException.java
    RelationshipNotAllowedException.java
    CitizenAlreadyDeadException.java
```

### 3.1 Слои

- `aggregate`, `entity`, `valueobject` — доменная модель;
- `service` — policies и операции, требующие несколько aggregate/портов;
- `factory` — создание NPC из конфигурации;
- `repository` — Core interfaces, implementations находятся в infrastructure;
- `event` — immutable facts;
- `port` — driving/driven contracts;
- `exception` — ошибки, независимые от Minecraft.

`core.npc` может зависеть от `core.shared`, общих `core.ports` и собственных пакетов, но не от `infrastructure`, `client` или Minecraft API.

## 4. Агрегат `Citizen`

### 4.1 Состав и владение

`Citizen` — aggregate root для индивидуального разумного NPC. Все изменения состояния проходят через его методы или application service, который загружает aggregate, вызывает метод, сохраняет его и публикует pending events.

```java
public final class Citizen {
    private final CitizenId id;
    private CitizenName name;
    private Gender gender;
    private RaceId race;
    private BirthDate birthDate;
    private final Set<Trait> traits;
    private final EnumMap<SkillType, Skill> skills;
    private final EnumMap<NeedType, Need> needs;
    private Mood mood;
    private final Map<CitizenId, Relationship> relationships;
    private ColonyId colonyId;
    private CitizenStatus status;
    private CurrentTask currentTask;
    private GridPosition position;
    private final List<DomainEvent> pendingEvents;

    public void updateNeeds(NeedUpdate update) {
        requireAlive();
        Need need = requireNeed(update.type());
        need.decay(update.delta(), update.tick());
        if (need.isCritical()) {
            pendingEvents.add(NPCNeedCriticalEvent.from(this, need));
        }
    }

    public void changeMood(MoodCalculation calculation) {
        requireAlive();
        Mood oldMood = mood;
        mood = mood.recalculate(calculation);
        if (oldMood.value() != mood.value()) {
            pendingEvents.add(NPCMoodChangedEvent.of(
                    id, oldMood.value(), mood.value(), calculation.reason()));
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

    public void modifyRelationship(CitizenId targetId, int delta, RelationshipReason reason) {
        requireAlive();
        if (id.equals(targetId)) {
            throw new RelationshipNotAllowedException("SELF_RELATIONSHIP");
        }
        Relationship relationship = relationships.computeIfAbsent(
                targetId, Relationship::neutral);
        int oldValue = relationship.value();
        relationship.modify(delta, reason);
        pendingEvents.add(NPCRelationshipChangedEvent.of(
                id, targetId, oldValue, relationship.value(), reason));
    }

    public void assignTask(CurrentTask task) {
        requireAlive();
        currentTask = Objects.requireNonNull(task);
    }

    public void die(DeathCause cause, GameTick tick) {
        if (status == CitizenStatus.DEAD) {
            return;
        }
        status = CitizenStatus.DEAD;
        currentTask = null;
        pendingEvents.add(NPCDeathEvent.of(id, colonyId, cause, tick));
    }
}
```

### 4.2 Поля агрегата

| Поле | Тип | Обязательность | Инвариант |
|---|---|---:|---|
| `id` | `CitizenId` | required | уникален и неизменяем |
| `name` | `CitizenName` | required | непустое, ограниченная длина |
| `gender` | `Gender` | required | enum или расширяемый ID |
| `race` | `RaceId` | required | valid namespaced ID |
| `birthDate` | `BirthDate` | required | не в будущем относительно world time |
| `traits` | `Set<Trait>` | required | уникальные IDs, conflicts checked |
| `skills` | `Map<SkillType, Skill>` | required | обязательные types имеют начальное значение |
| `needs` | `Map<NeedType, Need>` | required | все configured needs представлены |
| `mood` | `Mood` | required | `0..100` |
| `relationships` | `Map<CitizenId, Relationship>` | required | нет self-link, bounded size |
| `colonyId` | `ColonyId` | optional | есть только для assigned citizen |
| `status` | `CitizenStatus` | required | valid lifecycle transition |
| `currentTask` | `CurrentTask` | optional | отсутствует у dead citizen |
| `position` | `GridPosition` | required | правильный `WorldId` |

### 4.3 Lifecycle

```text
GENERATED -> ACTIVE -> INCAPACITATED -> ACTIVE
ACTIVE -> DEAD
GENERATED -> DEAD (только если spawn/bootstrap failed policy)
```

Недопустимы переходы из `DEAD` в `ACTIVE`, изменение needs/skills после смерти и назначение task мёртвому NPC. Recruited enemy создаётся как новый статус/role в рамках явного use case, а не прямой сменой внешним кодом.

## 5. Value objects и entities

### 5.1 `CitizenId`

```java
public record CitizenId(UUID value) {
    public CitizenId {
        Objects.requireNonNull(value, "value");
    }
}
```

ID создаётся фабрикой или migration mapper и никогда не основывается на runtime entity UUID без явного adapter mapping.

### 5.2 `Trait`

Trait — immutable value object. `ActiveTrait` может оборачивать его с `expiresAt` для временного эффекта.

```java
public record Trait(
        String id,
        String displayKey,
        String descriptionKey,
        Map<SkillType, Integer> skillModifiers,
        int moodModifier,
        Map<String, Integer> socialModifiers,
        TraitDuration duration
) {
    public Trait {
        if (id == null || id.isBlank()) throw new InvalidTraitException("EMPTY_ID");
        if (moodModifier < -100 || moodModifier > 100) {
            throw new InvalidTraitException("MOOD_MODIFIER_OUT_OF_RANGE");
        }
        skillModifiers = Map.copyOf(skillModifiers);
        socialModifiers = Map.copyOf(socialModifiers);
    }
}
```

Инварианты:

- `id` существует в `traits.json`;
- один citizen не получает duplicate trait;
- `conflicts` из конфигурации не нарушены;
- modifiers имеют documented ranges;
- permanent trait не имеет expiration, temporary trait имеет `expiresAt`.

### 5.3 `Skill`

```java
public final class Skill {
    private final SkillType type;
    private int level;
    private int experience;
    private final SkillProgression progression;

    public SkillExperienceResult addExperience(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        int oldLevel = level;
        experience += amount;
        while (level < 100 && experience >= progression.thresholdFor(level + 1)) {
            experience -= progression.thresholdFor(level + 1);
            level++;
        }
        return new SkillExperienceResult(oldLevel, level, experience, level > oldLevel);
    }

    public int getLevel() { return level; }
    public int getNextLevelThreshold() { return progression.thresholdFor(level + 1); }
}
```

Инварианты: `level` в `0..100`; `experience >= 0`; опыт выше level 100 либо capped, либо хранится как overflow согласно конфигурации; `SkillType` должен существовать в `skills.json`.

### 5.4 `Need`

```java
public final class Need {
    private final NeedType type;
    private double currentValue;
    private final double decayRate;
    private final double importanceWeight;
    private final double criticalThreshold;

    public void decay(double deltaTicks, GameTick tick) {
        currentValue = Math.max(0.0, currentValue - decayRate * deltaTicks);
    }

    public boolean isCritical() {
        return currentValue <= criticalThreshold;
    }
}
```

`decayRate >= 0`, `importanceWeight >= 0`, значения `0..100`, critical threshold `0..100`. Еда, кровать и безопасность влияют через `NeedEnvironmentModifier`, а не через прямой Minecraft lookup из Need.

### 5.5 `Mood`

```java
public record Mood(
        int value,
        List<MoodModifier> modifiers,
        GameTick lastUpdated
) {
    public Mood {
        if (value < 0 || value > 100) throw new IllegalArgumentException("Mood 0..100");
        modifiers = List.copyOf(modifiers);
    }

    public Mood recalculate(MoodCalculation calculation) {
        int result = (int) Math.round(calculation.baseValue()
                + calculation.modifiers().stream().mapToInt(MoodModifier::amount).sum());
        return new Mood(Math.max(0, Math.min(100, result)), calculation.modifiers(), calculation.tick());
    }
}
```

Модификатор содержит `reason`, `amount`, `sourceId`, `expiresAt` и optional priority. Дубликаты одного временного source должны быть идемпотентны.

### 5.6 `Relationship`

```java
public final class Relationship {
    private final CitizenId targetId;
    private int value;
    private final Deque<RelationshipHistoryEntry> history;

    public void modify(int delta, RelationshipReason reason) {
        value = Math.max(-100, Math.min(100, value + delta));
        history.addLast(new RelationshipHistoryEntry(reason, delta));
        while (history.size() > 20) history.removeFirst();
    }

    public int getOpinion() { return value; }
}
```

Relationship направлен: мнение `A` о `B` не обязано быть равно мнению `B` о `A`. Самосвязи запрещены. Максимум связей определяется `npc-settings.json`; при превышении используется policy pruning, а не случайное удаление без события.

### 5.7 `CurrentTask` и `GridPosition`

```java
public record CurrentTask(
        UUID taskId,
        String jobTypeId,
        GridPosition target,
        TaskStatus status,
        GameTick assignedAt
) {}

public record GridPosition(WorldId worldId, int x, int y, int z) {}
```

`CurrentTask` — доменное намерение; его исполнение физическим NPC делает `IEntitySpawnPort`/goal AI adapter. `GridPosition` не является `BlockPos`.

## 6. Фабрики и генераторы

### 6.1 `CitizenFactory`

Фабрика создаёт полностью валидный aggregate, а не partially initialized object.

```java
public final class CitizenFactory {
    private final NameGenerator names;
    private final TraitGenerator traits;
    private final SkillProfileGenerator skills;
    private final NpcConfigRepository config;
    private final ITimePort time;
    private final RandomPort random;

    public Citizen createColonist(WorldId worldId, ColonyId colonyId, NpcGenerationSeed seed) {
        NpcArchetype archetype = config.archetype("colonist");
        Citizen citizen = Citizen.create(
                CitizenId.newId(),
                names.generate(archetype.race(), archetype.culture(), archetype.gender(), random),
                archetype.gender(), archetype.race(),
                BirthDate.randomPast(time.currentGameTime(worldId), random),
                traits.generate(archetype, random),
                skills.generate(archetype, random),
                config.initialNeeds(),
                colonyId,
                new GridPosition(worldId, seed.x(), seed.y(), seed.z()));
        return citizen;
    }

    public Citizen createRandomCitizen(WorldId worldId, NpcGenerationSeed seed) {
        return createByRole(worldId, null, NpcRole.CITIZEN, seed);
    }

    public Citizen createEnemy(WorldId worldId, FactionId faction, NpcGenerationSeed seed) {
        return createByRole(worldId, null, NpcRole.ENEMY,
                seed.withFaction(faction));
    }
}
```

Фабрика зависит только от Core ports/config abstractions. Она не спаунит Minecraft entity; после создания application service может отправить `SpawnIntent` через `IEntitySpawnPort`.

### 6.2 `TraitGenerator`

Алгоритм:

1. Получить candidate traits по archetype/role.
2. Отфильтровать запрещённые role traits и conflicts.
3. Выполнить weighted selection через deterministic `RandomPort`.
4. Проверить максимальное количество и уникальность.
5. Создать immutable `Trait` objects.

```java
public interface TraitGenerator {
    Set<Trait> generate(NpcArchetype archetype, RandomPort random);
}
```

Генератор никогда не выбирает trait, отсутствующий в validated `traits.json`.

### 6.3 `NameGenerator`

`npc-names.json` группирует имена по `race`, `culture`, `gender` и optional phoneme rules. Fallback chain:

```text
race + culture + gender -> culture + gender -> race + gender -> neutral pool -> stable generated name
```

Имя должно быть детерминируемым при заданном seed, чтобы replay/test save мог воспроизводиться. Duplicate names разрешены, duplicate `CitizenId` — нет.

### 6.4 `SkillProfileGenerator`

Начальные skills задаются archetype profile:

- базовый уровень по каждому skill type;
- pool очков;
- min/max;
- role weights;
- optional passion multiplier.

Пример распределения для colonist builder:

```text
BUILDING 40%, CRAFTING 25%, MINING 15%, FARMING 10%, COMBAT 5%, CHARISMA 5%
```

Алгоритм обязан соблюдать общий budget и ranges `0..100`. Нельзя добавлять несуществующий skill type из JSON.

## 7. Доменные сервисы

### 7.1 `NeedDecayService`

**Ответственность:** обновлять needs активных NPC за simulation interval. Сервис не знает Minecraft objects.

```java
public interface NeedDecayService {
    NeedDecayReport updateAll(WorldId worldId, GameTick from, GameTick to);
    NeedDecayResult update(CitizenId citizenId, NeedEnvironmentSnapshot environment);
}
```

Зависимости:

- `ICitizenRepository`;
- `ITimePort`;
- `IBlockWorldPort` — только через `EnvironmentQuery` для bed/safety/temperature facts;
- `IInventoryPort` — проверка availability еды через Core-friendly inventory ref;
- `INpcConfigPort`;
- `IEventBusPort`.

Процесс:

1. выбрать ACTIVE/INCAPACITATED citizens по world;
2. получить cached environment snapshot, не сканировать мир для каждого NPC без budget;
3. применить decay и modifiers;
4. сохранить changed aggregates;
5. опубликовать `NPCNeedCriticalEvent` один раз при пересечении threshold;
6. вызвать mood calculation.

### 7.2 `MoodCalculationService`

```java
public interface MoodCalculationService {
    MoodCalculation calculate(Citizen citizen, MoodContext context);
    MoodUpdateResult recalculate(CitizenId citizenId, MoodContext context);
}
```

Учитывает:

- `Need.importanceWeight`;
- permanent/temporary traits;
- relationship average и significant events;
- active incidents from Storyteller projections;
- environment modifiers;
- recent death, injury, achievement и rest.

Storyteller не вызывает этот service напрямую: он публикует event или предоставляет read-only incident projection.

### 7.3 `RelationshipService`

```java
public interface RelationshipService {
    RelationshipChangeResult modify(
            CitizenId sourceId,
            CitizenId targetId,
            RelationshipDelta delta);
    void processEvent(RelationshipEvent event);
}
```

Зависимости: `ICitizenRepository`, optional `IRelationshipRepository`, `IEventBusPort`, `ITimePort`, `INpcConfigPort`.

Сервис проверяет:

- source/target существуют;
- они не совпадают;
- оба относятся к одному допустимому world scope;
- delta не нарушает policy;
- max relationship count.

### 7.4 `SkillExperienceService`

```java
public interface SkillExperienceService {
    SkillExperienceResult addExperience(
            CitizenId citizenId,
            SkillType skillType,
            int amount,
            SkillExperienceReason reason);
}
```

Зависимости: `ICitizenRepository`, `ISkillConfigPort`, `IEventBusPort`. Опыт за одну игровую операцию имеет cap; multiplier из traits/role применяется один раз. Level-up публикует `NPCSkillIncreasedEvent`.

### 7.5 `SocialInteractionService`

```java
public interface SocialInteractionService {
    SocialInteractionResult interact(
            CitizenId initiator,
            CitizenId target,
            SocialInteractionType type,
            SocialContext context);
}
```

Зависимости: `ICitizenRepository`, `IBlockWorldPort` или `IPathfinderPort` для proximity, `ITimePort`, `RandomPort`, config и events.

Правила:

- NPC должны быть в одном `WorldId` и пределах interaction distance;
- traits влияют на success/delta;
- настроение и текущая задача могут блокировать interaction;
- событие применяется к направленному relationship;
- крайне отрицательная opinion может выдать `ConflictIntent`, но Combat/Storyteller решают дальнейшее действие.

### 7.6 Оркестрация и tick budget

`NpcSimulationService` может координировать сервисы, но не должен владеть state:

```java
public final class NpcSimulationService {
    public void advance(WorldId worldId, GameTick tick) {
        needDecay.updateBatch(worldId, tick);
        moodCalculation.updateChanged(worldId, tick);
        socialInteractions.processScheduled(worldId, tick);
    }
}
```

Для больших миров используйте batches, dirty flags и фиксированный simulation interval. Render tick не должен запускать полную NPC simulation.

## 8. Репозитории

### 8.1 `ICitizenRepository`

```java
public interface ICitizenRepository {
    Optional<Citizen> findById(WorldId worldId, CitizenId citizenId);
    List<Citizen> findAllByColonyId(WorldId worldId, ColonyId colonyId);
    List<Citizen> findActive(WorldId worldId, int limit, PageToken page);
    void save(Citizen citizen);
    void saveAll(Collection<Citizen> citizens);
    void delete(WorldId worldId, CitizenId citizenId);
}
```

Repository возвращает aggregate, а не Minecraft entity. `save` должен быть atomic относительно aggregate version или использовать optimistic concurrency.

### 8.2 `IRelationshipRepository`

В первой версии relationships находятся внутри `Citizen`, потому что изменение relationship является частью citizen aggregate. Если количество связей станет большим, можно вынести их в отдельный repository:

```java
public interface IRelationshipRepository {
    Optional<Relationship> find(CitizenId source, CitizenId target);
    List<Relationship> findAllFor(CitizenId source, int limit);
    void save(Relationship relationship);
    void deleteAllFor(CitizenId citizenId);
}
```

Вынос допускается только с ADR, потому что изменяется consistency boundary.

### 8.3 Infrastructure implementation

- `NbtCitizenRepositoryAdapter` реализует `ICitizenRepository` через `ISaveLoadPort`;
- `NbtRelationshipRepositoryAdapter` — optional;
- mapper `CitizenSnapshotMapper` преобразует aggregate в platform-neutral `SaveDocument`;
- `MinecraftEntityAdapter` хранит mapping доменный `CitizenId` → runtime entity UUID.

`ISaveLoadPort` является единственным Core-facing storage dependency; NBT не импортируется в `core.npc`.

## 9. Доменные события

### 9.1 Общие требования

Все события immutable и содержат:

```text
eventId, eventType, schemaVersion, occurredAt, worldId, correlationId
```

Payload содержит только IDs, primitives, enums и Core DTO. События публикуются после успешного изменения aggregate/persistence либо через transactional outbox.

### 9.2 Каталог событий

| Событие | Публикатор | Основные данные | Подписчики |
|---|---|---|---|
| `NPCCreatedEvent` | `CitizenFactory`/creation service | `citizenId`, name, role, race, `colonyId`, `worldId` | Colony, Storyteller, projections, spawn adapter |
| `NPCJoinedColonyEvent` | membership/application flow | `citizenId`, `colonyId`, tick | Colony, Player, UI |
| `NPCLeftColonyEvent` | leave/death/recruitment flow | `citizenId`, `colonyId`, reason | Colony, Player, UI |
| `NPCMoodChangedEvent` | `Citizen`/Mood service | old/new mood, reason, tick | Storyteller, Colony summary, UI |
| `NPCSkillIncreasedEvent` | `SkillExperienceService` | skill type, old/new level, reason | Player progression, Storyteller, UI |
| `NPCRelationshipChangedEvent` | `RelationshipService` | source/target IDs, old/new value, reason | Storyteller, UI, Social projection |
| `NPCNeedCriticalEvent` | `NeedDecayService`/`Citizen` | need type/value, threshold | Storyteller, Colony, UI |
| `NPCDeathEvent` | `Citizen.die()` | citizen/colony IDs, cause, tick | Colony removes member; Storyteller creates loss event; Player UI |

### 9.3 Event contracts

```java
public record NPCMoodChangedEvent(
        UUID eventId,
        WorldId worldId,
        CitizenId citizenId,
        int oldMood,
        int newMood,
        String reason,
        GameTick occurredAt,
        String correlationId,
        int schemaVersion
) implements DomainEvent {}
```

```java
public record NPCDeathEvent(
        UUID eventId,
        WorldId worldId,
        CitizenId citizenId,
        ColonyId colonyId,
        DeathCause cause,
        GameTick occurredAt,
        String correlationId,
        int schemaVersion
) implements DomainEvent {}
```

### 9.4 Взаимодействие с Colony

- `NPCJoinedColonyEvent` обновляет Colony membership только через Colony application port/handler.
- `NPCLeftColonyEvent` удаляет membership идемпотентно.
- `NPCDeathEvent` является фактом смерти; Colony удаляет `NpcId`, но NPC Core остаётся владельцем death state/history.
- NPC Core не импортирует `Colony` aggregate и не вызывает `Colony.removeMember()` напрямую.

### 9.5 Взаимодействие с Storyteller

Storyteller использует NPC events как входные факты для pacing и incident eligibility:

- множество `NPCNeedCriticalEvent` повышает pressure;
- `NPCDeathEvent` может разрешить narrative event «Loss of colonist»;
- `NPCMoodChangedEvent` влияет на colony mood summary;
- `NPCRelationshipChangedEvent` может открыть conflict/social incident;
- `NPCSkillIncreasedEvent` учитывается в progression/wealth summary.

Storyteller не отправляет NPC commands через прямой вызов aggregate. Если нужен эффект, он публикует отдельный incident event/intent, который NPC handler применяет через собственный use case.

## 10. Сценарии использования

### 10.1 Сценарий 1: генерация нового колониста

**Предусловие:** Colony успешно создана и запросила стартовый набор из трёх жителей.

```text
ColonyContext -> NPC application: CreateStarterCitizens(ColonyId, WorldId, count=3)
NPC application -> CitizenFactory: createColonist(...)
CitizenFactory -> NameGenerator: generate(race, culture, gender)
CitizenFactory -> TraitGenerator: generate(archetype)
CitizenFactory -> SkillProfileGenerator: generate(role profile)
CitizenFactory --> NPC application: valid Citizen
NPC application -> ICitizenRepository: save(citizen)
NPC application -> IEventBusPort: NPCCreatedEvent
NPC application -> IEventBusPort: NPCJoinedColonyEvent
NPC application -> IEntitySpawnPort: SpawnIntent(citizenId, position)
IEntitySpawnPort --> Minecraft: create NPC entity
IEventBusPort --> ColonyContext: add NpcId membership
IEventBusPort --> StorytellerContext: update population facts
```

**Инварианты:** ровно три успешных citizen records; каждый ID уникален; `colonyId` совпадает; события имеют correlation ID bootstrap operation; duplicate retry не создаёт четвёртого NPC.

### 10.2 Сценарий 2: ежедневное обновление потребностей

```text
MinecraftServerTickAdapter -> AdvanceNpcSimulationUseCase: advance(worldId, tick)
AdvanceNpcSimulationUseCase -> ITimePort: currentTick(worldId)
AdvanceNpcSimulationUseCase -> ICitizenRepository: findActive(batch)
NeedDecayService -> IBlockWorldPort: environment facts (bed, temperature, safety)
NeedDecayService -> IInventoryPort: food availability summary
NeedDecayService -> Citizen: updateNeeds(delta)
Citizen -> EventBus: NPCNeedCriticalEvent (if threshold crossed)
NeedDecayService -> ICitizenRepository: saveAll(changed)
MoodCalculationService -> Citizen: changeMood(calculation)
Citizen -> EventBus: NPCMoodChangedEvent
EventBus -> Storyteller: pressure update
EventBus -> UI projection: HUD update
```

Обновление выполняется не по render frames, а по фиксированному simulation interval. Critical event публикуется при переходе через threshold, а не каждый tick, если это не предусмотрено policy.

### 10.3 Сценарий 3: социальное взаимодействие

```text
NpcSimulation -> SocialInteractionService: interact(A, B, TALK)
SocialInteractionService -> IBlockWorldPort/IPathfinderPort: validate proximity
SocialInteractionService -> ICitizenRepository: load A and B
SocialInteractionService -> Trait/Relationship policies: calculate delta
SocialInteractionService -> Citizen A: modifyRelationship(B, delta)
SocialInteractionService -> ICitizenRepository: save A
SocialInteractionService -> EventBus: NPCRelationshipChangedEvent
EventBus -> Storyteller: possible conflict/social incident
EventBus -> UI: relationship projection
```

Если value достигает `-80`, сервис может вернуть `ConflictIntent`. Он не создаёт combat entity и не меняет здоровье сам.

### 10.4 Сценарий 4: повышение навыка

```text
Goal AI adapter -> SkillExperienceService: addExperience(citizenId, BUILDING, 25)
SkillExperienceService -> ICitizenRepository: load Citizen
SkillExperienceService -> skills.json snapshot: progression threshold
SkillExperienceService -> Citizen: addSkillExperience(...)
Citizen -> EventBus: NPCSkillIncreasedEvent (if level-up)
SkillExperienceService -> ICitizenRepository: save
EventBus -> Player/Storyteller/UI: progression update
```

Опыт проверяется на non-negative amount и operation idempotency. Повторный callback от adapter не должен дважды начислить опыт.

### 10.5 Сценарий 5: смерть NPC

```text
Minecraft damage adapter -> ApplyDamageUseCase: lethal damage(citizenId, cause)
ApplyDamageUseCase -> ICitizenRepository: load Citizen
ApplyDamageUseCase -> Citizen: die(cause, tick)
Citizen -> EventBus: NPCDeathEvent
ApplyDamageUseCase -> ICitizenRepository: save(dead citizen)
EventBus -> ColonyContext: remove NpcId membership
EventBus -> StorytellerContext: evaluate "Loss of colonist"
EventBus -> IEntitySpawnPort: despawn runtime entity (adapter reaction)
EventBus -> Player/UI: notification and selection cleanup
```

Death event идемпотентен. Colony не удаляет historical NPC snapshot до политики retention; это нужно для save compatibility и storytelling history.

## 11. Конфигурационные параметры

### 11.1 `traits.json`

Источник: `config/rwc/npc/traits.json`.

Используется `TraitConfigRepository`, `TraitGenerator`, `MoodCalculationService`, `RelationshipService` и `CitizenFactory`.

Ключевые поля:

```json
{
  "$schema": "rwc://schemas/npc/traits.schema.json",
  "version": 1,
  "traits": [
    {
      "id": "rwc:optimist",
      "displayKey": "trait.rwc.optimist.name",
      "descriptionKey": "trait.rwc.optimist.description",
      "roles": ["COLONIST", "TRADER"],
      "skillModifiers": { "CHARISMA": 3, "FARMING": 1 },
      "moodModifier": 6,
      "socialModifiers": { "friendliness": 8, "conflictTolerance": 4 },
      "conflicts": ["rwc:pessimist"],
      "duration": { "type": "PERMANENT" }
    }
  ]
}
```

Validator проверяет IDs, ranges, conflicts и существование `SkillType`.

### 11.2 `skills.json`

Источник: `config/rwc/npc/skills.json`.

Используется `SkillProfileGenerator`, `SkillExperienceService` и `Citizen` hydration.

```json
{
  "$schema": "rwc://schemas/npc/skills.schema.json",
  "version": 1,
  "skills": [
    {
      "id": "BUILDING",
      "displayKey": "skill.rwc.building",
      "minLevel": 0,
      "maxLevel": 100,
      "baseExperienceThreshold": 100,
      "thresholdMultiplier": 1.15,
      "experienceGain": {
        "build_wall": 12,
        "build_room": 40
      }
    },
    {
      "id": "FARMING",
      "displayKey": "skill.rwc.farming",
      "minLevel": 0,
      "maxLevel": 100,
      "baseExperienceThreshold": 80,
      "thresholdMultiplier": 1.12,
      "experienceGain": { "plant": 8, "harvest": 18 }
    }
  ]
}
```

`id` должен соответствовать `SkillType`; threshold и gain неотрицательны.

### 11.3 `npc-names.json`

Источник: `config/rwc/npc/npc-names.json`.

Используется `NameGenerator`:

```json
{
  "$schema": "rwc://schemas/npc/npc-names.schema.json",
  "version": 1,
  "pools": [
    {
      "race": "human",
      "culture": "frontier",
      "gender": "FEMALE",
      "firstNames": ["Mara", "Elin", "Sera"],
      "surnames": ["Voss", "Kerr", "Vale"],
      "weight": 1.0
    }
  ]
}
```

Пустые pools запрещены, `weight > 0`, duplicate names допустимы только при разных generated citizens.

### 11.4 `npc-settings.json`

Новый файл: `config/rwc/npc/npc-settings.json`.

Используется `NpcConfigRepository`, `CitizenFactory`, `NeedDecayService`, `MoodCalculationService`, `RelationshipService` и `SocialInteractionService`.

```json
{
  "$schema": "rwc://schemas/npc/npc-settings.schema.json",
  "version": 1,
  "initialNeeds": {
    "HUNGER": 80,
    "FATIGUE": 80,
    "SOCIAL": 70,
    "COMFORT": 70,
    "SAFETY": 90
  },
  "criticalThresholds": {
    "HUNGER": 20,
    "FATIGUE": 15,
    "SOCIAL": 20,
    "COMFORT": 20,
    "SAFETY": 25
  },
  "decayRatesPerTick": {
    "HUNGER": 0.002,
    "FATIGUE": 0.0015,
    "SOCIAL": 0.0005,
    "COMFORT": 0.0003,
    "SAFETY": 0.0001
  },
  "importanceWeights": {
    "HUNGER": 1.5,
    "FATIGUE": 1.2,
    "SOCIAL": 0.7,
    "COMFORT": 0.5,
    "SAFETY": 1.8
  },
  "maxRelationships": 32,
  "interactionDistance": 6.0,
  "criticalEventCooldownTicks": 600
}
```

Все maps должны покрывать configured `NeedType`; values имеют documented ranges. `criticalEventCooldownTicks >= 0`, `maxRelationships` ограничен server safety upper bound.

### 11.5 Связь с другими конфигами

| Конфигурация | NPC-код |
|---|---|
| `traits.json` | `TraitConfigRepository`, `TraitGenerator`, mood/social policies |
| `skills.json` | `SkillConfigRepository`, initial profile, XP progression |
| `npc-names.json` | `NameGenerator` |
| `npc-settings.json` | needs, thresholds, decay, relationships, social distance |
| `colony-settings.json` | только через Colony contracts: starter count/resources; NPC не читает Colony raw config напрямую |
| `story-events.json` | Storyteller owns it; NPC принимает resulting incident events |
| `world-biome-modifiers.json` | World produces environment summary; NPC не читает JSON напрямую |

## 12. Порты, используемые NPC Core

### 12.1 `IEntitySpawnPort`

Используется при создании стартовых NPC, появлении врагов/торговцев и удалении runtime entity.

```java
public interface IEntitySpawnPort {
    SpawnResult spawn(SpawnIntent intent);
    DespawnResult despawn(EntityId entityId);
    Optional<EntitySnapshot> find(EntityId entityId);
    void applyIntent(EntityIntent intent);
}
```

NPC Core вызывает `spawn` только после формирования валидного `SpawnIntent`. `EntitySnapshot` — Core DTO, не Minecraft Entity.

### 12.2 `IPathfinderPort`

Опционален для базовой модели, обязателен для Goal AI/job execution:

```java
public interface IPathfinderPort {
    PathResult findPath(PathRequest request);
    ReachabilityResult canReach(GridPosition from, GridPosition to,
                                MovementProfile profile);
    void cancel(PathRequestId requestId);
}
```

Используется в `SocialInteractionService` для proximity validation и в task execution для движения к цели.

### 12.3 `ITimePort`

```java
public interface ITimePort {
    GameTick currentTick(WorldId worldId);
    GameTime currentGameTime(WorldId worldId);
    boolean elapsed(GameTick since, Duration duration, WorldId worldId);
}
```

Используется для age/birthDate, need decay, mood timestamps, relationship history, cooldowns, task assignment и deterministic tests.

### 12.4 `ISaveLoadPort`

```java
public interface ISaveLoadPort {
    Optional<SaveDocument> load(SaveKey key);
    void save(SaveKey key, SaveDocument document);
    void delete(SaveKey key);
}
```

NPC repository сохраняет Citizen snapshot, relationships и schema version. NBT mapping находится в `NbtCitizenRepositoryAdapter`.

### 12.5 `IBlockWorldPort`

```java
public interface IBlockWorldPort {
    BlockSnapshot inspect(GridPosition position);
    ClimateFact climateAt(GridPosition position);
    LightLevel lightAt(GridPosition position);
    EnvironmentFact environmentAt(GridPosition position);
}
```

Используется для bed/comfort/safety/temperature facts, proximity и target validation. NPC Core не проверяет `BlockState` напрямую.

### 12.6 `IInventoryPort`

```java
public interface IInventoryPort {
    InventorySnapshot read(InventoryRef ref);
    ReservationResult reserve(InventoryRef ref, ResourceRequest request);
    CommitResult commit(ReservationId reservationId);
    ReleaseResult release(ReservationId reservationId);
}
```

Используется при проверке еды, job resource requirements и execution results. Colony inventory ownership остаётся в ColonyContext.

### 12.7 `IEventBusPort`

```java
public interface IEventBusPort {
    void publish(EventEnvelope event);
    Subscription subscribe(EventFilter filter, EventHandler handler);
}
```

NPC Core публикует свои события и подписывается на `WorkAssigned`, incident effects, `ColonyFounded` и world/environment facts через integration contracts.

### 12.8 `IConfigPort`

```java
public interface IConfigPort {
    <T> ConfigSnapshot<T> get(ConfigKey key, Class<T> type);
    ConfigVersion version(ConfigKey key);
}
```

NPC использует keys `npc.traits`, `npc.skills`, `npc.names`, `npc.settings`. Конфиги validated and immutable.

## 13. Обработка ошибок и исключений

### 13.1 Доменные исключения

```java
public final class CitizenNotFoundException extends RuntimeException {
    public CitizenNotFoundException(CitizenId id) {
        super("Citizen not found: " + id);
    }
}

public final class InvalidTraitException extends RuntimeException {
    public InvalidTraitException(String reason) { super(reason); }
}

public final class SkillLevelOutOfRangeException extends RuntimeException {
    public SkillLevelOutOfRangeException(SkillType type, int level) {
        super("Skill level out of range: " + type + "=" + level);
    }
}

public final class RelationshipNotAllowedException extends RuntimeException {
    public RelationshipNotAllowedException(String reason) { super(reason); }
}
```

Дополнительные ошибки:

- `CitizenAlreadyDeadException`;
- `InvalidNeedValueException`;
- `NpcConfigurationException`;
- `NpcPersistenceException` как Core-facing wrapper;
- `TaskAssignmentRejectedException`.

### 13.2 Политика

- aggregate invariant violation → domain error, state не сохраняется;
- missing citizen → `CitizenNotFoundException`, adapter формирует safe command failure;
- invalid config → reload отклоняется, старый snapshot остаётся активным;
- save failure → retry/diagnostic, не публиковать fake success event;
- spawn/path failure → `SpawnResult`/`PathResult` failure и retry/deferred task;
- duplicate event/command → idempotent no-op или existing result;
- Minecraft exceptions переводятся в `PortAccessException`, не покидают adapter.

### 13.3 Транслирование в adapters

| Core error | Driving adapter response |
|---|---|
| `CitizenNotFoundException` | command result `NPC_NOT_FOUND` |
| `InvalidTraitException` | configuration error/log, reject creation |
| `SkillLevelOutOfRangeException` | reject progression request |
| `RelationshipNotAllowedException` | `RELATIONSHIP_NOT_ALLOWED` |
| port unavailable | retry/defer and user-safe notification |
| corrupt save | quarantine/backup and diagnostic |

Не показывать игроку stack trace и внутренние NBT/Minecraft details.

## 14. Тестирование модуля

### 14.1 Unit-тесты aggregate

Обязательные группы:

- создание valid/invalid citizen;
- lifecycle transitions;
- needs decay and critical threshold;
- mood recalculation/clamping;
- skill XP, threshold and level cap;
- relationship bounds/self-link/history;
- trait conflicts and temporary expiration;
- task assignment/death behavior;
- pending event collection and idempotency.

Пример:

```java
@Test
void moodDropsWhenHungerBecomesCritical() {
    Citizen citizen = CitizenFixtures.withMoodAndNeed(
            MoodFixtures.value(70), NeedType.HUNGER, 80);

    citizen.updateNeeds(new NeedUpdate(NeedType.HUNGER, 60, new GameTick(20_000)));
    citizen.changeMood(MoodCalculationFixtures.fromNeeds(citizen));

    assertThat(citizen.mood().value()).isEqualTo(40);
    assertThat(citizen.pullDomainEvents())
            .anyMatch(event -> event instanceof NPCMoodChangedEvent);
}
```

Точные числа зависят от configured weights; fixture должен явно задавать calculation policy, чтобы тест не зависел от случайных defaults.

### 14.2 Unit-тесты сервисов с mocks/fakes

`NeedDecayServiceTest` подменяет `ICitizenRepository`, `ITimePort`, `IBlockWorldPort`, `IInventoryPort`, `IConfigPort`, `IEventBusPort`.

```java
@ExtendWith(MockitoExtension.class)
class SkillExperienceServiceTest {
    @Mock ICitizenRepository citizens;
    @Mock ISkillConfigPort skills;
    @Mock IEventBusPort events;

    @Test
    void publishesEventWhenSkillCrossesLevelThreshold() {
        Citizen citizen = CitizenFixtures.withSkill(SkillType.BUILDING, 9, 95);
        when(citizens.findById(any(), any())).thenReturn(Optional.of(citizen));
        when(skills.progression(SkillType.BUILDING))
                .thenReturn(SkillProgressionFixtures.standard());

        SkillExperienceService service =
                new DefaultSkillExperienceService(citizens, skills, events);
        service.addExperience(
                citizen.id(), SkillType.BUILDING, 20,
                SkillExperienceReason.BUILDING_COMPLETED);

        verify(citizens).save(citizen);
        verify(events).publish(argThat(event ->
                event.eventType().equals("NPCSkillIncreasedEvent")));
    }
}
```

### 14.3 Интеграционные тесты

С реальными adapters проверяются:

1. сохранение Citizen в NBT через `ISaveLoadPort`;
2. загрузка всех полей: ID, name, traits, skills, needs, mood, relationships, task, position и status;
3. миграция старой schema version;
4. invalid/corrupt snapshot rejection;
5. spawn intent mapping в Forge и Fabric;
6. pathfinder contract для vanilla/Baritone adapter;
7. event serialization/deserialization;
8. JSON config loading всех четырёх NPC files;
9. reload atomicity: invalid new config не уничтожает old snapshot.

### 14.4 Приёмочный тест Gherkin

```gherkin
Feature: NPC mood

  Scenario: Mood drops when hungry
    Given a citizen with hunger 80 and mood 70
    And hunger decay is configured to reduce hunger to 30 after 10 in-game hours
    When 10 in-game hours pass without eating
    Then hunger is 30
    And mood is 40 according to the configured mood policy
    And NPCMoodChangedEvent is fired

  Scenario: Critical hunger is announced once
    Given a citizen has hunger 25
    And the critical hunger threshold is 20
    When hunger falls to 19
    Then NPCNeedCriticalEvent is fired
    When one more simulation tick passes with hunger below 20
    Then NPCNeedCriticalEvent is not duplicated during the cooldown
```

### 14.5 Coverage и quality gates

- aggregate/domain code target: `>=85%` line and branch coverage;
- all public aggregate methods covered by behavior tests;
- mutation testing рекомендуется для lifecycle, skill cap, relationship and inventory-related rules;
- no Minecraft runtime in unit source sets;
- integration tests run for NBT and JSON fixtures;
- ArchUnit runs in CI.

## 15. DoD модуля NPC

- [ ] Все агрегаты покрыты юнит-тестами с покрытием **не менее 85%**.
- [ ] Все доменные сервисы покрыты юнит-тестами, включая успешные, ошибочные и повторные сценарии.
- [ ] Реализованы все доменные события и их публикация после успешной фиксации состояния.
- [ ] Написаны интеграционные тесты для сохранения/загрузки NPC, миграций и corrupt data.
- [ ] JSON-конфиги `traits.json`, `skills.json`, `npc-names.json`, `npc-settings.json` загружаются и парсятся корректно.
- [ ] ArchUnit-правила для модуля не нарушены (см. раздел 16).
- [ ] Проверено взаимодействие с Colony: добавление, удаление, смерть и отсутствие прямого доступа к `Colony` aggregate.
- [ ] Проверено взаимодействие с Storyteller: mood, critical needs, skill, relationship и death events.
- [ ] Server authority, command/event idempotency и thread boundaries задокументированы и протестированы.
- [ ] Все используемые конфигурационные поля описаны в [`data-dictionaries.md`](data-dictionaries.md).
- [ ] Forge/Fabric-specific code отсутствует в `core.npc` и покрыт adapter contract tests.

## 16. ArchUnit-правила для NPC

### 16.1 Запрет Minecraft API

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.npc..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "net.minecraft..",
        "net.minecraftforge..",
        "net.fabricmc..",
        "com.mojang.blaze3d.."
    );
```

### 16.2 Разрешённое направление зависимостей

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.npc..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.infrastructure..",
        "com.rimworldcraft.client.."
    );
```

Разрешены собственные пакеты NPC, `core.shared`, общие `core.ports` и versioned contracts.

### 16.3 Запрет прямого обращения к `Colony`

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.npc..")
    .should().dependOnClassesThat()
    .haveSimpleName("Colony");
```

NPC может использовать `ColonyId`, `ColonySummary` и `WorkAssigned` event. Он не импортирует `com.rimworldcraft.core.colony.domain.Colony` и не вызывает его методы.

### 16.4 Порты должны быть интерфейсами

```java
classes()
    .that().resideInAnyPackage("com.rimworldcraft.core.npc.port..")
    .should().beInterfaces();
```

### 16.5 Адаптеры реализуют порты NPC

```java
classes()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.adapters.driven.npc..")
    .and().haveSimpleNameEndingWith("Adapter")
    .should().implement(
        JavaClass.Predicates.resideInAnyPackage(
            "com.rimworldcraft.core.npc.port.out.."));
```

### 16.6 Публичные методы aggregate покрыты тестами

ArchUnit сам по себе не доказывает behavior coverage. Используется обязательная комбинация:

```text
JaCoCo: Citizen >= 85% line + branch coverage
Mutation gate: lifecycle, skill, relationship and death policies
CitizenApiTest: каждый публичный behavior method вызывается тестом
```

### 16.7 Запрет `System.out`

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.npc..")
    .should().accessClassesThat()
    .belongToAnyOf(System.class);
```

Продакшн-логирование выполняется через проектный logger/observability port; `System.err` также запрещён.

### 16.8 Domain не зависит от application

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.core.npc.domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("com.rimworldcraft.core.npc.application..");
```

## 17. План расширения

### 17.1 Новые типы черт

Добавить trait только data-first способом:

1. внести namespaced ID и effects в `traits.json`;
2. добавить role/conflict validation;
3. расширить `TraitEffect` через generic modifier key, если новый эффект не меняет aggregate contract;
4. добавить unit tests для mood/social/skill impact;
5. обновить `data-dictionaries.md`.

Если эффект требует нового поведения, создать отдельную policy (`TraitEffectPolicy`), а не добавлять giant switch в `Citizen`.

### 17.2 Новые навыки

`SkillType` расширяется новым ID в `skills.json`; `SkillProfileGenerator` и XP service работают с каталогом. Если новый skill требует уникальной механики, используйте strategy:

```java
public interface SkillProgressionRule {
    SkillExperienceResult apply(Skill skill, SkillAction action, SkillConfig config);
}
```

Существующие skills и save snapshots должны продолжать загружаться; missing newly introduced skill получает default profile.

### 17.3 Новые социальные механики

Для браков, детей, семейных домов и наследования:

- не перегружать `Relationship` всеми family rules;
- выделить `FamilyContext`/`FamilyGroup` после определения aggregate boundary;
- NPC хранит `FamilyId` или relation reference;
- события: `FamilyFormed`, `ChildBorn`, `MarriageEnded`;
- миграция старых NPC должна создавать neutral/no-family state.

До выделения отдельного контекста можно добавить `RelationshipTag` (`FAMILY`, `FRIEND`, `RIVAL`) без изменения Colony ownership.

### 17.4 Новые роли NPC

Добавить `NpcRole` и role profile в config. Фабрика получает archetype, а не `if/else` в каждом сервисе:

```java
public interface NpcArchetypePolicy {
    ArchetypeDefinition definition(NpcRole role, FactionId faction);
}
```

### 17.5 Новые needs

Новый `NeedType` должен иметь:

- initial value;
- decay policy;
- critical threshold;
- importance weight;
- mood mapping;
- save migration default;
- unit/acceptance tests.

Если need требует внешнего факта, расширяется `NeedEnvironmentSnapshot`, а не добавляется Minecraft call в `Need`.

### 17.6 Новые AI behaviors

`Goal AI` получает `CitizenSummary`, `CurrentTask` и `NpcEnvironmentSnapshot`. NPC Core публикует intents, но не управляет navigation engine. Это сохраняет совместимость с vanilla и Baritone adapters.

## 18. Контроль производительности и безопасности

- обновлять needs batch-wise, а не всех NPC на каждом render tick;
- ограничивать `maxRelationships`, history length и event payload;
- кешировать environment facts на simulation interval;
- ограничивать XP per command и проверять server-side actor;
- никогда не принимать `CitizenId`, amount XP или relationship delta от клиента без authorization;
- проверять `WorldId` во всех position/path queries;
- сохранять idempotency markers для packet retries;
- не раскрывать полную социальную/health model игроку без permission.

## 19. Связи с другими документами

- [`system-overview.md`](system-overview.md) — контейнеры, общие порты, server authority и базовые use cases.
- [`bounded-contexts.md`](bounded-contexts.md) — границы `NPCContext`, aggregate ownership, identifiers и event-based interaction.
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — driving/driven ports, DI, adapters и Core isolation.
- [`data-dictionaries.md`](data-dictionaries.md) — схемы `traits.json`, `skills.json`, `npc-names.json`, `npc-settings.json` и cross-reference rules.
- `module-colony-manager.md` — интеграция при добавлении/удалении жителей; в текущей документации аналогичный Colony-модуль может называться `module-colony.md`.
- `module-storyteller.md` — использование NPC events для генерации сюжета и adaptive pressure.
- `module-goal-ai.md` — связь `CurrentTask`, pathfinding и execution intents с системой целей.
- `persistence.md` — сохранение Citizen snapshots, NBT mapping и migrations.
- `testing-strategy.md` — test pyramid, fixtures, contract tests и CI.
- `archunit-rules.md` — полный executable набор architectural rules.

## 20. Итоговая модель NPC Core

```text
JSON snapshots -> Config repositories -> CitizenFactory / Policies
                                      |
Minecraft tick -> NPC use cases -> Citizen aggregate
                                      |
                      needs / mood / skills / relationships
                                      |
                              domain events
                         /          |           \
                    Colony     Storyteller      UI/projections

Minecraft world/entity/save <-> driven ports <-> Core-friendly DTOs
```

Главное правило модуля: NPC Core владеет смыслом индивидуального состояния, но не знает, как Minecraft отображает или физически перемещает NPC. Новые traits, skills, social rules, persistence backends и adapters должны добавляться через конфигурации, стратегии, порты и события, сохраняя границы `core.npc` и независимость от Minecraft API.
