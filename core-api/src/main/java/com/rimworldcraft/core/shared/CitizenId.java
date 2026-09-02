package com.rimworldcraft.core.shared;

import java.util.Objects;
import java.util.UUID;

/** Canonical shared-kernel citizen identifier. */
public record CitizenId(UUID value) {
    /** Creates a citizen identifier. */
    public CitizenId { Objects.requireNonNull(value, "value"); }
    /** Converts this identifier to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.CitizenId toApiType() {
        return new com.rimworldcraft.core.api.types.CitizenId(value);
    }
}
