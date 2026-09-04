# Event System compliance report

**Status: PARTIAL.**

## Implemented

- Immutable versioned `EventEnvelope`.
- `EventBusPort`, explicit handler IDs and closeable subscriptions.
- Synchronous `InMemoryEventBus`.
- Supported-schema validation, aggregate-stream sequence checks.
- Per-handler event idempotency.
- Bounded retry policy and dead-letter records.
- Handler failure isolation and focused tests.

## Pending

Async delivery, durable outbox/event store, serialization codec, full production handler bootstrap, metrics and Minecraft adapter integration.

## Boundary guarantees

Events are facts, not commands. Handlers must call their own application boundary and may not mutate foreign aggregates or use platform objects.

## Verification

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```

The `core-impl` test compilation is currently blocked by the incompatible ArchUnit test source.
