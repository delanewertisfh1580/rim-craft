package com.rimworldcraft.core.story;

import com.rimworldcraft.core.api.types.IncidentType;
import java.util.Map;
/** Immutable storyteller configuration. */
public record StorytellerConfig(Map<IncidentType, Integer> eventWeights, int cooldownTicks, float difficultyMultiplier, float arcChance, int maxActiveIncidents) {
    /** Validates and copies configuration. */
    public StorytellerConfig { if (eventWeights == null || cooldownTicks < 0 || maxActiveIncidents < 0) throw new IllegalArgumentException("Invalid storyteller config"); eventWeights = Map.copyOf(eventWeights); }
}
