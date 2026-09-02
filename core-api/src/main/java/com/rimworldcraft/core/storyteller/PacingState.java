package com.rimworldcraft.core.storyteller;

/** Bounded pacing signal used to avoid incident bursts. */
public record PacingState(int pressure, long lastIncidentTick) {
    public PacingState {
        if (pressure < 0 || pressure > 100) throw new IllegalArgumentException("pressure must be 0..100");
        if (lastIncidentTick < 0) throw new IllegalArgumentException("lastIncidentTick must be >= 0");
    }
    public PacingState afterIncident(int pressureDelta, long tick) {
        return new PacingState(Math.max(0, Math.min(100, pressure + pressureDelta)), tick);
    }
}
