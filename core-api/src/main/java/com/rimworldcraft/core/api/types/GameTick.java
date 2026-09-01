package com.rimworldcraft.core.api.types;

/** Monotonic simulation time measured in game ticks. */
public record GameTick(long value) implements Comparable<GameTick> {
    /** Creates a game tick. */
    public GameTick {
        if (value < 0) throw new IllegalArgumentException("GameTick must not be negative");
    }
    @Override public int compareTo(GameTick other) { return Long.compare(value, other.value); }
}
