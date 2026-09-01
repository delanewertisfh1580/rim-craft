package com.rimworldcraft.core.factory;

import com.rimworldcraft.core.api.types.*;
import com.rimworldcraft.core.colony.Colony;
import com.rimworldcraft.core.story.Incident;
import java.util.UUID;

/** Creates storyteller incidents. */
public final class IncidentFactory {
    /** Creates a raid incident. */
    public Incident createRaid(Colony colony, int difficulty) { return new Incident(UUID.randomUUID(), IncidentType.RAID, "Raid on " + colony.getName(), Math.max(0, difficulty), 1200); }
    /** Creates a trade caravan incident. */
    public Incident createTradeCaravan(Colony colony) { return new Incident(UUID.randomUUID(), IncidentType.TRADE, "Trade caravan", 1, 600); }
    /** Creates a disaster incident. */
    public Incident createDisaster(Colony colony, String type) { return new Incident(UUID.randomUUID(), IncidentType.DISASTER, type, 5, 800); }
    /** Creates a boon incident. */
    public Incident createBoon(Colony colony, String type) { return new Incident(UUID.randomUUID(), IncidentType.BOON, type, 1, 500); }
}
