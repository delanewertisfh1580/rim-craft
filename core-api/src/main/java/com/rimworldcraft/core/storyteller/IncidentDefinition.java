package com.rimworldcraft.core.storyteller;

import java.util.Objects;
import java.util.Set;

/** Config-backed incident candidate with bounded eligibility and scaling inputs. */
public record IncidentDefinition(String id, IncidentType type, double weight, long cooldownTicks,
                                 int minimumPopulation, int minimumThreatPoints,
                                 int baseThreatPoints, int threatPointsPerCitizen,
                                 double wealthFactor, Set<String> eligibleHazards) {
    public IncidentDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("incident id");
        Objects.requireNonNull(type, "type");
        if (!Double.isFinite(weight) || weight <= 0) throw new IllegalArgumentException("weight must be positive");
        if (cooldownTicks < 0 || minimumPopulation < 0 || minimumThreatPoints < 0
                || baseThreatPoints < 0 || threatPointsPerCitizen < 0
                || !Double.isFinite(wealthFactor) || wealthFactor < 0) {
            throw new IllegalArgumentException("invalid incident definition");
        }
        Objects.requireNonNull(eligibleHazards, "eligibleHazards");
        if (eligibleHazards.stream().anyMatch(hazard -> hazard == null || hazard.isBlank())) {
            throw new IllegalArgumentException("eligible hazards must be named");
        }
        eligibleHazards = Set.copyOf(eligibleHazards);
    }
}
