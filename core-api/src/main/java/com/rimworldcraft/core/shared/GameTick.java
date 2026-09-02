package com.rimworldcraft.core.shared;

/** Canonical non-negative simulation tick. */
public record GameTick(long value) implements Comparable<GameTick> {
    /** Creates a game tick. */
    public GameTick { if (value < 0) throw new IllegalArgumentException("GameTick must not be negative"); }
    /** Compares simulation ticks. */
    @Override public int compareTo(GameTick other) { return Long.compare(value, other.value); }
    /** Converts this tick to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.GameTick toApiType() { return new com.rimworldcraft.core.api.types.GameTick(value); }
}
