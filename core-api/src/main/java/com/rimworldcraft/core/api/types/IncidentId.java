package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies a storyteller incident. */
public record IncidentId(UUID value) {
    /** Creates an incident identifier. */
    public IncidentId { Objects.requireNonNull(value, "value"); }
}
