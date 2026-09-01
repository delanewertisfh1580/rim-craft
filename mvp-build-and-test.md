# RimWorldCraft — MVP Build, Launch and Test Runbook

## 1. Назначение

Этот документ описывает сборку, запуск и ручную проверку MVP RimWorldCraft после этапов `core-api`, `core-impl`, `infrastructure-common` и `infrastructure-forge`.

Важно: текущий репозиторий содержит platform-neutral Forge integration seams. Реальные `ForgeGradle`, Minecraft mappings, Forge `Level`/`LivingEntity`/`CompoundTag` и Baritone runtime должны быть подключены отдельным platform wiring change. Поэтому команды ниже разделены на **текущий JVM skeleton** и **полный Forge runtime**.

## 2. Поддерживаемая матрица

| Компонент | MVP baseline |
|---|---|
| Java | JDK 17 |
| Gradle | Gradle 8.x или committed Gradle Wrapper |
| Minecraft | 1.20.1 |
| Forge | 47.2.0 (после настройки ForgeGradle) |
| Модули текущего build | `core-api`, `core-impl`, `infrastructure-common` |
| Forge module | compile-safe seam; actual Forge dependency pending mappings setup |
| Persistence | JSON file adapter in `infrastructure-common`; NBT adapter is future Forge wiring |
| Pathfinding | direct-route seam; Baritone backend is future wiring |

## 3. Подготовка окружения

### 3.1 JDK

Установить JDK 17 и проверить:

```bash
java -version
javac -version
```

Ожидается Java 17. Не смешивать JDK системного Gradle и IDE: IntelliJ должна использовать тот же toolchain.

### 3.2 Gradle

Сначала использовать wrapper:

```bash
./gradlew --version
```

Если wrapper ещё не добавлен:

```bash
gradle --version
```

В текущем skeleton wrapper отсутствует, поэтому до первого коммита рекомендуется добавить стандартные `gradlew`, `gradlew.bat` и `gradle/wrapper/gradle-wrapper.properties` через Gradle 8.x.

### 3.3 IntelliJ IDEA

1. Открыть корень репозитория как Gradle project.
2. Выбрать Gradle JVM = JDK 17.
3. Включить auto-reload Gradle project.
4. Убедиться, что source sets распознаны:
   - `core-api/src/main/java`;
   - `core-impl/src/main/java`;
   - `infrastructure-common/src/main/java`;
   - `infrastructure-forge/src/main/java` после его включения.
5. Не добавлять Minecraft classes вручную в Core classpath.

## 4. Конфигурация Gradle

### 4.1 Текущий JVM skeleton

Текущий `settings.gradle` включает:

```text
core-api
core-impl
infrastructure-common
```

Проверка задач:

```bash
./gradlew projects
./gradlew tasks --all
```

Сборка без запуска Minecraft:

```bash
./gradlew clean build --no-daemon
```

Целевые проверки:

```bash
./gradlew :core-api:build --no-daemon
./gradlew :core-impl:test --no-daemon
./gradlew :infrastructure-common:test --no-daemon
```

### 4.2 Full Forge runtime profile

Перед подключением Forge необходимо:

1. выбрать точную ForgeGradle версию и mappings;
2. добавить ForgeGradle plugin в `pluginManagement`/root build;
3. настроить `minecraft { mappings ...; runs { client {}; server {} } }`;
4. включить `infrastructure-forge` в `settings.gradle`;
5. заменить compile-safe `Object` seams на реальные Forge types;
6. подключить Forge dependency и client/common source separation;
7. подключить Baritone/Automatone с совместимой версией mappings;
8. определить jar manifest/mod metadata (`mods.toml`);
9. добавить Forge integration tasks.

Пример будущей зависимости, после подтверждения repository/artifact availability:

```groovy
dependencies {
    minecraft "net.minecraftforge:forge:${minecraftVersion}-${forgeVersion}"
}
```

Не объявлять неизвестный Baritone coordinate вслепую: проверить artifact, mappings и license в отдельном integration change.

### 4.3 Packaging

На текущем этапе каждый Java-модуль создаёт собственный library JAR. Это не готовый distributable Forge mod JAR. Для runtime packaging потребуется один из вариантов:

- ForgeGradle `reobfJar`/`reobf` pipeline;
- отдельный platform module, который содержит mod entrypoint;
- Shadow только для библиотек, если это разрешено Forge runtime и лицензиями.

Не собирать `core-api` и `core-impl` в fat jar без проверки duplicate classes и loader classpath.

## 5. Конфигурационные файлы

Перед запуском подготовить runtime directory:

