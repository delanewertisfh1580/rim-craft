package com.rimworldcraft.core.shared;

import java.util.Objects;
import java.util.UUID;

/** Canonical shared-kernel command identifier. */
public record CommandId(UUID value) {
    /** Creates a command identifier. */
    public CommandId { Objects.requireNonNull(value, "value"); }
    /** Converts this identifier to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.CommandId toApiType() {
        return new com.rimworldcraft.core.api.types.CommandId(value);
    }
}
