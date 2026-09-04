# Colony context

**Status: PARTIAL.** The JVM skeleton contains a typed Colony aggregate and application boundary. Full colony policy, durable persistence, event bootstrap and Forge integration are not active.

## Ownership

`Colony` owns world-scoped identity, lifecycle, citizen membership, resources, zones, value and threat summaries. It does not own Citizen internals, Minecraft blocks, entity physics or authentication.

## Implemented behavior

- `ColonyId`, `WorldId`, `ColonyName` and `SettlementSite` validation.
- Duplicate membership rejection and idempotent removal.
- Destroyed-state mutation guard.
- Positive resource mutation validation and atomic insufficient-resource failure.
- Constructor-injected `ColonyApplicationService`.
- Target `ColonyRepository`, driving use cases and settlement validation port.

## Compatibility

The active implementation remains in `core-impl/.../core/colony`. Legacy UUID overloads and `core.api` types remain for migration. New code uses target shared values and ports.

## Not complete

Work policy, full zones/objectives/morale model, complete reservation transaction semantics, typed event publication, processed-command persistence, complete snapshot mapper, durable repository and Forge/NBT adapter.

## Tests

`ColonyTest` and `ColonyInvariantTest` cover existing behavior, duplicate membership, idempotent removal, resource atomicity, lifecycle and world scope. Add repository/event/authorization tests when those boundaries become active.

## References

- [`bounded-contexts.md`](bounded-contexts.md)
- [`data-interfaces.md`](data-interfaces.md)
- [`colony-compliance-report.md`](colony-compliance-report.md)
- [`implementation-status.md`](implementation-status.md)
