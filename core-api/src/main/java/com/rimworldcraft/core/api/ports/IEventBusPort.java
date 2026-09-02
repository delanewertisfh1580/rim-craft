package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.events.DomainEvent;
import com.rimworldcraft.core.api.handlers.EventHandler;

/**
 * Legacy compatibility event bus contract.
 *
 * @deprecated Use {@code com.rimworldcraft.core.ports.driven.EventPublicationPort}
 * for outbound publication and keep subscription orchestration in application adapters.
 */
@Deprecated(forRemoval = false)
public interface IEventBusPort {
    /** Publishes an event to registered handlers. */
    void publish(DomainEvent event);
    /** Registers a handler for an event type. */
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);
    /** Removes a previously registered handler. */
    <T extends DomainEvent> void unsubscribe(Class<T> eventType, EventHandler<T> handler);
}
