package com.rimworldcraft.core.npc.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable snapshot of all configured needs. */
public record NeedState(Map<NeedType, Need> values) {
    public NeedState {
        Objects.requireNonNull(values, "values");
        EnumMap<NeedType, Need> copy = new EnumMap<>(NeedType.class);
        copy.putAll(values);
        if (copy.isEmpty()) throw new IllegalArgumentException("at least one need is required");
        values = Map.copyOf(copy);
    }
    public Need require(NeedType type) { return Objects.requireNonNull(values.get(type), "missing need: " + type); }
    public NeedState decay(long ticks) {
        EnumMap<NeedType, Need> next = new EnumMap<>(NeedType.class);
        values.forEach((type, need) -> next.put(type, need.decay(ticks)));
        return new NeedState(next);
    }
}
