package com.rimworldcraft.core.persistence;

public record AggregateVersion(long value) implements Comparable<AggregateVersion> {
    public AggregateVersion { if (value < 0) throw new IllegalArgumentException("aggregate version must be >= 0"); }
    public AggregateVersion next() { return new AggregateVersion(value + 1); }
    @Override public int compareTo(AggregateVersion other) { return Long.compare(value, other.value); }
}
