package com.rimworldcraft.core.world;

/** Accessibility projection; it contains no pathfinder or platform object. */
public record Accessibility(boolean accessible, int movementCost, String reason) {
    public Accessibility {
        if (movementCost < 0) throw new IllegalArgumentException("movementCost must be >= 0");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason");
    }

    public static Accessibility accessible(int movementCost) {
        return new Accessibility(true, movementCost, "OK");
    }

    public static Accessibility blocked(String reason) {
        return new Accessibility(false, 0, reason);
    }
}
