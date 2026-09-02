package com.rimworldcraft.core.storyteller;

import com.rimworldcraft.core.shared.IncidentId;
import com.rimworldcraft.core.shared.StorytellerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Aggregate root owning narrative pressure and incident scheduling state. */
public record Storyteller(StorytellerId id, WorldId worldId, ThreatBudget threatBudget,
                          IncidentCooldowns cooldowns, PacingState pacing,
                          List<IncidentRecord> recentIncidents, Set<IncidentId> appliedOutcomes) {
    public Storyteller {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(threatBudget, "threatBudget");
        Objects.requireNonNull(cooldowns, "cooldowns");
        Objects.requireNonNull(pacing, "pacing");
        Objects.requireNonNull(recentIncidents, "recentIncidents");
        Objects.requireNonNull(appliedOutcomes, "appliedOutcomes");
        if (recentIncidents.stream().anyMatch(Objects::isNull) || appliedOutcomes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("history must not contain nulls");
        }
        recentIncidents = List.copyOf(recentIncidents);
        appliedOutcomes = Set.copyOf(appliedOutcomes);
    }

    public static Storyteller create(StorytellerId id, WorldId worldId, int threatCapacity) {
        return new Storyteller(id, worldId, new ThreatBudget(threatCapacity, threatCapacity),
                IncidentCooldowns.empty(), new PacingState(0, 0), List.of(), Set.of());
    }

    public Storyteller schedule(IncidentRecord incident, int pressureDelta, int historyLimit) {
        return schedule(incident, pressureDelta, historyLimit, 0);
    }

    public Storyteller schedule(IncidentRecord incident, int pressureDelta, int historyLimit, long cooldownTicks) {
        Objects.requireNonNull(incident, "incident");
        if (historyLimit < 1 || cooldownTicks < 0) throw new IllegalArgumentException("invalid schedule policy");
        if (!cooldowns.ready(incident.definitionId(), incident.scheduledTick(), cooldownTicks)) {
            throw new IllegalStateException("incident is on cooldown");
        }
        Storyteller next = new Storyteller(id, worldId, threatBudget.spend(incident.threatPoints()),
                cooldowns.record(incident.definitionId(), incident.scheduledTick()),
                pacing.afterIncident(pressureDelta, incident.scheduledTick()),
                appendHistory(incident, historyLimit), appliedOutcomes);
        return next;
    }

    public Storyteller applyOutcome(IncidentOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        if (appliedOutcomes.contains(outcome.incidentId())) return this;
        IncidentRecord record = recentIncidents.stream()
                .filter(incident -> incident.incidentId().equals(outcome.incidentId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("unknown incident outcome"));
        return new Storyteller(id, worldId,
                outcome.successful() ? threatBudget.replenish(record.threatPoints()) : threatBudget,
                cooldowns, pacing.afterIncident(outcome.pressureDelta(), record.scheduledTick()),
                recentIncidents, union(appliedOutcomes, outcome.incidentId()));
    }

    public boolean hasAppliedOutcome(IncidentId incidentId) {
        return appliedOutcomes.contains(Objects.requireNonNull(incidentId, "incidentId"));
    }

    private List<IncidentRecord> appendHistory(IncidentRecord incident, int limit) {
        java.util.ArrayList<IncidentRecord> next = new java.util.ArrayList<>(recentIncidents);
        next.add(incident);
        if (next.size() > limit) next.subList(0, next.size() - limit).clear();
        return next;
    }

    private static Set<IncidentId> union(Set<IncidentId> values, IncidentId value) {
        java.util.HashSet<IncidentId> next = new java.util.HashSet<>(values);
        next.add(value);
        return next;
    }
}
