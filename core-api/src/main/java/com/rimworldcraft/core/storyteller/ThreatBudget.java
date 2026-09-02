package com.rimworldcraft.core.storyteller;

/** Immutable available threat-point budget. */
public record ThreatBudget(int available, int capacity) {
    public ThreatBudget {
        if (capacity < 0 || available < 0 || available > capacity) throw new IllegalArgumentException("invalid threat budget");
    }
    public ThreatBudget spend(int points) {
        if (points < 0 || points > available) throw new IllegalArgumentException("insufficient threat budget");
        return new ThreatBudget(available - points, capacity);
    }
    public ThreatBudget replenish(int points) {
        if (points < 0) throw new IllegalArgumentException("points must be >= 0");
        return new ThreatBudget((int) Math.min(capacity, (long) available + points), capacity);
    }
}
