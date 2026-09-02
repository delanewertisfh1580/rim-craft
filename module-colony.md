# RimWorldCraft — Colony Context

## Status

**PARTIAL / MVP** — the platform-neutral Colony aggregate and typed application boundary are implemented in the JVM skeleton. Full persistence, event outbox, authorization, zones/work policy, and Forge adapters remain pending.

## Canonical model

- Aggregate: `com.rimworldcraft.core.colony.Colony`
- Identity: `ColonyId` scoped by `WorldId`
- Citizen membership: `CitizenId` (legacy UUID overloads remain for compatibility)
- Site coordinates: `GridPosition`
- Name: immutable `ColonyName`
- Repository: `com.rimworldcraft.core.ports.driven.ColonyRepository`
- Use-case contracts: `com.rimworldcraft.core.ports.driving.ColonyUseCases`

The aggregate owns name, lifecycle, membership, resources, zones, value and threat summaries. It does not know repositories, serialization, Minecraft, Forge, entities, or NBT.

## Implemented invariants

- blank/oversized colony names are rejected;
- colony and world identities are typed;
- duplicate citizen membership is rejected;
- removing a missing citizen is an idempotent no-op;
- resource amounts must be positive for mutations;
- insufficient removal leaves the inventory unchanged;
- destroyed colonies reject further mutations;
- settlement validation is requested through `SettlementValidationPort`;
- repository access is world-scoped;
- application service dependencies are constructor-injected.

## Application boundary

`ColonyApplicationService` currently implements creation, rename, add/remove membership, resource reservation, production application and destruction using `ColonyRepository` and `SettlementValidationPort`. Commands are immutable and carry `CommandId`, typed identities, and canonical site/name types.

Event publication and full command idempotency storage are intentionally not claimed yet. Existing legacy `DomainEvent` subclasses remain compatibility events and still require a future typed envelope adapter.

## Configuration and persistence

Canonical configuration path: `config/rimworldcraft/colony/...`; configuration parsing and persistence adapters are outside Core. Forge/NBT wiring is not active in the current build.

## Tests

`ColonyTest` preserves existing behavior tests. `ColonyInvariantTest` verifies duplicate membership, idempotent removal, atomic insufficient-resource failure, destroyed lifecycle, and world scoping. Additional repository contract, event envelope, authorization, work-policy, zone, and persistence tests are pending.

## Known limitations

The skeleton still contains legacy `core.colony` APIs, compatibility UUID overloads, simplified resource ledger semantics, and no concrete implementations for work policy, morale, reservations, or event outbox. These are staged follow-up work, not implemented gameplay.
