package com.rimworldcraft.core.storyteller;

import com.rimworldcraft.core.contracts.StorytellerColonySummary;
import com.rimworldcraft.core.ports.driven.IncidentExecutionIntentPort;
import com.rimworldcraft.core.ports.driven.RandomPort;
import com.rimworldcraft.core.ports.driven.StorytellerColonySummaryPort;
import com.rimworldcraft.core.ports.driven.StorytellerEventPort;
import com.rimworldcraft.core.ports.driven.StorytellerPopulationSummaryPort;
import com.rimworldcraft.core.ports.driven.StorytellerRepository;
import com.rimworldcraft.core.ports.driven.WorldSnapshotPort;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.IncidentId;
import com.rimworldcraft.core.shared.RegionId;
import com.rimworldcraft.core.shared.StorytellerId;
import com.rimworldcraft.core.shared.WorldId;
import com.rimworldcraft.core.world.WorldRegion;
import com.rimworldcraft.core.world.WorldSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Server-authoritative storyteller orchestration over summaries and world snapshots. */
public final class StorytellerApplicationService {
    private final StorytellerRepository repository;
    private final StorytellerColonySummaryPort colonies;
    private final StorytellerPopulationSummaryPort population;
    private final WorldSnapshotPort worlds;
    private final RandomPort random;
    private final IncidentExecutionIntentPort execution;
    private final StorytellerEventPort events;
    private final StorytellerConfigSnapshot config;

