# RimWorldCraft — Definition of Done (DoD)

## 1. Введение

Definition of Done — обязательный критерий, по которому задача считается завершённой, интегрируемой и готовой к поставке. Для RimWorldCraft DoD защищает не только функциональность, но и независимость Core от Minecraft API, DDD-границы, multiplayer correctness, persistence compatibility и поддерживаемость кода.

Документ применяется к user stories, technical tasks, bugfixes, spikes и документации. Он связан с CI/CD, code review и тестированием:

```text
Issue -> Implementation -> Automated checks -> Review -> QA/manual verification -> Merge-ready
```

### 1.1 Для кого

- разработчики — выполняют критерии и предоставляют доказательства;
- ревьюверы — проверяют критерии и не заменяют ими автора;
- QA — подтверждает пользовательское поведение и регрессии;
- техлид/архитектор — принимает обоснованные исключения;
- release owner — проверяет готовность к поставке.

### 1.2 Что означает «готово»

Готово означает: код реализован, проверен на нужном уровне, документирован, совместим с архитектурой и может быть безопасно интегрирован. «Работает на моей машине» или «тесты будут позже» не являются Done.

## 2. Общие принципы

- DoD — минимальная планка качества, а не максимальная бюрократия.
- Каждая задача должна быть проверяема и демонстрируема.
- Автор отвечает за качество и доказательства; ревьювер проверяет, а не дописывает тесты.
- DoD применяется к любой задаче, даже к маленькому bugfix.
- Неприменимый критерий отмечается как `N/A` с причиной в PR.
- Исключение временно, явно, ограничено scope и имеет owner/дату возврата.
- Автоматические проверки выполняют механические проверки, люди оценивают смысл и UX.
- Не снижать coverage, архитектурные границы или безопасность ради зелёного CI.

## 3. Типы задач

| Тип | Результат | Особенности DoD |
|---|---|---|
| User Story | видимая игроку функция | acceptance, UX, multiplayer/performance при применимости |
| Technical Task | refactor, performance, architecture/tooling | доказать отсутствие unintended behavior и улучшение метрик |
| Bugfix | исправление дефекта | regression test, воспроизводимость и проверка смежных путей |
| Spike | исследование/прототип | ограниченный DoD, обязательный decision document и follow-up |
| Documentation | изменение технической/игровой документации | Markdown quality, links, consistency и review |

## 4. Универсальные критерии

### 4.1 Обязательный checklist

- [ ] Issue/тикет существует и связан с PR.
- [ ] Acceptance criteria задачи понятны и проверены.
- [ ] Код компилируется без ошибок; предупреждения исправлены или обоснованы.
- [ ] Код отформатирован единым formatter и проходит Checkstyle/Spotless/Google Java Format согласно toolchain.
- [ ] Unit-тесты добавлены/обновлены; все проходят.
- [ ] Coverage не ниже `80%` для Core и `70%` для Infrastructure, без снижения coverage затронутой области.
- [ ] Integration/contract тесты написаны и проходят, если затронута внешняя граница.
- [ ] Acceptance/Gherkin-тест написан и проходит, если изменено пользовательское поведение.
- [ ] Public API, ports, classes и methods документированы Javadoc, если изменены/добавлены.
- [ ] ArchUnit rules проходят.
- [ ] SpotBugs/SonarQube не имеют новых critical/high issues.
- [ ] Логирование добавлено на подходящем уровне без secrets/private data.
- [ ] Ошибки, retries, fallback и cancellation обработаны.
- [ ] Configurable values вынесены в JSON/config; hardcode обоснован.
- [ ] Обратная совместимость сохранена либо есть migration/ADR.
- [ ] Документация и cross-references обновлены.
- [ ] Для пользовательских историй выполнено ручное тестирование.
- [ ] PR прошёл требуемое code review; для архитектурно значимых изменений — минимум два approve.

### 4.2 Автоматический минимум

```text
format check
compile/test compile
unit tests
JaCoCo coverage
Checkstyle/Spotless
SpotBugs
ArchUnit
JSON/schema validation
integration tests when applicable
```

### 4.3 Архитектурный минимум

