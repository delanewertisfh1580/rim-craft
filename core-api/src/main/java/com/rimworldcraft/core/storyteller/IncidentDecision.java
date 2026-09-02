package com.rimworldcraft.core.storyteller;

import java.util.Objects;
import java.util.Optional;

/** Immutable result of one bounded incident evaluation. */
public record IncidentDecision(Status status, Optional<SpawnEntryPointRequest> request,
                               String reason, long retryAtTick) {
    public enum Status { SCHEDULED, POSTPONED, NOT_ELIGIBLE }

    public IncidentDecision {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(request, "request");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason");
        if (retryAtTick < 0) throw new IllegalArgumentException("retryAtTick must be >= 0");
        if (status == Status.SCHEDULED && request.isEmpty()) throw new IllegalArgumentException("scheduled decision requires request");
        if (status != Status.SCHEDULED && request.isPresent()) throw new IllegalArgumentException("only scheduled decisions have requests");
    }

    public static IncidentDecision postponed(String reason, long retryAtTick) {
        return new IncidentDecision(Status.POSTPONED, Optional.empty(), reason, retryAtTick);
    }

    public static IncidentDecision notEligible(String reason, long tick) {
        return new IncidentDecision(Status.NOT_ELIGIBLE, Optional.empty(), reason, tick);
    }
}
