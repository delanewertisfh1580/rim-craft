package com.rimworldcraft.core.events;

import com.rimworldcraft.core.shared.*;
import java.util.Objects;

public final class TypedEventContracts {
    private TypedEventContracts() {}
    public record ColonyFounded(WorldId worldId, ColonyId colonyId) { public ColonyFounded { Objects.requireNonNull(worldId); Objects.requireNonNull(colonyId); } }
    public record WorkAssigned(WorldId worldId, ColonyId colonyId, CitizenId citizenId, String jobType) { public WorkAssigned { Objects.requireNonNull(worldId); Objects.requireNonNull(colonyId); Objects.requireNonNull(citizenId); if(jobType==null||jobType.isBlank()) throw new IllegalArgumentException("jobType"); } }
    public record JobCompleted(WorldId worldId, ColonyId colonyId, CitizenId citizenId, String jobType) { public JobCompleted { Objects.requireNonNull(worldId); Objects.requireNonNull(colonyId); Objects.requireNonNull(citizenId); } }
    public record NpcDied(WorldId worldId, CitizenId citizenId, ColonyId colonyId) { public NpcDied { Objects.requireNonNull(worldId); Objects.requireNonNull(citizenId); } }
    public record RaidGenerated(WorldId worldId, ColonyId colonyId, IncidentId incidentId) { public RaidGenerated { Objects.requireNonNull(worldId); Objects.requireNonNull(colonyId); Objects.requireNonNull(incidentId); } }
}
