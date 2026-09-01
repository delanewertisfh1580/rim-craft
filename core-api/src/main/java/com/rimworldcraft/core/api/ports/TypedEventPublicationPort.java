package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.events.DomainEvent;

/** Publishes immutable domain events at a context boundary. */
public interface TypedEventPublicationPort {
    /** Publishes an event. */
    void publish(DomainEvent event);
}
