package com.rimworldcraft.core.api.events;

import java.util.UUID;

/** Signals that a colony was destroyed. */
public final class ColonyDestroyedEvent extends DomainEvent {
    /** Creates the event. */
    public ColonyDestroyedEvent(UUID sourceId) { super(sourceId); }
}
