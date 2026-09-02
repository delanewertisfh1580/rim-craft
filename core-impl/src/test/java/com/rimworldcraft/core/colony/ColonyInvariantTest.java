package com.rimworldcraft.core.colony;

import static org.junit.jupiter.api.Assertions.*;

import com.rimworldcraft.core.api.types.*;
import com.rimworldcraft.core.shared.ColonyName;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies documented Colony aggregate invariants. */
class ColonyInvariantTest {
    private final WorldId world = new WorldId(UUID.randomUUID());
    private Colony colony() { return new Colony(world, new ColonyId(UUID.randomUUID()), new ColonyName("Home")); }

    @Test void duplicateMembershipIsRejected() {
        Colony colony = colony();
        CitizenId citizen = new CitizenId(UUID.randomUUID());
        colony.addCitizen(citizen);
        assertThrows(IllegalArgumentException.class, () -> colony.addCitizen(citizen));
    }
    @Test void removingMissingMembershipIsNoOp() {
        assertTrue(colony().removeCitizen(new CitizenId(UUID.randomUUID())).isEmpty());
    }
    @Test void insufficientReservationDoesNotPartiallyChangeResources() {
        Colony colony = colony();
        colony.addResource(ResourceType.WOOD, 2);
        assertThrows(RuntimeException.class, () -> colony.removeResource(ResourceType.WOOD, 3));
        assertEquals(2, colony.getResources().get(ResourceType.WOOD));
    }
    @Test void destroyedColonyRejectsMutations() {
        Colony colony = colony();
        colony.deactivate();
        assertThrows(IllegalStateException.class, () -> colony.addResource(ResourceType.WOOD, 1));
        assertThrows(IllegalStateException.class, () -> colony.addCitizen(new CitizenId(UUID.randomUUID())));
    }
    @Test void sameColonyIdInDifferentWorldsIsDistinctScope() {
        ColonyId id = new ColonyId(UUID.randomUUID());
        assertNotEquals(new Colony(new WorldId(UUID.randomUUID()), id, new ColonyName("A")).worldId(),
                new Colony(new WorldId(UUID.randomUUID()), id, new ColonyName("A")).worldId());
    }
}
