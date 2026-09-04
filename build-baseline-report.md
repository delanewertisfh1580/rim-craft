# Build baseline report

**Checked:** 2026-09-03.

## Environment

- Java 17 toolchain configured in `build.gradle`.
- Gradle Wrapper 8.10.2 present: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`.
- Active modules: `core-api`, `core-impl`, `infrastructure-common`.

## Verified commands

Passed:

```text
./gradlew :core-api:test :infrastructure-common:test --no-daemon
BUILD SUCCESSFUL
```

Failed:

```text
./gradlew check --no-daemon
```

The failure occurs before architecture tests run, while compiling `ArchitectureTest.java`; ArchUnit symbols `slices`, `noMethods` and `exist` are unavailable in the configured API usage.

## Warnings/limitations

Gradle reports repository-preference and deprecation warnings. Forge and auxiliary test modules are not part of the active build. Do not report a full green build until `check` passes from source.

## Agent rule

This document records current verification only; previous successful build reports are historical and do not override the latest command result.
