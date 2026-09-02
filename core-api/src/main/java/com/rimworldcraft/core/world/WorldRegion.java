package com.rimworldcraft.core.world;

import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.RegionId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.Set;

/** Aggregate snapshot of the bounded world facts for one logical region. */
public record WorldRegion(WorldId worldId, RegionId regionId, RegionBounds bounds,
                          TerrainFacts terrain, ClimateFacts climate, HazardFacts hazards,
                          ResourceFacts resources, Accessibility accessibility,
                          Set<GridPosition> spawnEntryPoints, long observedTick) {
    public WorldRegion {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(terrain, "terrain");
        Objects.requireNonNull(climate, "climate");
        Objects.requireNonNull(hazards, "hazards");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(accessibility, "accessibility");
        if (spawnEntryPoints == null || spawnEntryPoints.stream().anyMatch(position -> !bounds.contains(position))) {
            throw new IllegalArgumentException("spawn entry points must be inside region bounds");
        }
        if (observedTick < 0) throw new IllegalArgumentException("observedTick must be >= 0");
        spawnEntryPoints = Set.copyOf(spawnEntryPoints);
    }

    public boolean contains(GridPosition position) { return bounds.contains(position); }
}
