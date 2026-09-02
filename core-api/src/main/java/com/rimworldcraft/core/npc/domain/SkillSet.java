package com.rimworldcraft.core.npc.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable skill levels and accumulated experience. */
public record SkillSet(Map<SkillType, Integer> levels, Map<SkillType, Integer> experience) {
    public SkillSet {
        Objects.requireNonNull(levels, "levels");
        Objects.requireNonNull(experience, "experience");
        EnumMap<SkillType, Integer> l = new EnumMap<>(SkillType.class); l.putAll(levels);
        EnumMap<SkillType, Integer> e = new EnumMap<>(SkillType.class); e.putAll(experience);
        l.forEach((type, value) -> { if (value < 0 || value > 100) throw new IllegalArgumentException("level must be 0..100"); });
        e.forEach((type, value) -> { if (value < 0) throw new IllegalArgumentException("experience must be >= 0"); });
        levels = Map.copyOf(l); experience = Map.copyOf(e);
    }
    public int level(SkillType type) { return levels.getOrDefault(type, 0); }
    public SkillSet gain(SkillType type, int amount) {
        Objects.requireNonNull(type, "type"); if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        EnumMap<SkillType, Integer> l = new EnumMap<>(SkillType.class); l.putAll(levels);
        EnumMap<SkillType, Integer> e = new EnumMap<>(SkillType.class); e.putAll(experience);
        int xp = e.getOrDefault(type, 0) + amount; int level = l.getOrDefault(type, 0);
        while (level < 100 && xp >= threshold(level)) { xp -= threshold(level); level++; }
        l.put(type, level); e.put(type, xp); return new SkillSet(l, e);
    }
    private static int threshold(int level) { return Math.max(1, (level + 1) * 10); }
}
