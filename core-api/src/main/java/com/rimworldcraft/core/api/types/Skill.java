package com.rimworldcraft.core.api.types;

/** Immutable skill level and accumulated experience. */
public record Skill(int level, int experience) {
    /** Creates a validated skill. */
    public Skill {
        if (level < 0 || level > 20) throw new IllegalArgumentException("level must be 0..20");
        if (experience < 0) throw new IllegalArgumentException("experience must not be negative");
    }
    /** Returns a skill with additional experience and level progression. */
    public Skill addExperience(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
        int total = Math.addExact(experience, amount);
        int nextLevel = Math.min(20, total / 100);
        return new Skill(Math.max(level, nextLevel), total);
    }
}
