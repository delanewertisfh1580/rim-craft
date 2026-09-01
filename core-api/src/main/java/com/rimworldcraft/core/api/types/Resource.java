package com.rimworldcraft.core.api.types;

/** Immutable resource quantity. */
public record Resource(ResourceType type, int amount) {
    /** Validates a resource quantity. */
    public Resource {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
    }
    /** Returns this quantity increased by the given amount. */
    public Resource add(int delta) {
        if (delta < 0) throw new IllegalArgumentException("delta must not be negative");
        return new Resource(type, Math.addExact(amount, delta));
    }
    /** Returns this quantity decreased by the given amount. */
    public Resource subtract(int delta) {
        if (delta < 0 || delta > amount) throw new IllegalArgumentException("insufficient resource");
        return new Resource(type, amount - delta);
    }
    /** Returns whether the quantity is zero. */
    public boolean isEmpty() { return amount == 0; }
    /** Returns whether the quantity is greater than zero. */
    public boolean isPositive() { return amount > 0; }
}
