# Architecture test report

## Status

**PARTIAL / BLOCKED.** ArchUnit `1.3.0` is configured in `core-impl` and `architectureTest` is attached to `check`, but `ArchitectureTest.java` currently does not compile against the configured API.

## Intended checks

- Core isolation from Minecraft, loaders, Baritone, Infrastructure and Client.
- Ports are interfaces.
- Events do not depend on repositories/outer layers.
- Shared types do not depend on contexts.
- No public setters or mutable public static fields.
- Context slices have no cycles.

## Evidence

- `core-impl/src/test/java/com/rimworldcraft/architecture/ArchitectureTest.java`
- `core-impl/build.gradle`
- `gradle.properties` (`archunitVersion=1.3.0`)
- `architecture-exclusions.md`

## Current blocker

Compilation fails for `slices()`, `noMethods()` and `.exist()` usages. Until the test is adapted to the pinned ArchUnit API, neither `architectureTest` nor full `check` can be called green.

## Exclusions

No intentional exclusions are registered. Do not hide this compile failure with broad ignores.

## Command

```bash
./gradlew :core-impl:architectureTest --no-daemon
```
