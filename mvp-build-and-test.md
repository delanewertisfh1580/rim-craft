# MVP build and test runbook

## Active MVP

The supported MVP is a Java 17 platform-neutral JVM skeleton. Active modules are `core-api`, `core-impl` and `infrastructure-common`. Forge/Minecraft launch, entities, NBT, Baritone and multiplayer are not active.

## Environment

- JDK 17.
- Gradle Wrapper 8.10.2 (`gradlew`, `gradlew.bat`, wrapper JAR).
- Repository root as working directory.

## Verification

Focused active checks:

```bash
./gradlew :core-api:test :infrastructure-common:test --no-daemon
```

Full check:

```bash
./gradlew check --no-daemon
```

Current known result: focused checks pass; full check is blocked compiling `core-impl/src/test/java/com/rimworldcraft/architecture/ArchitectureTest.java` because its ArchUnit calls do not match the configured API. See [`architecture-test-report.md`](architecture-test-report.md).

## Active resources

Configuration fixtures are under `infrastructure-common/src/main/resources/config/rwc/`. JSON schema files are under `.../config/schemas/`. Active persistence is JSON via `JsonFileSaveAdapter`.

## Future runtime prerequisites

Before documenting a runnable Minecraft mod:

1. Configure ForgeGradle/mappings or a separate Fabric build.
2. Include the runtime module in `settings.gradle`.
3. Add mod metadata, launch tasks and real dependencies.
4. Implement and test entity, packet, world, pathfinding and NBT adapters.
5. Run a clean test-world smoke suite and record logs/artifacts.

## Troubleshooting

- Wrapper failure: inspect wrapper files; do not claim a build from committed `build/` output.
- Architecture compile failure: fix the test API usage; do not suppress it with exclusions.
- Configuration failure: inspect schema, JSON path and effective snapshot policy.
- Persistence failure: test atomic write, quarantine and last-known-good behavior.

## References

- [`AGENTS.md`](AGENTS.md)
- [`README.md`](README.md)
- [`implementation-status.md`](implementation-status.md)
- [`testing-strategy.md`](testing-strategy.md)
