package com.rimworldcraft.core.shared;

import java.util.Objects;
import java.util.UUID;

/** Canonical shared-kernel incident identifier. */
public record IncidentId(UUID value) {
    /** Creates an incident identifier. */
    public IncidentId { Objects.requireNonNull(value, "value"); }
    /** Converts this identifier to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.IncidentId toApiType() {
        return new com.rimworldcraft.core.api.types.IncidentId(value);
    }
}
