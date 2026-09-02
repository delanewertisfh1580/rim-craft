package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies an application command for idempotency and tracing. */
public record CommandId(UUID value) {
    /** Creates a command identifier. */
    public CommandId {
        Objects.requireNonNull(value, "value");
    }

    /** Creates an identifier from an external UUID boundary value. */
    public static CommandId fromUuid(UUID value) {
        return new CommandId(value);
    }
}
