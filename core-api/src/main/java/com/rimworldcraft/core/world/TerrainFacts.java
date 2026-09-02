package com.rimworldcraft.core.world;

import java.util.Objects;

/** Minimal terrain facts supplied by a platform adapter. */
public record TerrainFacts(String terrainType, int elevation, int slope, boolean buildable, boolean waterAccess) {
    public TerrainFacts {
        if (terrainType == null || terrainType.isBlank()) throw new IllegalArgumentException("terrainType");
        if (elevation < -1000 || elevation > 10000) throw new IllegalArgumentException("elevation");
        if (slope < 0 || slope > 90) throw new IllegalArgumentException("slope");
    }
}