- Core не импортирует Minecraft/Forge/Fabric API.
- Контексты взаимодействуют через порты, contracts и events.
- Aggregates защищают свои invariants.
- Repository/factory/specification contracts находятся в Core.
- Adapters находятся в Infrastructure.
- Server является authoritative source of truth.
- Network input валидируется как untrusted.

## 5. DoD user story

Дополнительно к универсальному checklist:

- [ ] User-facing behavior описан в acceptance criteria.
- [ ] Написаны Gherkin-сценарии для happy path и основных failure paths.
- [ ] Новая функция описана в player-facing guide/help/localization, если применимо.
- [ ] UI/UX проверены: понятные состояния loading/error/empty/success.
- [ ] Проверены повторные действия, отмена, disconnect/reconnect и reload.
- [ ] Multiplayer проверен, если есть packets, shared state или permissions.
- [ ] Server/client responsibilities явно разделены.
- [ ] Performance проверена: tick time, memory, network payload, path/planning budget.
- [ ] Compatibility с существующими saves/content packs проверена.
- [ ] Демонстрационный сценарий или короткая запись результата приложены к PR, если это помогает review.

## 6. DoD technical task

Дополнительно:

- [ ] Описана исходная проблема и измеримый результат.
- [ ] Все существующие тесты проходят без изменения behavior.
- [ ] Измерены relevant metrics до/после: coverage, complexity, allocation, latency, warnings.
- [ ] Нет необоснованного API break.
- [ ] Удалён дублирующийся/мертвый код.
- [ ] ArchUnit-границы стали не слабее и желательно усилены.
- [ ] Migration/compatibility plan есть для persistence/API changes.
- [ ] Performance improvement подтверждён benchmark или профилем, если задача об оптимизации.
- [ ] Refactor не смешан с несвязанной feature без объяснения.

## 7. DoD bugfix

Дополнительно:

- [ ] Bug описан шагами воспроизведения, expected и actual result.
- [ ] Добавлен regression test, который до исправления воспроизводил bug или максимально близко моделирует его.
- [ ] Исправление минимально и не скрывает симптом fallback’ом без причины.
- [ ] Regression test проходит после исправления.
- [ ] Проверены соседние сценарии и возможные race/persistence/network effects.
- [ ] Добавлено диагностическое логирование, если дефект может повториться в production.
- [ ] Проверены существующие saves/configs, если bug связан с данными.
- [ ] Root cause отражён в PR.

## 8. Упрощённый DoD для Spike

Spike — не production feature и не должен маскироваться под Done implementation.

- [ ] Вопрос исследования сформулирован.
- [ ] Код компилируется и запускает proof of concept.
- [ ] Ключевая гипотеза проверена экспериментом/benchmark.
- [ ] Результаты, ограничения и риски задокументированы.
- [ ] Принято решение: adopt, reject или investigate further.
- [ ] Известны cleanup/production follow-up tasks.
- [ ] Prototype не подключён в production без отдельного полноценного PR.

После одобрения прототипа создаётся production task с полным DoD.

## 9. DoD документации

- [ ] Документ обновлён в Markdown и соответствует текущей архитектуре.
- [ ] Заголовки, таблицы, code blocks и links корректны.
- [ ] Добавлены cross-links на затронутые документы.
- [ ] Термины и имена классов совпадают с кодом/approved model.
- [ ] Примеры не содержат устаревших или несуществующих API без пометки.
- [ ] Проверены орфография, понятность и отсутствие противоречий.
- [ ] Архитектурные изменения согласованы с architect/tech lead.
- [ ] Для configuration/API changes описаны migration/compatibility rules.

## 10. Процесс проверки

### 10.1 Автор

1. Связать branch/PR с issue.
2. Определить тип задачи и применимый checklist.
3. Написать tests вместе с implementation.
4. Запустить локальный быстрый набор checks.
5. Обновить документы/config/schema.
6. Заполнить PR DoD и приложить evidence.
7. Ответить на review comments и повторить checks после изменений.

### 10.2 CI/CD

CI выполняет:

