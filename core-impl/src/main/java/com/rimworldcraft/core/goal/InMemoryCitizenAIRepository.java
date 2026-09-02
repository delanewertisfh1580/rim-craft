package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.ports.driven.CitizenAIRepository;
import com.rimworldcraft.core.shared.CitizenId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Deterministic repository implementation for the platform-neutral JVM skeleton. */
public final class InMemoryCitizenAIRepository implements CitizenAIRepository {
    private final Map<CitizenId, CitizenAIState> states = new HashMap<>();

    @Override
    public Optional<CitizenAIState> find(CitizenId id) {
        return Optional.ofNullable(states.get(Objects.requireNonNull(id, "id")));
    }

    @Override
    public void save(CitizenAIState state) {
        states.put(Objects.requireNonNull(state, "state").citizenId(), state);
    }
}
