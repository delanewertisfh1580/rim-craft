package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that a resource changed. */
public final class ResourceUpdatedEvent extends DomainEvent {
    /** Creates the event. */
    public ResourceUpdatedEvent(UUID sourceId) { super(sourceId); }
}
