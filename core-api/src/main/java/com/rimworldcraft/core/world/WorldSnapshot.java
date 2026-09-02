package com.rimworldcraft.core.world;

import com.rimworldcraft.core.shared.RegionId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable publication boundary for world observations. */
public record WorldSnapshot(WorldId worldId, long observedTick, Map<RegionId, WorldRegion> regions) {
    public WorldSnapshot {
        Objects.requireNonNull(worldId, "worldId");
        if (observedTick < 0) throw new IllegalArgumentException("observedTick must be >= 0");
        Objects.requireNonNull(regions, "regions");
        if (regions.entrySet().stream().anyMatch(entry -> !worldId.equals(entry.getValue().worldId())
                || !entry.getKey().equals(entry.getValue().regionId()))) {
            throw new IllegalArgumentException("all regions must belong to the snapshot world");
        }
        regions = Map.copyOf(regions);
    }

    public Optional<WorldRegion> region(RegionId regionId) {
        return Optional.ofNullable(regions.get(Objects.requireNonNull(regionId, "regionId")));
    }
}
