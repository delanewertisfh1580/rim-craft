package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.WorldId;
import com.rimworldcraft.core.world.SettlementValidationResult;
import com.rimworldcraft.core.world.WorldSnapshot;
import java.util.Optional;

/** Supplies platform-neutral world snapshots to Core contexts. */
public interface WorldSnapshotPort {
    Optional<WorldSnapshot> snapshot(WorldId worldId);

    default SettlementValidationResult validateSettlement(WorldId worldId, GridPosition position) {
        return snapshot(worldId)
                .filter(world -> worldId.equals(world.worldId()))
                .map(world -> world.regions().values().stream()
                .filter(region -> region.contains(position))
                .findFirst()
                .map(region -> {
                    if (!region.terrain().buildable()) return new SettlementValidationResult(worldId, position, false, "TERRAIN_NOT_BUILDABLE");
                    if (!region.accessibility().accessible()) return new SettlementValidationResult(worldId, position, false, "REGION_INACCESSIBLE");
                    if (region.hazards().severity() >= 90) return new SettlementValidationResult(worldId, position, false, "HAZARD_TOO_HIGH");
                    return new SettlementValidationResult(worldId, position, true, "VALID");
                })
                .orElseGet(() -> new SettlementValidationResult(worldId, position, false, "POSITION_OUTSIDE_SNAPSHOT")))
                .orElseGet(() -> new SettlementValidationResult(worldId, position, false, "WORLD_SNAPSHOT_UNAVAILABLE"));
    }
}
