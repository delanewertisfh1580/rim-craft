package com.rimworldcraft.core.goal;

/** Converts repeated failures into a terminal task/idle decision. */
public final class BoundedFailureHandler implements FailureHandler {
    private final int maxAttempts;

    public BoundedFailureHandler(int maxAttempts) {
        if (maxAttempts < 0) throw new IllegalArgumentException("maxAttempts");
        this.maxAttempts = maxAttempts;
    }

    @Override
    public Decision onFailure(String reason, int attempts) {
        if (attempts < 0) throw new IllegalArgumentException("attempts");
        if (attempts < maxAttempts) return Decision.RETRY;
        return Decision.IDLE;
    }
}
