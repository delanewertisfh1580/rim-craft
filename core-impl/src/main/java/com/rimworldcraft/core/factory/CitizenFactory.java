package com.rimworldcraft.core.factory;

import com.rimworldcraft.core.api.types.*;
import com.rimworldcraft.core.colony.Colony;
import com.rimworldcraft.core.npc.Citizen;
import java.util.UUID;

/** Creates valid citizen aggregates. */
public final class CitizenFactory {
    /** Creates an unassigned random-style citizen. */
    public Citizen createRandomCitizen() { return new Citizen(UUID.randomUUID(), "Colonist", Gender.UNKNOWN, "human", UUID.randomUUID(), new Position(0, 64, 0)); }
    /** Creates a citizen assigned to a colony. */
    public Citizen createColonist(Colony colony) { return new Citizen(UUID.randomUUID(), "Colonist", Gender.UNKNOWN, "human", colony.getColonyId(), new Position(0, 64, 0)); }
    /** Creates an enemy citizen using a faction label as race. */
    public Citizen createEnemy(String faction) { return new Citizen(UUID.randomUUID(), "Enemy", Gender.UNKNOWN, faction, UUID.randomUUID(), new Position(0, 64, 0)); }
}
