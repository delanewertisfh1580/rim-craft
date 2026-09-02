package com.rimworldcraft.core.npc.domain;

import java.util.Map;
import java.util.Objects;

/** Immutable trait IDs and their effects. */
public record TraitSet(Map<String, Integer> moodModifiers, Map<SkillType, Integer> skillModifiers) {
    public TraitSet {
        Objects.requireNonNull(moodModifiers, "moodModifiers");
        Objects.requireNonNull(skillModifiers, "skillModifiers");
        moodModifiers = Map.copyOf(moodModifiers);
        skillModifiers = Map.copyOf(skillModifiers);
    }
    public static TraitSet empty() { return new TraitSet(Map.of(), Map.of()); }
    public int moodModifier() { return moodModifiers.values().stream().mapToInt(Integer::intValue).sum(); }
}
