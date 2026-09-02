package com.rimworldcraft.core.events;

import java.util.Objects;

public record DeadLetter(EventEnvelope event, String handlerId, int attempts, String reason) {
    public DeadLetter { Objects.requireNonNull(event); Objects.requireNonNull(handlerId); Objects.requireNonNull(reason); }
}
