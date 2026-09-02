package com.rimworldcraft.core.events;

import com.rimworldcraft.core.shared.SchemaVersion;
import com.rimworldcraft.core.shared.WorldId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Versioned immutable event envelope used at bounded-context boundaries. */
public record EventEnvelope(UUID eventId, String eventType, SchemaVersion schemaVersion, Instant occurredAt,
                            WorldId worldId, String correlationId, Object payload, String aggregateStream, long sequence) {
    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId"); Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(occurredAt, "occurredAt"); Objects.requireNonNull(worldId, "worldId"); Objects.requireNonNull(payload, "payload");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType must not be blank");
        if (correlationId == null || correlationId.isBlank()) throw new IllegalArgumentException("correlationId must not be blank");
        if (aggregateStream == null || aggregateStream.isBlank()) throw new IllegalArgumentException("aggregateStream must not be blank");
        if (sequence < 0) throw new IllegalArgumentException("sequence must be >= 0");
    }
    public EventEnvelope(UUID eventId, String eventType, SchemaVersion schemaVersion, Instant occurredAt,
                         WorldId worldId, String correlationId, Object payload) {
        this(eventId, eventType, schemaVersion, occurredAt, worldId, correlationId, payload, eventType + "/" + worldId, 0);
    }
}