```text
checkout
compile
format/style checks
unit tests
coverage gate
SpotBugs/Sonar checks
ArchUnit
config/schema validation
integration tests
package/artifact verification
```

Красный обязательный check блокирует merge. Flaky test не отмечается как pass: он исправляется, изолируется с issue или временно исключается с owner/сроком.

### 10.3 Code Review

Ревьювер проверяет:

- соответствие issue и acceptance criteria;
- ownership/bounded context;
- SOLID/DDD и dependency direction;
- обработку ошибок и edge cases;
- tests quality, а не только coverage percentage;
- persistence/network compatibility;
- observability и эксплуатационные риски;
- понятность API и документации.

### 10.4 QA и ручная проверка

QA проверяет user-facing flows на supported loader/version, чистом мире и сохранённом мире, а для multiplayer — server/client matrix. Результат фиксируется в PR или тестовом отчёте.

### 10.5 Техлид/архитектор

Финальный технический review нужен для:

- новых bounded contexts или aggregate boundaries;
- изменений public ports/events/save schemas;
- новых внешних libraries;
- изменения server/client protocol;
- intentional exception из архитектурных правил;
- существенных performance/security changes.

## 11. PR-шаблон DoD

```markdown
## Summary
- Issue: #123
- Task type: User Story / Technical Task / Bugfix / Spike / Documentation

## DoD Check
- [ ] Код компилируется без ошибок
- [ ] Formatter/Checkstyle/Spotless пройден
- [ ] Unit-тесты написаны и проходят
- [ ] Coverage >= 80% Core / >= 70% Infrastructure
- [ ] Integration/contract tests пройдены (или N/A: причина)
- [ ] Acceptance/Gherkin tests пройдены (или N/A: причина)
- [ ] ArchUnit rules соблюдены
- [ ] SpotBugs/SonarQube без новых critical/high issues
- [ ] Javadoc/API documentation обновлены
- [ ] Логирование и обработка ошибок реализованы
- [ ] Конфигурация/schema/migrations обновлены
- [ ] Обратная совместимость проверена
- [ ] Ручное тестирование проведено (если применимо)
- [ ] Документация обновлена
- [ ] Review получен

## Evidence
- Tests:
- Coverage report:
- Manual test environment:
- Performance result:
- Screenshots/logs:

## Exceptions / N/A
- Criterion:
- Reason:
- Owner:
- Follow-up issue:
- Expiry/review date:
```

### 11.1 Политика N/A

`N/A` допустимо только если критерий действительно неприменим. Формулировка «не успел» не является N/A; это незавершённая задача или согласованное исключение.

## 12. Ответственность

| Роль | Ответственность |
|---|---|
| Автор | реализовать, протестировать, документировать и предоставить evidence |
| Ревьювер | проверить DoD и техническое качество, запросить исправления |
| QA | подтвердить пользовательские и регрессионные сценарии |
| Техлид | контролировать процесс и разрешать ограниченные исключения |
| Архитектор | одобрять изменения boundaries/contracts и ADR |
| Release owner | подтвердить release readiness и known risks |

## 13. Метрики качества

Команда отслеживает тренды, а не использует одну метрику как абсолютную цель:

- доля PR, принятых с первого review;
- coverage Core/Infrastructure;
- количество новых SpotBugs/Checkstyle/Sonar issues;
- число нарушений ArchUnit;
- flaky test rate;
- lead time от issue до merge;
- defect escape rate после релиза;
- среднее время восстановления после failure;
- tick latency, memory и network regressions;
- доля migrations, прошедших compatibility suite.

Падение метрики требует анализа причины, но не должно приводить к накрутке coverage бессмысленными тестами.

## 14. Примеры задач

### 14.1 User Story: новый NPC-торговец

**Implementation:** добавлены archetype/config, `CitizenFactory` profile, trader interaction и UI projection.

Проверки:

- unit tests generation, traits, skills и permissions;
- JSON schema fixture для trader;
- integration spawn и save/load `citizenId`;
- Gherkin: trader появляется, имеет корректную роль, interaction работает;
- multiplayer: только server создаёт entity и broadcasts projection;
- manual test: чистый мир, reload, reconnect;
- docs: `module-npc-core.md`, `data-dictionaries.md`, player help;
- ArchUnit и Core isolation проходят.

