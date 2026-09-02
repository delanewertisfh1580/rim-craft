package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.shared.GameTick;
import java.util.Objects;

/** Typed result event emitted when a bounded plan cannot continue. */
public record PlanFailureEvent(PlanFailure failure, GameTick occurredAt) {
    public PlanFailureEvent {
        Objects.requireNonNull(failure, "failure");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
