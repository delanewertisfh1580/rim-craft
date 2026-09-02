package com.rimworldcraft.core.storyteller;

import java.util.Map;

/** Last-scheduled tick per incident ID. */
public record IncidentCooldowns(Map<String, Long> lastScheduledTicks) {
    public IncidentCooldowns {
        if (lastScheduledTicks == null || lastScheduledTicks.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue() < 0)) {
            throw new IllegalArgumentException("invalid cooldowns");
        }
        lastScheduledTicks = Map.copyOf(lastScheduledTicks);
    }
    public static IncidentCooldowns empty() { return new IncidentCooldowns(Map.of()); }
    public boolean ready(String incidentId, long now, long cooldownTicks) {
        Long previous = lastScheduledTicks.get(incidentId);
        return previous == null || now - previous >= cooldownTicks;
    }
    public IncidentCooldowns record(String incidentId, long tick) {
        var next = new java.util.HashMap<>(lastScheduledTicks);
        next.put(incidentId, tick);
        return new IncidentCooldowns(next);
    }
}
