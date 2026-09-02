package com.rimworldcraft.core.npc.domain;

import java.util.Objects;

/** Immutable mood score derived by a mood policy. */
public record MoodState(int value, long lastUpdatedTick) {
    public MoodState {
        if (value < 0 || value > 100) throw new IllegalArgumentException("mood must be 0..100");
        if (lastUpdatedTick < 0) throw new IllegalArgumentException("tick must be >= 0");
    }
    public MoodState adjust(int delta, long tick) { return new MoodState(Math.max(0, Math.min(100, value + delta)), tick); }
}
