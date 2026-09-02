package com.rimworldcraft.core.npc.domain;

import java.util.Objects;

/** A satisfaction value whose decay is driven by simulation ticks. */
public record Need(NeedType type, double value, double decayPerTick, double criticalThreshold) {
    public Need {
        Objects.requireNonNull(type, "type");
        if (!Double.isFinite(value) || value < 0 || value > 100) throw new IllegalArgumentException("value must be 0..100");
        if (!Double.isFinite(decayPerTick) || decayPerTick < 0) throw new IllegalArgumentException("decayPerTick must be >= 0");
        if (!Double.isFinite(criticalThreshold) || criticalThreshold < 0 || criticalThreshold > 100) throw new IllegalArgumentException("criticalThreshold must be 0..100");
    }
    public Need decay(long ticks) {
        if (ticks < 0) throw new IllegalArgumentException("ticks must be >= 0");
        return new Need(type, Math.max(0, value - decayPerTick * ticks), decayPerTick, criticalThreshold);
    }
    public boolean critical() { return value <= criticalThreshold; }
}
