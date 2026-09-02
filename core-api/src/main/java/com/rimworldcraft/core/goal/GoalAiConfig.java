package com.rimworldcraft.core.goal;

import java.util.Map;
import java.util.Objects;

/** Immutable runtime configuration used by Goal AI decisions and planning. */
public record GoalAiConfig(
        int decisionIntervalTicks,
        int replanIntervalTicks,
        int maxPlanDepth,
        int maxPlanLength,
        int actionTimeoutTicks,
        int maxActionRetries,
        int maxReplans,
        Map<GoalType, Integer> priorityDefaults) {
    public GoalAiConfig {
        if (decisionIntervalTicks < 1 || replanIntervalTicks < 0 || maxPlanDepth < 1
                || maxPlanLength < 1 || actionTimeoutTicks < 1 || maxActionRetries < 0
                || maxReplans < 0) {
            throw new IllegalArgumentException("invalid Goal AI configuration");
        }
        Objects.requireNonNull(priorityDefaults, "priorityDefaults");
        if (priorityDefaults.values().stream().anyMatch(value -> value == null || value < 0 || value > 100)) {
            throw new IllegalArgumentException("priority defaults must be 0..100");
        }
        priorityDefaults = Map.copyOf(priorityDefaults);
    }

    public static GoalAiConfig defaults() {
        return new GoalAiConfig(10, 40, 10, 10, 200, 2, 3, Map.of(
                GoalType.EAT, 100, GoalType.SLEEP, 95, GoalType.FLEE, 90,
                GoalType.FIGHT, 85, GoalType.BUILD, 75, GoalType.WORK, 70,
                GoalType.SOCIALIZE, 40, GoalType.IDLE, 10));
    }
}
