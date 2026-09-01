# RimWorldCraft — Configuration Mutation Testing

## 1. Введение

Конфигурационное мутационное тестирование проверяет, что система не только принимает корректный JSON, но и предсказуемо реагирует на изменения, ошибки, крайние значения и несовместимые ссылки. Для RimWorldCraft это критично: дизайнер может изменить JSON без перекомпиляции Java-кода, поэтому обычные compile-time проверки не обнаружат отрицательный weight, удалённый `id` или prefab размером `0x0x0`.

Документ дополняет [`data-dictionaries.md`](data-dictionaries.md), [`testing-strategy.md`](testing-strategy.md), [`archunit-rules.md`](archunit-rules.md) и [`drift-detection-pipeline.md`](drift-detection-pipeline.md). Он описывает:

- JSON Schema validation;
- parser/mapper tests;
- controlled mutations;
- integration tests изменённой конфигурации;
- fallback/regeneration;
- CI execution и reports.

### 1.1 Цели

- обнаруживать schema и semantic defects до загрузки мира;
- доказывать, что invalid config не приводит к silent corruption;
- проверять влияние допустимых изменений на domain behavior;
- проверять defaults, logging и fallback;
- сохранять баланс при изменении весов, costs и difficulty;
- давать дизайнерам быстрый feedback.

## 2. Общая стратегия

```text
JSON fixture
  -> syntax parse
  -> JSON Schema validation
  -> semantic/cross-reference validation
  -> mutation generation
  -> parser/config snapshot
  -> domain integration scenario
  -> expected accept/reject/fallback/log assertion
  -> CI report
```

### 2.1 Виды проверок

| Вид | Что проверяет | Пример |
|---|---|---|
| Schema validation | structure/types/required/enum/ranges | `weight` number, `traits[].id` required |
| Parser test | JSON → Java DTO/snapshot | `TraitDefinition` populated |
| Semantic validation | cross-file/reference/domain rules | trait skill exists |
| Mutation test | реакцию на изменённое значение | weight `10 → -100` |
| Integration test | применение snapshot в Core | Storyteller selection changes |
| Boundary test | min/max/empty/missing | size `0`, 1000 NPC |
| Regeneration test | missing/corrupt file fallback | default config restored |

### 2.2 Категории ожидаемого результата

Каждая мутация классифицируется заранее:

- **accepted:** допустимое изменение меняет behavior предсказуемо;
- **rejected:** schema/semantic validator отклоняет input;
- **clamped:** значение ограничивается documented range;
- **ignored:** unknown optional field не влияет на domain;
- **fallback:** активируется last-known-good/default snapshot;
- **fatal:** критичный config без fallback останавливает загрузку модуля.

Нельзя писать тест «не должно упасть» без проверки конечного результата и diagnostics.

## 3. JSON Schema validation

### 3.1 Расположение схем

```text
config/schemas/
  colony-settings-schema.json
  traits-schema.json
  skills-schema.json
  story-events-schema.json
  story-arcs-schema.json
  storyteller-settings-schema.json
  prefabs-schema.json
  npc-names-schema.json
  npc-settings-schema.json
  goal-settings-schema.json
  actions-schema.json
  event-settings-schema.json
```

Runtime content:

```text
config/rwc/
  colony/colony-settings.json
  npc/traits.json
  npc/skills.json
  npc/npc-names.json
  npc/npc-settings.json
  storyteller/story-events.json
  building/prefabs.json
  goal/goal-settings.json
  goal/actions.json
  events/event-settings.json
```

