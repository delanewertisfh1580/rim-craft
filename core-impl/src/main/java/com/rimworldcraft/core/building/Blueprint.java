package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.ResourceType;
import java.util.*;
/** Immutable blueprint definition. */
public record Blueprint(String id, String name, String description, int width, int height, int depth, Map<ResourceType, Integer> requiredResources, int estimatedTicks) {
    /** Validates and copies blueprint data. */
    public Blueprint { Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(description); Objects.requireNonNull(requiredResources); if (width <= 0 || height <= 0 || depth <= 0 || estimatedTicks < 0) throw new IllegalArgumentException("Invalid blueprint dimensions"); requiredResources = Map.copyOf(requiredResources); }
    /** Validates resource requirements. */ public void validate() { if (requiredResources.values().stream().anyMatch(value -> value < 0)) throw new IllegalArgumentException("Invalid resource requirement"); }
}
