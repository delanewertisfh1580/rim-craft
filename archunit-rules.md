# ArchUnit rules

## Status

**BLOCKED.** ArchUnit is configured in `core-impl`, but the current `ArchitectureTest.java` does not compile against the pinned API. Do not describe architecture checks as passing until that source/API mismatch is fixed.

Current blocker:

- `slices()` is unavailable through the configured API;
- `noMethods()` is unavailable;
- the used field/method predicate call to `.exist()` is unavailable.

The authoritative implementation is [`core-impl/src/test/java/com/rimworldcraft/architecture/ArchitectureTest.java`](core-impl/src/test/java/com/rimworldcraft/architecture/ArchitectureTest.java). This document defines the intended policy; it is not evidence that every rule is currently executable.

## Purpose

Use ArchUnit to protect dependency direction and package boundaries. Use JUnit for behavior, JSON Schema validation for configuration shape, and static-analysis/formatting tools for concerns ArchUnit cannot reliably prove.

### Rule status vocabulary

- **ENFORCED:** implemented in the current architecture test and passing.
- **BLOCKED:** intended or present, but verification is blocked by the architecture-test compilation error.
- **PLANNED:** policy agreed, executable rule not yet implemented.
- **EXCEPTION:** temporary, explicit, owner- and expiry-bound deviation documented in [`architecture-exclusions.md`](architecture-exclusions.md).

## Repository facts

- Java 17 JVM project.
- Active modules: `core-api`, `core-impl`, `infrastructure-common`.
- `infrastructure-forge`, `test-common`, `test-core`, and `test-integration` are present but inactive in the current build.
- There is no active Minecraft/Forge runtime adapter.
- Both target typed packages and legacy compatibility packages exist during the staged migration; see [`adr/0001-core-package-migration.md`](adr/0001-core-package-migration.md).

## Dependency policy

### Core isolation — BLOCKED until the test compiles

Packages under `com.rimworldcraft.core..` must not depend on:

- `net.minecraft..`, `net.minecraftforge..`, `net.fabricmc..`;
- Baritone/Automatone or other platform libraries;
- `com.rimworldcraft.infrastructure..`;
- client/platform packages;
- direct file, database, NBT, or JSON implementation APIs where a port is required.

Core may use typed shared values such as `WorldId`, `ColonyId`, `CitizenId`, `PlayerId`, `GameTick`, `SchemaVersion`, and `GridPosition`.

### Ports and adapters — PLANNED/BLOCKED

- Driving and driven ports belong under `com.rimworldcraft.core.ports..`.
- Port contracts are interfaces and depend only on Core types and contracts.
- Implementations belong under infrastructure adapter/repository packages.
- Driving adapters must not reach into driven adapter implementations.
- Core must never import an adapter.

The actual package names in source take precedence over this target policy while migration is in progress.

### Bounded contexts — PLANNED/BLOCKED

Contexts must communicate through shared values, explicit contracts, events, and ports rather than direct sibling-domain imports. The intended contexts are Colony, NPC/Citizen, Storyteller, Goal AI, Building, World, and Player.

Do not add new direct dependencies between sibling domain packages. If an existing compatibility dependency must remain, record it in the migration documentation rather than copying the pattern into new code.

### Events — PLANNED/BLOCKED

Events are immutable facts. They must not depend on repositories, infrastructure, client/platform classes, or Minecraft types. Event handlers belong at application/integration boundaries and must not mutate foreign aggregates directly.

### Aggregates and value objects — PLANNED/BLOCKED

- Aggregate behavior belongs to its owning context.
- Aggregates do not serialize themselves or call repositories.
- Aggregate state must be changed through behavior methods, not public setters.
- Value objects should be immutable; records are preferred where suitable.
- Repository contracts belong to Core; repository implementations belong to infrastructure.

Because the currently configured ArchUnit API lacks methods used by the source test, these constraints are presently policy, not a green executable gate.

## Forbidden practices

Prefer a precise rule or another suitable tool for each item:

- no `System.out`, `System.err`, or `printStackTrace()` in production code;
- no public mutable static state;
- no direct platform objects in Core (`Level`, `BlockPos`, `Entity`, `ItemStack`, loader events, etc.);
- no direct file/configuration parsing in Core;
- no `Thread.sleep()` in tests; use a deterministic clock/executor or an approved test utility;
- no untracked architecture exclusions.

These checks must not be claimed as enforced unless the current build proves them.

## Exclusions

An exclusion is a temporary migration aid, never a way to normalize a violation. Each entry must include:

- rule ID;
- exact class/package/dependency;
- reason;
- owner;
- tracking issue;
- date added;
- review/expiry date;
- removal plan.

Do not exclude new Core-to-platform dependencies, authority/security violations, missing critical idempotency, or flaky tests merely to make CI green. See [`architecture-exclusions.md`](architecture-exclusions.md).

## Verification

Focused tests currently pass:

```bash
./gradlew :core-api:test :infrastructure-common:test --no-daemon
```

The complete gate currently fails during `ArchitectureTest.java` compilation:

```bash
./gradlew check --no-daemon
```

After fixing the ArchUnit source/API mismatch, run `./gradlew check --no-daemon` and update [`architecture-test-report.md`](architecture-test-report.md) with the observed result. Do not hand-edit generated build output or generated metadata to hide failures.

## Agent checklist

Before adding or changing Core code:

- [ ] Use typed IDs and `GridPosition` in new APIs.
- [ ] Keep platform and persistence details behind ports/adapters.
- [ ] Place code in the owning context.
- [ ] Avoid new sibling-context imports.
- [ ] Keep events immutable and repository-independent.
- [ ] Keep aggregates free of persistence calls and public setters.
- [ ] Add or update focused tests.
- [ ] Add an explicit, expiring exclusion if migration forces a boundary violation.
- [ ] Run the narrowest relevant Gradle test, then the full check when the architecture test is fixed.

## Related references

- [`AGENTS.md`](AGENTS.md) — agent entrypoint and repository rules.
- [`implementation-status.md`](implementation-status.md) — current implementation matrix.
- [`system-overview.md`](system-overview.md) — architecture overview.
- [`bounded-contexts.md`](bounded-contexts.md) — context ownership and boundaries.
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — dependency direction.
- [`testing-strategy.md`](testing-strategy.md) — test levels and verification.
- [`documentation-consistency-report.md`](documentation-consistency-report.md) — known documentation drift.
