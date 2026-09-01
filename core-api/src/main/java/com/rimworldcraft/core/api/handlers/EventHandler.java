package com.rimworldcraft.core.api.handlers;

import com.rimworldcraft.core.api.events.DomainEvent;

/** Handles one domain event type. */
@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    /** Handles the supplied event. */
    void handle(T event);
}
