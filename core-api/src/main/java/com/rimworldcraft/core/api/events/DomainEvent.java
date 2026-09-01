package com.rimworldcraft.core.api.events;

import java.util.Objects;
import java.util.UUID;

/** Base class for immutable domain event metadata. */
public abstract class DomainEvent {
    private final UUID eventId;
    private final long timestamp;
    private final UUID sourceId;

    /** Creates an event with generated identity and current epoch milliseconds. */
    protected DomainEvent(UUID sourceId) { this(UUID.randomUUID(), System.currentTimeMillis(), sourceId); }
    /** Creates an event with explicit metadata, useful for replay. */
    protected DomainEvent(UUID eventId, long timestamp, UUID sourceId) {
        this.eventId = Objects.requireNonNull(eventId);
        this.timestamp = timestamp;
        this.sourceId = Objects.requireNonNull(sourceId);
    }
    /** Returns the event identity. */
    public UUID getEventId() { return eventId; }
    /** Returns the event timestamp in epoch milliseconds. */
    public long getTimestamp() { return timestamp; }
    /** Returns the aggregate or entity that produced the event. */
    public UUID getSourceId() { return sourceId; }
}
