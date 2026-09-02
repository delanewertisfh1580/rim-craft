package com.rimworldcraft.core.storyteller;

import com.rimworldcraft.core.contracts.StorytellerColonySummary;
import com.rimworldcraft.core.contracts.StorytellerPopulationSummary;
import com.rimworldcraft.core.ports.driven.IncidentExecutionIntentPort;
import com.rimworldcraft.core.ports.driven.RandomPort;
import com.rimworldcraft.core.ports.driven.StorytellerColonySummaryPort;
import com.rimworldcraft.core.ports.driven.StorytellerEventPort;
import com.rimworldcraft.core.ports.driven.StorytellerPopulationSummaryPort;
import com.rimworldcraft.core.ports.driven.WorldSnapshotPort;
import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.StorytellerId;
import com.rimworldcraft.core.shared.WorldId;
import com.rimworldcraft.core.world.Accessibility;
import com.rimworldcraft.core.world.ClimateFacts;
import com.rimworldcraft.core.world.HazardFacts;
import com.rimworldcraft.core.world.RegionBounds;
import com.rimworldcraft.core.world.ResourceFacts;
import com.rimworldcraft.core.world.TerrainFacts;
import com.rimworldcraft.core.world.WorldRegion;
import com.rimworldcraft.core.world.WorldSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorytellerContextTest {
    private static final WorldId WORLD = new WorldId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
    private static final StorytellerId STORYTELLER = new StorytellerId(UUID.fromString("00000000-0000-0000-0000-000000000202"));
    private static final ColonyId COLONY = new ColonyId(UUID.fromString("00000000-0000-0000-0000-000000000203"));
    private static final RegionIdHolder REGION = new RegionIdHolder();

    @Test
    void weightedSelectionIsSeededAndSchedulesPlatformNeutralIntent() {
        InMemoryStorytellerRepository repository = new InMemoryStorytellerRepository();
        Recording recording = new Recording();
        StorytellerApplicationService service = service(repository, recording, 0.51, 0);
        List<IncidentDefinition> definitions = List.of(
                new IncidentDefinition("rwc:quiet", IncidentType.REWARD, 1, 0, 0, 0, 5, 0, 0, Set.of()),
                new IncidentDefinition("rwc:raid", IncidentType.RAID, 9, 0, 1, 0, 20, 10, 0, Set.of()));

        IncidentDecision decision = service.evaluate(WORLD, STORYTELLER, COLONY, 100, definitions);

        assertEquals(IncidentDecision.Status.SCHEDULED, decision.status());
        assertEquals("rwc:raid", repository.find(WORLD, STORYTELLER).orElseThrow().recentIncidents().get(0).definitionId());
        assertEquals(1, recording.executionRequests.size());
        assertEquals(1, recording.scheduledRequests.size());
        assertEquals(WORLD, recording.executionRequests.get(0).worldId());
    }

    @Test
    void pacingCooldownAndMissingEntryPointPostponeWithoutMutation() {
        InMemoryStorytellerRepository repository = new InMemoryStorytellerRepository();
        Recording recording = new Recording();
        WorldSnapshotPort noEntry = world -> Optional.of(snapshot(Set.of()));
        StorytellerApplicationService service = service(repository, recording, 0.0, 0, noEntry);
        IncidentDefinition definition = new IncidentDefinition("rwc:raid", IncidentType.RAID, 1, 0, 1, 0, 10, 0, 0, Set.of());

        IncidentDecision first = service.evaluate(WORLD, STORYTELLER, COLONY, 1, List.of(definition));
        assertEquals(IncidentDecision.Status.POSTPONED, first.status());
        assertEquals("NO_SPAWN_ENTRY_POINT", first.reason());
        assertTrue(repository.find(WORLD, STORYTELLER).isEmpty());

        Storyteller initial = Storyteller.create(STORYTELLER, WORLD, 100);
        Storyteller scheduled = initial.schedule(new IncidentRecord(new com.rimworldcraft.core.shared.IncidentId(UUID.randomUUID()),
                "rwc:raid", IncidentType.RAID, 10, 5), 5, 10, 100);
        assertThrows(IllegalStateException.class, () -> scheduled.schedule(new IncidentRecord(
                new com.rimworldcraft.core.shared.IncidentId(UUID.randomUUID()), "rwc:raid", IncidentType.RAID, 20, 5), 5, 10, 100));
    }

    @Test
    void incidentOutcomeIsIdempotentAndSuccessfulOutcomeRestoresBudget() {
        Storyteller initial = Storyteller.create(STORYTELLER, WORLD, 100);
        com.rimworldcraft.core.shared.IncidentId incidentId = new com.rimworldcraft.core.shared.IncidentId(UUID.randomUUID());
        Storyteller scheduled = initial.schedule(new IncidentRecord(incidentId, "rwc:raid", IncidentType.RAID, 5, 20), 10, 10);
        IncidentOutcome outcome = new IncidentOutcome(incidentId, true, -5, "RESOLVED");

        Storyteller applied = scheduled.applyOutcome(outcome);
        Storyteller repeated = applied.applyOutcome(outcome);

        assertEquals(100, applied.threatBudget().available());
        assertEquals(applied, repeated);
        assertTrue(applied.hasAppliedOutcome(incidentId));
    }

    @Test
    void snapshotMapperRoundTripsAndRejectsFutureSchema() {
        Storyteller storyteller = Storyteller.create(STORYTELLER, WORLD, 100);
        StorytellerSnapshotMapper mapper = new StorytellerSnapshotMapper();

        Storyteller restored = mapper.fromDocument(mapper.toDocument(storyteller, 12));

        assertEquals(storyteller, restored);
        var future = new com.rimworldcraft.core.persistence.SaveDocument("rwc-save", 1, "storyteller",
                STORYTELLER.value().toString(), WORLD, new com.rimworldcraft.core.shared.SchemaVersion(2),
                new com.rimworldcraft.core.persistence.AggregateVersion(0), 12, Map.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> mapper.fromDocument(future));
    }

    private static StorytellerApplicationService service(InMemoryStorytellerRepository repository, Recording recording,
                                                          double roll, int minimumInterval) {
        return service(repository, recording, roll, minimumInterval, world -> Optional.of(snapshot(Set.of(new GridPosition(2, 64, 2)))));
    }

    private static StorytellerApplicationService service(InMemoryStorytellerRepository repository, Recording recording,
                                                          double roll, int minimumInterval, WorldSnapshotPort worlds) {
        StorytellerColonySummaryPort colonies = (world, colony) -> Optional.of(new StorytellerColonySummary(WORLD, COLONY, 3, 500, 70, true));
        StorytellerPopulationSummaryPort population = world -> Optional.of(new StorytellerPopulationSummary(WORLD, 3, 0));
        return new StorytellerApplicationService(repository, colonies, population, worlds,
                new FixedRandom(roll), recording, recording,
                new StorytellerConfigSnapshot(minimumInterval, 5, 1, 20));
    }

    private static WorldSnapshot snapshot(Set<GridPosition> entries) {
        var regionId = REGION.id;
        WorldRegion region = new WorldRegion(WORLD, regionId,
                new RegionBounds(new GridPosition(0, 0, 0), new GridPosition(10, 100, 10)),
                new TerrainFacts("PLAINS", 64, 0, true, true), new ClimateFacts("temperate", 18, 50, 40),
                new HazardFacts(0, Set.of()), new ResourceFacts(Map.of()),
                Accessibility.accessible(1), entries, 1);
        return new WorldSnapshot(WORLD, 1, Map.of(regionId, region));
    }

    private static final class RegionIdHolder {
        private final com.rimworldcraft.core.shared.RegionId id = new com.rimworldcraft.core.shared.RegionId(
                UUID.fromString("00000000-0000-0000-0000-000000000204"));
    }

    private static final class FixedRandom implements RandomPort {
        private final double roll;
        private FixedRandom(double roll) { this.roll = roll; }
        @Override public int nextInt(int bound) { return 0; }
        @Override public double nextDouble() { return roll; }
    }

    private static final class Recording implements IncidentExecutionIntentPort, StorytellerEventPort {
        private final List<SpawnEntryPointRequest> executionRequests = new ArrayList<>();
        private final List<SpawnEntryPointRequest> scheduledRequests = new ArrayList<>();
        private final List<IncidentDecision> postponed = new ArrayList<>();
        @Override public void submit(SpawnEntryPointRequest request) { executionRequests.add(request); }
        @Override public void incidentScheduled(SpawnEntryPointRequest request) { scheduledRequests.add(request); }
        @Override public void incidentPostponed(IncidentDecision decision) { postponed.add(decision); }
    }
}
