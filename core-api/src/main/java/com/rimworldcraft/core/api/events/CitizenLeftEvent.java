package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that a citizen left a colony. */
public final class CitizenLeftEvent extends DomainEvent {
    /** Creates the event. */
    public CitizenLeftEvent(UUID sourceId) { super(sourceId); }
}
