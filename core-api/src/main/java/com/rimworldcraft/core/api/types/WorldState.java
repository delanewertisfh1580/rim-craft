package com.rimworldcraft.core.api.types;

import java.util.List;
import java.util.Map;

/** Immutable world snapshot consumed by Citizen AI. */
public record WorldState(GridPosition citizenPosition, Map<ResourceType, Integer> inventory,
                         List<CitizenId> nearbyCitizens, Map<ResourceType, Integer> availableResources,
                         GridPosition targetBlock, int mood, int health) {
    /** Creates an immutable world snapshot. */
    public WorldState {
        if (citizenPosition == null || inventory == null || nearbyCitizens == null || availableResources == null) {
            throw new IllegalArgumentException("World state values must not be null");
        }
        inventory = Map.copyOf(inventory);
        nearbyCitizens = List.copyOf(nearbyCitizens);
        availableResources = Map.copyOf(availableResources);
    }
}
