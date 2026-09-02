package com.rimworldcraft.core.npc.domain;

import java.util.Objects;
import java.util.UUID;

/** Records one attempt to execute a job. */
public record JobAttempt(UUID jobId, Status status, long tick, String reason) {
    public enum Status { STARTED, COMPLETED, FAILED, CANCELLED }
    public JobAttempt {
        Objects.requireNonNull(jobId, "jobId"); Objects.requireNonNull(status, "status");
        if (tick < 0) throw new IllegalArgumentException("tick must be >= 0");
        reason = reason == null ? "" : reason;
    }
}
