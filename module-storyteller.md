# Storyteller context

**Status: PARTIAL.** The JVM boundary implements deterministic incident policy and snapshot mapping. Runtime incident loading, event bootstrap and Minecraft execution are not active.

## Ownership

Storyteller owns threat budget, cooldowns, pacing, incident definitions, history and outcome application. It consumes immutable Colony/NPC summaries and World snapshots. It does not mutate foreign aggregates or spawn entities.

## Current code

- Models: `core-api/src/main/java/com/rimworldcraft/core/storyteller/`.
- Service/repository: `core-impl/src/main/java/com/rimworldcraft/core/storyteller/`.
- Tests: `core-impl/src/test/java/com/rimworldcraft/core/storyteller/StorytellerContextTest.java`.

## Implemented behavior

- Validated incident definitions and non-negative threat/weight constraints.
- Stable ordering plus injected random source for deterministic selection.
- Eligibility, cooldown, pacing and threat-budget checks.
- Spawn-entry intent rather than entity creation.
- Postponement when no valid entry point exists.
- Idempotent incident outcome application.
- JSON `SaveDocument` snapshot mapping and future-schema rejection.

## Not complete

JSON incident loader/schema integration, durable runtime repository, production event handlers, real incident executor, full history migration and metrics.

## References

- [`bounded-contexts.md`](bounded-contexts.md)
- [`event-system-api.md`](event-system-api.md)
- [`module-world.md`](module-world.md)
- [`storyteller-compliance-report.md`](storyteller-compliance-report.md)
