package com.rimworldcraft.core.api.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies shared-kernel value-object contracts. */
class SharedKernelValueObjectsTest {
    private static final UUID UUID_VALUE = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void identifiersAcceptUuidAndPreserveEquality() {
        assertEquals(new CitizenId(UUID_VALUE), CitizenId.fromUuid(UUID_VALUE));
        assertEquals(UUID_VALUE, new WorldId(UUID_VALUE).value());
    }

    @Test
    void identifiersRejectNull() {
        assertThrows(NullPointerException.class, () -> new WorldId(null));
        assertThrows(NullPointerException.class, () -> new ColonyId(null));
        assertThrows(NullPointerException.class, () -> new CitizenId(null));
        assertThrows(NullPointerException.class, () -> new PlayerId(null));
        assertThrows(NullPointerException.class, () -> new RegionId(null));
        assertThrows(NullPointerException.class, () -> new IncidentId(null));
        assertThrows(NullPointerException.class, () -> new CommandId(null));
    }

    @Test
    void contentIdRequiresNamespacedNonBlankValue() {
        assertEquals("rwc:optimist", new ContentId("rwc:optimist").value());
        assertThrows(NullPointerException.class, () -> new ContentId(null));
        assertThrows(IllegalArgumentException.class, () -> new ContentId(" "));
        assertThrows(IllegalArgumentException.class, () -> new ContentId("optimist"));
    }

    @Test
    void numericValuesRejectInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> new GameTick(-1));
        assertThrows(IllegalArgumentException.class, () -> new SchemaVersion(0));
        assertEquals(new GameTick(4), new GameTick(4));
    }

    @Test
    void positionIsImmutableAndValidatesDistanceArgument() {
        GridPosition origin = new GridPosition(0, 0, 0);
        assertEquals(new GridPosition(1, 2, 3), origin.add(1, 2, 3));
        assertEquals(Math.sqrt(14), origin.distance(new GridPosition(1, 2, 3)));
        assertThrows(NullPointerException.class, () -> origin.distance(null));
    }

    @Test
    void worldScopeIsPartOfIdentityBoundary() {
        WorldId firstWorld = new WorldId(UUID_VALUE);
        WorldId secondWorld = new WorldId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertNotEquals(firstWorld, secondWorld);
        assertEquals(UUID_VALUE, ExternalIdMapper.worldId(UUID_VALUE).value());
    }
}
