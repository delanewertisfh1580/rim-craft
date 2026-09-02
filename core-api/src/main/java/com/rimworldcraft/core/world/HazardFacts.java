package com.rimworldcraft.core.world;

import java.util.Set;

/** Snapshot of hazards relevant to settlement and incident eligibility. */
public record HazardFacts(int severity, Set<String> hazardIds) {
    public HazardFacts {
        if (severity < 0 || severity > 100) throw new IllegalArgumentException("severity must be 0..100");
        if (hazardIds == null || hazardIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("hazardIds must not contain blank values");
        }
        hazardIds = Set.copyOf(hazardIds);
    }
}
