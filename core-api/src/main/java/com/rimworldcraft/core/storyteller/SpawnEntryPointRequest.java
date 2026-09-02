package com.rimworldcraft.core.storyteller;

import com.rimworldcraft.core.shared.ColonyId;
import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.IncidentId;
import com.rimworldcraft.core.shared.RegionId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;

/** Request for an adapter to materialize a selected incident entry point. */
public record SpawnEntryPointRequest(IncidentId incidentId, WorldId worldId, ColonyId colonyId,
                                     RegionId regionId, GridPosition position, IncidentType incidentType,
                                     int threatPoints) {
    public SpawnEntryPointRequest {
        Objects.requireNonNull(incidentId, "incidentId");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(incidentType, "incidentType");
        if (threatPoints < 0) throw new IllegalArgumentException("threatPoints must be >= 0");
    }
}
