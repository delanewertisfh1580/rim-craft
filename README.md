# RimWorldCraft

RimWorldCraft — Java 17 platform-neutral JVM skeleton for a future Minecraft/Forge mod. Это не готовый Minecraft runtime.

## Быстрый статус

- Active modules: `core-api`, `core-impl`, `infrastructure-common`.
- JVM implementations: typed shared values, ports, Colony/Citizen/Building, World, Storyteller, Player, Goal AI, event bus, JSON config/save seams.
- Pending/blocked: Forge/Minecraft integration, NBT, Baritone, packets, rendering, durable runtime repositories and production event bootstrap.
- Focused tests pass: `./gradlew :core-api:test :infrastructure-common:test --no-daemon`.
- Full `./gradlew check --no-daemon` currently fails in `ArchitectureTest.java` because the checked-in ArchUnit calls do not match the configured API.

## Для coding agents

Главные правила и индекс документов находятся в [`AGENTS.md`](AGENTS.md). Сначала прочитайте его, затем [`implementation-status.md`](implementation-status.md) и документ затронутого контекста.

Канонические термины: `Citizen`/`CitizenId`, `GridPosition`, `WorldId`, `schemaVersion`, `core.shared`, `core.contracts`, `core.ports.driving`, `core.ports.driven`.

Legacy `core.api.*` и broad context packages являются compatibility migration state. Не добавляйте новые зависимости Minecraft в Core и не выдавайте design-only документы за runtime implementation.

## Сборка

```bash
./gradlew :core-api:test :infrastructure-common:test --no-daemon
./gradlew check --no-daemon
```

Подробные архитектурные решения: [`system-overview.md`](system-overview.md), [`bounded-contexts.md`](bounded-contexts.md), [`hexagonal-architecture.md`](hexagonal-architecture.md).
