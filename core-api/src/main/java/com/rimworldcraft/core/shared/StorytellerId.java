package com.rimworldcraft.core.shared;

import java.util.Objects;
import java.util.UUID;

/** Stable identity for a Storyteller aggregate. */
public record StorytellerId(UUID value) {
    public StorytellerId { Objects.requireNonNull(value, "value"); }
}
