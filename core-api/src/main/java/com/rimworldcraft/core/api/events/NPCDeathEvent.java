package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that an NPC died. */
public final class NPCDeathEvent extends DomainEvent {
    /** Creates the event. */
    public NPCDeathEvent(UUID sourceId) { super(sourceId); }
}
