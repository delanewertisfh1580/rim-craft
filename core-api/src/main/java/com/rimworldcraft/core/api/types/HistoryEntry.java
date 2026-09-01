package com.rimworldcraft.core.api.types;

import java.time.Instant;
import java.util.UUID;

/** Immutable storyteller history entry. */
public record HistoryEntry(Instant timestamp, UUID incidentId, String shortDescription, int influence) {
    /** Creates a validated history entry. */
    public HistoryEntry {
        if (timestamp == null || incidentId == null || shortDescription == null || shortDescription.isBlank()) {
            throw new IllegalArgumentException("Invalid history entry");
        }
    }
}
