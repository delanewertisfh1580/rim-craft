package com.rimworldcraft.core.api.types;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable world snapshot consumed by Citizen AI. */
public record WorldState(Position citizenPosition, Map<ResourceType, Integer> inventory,
                         List<UUID> nearbyEnemies, Map<ResourceType, Integer> availableResources,
                         Position targetBlock, int mood, int health) {
    /** Creates an immutable world snapshot. */
    public WorldState {
        if (citizenPosition == null || inventory == null || nearbyEnemies == null || availableResources == null) {
            throw new IllegalArgumentException("World state values must not be null");
        }
        inventory = Map.copyOf(inventory);
        nearbyEnemies = List.copyOf(nearbyEnemies);
        availableResources = Map.copyOf(availableResources);
    }
}
