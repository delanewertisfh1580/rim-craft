package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.api.types.CitizenId;
import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.WorldId;
import com.rimworldcraft.core.contracts.CitizenSummary;
import java.util.List;

/** Read-only citizen projection port for other bounded contexts. */
public interface CitizenSummaryPort {
    /** Returns summaries for citizens belonging to a colony. */
    List<CitizenSummary> findByColony(WorldId worldId, ColonyId colonyId);
    /** Returns one citizen summary in a world scope. */
    java.util.Optional<CitizenSummary> findById(WorldId worldId, CitizenId citizenId);
}