    public StorytellerApplicationService(StorytellerRepository repository,
                                         StorytellerColonySummaryPort colonies,
                                         StorytellerPopulationSummaryPort population,
                                         WorldSnapshotPort worlds,
                                         RandomPort random,
                                         IncidentExecutionIntentPort execution,
                                         StorytellerEventPort events,
                                         StorytellerConfigSnapshot config) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.colonies = Objects.requireNonNull(colonies, "colonies");
        this.population = Objects.requireNonNull(population, "population");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.random = Objects.requireNonNull(random, "random");
        this.execution = Objects.requireNonNull(execution, "execution");
        this.events = Objects.requireNonNull(events, "events");
        this.config = Objects.requireNonNull(config, "config");
    }

    public IncidentDecision evaluate(WorldId worldId, StorytellerId storytellerId, ColonyId colonyId,
                                     long tick, List<IncidentDefinition> definitions) {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(storytellerId, "storytellerId");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(definitions, "definitions");
        if (tick < 0) throw new IllegalArgumentException("tick must be >= 0");
        Storyteller storyteller = repository.find(worldId, storytellerId)
                .orElseGet(() -> Storyteller.create(storytellerId, worldId, 1000));
        StorytellerColonySummary colony = colonies.find(worldId, colonyId).orElse(null);
        if (colony == null || !colony.active()) return postpone("COLONY_UNAVAILABLE", tick);
        if (!worldId.equals(colony.worldId())) throw new IllegalArgumentException("colony world mismatch");
        if (!storyteller.recentIncidents().isEmpty()
                && tick - storyteller.pacing().lastIncidentTick() < config.minimumTicksBetweenIncidents()) {
            return postpone("PACING_COOLDOWN", tick);
        }
        var populationSummary = population.find(worldId).orElse(null);
        if (populationSummary == null) return postpone("POPULATION_UNAVAILABLE", tick);
        Optional<WorldSnapshot> world = worlds.snapshot(worldId);
        if (world.isEmpty()) return postpone("WORLD_SNAPSHOT_UNAVAILABLE", tick);
        Set<String> worldHazards = world.get().regions().values().stream()
                .flatMap(region -> region.hazards().hazardIds().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<IncidentDefinition> eligible = definitions.stream()
                .filter(definition -> eligible(definition, storyteller, colony, populationSummary.livingCitizens(), tick, worldHazards))
                .sorted(Comparator.comparing(IncidentDefinition::id))
                .toList();
        if (eligible.isEmpty()) return IncidentDecision.notEligible("NO_ELIGIBLE_INCIDENT", tick);
        IncidentDefinition selected = weightedSelect(eligible);
        Optional<WorldRegion> region = world.get().regions().values().stream()
                .filter(candidate -> !candidate.spawnEntryPoints().isEmpty())
                .sorted(Comparator.comparing(candidate -> candidate.regionId().value().toString()))
                .findFirst();
        if (region.isEmpty()) return postpone("NO_SPAWN_ENTRY_POINT", tick);
        int threat = scaledThreat(selected, colony);
        if (threat > storyteller.threatBudget().available()) return postpone("THREAT_BUDGET_EXCEEDED", tick);
        GridPosition position = selectPosition(region.get());
        IncidentId incidentId = new IncidentId(java.util.UUID.nameUUIDFromBytes(
                (worldId + ":" + colonyId + ":" + selected.id() + ":" + tick).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        SpawnEntryPointRequest request = new SpawnEntryPointRequest(incidentId, worldId, colonyId,
                region.get().regionId(), position, selected.type(), threat);
        Storyteller scheduled = storyteller.schedule(new IncidentRecord(incidentId, selected.id(), selected.type(), tick, threat),
                Math.min(100, threat * config.pressurePerThreatPoint()), config.maxHistorySize(), selected.cooldownTicks());
        repository.save(scheduled);
        execution.submit(request);
        events.incidentScheduled(request);
        return new IncidentDecision(IncidentDecision.Status.SCHEDULED, Optional.of(request), "SCHEDULED", tick);
    }

    public Storyteller applyOutcome(WorldId worldId, StorytellerId storytellerId, IncidentOutcome outcome) {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(storytellerId, "storytellerId");
        Objects.requireNonNull(outcome, "outcome");
        Storyteller storyteller = repository.find(worldId, storytellerId)
                .orElseThrow(() -> new IllegalArgumentException("storyteller not found"));
        Storyteller next = storyteller.applyOutcome(outcome);
        repository.save(next);
        events.incidentOutcomeApplied(outcome);
        return next;
    }

    private IncidentDecision postpone(String reason, long tick) {
        IncidentDecision decision = IncidentDecision.postponed(reason, tick + config.retryWindowTicks());
        events.incidentPostponed(decision);
        return decision;
    }

    private boolean eligible(IncidentDefinition definition, Storyteller storyteller,
                             StorytellerColonySummary colony, int livingCitizens, long tick,
                             java.util.Set<String> worldHazards) {
        if (colony.population() < definition.minimumPopulation()) return false;
        if (storyteller.threatBudget().available() < definition.minimumThreatPoints()) return false;
        if (!storyteller.cooldowns().ready(definition.id(), tick, definition.cooldownTicks())) return false;
        return definition.eligibleHazards().isEmpty()
                || definition.eligibleHazards().stream().anyMatch(worldHazards::contains);
    }

    private IncidentDefinition weightedSelect(List<IncidentDefinition> definitions) {
        double total = definitions.stream().mapToDouble(IncidentDefinition::weight).sum();
        double roll = random.nextDouble() * total;
        for (IncidentDefinition definition : definitions) {
            roll -= definition.weight();
            if (roll < 0) return definition;
        }
        return definitions.get(definitions.size() - 1);
    }

    private int scaledThreat(IncidentDefinition definition, StorytellerColonySummary colony) {
        long result = Math.round(definition.baseThreatPoints()
                + (long) definition.threatPointsPerCitizen() * colony.population()
                + definition.wealthFactor() * colony.wealth());
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, result));
    }

    private GridPosition selectPosition(WorldRegion region) {
        List<GridPosition> positions = new ArrayList<>(region.spawnEntryPoints());
        positions.sort(Comparator.comparingInt(GridPosition::x)
                .thenComparingInt(GridPosition::y).thenComparingInt(GridPosition::z));
        return positions.get(Math.min(positions.size() - 1, Math.max(0, random.nextInt(positions.size()))));
    }
}