### 3.2 Пример `traits-schema.json`

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "rwc://schemas/npc/traits.schema.json",
  "type": "object",
  "required": ["$schema", "version", "traits"],
  "additionalProperties": false,
  "properties": {
    "$schema": { "type": "string" },
    "version": { "type": "integer", "minimum": 1 },
    "traits": {
      "type": "array",
      "items": { "$ref": "#/$defs/trait" },
      "uniqueItems": true
    }
  },
  "$defs": {
    "trait": {
      "type": "object",
      "required": ["id", "displayKey", "descriptionKey", "moodModifier"],
      "additionalProperties": false,
      "properties": {
        "id": {
          "type": "string",
          "pattern": "^[a-z][a-z0-9_:-]{1,63}$"
        },
        "displayKey": { "type": "string", "minLength": 1 },
        "descriptionKey": { "type": "string", "minLength": 1 },
        "moodModifier": { "type": "integer", "minimum": -100, "maximum": 100 },
        "skillModifiers": {
          "type": "object",
          "additionalProperties": { "type": "integer", "minimum": -100, "maximum": 100 }
        },
        "conflicts": {
          "type": "array",
          "items": { "type": "string" },
          "uniqueItems": true
        }
      }
    }
  }
}
```

JSON Schema не всегда может выразить уникальность property `id` среди array objects или cross-file references. Такие проверки выполняет `SemanticConfigValidator`.

### 3.3 Библиотека

Допустимы Everit или NetworkNT JSON Schema validator; выбирается одна библиотека и фиксируется в Gradle. Jackson/Gson отвечает за parsing/mapping, но не заменяет schema validation.

```java
public final class SchemaValidator {
    private final JsonSchemaFactory factory;

    public ValidationReport validate(String json, String schemaResource) {
        JsonNode document = objectMapper.readTree(json);
        JsonSchema schema = factory.getSchema(load(schemaResource));
        schema.validate(document);
        return ValidationReport.valid();
    }
}
```

## 4. Тестирование parser и semantic validation

### 4.1 Позитивный parser test

```java
class TraitConfigParserTest {
    private final TraitConfigParser parser = new TraitConfigParser(
            new ObjectMapper(), new SchemaValidator(), new TraitSemanticValidator());

    @Test
    void parse_ShouldMapValidTraitConfig() {
        String json = Resources.read("config/valid/traits.json");

        TraitsSnapshot snapshot = parser.parse(json);

        assertThat(snapshot.traits()).extracting(TraitDefinition::id)
                .contains("rwc:optimist", "rwc:workaholic");
        assertThat(snapshot.find("rwc:optimist").moodModifier()).isEqualTo(6);
    }
}
```

### 4.2 Parameterized negative tests

```java
@ParameterizedTest(name = "rejects {0}")
@CsvSource({
        "traits-missing-id.json",
        "traits-invalid-mood.json",
        "traits-duplicate-id.json",
        "traits-unknown-skill.json",
        "traits-invalid-version.json"
})
void parse_ShouldRejectInvalidConfig(String fixture) {
    assertThatThrownBy(() -> parser.parse(Resources.read("config/invalid/" + fixture)))
            .isInstanceOf(ConfigurationException.class);
}
```

### 4.3 Cross-reference validation

```java
@Test
void trait_ShouldRejectUnknownSkillReference() {
    String json = Resources.read("config/invalid/traits-unknown-skill.json");

    assertThatThrownBy(() -> parser.parse(json))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("unknown skill");
}
```

## 5. Мутации конфигураций

### 5.1 Определение

Мутация — контролируемое изменение одной части валидного fixture. Мутатор должен создавать копию во временном test directory или memory tree и никогда не изменять canonical config.

### 5.2 Mutation matrix

| Категория | Мутация | Ожидаемый результат |
|---|---|---|
| Number valid | `weight 10 → 100` | accepted, selection distribution changes |
| Number invalid | `weight 10 → -100` | reject или clamp to 0 |
| Boundary | `maxActive 4 → 0` | reject/fallback |
| Required field | remove `id` | reject/fallback + error log |
| Type | number → string | schema rejection |
| Unknown field | add `designerNote` | ignore only if schema allows extensions |
| Reference | trait ID unknown | semantic rejection |
| Duplicate | duplicate `id` | semantic rejection |
| Empty | empty traits/events list | accepted only if context policy permits |
| Structural | prefab size `0x0x0` | domain validation failure |
| Overflow | count `Integer.MAX_VALUE` | reject bounded resource |
| Compatibility | old `version` | migration to current |

### 5.3 `ConfigMutator`

```java
public final class ConfigMutator {
    private final ObjectMapper mapper;

    public String removeRequiredField(String json, String path) throws JsonProcessingException {
        ObjectNode root = (ObjectNode) mapper.readTree(json);
        removeAt(root, path);
        return mapper.writeValueAsString(root);
    }

