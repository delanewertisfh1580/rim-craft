package com.rimworldcraft.core.storyteller;

import com.rimworldcraft.core.persistence.AggregateVersion;
import com.rimworldcraft.core.persistence.SaveDocument;
import com.rimworldcraft.core.shared.IncidentId;
import com.rimworldcraft.core.shared.SchemaVersion;
import com.rimworldcraft.core.shared.StorytellerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Persistence boundary containing only immutable Storyteller facts. */
public final class StorytellerSnapshotMapper {
    public static final int SCHEMA_VERSION = 1;

    public SaveDocument toDocument(Storyteller storyteller, long savedAtTick) {
        Objects.requireNonNull(storyteller, "storyteller");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("storytellerId", storyteller.id().value().toString());
        payload.put("worldId", storyteller.worldId().value().toString());
        payload.put("threatAvailable", storyteller.threatBudget().available());
        payload.put("threatCapacity", storyteller.threatBudget().capacity());
        payload.put("cooldowns", storyteller.cooldowns().lastScheduledTicks());
        payload.put("pressure", storyteller.pacing().pressure());
        payload.put("lastIncidentTick", storyteller.pacing().lastIncidentTick());
        payload.put("appliedOutcomes", storyteller.appliedOutcomes().stream().map(id -> id.value().toString()).toList());
        payload.put("recentIncidents", storyteller.recentIncidents().stream().map(incident -> Map.of(
                "incidentId", incident.incidentId().value().toString(), "definitionId", incident.definitionId(),
                "type", incident.type().name(), "scheduledTick", incident.scheduledTick(), "threatPoints", incident.threatPoints())).toList());
        return new SaveDocument("rwc-save", 1, "storyteller", storyteller.id().value().toString(), storyteller.worldId(),
                new SchemaVersion(SCHEMA_VERSION), new AggregateVersion(0), savedAtTick, payload, Map.of());
    }

    public Storyteller fromDocument(SaveDocument document) {
        Objects.requireNonNull(document, "document");
        if (!"storyteller".equals(document.aggregateType())) throw new IllegalArgumentException("wrong aggregate type");
        if (document.schemaVersion().value() > SCHEMA_VERSION) throw new IllegalArgumentException("unsupported future storyteller schema version");
        Map<String, Object> payload = document.payload();
        StorytellerId id = new StorytellerId(UUID.fromString(string(payload, "storytellerId", document.aggregateId())));
        WorldId world = new WorldId(UUID.fromString(string(payload, "worldId", document.worldId().value().toString())));
        ThreatBudget budget = new ThreatBudget(integer(payload, "threatAvailable", 0), integer(payload, "threatCapacity", 0));
        IncidentCooldowns cooldowns = new IncidentCooldowns(longMap(payload.get("cooldowns")));
        PacingState pacing = new PacingState(integer(payload, "pressure", 0), longValue(payload, "lastIncidentTick", 0));
        List<IncidentRecord> history = incidentList(payload.get("recentIncidents"));
        Set<IncidentId> applied = stringList(payload.get("appliedOutcomes")).stream().map(value -> new IncidentId(UUID.fromString(value))).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new Storyteller(id, world, budget, cooldowns, pacing, history, applied);
    }

    private static String string(Map<String, Object> values, String key, String fallback) { return values.get(key) == null ? fallback : String.valueOf(values.get(key)); }
    private static int integer(Map<String, Object> values, String key, int fallback) { return values.get(key) instanceof Number number ? number.intValue() : fallback; }
    private static long longValue(Map<String, Object> values, String key, long fallback) { return values.get(key) instanceof Number number ? number.longValue() : fallback; }
    private static Map<String, Long> longMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) return Map.of();
        Map<String, Long> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> { if (key != null && item instanceof Number number) result.put(String.valueOf(key), number.longValue()); });
        return result;
    }
    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(String::valueOf).toList();
    }
    @SuppressWarnings("unchecked")
    private static List<IncidentRecord> incidentList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<IncidentRecord> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) continue;
            Map<String, Object> map = (Map<String, Object>) raw;
            result.add(new IncidentRecord(new IncidentId(UUID.fromString(String.valueOf(map.get("incidentId")))),
                    String.valueOf(map.get("definitionId")), IncidentType.valueOf(String.valueOf(map.get("type"))),
                    ((Number) map.getOrDefault("scheduledTick", 0)).longValue(), ((Number) map.getOrDefault("threatPoints", 0)).intValue()));
        }
        return List.copyOf(result);
    }
}
