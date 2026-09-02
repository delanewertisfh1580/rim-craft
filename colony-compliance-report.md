# Colony compliance report

## Overall status

**PARTIAL** — the JVM-skeleton Colony aggregate now has typed identity/world scope, lifecycle guards, duplicate membership protection, atomic resource failure behavior, and constructor-injected application orchestration. The complete documented Colony module is not yet implemented.

## Evidence

| Requirement | Status | Evidence |
|---|---|---|
| ColonyId and WorldId | DONE | `core-impl/src/main/java/com/rimworldcraft/core/colony/Colony.java` |
| Validated ColonyName | DONE | `core-api/src/main/java/com/rimworldcraft/core/shared/ColonyName.java` |
| Settlement site and GridPosition boundary | PARTIAL | `core-api/src/main/java/com/rimworldcraft/core/shared/SettlementSite.java`, `ColonyUseCases.java` |
| Lifecycle and destroyed guard | DONE | `Colony.deactivate`, `ColonyInvariantTest` |
| Typed citizen membership | DONE | `Colony.citizenIds()` and `CitizenId` overload |
| Duplicate membership protection | DONE | `ColonyInvariantTest.duplicateMembershipIsRejected` |
| Atomic insufficient-resource failure | DONE | `ColonyInvariantTest.insufficientReservationDoesNotPartiallyChangeResources` |
| World settlement validation port | DONE | `core-api/.../ports/driven/SettlementValidationPort.java` |
| Repository contract | DONE | `core-api/.../ports/driven/ColonyRepository.java` |
| Constructor-injected application service | DONE | `ColonyApplicationService.java` |
| Create/rename/add/remove/reserve/produce/destroy commands | PARTIAL | `ColonyUseCases.java`, `ColonyApplicationService.java` |
| Typed event envelope and publication | PARTIAL | `ColonyEventEnvelope.java`; legacy event hierarchy remains |
| Idempotent repeated commands | PARTIAL | aggregate removal is idempotent; processed-command store/publication deduplication is pending |
| Work policy, zones, morale, objectives, membership metadata | PARTIAL | existing skeleton zones/value/threat only; full documented model pending |
| NBT/Minecraft implementation | BLOCKED | intentionally excluded from active JVM build |

## Verification

Command executed:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && ./gradlew :core-api:test :core-impl:test --no-daemon
```

Result: **BUILD SUCCESSFUL**. Existing warnings concern deprecated compatibility ports and auxiliary legacy facade classes.

## Limitations and next steps

1. Persist full aggregate snapshots rather than the current minimal repository record.
2. Introduce a typed event publication port with correlation/causation and processed-command markers.
3. Implement work policy, resource reservations, production operation IDs, zones with world scope, morale, objectives, and membership metadata.
4. Add application-service tests with fake repository/world ports.
5. Add Forge/NBT adapters only after platform coordinates and mappings are explicitly confirmed.
