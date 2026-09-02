package com.rimworldcraft.core.storyteller;

import com.rimworldcraft.core.shared.IncidentId;
import java.util.Objects;

/** Immutable record of a scheduled incident. */
public record IncidentRecord(IncidentId incidentId, String definitionId, IncidentType type,
                             long scheduledTick, int threatPoints) {
    public IncidentRecord {
        Objects.requireNonNull(incidentId, "incidentId");
        if (definitionId == null || definitionId.isBlank()) throw new IllegalArgumentException("definitionId");
        Objects.requireNonNull(type, "type");
        if (scheduledTick < 0 || threatPoints < 0) throw new IllegalArgumentException("invalid incident record");
    }
}
