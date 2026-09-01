package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that a colony was created. */
public final class ColonyCreatedEvent extends DomainEvent {
    /** Creates the event. */
    public ColonyCreatedEvent(UUID sourceId) { super(sourceId); }
}
