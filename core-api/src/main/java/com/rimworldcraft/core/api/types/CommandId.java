package com.rimworldcraft.core.api.types;

import java.util.Objects;
import java.util.UUID;

/** Identifies an application command for idempotency and tracing. */
public record CommandId(UUID value) {
    /** Creates a command identifier. */
    public CommandId { Objects.requireNonNull(value, "value"); }
}
