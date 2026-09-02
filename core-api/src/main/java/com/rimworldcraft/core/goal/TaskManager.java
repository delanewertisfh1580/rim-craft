package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.shared.CitizenId;
import java.util.List;
import java.util.Optional;

/** Application boundary for player/colony work tasks; it does not own Building aggregates. */
public interface TaskManager {
    Optional<Task> next(CitizenId citizenId, WorldState state);
    void assign(Task task, CitizenId citizenId);
    void progress(Task task, int amount);
    void complete(Task task);
    void cancel(Task task, String reason);

    default List<Task> availableFor(CitizenId citizenId, WorldState state) {
        return next(citizenId, state).map(List::of).orElseGet(List::of);
    }
}
