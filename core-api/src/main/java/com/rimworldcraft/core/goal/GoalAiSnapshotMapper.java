package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.persistence.AggregateVersion;
import com.rimworldcraft.core.persistence.SaveDocument;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.SchemaVersion;
import com.rimworldcraft.core.shared.WorldId;
import java.util.*;
import java.util.stream.Collectors;

/** Maps only stable AI identifiers and plan definitions; runtime execution handles are excluded. */
public final class GoalAiSnapshotMapper {
    public static final int SCHEMA_VERSION = 1;

    public SaveDocument toDocument(CitizenAIState state) {
        Objects.requireNonNull(state, "state");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("citizenId", state.citizenId().value().toString());
        payload.put("worldId", state.worldId().value().toString());
        payload.put("status", state.status().name());
        payload.put("replans", state.replans());
        payload.put("nextActionIndex", state.nextActionIndex());
        payload.put("actionStartedTick", state.actionStartedTick());
        payload.put("actionAttempts", state.actionAttempts());
        payload.put("lastTick", state.lastTick());
        if (state.goal() != null) payload.put("goal", Map.of(
                "type", state.goal().type().name(), "priority", state.goal().priority(), "targetKey", state.goal().targetKey()));
        if (state.plan() != null) payload.put("plan", planMap(state.plan()));
        return new SaveDocument("rwc-save", 1, "citizen-ai", state.citizenId().value().toString(),
                state.worldId(), new SchemaVersion(SCHEMA_VERSION), new AggregateVersion(0), state.lastTick(), payload, Map.of());
    }

    public CitizenAIState fromDocument(SaveDocument document) {
        Objects.requireNonNull(document, "document");
        if (!"citizen-ai".equals(document.aggregateType())) throw new IllegalArgumentException("wrong aggregate type");
        if (document.schemaVersion().value() > SCHEMA_VERSION) throw new IllegalArgumentException("unsupported future AI schema version");
        Map<String, Object> p = document.payload();
        CitizenId citizenId = new CitizenId(UUID.fromString(stringValue(p, "citizenId", document.aggregateId())));
        WorldId worldId = new WorldId(UUID.fromString(stringValue(p, "worldId", document.worldId().value().toString())));
        Goal goal = goalFrom(p.get("goal"));
        Plan plan = planFrom(p.get("plan"), goal);
        AIStatus status = enumValue(AIStatus.class, stringValue(p, "status", plan == null ? "IDLE" : "ACTIVE"));
        if (plan == null && status == AIStatus.ACTIVE) status = AIStatus.IDLE;
        return new CitizenAIState(citizenId, worldId, goal, plan, status,
                intValue(p, "replans", 0), intValue(p, "nextActionIndex", 0),
                longValue(p, "actionStartedTick", 0), intValue(p, "actionAttempts", 0),
                longValue(p, "lastTick", document.savedAtTick()));
    }

    private static Map<String, Object> planMap(Plan plan) {
        return Map.of("id", plan.id().toString(), "totalCost", plan.totalCost(), "createdTick", plan.createdTick(),
                "actions", plan.actions().stream().map(GoalAiSnapshotMapper::actionMap).toList());
    }

    private static Map<String, Object> actionMap(ActionDefinition action) {
        return Map.of("id", action.id(), "type", action.type().name(), "cost", action.cost(),
                "durationTicks", action.durationTicks(), "preconditions", facts(action.preconditions()), "effects", facts(action.effects()));
    }

    private static List<Map<String, Object>> facts(Set<StateFact> facts) {
        return facts.stream().sorted(Comparator.comparing(StateFact::key)).map(f -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("key", f.key());
            result.put("value", f.value());
            return result;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private static Goal goalFrom(Object value) {
        if (!(value instanceof Map<?, ?> raw)) return null;
        Map<String, Object> map = (Map<String, Object>) raw;
        return new Goal(GoalType.valueOf(String.valueOf(map.get("type"))), number(map.get("priority"), 1), String.valueOf(map.getOrDefault("targetKey", "")));
    }

    @SuppressWarnings("unchecked")
    private static Plan planFrom(Object value, Goal goal) {
        if (!(value instanceof Map<?, ?> raw) || goal == null) return null;
        Map<String, Object> map = (Map<String, Object>) raw;
        Object rawActions = map.get("actions");
        if (!(rawActions instanceof List<?> list)) return null;
        List<ActionDefinition> actions = list.stream().filter(Map.class::isInstance).map(item -> actionFrom((Map<String, Object>) item)).toList();
        return new Plan(UUID.fromString(String.valueOf(map.get("id"))), goal, actions,
                number(map.get("totalCost"), 0), longNumber(map.get("createdTick"), 0));
    }

    private static ActionDefinition actionFrom(Map<String, Object> map) {
        return new ActionDefinition(String.valueOf(map.get("id")), ActionType.valueOf(String.valueOf(map.get("type"))),
                number(map.get("cost"), 0), number(map.get("durationTicks"), 0), factsFrom(map.get("preconditions")), factsFrom(map.get("effects")));
    }

    @SuppressWarnings("unchecked")
    private static Set<StateFact> factsFrom(Object value) {
        if (!(value instanceof List<?> list)) return Set.of();
        return list.stream().filter(Map.class::isInstance).map(item -> {
            Map<String, Object> map = (Map<String, Object>) item;
            return new StateFact(String.valueOf(map.get("key")), Boolean.parseBoolean(String.valueOf(map.getOrDefault("value", true))));
        }).collect(Collectors.toUnmodifiableSet());
    }

    private static String stringValue(Map<String, Object> map, String key, String fallback) { return map.get(key) == null ? fallback : String.valueOf(map.get(key)); }
    private static int intValue(Map<String, Object> map, String key, int fallback) { return number(map.get(key), fallback); }
    private static long longValue(Map<String, Object> map, String key, long fallback) { return longNumber(map.get(key), fallback); }
    private static int number(Object value, int fallback) { return value instanceof Number n ? n.intValue() : fallback; }
    private static long longNumber(Object value, long fallback) { return value instanceof Number n ? n.longValue() : fallback; }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) { return Enum.valueOf(type, value); }
}
