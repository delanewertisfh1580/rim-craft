package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.shared.CitizenId;
import java.util.*;

/** In-memory task boundary used by the JVM skeleton and contract tests. */
public final class InMemoryTaskManager implements TaskManager {
    private final Map<CitizenId, List<Task>> assignments = new HashMap<>();
    private final Set<UUID> completed = new HashSet<>();

    @Override
    public Optional<Task> next(CitizenId citizenId, WorldState state) {
        Objects.requireNonNull(citizenId, "citizenId");
        return assignments.getOrDefault(citizenId, List.of()).stream()
                .filter(task -> !completed.contains(task.id()))
                .sorted(Comparator.comparingInt(Task::priority).reversed().thenComparing(Task::id))
                .findFirst();
    }

    @Override
    public void assign(Task task, CitizenId citizenId) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(citizenId, "citizenId");
        assignments.computeIfAbsent(citizenId, ignored -> new ArrayList<>()).removeIf(existing -> existing.id().equals(task.id()));
        assignments.computeIfAbsent(citizenId, ignored -> new ArrayList<>()).add(task);
    }

    @Override public void progress(Task task, int amount) {
        Objects.requireNonNull(task, "task");
        if (amount < 0) throw new IllegalArgumentException("amount");
    }

    @Override public void complete(Task task) { completed.add(Objects.requireNonNull(task, "task").id()); }

    @Override public void cancel(Task task, String reason) {
        Objects.requireNonNull(task, "task");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason");
        completed.add(task.id());
    }
}
