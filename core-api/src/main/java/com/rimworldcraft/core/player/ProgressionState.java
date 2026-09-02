package com.rimworldcraft.core.player;

import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/** Immutable progression projection owned by Player Context. */
public record ProgressionState(Map<String, Integer> milestones) {
    public ProgressionState {
        Objects.requireNonNull(milestones, "milestones");
        milestones = Map.copyOf(milestones);
        if (milestones.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 0)) {
            throw new IllegalArgumentException("milestones must have non-negative values and non-blank keys");
        }
    }

    public static ProgressionState empty() {
        return new ProgressionState(Collections.emptyMap());
    }

    public ProgressionState withMilestone(String key, int value) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank() || value < 0) {
            throw new IllegalArgumentException("milestone key/value is invalid");
        }
        java.util.Map<String, Integer> updated = new java.util.HashMap<>(milestones);
        updated.put(key, value);
        return new ProgressionState(updated);
    }
}
