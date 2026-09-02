package com.rimworldcraft.core.goal;

import java.util.Objects;
import java.util.Optional;

/** Immutable result of one server-side decision tick. */
public record DecisionResult(Status status, Optional<PlanFailure> failure, int replans, Optional<ActionDefinition> dispatchedAction) {
    public DecisionResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(failure, "failure");
        Objects.requireNonNull(dispatchedAction, "dispatchedAction");
        if (replans < 0) throw new IllegalArgumentException("replans");
    }

    public DecisionResult(Status status, int replans) {
        this(status, Optional.empty(), replans, Optional.empty());
    }

    public enum Status {
        IDLE,
        ACTION_DISPATCHED,
        ACTION_COMPLETED,
        ACTION_FAILED,
        PLAN_FAILED,
        REPLAN_REQUIRED,
        TIMED_OUT,
        CANCELLED
    }
}
