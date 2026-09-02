package com.rimworldcraft.core.contracts;

import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.CommandId;
import com.rimworldcraft.core.api.types.GameTick;
import com.rimworldcraft.core.api.types.SchemaVersion;
import com.rimworldcraft.core.api.types.WorldId;
import java.util.Objects;
import java.util.UUID;

/** Immutable metadata envelope for Colony integration events. */
public record ColonyEventEnvelope(UUID eventId, String eventType, WorldId worldId,
                                  ColonyId colonyId, GameTick occurredAt,
                                  SchemaVersion schemaVersion, CommandId commandId,
                                  UUID correlationId, Object payload) {
    /** Validates required event metadata and payload. */
    public ColonyEventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType must not be blank");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(payload, "payload");
    }
}