    public String replaceNumber(String json, String path, BigDecimal value)
            throws JsonProcessingException {
        ObjectNode root = (ObjectNode) mapper.readTree(json);
        nodeAt(root, path).set("", DecimalNode.valueOf(value));
        return mapper.writeValueAsString(root);
    }
}
```

Вместо хрупкого string replacement использовать JSON tree/path mutation. Реальная реализация должна корректно обрабатывать array indices и проверять, что изменён именно один target.

### 5.4 Mutation case contract

```java
public record ConfigMutationCase(
        String name,
        String sourceFixture,
        UnaryOperator<String> mutation,
        MutationExpectation expectation
) {}

public enum MutationExpectation {
    ACCEPTED,
    REJECTED,
    CLAMPED,
    FALLBACK,
    IGNORED,
    FATAL
}
```

## 6. Примеры мутационных тестов

### 6.1 Удаление обязательного `traits[].id`

```java
@Test
void missingTraitId_ShouldUseFallbackAndLogError() {
    String original = Resources.read("config/valid/traits.json");
    String mutated = mutator.removeRequiredField(original, "traits[0].id");

    ConfigLoadResult result = loader.load(mutated, ConfigKey.of("npc", "traits"));

    assertThat(result.status()).isEqualTo(ConfigLoadStatus.FALLBACK);
    assertThat(result.snapshot()).isEqualTo(defaultConfigs.traits());
    assertThat(logCapture.messages())
            .anyMatch(message -> message.contains("traits[0].id"));
}
```

Если политика проекта считает traits config critical, expectation меняется на `FATAL`: модуль не стартует, а default snapshot не применяется. Это должно быть единообразно документировано.

### 6.2 Отрицательный weight в `story-events.json`

```java
@Test
void negativeIncidentWeight_ShouldBeRejectedOrClampedToZero() {
    String original = Resources.read("config/valid/story-events.json");
    String mutated = mutator.replaceNumber(
            original, "events[0].weight", BigDecimal.valueOf(-100));

    ConfigLoadResult result = loader.load(mutated,
            ConfigKey.of("storyteller", "story-events"));

    assertThat(result.status()).isIn(
            ConfigLoadStatus.REJECTED, ConfigLoadStatus.CLAMPED);
    StoryEventsSnapshot snapshot = result.effectiveSnapshot();
    assertThat(snapshot.event("raider_wave").weight()).isGreaterThanOrEqualTo(0);

    Storyteller storyteller = StorytellerFixtures.with(snapshot);
    assertThatCode(() -> storyteller.evaluateNextIncident())
            .doesNotThrowAnyException();
}
```

Ожидаемый контракт должен выбрать один вариант: строгий reject или clamp. В production не допускается неявное смешение этих политик.

### 6.3 Prefab `0x0x0`

```java
@Test
void zeroSizedPrefab_ShouldBeRejectedAndBuildOrderNotCreated() {
    String original = Resources.read("config/valid/prefabs.json");
    String mutated = mutator.replaceNumbers(original, Map.of(
            "prefabs[0].size.width", 0,
            "prefabs[0].size.height", 0,
            "prefabs[0].size.depth", 0));

    Blueprint blueprint = prefabLoader.load(mutated).blueprint("oak_house");

    assertThatThrownBy(() -> blueprintValidator.validate(blueprint))
            .isInstanceOf(BlueprintValidationException.class)
            .hasMessageContaining("size");
    verifyNoInteractions(buildOrderRepository);
}
```

### 6.4 Допустимое изменение traits

```java
@Test
void increasedOptimistMoodModifier_ShouldAffectMoodPredictably() {
    String original = Resources.read("config/valid/traits.json");
    String mutated = mutator.replaceNumber(
            original, "traits[0].moodModifier", BigDecimal.valueOf(12));

    TraitsSnapshot snapshot = traitLoader.load(mutated);
    Citizen citizen = CitizenFixtures.withTrait("rwc:optimist");

    Mood mood = moodService.calculate(citizen,
            MoodContext.withTraits(snapshot));

    assertThat(mood.value()).isEqualTo(MoodFixtures.expectedWithOptimist(12));
}
```

### 6.5 Несуществующая ссылка

```java
@Test
void unknownRequiredTraitReference_ShouldRejectWholeSnapshot() {
    String original = Resources.read("config/valid/story-events.json");
    String mutated = mutator.replaceString(
            original, "events[0].conditions.requiredTraits[0]", "rwc:missing");

    assertThatThrownBy(() -> storyEventLoader.load(mutated))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("rwc:missing");
}
```

## 7. Интеграционные мутации по контекстам

### 7.1 Storyteller weights

Тест должен фиксировать seed и population:

1. загрузить baseline snapshot;
2. выполнить много выборов с deterministic random или controlled distribution;
3. мутировать один weight;
4. проверить distribution/eligibility, а не конкретное случайное событие без seed;
5. убедиться, что cooldown/conditions остаются рабочими.

Для статистического теста задаётся tolerance, например 95% confidence interval, чтобы не сделать flaky test.

### 7.2 NPC traits

Проверить:

- valid modifier меняет mood/skill/social result;
- missing optional traits даёт empty set;
- unknown trait reference отклоняется;
- conflict pair не может попасть в один citizen;
- extreme modifier clamped/rejected согласно schema.

### 7.3 Resources и values

Проверить:

- resource amount `0` и max bound;
- negative resource value rejected или normalized according to policy;
- unknown resource ID не создаёт phantom resource;
- Colony value calculator не выдаёт negative wealth;
- Storyteller receives updated summary only after valid snapshot.

### 7.4 Prefabs

Проверить:

- valid size and layers;
- empty layer policy;
- oversized blueprint rejected before world mutation;
- unknown block type rejected unless optional content fallback;
- resource references exist;
- changed estimated time affects UI/calculation predictably.

### 7.5 Goal/actions

Проверить:

- action with negative cost rejected;
- missing effect makes plan impossible, not successful;
- unknown precondition is rejected;
- max duration/plan length enforced;
- changing cost changes selected plan without breaking execution.

## 8. Крайние значения

### 8.1 Минимум

- `0` weight, count, duration, threshold;
- empty list of traits/events/resources;
- empty string IDs/names;
- zero-size prefab;
- no active incidents;
- missing optional field.

Каждое значение классифицируется: valid no-op, reject или fallback.

### 8.2 Максимум

- 1000 NPC;
- maximum allowed event queue;
- prefab near configured block limit;
- `Integer.MAX_VALUE` count/experience;
- 100% mood/need and level 100;
- maximum plan depth/waypoints.

Проверять не только validation, но и отсутствие OOM, integer overflow и server tick starvation.

### 8.3 Некорректные ссылки

Mutation generator создаёт random unknown IDs для:

- `traitId`;
- `skillId`;
- `resourceId`;
- `blueprintId`;
- `eventId`;
- `actionId`;
- `climateId`.

Ожидаемое поведение — explicit configuration error до публикации snapshot.

## 9. Регенерация конфигов

### 9.1 Missing/corrupt files

При запуске/reload:

1. проверить наличие required file;
2. прочитать JSON;
3. schema + semantic validate;
4. при успехе atomic publish;
5. при отсутствии — materialize default resource;
6. при corrupt — сохранить diagnostic/quarantine и применить documented fallback;
7. залогировать file, path, error и effective source;
8. уведомить operator через server log/diagnostics.

### 9.2 Regeneration test

```java
@Test
void missingConfig_ShouldRegenerateDefaultAndLogWarning() {
    configDirectory.delete("npc/traits.json");

    ConfigLoadResult result = bootstrap.loadAll();

    assertThat(result.effective("npc", "traits")).isEqualTo(defaultConfigs.traits());
    assertThat(configDirectory.exists("npc/traits.json")).isTrue();
    assertThat(logCapture.messages())
            .anyMatch(message -> message.contains("missing")
                    && message.contains("npc/traits.json"));
}
```

### 9.3 Fallback hierarchy

```text
valid user config
  -> last-known-good snapshot
  -> packaged default config
  -> empty safe snapshot (only for optional config)
  -> fail module/world startup (critical config)
