package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that a citizen joined a colony. */
public final class CitizenJoinedEvent extends DomainEvent {
    /** Creates the event. */
    public CitizenJoinedEvent(UUID sourceId) { super(sourceId); }
}
