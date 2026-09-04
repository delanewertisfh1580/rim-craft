# Implementation status

**Дата проверки:** 2026-09-03. Статус основан на `settings.gradle`, исходниках и выполненных Gradle-командах.

## Сводка

Проект — **PARTIAL JVM MVP** на Java 17. Активны `core-api`, `core-impl`, `infrastructure-common`. Forge и вспомогательные test modules существуют в репозитории, но исключены из active build.

## Матрица

| Область | Статус | Фактическая граница |
|---|---|---|
| Shared IDs/time/position | IMPLEMENTED | `core.shared`, immutable records |
| Target ports/contracts | IMPLEMENTED | `core.ports.driving/driven`, `core.contracts` |
| Colony | PARTIAL | aggregate/application service/invariants; full policy/outbox/durable persistence отсутствуют |
| Citizen/NPC | PARTIAL | lifecycle, needs, mood, health, traits, skills, jobs; config/relationships/runtime adapters отсутствуют |
| Goal AI | PARTIAL | deterministic selection/planning/execution boundaries; real pathfinding/build handlers/config loader отсутствуют |
| Building | PARTIAL | blueprint/order/ghost validation and intents; full loader/adapter/event integration отсутствуют |
| World | PARTIAL | immutable snapshots and settlement validation; Minecraft observation/persistence отсутствуют |
| Storyteller | PARTIAL | threat/cooldown/weighted selection/intents/mapper; runtime loader/executor/events отсутствуют |
| Player | PARTIAL | server-side JVM authority and replay-safe command records; authentication/network/durable storage отсутствуют |
| Events | PARTIAL | synchronous `InMemoryEventBus`, retries/dead letters/idempotency/order; durable outbox/production handlers отсутствуют |
| Configuration | PARTIAL | NetworkNT schema validation, snapshots, diagnostics, mutator; full discovery/fallback orchestration отсутствует |
| Persistence | PARTIAL | `SaveDocument` and JSON adapter with recovery; full context mappers and NBT отсутствуют |
| Forge/Minecraft | BLOCKED | no ForgeGradle/mappings/runtime dependency in active build |
| CI/Docs-as-Code | PLANNED | documents describe desired tasks/workflow; no active workflow/tasks found |

## Canonical migration state

- New code uses `Citizen`, `CitizenId`, `GridPosition`, typed IDs, summaries and target ports.
- `core.api.*`, `core.story`, `core.npc`, raw UUIDs and legacy `Position` remain only for compatibility.
- `$schema` identifies schema resource; `schemaVersion` is the only contract version field.
- `config/rwc/` is the repository fixture/resource path. `config/rimworldcraft/` is the intended runtime convention, not an active loaded directory.

## Проверка

Passed:

```text
./gradlew :core-api:test :infrastructure-common:test --no-daemon
BUILD SUCCESSFUL
```

Failed:

```text
./gradlew check --no-daemon
```

Failure: `core-impl/src/test/java/com/rimworldcraft/architecture/ArchitectureTest.java` uses unavailable/incompatible `slices()`, `noMethods()` and `.exist()` calls. This is a repository blocker, not a documentation-only status.

## Следующий приоритет

1. Исправить ArchUnit test API mismatch and restore green `check`.
2. Keep this matrix synchronized with every new verified capability.
3. Complete JSON config orchestration and context snapshot mappers before NBT/Forge wiring.
4. Enable Forge only after explicit ForgeGradle/mappings decision and runtime test harness.
