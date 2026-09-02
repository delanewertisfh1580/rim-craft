package com.rimworldcraft.core.world;

import java.util.Map;

/** Read-only resource availability projection for one region. */
public record ResourceFacts(Map<String, Integer> availableById) {
    public ResourceFacts {
        if (availableById == null || availableById.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue() < 0)) {
            throw new IllegalArgumentException("resource facts must be non-negative and named");
        }
        availableById = Map.copyOf(availableById);
    }
}