```

Нельзя использовать старый snapshot как fallback бесконечно без warning и version visibility.

## 10. Логирование и отчёты

Каждая config error содержит:

- logical `ConfigKey` и physical path;
- JSON path (`events[0].weight`);
- schema/semantic reason;
- source version;
- effective fallback (`last-known-good`, `packaged-default`, `empty`, `fatal`);
- config correlation/reload ID.

Пример:

```text
WARN Config npc/traits rejected at traits[0].id:
required field is missing; using packaged default snapshot; reloadId=...
```

Не логировать полные пользовательские save/config files, если это создаёт лишний объём или раскрывает private data.

## 11. Инструменты и Gradle-задачи

Рекомендуемые задачи:

```text
./gradlew configTest
./gradlew configSchemaTest
./gradlew configMutationTest
./gradlew configMutationTest -PmutationSet=smoke
./gradlew configMutationTest -PmutationSet=full
./gradlew migrationTest
```

Компоненты:

- JSON Schema validator: Everit или NetworkNT;
- Jackson/Gson parser согласно project setup;
- JUnit 5 + AssertJ + Mockito;
- custom `ConfigMutator`;
- Cucumber для cross-context behavior;
- JaCoCo;
- Gradle test fixtures;
- `jsonschema2pojo` допустим как генератор DTO, если принят проектом, но domain model не должна зависеть от generated schema classes.

### 11.1 Pitest

Pitest полезен для mutation testing Java-кода, но не заменяет config mutator. Его можно запускать вместе с config mutation suite:

```text
Pitest: убивает мутантов в validators/services
ConfigMutator: изменяет JSON input и проверяет runtime policy
```

## 12. CI/CD интеграция

### 12.1 Pull Request

Быстрый набор:

- schema validation всех canonical configs;
- parser tests;
- semantic validation;
- smoke mutation set: missing required, wrong type, negative number, unknown reference;
- selected integration scenarios;
- reports as artifacts.

### 12.2 Release candidate

Полный набор:

- все mutation operators для всех configs;
- boundary and overflow tests;
- full context integration;
- save/config reload;
- migration matrix;
- performance of parsing/reload;
- generated mutation report attached to PR/release.

### 12.3 Nightly

- random/property-based mutations;
- mutation combinations (ограниченно, чтобы избежать комбинаторного взрыва);
- 1000 NPC/config reload stress;
- Storyteller distribution tests;
- long-running reload stability;
- corrupt/missing file regeneration;
- full historical fixtures.

### 12.4 Gradle example

```groovy
tasks.register('configMutationTest', Test) {
    description = 'Runs configuration mutation tests'
    group = 'verification'
    useJUnitPlatform()
    systemProperty 'mutationSet', project.findProperty('mutationSet') ?: 'smoke'
    include '**/*ConfigMutationTest.class'
}

