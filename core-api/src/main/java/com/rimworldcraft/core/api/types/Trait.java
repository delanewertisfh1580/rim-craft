package com.rimworldcraft.core.api.types;

import java.util.Map;

/** Immutable citizen trait definition. */
public record Trait(String id, String name, String description,
                    Map<SkillType, Integer> skillModifiers, int moodModifier) {
    /** Creates a validated immutable trait. */
    public Trait {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (description == null) throw new IllegalArgumentException("description must not be null");
        if (skillModifiers == null) throw new IllegalArgumentException("skillModifiers must not be null");
        skillModifiers = Map.copyOf(skillModifiers);
    }
}
