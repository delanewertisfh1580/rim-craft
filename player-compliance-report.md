# Player compliance report

**Status: PARTIAL.**

## Implemented

- Immutable world-scoped `PlayerProfile`.
- Membership, permissions, control mode, selection, preferences and progression.
- Server-side authorization boundary.
- Accepted/rejected command replay receipts.
- Selection cleanup on leave.
- Constructor-injected service, in-memory repository and focused tests.

## Pending

Authentication, packet normalization and bounds checks, distance/ownership/rate/stale-revision validation, durable profile persistence, event projections, client/network integration and audit retention.

## Security rule

Authentication adapter supplies normalized `PlayerId`; Core does not authenticate or inspect Minecraft packets. The server derives actor identity and remains authoritative.

## Verification

```bash
./gradlew :core-impl:test --tests com.rimworldcraft.core.player.PlayerContextTest --no-daemon
```

This focused command is subject to the current `core-impl` architecture-test compilation blocker when test classes are recompiled.
