package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that a zone was removed. */
public final class ZoneRemovedEvent extends DomainEvent {
    /** Creates the event. */
    public ZoneRemovedEvent(UUID sourceId) { super(sourceId); }
}
