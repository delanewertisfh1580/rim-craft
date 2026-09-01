package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that a zone was added. */
public final class ZoneAddedEvent extends DomainEvent {
    /** Creates the event. */
    public ZoneAddedEvent(UUID sourceId) { super(sourceId); }
}