```text
config/rimworldcraft/
├── traits.json
├── story-events.json
├── colony-settings.json
├── npc-names.json
├── prefabs.json
├── goal-settings.json
├── building-settings.json
├── pathfinder-settings.json
└── event-settings.json
```

Существующие примеры находятся в `infrastructure-common/src/main/resources/config/rwc/`. При полном Forge wiring выбрать один canonical path (`config/rimworldcraft/` согласно runtime convention) и не дублировать источники истины.

Минимальные требования:

- каждый JSON имеет `schemaVersion`;
- IDs стабильны и namespaced;
- веса и количества не отрицательны;
- ссылки между конфигами проверяются до публикации snapshot;
- повреждённый файл не должен частично менять активную конфигурацию;
- fallback документируется в логе.

Пример `pathfinder-settings.json`:

```json
{
  "schemaVersion": 1,
  "allowBreak": false,
  "allowPlace": false,
  "allowParkour": false,
  "primaryTimeoutMs": 5000,
  "maxFallHeight": 3,
  "maxJumpHeight": 1
}
```

Пример `event-settings.json`:

```json
{
  "schemaVersion": 1,
  "synchronousDelivery": true,
  "maxQueueSize": 2048,
  "retryCount": 2
}
```

## 6. Логирование

### 6.1 JVM skeleton

`infrastructure-common` использует SLF4J API. На runtime classpath должен быть один binding, предоставляемый launcher/Forge logging setup.

Не использовать:

```java
System.out.println("debug");
exception.printStackTrace();
```

Использовать:

```java
private static final Logger LOG = LoggerFactory.getLogger(MyAdapter.class);

LOG.info("RimWorldCraft infrastructure initialized");
LOG.debug("Saving colony {}", colonyId);
LOG.error("Cannot load colony {}", colonyId, exception);
```

### 6.2 Forge runtime

Для Forge:

- `INFO` — startup, registration, world lifecycle;
- `DEBUG` — sync revisions, path requests, config decisions;
- `WARN` — fallback, stale packet, missing optional content;
- `ERROR` — corrupted save, failed registration, unrecoverable adapter error.

Не логировать secrets, tokens, полные пользовательские save payloads или чувствительные player data.

## 7. Запуск из IntelliJ IDEA

### 7.1 Forge Client

После настройки ForgeGradle:

1. Выполнить `./gradlew genIntellijRuns`.
2. Перезагрузить Gradle project.
3. Выбрать generated `Minecraft Client` run configuration.
4. Проверить working directory: Forge run directory, обычно `run/`.
5. Указать VM options только из безопасного generated configuration.
6. Запустить client.

Ожидаемый startup:

```text
RimWorldCraft mod initialized
Entity registrations completed
Configuration validation completed
```

### 7.2 Forge Server

1. Выполнить `./gradlew runServer` после настройки run task.
2. Принять EULA локально в соответствии с правилами Minecraft test environment.
3. Запустить dedicated server.
4. Подключить клиент с тем же mod set/version.
5. Проверить отсутствие registry/network mismatch.

Не запускать dedicated server с production saves. Использовать отдельный test world.

### 7.3 Командная строка

Будущий запуск после Forge wiring:

```bash
./gradlew runClient
./gradlew runServer
```

Текущий skeleton запускает только JVM tests/build, а не Minecraft:

```bash
./gradlew :core-api:build
./gradlew :core-impl:test
./gradlew :infrastructure-common:test
```

## 8. Подключение удалённого отладчика

### Client/server JDWP

Для локального debug run добавить в IDE-generated configuration:

```text
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

Рекомендуемые порты:

| Процесс | Порт |
|---|---:|
| Forge client | 5005 |
| Forge server | 5006 |

В IntelliJ создать `Remote JVM Debug` с соответствующим портом. Не открывать JDWP наружу и не использовать `suspend=n` на shared server.

### Что проверять debugger’ом

- `RimWorldCraftMod` composition root;
- entity binding `CitizenId` ↔ runtime UUID;
- server-side interaction handler;
- repository load/save boundary;
- event publication;
- pathfinding request/cancellation;
- projection revision and stale packet rejection.

## 9. MVP smoke-тесты

### Smoke 0: загрузка мода

**Шаги:**

1. Запустить client/server test profile.
2. Проверить mod list и version.
3. Проверить startup log.
4. Убедиться, что нет `NoSuchMethodError`, missing registry или mapping errors.

**Ожидание:** мод загружен, конфиги прочитаны, критических ошибок нет.

### Smoke 1: создание колонии и спавн NPC

**Предусловия:** новый test world, включён debug command/GUI.

**Шаги:**

1. Выполнить `/spawncitizen` или использовать debug GUI после реализации command adapter.
2. Создать колонию через соответствующий use case.
3. Проверить имя, `citizenId`, стартовые needs/skills и position.
4. Проверить, что NPC виден в server и client.
5. Проверить лог `Citizen` binding.

**Ожидание:** создаётся ровно один NPC, Core ID сохраняется, entity не дублируется.

### Smoke 2: движение NPC

**Шаги:**

1. Выбрать NPC.
2. Назначить задачу «идти в точку» через command/GUI.
3. Указать reachable target.
4. Наблюдать server movement и client interpolation.
5. Повторить на препятствии/лестнице после подключения реального pathfinder.

**Ожидание:** Goal AI создаёт intent, pathfinder возвращает путь, entity выполняет движение на server. Client не телепортирует NPC самостоятельно.

**Текущий skeleton:** `BaritonePathfinderAdapter` возвращает маршрут только для совпадающих start/target; полноценное движение требует Forge/Baritone wiring.

### Smoke 3: взаимодействие

**Шаги:**

1. ПКМ по NPC.
2. Проверить, что пакет/событие приходит на server.
3. Проверить permission, distance и target validation.
4. Открыть диалог или trade UI после реализации interaction adapter.
5. Повторить с неавторизованным игроком в multiplayer test.

**Ожидание:** действие выполняется только server-side, invalid packet отклоняется, Core получает typed command.

### Smoke 4: сохранение и загрузка

**Шаги:**

1. Создать NPC и изменить mood/task/position.
2. Сохранить world.
3. Полностью остановить client/server.
4. Запустить снова и загрузить world.
5. Проверить `citizenId`, position и Core state.
6. Проверить логи migration/fallback.

**Ожидание:** NPC не дублируется, identity сохраняется, повреждённый save не уничтожает рабочий мир.

**Текущий skeleton:** JSON marker persistence доступен через `JsonSaveAdapter`; полноценный Forge NBT round-trip ещё не подключён.

### Smoke 5: базовая экономика

**Шаги:**

1. Добавить WOOD/STONE в colony через debug command или fixture.
2. Проверить repository/domain state.
3. Проверить `ColonyValueCalculator`.
4. Отобразить значение в GUI/HUD после его подключения.
5. Создать build order и зарезервировать ресурсы.

**Ожидание:** отрицательные количества отклоняются, недостаток ресурсов даёт domain error, значение обновляется предсказуемо.

## 10. Multiplayer smoke-тесты

| Тест | Ожидание |
|---|---|
| Два клиента видят NPC | одинаковая server projection |
| Mood/task update | только новый revision принимается |
| C2S interaction | server проверяет ownership/range |
| Неверный citizen ID | packet отклонён |
| Reconnect | entity rebind по `citizenId` |
| Save/load | один aggregate/entity без дублей |
| Client-only attempt | состояние server не изменяется |

Записывать:

- server log;
- client log каждого клиента;
- packet/revision diagnostics в debug mode;
- seed и координаты test world;
- build/mod versions.

## 11. Быстрый troubleshooting

### `ClassNotFoundException` или `NoSuchMethodError`

Проверить:

- несовпадающие Forge/Minecraft mappings;
- duplicate dependency versions;
- не тот Java runtime;
- неправильно упакованный fat jar;
- stale Gradle cache после изменения mappings.

Исправлять dependency/build configuration, не обходить ошибку случайным удалением runtime файлов.

### `CitizenNotFoundException`

Означает, что adapter/use case запросил неизвестный `citizenId`.

Проверить:

1. правильный `WorldId`/world scope;
2. был ли aggregate загружен до entity binding;
3. не устарел ли packet;
4. не был ли citizen удалён из repository;
5. не потерялся ли `citizenId` при NBT/JSON migration.

Ожидаемое поведение: понятный ERROR/WARN, controlled despawn/quarantine, отсутствие автоматического дублирования NPC.

### Путь не найден

Проверить:

- target reachable;
- traversal context;
- timeout settings;
- Baritone initialization;
- server thread;
- dynamic doors/obstacles;
- fallback policy.

Путь не найден — нормальный domain result, а не причина для teleport без разрешения.

### Конфиг отклонён

Проверить:

- JSON syntax;
- `schemaVersion`;
- required IDs/fields;
- numeric ranges;
- cross-file references;
- fallback log.

### Save не загружается

Сохранить копию проблемного файла, проверить schema/format version и migration log. Не редактировать production save до backup/quarantine policy.

## 12. Bug report template

```markdown
## Summary
Short description.

## Environment
- Minecraft:
- Forge:
- Java:
- RimWorldCraft commit/version:
- Loader profile:
- Client/server:

## Reproduction
1.
2.
3.

## Expected behavior
...

## Actual behavior
...

## Logs
Attach sanitized client/server logs and stack trace.

## World/save details
Use test-world seed and sanitized save identifier; do not attach secrets.

## Severity
- [ ] blocker
- [ ] critical
- [ ] normal
- [ ] minor

## Suspected area
- [ ] Core
- [ ] entity integration
- [ ] pathfinding
- [ ] persistence
- [ ] network
- [ ] configuration
```

## 13. Issue и PR process

1. Создать issue с reproduction и expected/actual behavior.
2. Приложить версии, logs и test seed.
3. Исключить tokens, private saves и персональные данные.
4. Связать PR с issue.
5. Добавить regression test до исправления.
6. Обновить соответствующую документацию.
7. Выполнить DoD из `definition-of-done-do-d.md`.

## 14. CI checklist

Для текущего JVM skeleton:

```bash
./gradlew clean build --no-daemon
```

Для полного pipeline после добавления задач:

```bash
./gradlew check architectureTest jacocoTestCoverageVerification
./gradlew integrationTest acceptanceTest
./gradlew migrationTest configTest
./gradlew performanceTest stressTest
```

Обязательные gates:

- compile без ошибок;
- unit tests зелёные;
- Core coverage ≥ 80%;
- Infrastructure coverage ≥ 70%;
- ArchUnit зелёный;
- Checkstyle/SpotBugs зелёные;
- config/schema validation зелёная;
- docs check пройден;
- Forge client/server smoke для release candidate.

## 15. Ограничения текущего MVP scaffold

До подключения реального Forge runtime нельзя утверждать, что выполнены:

- визуальный спавн NPC в Minecraft;
- реальный `LivingEntity` lifecycle;
- Forge registry/renderers/network channel;
- NBT round-trip через `CompoundTag`;
- Baritone path search;
- полноценное ПКМ/атака взаимодействие;
- dedicated server integration.

Текущий код предоставляет компилируемые seams и JVM-level tests. Следующий production step — platform wiring с конкретными Forge mappings и TestModLoader integration.

## 16. Следующие шаги после MVP

1. Добавить Gradle Wrapper.
2. Настроить ForgeGradle и generated runs.
3. Подключить реальный `EntityCitizen extends LivingEntity`.
4. Подключить Forge registries, renderer и network channel.
5. Реализовать `CompoundTag` NBT adapter и migration tests.
6. Подключить Baritone/Automatone и pathfinding integration world tests.
7. Добавить authoritative server interaction handlers.
8. Перенести runtime configs в canonical `config/rimworldcraft/`.
9. Добавить ArchUnit, JaCoCo, Checkstyle и CI workflow tasks.
10. Провести multiplayer acceptance suite.

## 17. Связи с документацией

- [`system-overview.md`](system-overview.md) — контейнеры и dependency direction.
- [`bounded-contexts.md`](bounded-contexts.md) — границы контекстов и ownership.
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — ports/adapters и composition root.
- [`entity-integration.md`](entity-integration.md) — entity lifecycle, sync и packets.
- [`pathfinding-layer.md`](pathfinding-layer.md) — Baritone adapter and navigation.
- [`save-serialization.md`](save-serialization.md) — NBT schemas, migrations and recovery.
- [`data-interfaces.md`](data-interfaces.md) — repositories and persistence contracts.
- [`testing-strategy.md`](testing-strategy.md) — test pyramid and acceptance tests.
- [`drift-detection-pipeline.md`](drift-detection-pipeline.md) — CI quality gates.
- [`definition-of-done-do-d.md`](definition-of-done-do-d.md) — release readiness.
- [`configuration-mutation-testing.md`](configuration-mutation-testing.md) — config robustness.

## 18. Итоговый MVP sign-off

MVP можно подписать как runtime-ready только после выполнения всех пунктов:

- [ ] JDK 17 и Gradle Wrapper воспроизводимы.
- [ ] Forge client/server запускаются из IDE.
- [ ] Mod JAR собирается и загружается в чистом test instance.
- [ ] NPC регистрируется, спаунится и сохраняет `citizenId`.
- [ ] NPC получает Core projection и синхронизируется с клиентом.
- [ ] Pathfinding работает на реальном test world.
- [ ] ПКМ/атака проходят server validation.
- [ ] Save/load round-trip сохраняет NPC и colony state.
- [ ] JSON configs валидируются и имеют fallback.
- [ ] Smoke и multiplayer tests зелёные.
- [ ] CI artifacts и logs доступны.
- [ ] Known limitations записаны в issue/release notes.