tasks.named('check') {
    dependsOn tasks.named('configSchemaTest')
    dependsOn tasks.named('configMutationTest')
}
```

Пример GitHub Actions step:

```yaml
- name: Configuration schema and smoke mutations
  run: ./gradlew configSchemaTest configMutationTest -PmutationSet=smoke --no-daemon

- name: Upload config mutation report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: config-mutations-${{ github.run_id }}
    path: build/reports/config-mutations/**
```

## 13. Отчёт mutation testing

```markdown
# Configuration Mutation Report

| Config | Mutation | Expected | Actual | Result |
|---|---|---|---|---|
| traits.json | remove traits[0].id | FALLBACK | FALLBACK | PASS |
| story-events.json | weight=-100 | REJECTED | REJECTED | PASS |
| prefabs.json | size=0 | REJECTED | REJECTED | PASS |
| npc-settings.json | maxRelationships=0 | ACCEPTED | ACCEPTED | PASS |

Killed mutations: 42/42
Survived mutations: 0
Fallback assertions: 8/8
Cross-reference assertions: 17/17
```

«Killed» означает, что тест обнаружил и корректно классифицировал изменение. «Survived» требует анализа: mutation может быть допустимой, но чаще означает недостаток теста.

## 14. Связь с DoD

Mutation suite подтверждает критерии [`definition-of-done-do-d.md`](definition-of-done-do-d.md):

- configs valid and parsable;
- changed config does not cause uncontrolled crash;
- invalid values rejected/clamped/fallback according to policy;
- errors logged with path and reason;
- default regeneration works;
- cross-references remain valid;
- behavior change is intentional and tested;
- migration compatibility preserved.

Config change не считается Done, если canonical parser test проходит, но соответствующая mutation остаётся необъяснённой.

## 15. Эволюция практики

### 15.1 Новые mutation operators

Добавлять mutation operator при появлении дефекта:

1. сохранить production incident как fixture;
2. добавить minimal mutation;
3. определить expected policy;
4. добавить test и report;
5. включить operator в smoke/full set.

### 15.2 Diff-aware mutations

В будущем mutator может анализировать Git diff:

- если изменился `weight`, создать boundary/negative/large mutations;
- если изменился reference, создать unknown/duplicate mutations;
- если изменился schema field, создать missing/type/old-version mutations;
- если изменился prefab size, создать zero/maximum/overflow mutations.

Это уменьшает runtime PR suite, но nightly всё равно запускает полный matrix.

### 15.3 Property-based testing

Для числовых ranges и collections допустимо генерировать values property-based способом:

```text
for all valid weights >= 0:
  selection does not throw
for all invalid negative weights:
  effective weight >= 0 or config is rejected
for all migrated documents:
  current schema validation passes
```

### 15.4 Комбинированные мутации

Комбинированные mutation sets (`weight + unknown reference`) запускаются только в release/nightly, потому что они быстро увеличивают количество тестов. Каждая комбинация должна иметь понятную ожидаемую policy.

## 16. FAQ

### Нужно ли использовать Pitest для JSON?

Нет. Pitest тестирует Java-код. Для JSON нужен структурный `ConfigMutator`; Pitest дополняет его проверкой validators/services.

### Должна ли любая ошибка конфигурации останавливать игру?

Нет. Критичность определяется конфигом и контекстом. Optional UI/content config может использовать default, а critical simulation config — остановить загрузку мира. Политика должна быть документирована.

### Можно ли просто игнорировать неизвестные поля?

Только если schema явно допускает extensions/additional properties и это безопасно. Для typo в обязательном поле strict rejection предпочтительнее.

### Как тестировать случайность Storyteller?

Фиксировать seed или использовать controlled random. Для distribution tests задавать tolerance и достаточное число выборок; не проверять случайный единичный результат без deterministic setup.

### Что делать с survived mutation?

Проверить, была ли мутация допустимой. Если нет — добавить тест, который отличает baseline от mutated behavior. Если допустима — зафиксировать expected policy и не считать её ошибкой.

### Нужно ли мутировать конфиги в каждом PR?

Smoke set — да. Полный набор — release/nightly, если runtime дорогой. Изменённые конфиги получают targeted full mutation set.

## 17. Связи с другими документами

- [`data-dictionaries.md`](data-dictionaries.md) — canonical JSON schemas, fields, ranges и cross-references.
- [`testing-strategy.md`](testing-strategy.md) — общая пирамида тестов, JUnit, Cucumber, migrations и performance.
- [`archunit-rules.md`](archunit-rules.md) — архитектурные правила конфигурационных loaders.
- [`drift-detection-pipeline.md`](drift-detection-pipeline.md) — CI/CD stages, reports и artifacts.
- [`definition-of-done-do-d.md`](definition-of-done-do-d.md) — DoD config/documentation/testing requirements.
- [`module-npc-core.md`](module-npc-core.md) — traits, skills, needs и NPC config usage.
- [`module-storyteller.md`](module-storyteller.md) — incident weights, arcs и storyteller settings.
- [`module-building-system.md`](module-building-system.md) — prefabs, block costs и building settings.
- [`module-goal-ai.md`](module-goal-ai.md) — goal/action configuration.
- [`save-serialization.md`](save-serialization.md) — schema migrations и corrupt save recovery.

## 18. Итоговый checklist

- [ ] Все canonical JSON прошли schema validation.
- [ ] Parser tests проверяют mapping в immutable snapshots.
- [ ] Semantic validator проверяет уникальность и cross-file references.
- [ ] Для изменённых конфигов запущен targeted mutation set.
- [ ] Missing fields/types/negative/range/reference mutations классифицированы.
- [ ] Fallback/default/regeneration проверены.
- [ ] Logs содержат file и JSON path.
- [ ] Storyteller/NPC/Building/Goal integration behavior проверен.
- [ ] Migration fixtures проходят.
- [ ] Mutation report приложен к CI artifacts.
- [ ] Survived mutations объяснены или устранены.
- [ ] Документация и DoD обновлены.
