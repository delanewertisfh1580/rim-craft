package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.api.events.DomainEvent;

/** Publishes immutable domain facts outside their owning bounded context. */
public interface EventPublicationPort {
    /** Publishes one domain event. */
    void publish(DomainEvent event);
}
