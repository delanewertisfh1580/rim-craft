package com.rimworldcraft.core.storyteller;

/** Immutable configuration for pacing and incident evaluation. */
public record StorytellerConfigSnapshot(int minimumTicksBetweenIncidents, int maxHistorySize,
                                        int pressurePerThreatPoint, int retryWindowTicks) {
    public StorytellerConfigSnapshot {
        if (minimumTicksBetweenIncidents < 0 || maxHistorySize < 1
                || pressurePerThreatPoint < 0 || retryWindowTicks < 0) {
            throw new IllegalArgumentException("invalid storyteller configuration");
        }
    }

    public static StorytellerConfigSnapshot defaults() {
        return new StorytellerConfigSnapshot(600, 20, 1, 200);
    }
}
