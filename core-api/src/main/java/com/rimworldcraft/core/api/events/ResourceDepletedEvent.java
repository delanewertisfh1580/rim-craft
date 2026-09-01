package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that a resource was depleted. */
public final class ResourceDepletedEvent extends DomainEvent {
    /** Creates the event. */
    public ResourceDepletedEvent(UUID sourceId) { super(sourceId); }
}
