# ADR 0001: Staged Core package migration

- **Status:** Accepted
- **Date:** 2026-09-02

## Context

The JVM skeleton currently exposes Core API types under `com.rimworldcraft.core.api.*` and implementations under broad context packages. The target bounded-context and hexagonal layout requires shared kernel, contracts, driving ports, and driven ports. A mass package move would break existing consumers and obscure the boundary between migration and behavior changes.

## Decision

Adopt a staged migration:

1. New shared value objects and integration DTOs live in `core.shared` and `core.contracts`.
2. New use-case interfaces live in `core.ports.driving`.
3. New external capabilities live in `core.ports.driven`.
4. Existing `core.api.*` packages remain compatibility locations and are marked deprecated where a typed replacement exists.
5. Aggregates do not depend on infrastructure or client packages and exchange only IDs, summaries, DTOs, events, and ports.
6. Persistence serialization is implemented by infrastructure mappers; aggregate classes do not expose NBT methods.
7. Context-specific aggregate package moves happen incrementally with compile/test gates after each context.

## Consequences

The repository temporarily contains compatibility and target packages. This is intentional and allows consumers to migrate without a flag-day breaking release. Raw UUIDs and legacy `Position` remain only in older APIs until their adapters and persistence mappings are migrated.

## Migration exit criteria

- all new Core APIs use typed IDs and `GridPosition`;
- no active consumer imports deprecated compatibility ports;
- repository contracts are context-owned driven ports;
- aggregate persistence hooks are removed;
- architecture tests enforce Core dependency direction and context boundaries.
