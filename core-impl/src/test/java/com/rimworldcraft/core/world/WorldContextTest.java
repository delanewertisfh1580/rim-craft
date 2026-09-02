package com.rimworldcraft.core.world;

import com.rimworldcraft.core.ports.driven.WorldSnapshotPort;
import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.RegionId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldContextTest {
    private static final WorldId WORLD = new WorldId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final RegionId REGION = new RegionId(UUID.fromString("00000000-0000-0000-0000-000000000102"));
    private static final GridPosition SITE = new GridPosition(5, 64, 5);

    @Test
    void snapshotIsImmutableAndWorldScoped() {
        WorldRegion region = region(true, 10, true);
        WorldSnapshot snapshot = new WorldSnapshot(WORLD, 7, Map.of(REGION, region));

        assertEquals(region, snapshot.region(REGION).orElseThrow());
        assertTrue(snapshot.region(new RegionId(UUID.randomUUID())).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.regions().clear());
        assertThrows(IllegalArgumentException.class, () -> new WorldSnapshot(
                WORLD, 7, Map.of(REGION, new WorldRegion(new WorldId(UUID.randomUUID()), REGION,
                        region.bounds(), region.terrain(), region.climate(), region.hazards(),
                        region.resources(), region.accessibility(), region.spawnEntryPoints(), 7))));
    }

    @Test
    void settlementValidationUsesTerrainAccessibilityAndHazards() {
        WorldSnapshotPort valid = world -> java.util.Optional.of(new WorldSnapshot(WORLD, 1,
                Map.of(REGION, region(true, 10, true))));
        WorldSnapshotPort blocked = world -> java.util.Optional.of(new WorldSnapshot(WORLD, 1,
                Map.of(REGION, region(false, 10, true))));
        WorldSnapshotPort dangerous = world -> java.util.Optional.of(new WorldSnapshot(WORLD, 1,
                Map.of(REGION, region(true, 95, true))));

        assertTrue(valid.validateSettlement(WORLD, SITE).valid());
        assertEquals("TERRAIN_NOT_BUILDABLE", blocked.validateSettlement(WORLD, SITE).reason());
        assertEquals("HAZARD_TOO_HIGH", dangerous.validateSettlement(WORLD, SITE).reason());
        assertEquals("WORLD_SNAPSHOT_UNAVAILABLE", valid.validateSettlement(new WorldId(UUID.randomUUID()), SITE).reason());
    }

    @Test
    void positionOutsideRegionIsRejected() {
        WorldSnapshotPort port = world -> java.util.Optional.of(new WorldSnapshot(WORLD, 1,
                Map.of(REGION, region(true, 0, true))));

        SettlementValidationResult result = port.validateSettlement(WORLD, new GridPosition(100, 64, 100));

        assertFalse(result.valid());
        assertEquals("POSITION_OUTSIDE_SNAPSHOT", result.reason());
    }

    private static WorldRegion region(boolean buildable, int hazardSeverity, boolean accessible) {
        return new WorldRegion(WORLD, REGION,
                new RegionBounds(new GridPosition(0, 0, 0), new GridPosition(10, 100, 10)),
                new TerrainFacts("PLAINS", 64, 0, buildable, true),
                new ClimateFacts("temperate", 18, 50, 40),
                new HazardFacts(hazardSeverity, Set.of()),
                new ResourceFacts(Map.of("wood", 10)),
                new Accessibility(accessible, accessible ? 1 : 0, accessible ? "OK" : "BLOCKED"),
                Set.of(SITE), 1);
    }
}
