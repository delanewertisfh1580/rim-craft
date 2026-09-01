package com.rimworldcraft.core.api.types;

import java.util.List;

/** Immutable citizen mood value. */
public record Mood(int value, List<String> modifiers) {
    /** Creates a validated immutable mood. */
    public Mood {
        if (value < 0 || value > 100) throw new IllegalArgumentException("value must be between 0 and 100");
        if (modifiers == null) throw new IllegalArgumentException("modifiers must not be null");
        modifiers = List.copyOf(modifiers);
    }
    /** Returns a mood with a changed value. */
    public Mood withValue(int newValue) { return new Mood(newValue, modifiers); }
}
