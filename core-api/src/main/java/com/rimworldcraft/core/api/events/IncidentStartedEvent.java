package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that an incident started. */
public final class IncidentStartedEvent extends DomainEvent {
    /** Creates the event. */
    public IncidentStartedEvent(UUID sourceId) { super(sourceId); }
}
