package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.events.DomainEvent;
import com.rimworldcraft.core.api.handlers.EventHandler;

/** Publishes and manages subscriptions for domain events. */
public interface IEventBusPort {
    /** Publishes an event to registered handlers. */
    void publish(DomainEvent event);
    /** Registers a handler for an event type. */
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);
    /** Removes a previously registered handler. */
    <T extends DomainEvent> void unsubscribe(Class<T> eventType, EventHandler<T> handler);
}
