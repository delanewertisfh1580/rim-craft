# RimWorldCraft — правила для coding agents

## 1. Источник истины

Перед изменением кода прочитайте этот файл и документ, связанный с затронутой областью. Если документация расходится с кодом, доверяйте проверенному коду и Gradle-конфигурации, затем обновляйте документацию. Не выдавайте проектный план за реализованную функциональность.

Статусы:

- **IMPLEMENTED** — подтверждено исходным кодом и тестом/проверкой;
- **PARTIAL** — часть контракта реализована, production/runtime часть отсутствует;
- **PLANNED** — описано как следующий этап, код не считается готовым;
- **BLOCKED** — невозможно завершить без внешней платформы, координат или решения.

## 2. Текущий verified status

- Язык/runtime: Java 17, Gradle 8.10.2 Wrapper.
- Активные модули: `core-api`, `core-impl`, `infrastructure-common`.
- Неактивные модули: `infrastructure-forge`, `test-common`, `test-core`, `test-integration`; они не включены в `settings.gradle`.
- Реализовано: typed shared values, Core ports, JVM-агрегаты Colony/Citizen/Building, World/Storyteller/Player boundaries, Goal AI primitives, synchronous in-memory event bus, JSON config validation и JSON save adapter.
- Не реализовано: Forge/Minecraft runtime, NBT adapter, Baritone integration, network/packet runtime, rendering, production event bootstrap, durable context repositories, full config orchestration.
- Текущая полная проверка не зелёная: `./gradlew check --no-daemon` блокируется несовместимыми вызовами ArchUnit в `core-impl/src/test/.../ArchitectureTest.java` (`slices`, `noMethods`, `exist`).
- Фокусные проверки: `./gradlew :core-api:test :infrastructure-common:test --no-daemon` проходят.

## 3. Canonical vocabulary

| Использовать | Не вводить заново |
|---|---|
| `Citizen`, `CitizenId` | `Npc`, `NpcId` в новых API |
| `GridPosition` | `Position` в новых API |
| `WorldId`, world-scoped contracts | неявный world scope |
| `schemaVersion` | `version` как второй version field |
| `core.shared`, `core.contracts` | новые дубли shared values |
| `core.ports.driving`, `core.ports.driven` | новые ports в `core.api.ports` |
| immutable records/DTOs | public setters и mutable boundary state |
| JSON persistence (`SaveDocument`) | NBT внутри Core |
| event facts + application ports | прямую мутацию чужого aggregate |

Legacy пакеты `core.api.*`, `core.story`, `core.npc`, `core.colony` сохраняются только как migration compatibility. Не удаляйте их без coordinated migration.

## 4. Архитектурные правила

1. Core не импортирует Minecraft, Forge, Fabric, Baritone, Infrastructure или Client.
2. Aggregate владеет своими invariants и не знает repository/adapter.
3. Межконтекстные связи используют typed IDs, summaries, DTO, ports или events; не foreign aggregates.
4. Все операции изменения состояния идут через application service/use case.
5. Время, random и внешние наблюдения приходят через ports.
6. Состояние на границе immutable; коллекции defensive-copy.
7. JSON/NBT/filesystem находятся вне Core.
8. Player authority проверяется server-side; client input недоверен.
9. Событие публикуется после успешного изменения/сохранения состояния; handlers идемпотентны.
10. Не добавляйте внешнюю зависимость без проверки фактического Gradle stack.

## 5. Карта документации

- Обзор и статус: `README.md`, `implementation-status.md`, `documentation-consistency-report.md`.
- Архитектура: `system-overview.md`, `bounded-contexts.md`, `hexagonal-architecture.md`, `archunit-rules.md`, `architecture-test-report.md`.
- Моделирование: `ddd-tactical-patterns.md`, `data-dictionaries.md`, `data-interfaces.md`, `codestyle-and-solidd.md`.
- Контексты: `module-colony.md`, `module-npc-core.md`, `module-goal-ai.md`, `module-storyteller.md`, `module-world.md`, `module-player.md`.
- Runtime boundaries: `event-system-api.md`, `save-serialization.md`, `entity-integration.md`, `pathfinding-layer.md`.
- Качество: `testing-strategy.md`, `configuration-mutation-testing.md`, `drift-detection-pipeline.md`, `definition-of-done-do-d.md`, compliance reports.
- Миграция: `core-migration-notes.md`, `bounded-context-migration-report.md`, `adr/0001-core-package-migration.md`.

## 6. Рабочий процесс агента

1. Определите затронутый context и owner состояния.
2. Прочитайте этот файл, `implementation-status.md` и профильный module/report документ.
3. Найдите фактический Java contract и тесты до написания кода.
4. Сначала изменяйте Core contracts/domain, затем application, затем adapters.
5. Для новой boundary добавьте test и обновите профильный документ.
6. Не создавайте Forge/runtime code в активном JVM scope.
7. Проверяйте узкий модульный тест; после backend/source changes запускайте полный `check`.
8. Если проверка падает из-за существующего дефекта, исправьте причину или явно сообщите о блокере.

## 7. Команды проверки

```bash
./gradlew :core-api:test :infrastructure-common:test --no-daemon
./gradlew :core-impl:test --no-daemon
./gradlew check --no-daemon
```

`check` должен считаться успешным только при наличии строки `BUILD SUCCESSFUL`. Не используйте build artifacts как доказательство текущего исходного состояния.

## 8. Что запрещено считать реализованным

Не описывайте проект как готовый Minecraft-мод. Не заявляйте Forge entities, NBT, Baritone, packets, renderer, Cucumber, JaCoCo, SpotBugs, Docs-as-Code или CI workflow как активные возможности, пока соответствующие source/build/test files не подключены и не прошли проверку.
