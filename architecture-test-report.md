# Architecture test report

## Status

**PARTIAL / JVM baseline.** An executable ArchUnit baseline is now wired into the active `core-impl` verification. It checks the platform-neutral Core boundary without claiming Forge/Minecraft runtime coverage.

## Implemented rules

- Core packages do not depend on Minecraft, Forge, Fabric, Baritone, Infrastructure, or Client packages.
- Every class under `core.ports..` is an interface.
- Every type under `core.events..` is isolated from repository and outer-layer packages.
- Shared Kernel types do not depend on bounded-context packages.
- Core package slices are checked for cycles.
- Production classes do not expose public setter methods.
- Public mutable static fields are prohibited.

The rules are intentionally scoped to packages present in the current JVM skeleton. They do not use broad `ignoreDependency` calls.

## Evidence

- `core-impl/src/test/java/com/rimworldcraft/architecture/ArchitectureTest.java`
- `core-impl/build.gradle`
- `gradle.properties` (`archunitVersion=1.3.0`)
- `architecture-exclusions.md`

## Verification command

```bash
./gradlew :core-impl:architectureTest --no-daemon
```

The task is also attached to `core-impl:check`.

## Known gaps

- Legacy compatibility packages under `core.api` remain during migration and are not yet reorganized into the target `port.in`/`port.out` layout.
- In-memory repository implementations remain in `core-impl` for JVM testing; production repository placement under Infrastructure is not yet active.
- Full dependency-direction rules for the future Forge, network, client, and adapter modules cannot execute until those modules are enabled.
- Exclusions registry is empty; no legacy violation is silently suppressed.
