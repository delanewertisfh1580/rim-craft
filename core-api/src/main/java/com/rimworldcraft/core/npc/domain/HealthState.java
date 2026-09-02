package com.rimworldcraft.core.npc.domain;

/** Immutable health state; death is terminal at aggregate level. */
public record HealthState(int hitPoints, int maximumHitPoints) {
    public HealthState {
        if (maximumHitPoints <= 0 || hitPoints < 0 || hitPoints > maximumHitPoints) throw new IllegalArgumentException("invalid health");
    }
    public boolean incapacitated() { return hitPoints > 0 && hitPoints <= Math.max(1, maximumHitPoints / 5); }
    public boolean dead() { return hitPoints == 0; }
    public HealthState damage(int amount) { if (amount < 0) throw new IllegalArgumentException("amount must be >= 0"); return new HealthState(Math.max(0, hitPoints - amount), maximumHitPoints); }
    public HealthState heal(int amount) { if (amount < 0) throw new IllegalArgumentException("amount must be >= 0"); return new HealthState(Math.min(maximumHitPoints, hitPoints + amount), maximumHitPoints); }
}
