package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that an NPC was created. */
public final class NPCCreatedEvent extends DomainEvent {
    /** Creates the event. */
    public NPCCreatedEvent(UUID sourceId) { super(sourceId); }
}
