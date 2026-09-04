# Event system API

**Status: PARTIAL.** The active implementation is a synchronous in-memory event bus in `core-impl`. Durable outbox/event store, async delivery and production handler bootstrap are not active.

## Contract

An event is an immutable fact created after a successful state transition. New cross-context events use `core.events.EventEnvelope`:

- `eventId`;
- `eventType`;
- `SchemaVersion`;
- `occurredAt`;
- `WorldId`;
- `correlationId`;
- immutable `payload`;
- `aggregateStream` and non-negative `sequence`.

Events are not commands. Handlers call their own application ports and do not mutate foreign aggregates.

## Active implementation

- API: `core-api/.../core/events/` and `core/ports/driven/EventBusPort.java`.
- Implementation: `core-impl/.../core/events/InMemoryEventBus.java`.
- Behavior: supported-schema validation, aggregate-stream ordering, explicit handler IDs, closeable subscriptions, retry policy, per-handler idempotency and dead-letter records.
- Tests: `core-impl/src/test/java/com/rimworldcraft/core/events/InMemoryEventBusTest.java`.

## Delivery rules

1. Validate envelope before dispatch.
2. Reject unknown schema versions.
3. Preserve sequence per aggregate stream.
4. Isolate one handler failure from other handlers.
5. Retry only within a bounded policy.
6. Mark `(handlerId, eventId)` as processed only after success.
7. Record permanent failures as dead letters.
8. Do not use an event to bypass the owner’s command/application boundary.

The active bus is in-process and synchronous. Do not describe it as durable or exactly-once. Exactly-once end-to-end requires durable outbox plus idempotent handlers.

## Event ownership

- Colony: membership/resource/building facts.
- Citizen: lifecycle/job facts.
- Goal AI: goal/plan/action/task facts.
- Storyteller: incident facts.
- Player: authority/membership facts.

Avoid duplicate events with overlapping meaning. Add owner, schema version, payload contract, consumers and idempotency behavior before introducing a new event.

## Pending work

Async bounded delivery, durable outbox/event store, codecs, full handler bootstrap, metrics, runtime integration and cross-context event-chain tests.

## Verification

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```

The `core-impl` test compilation is currently blocked by the incompatible ArchUnit test source; see [`architecture-test-report.md`](architecture-test-report.md).

## References

- [`AGENTS.md`](AGENTS.md)
- [`bounded-contexts.md`](bounded-contexts.md)
- [`hexagonal-architecture.md`](hexagonal-architecture.md)
- [`testing-strategy.md`](testing-strategy.md)
