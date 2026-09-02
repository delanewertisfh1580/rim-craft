package com.rimworldcraft.core.goal;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small deterministic policy set used by the JVM skeleton and tests. */
public final class BasicGoalPolicies {
    private BasicGoalPolicies() { }

    public static PriorityEvaluator priority(Map<GoalType, Integer> configured) {
        Map<GoalType, Integer> values = Map.copyOf(configured);
        return (goal, state) -> {
            if (goal == GoalType.IDLE) return values.getOrDefault(goal, 10);
            if (state.has(new StateFact("critical_need", true))
                    && (goal == GoalType.EAT || goal == GoalType.SLEEP)) {
                return 100;
            }
            if (state.has(new StateFact("enemy_near", true))
                    && (goal == GoalType.FLEE || goal == GoalType.FIGHT)) {
                return values.getOrDefault(goal, 90);
            }
            return values.getOrDefault(goal, 0);
        };
    }

    public static GoalSelector selector(PriorityEvaluator evaluator) {
        Objects.requireNonNull(evaluator, "evaluator");
        return state -> Arrays.stream(GoalType.values())
                .map(type -> Map.entry(type, evaluator.score(type, state)))
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new Goal(entry.getKey(), entry.getValue(), defaultTarget(entry.getKey())))
                .max(Comparator.comparingInt(Goal::priority).thenComparing(goal -> goal.type().name()))
                .orElse(new Goal(GoalType.IDLE, 10, ""));
    }

    public static GOAPPlanner planner(List<ActionDefinition> actions, int maxDepth) {
        List<ActionDefinition> definitions = List.copyOf(actions);
        if (maxDepth < 1) throw new IllegalArgumentException("maxDepth");
        return (goal, state) -> {
            Deque<List<ActionDefinition>> queue = new ArrayDeque<>();
            queue.add(List.of());
            Set<Set<StateFact>> visited = new HashSet<>();
            while (!queue.isEmpty()) {
                List<ActionDefinition> path = queue.removeFirst();
                Set<StateFact> facts = new HashSet<>(state.facts());
                for (ActionDefinition action : path) facts = new HashSet<>(Effects.apply(facts, action));
                if (goal.type() == GoalType.IDLE || (!goal.targetKey().isBlank() && facts.contains(new StateFact(goal.targetKey(), true)))) {
                    UUID id = UUID.nameUUIDFromBytes(path.toString().getBytes(StandardCharsets.UTF_8));
                    return Optional.of(new Plan(id, goal, path,
                            path.stream().mapToInt(ActionDefinition::cost).sum(), state.observedTick()));
                }
                if (path.size() >= maxDepth || !visited.add(Set.copyOf(facts))) continue;
                for (ActionDefinition action : definitions) {
                    if (!facts.containsAll(action.preconditions())) continue;
                    List<ActionDefinition> next = new ArrayList<>(path);
                    next.add(action);
                    queue.add(next);
                }
            }
            return Optional.empty();
        };
    }

    private static String defaultTarget(GoalType type) {
        return switch (type) {
            case EAT -> "fed";
            case SLEEP -> "rested";
            case FLEE -> "safe";
            case FIGHT -> "enemy_defeated";
            case BUILD -> "built";
            case WORK -> "work_done";
            case SOCIALIZE -> "socialized";
            case IDLE -> "";
        };
    }
}
