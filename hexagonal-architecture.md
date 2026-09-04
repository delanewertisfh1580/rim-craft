# Hexagonal architecture

## Model

```text
Driving adapter → driving port → application service → domain
                                      ↓
                              driven port → driven adapter
```

Core defines the business contracts. Infrastructure implements external capabilities. Client/network and Minecraft code are outside Core.

## Active packages

- Shared values: `com.rimworldcraft.core.shared`
- Cross-context contracts: `com.rimworldcraft.core.contracts`
- Driving ports: `com.rimworldcraft.core.ports.driving`
- Driven ports: `com.rimworldcraft.core.ports.driven`
- Compatibility API: `com.rimworldcraft.core.api.*`
- Common infrastructure: `com.rimworldcraft.infrastructure.common.*`
- Future Forge infrastructure: `com.rimworldcraft.infrastructure.forge.*` (not active)

## Rules for new code

1. Core imports only JDK, Core contracts and approved Core libraries already configured for that module.
2. Ports are interfaces and express Core needs, not Minecraft APIs.
3. Application services use constructor injection and own orchestration.
4. Aggregates protect invariants and do not call repositories, files or adapters.
5. Adapter mapping converts external IDs/coordinates/data at the boundary.
6. Commands are validated and authorized server-side; client data is untrusted.
7. Time, random and world observations enter through ports.
8. Persistence uses `SaveDocument`/snapshot mappers outside domain aggregates.
9. Events contain immutable facts and metadata; they are not commands.
10. Do not add a new compatibility port when an existing typed target port fits.

## Current implementation

Typed target ports and shared values exist in `core-api`. Colony, NPC, Goal AI, World, Storyteller and Player application boundaries exist across `core-api`/`core-impl`. JSON configuration and save adapters exist in `infrastructure-common`.

No active Forge adapter, NBT adapter, Baritone adapter, packet layer, renderer or runtime composition root is present in the active build.

## Migration

Use [`adr/0001-core-package-migration.md`](adr/0001-core-package-migration.md) and [`core-migration-notes.md`](core-migration-notes.md). Keep legacy overloads only while consumers migrate. New code must not deepen legacy API usage.

## Verification

The intended architectural check is `./gradlew :core-impl:architectureTest --no-daemon`. It is currently blocked by API-incompatible calls in `ArchitectureTest.java`; see [`architecture-test-report.md`](architecture-test-report.md).

## References

- [`AGENTS.md`](AGENTS.md)
- [`system-overview.md`](system-overview.md)
- [`bounded-contexts.md`](bounded-contexts.md)
- [`archunit-rules.md`](archunit-rules.md)