### 14.2 Technical Task: рефакторинг Colony Manager

План:

1. Зафиксировать behavior через tests.
2. Разделить resource policy, membership и persistence.
3. Ввести ports/DTO вместо Minecraft references.
4. Запустить coverage/ArchUnit до и после.
5. Проверить save round-trip и event chains.
6. Удалить duplicate code.
7. Приложить метрики сложности и подтверждение отсутствия behavior change.

### 14.3 Bugfix: падение при сохранении мира

Порядок:

1. Воспроизвести на corrupted/missing optional field.
2. Добавить failing migration/serialization regression test.
3. Исправить mapper/recovery policy.
4. Проверить valid, old-version, future-version и corrupt snapshots.
5. Проверить backup/quarantine и error log.
6. Запустить полный persistence/integration suite.
7. Описать root cause и compatibility impact.

## 15. FAQ

### Можно ли пропустить тест для маленького bugfix?

Нет. Минимум — regression test или документированное ручное доказательство, если дефект невозможно выразить автоматически. Для обычной логики regression test обязателен.

### Что делать, если UI невозможно полностью покрыть unit-тестом?

Покрыть application/query logic unit-тестами, adapter contract — integration-тестом, а UI flow — ручной/acceptance проверкой с evidence.

### Можно ли временно отключить ArchUnit?

Только через явное исключение с причиной, owner, issue и сроком пересмотра. Предпочтительно исправить dependency.

### Что делать при смене public API?

Использовать versioning, compatibility facade или migration. Изменение должно быть отражено в документации и ADR.

### Обязательно ли обновлять документацию для каждой строки кода?

Нет. Обновление обязательно при изменении public API, domain behavior, config schema, persistence, protocol, architecture или user-facing behavior.

### Можно ли оставить hardcoded значение?

Только если это invariant/technical constant и оно named, documented и не является content/difficulty setting. Игровые параметры выносятся в JSON.

## 16. Эволюция DoD

DoD пересматривается минимум раз в квартал на ретроспективе и дополнительно после:

- production incident;
- появления нового loader/backend;
- изменения Java/toolchain;
- добавления security/performance requirement;
- изменения CI/CD или release process.

Процедура:

1. предложить изменение с причиной и examples;
2. проверить burden и automation cost;
3. обновить этот документ, PR template и CI rules;
4. объявить migration/grace period для legacy code;
5. измерить effect;
6. зафиксировать архитектурно значимые изменения в ADR.

Документ versioned в Git; устаревшие критерии не удаляются молча, а заменяются с пояснением.

## 17. Связи с другими документами

- [`system-overview.md`](system-overview.md) — общие контейнеры и архитектурные инварианты.
- [`bounded-contexts.md`](bounded-contexts.md) — границы контекстов и ownership.
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — ports/adapters и Core isolation.
- [`codestyle-and-solidd.md`](codestyle-and-solidd.md) — стиль кода и SOLID.
- [`ddd-tactical-patterns.md`](ddd-tactical-patterns.md) — tactical DDD patterns.
- [`event-system-api.md`](event-system-api.md) — event contracts, handlers и idempotency.
- [`data-interfaces.md`](data-interfaces.md) — repositories, factories и specifications.
- `testing-strategy.md` — test pyramid и CI test matrix.
- `archunit-rules.md` — executable architecture checks.
- `drift-detection-pipeline.md` — автоматическое обнаружение архитектурного дрейфа.
- `save-serialization.md` — persistence, migration и recovery.

## 18. Финальная проверка перед статусом Done

```text
Работает ли acceptance scenario?
Есть ли regression/edge-case tests?
Проходит ли CI полностью?
Сохранены ли Core boundaries?
Обработаны ли errors/retries/timeouts?
Совместимы ли saves/configs/network packets?
Обновлены ли docs/Javadoc/ADR?
Проверены ли performance и multiplayer risks?
Есть ли evidence в PR?
Понимает ли другой разработчик, что изменилось и как это поддерживать?
```

Если на любой обязательный вопрос нет ответа, задача не считается Done.
